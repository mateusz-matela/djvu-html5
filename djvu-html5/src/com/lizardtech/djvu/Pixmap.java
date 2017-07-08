package com.lizardtech.djvu;

/**
 * Common interface for IWPixmap and GPixmap
 */
public interface Pixmap extends Codec {

	int getMemoryUsage();

	int getWidth();

	int getHeight();

	/**
	 * Create a pixmap with the specified subsample rate and bounds.
	 *
	 * @param subsample rate at which to subsample
	 * @param rect Bounding box of the desired pixmap.
	 * @param retval An old pixmap to try updating, or null.
	 *
	 * @return DOCUMENT ME!
	 */
	GPixmap getPixmap(int subsample, GRect rect, GPixmap retval);

}
