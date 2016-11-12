package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.gwt.xhr.client.XMLHttpRequest.ResponseType;
import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.DataSource;
import com.lizardtech.djvu.DjVmDir;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.IFFEnumeration;
import com.lizardtech.djvu.URLInputStream;
import com.lizardtech.djvu.text.DjVuText;

import pl.djvuhtml5.client.Djvu_html5.Status;

public class PageCache implements DataSource {

	private static class FileItem {
		public Uint8Array data;
		public int dataSize;
		public final List<ReadyListener> listeners = new ArrayList<>();
		public boolean downloadStarted;
	}

	private class PageItem implements Comparable<PageItem> {
		public final int pageNum;
		public DjVuPage page;
		public DjVuInfo info;
		public DjVuText text;
		public boolean isDecoded;
		public int memoryUsage;
		public int rank = 10000;

		public PageItem(int pageNum) {
			this.pageNum = pageNum;
		}

		@Override
		public int compareTo(PageItem o) {
			int result = this.rank - o.rank;
			if (result == 0)
				result = Math.abs(lastRequestedPage - o.pageNum) - Math.abs(lastRequestedPage - this.pageNum);
			return -result;
		}

		public void setText(DjVuText text) {
			if (text == null)
				text = new DjVuText();
			this.text = text;
			if (text.length() > 0)
				textAvailable = true;
			fireDecoded(pageNum, textDecodeListeners);
		}
	}

	private final Djvu_html5 app;

	private Document document;
	
	private final HashMap<String, FileItem> fileCache = new HashMap<>();
	/** Most recently used files are at the beginning */
	private final List<FileItem> filesByMRU = new ArrayList<>();

	private long filesMemoryUsage = 0;

	private int downloadsInProgress = 0;

	private boolean textDownloadInProgress = false;
	private boolean textAvailable = false;

	private List<PageItem> pages;
	/** The most important pages are at the beginning. */
	private List<PageItem> pagesByRank;

	private int pagesMemoryUsage = 0;

	private final ArrayList<DecodeListener> fullDecodeListeners = new ArrayList<>();
	private final ArrayList<DecodeListener> textDecodeListeners = new ArrayList<>();

	private int lastRequestedPage = 0;

	public PageCache(final Djvu_html5 app, final String url) {
		this.app = app;

		Uint8Array data = getData(url, new ReadyListener() {
			
			@Override
			public void dataReady() {
				init(url);
			}
		});
		if (data != null)
			init(url);
	}

	private void init(String url) {
		try {
			document = new Document();
			document.read(url);
			int pageCount = document.getDjVmDir().get_pages_num();
			pages = new ArrayList<>(pageCount);
			for (int i = 0; i < pageCount; i++)
				pages.add(new PageItem(i));

			pagesByRank = new ArrayList<>(pages);
			Collections.sort(pagesByRank);

			app.getToolbar().setPageCount(pageCount);

			app.startProcessing();
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not parse document", e);
		}
	}

	public boolean decodeCurrentPage() {
		PageItem currentPageItem = pages.get(lastRequestedPage);
		app.setStatus(currentPageItem.isDecoded ? null : Status.LOADING);
		if (currentPageItem.isDecoded)
			return false;
		cleanCacheOverflow(0);
		return decodePage(currentPageItem);
	}

	public boolean decodePages() {
		int memoryLimit = app.getPageCacheSize();
		int totalMemory = 0;
		int fetchIndex = 0;
		while (fetchIndex < pagesByRank.size() && totalMemory < memoryLimit) {
			PageItem pageItem = pagesByRank.get(fetchIndex);
			if (!pageItem.isDecoded)
				break;
			totalMemory += pageItem.memoryUsage;
			fetchIndex++;
		}
		if (fetchIndex == pagesByRank.size())
			return false; // all is decoded
		cleanCacheOverflow(fetchIndex + 1);
		PageItem pageToDecode = pagesByRank.get(fetchIndex);
		if (pagesMemoryUsage + pageToDecode.memoryUsage > memoryLimit)
			return false; // all the best pages are in memory

		return decodePage(pageToDecode);
	}

