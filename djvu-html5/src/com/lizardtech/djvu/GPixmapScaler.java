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

import java.beans.*;
import java.io.*;
import java.net.URL;
import java.util.*;


/**
 * The GPixmapScaler class provides a method to rescale portions of GPixmap
 * images.
 *
 * @author $author$
 * @version $Revision: 1.4 $
 */
class GPixmapScaler
{
  //~ Static fields/initializers ---------------------------------------------

  private static final int       FRACBITS  = 4;
  private static final int       FRACSIZE  = (1 << FRACBITS);
  private static final int       FRACSIZE2 = (FRACSIZE >> 1);
  private static final int       FRACMASK  = (FRACSIZE - 1);
  private static final short[][] interp    = new short[FRACSIZE][512];

  static
  {
    for(int i = 0; i < FRACSIZE; i++)
    {
      final short[] deltas = interp[i];

      for(int j = -255; j <= 255; j++)
      {
        deltas[256 + j] = (short)(((j * i) + FRACSIZE2) >> FRACBITS);
      }
    }
  }

  //~ Instance fields --------------------------------------------------------

  private GPixmap p1     = null;
  private GPixmap p2     = null;
  private int[]   hcoord = null;

  // Fixed point coordinates
  private int[] vcoord     = null;
  private int   destHeight = 0;
  private int   destWidth  = 0;

  // Temporaries
  private int l1        = (-1);
  private int l2        = (-1);
  private int redh      = 0;
  private int redw      = 0;
  private int srcHeight = 0;

  // The sizes
  private int srcWidth = 0;
  private int xshift = 0;
  private int yshift = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Scaler object.
   */
  public GPixmapScaler() {}

