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

import java.util.HashMap;

import com.google.gwt.typedarrays.shared.ArrayBuffer;
import com.google.gwt.typedarrays.shared.TypedArrays;
import com.google.gwt.typedarrays.shared.Uint8Array;

import jsinterop.annotations.JsProperty;

/**
 * This is an abstract class for representing pixel maps.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.9 $
 */
public class GMap
{
  //~ Instance fields --------------------------------------------------------

	protected static int BYTES_PER_PIXEL = 4;

	protected Uint8Array data;
	private ArrayBuffer dataBuffer;
	@JsProperty
	protected int dataWidth;
	@JsProperty
	protected int dataHeight;
	
	/** number of border pixels */
	@JsProperty
	protected int border = 0;

/** properties associated with this image map */
  public final HashMap<String, Object> properties = new HashMap<>();

  /** The offset to the color red. */
  protected final int redOffset;
  
  /** The offset to the color green. */
  protected final int greenOffset;
  
  /** The offset to the color blue. */
  protected final int blueOffset;
  
  /** The number of columns. */
  protected int ncolumns=0;
  
  /** The number of rows. */
  protected int nrows=0;
  
  /** False if we can skip the ramp call. */
  protected final boolean needRamp;
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new GMap object.
   */
  public GMap(final int redOffset,final int greenOffset,final int blueOffset,final boolean needRamp)
  {
    this.needRamp=needRamp;
    this.redOffset=redOffset;
    this.greenOffset=greenOffset;
    this.blueOffset=blueOffset;
  }

	public GMap(GMap toCopy) {
		this(toCopy.redOffset, toCopy.greenOffset, toCopy.blueOffset, toCopy.needRamp);
		this.dataBuffer = toCopy.dataBuffer;
		this.data = toCopy.data;
		this.dataWidth = toCopy.dataWidth;
		this.dataHeight = toCopy.dataHeight;
		this.border = toCopy.border;
	}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query the number of columns in an image.
   *
   * @return the number of columns
   */
  public final int columns()
  {
      return ncolumns;
  }

  /**
   * Query the number of rows in an image.
   *
   * @return the number of rows
   */
  public final int rows()
  {
      return nrows;
  }

  public Uint8Array getImageData() {
    return data;
  }
  public int getDataWidth() {
	  return dataWidth;
  }
  public int getDataHeight() {
	  return dataHeight;
  }

  public int getBorder() {
    return border;
  }

  protected void createImageData(int columns, int rows) {
    data = TypedArrays.createUint8Array(columns * rows * BYTES_PER_PIXEL);
    dataBuffer = data.buffer();
    dataWidth = columns;
    dataHeight = rows;
  }
  /**
   * Query the start offset of a row.
   *
   * @param row the row to query
   *
   * @return the offset to the pixel data
   */
  public int rowOffset(final int row)
  {
    return row * getRowSize() + border;
  }

  /**
   * Query the getRowSize.
   * 
   * @return the getRowSize
   */
  public int getRowSize()
  {
    return columns();
  }

  /**
   * Query the data offset for red pixels.
   * 
   * @return red data offset
   */
  public final int getRedOffset()
  {
    return redOffset;
  }
  
  /**
   * Query the data offset for green pixels.
   * 
   * @return the number of bytes per pixel
   */
  public final int getGreenOffset()
  {
    return greenOffset;
  }

  /**
   * Query the data offset for blue pixels.
   * 
   * @return the number of bytes per pixel
   */
  public final int getBlueOffset()
  {
    return blueOffset;
  }

  /**
   * Convert the pixel to 24 bit color.
   *
   * @return the converted pixel
   */
  public GPixel ramp(final GPixelReference ref)
  {
    return ref;
  }
  
  /**
   * Query if we are allowed to skip the ramp call.  
   * This call can be used to help optimize loops.
   *
   * @return true if not 24 bit color
   */
  public boolean isRampNeeded()
  {
    return needRamp;
  }
  
	public int getMemoryUsage() {
		return data.byteLength();
	}

	public Object getTransferable() {
		return dataBuffer;
	}
}
