//C- -------------------------------------------------------------------
//C- Java DjVu (r) (v. 0.8)
//C- Copyright (c) 2004-2005 LizardTech, Inc.  All Rights Reserved.
//C- Java DjVu is protected by U.S. Pat. No.C- 6,058,214 and patents
//C- pending.
//C-
//C- This software is subject to, and may be distributed under, the
//C- GNU General Public License, Version 2. The license should have
//C- accompanied the software or you may obtain a copy of the license
//C- from the Free Software Foundation at http://www.fsf.org .
//C-
//C- The computer code originally released by LizardTech under this
//C- license and unmodified by other parties is deemed "the LIZARDTECH
//C- ORIGINAL CODE."  Subject to any third party intellectual property
//C- claims, LizardTech grants recipient a worldwide, royalty-free,
//C- non-exclusive license to make, use, sell, or otherwise dispose of
//C- the LIZARDTECH ORIGINAL CODE or of programs derived from the
//C- LIZARDTECH ORIGINAL CODE in compliance with the terms of the GNU
//C- General Public License.   This grant only confers the right to
//C- infringe patent claims underlying the LIZARDTECH ORIGINAL CODE to
//C- the extent such infringement is reasonably necessary to enable
//C- recipient to make, have made, practice, sell, or otherwise dispose
//C- of the LIZARDTECH ORIGINAL CODE (or portions thereof) and not to
//C- any greater extent that may be necessary to utilize further
//C- modifications or combinations.
//C-
//C- The LIZARDTECH ORIGINAL CODE is provided "AS IS" WITHOUT WARRANTY
//C- OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//C- TO ANY WARRANTY OF NON-INFRINGEMENT, OR ANY IMPLIED WARRANTY OF
//C- MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
//C-
//C- In addition, as a special exception, LizardTech Inc. gives permission
//C- to link the code of this program with the proprietary Java
//C- implementation provided by Sun (or other vendors as well), and
//C- distribute linked combinations including the two. You must obey the
//C- GNU General Public License in all respects for all of the code used
//C- other than the proprietary Java implementation. If you modify this
//C- file, you may extend this exception to your version of the file, but
//C- you are not obligated to do so. If you do not wish to do so, delete
//C- this exception statement from your version.
//C- -------------------------------------------------------------------
//C- Developed by Bill C. Riemers, Foxtrot Technologies Inc. as work for
//C- hire under US copyright laws.
//C- -------------------------------------------------------------------
//
package com.lizardtech.djvu;

import java.io.IOException;
import java.util.*;

import com.google.gwt.core.client.GWT;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.gwt.xhr.client.XMLHttpRequest.ResponseType;


/**
 * This class implements a random access mapping of input streams and URL 
 * connections.  For input streams and non-http connections, the mapping 
 * is achieved by reading in all the data into memory.  Http streams are
 * mapped by reading in data blocks when needed.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.13 $
 */
public class URLInputStream extends InputStream
{
  //~ Static fields/initializers ---------------------------------------------

  /** The default size of each block. */
  public static final int BLOCKSIZE = 8192;
    
  /** Object for caching raw data. ! */
  public static HashMap<String, Uint8Array> cache = new HashMap<>();

  private static final HashMap<String, List<InputStateListener>> listeners = new HashMap<>();

  //~ Instance fields --------------------------------------------------------

  // This contains the data we a buffering.
  private Uint8Array data;

  private int offset;

  // Used for the mark and reset features.
  private int markOffset=0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DataPool object.
   */
  public URLInputStream()
  {
  }

  public URLInputStream(URLInputStream toCopy) {
	  this.data = toCopy.data;
	  this.offset = toCopy.offset;
	  this.markOffset = toCopy.markOffset;
  }

  //~ Methods ----------------------------------------------------------------

	/**
	 * Initialize this map to read the specified URL. If a cached DataPool for
	 * this URL exists, it will be returned.
	 * 
	 * @param url
	 *            the URL to read
	 * 
	 * @return an initialized DataPool
	 */
	public URLInputStream init(final String url, final InputStateListener listener) {
		data = cache.get(url);
		if (data == null) {
			startDownload(url);

			if (listener != null) {
				List<InputStateListener> listenersList = listeners.get(url);
				if (listenersList == null) {
					listeners.put(url, listenersList = new ArrayList<>());
					listenersList.add(listener);
				} else {
					listenersList.add(new InputStateListener() {

						@Override
						public void inputReady() {
							init(url, this);
							listener.inputReady();
						}
					});
				}
			}
		} else {
			offset = 0;
		}
		return this;
	}

	private void startDownload(final String url) {
		XMLHttpRequest request = XMLHttpRequest.create();
		request.open("GET", url);
		request.setResponseType(ResponseType.ArrayBuffer);
		request.setOnReadyStateChange(new ReadyStateChangeHandler() {

			@Override
			public void onReadyStateChange(XMLHttpRequest xhr) {
				if (xhr.getReadyState() == XMLHttpRequest.DONE) {
					if (xhr.getStatus() == 200) {
						data = TypedArrays.createUint8Array(xhr.getResponseArrayBuffer());
						cache.put(url, data);
					} else {
						GWT.log("Error downloading " + url);
						GWT.log("response status: " + xhr.getStatus() + " " + xhr.getStatusText());
					}
					fireReady(url);
				}
			}
		});
		request.send();
	}

	protected void fireReady(String url) {
		List<InputStateListener> listenersList = listeners.remove(url);
		if (listenersList != null)
			for (InputStateListener listener : listenersList)
				listener.inputReady();
	}

  public boolean isReady()
  {
	  return data != null;
  }

	@Override
	public int read() {
		if (offset < data.length()) {
			return data.get(offset++);
		}
		return -1;
	}

	@Override
	public int read(byte[] b) {
		int i = 0;
		for (; i < b.length && (offset + i) < data.length(); i++) {
			b[i] = (byte) data.get(offset + i);
		}
		if (i == 0)
			return -1;
		offset += i;
		return i;
	}

	@Override
	public long skip(long n) throws IOException {
		int oldOffset = offset;
		offset = (int) Math.min(offset + n, data.length());
		return offset - oldOffset;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		markOffset = offset;
	}

	@Override
	public synchronized void reset() throws IOException {
		offset = markOffset;
	}
}