  /**
   * Creates a new GPixmapScaler object.
   *
   * @param srcWidth width of the source image
   * @param srcHeight height of the source image
   * @param destWidth width of the target image
   * @param destHeight height of the target image
   */
  public GPixmapScaler(
    final int srcWidth,
    final int srcHeight,
    final int destWidth,
    final int destHeight)
  {
    setSrcSize(srcWidth, srcHeight);
    setDestSize(destWidth, destHeight);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Sets the size of the output image.  This size is used to initialize the
   * internal data structures of the scaler object.
   *
   * @param destWidth width of the destination image map.
   * @param destHeight height of the destination image map.
   */
  final void setDestSize(
    final int destWidth,
    final int destHeight)
  {
    this.destWidth    = destWidth;
    this.destHeight   = destHeight;
    vcoord            = null;
    hcoord            = null;
  }

  /**
   * Sets the horizontal scaling ratio #numer/denom#.  This function may be
   * used to force an exact scaling ratio.  The scaling ratios are otherwise
   * derived from the sizes of the input and output images.
   *
   * @param numer upsample rate
   * @param denom subsample rate
   *
   * @throws IllegalStateException if an error occurs
   * @throws IllegalArgumentException if an error occurs
   */
  final void setHorzRatio(
    int numer,
    int denom)
  {
    if(
      (srcWidth <= 0)
      || (srcHeight <= 0)
      || (destWidth <= 0)
      || (destHeight <= 0))
    {
      throw new IllegalStateException("Scaler undefined size");
    }

    // Implicit ratio (determined by the input/output sizes)
    if((numer == 0) && (denom == 0))
    {
      numer   = destWidth;
      denom   = srcWidth;
    }
    else if((numer <= 0) || (denom <= 0))
    {
      throw new IllegalArgumentException("Scaler illegal ratio");
    }

    // Compute horz reduction
    xshift   = 0;
    redw     = srcWidth;

    while((numer + numer) < denom)
    {
      xshift++;
      redw = (redw + 1) >> 1;
      numer <<= 1;
    }

    // Compute coordinate table
    if(hcoord == null)
    {
      hcoord = new int[destWidth];
    }

    prepare_coord(hcoord, redw, destWidth, denom, numer);
  }

  /**
   * Computes which input pixels are required to compute specified output
   * pixels.  Let us assume that we only need a part of the output image.
   * This part is defined by rectangle targetRect.  Only a part of the input
   * image is necessary to compute the output pixels.  This  method computes
   * the coordinates of that part of the input image, and stores them into
   * rectangle sourceRect.
   *
   * @param targetRect Bounding rectangle for the target output.
   *
   * @return Bounding rectangle of portion of the input rectangle used.
   */
  final GRect getRequiredRect(final GRect targetRect)
  {
    final GRect red = new GRect();

    return createRectangles(targetRect, red);
  }

  /**
   * Sets the size of the input image.  This size is used to initialize the
   * internal data structures of the  scaler object.
   *
   * @param srcWidth width of the source image map.
   * @param srcHeight height of the source image map.
   */
  final void setSrcSize(
    final int srcWidth,
    final int srcHeight)
  {
    this.srcWidth    = srcWidth;
    this.srcHeight   = srcHeight;
    vcoord           = null;
    hcoord           = null;
  }

  /**
   * Sets the vertical scaling ratio to #numer/denom#.  This function may be
   * used to force an exact scaling ratio.  The scaling ratios are otherwise
   * derived from the sizes of the input and output images.
   *
   * @param numer numerator
   * @param denom denominator
   *
   * @throws IllegalStateException if an error occurs
   * @throws IllegalArgumentException if an error occurs
   */
  final void setVertRatio(
    int numer,
    int denom)
  {
    if(
      (srcWidth <= 0)
      || (srcHeight <= 0)
      || (destWidth <= 0)
      || (destHeight <= 0))
    {
      throw new IllegalStateException("Scaler undefined size");
    }

    // Implicit ratio (determined by the input/output sizes)
    if((numer == 0) && (denom == 0))
    {
      numer   = destHeight;
      denom   = srcHeight;
    }
    else if((numer <= 0) || (denom <= 0))
    {
      throw new IllegalArgumentException("Scaler illegal ratio");
    }

    // Compute horz reduction
    yshift   = 0;
    redh     = srcHeight;

    while((numer + numer) < denom)
    {
      yshift++;
      redh = (redh + 1) >> 1;
      numer <<= 1;
    }

    // Compute coordinate table
    if(vcoord == null)
    {
      vcoord = new int[destHeight];
    }

    prepare_coord(vcoord, redh, destHeight, denom, numer);
  }

  // Helper
  final GRect createRectangles(
    final GRect desired,
    final GRect red)
  {
    final GRect inp = new GRect();

    // Parameter validation
    if(
      (desired.xmin < 0)
      || (desired.ymin < 0)
      || (desired.xmax > destWidth)
      || (desired.ymax > destHeight))
    {
      throw new IllegalArgumentException(
        "desired rectangle too big: " + desired.xmin + "," + desired.ymin
        + "," + desired.xmax + "," + desired.ymax + "," + destWidth + ","
        + destHeight);
    }

    // Compute ratio (if not done yet)
    if(vcoord == null)
    {
      setVertRatio(0, 0);
    }

    if(hcoord == null)
    {
      setHorzRatio(0, 0);
    }

    // Compute reduced bounds
    red.xmin   = (hcoord[desired.xmin]) >> FRACBITS;
    red.ymin   = (vcoord[desired.ymin]) >> FRACBITS;
    red.xmax   = ((hcoord[desired.xmax - 1] + FRACSIZE) - 1) >> FRACBITS;
    red.ymax   = ((vcoord[desired.ymax - 1] + FRACSIZE) - 1) >> FRACBITS;

    // Borders
    red.xmin   = (red.xmin > 0)
      ? red.xmin
      : 0;
    red.xmax   = (red.xmax < redw)
      ? (red.xmax + 1)
      : redw;
    red.ymin   = (red.ymin > 0)
      ? red.ymin
      : 0;
    red.ymax   = (red.ymax < redh)
      ? (red.ymax + 1)
      : redh;

    // Input
    inp.xmin = red.xmin << xshift;

    if(inp.xmin < 0)
    {
      inp.xmin = 0;
    }

    inp.xmax = red.xmax << xshift;

    if(inp.xmax > srcWidth)
    {
      inp.xmax = srcWidth;
    }

    inp.ymin = red.ymin << yshift;

    if(inp.ymin < 0)
    {
      inp.ymin = 0;
    }

    inp.ymax = red.ymax << yshift;

    if(inp.ymax > srcHeight)
    {
      inp.ymax = srcHeight;
    }

    return inp;
  }

  /**
   * Computes a segment of the rescaled output image.  The pixmap #output# is
   * overwritten with the segment of the output image specified by the
   * rectangle #targetRect#.  The rectangle #provided_input# specifies which
   * segment of the sourceMap image is provided in the pixmap #input#. An
   * exception \Ref{GException} is thrown if the rectangle #provided_input#
   * is smaller then the rectangle #sourceRect# returned by function
   * \Ref{GPixmapScaler::getSourceRect}.
   *
   * @param provided_input input rectangle
   * @param sourceMap input image map
   * @param targetRect target rectangle
   * @param output target image map
   *
   * @throws IllegalArgumentException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  final void scale(
    final GRect   provided_input,
    final GPixmap sourceMap,
    final GRect   targetRect,
    final GPixmap output)
  {
    // Compute rectangles
    final GRect required_red = new GRect();
    final GRect sourceRect = createRectangles(targetRect, required_red);

    // Parameter validation
    if(
      (provided_input.width() != sourceMap.columns())
      || (provided_input.height() != sourceMap.rows()))
    {
      throw new IllegalArgumentException("invalid rectangle");
    }

    if(
      (provided_input.xmin > sourceRect.xmin)
      || (provided_input.ymin > sourceRect.ymin)
      || (provided_input.xmax < sourceRect.xmax)
      || (provided_input.ymax < sourceRect.ymax))
    {
      throw new IllegalStateException("invalid rectangle");
    }

    // Adjust output pixmap
    if(
      (targetRect.width() != (int)output.columns())
      || (targetRect.height() != (int)output.rows()))
    {
      output.init(
        targetRect.height(),
        targetRect.width(),
        null);
    }

    // Prepare temp stuff 
    final int bufw    = required_red.width();
    GPixel[]  lbuffer = new GPixel[bufw + 2];

    for(int i = 0; i < lbuffer.length;)
    {
      lbuffer[i++] = new GPixel();
    }

    try
    {
      if((xshift > 0) || (yshift > 0))
      {
        p1   = GPixmap.createGPixmap(output).init(1, bufw, null);
        p2   = GPixmap.createGPixmap(output).init(2, bufw, null);
        l1   = l2 = -1;
      }

      // Loop on output lines
      for(int y = targetRect.ymin; y < targetRect.ymax; y++)
      {
        // Perform vertical interpolation
        {
          int             fy    = vcoord[y];
          int             fy1   = fy >> FRACBITS;
          int             fy2   = fy1 + 1;
          GPixelReference upper;
          GPixelReference lower;

          // Obtain upper and lower line in reduced image
          if((xshift > 0) || (yshift > 0))
          {
            lower   = get_line(fy1, required_red, provided_input, sourceMap);
            upper   = get_line(fy2, required_red, provided_input, sourceMap);
          }
          else
          {
            int dx = required_red.xmin - provided_input.xmin;

            if(required_red.ymin > fy1)
            {
              fy1 = required_red.ymin;
            }

            if(required_red.ymax <= fy2)
            {
              fy2 = required_red.ymax - 1;
            }

            lower =
              sourceMap.createGPixelReference(fy1 - provided_input.ymin, dx);
            upper =
              sourceMap.createGPixelReference(fy2 - provided_input.ymin, dx);
          }

          // Compute line
          int           idest  = 1;
          final short[] deltas = interp[fy & FRACMASK];

          for(
            int edest = idest + bufw;
            idest < edest;
            upper.incOffset(), lower.incOffset())
          {
            final GPixel dest    = lbuffer[idest++];
            final int    lower_r = lower.getRed();
            final int    delta_r = deltas[(256 + upper.getRed()) - lower_r];
            final int    lower_g = lower.getGreen();
            final int    delta_g = deltas[(256 + upper.getGreen()) - lower_g];
            final int    lower_b = lower.getBlue();
            final int    delta_b = deltas[(256 + upper.getBlue()) - lower_b];
            dest.setBGR(lower_b + delta_b, lower_g + delta_g, lower_r + delta_r);
          }
        }

        // Perform horizontal interpolation
        {
          // Prepare for side effects
          lbuffer[0] = lbuffer[1];

          // lbuffer[bufw] = lbuffer[bufw];
          int                   line = 1 - required_red.xmin;
          final GPixelReference dest =
            output.createGPixelReference(y - targetRect.ymin, 0);

          // Loop horizontally
          for(int x = targetRect.xmin; x < targetRect.xmax; x++)
          {
            final int     n       = hcoord[x];
            final int     lower   = line + (n >> FRACBITS);
            final GPixel  lower0  = lbuffer[lower];
            final GPixel  lower1  = lbuffer[lower + 1];
            final short[] deltas  = interp[n & FRACMASK];
            final int     lower_r = lower0.getRed();
            final int     delta_r = deltas[(256 + lower1.getRed()) - lower_r];
            final int     lower_g = lower0.getGreen();
            final int     delta_g = deltas[(256 + lower1.getGreen()) - lower_g];
            final int     lower_b = lower0.getBlue();
            final int     delta_b = deltas[(256 + lower1.getBlue()) - lower_b];
            dest.setBGR(lower_b + delta_b, lower_g + delta_g, lower_r + delta_r);
            dest.incOffset();
          }
        }
      }
    }
    finally
    {
      p1   = null;
      p2   = null;
    }
  }

  private static void prepare_coord(
    int[] coord,
    int   inmax,
    int   outmax,
    int   in,
    int   out)
  {
    int len = (in * FRACSIZE);
    int beg = ((len + out) / (2 * out)) - FRACSIZE2;

    // Bresenham algorithm
    int y        = beg;
    int z        = out / 2;
    int inmaxlim = (inmax - 1) * FRACSIZE;

    for(int x = 0; x < outmax; x++)
    {
      coord[x]   = (y < inmaxlim)
        ? y
        : inmaxlim;
      z   = z + len;
      y   = y + (z / out);
      z   = z % out;
    }

    // Result must fit exactly
    if((out == outmax) && (y != (beg + len)))
    {
      throw new IllegalStateException("Scaler assertion");
    }
  }

  // Helpers
  private GPixelReference get_line(
    int           fy,
    final GRect   required_red,
    final GRect   provided_input,
    final GPixmap input)
  {
    if(fy < required_red.ymin)
    {
      fy = required_red.ymin;
    }
    else if(fy >= required_red.ymax)
    {
      fy = required_red.ymax - 1;
    }

    // Cached line
    if(fy == l2)
    {
      return p2.createGPixelReference(0);
    }

    if(fy == l1)
    {
      return p1.createGPixelReference(0);
    }

    // Shift
    GPixmap p = p1;
    p1   = p2;
    l1   = l2;
    p2   = p;
    l2   = fy;

    // Compute location of line
    final GRect line = new GRect();
    line.xmin   = required_red.xmin << xshift;
    line.xmax   = required_red.xmax << xshift;
    line.ymin   = fy << yshift;
    line.ymax   = (fy + 1) << yshift;
    line.intersect(line, provided_input);
    line.translate(-provided_input.xmin, -provided_input.ymin);

    // Prepare variables
    final int             botline = input.rowOffset(line.ymin);
    final int             rowsize = input.getRowSize();
    final int             sw      = 1 << xshift;
    final int             div     = xshift + yshift;
    final int             rnd     = 1 << (div - 1);
    final int             rnd2    = rnd + rnd;

    final GPixelReference inp1 = input.createGPixelReference(0);
    final GPixelReference ip   = p.createGPixelReference(0);

    // Compute averages
    for(int x = line.xmin; x < line.xmax; x += sw, ip.incOffset())
    {
      int       r    = 0;
      int       g    = 0;
      int       b    = 0;
      int       s    = 0;
      int       inp0 = botline + x;
      final int sy2  = line.height();
      int       sy1  = (1 << yshift);

      if(sy1 > sy2)
      {
        sy1 = sy2;
      }

      for(int sy = 0; sy < sy1; sy++, inp0 += rowsize)
      {
        int sx1 = x + sw;
        inp1.setOffset(inp0);

        if(sx1 > line.xmax)
        {
          sx1 = line.xmax;
        }

        for(int sx = sx1 - x; sx-- > 0; s++, inp1.incOffset())
        {
          r += inp1.getRed();
          g += inp1.getGreen();
          b += inp1.getBlue();
        }
      }

      if(s == rnd2)
      {
        ip.setBGR((b + rnd) >> div, (g + r) >> div, (r + rnd) >> div);
      }
      else
      {
        ip.setBGR((b + (s / 2)) / 2, (g + (s / 2)) / s, (r + (s / 2)) / s);
      }
    }

    // Return
    return p2.createGPixelReference(0);
  }
}
