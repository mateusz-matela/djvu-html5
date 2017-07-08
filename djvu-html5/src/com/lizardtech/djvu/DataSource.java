package com.lizardtech.djvu;

import com.google.gwt.typedarrays.shared.Uint8Array;

public interface DataSource {

	interface ReadyListener {
		void dataReady();
	}

	/**
	 * @return full contents of requested resource or {@code null} if it's not
	 *         currently available. In the latter case, if a
	 *         {@code readyListener} is given, it will be notified when when the
	 *         resource becomes available.
	 */
	Uint8Array getData(String url, ReadyListener readyListener);
}
