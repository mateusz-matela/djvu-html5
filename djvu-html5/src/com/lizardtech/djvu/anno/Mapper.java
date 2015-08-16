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
package com.lizardtech.djvu.anno;

import com.lizardtech.djvu.*;

/**
 * Maps points from one rectangle to another rectangle.  This class
 * represents a relation between the points of two rectangles. Given the
 * coordinates of a point in the first rectangle (input rectangle), function
 * map computes the coordinates of the corresponding point in the second
 * rectangle (the output rectangle).  This function actually implements an
 * affine transform which maps the corners of the first rectangle onto the
 * matching corners of the second rectangle. The scaling operation is
 * performed using integer fraction arithmetic in order to maximize
 * accuracy.
 */
public class Mapper
{
  //~ Static fields/initializers ---------------------------------------------

  private static final int MIRRORX = 1;
  private static final int MIRRORY = 2;
  private static final int SWAPXY  = 4;

  //~ Instance fields --------------------------------------------------------

  private final int[] rh = {0, 1};
  private final int[] rw = {0, 1};

  // Data
  private GRect rectFrom = new GRect(0, 0, 1, 1);
  private GRect rectTo = new GRect(0, 0, 1, 1);
  private int   code   = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a rectangle mapper.
   */
  public Mapper() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Resets the rectangle mapper state. Both the input rectangle and the
   * output rectangle are marked as undefined.
   */
  public void clear()
  {
    rectFrom   = new GRect(0, 0, 1, 1);
    rectTo     = new GRect(0, 0, 1, 1);
    code       = 0;
  }

  /**
   * Query the input rectangle.
   *
   * @return the input rectangle
   */
  public GRect getInput()
  {
    return rectFrom;
  }

  /**
   * Query the output rectangle.
   *
   * @return the output rectangle
   */
  public GRect getOutput()
  {
    return rectTo;
  }

  /**
   * Maps a rectangle according to the affine transform. This operation
   * consists in mapping the rectangle corners and reordering the corners in
   * the canonical rectangle representation.  Variable rect is overwritten
   * with the new rectangle coordinates.
   *
   * @param rect the rectangle to transform
   */
  public void map(final GRect rect)
  {
    final int x0 = mapX(rect.xmin, rect.ymin);
    final int y0 = mapY(rect.xmin, rect.ymin);
    final int x1 = mapX(rect.xmax, rect.ymax);
    final int y1 = mapY(rect.xmax, rect.ymax);

    if(x0 < x1)
    {
      rect.xmin   = x0;
      rect.xmax   = x1;
    }
    else
    {
      rect.xmin   = x1;
      rect.xmax   = x0;
    }

    if(y0 < y0)
    {
      rect.ymin   = y0;
      rect.ymax   = y1;
    }
    else
    {
      rect.ymin   = y1;
      rect.ymax   = y0;
    }
  }

  /**
   * Map a point and return the new x coordinate.
   *
   * @param x old x coordinate
   * @param y old y coordinate
   *
   * @return new x coordinate
   */
  public int mapX(
    final int x,
    final int y)
  {
    return map(x, y, true);
  }

  /**
   * Map a point and return the new y coordinate.
   *
   * @param x old x coordinate
   * @param y old y coordinate
   *
   * @return new y coordinate
   */
  public int mapY(
    final int x,
    final int y)
  {
    return map(x, y, false);
  }

  /**
   * Composes the affine transform with a symmetry with respect to the
   * vertical line crossing the center of the output rectangle.  This
   * operation essentially is a modification of the match between the
   * corners of the input rectangle and the corners of the output rectangle.
   */
  public void mirrorx()
  {
    code ^= MIRRORX;
  }

  /**
   * Composes the affine transform with a symmetry with respect to the
   * horizontal line crossing the center of the output rectangle.  This
   * operation essentially is a modification of the match between the
   * corners of the input rectangle and the corners of the output rectangle.
   */
  public void mirrory()
  {
    code ^= MIRRORY;
  }

