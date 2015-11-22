package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GWT;
import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.InputStateListener;

public class PageCache {

	private final Djvu_html5 app;

	private final Document document;

	private final DjVuPage[] pages;

	private final DjVuPage[] uncodedPages;
	
	private final boolean[] downloadStarted;

	private final ArrayList<PageDownloadListener> listeners = new ArrayList<>();

	private int lastRequestedPage = 0;

	public PageCache(Djvu_html5 app, String url) throws IOException {
		this.app = app;
		this.document = new Document();
		document.read(url);
		int pageCount = document.getDjVmDir().get_pages_num();
		this.pages = new DjVuPage[pageCount];
		this.uncodedPages = new DjVuPage[pageCount];
		this.downloadStarted = new boolean[pageCount];

		startDownload();
	}

	private void startDownload() {
		if (document.getDjVmDir().is_bundled()) {
			app.startProcessing();
			return;
		}
		for (int i = 0; i < pages.length; i++) {
			final int pageIndex = (lastRequestedPage + i) % pages.length;
			if (downloadStarted[pageIndex])
				continue;
			downloadStarted[pageIndex] = true;
			try {
				CachedInputStream data = document.get_data(pageIndex, new InputStateListener() {

					@Override
					public void inputReady() {
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
		for (int i = 0; i < (currentOnly ? 1 : pages.length); i++) {
			final int pageIndex = (lastRequestedPage + i) % pages.length;
			if (pages[pageIndex] != null)
				continue;
			DjVuPage page = uncodedPages[pageIndex];
			try {
				if (page == null) {
					GWT.log("Decoding page " + pageIndex);
					page = uncodedPages[pageIndex] = document.getPage(pageIndex);
					if (page == null)
						return false; // not downloaded yet
				}
				if (page.decodeStep()) {
					pages[pageIndex] = page;
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
		return pages.length;
	}

	public DjVuPage fetchPage(int number) {
		lastRequestedPage = number;
		DjVuPage page = pages[number];
		return page;
	}

	public DjVuPage getPage(int number) {
		return pages[number];
	}

	public void addPageDownloadListener(PageDownloadListener listener) {
		listeners.add(listener);
	}

	public static interface PageDownloadListener {
		void pageAvailable(int pageNum);
	}
}
