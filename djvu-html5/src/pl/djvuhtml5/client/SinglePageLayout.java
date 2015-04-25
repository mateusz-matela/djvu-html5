package pl.djvuhtml5.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import pl.djvuhtml5.client.TileCache.TileInfo;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.dom.client.CanvasElement;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;

public class SinglePageLayout {

	private float zoom = 1;

	private int page;

	private DjVuInfo pageInfo;

	private int centerX, centerY;

	private ArrayList<TileInfo> tileInfos = new ArrayList<>();

	private final Document document;

	private final TileCache tileCache;

	private final Canvas canvas;

	private String background;

	TileInfo tileInfoTemp = new TileInfo();

	public SinglePageLayout(Canvas canvas, Document document) {
		this.document = document;
		this.tileCache = new TileCache(document);
		this.canvas = canvas;

		this.background = DjvuContext.getBackground();

		setPage(0);
		zoomToFitPage();
	}

	public void setPage(int pageNum) {
		try {
			pageInfo = document.getPage(pageNum).getInfo();
			page = pageNum;
		} catch (Exception e) {
			Logger.getGlobal().log(Level.WARNING, "Could not read page info", e);
		}
	}

	public int getPage() {
		return page;
	}

	public void canvasResized() {
		
	}

	public void zoomToFitPage() {
		if (pageInfo == null)
			return;
		zoom = Math.min(1.0f * canvas.getCoordinateSpaceWidth() / pageInfo.width,
				1.0f * canvas.getCoordinateSpaceHeight() / pageInfo.height);

		centerX = (int)(pageInfo.width * zoom) / 2;
		centerY = (int)(pageInfo.height * zoom) / 2;
	}

	public void redraw() {
		Context2d graphics2d = canvas.getContext2d();
		int w = canvas.getCoordinateSpaceWidth(), h = canvas.getCoordinateSpaceHeight();
		if (pageInfo == null) {
			graphics2d.setFillStyle(background);
			graphics2d.fillRect(0, 0, w, h);
			return;
		}
		int pw = (int) (pageInfo.width * zoom), ph = (int) (pageInfo.height * zoom);
		if (pw < w || ph < h) {
			graphics2d.setFillStyle(background);
			graphics2d.fillRect(0, 0, w, h);
		}
		int tileSize = tileCache.tileSize;
		int fromX = Math.max(0, centerX - w / 2) / tileSize;
		int toX = Math.min(pw, centerX + w / 2) / tileSize;
		int fromY = Math.max(0, centerY - h / 2) / tileSize;
		int toY = Math.min(ph, centerY + h / 2) / tileSize;
		tileInfoTemp.page = page;
		tileInfoTemp.zoom = zoom;
		for (int  x = fromX; x <= toX; x++) {
			for (int y = fromY; y <= toY; y++) {
				tileInfoTemp.x = x;
				tileInfoTemp.y = y;
				ImageData tileImage = tileCache.getTileImage(tileInfoTemp);
				graphics2d.putImageData(tileImage, x * tileSize - centerX + w / 2, y * tileSize - centerY + h / 2);
			}
		}
		int pageEndX = pw - centerX + w / 2, pageEndY = ph - centerY + h / 2;
		graphics2d.fillRect(pageEndX, 0, w - pageEndX, h);
		graphics2d.fillRect(0, pageEndY, w, h - pageEndY);
	}
}
