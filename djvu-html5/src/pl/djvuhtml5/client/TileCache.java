package pl.djvuhtml5.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pl.djvuhtml5.client.PageCache.PageDownloadListener;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.ImageElement;
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

	private final HashMap<TileInfo, CachedItem> cache = new HashMap<>();

	/** All tiles of max subsample are stored here and will never be thrown away */
	private final HashMap<TileInfo, CachedItem> smallCache = new HashMap<>();

	private final Fetcher fetcher;

	private ArrayList<TileCacheListener> listeners = new ArrayList<>();

	private final GRect tempRect = new GRect();
	private final TileInfo tempTI = new TileInfo();

	private int lastPageNum, lastSubsample;
	private final GRect lastRange = new GRect();

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
		pageCache.addPageDownloadListener(fetcher);

		GMap.imageContext = bufferContext;
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

	public Image[][] getTileImages(int pageNum, int subsample, GRect range, Image[][] reuse) {
		Image[][] result = reuse;
		int w = range.width() + 1, h = range.height() + 1;
		if (reuse == null || reuse.length != h || reuse[0].length != w) {
			result = new Image[h][w];
		}

		if (pageNum != lastPageNum || subsample != lastSubsample || !lastRange.equals(range)) {
			lastPageNum = pageNum;
			lastSubsample = subsample;
			lastRange.clear();
			lastRange.recthull(lastRange, range);
			fetcher.fetch();
		}

		tempTI.page = pageNum;
		tempTI.subsample = subsample;
		for (int y = range.ymin; y <= range.ymax; y++)
			for (int x = range.xmin; x <= range.xmax; x++)
				result[y - range.ymin][x - range.xmin] = getTileImage(tempTI.setXY(x, y));

		return result;
	}

	private Image getTileImage(TileInfo tileInfo) {
		CachedItem cachedItem = getItem(tileInfo);
		if (cachedItem == null) {
			DjVuInfo pageInfo = pageCache.getPage(tileInfo.page).getInfo();
			tileInfo.getScreenRect(tempRect, tileSize, pageInfo);
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

			// fill with rescaled other tiles
			ArrayList<TileInfo> fetched = new ArrayList<>();
			tileInfo.getPageRect(tempRect, tileSize, pageInfo);
			GRect tempRect2 = new GRect();
			for (Map<TileInfo, CachedItem> map : Arrays.asList(smallCache, cache)) {
				for (Entry<TileInfo, CachedItem> entry : map.entrySet()) {
					TileInfo ti = entry.getKey();
					if (ti.page == tileInfo.page && entry.getValue().isFetched) {
						ti.getPageRect(tempRect2, tileSize, pageInfo);
						if (tempRect2.intersect(tempRect2, tempRect))
							fetched.add(ti);
					}
				}
			}
			if (fetched.isEmpty())
				return new Image(canvas.toDataUrl());

			Collections.sort(fetched, new Comparator<TileInfo>() {
				@Override
				public int compare(TileInfo ti1, TileInfo ti2) {
					return ti2.subsample - ti1.subsample;
				}
			});
			tileInfo.getScreenRect(tempRect, tileSize, pageInfo);
			double zoom = toZoom(tileInfo.subsample);
			for (TileInfo ti : fetched) {
				bufferContext.save();
				double scale = zoom / toZoom(ti.subsample);
				ti.getScreenRect(tempRect2, tileSize, pageInfo);
				bufferContext.translate(-tempRect.xmin, -tempRect.ymin);
				bufferContext.scale(scale, scale);
				bufferContext.translate(tempRect2.xmin, tempRect2.ymin);
				bufferContext.drawImage(ImageElement.as(getItem(ti).image.getElement()), 0, 0);
				bufferContext.restore();
			}

			cachedItem = new CachedItem();
			cachedItem.image = new Image(canvas.toDataUrl());
			putItem(tileInfo, cachedItem);
		}
		cachedItem.lastUsed = System.currentTimeMillis();
		return cachedItem.image;
	}

	public void addTileCacheListener(TileCacheListener listener) {
		listeners.add(listener);
	}

	private void putItem(TileInfo tileInfo, CachedItem cachedItem) {
		if (tileInfo.subsample == MAX_SUBSAMPLE) {
			smallCache.put(new TileInfo(tileInfo), cachedItem);
		} else {
			removeStaleItems();
			cache.put(new TileInfo(tileInfo), cachedItem);
		}
	}

	private CachedItem getItem(TileInfo tileInfo) {
		if (tileInfo.subsample == MAX_SUBSAMPLE)
			return smallCache.get(tileInfo);
		return cache.get(tileInfo);
	}

	private void removeStaleItems() {
		if (cache.size() < tileCacheSize)
			return;

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			@Override
			public void execute() {
				ArrayList<Entry<TileInfo, CachedItem>> cacheEntries = new ArrayList<>(cache.entrySet());
				Collections.sort(cacheEntries, new Comparator<Entry<TileInfo, CachedItem>>() {
					@Override
					public int compare(Entry<TileInfo, CachedItem> e1, Entry<TileInfo, CachedItem> e2) {
						long d = e1.getValue().lastUsed - e2.getValue().lastUsed;
						return d > 0 ? 1 : d < 0 ? -1 : 0;
					}
				});
				for (int i = 0; i < tileCacheSize / 4; i++) {
					cache.remove(cacheEntries.get(i).getKey());
				}
			}
		});
	}

	private class Fetcher implements RepeatingCommand, PageDownloadListener {
		private static final int PREFETCH_AGE = 500;

		private boolean isRunning;

		private GMap bufferGMap;

		public void fetch() {
			if (!isRunning)
				Scheduler.get().scheduleIncremental(this);
			isRunning = true;
		}

		@Override
		public boolean execute() {
			if (lastRange.isEmpty() || lastSubsample == MAX_SUBSAMPLE) {
				isRunning = false;
				return false;
			}

			for (int pageNum : Arrays.asList(lastPageNum, lastPageNum + 1, lastPageNum - 1)) {
				if (pageNum < 0 || pageNum >= pageCache.getPageCount())
					continue;
				final DjVuPage page = pageCache.getPage(pageNum);
				if (page == null)
					continue;
				tempTI.page = pageNum;
				tempTI.subsample = lastSubsample;
				for (int y = lastRange.ymin; y <= lastRange.ymax; y++) {
					for (int x = lastRange.xmin; x <= lastRange.xmax; x++) {
						CachedItem cachedItem = cache.get(tempTI.setXY(x, y));
						if (cachedItem != null && cachedItem.isFetched)
							continue;
						CachedItem item = prepareItem(tempTI, cachedItem, page);
						if (pageNum == lastPageNum) {
							final TileInfo tileInfo = new TileInfo(tempTI);
							Scheduler.get().scheduleDeferred(new ScheduledCommand() {
								
								@Override
								public void execute() {
									for (TileCacheListener listener : listeners)
										listener.tileAvailable(tileInfo);
								}
							});
						} else {
							item.lastUsed -= PREFETCH_AGE;
						}
						return true;
					}
				}

				final DjVuInfo pageInfo = page.getInfo();
				final int maxX = (int) Math.ceil(1.0 * pageInfo.width / lastSubsample / tileSize) - 1;
				final int maxY = (int) Math.ceil(1.0 * pageInfo.height / lastSubsample / tileSize) - 1;
				final int dx = (lastRange.width() + 1) / 2, dy = (lastRange.height() + 1) / 2;
				for (int d = 1; d <= dx; d++) {
					int x = lastRange.xmax + d;
					for (int y = lastRange.ymin; y <= lastRange.ymax + Math.min(d, dy); y++) {
						if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && !cache.containsKey(tempTI.setXY(x, y))) {
							prepareItem(tempTI, null, page).lastUsed -= PREFETCH_AGE;
							return true;
						}
					}
					x = lastRange.ymin - d;
					for (int y = lastRange.ymin - Math.min(d, dy); y <= lastRange.ymax; y++) {
						if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && !cache.containsKey(tempTI.setXY(x, y))) {
							prepareItem(tempTI, null, page).lastUsed -= PREFETCH_AGE;
							return true;
						}
					}
				}
				for (int d = 1; d <= dy; d++) {
					int y = lastRange.ymax + d;
					for (int x = lastRange.xmin; x <= lastRange.xmax + Math.min(d, dx); x++) {
						if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && !cache.containsKey(tempTI.setXY(x, y))) {
							prepareItem(tempTI, null, page).lastUsed -= PREFETCH_AGE;
							return true;
						}
					}
					y = lastRange.ymin - d;
					for (int x = lastRange.xmin - Math.min(d, dx); x <= lastRange.xmax; x++) {
						if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && !cache.containsKey(tempTI.setXY(x, y))) {
							prepareItem(tempTI, null, page).lastUsed -= PREFETCH_AGE;
							return true;
						}
					}
				}
			}

			isRunning = false;
			return false;
		}

		private CachedItem prepareItem(TileInfo tileInfo, CachedItem cachedItem, DjVuPage page) {
			if (cachedItem == null) {
				cachedItem = new CachedItem();
				putItem(tileInfo, cachedItem);
			}

			tileInfo.getScreenRect(tempRect, tileSize, page.getInfo());
			int w = tempRect.width(), h = tempRect.height();
			bufferGMap = page.getMap(tempRect, tileInfo.subsample, bufferGMap);
			CanvasElement canvas = bufferContext.getCanvas();
			canvas.setWidth(w);
			canvas.setHeight(h);
			bufferContext.putImageData(bufferGMap.getData(), 0, 0);
			cachedItem.image = new Image(canvas.toDataUrl());
			cachedItem.isFetched = true;
			cachedItem.lastUsed = System.currentTimeMillis();
			return cachedItem;
		}

		@Override
		public void pageAvailable(int pageNum) {
			// prepare full view for the lowest quality
			DjVuPage page = pageCache.getPage(pageNum);
			DjVuInfo info = page.getInfo();
			int w = (info.width + MAX_SUBSAMPLE - 1) / MAX_SUBSAMPLE;
			int h = (info.height + MAX_SUBSAMPLE - 1) / MAX_SUBSAMPLE;
			TileInfo tileInfo = new TileInfo();
			tileInfo.page = pageNum;
			tileInfo.subsample = MAX_SUBSAMPLE;
			for (int x = 0; x * tileSize < w; x++) {
				for (int y = 0; y * tileSize < h; y++) {
					prepareItem(tileInfo.setXY(x, y), null, page);
				}
			}

			fetch();
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

		void getScreenRect(GRect rect, int tileSize, DjVuInfo info) {
			int pw = (info.width + subsample - 1) / subsample, ph = (info.height + subsample - 1) / subsample;
			getRect(rect, tileSize, pw, ph);
		}

		void getPageRect(GRect rect, int tileSize, DjVuInfo info) {
			getRect(rect, tileSize * subsample, info.width, info.height);
		}

		private void getRect(GRect rect, int tileSize, int pw, int ph) {
			rect.xmin = x * tileSize;
			rect.xmax = Math.min((x + 1) * tileSize, pw);
			rect.ymin = Math.max(ph - (y + 1) * tileSize, 0);
			rect.ymax = ph - y * tileSize;
		}

		private TileInfo setXY(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
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
