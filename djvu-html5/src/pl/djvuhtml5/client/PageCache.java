package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.ArrayList;
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
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;

public class PageCache implements DataSource {

	private static class FileItem {
		public Uint8Array data;
		public final List<ReadyListener> listeners = new ArrayList<>();
	}

	private static class PageItem {
		public boolean downloadStarted;
		public boolean isDecoded;
		public DjVuPage page;
	}

	private final Djvu_html5 app;

	private Document document;
	
	private final HashMap<String, FileItem> fileCache = new HashMap<>();

	private List<PageItem> pages;

	private final ArrayList<PageDownloadListener> listeners = new ArrayList<>();

	private int lastRequestedPage = 0;

	private long memoryUsage = 0;

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

					startDownload();
				} catch (IOException e) {
					Logger.getGlobal().log(Level.SEVERE, "Could not parse document", e);
				}
			}
		});
	}

	private void startDownload() {
		if (document.getDjVmDir().is_bundled()) {
			app.startProcessing();
			return;
		}
		for (int i = 0; i < pages.size(); i++) {
			final int pageIndex = (lastRequestedPage + i) % pages.size();
			PageItem pageItem = pages.get(pageIndex);
			if (pageItem.downloadStarted)
				continue;
			pageItem.downloadStarted = true;
			try {
				CachedInputStream data = document.get_data(pageIndex, new ReadyListener() {

					@Override
					public void dataReady() {
						app.startProcessing();
						startDownload();
					}
				});
				if (data == null || !data.isReady())
					return;
				app.startProcessing();
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE, "Could not initiate download of page " + pageIndex, e);
			}
		}
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
					GWT.log("Decoding page " + pageIndex);
					page = pageItem.page = document.getPage(pageIndex);
					if (page == null)
						return false; // not downloaded yet
				}
				if (page.decodeStep()) {
					pageItem.isDecoded = true;
					memoryUsage += page.getMemoryUsage();
					GWT.log("Memory usage: " + memoryUsage);
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
		if (entry.data == null && listener != null) {
			downloadFile(url);
			entry.listeners.add(listener);
		}
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
						memoryUsage += entry.data.length();
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
