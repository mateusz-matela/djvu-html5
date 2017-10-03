package pl.djvuhtml5.client;

import java.util.ArrayList;

import com.lizardtech.djvu.GRect;

public class DjvuContext {

	private DjvuContext() {
		// no instances
	}

	private static String url;
	private static int page = -1;
	private static int subsample = 1;
	private static GRect tileRange = new GRect();
	private static ArrayList<Runnable> viewChangeListeners = new ArrayList<>();

	public static void setUrl(String url) {
		DjvuContext.url = url;
	}

	public static String getUrl() {
		return url;
	}

	public static void setPage(int page) {
		if (DjvuContext.page != page) {
			DjvuContext.page = page;
			fireViewChanged();
		}
	}

	public static int getPage() {
		return page;
	}

	public static void setTileRange(GRect tileRange, int subsample) {
		if (!DjvuContext.tileRange.equals(tileRange) || DjvuContext.subsample != subsample) {
			DjvuContext.tileRange.clear();
			DjvuContext.tileRange.recthull(DjvuContext.tileRange, tileRange);
			DjvuContext.subsample = subsample;
			fireViewChanged();
		}
	}

	public static void getTileRange(GRect result) {
		result.clear();
		result.recthull(result, tileRange);
	}

	public static int getSubsample() {
		return subsample;
	}

	public static void addViewChangeListener(Runnable listener) {
		viewChangeListeners.add(listener);
	}

	private static void fireViewChanged() {
		for (Runnable listener : viewChangeListeners)
			listener.run();
	}

	public static String getIndexFile() {
		return getString("file", null);
	}
	
	public static int getTileSize() {
		return getInt("tileSize", 512);
	}
	
	public static int getTileCacheSize() {
		return getInt("tileCacheSize", 256);
	}
	
	public static int getPageCacheSize() {
		return getInt("pageCacheSize", 128 * 1024 * 1024);
	}
	
	public static int getFileCacheSize() {
		return getInt("fileCacheSize", 16 * 1024 * 1024);
	}
	
	public static String getBackground() {
		return getString("background", "#666");
	}
	
	public static int getUiHideDelay() {
		return getInt("uiHideDelay", 1500);
	}
	
	public static int getPageMargin() {
		return getInt("pageMargin", 8);
	}
	
	public static int getScreenDPI() {
		return getInt("screenDPI", 96);
	}
	
	public static int getMaxZoom() {
		return getInt("maxZoom", 10000);
	}
	
	public static boolean getLocationUpdateEnabled() {
		return getBoolean("locationUpdateEnabled", true);
	}
	
	public static boolean getTextLayerEnabled() {
		return getBoolean("textLayerEnabled", true);
	}

	public static boolean getUseWebWorkers() {
		return getBoolean("useWebWorkers", true);
	}

	public static String getString(String key, String defaultValue) {
		String value = get(key);
		return value != null ? value : defaultValue;
	}
	
	private static int getInt(String key, int defaultValue) {
		try {
			return Integer.valueOf(get(key));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	private static boolean getBoolean(String key, boolean defaultValue) {
		String value = get(key);
		return value == null ? defaultValue : Boolean.valueOf(value);
	}

	private static native String get(String key) /*-{
		return $wnd ? $wnd.DJVU_CONTEXT ? $wnd.DJVU_CONTEXT[key] : null : null;
	}-*/;

	public static native Object exportConfig() /*-{
		return $wnd.DJVU_CONTEXT;
	}-*/;

	public static native void importConfig(Object data) /*-{
		$wnd.DJVU_CONTEXT = data;
	}-*/;
}
