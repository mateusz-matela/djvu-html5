package pl.djvuhtml5.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.lizardtech.djvu.URLInputStream;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Djvu_html5 implements EntryPoint {

	private static final String CONTEXT_GLOBAL_VARIABLE = "DJVU_CONTEXT";

	private static Djvu_html5 instance;

	private Dictionary context;

	private String url;
	private Canvas canvas;
	private SinglePageLayout pageLayout;
	private Toolbar toolbar;
	private TileCache tileCache;
	private PageCache pageCache;
	private BackgroundProcessor backgroundProcessor;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		Djvu_html5.instance = this;
		
		context = Dictionary.getDictionary(CONTEXT_GLOBAL_VARIABLE);

		RootPanel container = RootPanel.get("djvuContainer");
		container.add(prepareCanvas());
		container.add(toolbar = new Toolbar(this));

		url = Window.Location.getParameter("file");
		if (url == null || url.isEmpty())
			url = container.getElement().getAttribute("file");
		if (url == null || url.isEmpty())
			url = getIndexFile();
		if (url ==  null || url.isEmpty()) {
			GWT.log("ERROR: No djvu file defined");
			return;
		}

		pageCache = new PageCache(this, url);
		URLInputStream.dataSource = pageCache;

		backgroundProcessor = new BackgroundProcessor(this);
		
		tileCache = new TileCache(this);
		pageLayout = new SinglePageLayout(this);
		toolbar.setPageLayout(pageLayout);
	}

	private Widget prepareCanvas() {
		canvas = Canvas.createIfSupported();
		if (canvas == null) {
			// TODO
			throw new RuntimeException("Canvas not supported!");
		}
		canvas.setTabIndex(0);

		final SimplePanel panel = new SimplePanel(canvas);
		panel.setStyleName("content");

		Window.addResizeHandler(new ResizeHandler() {
			
			@Override
			public void onResize(ResizeEvent event) {
				resizeCanvas();
			}
		});
		Scheduler.get().scheduleFinally(new Scheduler.ScheduledCommand() {
			
			@Override
			public void execute() {
				resizeCanvas();
			}
		});
		return panel;
	}

	private void resizeCanvas() {
		Widget panel = canvas.getParent();
		int width = panel.getOffsetWidth();
		int height = panel.getOffsetHeight();
		canvas.setWidth(width + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceHeight(height);
		if (pageLayout != null)
			pageLayout.canvasResized();
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public TileCache getTileCache() {
		return tileCache;
	}

	public PageCache getPageCache() {
		return pageCache;
	}

	public void startProcessing() {
		backgroundProcessor.start();
	}

	public void interruptProcessing() {
		backgroundProcessor.interrupt();
	}

	public Toolbar getToolbar() {
		return toolbar;
	}

	public String getIndexFile() {
		return getString("file", null);
	}

	public int getTileSize() {
		return getInt("tileSize", 512);
	}

	public int getTileCacheSize() {
		return getInt("tileCacheSize", 256);
	}

	public int getPageCacheSize() {
		return getInt("pageCacheSize", 128 * 1024 * 1024);
	}

	public String getBackground() {
		return getString("background", "#666");
	}

	public int getPageMargin() {
		return getInt("pageMargin", 8);
	}

	public int getScreenDPI() {
		return getInt("screenDPI", 96);
	}

	public int getMaxZoom() {
		return getInt("maxZoom", 10000);
	}

	public String getString(String key, String defaultValue) {
		if (!instance.context.keySet().contains(key))
			return defaultValue;
		return instance.context.get(key);
	}

	private int getInt(String key, int defaultValue) {
		if (!instance.context.keySet().contains(key))
			return defaultValue;
		String value = instance.context.get(key);
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
