package pl.djvuhtml5.client;

import static pl.djvuhtml5.client.TileCache.toZoom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;
import com.lizardtech.djvu.text.DjVuText;

import pl.djvuhtml5.client.TileCache.TileInfo;

public class DataStore {

	private List<DjVuInfo> pageInfos;
	private List<DjVuText> pageTexts;

	private Map<TileInfo, CanvasElement> tiles = new HashMap<>();
	private final CanvasElement missingTileImage;

	private final List<Consumer<Integer>> pageCountListeners = new ArrayList<>();
	private final List<Consumer<Integer>> textListeners = new ArrayList<>();
	private final List<Consumer<Integer>> infoListeners = new ArrayList<>();
	private final List<Consumer<Integer>> tileListeners = new ArrayList<>();

	private final GRect tempRect = new GRect();
	private final TileInfo tempTI = new TileInfo();
	private final int tileSize;
	private CanvasElement bufferCanvas;
	private ImageData bufferImageData;

	public DataStore() {
		missingTileImage = prepareMissingTileImage();
		this.tileSize = DjvuContext.getTileSize();
		bufferCanvas = createImage(tileSize, tileSize);
	}

	private CanvasElement prepareMissingTileImage() {
		int tileSize = DjvuContext.getTileSize();
		CanvasElement canvas = createImage(tileSize, tileSize);
		Context2d context2d = canvas.getContext2d();
		context2d.setFillStyle("white");
		context2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		Image image = new Image();
		final ImageElement imageElement = image.getElement().cast();
		imageElement.getStyle().setProperty("visibility", "hidden");
		Event.setEventListener(imageElement, event -> {
			if (Event.ONLOAD == event.getTypeInt()) {
				missingTileImage.getContext2d().drawImage(imageElement, 0, 0);
				RootPanel.get().getElement().removeChild(imageElement);
			}
		});
		RootPanel.get().getElement().appendChild(imageElement);
		image.setUrl(getBlankImageUrl());
		return canvas;
	}

	private String getBlankImageUrl() {
		Element element = new Label().getElement().cast();
		element.addClassName("blankImage");
		RootPanel.get().getElement().appendChild(element);
		try {
			String url = Djvu_html5.getComputedStyleProperty(element, "background-image");
			url = url.replaceAll("^url\\(['\"]?(.*)['\"]\\)$", "$1");
			return url;
		} finally {
			RootPanel.get().getElement().removeChild(element);
		}
	}

	public void addPageCountListener(Consumer<Integer> listener) {
		pageCountListeners.add(listener);
	}

	public void addTextListener(Consumer<Integer> listener) {
		textListeners.add(listener);
	}

	public void addInfoListener(Consumer<Integer> listener) {
		infoListeners.add(listener);
	}

	public void addTileListener(Consumer<Integer> listener) {
		tileListeners.add(listener);
	}

	public int getPageCount() {
		return pageInfos != null ? pageInfos.size() : 1;
	}

	public void setPageCount(int pageCount) {
		pageInfos = Arrays.asList(new DjVuInfo[pageCount]);
		pageTexts = Arrays.asList(new DjVuText[pageCount]);
		for (Consumer<Integer> l : pageCountListeners)
			l.accept(pageCount);
	}

	public DjVuInfo getPageInfo(int pageNum) {
		return pageInfos != null ? pageInfos.get(pageNum) : null;
	}

	public void setPageInfo(int pageNum, DjVuInfo info) {
		pageInfos.set(pageNum, info);
		for (Consumer<Integer> l : infoListeners)
			l.accept(pageNum);
	}

	public DjVuText getText(int pageNum) {
		return pageTexts != null ? pageTexts.get(pageNum) : null;
	}

	public void setText(int pageNum, DjVuText text) {
		pageTexts.set(pageNum, text);
		for (Consumer<Integer> l : textListeners)
			l.accept(pageNum);
	}

