package pl.djvuhtml5.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.ImageData;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.GRect;

public class TileCache {

	public final int tileSize;

	private final int tileCacheSize;

	private final Context2d bufferContext;

	private final Document document;

	private HashMap<TileInfo, CachedItem> cache = new HashMap<>();

	private ArrayList<TileInfo> fetchNeeded = new ArrayList<>();

	public TileCache(Document document) {
		this.document = document;
		this.tileCacheSize = DjvuContext.getTileCacheSize();
		this.tileSize = DjvuContext.getTileSize();

		Canvas buffer = Canvas.createIfSupported();
		buffer.setWidth(tileSize + "px");
		buffer.setCoordinateSpaceWidth(tileSize);
		buffer.setHeight(tileSize + "px");
		buffer.setCoordinateSpaceHeight(tileSize);
		bufferContext = buffer.getContext2d();
		bufferContext.setFillStyle("#AAA");
	}

	public ImageData getTileImage(TileInfo tileInfo) {
		CachedItem cachedItem = cache.get(tileInfo);
		if (cachedItem == null) {
			bufferContext.clearRect(0, 0, tileSize, tileSize);
			final int count = 16;
			final int size = tileSize / count;
			for (int x = 0; x < count; x++)
				for (int y = 0; y < count; y++)
					if ((x + y) % 2 == 1)
						bufferContext.fillRect(x * size, y * size, size, size);

			//TODO fill with rescaled other tiles

			cachedItem = new CachedItem();
			cachedItem.imageData = bufferContext.getImageData(0, 0, tileSize, tileSize);
			tileInfo = new TileInfo(tileInfo);
			cache.put(tileInfo, cachedItem);
			fetchNeeded.add(tileInfo);
		}
		cachedItem.lastUsed = System.currentTimeMillis();
		return cachedItem.imageData;
	}

	public static final class TileInfo {
		public int page;
		public float zoom;
		public int x;
		public int y;

		public TileInfo(int page, float zoom, int x, int y) {
			this.page = page;
			this.zoom = zoom;
			this.x = x;
			this.y = y;
		}

		public TileInfo(TileInfo toCopy) {
			this(toCopy.page, toCopy.zoom, toCopy.x, toCopy.y);
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
			result = prime * result + Float.floatToIntBits(zoom);
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
			if (Float.floatToIntBits(zoom) != Float.floatToIntBits(other.zoom))
				return false;
			return true;
		}
	}

	private static final class CachedItem {
		public ImageData imageData;
		public long lastUsed;
		public boolean isFetched;
	}
}
