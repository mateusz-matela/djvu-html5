package pl.djvuhtml5.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Window;

import pl.djvuhtml5.client.Djvu_html5.Status;

public class BackgroundProcessor implements RepeatingCommand {

	private final static int LAZY_MODE_INTERVAL = 400;

	private final PageCache pageCache;
	private final TileCache tileCache;

	private boolean isRunning;

	private boolean isInterruptScheduled;

	private long lastInterrupt;

	private final Djvu_html5 app;

	/**
	 * IE hangs while RepeatingCommand execution runs, so this mode interrupts from time to time
	 * to allow GUI redrawing.
	 */
	private final boolean lazyMode;

	public BackgroundProcessor(Djvu_html5 app) {
		this.app = app;

		pageCache = new PageCache(app, DjvuContext.getUrl());
		tileCache = new TileCache(app, pageCache);

		String userAgent = Window.Navigator.getUserAgent();
		lazyMode = userAgent.contains("msie") || userAgent.contains("trident");
	}

	public void start() {
		if (!isRunning) {
			Scheduler.get().scheduleIncremental(this);
			lastInterrupt = System.currentTimeMillis();
			isInterruptScheduled = false;
		}
		isRunning = true;
	}

	@Override
	public boolean execute() {
		try {
			return doExecute();
		} catch (Exception e) {
			app.setStatus(Status.ERROR);
			GWT.log("background processing failed", e);
			return isRunning = false;
		}
	}

	private boolean doExecute() {
		boolean didSomething = pageCache.decodeCurrentPage()
				|| tileCache.prefetchPreviews(false)
				|| tileCache.prefetchCurrentView(0)
				|| pageCache.decodeTexts()
				|| tileCache.prefetchPreviews(true)
				|| pageCache.decodePages()
				|| tileCache.prefetchAdjacent(0)
				|| tileCache.prefetchCurrentView(1)
				|| tileCache.prefetchCurrentView(-1)
				|| tileCache.prefetchAdjacent(1)
				|| tileCache.prefetchAdjacent(-1);
		if (didSomething) {
			if (lazyMode && lastInterrupt + LAZY_MODE_INTERVAL < System.currentTimeMillis()) {
				isInterruptScheduled = true;
			}
			if (isInterruptScheduled) {
				isInterruptScheduled = false;
				lastInterrupt = System.currentTimeMillis();
				Scheduler.get().scheduleFixedDelay(() -> {
					Scheduler.get().scheduleIncremental(BackgroundProcessor.this);
					return false;
				}, 50);
				return false;
			}
			return true;
		}
		return isRunning = false;
	}

	public void interrupt() {
		if (!isRunning)
			return;
		isInterruptScheduled = true;
	}

}
