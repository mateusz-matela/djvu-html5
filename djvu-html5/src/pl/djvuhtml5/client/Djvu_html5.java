package pl.djvuhtml5.client;

import java.io.IOException;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.DjVmDir;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;
import com.lizardtech.djvu.InputStateListener;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Djvu_html5 implements EntryPoint {

	private String url;
	private Document document;
	private Canvas canvas;
	private Context2d drawingContext;
	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {

		RootPanel container = RootPanel.get("djvuContainer");
		container.add(prepareToolBar());
		container.add(prepareCanvas());

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
	}

	private Widget prepareToolBar() {
		HorizontalPanel toolBar = new HorizontalPanel();
		toolBar.setStyleName("toolbar");

		Button button = new Button();
		button.setStyleName("toolbarSquareButton");
		button.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				parsePages();
			}
		});
		toolBar.add(button);

		return toolBar;
	}

	private void parseDocument() {
		try {
			document = new Document();
			document.read(url);
			DjVmDir djVmDir = document.getDjVmDir();
			int filesCount = djVmDir.get_files_num();
			filesCount = 1;
			System.out.println("document read, found " + filesCount + " files.");
			final int[] countDown = { filesCount };
			InputStateListener listener = new InputStateListener() {

				@Override
				public void inputReady() {
					countDown[0]--;
					if (countDown[0] <= 0) {
						parsePages();
					}
				}
			};
			for (int i = 0; i < filesCount; i++) {
				document.get_data(i, listener);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void parsePages() {
		System.out.println("all pages downloaded!");
//		try {
//			for (int i = 0; i < document.size(); i++) {
//				DjVuPage page = document.getPage(i);
//				DjVuInfo info = page.getInfo();
//				System.out.println("page " + i + ": " + info.width + " x " + info.height);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		try {
			DjVuPage page = document.getPage(0);
			int w = Math.min(canvas.getCoordinateSpaceWidth(), page.getInfo().width);
			int h = Math.min(canvas.getCoordinateSpaceHeight(), page.getInfo().height);
			GRect segment = new GRect(0, page.getInfo().height - h, w, h);
			GMap map = page.getMap(segment, 1, null);
			byte[] data = map.getData();
			ImageData imageData = drawingContext.createImageData(w, h);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int offset = 3 * ((h - y - 1) * w + x);
					imageData.setRedAt(data[offset + map.getRedOffset()] & 0xFF, x, y);
					imageData.setGreenAt(data[offset + map.getGreenOffset()] & 0xFF, x, y);
					imageData.setBlueAt(data[offset + map.getBlueOffset()] & 0xFF, x, y);
					imageData.setAlphaAt(255, x, y);
				}
			}
			drawingContext.putImageData(imageData, 0, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
