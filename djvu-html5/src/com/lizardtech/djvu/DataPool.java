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
public class DataPool
{
  //~ Static fields/initializers ---------------------------------------------

  /** The default size of each block. */
  public static final int BLOCKSIZE = 8192;
    
  /** Object for caching raw data. ! */
  public static HashMap<String, DataPool> cache = new HashMap<>();

  //~ Instance fields --------------------------------------------------------

  // This contains the data we a buffering.
  private final Vector<byte[]> buffer = new Vector<>();

  // The end of the stream, or a number larger than the end of the stream.
  private int endOffset=0;

  private boolean isReady = false;

  private Vector<InputStateListener> listeners = new Vector<>();
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DataPool object.
   */
  public DataPool()
  {
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Initialize this map to read the specified URL. If a cached DataPool for this 
   * URL exists, it will be returned.
   * 
   * @param url the URL to read
   * 
   * @return an initialized DataPool
   */
  public DataPool init(final String url, InputStateListener listener)
  {
    DataPool retval=this;
    if(url != null)
    {
      retval=cache.get(url);
      if(retval == null)
      {
        retval=this;
        cache.put(url, this);
        startDownload(url);
      }
    }
    if (listener != null)
    	retval.listeners.add(listener);
    return retval;
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
						long startTime = System.currentTimeMillis();
						Uint8Array array = TypedArrays.createUint8Array(xhr.getResponseArrayBuffer());
						endOffset = array.length();
						int blocks = 0;
						while (blocks * BLOCKSIZE < endOffset) {
							byte[] bytes = new byte[BLOCKSIZE];
							buffer.add(bytes);
							for (int i = 0; i < BLOCKSIZE && blocks * BLOCKSIZE + i < endOffset; i++)
								bytes[i] = (byte) array.get(blocks * BLOCKSIZE + i);
							blocks++;
						}
						System.out.println("Response conversion time: " + (System.currentTimeMillis() - startTime));
					} else {
						DjVuOptions.err.println("Error downloading " + url);
						DjVuOptions.err.println("response status: " + xhr.getStatus() + " " + xhr.getStatusText());
					}
					isReady = true;
					fireReady();
				}
			}
		});
		request.send();
	}

	protected void fireReady() {
		for (InputStateListener listener : listeners)
			listener.inputReady();
	}

/**
   * Initialize this map to read the specified stream
   * 
   * @param input the InputStream to read
   * 
   * @return the initialized DataPool
   */
  public DataPool init(final InputStream input) throws IOException
  {
	  int totalBytesRead = 0;
	  for (;;) {
		  byte[] bytes = new byte[BLOCKSIZE];
		  buffer.add(bytes);
		  int bytesRead = 0;
		  int lastRead = 1;
		  while (lastRead > 0 && bytesRead < BLOCKSIZE) {
			  lastRead = input.read(bytes, bytesRead, BLOCKSIZE - bytesRead);
			  bytesRead += lastRead;
		  }
		  totalBytesRead += bytesRead;
		  if (lastRead <= 0)
			  break;
	  }
	  endOffset = totalBytesRead;
	  isReady = true;
	  input.close();
    return this;
  }
  
  /**
   * Request the specified block of data. Data may be buffered, or read.
   *
   * @param index the position of the block start position divided by BLOCKSIZE.
   * @param read True if unavailable blocks should be read from the data source.
   *
   * @return a byte array up to size BLOCKSIZE, or null if no data is available.
   */
  public byte [] getBlock(final int index, final boolean read)
  {
    int start=index*BLOCKSIZE;
    if((index < 0)||(start >= endOffset))
    {
      return null;
    }
    if(index < buffer.size())
    {
      byte[] block=buffer.elementAt(index);
      if(block != null)
      {
        return block;
      }
    }
    return null;
  }

  /**
   * Query the size of this vector.
   *
   * @return the size of this vector
   */
  public int getEndOffset()
  {
    return endOffset;
  }

  public boolean isReady()
  {
	  return isReady;
  }

}