  /**
   * Composes the affine transform with a rotation of count quarter turns
   * counter-clockwise.  This operation essentially is a modification of the
   * match between the corners of the input rectangle and the corners of the
   * output rectangle.
   *
   * @param count angle to rotate divided by 90
   */
  public void rotate(int count)
  {
    int oldcode = code;

    switch(count & 0x3)
    {
      case 1 :
        code ^= (((code & SWAPXY) != 0)
        ? MIRRORY
        : MIRRORX);
        code ^= SWAPXY;

        break;
      case 2 :
        code ^= (MIRRORX | MIRRORY);

        break;
      case 3 :
        code ^= (((code & SWAPXY) != 0)
        ? MIRRORX
        : MIRRORY);
        code ^= SWAPXY;

        break;
    }

    if(((oldcode ^ code) & SWAPXY) != 0)
    {
      {
        final int iswap = rectFrom.xmin;
        rectFrom.xmin   = rectFrom.ymin;
        rectFrom.ymin   = iswap;
      }

      ;

      {
        final int iswap = rectFrom.xmax;
        rectFrom.xmax   = rectFrom.ymax;
        rectFrom.ymax   = iswap;
      }

      ;
      rw[0]   = 0;
      rw[1]   = 1;
      rh[0]   = 0;
      rh[1]   = 1;
    }
  }

  /**
   * Composes the affine transform with a rotation of count quarter turns
   * counter-clockwise.  This operation essentially is a modification of the
   * match between the corners of the input rectangle and the corners of the
   * output rectangle.
   */
  public void rotate()
  {
    rotate(1);
  }

  /**
   * Sets the input rectangle.
   *
   * @param rect input rectangle
   *
   * @throws IllegalArgumentException if the rectangle is empty
   */
  public void setInput(final GRect rect)
  {
    if(rect.isEmpty())
    {
      throw new IllegalArgumentException("GRect.empty_rect1");
    }

    rectFrom = rect;

    if((code & SWAPXY) != 0)
    {
      {
        final int iswap = rectFrom.xmin;
        rectFrom.xmin   = rectFrom.ymin;
        rectFrom.ymin   = iswap;
      }

      ;

      {
        final int iswap = rectFrom.xmax;
        rectFrom.xmax   = rectFrom.ymax;
        rectFrom.ymax   = iswap;
      }

      ;
    }

    rw[0]   = 0;
    rw[1]   = 1;
    rh[0]   = 0;
    rh[1]   = 1;
  }

  /**
   * Sets the output rectangle.
   *
   * @param rect output rectangle
   *
   * @throws IllegalArgumentException if the rectangle is empty
   */
  public void setOutput(final GRect rect)
  {
    if(rect.isEmpty())
    {
      throw new IllegalArgumentException("GRect.empty_rect2");
    }

    rectTo   = rect;
    rw[0]    = 0;
    rw[1]    = 1;
    rh[0]    = 0;
    rh[1]    = 1;
  }

  /**
   * Maps a point according to the inverse of the affine transform. Variables
   * x and y initially contain the coordinates of a point. This operation
   * overwrites these variables with the coordinates of a second point
   * located in the same position relative to the corners of input rectangle
   * as the first point relative to the matching corners of the input
   * rectangle. Coordinates are rounded to the nearest integer.
   *
   * @param x horizontal coordinate
   * @param y vertical coordinate
   * @param needX true if x should be returned
   *
   * @return the transformed x or y
   */
  public int unmap(
    final int     x,
    final int     y,
    final boolean needX)
  {
    // precalc 
    if((rw[0] == 0) || (rh[0] == 0))
    {
      precalc();
    }

    // scale and translate
    int mx = rectFrom.xmin + DIVI((x - rectTo.xmin), rw);
    int my = rectFrom.ymin + DIVI((y - rectTo.ymin), rh);

    //  mirror and swap
    if((code & MIRRORX) != 0)
    {
      mx = (rectFrom.xmin + rectFrom.xmax) - mx;
    }

    if((code & MIRRORY) != 0)
    {
      my = (rectFrom.ymin + rectFrom.ymax) - my;
    }

    if((code & SWAPXY) != 0)
    {
      final int iswap = mx;
      mx   = my;
      my   = iswap;
    }

    ;

    return (needX
    ? mx
    : my);
  }