	public CanvasElement[][] getTileImages(int pageNum, int subsample, GRect range, CanvasElement[][] reuse) {
		CanvasElement[][] result = reuse;
		int w = range.width() + 1, h = range.height() + 1;
		if (reuse == null || reuse.length != h || reuse[0].length != w) {
			result = new CanvasElement[h][w];
		}

		tempTI.page = pageNum;
		tempTI.subsample = subsample;
		for (int y = range.ymin; y <= range.ymax; y++)
			for (int x = range.xmin; x <= range.xmax; x++)
				result[y - range.ymin][x - range.xmin] = getTileImage(tempTI.setXY(x, y));

		return result;
	}

	public void releaseTileImages(List<TileInfo> tiles) {
		for (TileInfo tile : tiles)
			this.tiles.remove(tile);
	}

	private CanvasElement getTileImage(TileInfo tileInfo) {
		CanvasElement tileImage = tiles.get(tileInfo);
		if (tileImage != null)
			return tileImage;

		DjVuInfo pageInfo = getPageInfo(tileInfo.page);
		int tileSize = DjvuContext.getTileSize();

		// fill with rescaled other tiles
		ArrayList<TileInfo> fetched = new ArrayList<>();
		tileInfo.getPageRect(tempRect, tileSize, pageInfo);
		GRect tempRect2 = new GRect();
		for (Entry<TileInfo, CanvasElement> entry : tiles.entrySet()) {
			TileInfo ti = entry.getKey();
			if (ti.page == tileInfo.page) {
				ti.getPageRect(tempRect2, tileSize, pageInfo);
				if (tempRect2.intersect(tempRect2, tempRect))
					fetched.add(ti);
			}
		}
		if (fetched.isEmpty())
			return missingTileImage;

		Collections.sort(fetched, (ti1, ti2) -> ti2.subsample - ti1.subsample);
		tileInfo.getScreenRect(tempRect, tileSize, pageInfo);
		tileImage = createImage(tempRect.width(), tempRect.height());
		tiles.put(new TileInfo(tileInfo), tileImage);
		Context2d context = tileImage.getContext2d();

		tileInfo.getScreenRect(tempRect, tileSize, pageInfo);
		double zoom = toZoom(tileInfo.subsample);
		for (TileInfo ti : fetched) {
			context.save();
			double scale = zoom / toZoom(ti.subsample);
			ti.getScreenRect(tempRect2, tileSize, pageInfo);
			context.translate(-tempRect.xmin, -tempRect.ymin);
			context.scale(scale, scale);
			context.translate(tempRect2.xmin, tempRect2.ymin);
			context.drawImage(tiles.get(ti), 0, 0);
			context.restore();
		}
		return tileImage;
	}

	public void setTile(TileInfo tileInfo, GMap bufferGMap) {
		if (bufferImageData == null || bufferImageData.getWidth() != bufferGMap.getDataWidth()
				|| bufferImageData.getHeight() != bufferGMap.getDataHeight()) {
			bufferImageData = bufferCanvas.getContext2d()
					.createImageData(bufferGMap.getDataWidth(), bufferGMap.getDataHeight());
		}
		Uint8Array imageArray = bufferImageData.getData().cast();
		imageArray.set(bufferGMap.getImageData());
		bufferCanvas.getContext2d().putImageData(bufferImageData, -bufferGMap.getBorder(), 0);

		CanvasElement tile = tiles.get(tileInfo);
		if (tile == null) {
			tile = createImage(bufferGMap.getDataWidth() - bufferGMap.getBorder(), bufferGMap.getDataHeight());
			tiles.put(new TileInfo(tileInfo), tile);
		}
		Context2d c = tile.getContext2d();
		c.setFillStyle("white");
		c.fillRect(0, 0, tileSize, tileSize);
		c.drawImage(bufferCanvas, 0, 0);
		for (Consumer<Integer> listener : tileListeners)
			listener.accept(tileInfo.page);
	}

	public static CanvasElement createImage(int width, int height) {
		Canvas canvas = Canvas.createIfSupported();
		canvas.setWidth(width + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceHeight(height);
		return canvas.getCanvasElement();
	}
}
