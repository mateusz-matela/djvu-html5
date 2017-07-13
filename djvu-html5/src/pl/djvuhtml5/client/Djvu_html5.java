package pl.djvuhtml5.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import pl.djvuhtml5.client.ui.Scrollbar;
import pl.djvuhtml5.client.ui.SinglePageLayout;
import pl.djvuhtml5.client.ui.TextLayer;
import pl.djvuhtml5.client.ui.Toolbar;
import pl.djvuhtml5.client.ui.UIHider;

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
			"Starting djvu-html5 viewer v0.2.3 from https://github.com/mateusz-matela/djvu-html5";

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
	private DataStore dataStore;
	private BackgroundProcessor backgroundProcessor;
	private Label statusImage;
	private Status currentStatus;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		log(WELCOME_MESSAGE);

		container = RootPanel.get("djvuContainer");
		String url = Window.Location.getParameter("file");
		if (url == null || url.isEmpty())
			url = container.getElement().getAttribute("file");
		if (url == null || url.isEmpty())
			url = DjvuContext.getIndexFile();
		if (url == null || url.isEmpty()) {
			GWT.log("ERROR: No djvu file defined");
			return;
		}
		DjvuContext.setUrl(url);

		dataStore = new DataStore();

		if (DjvuContext.getTextLayerEnabled())
			container.add(textLayer = new TextLayer(this));

		container.add(prepareCanvas());
		container.add(toolbar = new Toolbar());
		container.add(horizontalScrollbar = new Scrollbar(true));
		container.add(verticalScrollbar = new Scrollbar(false));

		container.add(statusImage = new Label());
		statusImage.setStyleName("statusImage");

		int uiHideDelay = DjvuContext.getUiHideDelay();
		if (uiHideDelay > 0) {
			UIHider uiHider = new UIHider(uiHideDelay, canvas, textLayer);
			uiHider.addUIElement(toolbar, "toolbarHidden");
			uiHider.addUIElement(horizontalScrollbar, "scrollbarHidden");
			uiHider.addUIElement(verticalScrollbar, "scrollbarHidden");
		}

		backgroundProcessor = new BackgroundProcessor(this);

		pageLayout = new SinglePageLayout(this);
		toolbar.setPageLayout(pageLayout);

		BackgroundWorker.test();
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

		Window.addResizeHandler(e -> resizeCanvas());
		Scheduler.get().scheduleFinally(() -> resizeCanvas());
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

	public DataStore getDataStore() {
		return dataStore;
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

	public static native void log(String message) /*-{
		console.log(message);
	}-*/;

	public static native String getComputedStyleProperty(Element element, String property) /*-{
		var cs = $doc.defaultView.getComputedStyle(element, null);
		return cs.getPropertyValue(property);
	}-*/;
}
