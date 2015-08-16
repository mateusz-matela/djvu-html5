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
import java.util.*;


/**
 * Implements polygonal map areas. The only supported types of border are
 * NO_BORDER, XOR_BORDER and SOLID_BORDER. Its contents can not be
 * highlighted either. It's worth mentioning here that despite its name the
 * polygon may be open, which basically makes it a broken line. This very
 * specific mode is used by the hyperlink editor when creating the polygonal
 * hyperlink.
 */
public class Poly
  extends Rect
{
  //~ Static fields/initializers ---------------------------------------------

  /** The tag name for this shape. */
  public static final String POLY_TAG = "poly";

  //~ Instance fields --------------------------------------------------------

  private Boolean hasValidData = null;
  private GRect   bounds      = null;
  private Vector  pointVector = new Vector();
  private boolean open        = false;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Poly object.
   */
  public Poly() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query the bounding rectangle for this shape.
   *
   * @return the bounding rectangle
   */
  public GRect getBounds()
  {
    GRect retval = bounds;

    if(bounds == null)
    {
      int[] point = getPoint(0);
      int   xmin = point[0];
      int   xmax = xmin;
      int   ymin = point[1];
      int   ymax = ymin;

      for(int i = 1; i < pointVector.size(); i++)
      {
        point = getPoint(i);

        if(xmin > point[0])
        {
          xmin = point[0];
        }
        else if(xmax < point[0])
        {
          xmax = point[0];
        }

        if(ymin > point[1])
        {
          ymin = point[1];
        }
        else if(ymax < point[1])
        {
          ymax = point[1];
        }
      }

      bounds = retval = new GRect(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    return retval;
  }

  /**
   * Query the map type.
   *
   * @return MAP_POLY
   */
  public int getMapType()
  {
    return MAP_POLY;
  }

  /**
   * Creates an instance of Poly with the options interherited from the
   * specified reference.
   * 
   * @param ref Object to interherit DjVuOptions from.
   * 
   * @return a new instance of Poly.
   */
  public static Poly createPoly(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (Poly)create(
      options,
      options.getAnnoPolyClass(),
      Poly.class);
  }

  /**
   * Query if the poly is closed
   *
   * @return true if closed or does not have valid data.
   */
  public boolean isEmpty()
  {
    return !(isOpen()||isDataValid());
  }

  /**
   * Checks validity of the polygon
   *
   * @return true if there are enough segments, all of which are valid.
   */
  public boolean isDataValid()
  {
    Boolean retval = hasValidData;

    if(retval == null)
    {
      final int n = pointVector.size();

      if((n < 2) || ((n < 3) && !isOpen()))
      {
        hasValidData = new Boolean(false);

        return false;
      }

      for(int i = 0; i < size(); i++)
      {
        for(int j = i + 2; j < size(); j++)
        {
          if(i != ((j + 1) % pointVector.size()))
          {
            if(
              do_segments_intersect(
                getPoint(i)[0],
                getPoint(i)[1],
                getPoint(i + 1)[0],
                getPoint(i + 1)[1],
                getPoint(j),
                getPoint(j + 1)))
            {
              hasValidData = new Boolean(false);

              return false;
            }
          }
        }
      }
      retval=hasValidData=new Boolean(true);
    }

    return retval.booleanValue();
  }

  /**
   * Closes or open the polygon
   *
   * @param open true if this should be an open polygon
   */
  public void setOpen(final boolean open)
  {
    this.open = open;
  }

  /**
   * Query if this is an open polygon.
   *
   * @return true if an open polygon
   */
  public boolean isOpen()
  {
    return open;
  }

  /**
   * Method generating a list of defining coordinates
   *
   * @return the vector of points
   */
  public Vector getPoints()
  {
    return pointVector;
  }

  /**
   * Query the shape tag name
   *
   * @return POLY_TAG
   */
  public String getShapeName()
  {
    return POLY_TAG;
  }

  /**
   * Checks if the object is OK.
   *
   * @return true if valid
   */
  public boolean isValid()
  {
    final int    border_type  = getBorderType();
    final int hilite_color = getHiliteColor();

    return ((border_type == NO_BORDER) || (border_type == SOLID_BORDER)
    || (border_type == XOR_BORDER))
    && (hilite_color == 0xffffffff)
    && super.isValid();
  }

  /**
   * Adds a new vertex and returns number of vertices in the polygon
   *
   * @param x horizontal position
   * @param y vertical position
   *
   * @return the number of points
   */
  public int add_vertex(
    int x,
    int y)
  {
    final int[] point = {x, y};
    pointVector.addElement(point);

    return pointVector.size();
  }

  /**
   * Check if the point is inside the hyperlink area
   *
   * @param xin horizontal position
   * @param yin vertical position
   *
   * @return true if the given point is inside the hyperlink area
   */
  public boolean contains(
    final int xin,
    final int yin)
  {
    if(isOpen())
    {
      return false;
    }

    final GRect bounds = getBounds();

    if(!bounds.contains(xin, yin))
    {
      return false;
    }
    
    // This algorithm looks complicated but is really quite simple.  If we draw a line straight
    // up from our point it will cross an odd number of segments iff the point is inside the
    // polygon.
    boolean retval=false;
    final int s=pointVector.size();
    for(int i=s,j=0;--i >= 0;j=i)
    {
      final int [] point1=getPoint(i);
      final int [] point2=getPoint(j);
      if(point1[0] < point2[0])
      {
        if((xin < point1[0])||(xin > point2[0]))
        {
          continue;
        }
      }
      else if((xin < point2[0])||(xin > point1[0]))
      {
        continue;
      }
      if(point2[0] == point1[0])
      {
        if(point1[0] > yin)
        {
          continue;
        }
      }
      else
      {
        final int y=point1[1]+(int)Math.ceil(((double)(point2[1]-point1[1])/(double)(point2[0]-point1[0]))*(double)(xin-point1[0]));
        if(y > yin)
        {
          continue;
        }
        if(point1[1] < point2[1])
        {
          if((y < point1[1])||(y > point2[1]))
          {
            continue;
          }
        }
        else if((y < point2[1])||(y > point1[1]))
        {
          continue;
        }
      }
      retval=!retval;
    }
    return retval;
  }

  /**
   * Query if side crosses the specified rectangle rect.
   *
   * @param grect the rectangle to check
   * @param side the side number to check
   *
   * @return true if side crosses the specified rectangle rect.
   */
  public boolean does_side_cross_rect(
    final GRect grect,
    int         side)
  {
    int[]     point1 = getPoint(side);
    int[]     point2 = getPoint(side + 1);
    final int xmin   = (point1[0] < point2[0])
      ? point1[0]
      : point2[0];
    final int ymin = (point1[1] < point2[1])
      ? point1[1]
      : point2[1];
    final int xmax = (point1[0] + point2[0]) - xmin;
    final int ymax = (point1[1] + point2[1]) - ymin;

    if(
      (xmax < grect.xmin)
      || (xmin > grect.xmax)
      || (ymax < grect.ymin)
      || (ymin > grect.ymax))
    {
      return false;
    }

    return ((point1[0] >= grect.xmin) && (point1[0] <= grect.xmax)
    && (point1[1] >= grect.ymin) && (point1[1] <= grect.ymax))
    || ((point2[0] >= grect.xmin) && (point2[0] <= grect.xmax)
    && (point2[1] >= grect.ymin) && (point2[1] <= grect.ymax))
    || do_segments_intersect(
      grect.xmin,
      grect.ymin,
      grect.xmax,
      grect.ymax,
      point1,
      point2)
    || do_segments_intersect(
      grect.xmax,
      grect.ymin,
      grect.xmin,
      grect.ymax,
      point1,
      point2);
  }

  /**
   * Initialize from specified coordinates.
   * 
   * @param xx array of horizontal positions
   * @param yy array of vertical positions
   * @param points number  of points
   * @param open true if an open polygon
   * 
   * @return the initialized Poly
   */
  public Poly init(
    final int[]   xx,
    final int[]   yy,
    final int     points,
    final boolean open)
  {
    setOpen(open);
    
    int i=0;
    for(; i < points-1; i++)
    {
      add_vertex(xx[i], yy[i]);
    }
    if((xx[i] != xx[0])||(yy[i] != yy[0]))
    {
      add_vertex(xx[i], yy[i]);        
    }
    optimize_data();

    return this;
  }

  /**
   * Initialize a closed polygon from specified coordinates.
   * 
   * @param xx array of horizontal positions
   * @param yy array of vertical positions
   * @param points number  of points
   * 
   * @return the initialized Poly
   */
  public Poly init(
    final int[] xx,
    final int[] yy,
    final int   points)
  {
    return init(xx, yy, points, false);
  }

  /**
   * Method maps polygon from one area to another using mapper
   *
   * @param mapper used to remap the points
   */
  public void map(Mapper mapper)
  {
    for(int i = 0; i < pointVector.size(); i++)
    {
      final int[] point = getPoint(i);
      point[0]   = mapper.mapX(point[0], point[1]);
      point[1]   = mapper.mapY(point[0], point[1]);
    }

    reset();
  }

  /**
   * Move all the points of the polygon.
   *
   * @param dx Delta x value to add.
   * @param dy Delta y value to add.
   */
  public void move(
    int dx,
    int dy)
  {
    if((dx != 0) || (dy != 0))
    {
      for(int i = 0; i < pointVector.size(); i++)
      {
        final int[] point = getPoint(i);
        point[0] += dx;
        point[1] += dy;
      }

      if(bounds != null)
      {
        super.move(dx, dy);
      }
    }
  }

  /**
   * Moves vertex i to position (x, y)
   *
   * @param i point to move
   * @param x horizontal position
   * @param y vertical position
   */
  public void move_vertex(
    final int i,
    final int x,
    final int y)
  {
    final int[] point = getPoint(i);
    point[0]   = x;
    point[1]   = y;
    reset();
  }

  /**
   * Optimizes the polygon
   */
  public void optimize_data()
  {
    // Removing segments of length zero
    for(int i=0;i < size();)
    {
      if(
        (getPoint(i)[0] == getPoint(i + 1)[0])
        && (getPoint(i)[1] == getPoint(i + 1)[1]))
      {
        removePoint(i);
      }
      else
      {
        i++;
      }
    }

    // Concatenating consequitive parallel segments
    for(int i = 0; i < size();)
    {
      if(
        ((isOpen() && ((i + 1) < size())) || !isOpen())
        && are_segments_parallel(
          getPoint(i),
          getPoint(i + 1),
          getPoint(i + 1),
          getPoint(i + 2)))
      {
        removePoint(i + 1);
      }
      else
      {
        i++;
      }
    }
  }

  /**
   * Rescale the polygon to the specified bounds size.
   *
   * @param width horizontal length to scale to
   * @param height vertical length to scale to
   */
  public void resize(
    final int width,
    final int height)
  {
    final GRect bounds = getBounds();
    reset();

    final int xwidth  = bounds.width();
    final int xheight = bounds.height();

    if((xwidth != width) || (xheight != height))
    {
      for(int i = 0; i < pointVector.size(); i++)
      {
        final int[] point = getPoint(i);
        point[0] =
          bounds.xmin + (((point[0] - bounds.xmin) * width) / xwidth);
        point[1] =
          bounds.ymin + (((point[1] - bounds.ymin) * height) / xheight);
      }
    }
  }

  /**
   * Rotate this area about the specified origin
   *
   * @param rot angle to rotate divided by 90
   * @param cx horizontal position of the center of rotation
   * @param cy vertical position of the center of rotation
   */
  public void rotateArea(
    int rot,
    final int cx,
    final int cy)
  {
    if(rot == 0)
    {
      return;
    }

    rot = rot % 4;

    int temp = 0;
    int i = 0;

    switch(rot)
    {
      case 1 :
      {
        //rotate
        for(i = 0; i < pointVector.size(); i++)
        {
          final int[] point = getPoint(i);
          temp       = point[0];
          point[0]   = cy - point[1];
          point[1]   = temp;
        }
      }

      break;
      case 2 :
      {
        //rotate
        for(i = 0; i < pointVector.size(); i++)
        {
          final int[] point = getPoint(i);
          point[0]   = cx - point[0];
          point[1]   = cy - point[1];
        }
      }

      break;
      case 3 :
      {
        for(i = 0; i < pointVector.size(); i++)
        {
          final int[] point = getPoint(i);
          temp       = point[0];
          point[0]   = point[1];
          point[1]   = cx - temp;
        }
      }

      break;
    }

    reset();
  }

  /**
   * Query the number sides in the polygon
   *
   * @return the number sides in the polygon
   */
  public int size()
  {
    return isOpen()
    ? (pointVector.size() - 1)
    : pointVector.size();
  }

  /**
   * Move and scale this polygon to the specified bounding rectangle.
   *
   * @param grect the new bounding rectangle
   */
  public void transform(final GRect grect)
  {
    final GRect bounds = getBounds();

    if(
      (grect.xmin != bounds.xmin)
      || (grect.ymin != bounds.ymin)
      || (grect.xmax != bounds.xmax)
      || (grect.ymax != bounds.ymax))
    {
      final int width  = bounds.xmax - bounds.xmin;
      final int height = bounds.ymax - bounds.ymin;
      final int xmin   = bounds.xmin;
      final int ymin   = bounds.ymin;

      for(int i = 0; i < pointVector.size(); i++)
      {
        final int[] point = getPoint(i);
        point[0]   = grect.xmin
          + (((point[0] - xmin) * grect.width()) / width);
        point[1] =
          grect.ymin + (((point[1] - ymin) * grect.height()) / height);
      }

      reset();
    }
  }

  /**
   * Method unmaps polygon from one area to another using mapper
   *
   * @param mapper the mapper to transform the coordinates
   */
  public void unmap(Mapper mapper)
  {
    for(int i = 0; i < pointVector.size(); i++)
    {
      final int[] point = getPoint(i);
      point[0]   = mapper.unmapX(point[0], point[1]);
      point[1]   = mapper.unmapY(point[0], point[1]);
    }

    reset();
  }

  /**
   * Resets cached results.
   */
  public void reset()
  {
    bounds         = null;
    hasValidData   = null;
    super.reset();
  }

  // Query the starting point for the specified side.
  private int[] getPoint(final int side)
  {
    return (int[])pointVector.elementAt(side % pointVector.size());
  }

  // Query if two segments are parallel
  private static boolean are_segments_parallel(
    final int[] point11,
    final int[] point12,
    final int[] point21,
    final int[] point22)
  {
    return (((point12[0] - point11[0]) * (point22[1] - point21[1]))
    - ((point12[1] - point11[1]) * (point22[0] - point21[0]))) == 0;
  }

  // Query if segments intersect
  private static boolean do_segments_intersect(
    int   x11,
    int   y11,
    int   x12,
    int   y12,
    int[] point21,
    int[] point22)
  {
    int res11 =
      ((x11 - point21[0]) * (point22[1] - point21[1]))
      - ((y11 - point21[1]) * (point22[0] - point21[0]));
    int res12 =
      ((x12 - point21[0]) * (point22[1] - point21[1]))
      - ((y12 - point21[1]) * (point22[0] - point21[0]));
    int res21 =
      ((point21[0] - x11) * (y12 - y11)) - ((point21[1] - y11) * (x12 - x11));
    int res22 =
      ((point22[0] - x11) * (y12 - y11)) - ((point22[1] - y11) * (x12 - x11));

    if((res11 == 0) && (res12 == 0))
    {
      // Segments are on the same line
      return is_projection_on_segment(
        x11,
        y11,
        point21[0],
        point21[1],
        point22[0],
        point22[1])
      || is_projection_on_segment(
        x12,
        y12,
        point21[0],
        point21[1],
        point22[0],
        point22[1])
      || is_projection_on_segment(point21[0], point21[1], x11, y11, x12, y12)
      || is_projection_on_segment(point22[0], point22[1], x11, y11, x12, y12);
    }

    int sign1 = sign(res11) * sign(res12);
    int sign2 = sign(res21) * sign(res22);

    return (sign1 <= 0) && (sign2 <= 0);
  }

  // Query if a projection
  private static boolean is_projection_on_segment(
    int x,
    int y,
    int x1,
    int y1,
    int x2,
    int y2)
  {
    int res1 = ((x - x1) * (x2 - x1)) + ((y - y1) * (y2 - y1));
    int res2 = ((x - x2) * (x2 - x1)) + ((y - y2) * (y2 - y1));

    return ((sign(res1) * sign(res2)) <= 0);
  }

  // remove a point
  private void removePoint(final int side)
  {
    pointVector.removeElementAt(side % pointVector.size());
  }

  // sign function
  private static int sign(final int x)
  {
    return (x < 0)
    ? (-1)
    : ((x > 0)
    ? 1
    : 0);
  }
}
