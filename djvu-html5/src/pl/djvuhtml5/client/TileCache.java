package pl.djvuhtml5.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.user.client.ui.Image;
import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;

public class TileCache {

	public final static int MAX_SUBSAMPLE = 12;

	public final int tileSize;

	private final int tileCacheSize;

	private final Context2d bufferContext;

	private final PageCache pageCache;

	private HashMap<TileInfo, CachedItem> cache = new HashMap<>();

	private final Fetcher fetcher;

	private ArrayList<TileCacheListener> listeners = new ArrayList<>();

	private final GRect tempRect = new GRect();

	public TileCache(PageCache pageCache) {
		this.pageCache = pageCache;
		this.tileCacheSize = DjvuContext.getTileCacheSize();
		this.tileSize = DjvuContext.getTileSize();

		Canvas buffer = Canvas.createIfSupported();
		buffer.setWidth(tileSize + "px");
		buffer.setCoordinateSpaceWidth(tileSize);
		buffer.setHeight(tileSize + "px");
		buffer.setCoordinateSpaceHeight(tileSize);
		bufferContext = buffer.getContext2d();

		fetcher = new Fetcher();
	}

	public static int toSubsample(double zoom) {
		int subsample = (int) Math.floor(1 / zoom);
		subsample = Math.max(1, Math.min(MAX_SUBSAMPLE, subsample));
		return subsample;
	}

	public static double toZoom(int subsample) {
		double zoom = 1.0 / subsample;
		return zoom;
	}

	public Image getTileImage(TileInfo tileInfo) {
		CachedItem cachedItem = cache.get(tileInfo);
		if (cachedItem == null) {
			tileInfo.getRect(tempRect, tileSize, pageCache.getPage(tileInfo.page).getInfo());
			CanvasElement canvas = bufferContext.getCanvas();
			canvas.setWidth(tempRect.width());
			canvas.setHeight(tempRect.height());
			bufferContext.setFillStyle("white");
			bufferContext.fillRect(0, 0, tempRect.width(), tempRect.height());
			bufferContext.setFillStyle("#999");
			final int count = tileSize / 16;
			for (int x = 0; x < count; x++)
				for (int y = 0; y < count; y++)
					if ((x + y) % 2 == 1) {
						int x1 = tileSize * x / count, x2 = tileSize * (x + 1) / count;
						int y1 = tileSize * y / count, y2 = tileSize * (y + 1) / count;
						bufferContext.fillRect(x1, y1, x2 - x1, y2 - y1);
					}

			//TODO fill with rescaled other tiles

			cachedItem = new CachedItem();
			cachedItem.image = new Image(canvas.toDataUrl());
			tileInfo = new TileInfo(tileInfo);
			cache.put(tileInfo, cachedItem);
			fetcher.fetch(tileInfo);
		}
		cachedItem.lastUsed = System.currentTimeMillis();
		return cachedItem.image;
	}

	public void addTileCacheListener(TileCacheListener listener) {
		listeners.add(listener);
	}

	private class Fetcher implements RepeatingCommand {
		private boolean isRunning = false;

		private final ArrayList<TileInfo> tilesToFetch = new ArrayList<>();

		private final ImageData bufferData = bufferContext.createImageData(tileSize, tileSize);

		private GMap bufferGMap;

		public void fetch(TileInfo tileInfo) {
			tilesToFetch.add(tileInfo);
			if (!isRunning)
				Scheduler.get().scheduleIncremental(this);
			isRunning = true;
		}

		@Override
		public boolean execute() {
			for (int i = tilesToFetch.size() - 1; i >= 0; i--) {
				final TileInfo tileInfo = tilesToFetch.get(i);
				CachedItem cachedItem = cache.get(tileInfo);
				if (cachedItem != null && cachedItem.isFetched)
					continue;
				DjVuPage page = pageCache.getPage(tileInfo.page);
				if (page == null)
					continue;
				tilesToFetch.remove(i);
				if (cachedItem == null) {
					cachedItem = new CachedItem();
					cache.put(tileInfo, cachedItem);
				}
		
				tileInfo.getRect(tempRect, tileSize, page.getInfo());
				int w = tempRect.width(), h = tempRect.height();
				bufferGMap = page.getMap(tempRect, tileInfo.subsample, bufferGMap);
				int r = bufferGMap.getRedOffset(), g = bufferGMap.getGreenOffset(), b = bufferGMap.getBlueOffset();
				byte[] data = bufferGMap.getData();
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						int offset = 3 * ((h - y - 1) * w + x);
						bufferData.setRedAt(data[offset + r] & 0xFF, x, y);
						bufferData.setGreenAt(data[offset + g] & 0xFF, x, y);
						bufferData.setBlueAt(data[offset + b] & 0xFF, x, y);
						bufferData.setAlphaAt(0xFF, x, y);
					}
				}
				CanvasElement canvas = bufferContext.getCanvas();
				canvas.setWidth(w);
				canvas.setHeight(h);
				bufferContext.putImageData(bufferData, 0, 0);
				cachedItem.image = new Image(canvas.toDataUrl());
		
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					
					@Override
					public void execute() {
						for (TileCacheListener listener : listeners)
							listener.tileAvailable(tileInfo);
					}
				});
				return true;
			}
			isRunning = false;
			return false;
		}
	};

	public static final class TileInfo {
		public int page;
		public int subsample;
		public int x;
		public int y;

		public TileInfo(int page, int subsample, int x, int y) {
			this.page = page;
			this.subsample = subsample;
			this.x = x;
			this.y = y;
		}

		private void getRect(GRect rect, int tileSize, DjVuInfo info) {
			int pw = (info.width + subsample - 1) / subsample, ph = (info.height + subsample - 1) / subsample;
			rect.xmin = x * tileSize;
			rect.xmax = Math.min((x + 1) * tileSize, pw);
			rect.ymin = Math.max(ph - (y + 1) * tileSize, 0);
			rect.ymax = ph - y * tileSize;
		}

		public TileInfo(TileInfo toCopy) {
			this(toCopy.page, toCopy.subsample, toCopy.x, toCopy.y);
		}

		public TileInfo() {
			// nothing to do
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + page;
			result = prime * result + x;
			result = prime * result + y;
			result = prime * result + subsample;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TileInfo other = (TileInfo) obj;
			if (page != other.page)
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			if (subsample != other.subsample)
				return false;
			return true;
		}
	}

	public static interface TileCacheListener {
		void tileAvailable(TileInfo tileInfo);
	}

	private static final class CachedItem {
		public Image image;
		public long lastUsed;
		public boolean isFetched;
	}
}
