package pl.djvuhtml5.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Window;

public class BackgroundProcessor implements RepeatingCommand {

	private final static int LAZY_MODE_INTERVAL = 400;

	private final PageDecoder pageDecoder;
	private final TileRenderer tileRenderer;

	private boolean isRunning;

	private boolean isInterruptScheduled;

	private long lastInterrupt;

	private final ProcessingContext context;

	/**
	 * IE hangs while RepeatingCommand execution runs, so this mode interrupts from time to time
	 * to allow GUI redrawing.
	 */
	private final boolean lazyMode;

	public BackgroundProcessor(ProcessingContext context) {
		this.context = context;

		pageDecoder = new PageDecoder(context, DjvuContext.getUrl());
		tileRenderer = new TileRenderer(context, pageDecoder);

		String userAgent = Window.Navigator.getUserAgent();
		lazyMode = userAgent.contains("msie") || userAgent.contains("trident");
	}

	public void start() {
		if (pageDecoder.getPageCount() == 0)
			return;
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
			context.setStatus(ProcessingContext.STATUS_ERROR);
			Djvu_html5.log("background processing failed: " + e);
			return isRunning = false;
		}
	}

	private boolean doExecute() {
		boolean didSomething = pageDecoder.decodeCurrentPage()
				|| tileRenderer.prefetchPreviews(false)
				|| tileRenderer.prefetchCurrentView(0)
				|| pageDecoder.decodeTexts()
				|| tileRenderer.prefetchPreviews(true)
				|| pageDecoder.decodePages()
				|| tileRenderer.prefetchAdjacent(0)
				|| tileRenderer.prefetchCurrentView(1)
				|| tileRenderer.prefetchCurrentView(-1)
				|| tileRenderer.prefetchAdjacent(1)
				|| tileRenderer.prefetchAdjacent(-1);
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
