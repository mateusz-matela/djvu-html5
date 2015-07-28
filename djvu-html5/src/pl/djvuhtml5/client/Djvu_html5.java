package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
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
	private Toolbar toolbar;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		
		DjvuContext.init(drawingContext);

		RootPanel container = RootPanel.get("djvuContainer");
		container.add(prepareCanvas());
		container.add(toolbar = new Toolbar(canvas));

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
				initDocumentDisplay();
			}
		});
	}

	private Widget prepareCanvas() {
		canvas = Canvas.createIfSupported();
		if (canvas == null) {
			// TODO
			throw new RuntimeException("Canvas not supported!");
		}
		canvas.setTabIndex(0);
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

	private void initDocumentDisplay() {
		try {
			document = new Document();
			document.read(url);

			pageLayout = new SinglePageLayout(canvas, toolbar, document);
			toolbar.setPageLayout(pageLayout);
		} catch (IOException e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not parse document", e);
		}
	}

}