	public boolean decodeTexts() {
		PageItem firstMissing = null;
		DjVmDir dir = document.getDjVmDir();
		for (PageItem page : pagesByRank) {
			if (page.text != null)
				continue;
			CachedInputStream stream;
			if (dir.is_bundled()) {
				try {
					stream = document.get_data(page.pageNum, null);
				} catch (IOException e) {
					GWT.log("Error while decoding text in page " + page.pageNum, e);
					return false;
				}
			} else {
				FileItem file = getCachedFile(dir.page_to_url(page.pageNum));
				if (file.data != null) {
					stream = new CachedInputStream().init(new URLInputStream().init(file.data));
				} else {
					if (firstMissing == null)
						firstMissing = page;
					continue;
				}
			}
			extractInfoAndText(page, stream);
		}
		if (firstMissing == null || downloadsInProgress > 0 || textDownloadInProgress || !textAvailable)
			return false;

		// download missing page specifically to extract text (bypass file cache)
		final String url = dir.page_to_url(firstMissing.pageNum);
		final PageItem page = firstMissing;
		XMLHttpRequest request = XMLHttpRequest.create();
		request.open("GET", url);
		request.setResponseType(ResponseType.ArrayBuffer);
		request.setOnReadyStateChange(new ReadyStateChangeHandler() {
			@Override
			public void onReadyStateChange(XMLHttpRequest xhr) {
				if (xhr.getReadyState() != XMLHttpRequest.DONE)
					return;
				textDownloadInProgress = false;
				if (xhr.getStatus() == 200) {
					Uint8Array data = TypedArrays.createUint8Array(xhr.getResponseArrayBuffer());
					extractInfoAndText(page, new CachedInputStream().init(new URLInputStream().init(data)));
					app.startProcessing();
				} else {
					GWT.log("Error downloading " + url);
					GWT.log("response status: " + xhr.getStatus() + " " + xhr.getStatusText());
				}
			}
		});
		request.send();
		textDownloadInProgress = true;
		return true;
	}

	private void extractInfoAndText(PageItem page, CachedInputStream input) {
		try {
			IFFEnumeration chunks = input.getIFFChunks();
			while (chunks.hasMoreElements() && (page.info == null || page.text == null)) {
				CachedInputStream chunk = chunks.nextElement();
				String chunkName = chunk.getName();
				if (chunkName.startsWith("FORM:")) {
					extractInfoAndText(page, chunk);
				} else if ("INFO".equals(chunkName)) {
					page.info = new DjVuInfo();
					page.info.decode(chunk);
				} else if ("TXTa".equals(chunkName) || "TXTz".equals(chunkName)) {
					page.setText(new DjVuText().init(chunk));
				}
			}
			if (page.text == null)
				page.setText(new DjVuText());
		} catch (IOException e) {
			GWT.log("Error while decoding text in page " + page.pageNum, e);
			page.setText(null);
		}
	}

	/**
	 * @param cutoffIndex
	 *            pages in ranking from first to this index will not be removed.
	 *            Not inclusive (the page at this index can be removed).
	 */
	private void cleanCacheOverflow(int cutoffIndex) {
		int memoryLimit = app.getPageCacheSize();
		for (int i = pagesByRank.size() - 1; pagesMemoryUsage > memoryLimit && i >= cutoffIndex; i--) {
			PageItem pageItem = pagesByRank.get(i);
			if (pageItem.pageNum == lastRequestedPage)
				continue;
			if (pageItem.isDecoded) {
				pagesMemoryUsage -= pageItem.memoryUsage;
				pageItem.isDecoded = false;
			}
			pageItem.page = null;
		}
	}

	private boolean decodePage(PageItem pageItem) {
		DjVuPage page = pageItem.page;
		try {
			if (page == null) {
				page = pageItem.page = document.getPage(pageItem.pageNum);
				if (page == null)
					return true; // not downloaded yet
				GWT.log("Decoding page " + pageItem.pageNum);
			}
			if (page.decodeStep()) {
				pageItem.isDecoded = true;
				if (pageItem.info == null) {
					pageItem.info = page.getInfo();
					pageItem.setText(page.getText());
				}
				pageItem.memoryUsage = page.getMemoryUsage();
				pagesMemoryUsage += pageItem.memoryUsage;
				fireDecoded(pageItem.pageNum, fullDecodeListeners);
			}
			return true;
		} catch (IOException e) {
			GWT.log("Error while decoding page " + pageItem.pageNum, e);
			return false;
		}
	}

	public int getPageCount() {
		return pages.size();
	}

	public DjVuPage fetchPage(int number) {
		if (pages == null) {
			lastRequestedPage = number;
			return null;
		}
		if (lastRequestedPage != number) {
			lastRequestedPage = number;
			updateRanks();
			Collections.sort(pagesByRank);
		}
		app.startProcessing();
		return getPage(number);
	}

