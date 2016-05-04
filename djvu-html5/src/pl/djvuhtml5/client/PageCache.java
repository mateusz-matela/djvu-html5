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
import com.lizardtech.djvu.DataSource;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;

import pl.djvuhtml5.client.Djvu_html5.Status;

public class PageCache implements DataSource {

	private static class FileItem implements Comparable<FileItem> {
		public Uint8Array data;
		public final List<ReadyListener> listeners = new ArrayList<>();
		public long lastUsed;
		public boolean downloadStarted;

		@Override
		public int compareTo(FileItem o) {
			return (int) (this.lastUsed - o.lastUsed);
		}
	}

	private class PageItem implements Comparable<PageItem> {
		public final int pageNum;
		public DjVuPage page;
		public boolean isDecoded;
		public int rank = 10000;

		public PageItem(int pageNum) {
			this.pageNum = pageNum;
		}

		@Override
		public int compareTo(PageItem o) {
			int result = this.rank - o.rank;
			if (result == 0)
				result = Math.abs(lastRequestedPage - o.pageNum) - Math.abs(lastRequestedPage - this.pageNum);
			return result;
		}
	}

	private final Djvu_html5 app;

	private Document document;
	
	private final HashMap<String, FileItem> fileCache = new HashMap<>();

	private long filesMemoryUsage = 0;

	private List<PageItem> pages;

	private int pagesMemoryUsage = 0;

	private final ArrayList<PageDownloadListener> listeners = new ArrayList<>();

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

			app.getToolbar().setPageCount(pageCount);

			app.startProcessing();
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not parse document", e);
		}
	}

	boolean decodePage(boolean currentOnly) {
		PageItem currentPageItem = pages.get(lastRequestedPage);
		int memoryLimit = app.getPageCacheSize();
		List<PageItem> pagesTemp = new ArrayList<>(pages);
		Collections.sort(pagesTemp);
		pagesTemp.remove(currentPageItem);

		if (currentOnly) {
			app.setStatus(currentPageItem.isDecoded ? null : Status.LOADING);
			if (currentPageItem.isDecoded)
				return false;
			cleanCacheOverflow(pagesTemp, memoryLimit);
			return decodePage(currentPageItem);
		}

		int totalMemory = 0;
		int fetchIndex = pagesTemp.size();
		while (fetchIndex-- > 0 && totalMemory < memoryLimit) {
			PageItem pageItem = pagesTemp.get(fetchIndex);
			if (!pageItem.isDecoded)
				break;
			totalMemory += pageItem.page.getMemoryUsage();
		}
		if (fetchIndex < 0)
			return false; // all is decoded
		cleanCacheOverflow(pagesTemp.subList(0, fetchIndex), memoryLimit);
		if (pagesMemoryUsage > memoryLimit)
			return false; // all the best pages are in memory

		return decodePage(pagesTemp.get(fetchIndex));
	}

	private void cleanCacheOverflow(List<PageItem> pagesTemp, int memoryLimit) {
		for (int i = 0; pagesMemoryUsage > memoryLimit && i < pagesTemp.size(); i++) {
			PageItem pageItem = pagesTemp.get(i);
			if (pageItem.isDecoded) {
				pagesMemoryUsage -= pageItem.page.getMemoryUsage();
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
				pagesMemoryUsage += page.getMemoryUsage();
				for (PageDownloadListener listener : listeners)
					listener.pageAvailable(pageItem.pageNum);
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
		if (lastRequestedPage != number) {
			lastRequestedPage = number;
			updateRanks();
		}
		app.startProcessing();
		return getPage(number);
	}

	private void updateRanks() {
		int points = 0;
		for (PageItem item : pages) {
			int d = item.rank / 50;
			points += d;
			item.rank -= d;
		}
		int[] dd = { 1, -1 };
		int i = 0;
		while (points > 0) {
			for (int d : dd) {
				int p = points / 5 + 1;
				points -= p;
				int index = lastRequestedPage + d * (i % pages.size());
				if (index < 0 || index >= pages.size())
					continue;
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

	@Override
	public Uint8Array getData(String url, ReadyListener listener) {
		FileItem entry = fileCache.get(url);
		if (entry == null)
			fileCache.put(url, entry = new FileItem());
		if (!entry.downloadStarted) {
			downloadFile(url);
			entry.downloadStarted = true;
		}
		if (entry.data == null && listener != null)
			entry.listeners.add(listener);
		entry.lastUsed = System.currentTimeMillis();
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
					if (xhr.getStatus() == 200) {
						FileItem entry = fileCache.get(url);
						if (entry == null)
							fileCache.put(url, entry = new FileItem());
						entry.data = TypedArrays.createUint8Array(xhr.getResponseArrayBuffer());
						entry.lastUsed = System.currentTimeMillis();
						filesMemoryUsage += entry.data.byteLength();
						checkFilesMemory();
						app.startProcessing();
						fireReady(url);
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
	}

	protected void checkFilesMemory() {
		int limit = app.getFileCacheSize();
		if (filesMemoryUsage <= limit)
			return;
		ArrayList<FileItem> files = new ArrayList<>(fileCache.values());
		Collections.sort(files);
		while (filesMemoryUsage > limit && files.size() > 4) {
			FileItem item = files.remove(0);
			if (item.data == null)
				continue;
			filesMemoryUsage -= item.data.byteLength();
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

	public void addPageDownloadListener(PageDownloadListener listener) {
		listeners.add(listener);
	}

	public static interface PageDownloadListener {
		void pageAvailable(int pageNum);
	}
}
