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

import java.io.*;
import java.util.*;


/**
 * This is an abstract class for representing pixel maps.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.9 $
 */
public abstract class GMap
  extends DjVuObject
  implements Cloneable
{
  //~ Instance fields --------------------------------------------------------

  /** properties associated with this image map */
  public final Hashtable properties = new Hashtable();

  /** The number of bytes per pixel */
  protected final int ncolors;

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
  
  /** The raw pixel data. */
  protected byte [] data=null;
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new GMap object.
   */
  public GMap(final int ncolors,final int redOffset,final int greenOffset,final int blueOffset,final boolean needRamp)
  {
    this.ncolors=ncolors;
    this.needRamp=needRamp;
    this.redOffset=redOffset;
    this.greenOffset=greenOffset;
    this.blueOffset=blueOffset;
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
   * Insert the reference map at the specified location.
   *
   * @param ref map to insert
   * @param dx horizontal position to insert at
   * @param dy vertical position to insert at
   */
  public abstract void fill(
    GMap ref,
    int  dx,
    int  dy);

  /**
   * Fills an array of pixels from the specified values.
   *
   * @param x the x-coordinate of the upper-left corner of the region of
   *        pixels
   * @param y the y-coordinate of the upper-left corner of the region of
   *        pixels
   * @param w the width of the region of pixels
   * @param h the height of the region of pixels
   * @param pixels the array of pixels
   * @param off the offset into the pixel array
   * @param scansize the distance from one row of pixels to the next in the
   *        array
   */
  public void fillRGBPixels(
    final int   x,
    final int   y,
    final int   w,
    final int   h,
    final int[] pixels,
    int         off,
    final int   scansize)
  {
    createGPixelReference(0).fillRGBPixels(x, y, w, h, pixels, off, scansize);
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

  /**
   * Shift the origin of the image by coping the pixel data.
   *
   * @param dx amount to shift the origin of the x-axis
   * @param dy amount to shift the origin of the y-axis
   * @param retval the image to copy the data into
   *
   * @return the translated image
   */
  public abstract GMap translate(
    int  dx,
    int  dy,
    GMap retval);

  /**
   * Create a copy of this image.
   *
   * @return The newly created copy.
   */
  public Object clone()
  {
    GMap retval = null;

    try
    {
      retval = (GMap)super.clone();
      if(data != null)
      {
        retval.data = (byte[])data.clone();
      }
    }
    catch(final CloneNotSupportedException ignored) {}

    return retval;
  }

  /**
   * Query the raw data buffer.
   *
   * @return the array of pixels
   */
  public final byte[] getData()
  {
    return data;
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
    return row * getRowSize();
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
   * Query the bytes per pixel.
   * 
   * @return the number of bytes per pixel
   */
  public final int getColorSize()
  {
    return ncolors;
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
   * Create a GPixelReference (a pixel iterator) that refers to this map
   * starting at the specified offset.
   *
   * @param offset position of the first pixel to reference
   *
   * @return the newly created GPixelReference
   */
  public GPixelReference createGPixelReference(final int offset)
  {
    return new GPixelReference(this, offset);
  }

  /**
   * Create a GPixelReference (a pixel iterator) that refers to this map
   * starting at the specified position.
   *
   * @param row initial vertical position
   * @param column initial horizontal position
   *
   * @return the newly created GPixelReference
   */
  public GPixelReference createGPixelReference(
    final int row,
    final int column)
  {
    return new GPixelReference(this, row, column);
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
  
  
}