	private void updateRanks() {
		int points = 0;
		for (PageItem item : pages) {
			int d = item.rank / 10;
			points += d;
			item.rank -= d;
		}
		int[] dd = { 1, -1 };
		int i = 0;
		while (points > 0) {
			for (int d : dd) {
				int index = lastRequestedPage + d * (i % pages.size());
				if (index < 0 || index >= pages.size())
					continue;
				int p = points / 10 + 1;
				points -= p;
				pages.get(index).rank += p;
				if (points <= 0)
					break;
			}
			i++;
		}
	}

	public DjVuPage getPage(int number) {
		PageItem pageItem = pages.get(number);
		return pageItem.isDecoded ? pageItem.page : null;
	}

	public DjVuInfo getInfo(int pageNumber) {
		return pages.get(pageNumber).info;
	}

	public DjVuText getText(int pageNumber) {
		return pages.get(pageNumber).text;
	}

	@Override
	public Uint8Array getData(String url, ReadyListener listener) {
		FileItem entry = getCachedFile(url);
		if (!entry.downloadStarted) {
			downloadFile(url);
		}
		if (entry.data == null && listener != null)
			entry.listeners.add(listener);
		filesByMRU.remove(entry);
		filesByMRU.add(0, entry);
		return entry.data;
	}

	private void downloadFile(final String url) {
		XMLHttpRequest request = XMLHttpRequest.create();
		request.open("GET", url);
		request.setResponseType(ResponseType.ArrayBuffer);
		request.setOnReadyStateChange(new ReadyStateChangeHandler() {
			@Override
			public void onReadyStateChange(XMLHttpRequest xhr) {
				if (xhr.getReadyState() == XMLHttpRequest.DONE) {
					downloadsInProgress--;
					if (xhr.getStatus() == 200) {
						FileItem entry = getCachedFile(url);
						entry.data = TypedArrays.createUint8Array(xhr.getResponseArrayBuffer());
						entry.dataSize = entry.data.byteLength();
						filesMemoryUsage += entry.dataSize;
						checkFilesMemory();
						app.startProcessing();
						fireReady(url);
						continueDownload();
					} else {
						GWT.log("Error downloading " + url);
						GWT.log("response status: " + xhr.getStatus() + " " + xhr.getStatusText());
						app.setStatus(Status.ERROR);
						fileCache.get(url).downloadStarted = false;
					}
				}
			}
		});
		request.send();
		fileCache.get(url).downloadStarted = true;
		downloadsInProgress++;
	}

	protected void checkFilesMemory() {
		int limit = app.getFileCacheSize();
		for (int i = filesByMRU.size() - 1; filesMemoryUsage > limit && i > 4; i--) {
			FileItem item = filesByMRU.remove(i);
			if (item.data == null)
				continue;
			filesMemoryUsage -= item.dataSize;
			item.data = null;
			item.downloadStarted = false;
		}
	}

	protected void fireReady(String url) {
		FileItem entry = fileCache.get(url);
		if (entry == null)
			return;
		for (ReadyListener listener : entry.listeners)
			listener.dataReady();
		entry.listeners.clear();
	}

	protected void continueDownload() {
		DjVmDir dir = document.getDjVmDir();
		if (downloadsInProgress > 0 || dir.is_bundled() || filesMemoryUsage > app.getFileCacheSize())
			return;
		for (PageItem page : pagesByRank) {
			String url = dir.page_to_url(page.pageNum);
			FileItem file = getCachedFile(url);
			if (file.data == null && !file.downloadStarted) {
				if (filesMemoryUsage + file.dataSize < app.getFileCacheSize()) {
					downloadFile(url);
				}
				break;
			}
		}
	}

	private FileItem getCachedFile(String url) {
		FileItem file = fileCache.get(url);
		if (file == null)
			fileCache.put(url, file = new FileItem());
		return file;
	}

	public void addFullDecodeListener(DecodeListener listener) {
		fullDecodeListeners.add(listener);
	}

	public void addTextDecodeListener(DecodeListener listener) {
		textDecodeListeners.add(listener);
	}

	private void fireDecoded(int pageNum, List<DecodeListener> listeners) {
		for (DecodeListener listener : listeners)
			listener.pageDecoded(pageNum);
	}

	public static interface DecodeListener {
		void pageDecoded(int pageNum);
	}
}
