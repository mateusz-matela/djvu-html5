package pl.djvuhtml5.client;

import java.io.IOException;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
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
	private Context2d drawingContext;

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {

		Canvas canvas = Canvas.createIfSupported();
		if (canvas == null) {
			// TODO
			throw new RuntimeException("Canvas not supported!");
		}
		canvas.setSize("1000px", "1000px");
		canvas.setCoordinateSpaceWidth(1000);
		canvas.setCoordinateSpaceHeight(1000);
		RootPanel.get("djvuContainer").add(canvas);

		drawingContext = canvas.getContext2d();
		drawingContext.fillRect(10, 10, 200, 200);

		url = "http://127.0.0.1:8888/sample/index.djvu";
		new CachedInputStream().init(url, new InputStateListener() {

			@Override
			public void inputReady() {
				parseDocument();
			}
		});
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
			int w = 1000, h = 1000;
			GRect segment = new GRect(0, page.getInfo().height - h, w, h);
			long start = System.currentTimeMillis();
			GMap map = page.getMap(segment, 1, null);
			byte[] data = map.getData();
			System.out.println("bitmap generation: " + (System.currentTimeMillis() - start));
			start = System.currentTimeMillis();
			ImageData imageData = drawingContext.createImageData(1000, 1000);
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int offset = 3 * ((h - y - 1) * w + x);
					imageData.setRedAt(data[offset + map.getRedOffset()] & 0xFF, x, y);
					imageData.setGreenAt(data[offset + map.getGreenOffset()] & 0xFF, x, y);
					imageData.setBlueAt(data[offset + map.getBlueOffset()] & 0xFF, x, y);
					imageData.setAlphaAt(255, x, y);
				}
			}
			System.out.println("bitmap copying: " + (System.currentTimeMillis() - start));
			drawingContext.putImageData(imageData, 0, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
