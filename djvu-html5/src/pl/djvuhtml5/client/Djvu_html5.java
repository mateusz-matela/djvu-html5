package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.InputStateListener;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Djvu_html5 implements EntryPoint {

	private String url;
	private Document document;
	private Canvas canvas;
	private Context2d drawingContext;
	private SinglePageLayout pageLayout;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {

		RootPanel container = RootPanel.get("djvuContainer");
		container.add(prepareCanvas());
		container.add(prepareToolBar());

		DjvuContext.init(drawingContext);

		url = Window.Location.getParameter("file");
		if (url == null || url.isEmpty())
			url = container.getElement().getAttribute("file");
		if (url == null || url.isEmpty())
			url = DjvuContext.getIndexFile();
		if (url ==  null || url.isEmpty()) {
			DjvuContext.printError("No djvu file defined");
			return;
		}

		new CachedInputStream().init(url, new InputStateListener() {

			@Override
			public void inputReady() {
				parseDocument();
			}
		});
	}

	private Widget prepareCanvas() {
		canvas = Canvas.createIfSupported();
		if (canvas == null) {
			// TODO
			throw new RuntimeException("Canvas not supported!");
		}
		drawingContext = canvas.getContext2d();

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

	private Widget prepareToolBar() {
		final FlowPanel toolBar = new FlowPanel();
		toolBar.setStyleName("toolbar");

		new ToolBarHandler(toolBar, canvas);

		Button button = new Button();
		button.setStyleName("toolbarSquareButton");
		button.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				toolBar.addStyleName("toolbarHidden");
			}
		});
		toolBar.add(button);

		button = new Button();
		button.setStyleName("toolbarSquareButton");
		button.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				pageLayout.redraw();
			}
		});
		toolBar.add(button);

		return toolBar;
	}

	private void parseDocument() {
		try {
			document = new Document();

			document.read(url);
			pageLayout = new SinglePageLayout(canvas, document);
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not parse document", e);
		}
	}

	private class ToolBarHandler implements MouseMoveHandler, MouseOverHandler, MouseOutHandler {

		private static final int TOOLBAR_HIDE_DELAY = 1500;

		private boolean isMouseOverToolbar = false;

		private final Widget toolBar;

		private final Timer timer = new Timer() {
			
			@Override
			public void run() {
				if (!isMouseOverToolbar)
					toolBar.addStyleName("toolbarHidden");
			}
		};

		public ToolBarHandler(Widget toolbar, Canvas canvas) {
			this.toolBar = toolbar;
			canvas.addMouseMoveHandler(this);
			toolBar.addDomHandler(this, MouseOverEvent.getType());
			toolBar.addDomHandler(this, MouseOutEvent.getType());
		}

		@Override
		public void onMouseMove(MouseMoveEvent event) {
			toolBar.removeStyleName("toolbarHidden");
			timer.cancel();
			timer.schedule(TOOLBAR_HIDE_DELAY);
		}

		@Override
		public void onMouseOver(MouseOverEvent event) {
			isMouseOverToolbar = true;
		}

		@Override
		public void onMouseOut(MouseOutEvent event) {
			isMouseOverToolbar = false;
		}
		
	}
}
