package pl.djvuhtml5.client;

public class DjvuContext {

	private DjvuContext() {
		// no instances
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
		return Boolean.valueOf(value);
	}

	private static native String get(String key) /*-{
		return $wnd ? $wnd.DJVU_CONTEXT ? $wnd.DJVU_CONTEXT[key] : null : null;
	}-*/;
}
