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

	private static class PageItem {
		public boolean isDecoded;
		public DjVuPage page;
	}

	private final Djvu_html5 app;

	private Document document;
	
	private final HashMap<String, FileItem> fileCache = new HashMap<>();

	private long filesMemoryUsage = 0;

	private List<PageItem> pages;

	private long pagesMemoryUsage = 0;

	private final ArrayList<PageDownloadListener> listeners = new ArrayList<>();

	private int lastRequestedPage = 0;

	public PageCache(final Djvu_html5 app, final String url) {
		this.app = app;

		getData(url, new ReadyListener() {
			
			@Override
			public void dataReady() {
				try {
					document = new Document();
					document.read(url);
					int pageCount = document.getDjVmDir().get_pages_num();
					pages = new ArrayList<>(pageCount);
					for (int i = 0; i < pageCount; i++)
						pages.add(new PageItem());

					app.getToolbar().setPageCount(pageCount);

					app.startProcessing();
				} catch (IOException e) {
					Logger.getGlobal().log(Level.SEVERE, "Could not parse document", e);
				}
			}
		});
	}

	boolean decodePage(boolean currentOnly) {
		for (int i = 0; i < (currentOnly ? 1 : pages.size()); i++) {
			final int pageIndex = (lastRequestedPage + i) % pages.size();
			PageItem pageItem = pages.get(pageIndex);
			if (pageItem.isDecoded)
				continue;
			DjVuPage page = pageItem.page;
			try {
				if (page == null) {
					page = pageItem.page = document.getPage(pageIndex);
					if (page == null)
						return false; // not downloaded yet
					GWT.log("Decoding page " + pageIndex);
				}
				if (page.decodeStep()) {
					pageItem.isDecoded = true;
					pagesMemoryUsage += page.getMemoryUsage();
					for (PageDownloadListener listener : listeners) {
						listener.pageAvailable(pageIndex);
					}
				}
			} catch (IOException e) {
				GWT.log("Error while decoding page " + pageIndex, e);
				return false;
			}
			return true;
		}
		return false;
	}

	public int getPageCount() {
		return pages.size();
	}

	public DjVuPage fetchPage(int number) {
		lastRequestedPage = number;
		return getPage(number);
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
					} else {
						GWT.log("Error downloading " + url);
						GWT.log("response status: " + xhr.getStatus() + " " + xhr.getStatusText());
					}
					fireReady(url);
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