  /**
   * Maps a rectangle according to the inverse of the affine transform. This
   * operation consists in mapping the rectangle corners and reordering the
   * corners in the canonical rectangle representation.  Variable rect is
   * overwritten with the new rectangle coordinates.
   *
   * @param rect rectangle to unmap
   */
  public void unmap(final GRect rect)
  {
    final int x0 = unmapX(rect.xmin, rect.ymin);
    final int y0 = unmapY(rect.xmin, rect.ymin);
    final int x1 = unmapX(rect.xmax, rect.ymax);
    final int y1 = unmapY(rect.xmax, rect.ymax);

    if(x0 < x1)
    {
      rect.xmin   = x0;
      rect.xmax   = x1;
    }
    else
    {
      rect.xmin   = x1;
      rect.xmax   = x0;
    }

    if(y0 < y1)
    {
      rect.ymin   = y0;
      rect.ymax   = y1;
    }
    else
    {
      rect.ymin   = y1;
      rect.ymax   = y0;
    }
  }

  /**
   * Transform the specified point and return the x coordinate
   *
   * @param x horizontal coordinate
   * @param y vertical coordinate
   *
   * @return transformed x coordinate
   */
  public int unmapX(
    final int x,
    final int y)
  {
    return unmap(x, y, true);
  }

  /**
   * Transform the specified point and return the y coordinate
   *
   * @param x horizontal coordinate
   * @param y vertical coordinate
   *
   * @return transformed y coordinate
   */
  public int unmapY(
    final int x,
    final int y)
  {
    return unmap(x, y, false);
  }

  static int DIVI(
    int         n,
    final int[] r)
  {
    return (int)((((double)n * (double)r[1]) + ((double)r[0] / 2)) / (double)r[0]);
  }

  private static int MULT(
    int         n,
    final int[] r)
  {
    return (int)((((double)n * (double)r[0]) + ((double)r[1] / 2)) / (double)r[1]);
  }

  /**
   * Maps a point according to the affine transform.  Variables x and y
   * initially contain the coordinates of a point. This operation overwrites
   * these variables with the coordinates of a second point located in the
   * same position relative to the corners of the output rectangle as the
   * first point relative to the matching corners of the input rectangle.
   * Coordinates are rounded to the nearest integer.
   *
   * @param x horizontal coordinate
   * @param y vertical coordinate
   * @param needX true if x should be returned
   *
   * @return the transformed x or y coordinate
   */
  private int map(
    final int x,
    final int y,
    boolean   needX)
  {
    int mx = x;
    int my = y;

    // precalc
    if(((rw[0] == 0) || (rh[0] == 0)))
    {
      precalc();
    }

    // swap and mirror
    if((code & SWAPXY) != 0)
    {
      final int iswap = mx;
      mx   = my;
      my   = iswap;
    }

    ;

    if((code & MIRRORX) != 0)
    {
      mx = (rectFrom.xmin + rectFrom.xmax) - mx;
    }

    if((code & MIRRORY) != 0)
    {
      my = (rectFrom.ymin + rectFrom.ymax) - my;
    }

    // scale and translate
    return needX
    ? (rectTo.xmin + MULT((mx - rectFrom.xmin), rw))
    : (rectTo.ymin + MULT((my - rectFrom.ymin), rh));
  }

  private static void ratio(
    int[] retval,
    int   p,
    int   q)
  {
    if(q == 0)
    {
      new IllegalStateException("GRect.div_zero");
    }

    if(p == 0)
    {
      q = 1;
    }

    if(q < 0)
    {
      p   = -p;
      q   = -q;
    }

    int gcd = 1;
    int g1 = p;
    int g2 = q;

    if(g1 > g2)
    {
      gcd   = g1;
      g1    = g2;
      g2    = gcd;
    }

    while(g1 > 0)
    {
      gcd   = g1;
      g1    = g2 % g1;
      g2    = gcd;
    }

    retval[0]   = p / gcd;
    retval[1]   = q / gcd;
  }

  // Helper
  private void precalc()
  {
    if(rectTo.isEmpty() || rectFrom.isEmpty())
    {
      throw new IllegalStateException("GRect.empty_rect3");
    }

    ratio(
      rw,
      rectTo.width(),
      rectFrom.width());
    ratio(
      rh,
      rectTo.height(),
      rectFrom.height());
  }
}
