package pl.djvuhtml5.client;

import java.util.MissingResourceException;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Djvu_html5 implements EntryPoint {

	public static enum Status {
		LOADING(0), ERROR(1);

		public final int imagePosition;

		Status(int imagePosition) {
			this.imagePosition = imagePosition;
		}
	}

	private static final String WELCOME_MESSAGE =
			"Starting djvu-html5 viewer v0.2.0-beta1 from https://github.com/mateusz-matela/djvu-html5";

	private static final String CONTEXT_GLOBAL_VARIABLE = "DJVU_CONTEXT";

	private static Djvu_html5 instance;

	private Dictionary context;

	private RootPanel container;

	private Canvas canvas;
	private SinglePageLayout pageLayout;
	private TextLayer textLayer;
	private Toolbar toolbar;
	private Scrollbar horizontalScrollbar;
	private Scrollbar verticalScrollbar;
	private TileCache tileCache;
	private PageCache pageCache;
	private BackgroundProcessor backgroundProcessor;
	private Label statusImage;
	private Status currentStatus;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		log(WELCOME_MESSAGE);

		Djvu_html5.instance = this;

		try {
			context = Dictionary.getDictionary(CONTEXT_GLOBAL_VARIABLE);
		} catch (MissingResourceException e) {
			// no custom config
		}

		container = RootPanel.get("djvuContainer");
		String url = Window.Location.getParameter("file");
		if (url == null || url.isEmpty())
			url = container.getElement().getAttribute("file");
		if (url == null || url.isEmpty())
			url = getIndexFile();
		if (url == null || url.isEmpty()) {
			GWT.log("ERROR: No djvu file defined");
			return;
		}

		pageCache = new PageCache(this, url);

		if (getTextLayerEnabled())
			container.add(textLayer = new TextLayer(this));

		container.add(prepareCanvas());
		container.add(toolbar = new Toolbar(this));
		container.add(horizontalScrollbar = new Scrollbar(true));
		container.add(verticalScrollbar = new Scrollbar(false));

		container.add(statusImage = new Label());
		statusImage.setStyleName("statusImage");

		int uiHideDelay = getUiHideDelay();
		if (uiHideDelay > 0) {
			UIHider uiHider = new UIHider(canvas, uiHideDelay);
			uiHider.addUIElement(toolbar, "toolbarHidden");
			uiHider.addUIElement(horizontalScrollbar, "scrollbarHidden");
			uiHider.addUIElement(verticalScrollbar, "scrollbarHidden");
		}

		tileCache = new TileCache(this);
		backgroundProcessor = new BackgroundProcessor(this);

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

	public RootPanel getContainer() {
		return container;
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

	public void setStatus(Status status) {
		if (status == currentStatus)
			return;
		statusImage.setVisible(status != null);
		if (status != null && currentStatus != Status.ERROR) {
			statusImage.getElement().getStyle().setProperty("backgroundPosition",
					-status.imagePosition * statusImage.getOffsetHeight() + "px 0px");
		}
		if (status == null || currentStatus != Status.ERROR)
			currentStatus = status;
	}

	public SinglePageLayout getPageLayout() {
		return pageLayout;
	}

	/** @return current text layer or {@code null} if disabled. */
	public TextLayer getTextLayer() {
		return textLayer;
	}

	public Toolbar getToolbar() {
		return toolbar;
	}

	public Scrollbar getHorizontalScrollbar() {
		return horizontalScrollbar;
	}

	public Scrollbar getVerticalScrollbar() {
		return verticalScrollbar;
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

	public int getFileCacheSize() {
		return getInt("fileCacheSize", 16 * 1024 * 1024);
	}

	public String getBackground() {
		return getString("background", "#666");
	}

	private int getUiHideDelay() {
		return getInt("uiHideDelay", 1500);
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

	public boolean getLocationUpdateEnabled() {
		return getBoolean("locationUpdateEnabled", true);
	}

	public boolean getTextLayerEnabled() {
		return getBoolean("textLayerEnabled", true);
	}

	public String getString(String key, String defaultValue) {
		if (instance.context == null || !instance.context.keySet().contains(key))
			return defaultValue;
		return instance.context.get(key);
	}

	private int getInt(String key, int defaultValue) {
		if (instance.context == null || !instance.context.keySet().contains(key))
			return defaultValue;
		String value = instance.context.get(key);
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private boolean getBoolean(String key, boolean defaultValue) {
		if (instance.context == null || !instance.context.keySet().contains(key))
			return defaultValue;
		String value = instance.context.get(key);
		return Boolean.valueOf(value);
	}

	public static native void log(String message) /*-{
		console.log(message);
	}-*/;
}
