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

/**
 * A general class for rectange shapes.  By convention DjVu images are in bottom up
 * coordinates.  Thus, ymax corresponds to the top of a rectangle and ymin to the bottom.
 *
 * @author $author$
 * @version $Revision: 1.6 $
 */
public class GRect
  extends DjVuObject
  implements Cloneable
{
  //~ Instance fields --------------------------------------------------------

  /** Left edge */
  public int xmax;

  /** Right edge */
  public int xmin;

  /** Top edge. */
  public int ymax;

  /** Bottom edge. */
  public int ymin;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new GRect object.
   */
  public GRect()
  {
    xmin = xmax = ymin = ymax = 0;
  }

  /**
   * Creates a new GRect object.
   *
   * @param xmin left edge
   * @param ymin bottom edge
   * @param width horizontal length
   * @param height vertical length
   */
  public GRect(
    int xmin,
    int ymin,
    int width,
    int height)
  {
    this.xmin   = xmin;
    this.ymin   = ymin;
    xmax        = xmin + width;
    ymax        = ymin + height;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Query if the rectange is empty.
   *
   * @return true if no pixels are enclosed
   */
  public boolean isEmpty()
  {
    return (xmin >= xmax) || (ymin >= ymax);
  }

  /**
   * Compute the area of this rectangle.
   *
   * @return area, width()*height()
   */
  public long area()
  {
    return (long)(xmax - xmin) * (long)(ymax - ymin);
  }

  /**
   * Reset this rectange with all edges at the origin.
   */
  public void clear()
  {
    xmin = xmax = ymin = ymax = 0;
  }

  /**
   * Create a clone of this rectangle.
   *
   * @return the newly created copy
   */
  public Object clone()
  {
    //verbose("1. GRect clone");
    Cloneable retval = null;

    try
    {
      retval = (GRect)super.clone();
    }
    catch(final CloneNotSupportedException ignored) {}

    //verbose("2. GRect clone");
    return retval;
  }

  /**
   * Test if a point is contained in this rectangle.
   *
   * @param x horizontal coordinate
   * @param y vertical coordinate
   *
   * @return true if the point is within this rectangle
   */
  public boolean contains(
    int x,
    int y)
  {
    return (x >= xmin) && (x < xmax) && (y >= ymin) && (y < ymax);
  }

  /**
   * Test if a rectangle is contained within this rectangle.
   *
   * @param rect rectangle to test
   *
   * @return true if the rectangle is contained
   */
  public boolean contains(final GRect rect)
  {
    return rect.isEmpty()
    || (contains(rect.xmin, rect.ymin)
    && contains(rect.xmax - 1, rect.ymax - 1));
  }

  /**
   * Test if two rectangles are equal.
   *
   * @param ref reference rectangle to compare with
   *
   * @return true if all the edges are equal
   */
  public boolean equals(final Object ref)
  {
    if(ref instanceof GRect)
    {
      final GRect   r        = (GRect)ref;
      final boolean isempty1 = isEmpty();
      final boolean isempty2 = r.isEmpty();

      return ((isempty1 || isempty2) && isempty1 && isempty2)
      || ((xmin == r.xmin) && (xmax == r.xmax) && (ymin == r.ymin)
      && (ymax == r.ymax));
    }

    return false;
  }

  /**
   * Query the height of this rectangle.
   *
   * @return the rectangle height
   */
  public int height()
  {
    return ymax - ymin;
  }

  /**
   * Grow the size of this rectangle by moving all the edges outwards.
   *
   * @param dx Amount to grow the horizontal edges
   * @param dy Amount to grow the vertical edges
   *
   * @return true if not empty.
   */
  public boolean inflate(
    int dx,
    int dy)
  {
    xmin -= dx;
    xmax += dx;
    ymin -= dy;
    ymax += dy;

    if(!isEmpty())
    {
      return true;
    }
    else
    {
      xmin = ymin = xmax = ymax = 0;

      return false;
    }
  }

  /**
   * Set this rectangle as the intersection of two rectangles.
   *
   * @param rect1 rectangle to intersect
   * @param rect2 rectangle to intersect
   *
   * @return true if the intersection is not empty
   */
  public boolean intersect(
    GRect rect1,
    GRect rect2)
  {
    xmin   = Math.max(rect1.xmin, rect2.xmin);
    xmax   = Math.min(rect1.xmax, rect2.xmax);
    ymin   = Math.max(rect1.ymin, rect2.ymin);
    ymax   = Math.min(rect1.ymax, rect2.ymax);

    if(isEmpty())
    {
      xmin = ymin = xmax = ymax = 0;

      return false;
    }

    return true;
  }

  /**
   * Set this rectangle as the union of two rectangles.
   *
   * @param rect1 rectangle to union
   * @param rect2 rectangle to union
   *
   * @return true if the results are non-empty
   */
  public boolean recthull(
    GRect rect1,
    GRect rect2)
  {
    if(rect1.isEmpty())
    {
      xmin   = rect2.xmin;
      xmax   = rect2.xmax;
      ymin   = rect2.ymin;
      ymax   = rect2.ymax;

      return !isEmpty();
    }

    if(rect2.isEmpty())
    {
      xmin   = rect1.xmin;
      xmax   = rect1.xmax;
      ymin   = rect1.ymin;
      ymax   = rect1.ymax;

      return !isEmpty();
    }
    xmin   = Math.min(rect1.xmin, rect2.xmin);
    xmax   = Math.max(rect1.xmax, rect2.xmax);
    ymin   = Math.min(rect1.ymin, rect2.ymin);
    ymax   = Math.max(rect1.ymax, rect2.ymax);
    return true;
  }

  /**
   * Shift this rectangle
   *
   * @param dx horizontal distance to shift
   * @param dy vertical distance to shift
   *
   * @return true if not empty
   */
  public boolean translate(
    final int dx,
    final int dy)
  {
    if(!isEmpty())
    {
      xmin += dx;
      xmax += dx;
      ymin += dy;
      ymax += dy;
      return true;
    }
    xmin = ymin = xmax = ymax = 0;
    return false;
  }

  /**
   * Compute the width of this rectangle.
   *
   * @return the width of this rectangle.
   */
  public int width()
  {
    return xmax - xmin;
  }
}
