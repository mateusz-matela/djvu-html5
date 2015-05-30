package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.InputStateListener;

public class PageCache {

	private final Document document;

	private final BackgroundProcessor backgroundProcessor;

	private final DjVuPage[] pages;
	
	private final boolean[] downloadStarted;

	private final ArrayList<Integer> needFetch = new ArrayList<>();

	private final ArrayList<PageDownloadListener> listeners = new ArrayList<>();

	public PageCache(Document document, BackgroundProcessor backgroundProcessor) {
		this.document = document;
		this.backgroundProcessor = backgroundProcessor;
		int pageCount = document.getDjVmDir().get_pages_num();
		this.pages = new DjVuPage[pageCount];
		this.downloadStarted = new boolean[pageCount];

		int parallelDownloads = DjvuContext.getParallelDownloads();
		for (int i = 0; i < parallelDownloads && i < pageCount; i++) {
			startDownload(i);
		}
	}

	private void startDownload(int i) {
		final int pageNum = i;
		try {
			document.get_data(i, new InputStateListener() {
				
				@Override
				public void inputReady() {
					downloadComplete(pageNum);
				}
			});
			downloadStarted[i] = true;
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not initiate download of page " + pageNum, e);
		}
	}

	protected void downloadComplete(final int pageNum) {
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			
			@Override
			public void execute() {
				decodePage(pageNum);
			}
		});

		// start new download if needed
		if (needFetch.isEmpty()) {
			for (int i = 0; i < downloadStarted.length; i++) {
				if (!downloadStarted[i]) {
					startDownload(i);
					return;
				}
			}
		}
		for (int i = needFetch.size() - 1; i >= 0; i--) {
			int pageToDownload = needFetch.remove(i);
			if (!downloadStarted[pageToDownload]) {
				startDownload(pageToDownload);
				return;
			}
		}
	}

	protected void decodePage(final int pageNum) {
		try {
			pages[pageNum] = document.getPage(pageNum);
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				
				@Override
				public void execute() {
					for (PageDownloadListener listener : listeners) {
						listener.pageAvailable(pageNum);
					}
				}
			});
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not decode page " + pageNum, e);
		}
	}

	public int getPageCount() {
		return pages.length;
	}

	public DjVuPage getPage(int number) {
		DjVuPage page = pages[number];
		if (page == null && !downloadStarted[number]) {
			needFetch.add(number);
		}
		return page ;
	}

	public void addPageDownloadListener(PageDownloadListener listener) {
		listeners.add(listener);
	}

	public static interface PageDownloadListener {
		void pageAvailable(int pageNum);
	}
}
