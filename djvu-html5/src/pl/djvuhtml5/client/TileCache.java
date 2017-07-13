package pl.djvuhtml5.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import com.lizardtech.djvu.DjVuInfo;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;

public class TileCache {

	public static final int MAX_SUBSAMPLE = 12;

	private static final int PREFETCH_AGE = 500;

	private final Djvu_html5 app;

	private final int tileSize;

	private final int tileCacheSize;

	private final PageCache pageCache;

	private final HashMap<TileInfo, CachedItem> cache = new HashMap<>();

	/** All tiles of max subsample are stored here and will never be thrown away */
	private final HashMap<TileInfo, CachedItem> smallCache = new HashMap<>();

	private GMap bufferGMap;

	private final GRect tempRect = new GRect();
	private final TileInfo tempTI = new TileInfo();

	private int lastPageNum = -1, lastSubsample;
	private final GRect lastRange = new GRect();

	public TileCache(Djvu_html5 app, PageCache pageCache) {
		this.app = app;
		this.pageCache = pageCache;
		this.tileCacheSize = DjvuContext.getTileCacheSize();
		this.tileSize = DjvuContext.getTileSize();

		DjvuContext.addViewChangeListener(this::viewChanged);
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

	private void viewChanged() {
		lastPageNum = DjvuContext.getPage();
		lastSubsample = DjvuContext.getSubsample();
		DjvuContext.getTileRange(lastRange);
		
		tempTI.page = lastPageNum;
		tempTI.subsample = lastSubsample;
		for (int y = lastRange.ymin; y <= lastRange.ymax; y++) {
			for (int x = lastRange.xmin; x <= lastRange.xmax; x++) {
				CachedItem item = cache.get(tempTI.setXY(x, y));
				if (item != null)
					item.lastUsed = System.currentTimeMillis();
			}
		}
		app.startProcessing();
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

		ArrayList<Entry<TileInfo, CachedItem>> cacheEntries = new ArrayList<>(cache.entrySet());
		Collections.sort(cacheEntries, (e1, e2) -> {
			long d = e1.getValue().lastUsed - e2.getValue().lastUsed;
			return d > 0 ? 1 : d < 0 ? -1 : 0;
		});
		ArrayList<TileInfo> tilesToRemove = new ArrayList<>();
		for (int i = 0; i < tileCacheSize / 4; i++) {
			TileInfo tile = cacheEntries.get(i).getKey();
			cache.remove(tile);
			tilesToRemove.add(tile);
		}
		app.getDataStore().releaseTileImages(tilesToRemove);
	}

	boolean prefetchPreviews(boolean all) {
		if (lastPageNum < 0)
			return false;
		tempTI.subsample = MAX_SUBSAMPLE;
		for (int i = 0; i < (all ? pageCache.getPageCount() * 2 : 1); i++) {
			int index = lastPageNum + (i % 2 == 0 ? -1 : 1) * (i / 2);
			if (index < 0 || index >= pageCache.getPageCount())
				continue;
			DjVuPage page = pageCache.getPage(index);
			if (page == null)
				continue;
			tempTI.page = index;
			DjVuInfo info = page.getInfo();
			int w = (info.width + MAX_SUBSAMPLE - 1) / MAX_SUBSAMPLE;
			int h = (info.height + MAX_SUBSAMPLE - 1) / MAX_SUBSAMPLE;
			for (int x = 0; x * tileSize < w; x++) {
				for (int y = 0; y * tileSize < h; y++) {
					if (prepareItem(tempTI.setXY(x, y), page, false))
						return true;
				}
			}
		}
		return false;
	}

	boolean prefetchCurrentView(int pageDelta) {
		int pageNum = lastPageNum + pageDelta;
		if (pageNum < 0 || pageNum >= pageCache.getPageCount())
			return false;
		final DjVuPage page = pageCache.getPage(pageNum);
		if (page == null)
			return false;

		tempTI.page = pageNum;
		tempTI.subsample = lastSubsample;
		for (int y = lastRange.ymin; y <= lastRange.ymax; y++) {
			for (int x = lastRange.xmin; x <= lastRange.xmax; x++) {
				boolean isPrefetch = pageNum != lastPageNum;
				if (prepareItem(tempTI.setXY(x, y), page, isPrefetch))
					return true;
			}
		}
		return false;
	}

	boolean prefetchAdjacent(int pageDelta) {
		int pageNum = lastPageNum + pageDelta;
		if (pageNum < 0 || pageNum >= pageCache.getPageCount())
			return false;
		final DjVuPage page = pageCache.getPage(pageNum);
		if (page == null)
			return false;

		tempTI.page = pageNum;
		tempTI.subsample = lastSubsample;
		final DjVuInfo pageInfo = page.getInfo();
		final int maxX = (int) Math.ceil(1.0 * pageInfo.width / lastSubsample / tileSize) - 1;
		final int maxY = (int) Math.ceil(1.0 * pageInfo.height / lastSubsample / tileSize) - 1;
		final int dx = (lastRange.width() + 1) / 2, dy = (lastRange.height() + 1) / 2;
		for (int d = 1; d <= dx; d++) {
			int x = lastRange.xmax + d;
			for (int y = lastRange.ymin; y <= lastRange.ymax + Math.min(d, dy); y++) {
				if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && prepareItem(tempTI.setXY(x, y), page, true))
					return true;
			}
			x = lastRange.ymin - d;
			for (int y = lastRange.ymin - Math.min(d, dy); y <= lastRange.ymax; y++) {
				if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && prepareItem(tempTI.setXY(x, y), page, true))
					return true;
			}
		}
		for (int d = 1; d <= dy; d++) {
			int y = lastRange.ymax + d;
			for (int x = lastRange.xmin; x <= lastRange.xmax + Math.min(d, dx); x++) {
				if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && prepareItem(tempTI.setXY(x, y), page, true))
					return true;
			}
			y = lastRange.ymin - d;
			for (int x = lastRange.xmin - Math.min(d, dx); x <= lastRange.xmax; x++) {
				if (x >= 0 && x <= maxX && y >= 0 && y <= maxY && prepareItem(tempTI.setXY(x, y), page, true))
					return true;
			}
		}
		return false;
	}

	/**
	 * @return {@code false} iff the item has already been prepared and this method did nothing 
	 */
	private boolean prepareItem(final TileInfo tileInfo, DjVuPage page, boolean isPrefetch) {
		tileInfo.getScreenRect(tempRect, tileSize, page.getInfo());
		if (tempRect.isEmpty())
			return false;
		CachedItem cachedItem = getItem(tileInfo);
		if (cachedItem == null) {
			cachedItem = new CachedItem();
			putItem(tileInfo, cachedItem);
		}
		if (cachedItem.isFetched)
			return false;
	
		bufferGMap = page.getMap(tempRect, tileInfo.subsample, bufferGMap);
		if (bufferGMap != null) {
			app.getDataStore().setTile(tileInfo, bufferGMap);
		}
		cachedItem.isFetched = true;
		cachedItem.lastUsed = System.currentTimeMillis() - (isPrefetch ? PREFETCH_AGE : 0);
		if (!isPrefetch) {
			app.interruptProcessing();
		}
		return true;
	}

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
			rect.xmin = Math.min(x * tileSize, pw);
			rect.xmax = Math.min((x + 1) * tileSize, pw);
			rect.ymin = Math.max(ph - (y + 1) * tileSize, 0);
			rect.ymax = Math.max(ph - y * tileSize, 0);
			assert rect.xmin <= rect.xmax && rect.ymin <= rect.ymax;
		}

		TileInfo setXY(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}
	}

	private static final class CachedItem {
		public long lastUsed;
		public boolean isFetched;
	}
}
