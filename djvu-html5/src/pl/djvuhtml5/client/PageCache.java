package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.InputStateListener;

public class PageCache implements BackgroundProcessor.Operation {

	private final Document document;

	private final BackgroundProcessor backgroundProcessor;

	private final DjVuPage[] pages;

	private final DjVuPage[] uncodedPages;
	
	private final boolean[] downloadStarted;

	private final ArrayList<PageDownloadListener> listeners = new ArrayList<>();

	private int lastRequestedPage = 0;

	public PageCache(Document document, BackgroundProcessor backgroundProcessor) {
		this.document = document;
		this.backgroundProcessor = backgroundProcessor;
		backgroundProcessor.addOperation(this);
		int pageCount = document.getDjVmDir().get_pages_num();
		this.pages = new DjVuPage[pageCount];
		this.uncodedPages = new DjVuPage[pageCount];
		this.downloadStarted = new boolean[pageCount];

		startDownload();
	}

	private void startDownload() {
		if (document.getDjVmDir().is_bundled()) {
			backgroundProcessor.start();
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
						backgroundProcessor.start();
						startDownload();
					}
				});
				if (data == null || !data.isReady())
					return;
				backgroundProcessor.start();
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE, "Could not initiate download of page " + pageIndex, e);
			}
		}
	}

	@Override
	public boolean doOperation(int priority) {
		switch (priority) {
		case 0:
			return decodePage(true);
		case 3:
			return decodePage(false);
		default:
			return false;
		}
	}

	private boolean decodePage(boolean currentOnly) {
		for (int i = 0; i < (currentOnly ? 1 : pages.length); i++) {
			final int pageIndex = (lastRequestedPage + i) % pages.length;
			if (pages[pageIndex] != null)
				continue;
			DjVuPage page = uncodedPages[pageIndex];
			try {
				if (page == null) {
					GWT.log("Decoding page " + pageIndex);
					page = uncodedPages[pageIndex] = document.getPage(pageIndex);
				}
				if (page.decodeStep()) {
					pages[pageIndex] = page;
					Scheduler.get().scheduleDeferred(new ScheduledCommand() {
						
						@Override
						public void execute() {
							for (PageDownloadListener listener : listeners) {
								listener.pageAvailable(pageIndex);
							}
						}
					});
				}
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE, "Error while decoding page " + pageIndex, e);
				return false;
			}
			return true;
		}
		if (!currentOnly)
			backgroundProcessor.removeOperation(this); // everything decoded
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
