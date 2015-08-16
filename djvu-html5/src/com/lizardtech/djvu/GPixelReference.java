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
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.10 $
 */
public final class GPixelReference
  extends GPixel
{
  //~ Instance fields --------------------------------------------------------

  private GMap parent;

  /** The current byte position in the data array. */
  private int offset;
  
  private final int ncolors;
  private final int redOffset;
  private final int greenOffset;
  private final int blueOffset;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a createGPixelReference object.
   *
   * @param parent the image map to refere to
   * @param offset the initial pixel position to refere to
   */
  public GPixelReference(
    final GMap parent,
    final int  offset)
  {
    this.parent   = parent;
    this.ncolors  = parent.getColorSize();
    this.offset   = offset * ncolors;
    blueOffset=parent.getBlueOffset();
    greenOffset=parent.getGreenOffset();
    redOffset=parent.getRedOffset();
  }

  /**
   * Creates a createGPixelReference object.
   *
   * @param parent DOCUMENT ME!
   * @param row DOCUMENT ME!
   * @param column DOCUMENT ME!
   */
  public GPixelReference(
    final GMap parent,
    final int  row,
    final int  column)
  {
    this.parent   = parent;
    this.ncolors  = parent.getColorSize();
    this.offset   = (parent.rowOffset(row) + column) * ncolors;
    blueOffset=parent.getBlueOffset();
    greenOffset=parent.getGreenOffset();
    redOffset=parent.getRedOffset();
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Copy the pixel values.
   *
   * @param ref pixel to copy
   */
  public final void setPixels(final GPixelReference ref,int length)
  {
    if(ref.ncolors != ncolors ||
            ref.blueOffset != blueOffset ||
            ref.greenOffset != greenOffset || 
            ref.redOffset != redOffset)
    {
      while(length-- > 0)
      {
        set(ref);
        ref.incOffset();
        incOffset();
      }
    }
    else
    {
      System.arraycopy(ref.parent.data,ref.offset,parent.data,offset,length*ncolors);
      ref.incOffset(length);
      incOffset(length);
    }
  }

  /**
   * Set the map image pixel we are refering to.
   *
   * @param offset pixel position
   */
  public void setOffset(int offset)
  {
    this.offset = offset * ncolors;
  }

  /**
   * Set the map image pixel we are refering to.
   *
   * @param row vertical position
   * @param column horizontal position
   */
  public void setOffset(
    int row,
    int column)
  {
    this.offset = (parent.rowOffset(row) + column) * ncolors;
  }

  /**
   * Convert the following number of pixels from YCC to RGB. The offset will
   * be advanced to the end.
   *
   * @param count The number of pixels to convert.
   */
  public void YCC_to_RGB(int count)
  {
    if((ncolors != 3)||parent.isRampNeeded())
    {
        throw new IllegalStateException("YCC_to_RGB only legal with three colors");
    }
    while(count-- > 0)
    {
      final int y                = parent.data[offset];
      final int b                = parent.data[offset + 1];
      final int r                = parent.data[offset + 2];
      final int t2               = r + (r >> 1);
      final int t3               = (y + 128) - (b >> 2);
      final int b0               = t3 + (b << 1);
      parent.data[offset++] = (byte)((b0 < 255)
        ? ((b0 > 0)
        ? b0
        : 0)
        : 255);

      final int g0 = t3 - (t2 >> 1);
      parent.data[offset++] = (byte)((g0 < 255)
        ? ((g0 > 0)
        ? g0
        : 0)
        : 255);

      final int r0 = y + 128 + t2;
      parent.data[offset++] = (byte)((r0 < 255)
        ? ((r0 > 0)
        ? r0
        : 0)
        : 255);
    }
  }

  /**
   * Set the blue, green, and red values of the current pixel.
   *
   * @param blue pixel value
   * @param green pixel value
   * @param red pixel value
   */
  public void setBGR(
    final int blue,
    final int green,
    final int red)
  {
    parent.data[offset + blueOffset]  = (byte)blue;
    parent.data[offset + greenOffset] = (byte)green;
    parent.data[offset + redOffset]   = (byte)red;
  }

  /**
   * Set the blue pixel value.
   *
   * @param blue pixel value
   */
  public void setBlue(final byte blue)
  {
    parent.data[offset+blueOffset] = blue;
  }

  /**
   * Query the blue pixel value.
   *
   * @return blue pixel value
   */
  public byte blueByte()
  {
    return parent.data[offset+blueOffset];
  }

  /**
   * Create a duplicate of this GPixelReference.
   *
   * @return the newly created GPixelReference
   */
  public GPixelReference duplicate()
  {
    return new GPixelReference(parent, offset);
  }

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
    final int yrev = parent.rows() - y;

    if(! parent.isRampNeeded())
    {
      for(int y0 = yrev; y0-- > (yrev - h); off += scansize)
      {
        for(
          int i = off, j = (parent.rowOffset(y0) + x) * ncolors, k = w;
          k > 0;
          k--, j += ncolors)
        {
          pixels[i++] =
            0xff000000 | (0xff0000 & (parent.data[j + redOffset] << 16))
            | (0xff00 & (parent.data[j + greenOffset] << 8))
            | (0xff & parent.data[j + blueOffset]);
        }
      }
    }
    else
    {
      final GPixelReference ref=parent.createGPixelReference(0);
      for(int y0 = yrev; y0-- > (yrev - h); off += scansize)
      {
        ref.setOffset(y0,x);
        for(
          int i = off, k = w;
          k > 0;
          k--, ref.incOffset())
        {
          pixels[i++] = parent.ramp(ref).hashCode();
        }          
      }
    }
  }

  /**
   * Set the green pixel value.
   *
   * @param green pixel value
   */
  public void setGreen(final byte green)
  {
    parent.data[offset + greenOffset] = green;
  }

  /**
   * Query the green pixel value.
   *
   * @return green pixel value
   */
  public byte greenByte()
  {
    return parent.data[offset + greenOffset];
  }

  /**
   * Step to the next pixel.  Care should be taken when stepping past the end of a row.
   */
  public void incOffset()
  {
    this.offset += ncolors;
  }

  /**
   * Skip past the specified number of pixels.  Care should be taken when stepping 
   * past the end of a row.
   *
   * @param offset number of pixels to step past.
   */
  public void incOffset(final int offset)
  {
    this.offset += (ncolors * offset);
  }

  /**
   * Set the red pixel value.
   *
   * @param red pixel value
   */
  public void setRed(final byte red)
  {
    parent.data[offset + redOffset] = red;
  }

  /**
   * Query the red pixel value.
   *
   * @return red pixel value
   */
  public byte redByte()
  {
    return parent.data[offset + redOffset];
  }
}
