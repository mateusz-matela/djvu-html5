package pl.djvuhtml5.client;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.i18n.client.Dictionary;

public class DjvuContext {

	private static final String CONTEXT_GLOBAL_VARIABLE = "DJVU_CONTEXT";

	private static Dictionary context;

	private static Context2d drawingContext;

	public static final void init(Context2d drawingContext) {
		context = Dictionary.getDictionary(CONTEXT_GLOBAL_VARIABLE);
		DjvuContext.drawingContext = drawingContext;
	}

	public static final void printError(String message) {
		drawingContext.save();
		drawingContext.setFont("bold 12px sans-serif");
		drawingContext.setFillStyle("red");
		drawingContext.fillText(message, 10, 20);
		drawingContext.restore();
	}

	public static String getIndexFile() {
		return getString("file", null);
	}

	public static int getParallelDownloads() {
		return getInt("parallelDownloads", 3);
	}

	public static int getTileSize() {
		return getInt("tileSize", 256);
	}

	public static int getTileCacheSize() {
		return getInt("tileCacheSize", 512);
	}

	public static String getBackground() {
		return getString("background", "#666");
	}

	public static int getFitToPageMargin() {
		return getInt("fitToPageMargin", 8);
	}

	private static String getString(String key, String defaultValue) {
		if (!context.keySet().contains(key))
			return defaultValue;
		return context.get(key);
	}

	private static int getInt(String key, int defaultValue) {
		if (!context.keySet().contains(key))
			return defaultValue;
		String value = context.get(key);
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
