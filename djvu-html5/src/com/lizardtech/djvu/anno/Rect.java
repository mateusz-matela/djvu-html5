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

import java.util.*;
import com.lizardtech.djvu.*;

/**
 * <p>
 * This class is the base of all annotations by the viewer to display and manage
 * hyperlinks and highlighted areas inside a DjVuPage.
 * </p>
 * 
 * <p>
 * The currently supported areas can be rectangular GMapRect, elliptical
 * GMapOval and polygonal GMapPoly. Every map area besides the definition of
 * its shape contains information about display style and optional URL,
 * which it may refer to.  If this URL is not empty then the map area will
 * work like a hyperlink.
 * </p>
 * 
 * <p>
 * The classes also implement some useful functions to ease geometry
 * manipulations
 * </p>
 * 
 * <p>
 * This is the interface for all map areas. This defines some standard
 * interface to access the geometrical properties of the areas and describes
 * the area itself.
 * </p>
 * 
 * <p>
 * <b>target</b> - Defines where the specified URL should be loaded
 * </p>
 * 
 * <p>
 * <b>comment</b> - This is a string displayed in a status line or in a popup
 * window when the mouse pointer moves over the hyperlink area
 * </p>
 * 
 * <p>
 * <b>border_width</b> - describes how the area border should be drawn
 * </p>
 * 
 * <p>
 * <b>area_color</b> - describes how the area should be highlighted.
 * </p>
 * 
 * <p>
 * The map areas can be displayed using two different techniques, which can
 * be combined together:
 * </p>
 * 
 * <p>
 * <b>Visible border</b> -  The border of a map area can be drawn in several
 * different ways (like XOR_BORDER or SHADOW_IN_BORDER). It can be made
 * always visible, or appearing only when the mouse pointer moves over the
 * map area.
 * </p>
 * 
 * <p>
 * <b>Highlighted contents</b> - Contents of rectangular map areas can also
 * be highlighted with some given color.
 * </p>
 */
public class Rect
  extends DjVuObject
  implements Hyperlink
{
  //~ Static fields/initializers ---------------------------------------------
  /** rectangle map type */
  public static final int MAP_RECT = 0;

  /** oval map type */
  public static final int MAP_OVAL = 1;

  /** poly map type */
  public static final int MAP_POLY = 2;

  /** text map type */
  public static final int MAP_TEXT = 3;

  /** line map type */
  public static final int MAP_LINE = 4;

  /** no border flag */
  public static final int NO_BORDER = 0;

  /** xor border flag */
  public static final int XOR_BORDER = 1;

  /** solid border flag */
  public static final int SOLID_BORDER = 2;

  /** shadow in border flag */
  public static final int SHADOW_IN_BORDER = 3;

  /** shadow out border flag */
  public static final int SHADOW_OUT_BORDER = 4;

  /** shadow ein border flag */
  public static final int SHADOW_EIN_BORDER = 5;

  /** shadow ein border flag */
  public static final int SHADOW_EOUT_BORDER = 6;

  /** maparea tag */
  public static final String MAPAREA_TAG = "maparea";

  /** rect tag */
  public static final String RECT_TAG = "rect";

  /** no border tag */
  public static final String NO_BORDER_TAG = "none";

  /** xor border tag */
  public static final String XOR_BORDER_TAG = "xor";

  /** border tag */
  public static final String SOLID_BORDER_TAG = "border";

  /** shadow in tag */
  public static final String SHADOW_IN_BORDER_TAG = "shadow_in";

  /** shadow out tag */
  public static final String SHADOW_OUT_BORDER_TAG = "shadow_out";

  /** shadow ein tag */
  public static final String SHADOW_EIN_BORDER_TAG = "shadow_ein";

  /** shadow eout tag */
  public static final String SHADOW_EOUT_BORDER_TAG = "shadow_eout";

  /** border always visible tag */
  public static final String BORDER_AVIS_TAG = "border_avis";

  /** hilite tag */
  public static final String HILITE_TAG = "hilite";

  /** arrow tag */
  public static final String ARROW_TAG = "arrow";

  /** url tag */
  public static final String URL_TAG = "url";

  /** target self tag */
  public static final String TARGET_SELF = "_self";
  
  /** text pushpin tag */
  public static final String PUSHPIN_TAG = "pushpin";
  
  /** text background color tag */
  public static final String BGCOLOR_TAG = "backclr";
  
  /** text foreground color tag */
  public static final String TEXTCOLOR_TAG = "textclr";
  
  /** line color tag */
  public static final String LINECOLOR_TAG = "lineclr";

  /** line arrow tag */
  public static final String LINE_ARROW_TAG = "arrow";
  
  /** line width tag */
  public static final String LINE_WIDTH_TAG = "width";
  
//  /** line clear tag */
//  public static final String LINE_CLR_TAG = "lineclr";
  
  /** opacity tag */
  public static final String OPACITY_TAG = "opacity";

  /** no hilite color */
  public static final int NO_HILITE = 0xFFFFFFFF;

  /** xor hilite color */
  public static final int XOR_HILITE = 0xFF000000;


  //~ Instance fields --------------------------------------------------------

  private final GRect bounds = new GRect();

  // Border color (when relevant) in 0x00RRGGBB format.
  private int border_color = 0xff;
  private int hilite_color = NO_HILITE;
  private int text_color = 0;
  private int bg_color = NO_HILITE;
  private int line_color = XOR_HILITE;
  private int opacity = 50;
  private int line_width = 1;
  
  // Comment - displayed in a status line or as a popup hint when the mouse
  // pointer moves over the map area
  private String comment = null;
  private String target = TARGET_SELF;

  // optional url
  private String  url                   = null;
  private int[]   xx                    = null;
  private int[]   yy                    = null;
  private boolean border_always_visible = false;
  private boolean visible               = false;
  private boolean pushpin               = false;
  private boolean arrow                 = false;

  // This defines how the map area border should be drawn<br>
  private int border_type = NO_BORDER;

  // Border width in pixels
  private int  border_width     = 1;
  private long pageInfoHeight   = 0;
  private long pageInfoWidth    = 0;
  private long pageScaledHeight = 0;
  private long pageScaledWidth  = 0;
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Rect object.
   */
  public Rect() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Set if the border is always visible, or just when the mouse is over  the
   * link.
   *
   * @param visible true if the border should always be visible.
   */
  public void setBorderAlwaysVisible(boolean visible)
  {
    border_always_visible = visible;
    setVisible(visible);
  }

  /**
   * Query if the border is always visible, or just when the mouse is over
   * the link.
   *
   * @return true if the border should always be visible.
   */
  public boolean isBorderAlwaysVisible()
  {
    return border_always_visible;
  }

  /**
   * Set if a pushpin should be used.
   *
   * @param value true if this is a pushpin
   */
  public void setPushpin(final boolean value)
  {
    pushpin = value;
  }

  /**
   * Query if a pushpin should be used.
   *
   * @return true if this is a pushpin
   */
  public boolean isPushpin()
  {
    return pushpin;
  }

  /**
   * Set if a arrow should be drawn.
   *
   * @param value true if this should have an arrow
   */
  public void setArrow(final boolean value)
  {
    arrow = value;
  }

  /**
   * Query if an arrow should be drawn.
   *
   * @return true if this should have an arrow
   */
  public boolean isArrow()
  {
    return arrow;
  }

  /**
   * Sets the color the area border should be drawn. Border color (when
   * relevant) in 0x00RRGGBB format.
   *
   * @param color DOCUMENT ME!
   */
  public void setBorderColor(final Number color)
  {
    border_color = color.intValue();
  }

  /**
   * Get the color the area border should be drawn. Border color (when
   * relevant) in 0x00RRGGBB format.
   *
   * @return color in RRGGBB format
   */
  public int getBorderColor()
  {
    return border_color;
  }

  /**
   * Set the border type. This defines how the map area border should be drawn<br>
   * <b>NO_BORDER</b> - No border drawn<br>
   * <b>XOR_BORDER</b> - The border is drawn using XOR method.<br>
   * <b>SOLID_BORDER</b> - The border is drawn as a solid line of a given color.<br>
   * <b>SHADOW_IN_BORDER</b> - Supported for \Ref{Rect} only. The map
   * area area looks as if it was "pushed-in".<br>
   * <b>SHADOW_OUT_BORDER</b> - The opposite of SHADOW_OUT_BORDER<br>
   * <b>SHADOW_EIN_BORDER</b> - Also for Rectangle only. Is translated as
   * "shadow etched in"<br>
   * <b>SHADOW_EOUT_BORDER</b> - The opposite of SHADOW_EIN_BORDER.<br>
   * 
   * @param borderType DOCUMENT ME!
   */
  public void setBorderType(final int borderType)
  {
    switch(borderType)
    {
      case XOR_BORDER :
      case SOLID_BORDER :
      case SHADOW_IN_BORDER :
      case SHADOW_OUT_BORDER :
      case SHADOW_EIN_BORDER :
      case SHADOW_EOUT_BORDER :
        border_type = borderType;

        return;
      default :
        border_type = NO_BORDER;
    }
  }

  /**
   * Get the border type. This defines how the map area border should be drawn<br>
   * <b>NO_BORDER</b> - No border drawn<br>
   * <b>XOR_BORDER</b> - The border is drawn using XOR method.<br>
   * <b>SOLID_BORDER</b> - The border is drawn as a solid line of a given color.<br>
   * <b>SHADOW_IN_BORDER</b> - Supported for \Ref{Rect} only. The map
   * area area looks as if it was "pushed-in".<br>
   * <b>SHADOW_OUT_BORDER</b> - The opposite of SHADOW_OUT_BORDER<br>
   * <b>SHADOW_EIN_BORDER</b> - Also for Rectangle only. Is translated as
   * "shadow etched in"<br>
   * <b>SHADOW_EOUT_BORDER</b> - The opposite of SHADOW_EIN_BORDER.<br>
   * 
   * @return DOCUMENT ME!
   */
  public int getBorderType()
  {
    return border_type;
  }

  /**
   * Set border width in pixels.
   *
   * @param width DOCUMENT ME!
   */
  public void setBorderWidth(final int width)
  {
    border_width = width;
  }

  /**
   * Get border width in pixels.
   *
   * @return DOCUMENT ME!
   */
  public int getBorderWidth()
  {
    return border_width;
  }

  /**
   * Comment - displayed in a status line or as a popup hint when the mouse
   * pointer moves over the map area
   *
   * @param comment DOCUMENT ME!
   */
  public void setComment(String comment)
  {
    this.comment = comment;
  }

  /**
   * Comment - displayed in a status line or as a popup hint when the mouse
   * pointer moves over the map area
   *
   * @return the comment
   */
  public String getComment()
  {
    return comment;
  }

  /**
   * Set the specified a color for highlighting the internal area of the map
   * area.  Will work with rectangular map areas only. The color is
   * specified in #00RRGGBB format. A special value of #FFFFFFFF disables
   * highlighting and #FF000000 is for XOR highlighting.
   *
   * @param color the hilite color
   */
  public void setHiliteColor(final Number color)
  {
    this.hilite_color = color.intValue();
  }

  /**
   * Get the specified a color for highlighting the internal area of the map
   * area.  Will work with rectangular map areas only. The color is
   * specified in #00RRGGBB format. A special value of #FFFFFFFF disables
   * highlighting and #FF000000 is for XOR highlighting.
   *
   * @return the hilite color
   */
  public int getHiliteColor()
  {
    return hilite_color;
  }

  /**
   * Set the specified a color for text of the map
   * area.  Will work with text map areas only. The color is
   * specified in #00RRGGBB format.
   *
   * @param color the text color
   */
  public void setTextColor(final Number color)
  {
    this.text_color = color.intValue();
  }

  /**
   * Get the color of text in  the map
   * area.  Will work with text map areas only. The color is
   * specified in #00RRGGBB format.
   *
   * @return the text color
   */
  public int getTextColor()
  {
    return text_color;
  }

  /**
   * Set the specified a color for text background of the map
   * area.  Will work with text map areas only. The color is
   * specified in #00RRGGBB format.
   *
   * @param color the text color
   */
  public void setBgColor(final Number color)
  {
    this.bg_color = color.intValue();
  }

  /**
   * Get the color of text background in  the map
   * area.  Will work with text map areas only. The color is
   * specified in #00RRGGBB format.
   *
   * @return the text color
   */
  public int getBgColor()
  {
    return bg_color;
  }

  /**
   * Set the specified a color for a line across map
   * area. The color is specified in #00RRGGBB format.
   *
   * @param color the line color
   */
  public void setLineColor(final Number color)
  {
    this.line_color = color.intValue();
  }

  /**
   * Set the specified a thickness of a line.
   *
   * @param weight the line width
   */
  public void setLineWidth(final Number weight)
  {
    this.line_width = weight.intValue();
  }

  /**
   * Get the color of the line across the map area.  Will work 
   * with text map areas only. The color is specified in 
   * #00RRGGBB format.
   *
   * @return the line color
   */
  public int getLineColor()
  {
    return line_color;
  }


  /**
   * Set the specified a thickness of a line.
   *
   * @return the line weight
   */
  public int getLineWidth()
  {
    return line_width;
  }

  /**
   * Set the specified a opacity.  Will work with text map areas 
   * only. The value is from 0 to 100.
   *
   * @param opacity the opacity of this annotation
   */
  public void setOpacity(final Number opacity)
  {
    this.opacity = opacity.intValue();
  }

  /**
   * Query the opacity.  Will work with text map areas 
   * only. The value is from 0 to 100.
   *
   * @return the opacity of this annotation
   */
  public int getOpacity()
  {
    return opacity;
  }

  /**
   * Query the map type.
   *
   * @return the map type
   */
  public int getMapType()
  {
    return MAP_RECT;
  }

  /**
   * Set the target for the URL. Standard targets are:<br>
   * <b>_blank</b> - Load the link in a new blank window<br>
   * <b>_self</b> - Load the link into the plugin window<br>
   * <b>_top</b> - Load the link into the top-level frame<br>
   *
   * @param target DOCUMENT ME!
   */
  public void setTarget(final String target)
  {
    this.target = target;
  }

  /**
   * Get the target for the URL. Standard targets are:<br>
   * <b>_blank</b> - Load the link in a new blank window<br>
   * <b>_self</b> - Load the link into the plugin window<br>
   * <b>_top</b> - Load the link into the top-level frame<br>
   *
   * @return DOCUMENT ME!
   */
  public String getTarget()
  {
    return target;
  }

  /**
   * Set optional URL which this map area can be associated with. If it's not
   * empty then clicking this map area with the mouse will make the browser
   * load the HTML page referenced by this url.  Note: This may also be a
   * relative URL.
   *
   * @param url DOCUMENT ME!
   */
  public void setURL(final String url)
  {
    this.url = url;
  }

  /**
   * Get optional URL which this map area can be associated with. If it's not
   * empty then clicking this map area with the mouse will make the browser
   * load the HTML page referenced by this url.  Note: This may also be a
   * relative URL.
   *
   * @return the URL string
   */
  public String getURL()
  {
    return url;
  }

  /**
   * Set if the border should current by visible.
   *
   * @param visible true if visible
   */
  public void setVisible(boolean visible)
  {
    this.visible = visible;
  }

  /**
   * Query if the border should current by visible.
   *
   * @return true if visible
   */
  public boolean isVisible()
  {
    return visible||(isBorderAlwaysVisible()&&!isPushpin());
  }

  /**
   * Creates an instance of Rect with the options interherited from the
   * specified reference.
   * 
   * @param ref Object to interherit DjVuOptions from.
   * 
   * @return a new instance of Rect.
   */
  public static Rect createRect(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (Rect)create(
      options,
      options.getAnnoRectClass(),
      Rect.class);
  }

  /**
   * Query the bounding rectangle.
   *
   * @return the bounding rectangle
   */
  public GRect getBounds()
  {
    return bounds;
  }

  /**
   * Query if the area is empty
   *
   * @return true if has empty bounds
   */
  public boolean isEmpty()
  {
    return getBounds().isEmpty();
  }

  /**
   * Use this method to set the page height and width for translating from
   * bottom up to top down coordinates and scaling.  Using a values of 0
   * avoids translation.
   *
   * @param width page width
   * @param height page height
   * @param scaledWidth scaled page width
   * @param scaledHeight scaled page height
   */
  public final void setPageSize(
    final int width,
    final int height,
    final int scaledWidth,
    final int scaledHeight)
  {
    if(
      (this.pageInfoHeight != height)
      || (this.pageInfoWidth != width)
      || (this.pageScaledHeight != scaledHeight)
      || (this.pageScaledWidth != scaledWidth))
    {
      this.pageInfoHeight     = height;
      this.pageInfoWidth      = width;
      this.pageScaledHeight   = scaledHeight;
      this.pageScaledWidth    = scaledWidth;
      xx                      = null;
      yy                      = null;
    }
  }

  /**
   * Method generating a list of defining coordinates. (default are the
   * opposite corners of the enclosing rectangle)
   *
   * @return a vector of points
   */
  public Vector getPoints()
  {
    final GRect  bounds = getBounds();
    final int[]  point1 = {bounds.xmin, bounds.ymin};
    final int[]  point2 = {bounds.xmax, bounds.ymax};
    final Vector retval = new Vector();
    retval.addElement(point1);
    retval.addElement(point2);

    return retval;
  }

  /**
   * Gets an array of X coordinates, converted from bottom up to dop down if
   * the pageInfoHeight has been set to non-zero.
   *
   * @return an array of x coordinates
   */
  public final int[] getXCoordinates()
  {
    int[] retval = xx;

    if(retval == null)
    {
      retval = computeArray(false);
    }

    return retval;
  }

  /**
   * Gets an array of Y coordinates, converted from bottom up to dop down if
   * the pageInfoHeight has been set to non-zero.
   *
   * @return an array of y coordinates
   */
  public final int[] getYCoordinates()
  {
    int[] retval = yy;

    if(retval == null)
    {
      retval = computeArray(true);
    }

    return retval;
  }

  /**
   * A method for setting the coordinates
   *
   * @param needY true if the y coordinates should be returned
   *
   * @return an array of x or y coordinates
   */
  public final int[] computeArray(final boolean needY)
  {
    final Vector points = getPoints();
    int[]        xx = new int[points.size()];
    int[]        yy = new int[points.size()];

    for(int i = points.size(); i-- > 0;)
    {
      final int[] point = (int[])points.elementAt(i);
      xx[i]   = scaleX(point[0]);
      yy[i]   = scaleY(point[1]);

//      verbose("("+point[0]+","+point[1]+") -> ("+xx[i]+","+yy[i]+")");
    }

    this.xx   = xx;
    this.yy   = yy;

    return needY
    ? yy
    : xx;
  }

  /**
   * Checks if the object is OK.
   *
   * @return true if valid
   */
  public boolean isValid()
  {
    final GRect bounds = getBounds();

    if(bounds.xmax == bounds.xmin)
    {
      return false;
    }

    if(bounds.ymax == bounds.ymin)
    {
      return false;
    }

    if(
      ((border_type == XOR_BORDER) || (border_type == SOLID_BORDER))
      && (border_width != 1))
    {
      return false;
    }

    if(
      ((border_type == SHADOW_IN_BORDER)
      || (border_type == SHADOW_OUT_BORDER)
      || (border_type == SHADOW_EIN_BORDER)
      || (border_type == SHADOW_EOUT_BORDER))
      && ((border_width < 3) || (border_width > 32)))
    {
      return false;
    }

    return true;
  }

  /**
   * Check if the point is inside the hyperlink area
   *
   * @param x horizontal coordinate
   * @param y vertical coordinate
   *
   * @return true if the given point is inside the hyperlink area
   */
  public boolean contains(
    final int x,
    final int y)
  {
    return getBounds().contains(x, y);
  }

  /**
   * Query the name of this shape.
   *
   * @return "rect"
   */
  public String get_shape_name()
  {
    return RECT_TAG;
  }

  /**
   * Changes the Rect's geometry
   * 
   * @param rect bounding rectangle
   * 
   * @return the initialized shape
   */
  public Rect init(final GRect rect)
  {
    final GRect bounds = getBounds();
    bounds.xmin   = rect.xmin;
    bounds.xmax   = rect.xmax;
    bounds.ymin   = rect.ymin;
    bounds.ymax   = rect.ymax;

    return this;
  }

  /**
   * Method maps rectangle from one area to another using mapper
   *
   * @param mapper the coordinate mapper
   */
  public void map(final Mapper mapper)
  {
    mapper.map(getBounds());
  }

  /**
   * Moves the hyperlink along the given vector. Is used by the hyperlinks
   * editor.
   *
   * @param dx distance to move the x coordinates
   * @param dy distance to move the y coordinates
   */
  public void move(
    final int dx,
    final int dy)
  {
    if((dx != 0) || (dy != 0))
    {
      final GRect bounds = getBounds();
      bounds.xmin += dx;
      bounds.ymin += dy;
      bounds.xmax += dx;
      bounds.ymax += dy;
    }

    reset();
  }

  /**
   * Resize this shape.
   *
   * @param width the new width
   * @param height the new height
   */
  public void resize(
    final int width,
    final int height)
  {
    final GRect bounds = getBounds();
    bounds.xmax   = bounds.xmin + width;
    bounds.ymax   = bounds.ymin + height;
    reset();
  }

  /**
   * Called to rotate this shape.
   *
   * @param rot angle to rotate divided by 90
   * @param cx horizontal center of rotation
   * @param cy vertical center of rotation
   */
  public void rotateArea(
    int rot,
    int cx,
    int cy)
  {
    if(rot == 0)
    {
      return;
    }

    rot = rot % 4;

    int         temp = 0;

    final GRect bounds = getBounds();

    switch(rot)
    {
      case 1 :
      {
        //rotate
        temp          = bounds.xmin;
        bounds.xmin   = cy - bounds.ymin;
        bounds.ymin   = temp;
        temp          = bounds.xmax;
        bounds.xmax   = cy - bounds.ymax;
        bounds.ymax   = temp;
      }

      break;
      case 2 :
      {
        //rotate
        bounds.xmin   = cx - bounds.xmin;
        bounds.ymin   = cx - bounds.ymin;
        bounds.xmax   = cy - bounds.xmax;
        bounds.ymax   = cy - bounds.ymax;
      }

      break;
      case 3 :
      {
        temp          = bounds.xmin;
        bounds.xmin   = bounds.ymin;
        bounds.ymin   = cx - temp;

        temp          = bounds.xmax;
        bounds.xmax   = bounds.ymax;
        bounds.ymax   = cx - temp;
      }

      break;
    }

    if(bounds.xmin > bounds.xmax)
    {
      temp          = bounds.xmin;
      bounds.xmin   = bounds.xmax;
      bounds.xmax   = temp;
    }

    if(bounds.ymin > bounds.ymax)
    {
      temp          = bounds.ymin;
      bounds.ymin   = bounds.ymax;
      bounds.ymax   = temp;
    }

    reset();
  }

  /**
   * Move and resize this shape to the new bounding rectangle.
   *
   * @param grect new bounding rectangle
   */
  public void transform(final GRect grect)
  {
    final GRect bounds = getBounds();
    bounds.xmin   = grect.xmin;
    bounds.ymin   = grect.ymin;
    bounds.xmax   = grect.xmax;
    bounds.ymax   = grect.ymax;
    reset();
  }

  /**
   * Method unmaps rectangle from one area to another using mapper
   *
   * @param mapper object to map coordinates
   */
  public void unmap(Mapper mapper)
  {
    mapper.unmap(getBounds());
  }  
  
  /**
   * Query the info height.
   *
   * @return the total page height
   */
  protected final long getInfoHeight()
  {
    return pageInfoHeight;
  }

  /**
   * Query the info width.
   *
   * @return the total page width
   */
  protected final long getInfoWidth()
  {
    return pageInfoWidth;
  }

  /**
   * Query the scaled height.
   *
   * @return the scaled page height
   */
  protected final long getScaledHeight()
  {
    return pageScaledHeight;
  }

  /**
   * Query the scaled width.
   *
   * @return the scaled page width
   */
  protected final long getScaledWidth()
  {
    return pageScaledWidth;
  }

  /**
   * Resets cached results.
   */
  protected void reset()
  {
    xx   = null;
    yy   = null;
  }

  // Translate x coordinate from document space coordinates to viewing space.
  private int scaleX(int x)
  {
    if(
      (pageInfoWidth != 0)
      && (pageScaledWidth != 0)
      && (pageScaledWidth != pageInfoWidth))
    {
      x = (int)((pageScaledWidth * (long)x) / pageInfoWidth);
    }

    return x;
  }

  // Translate x coordinate from document space coordinates to viewing space.
  private int scaleY(int y)
  {
    if(pageInfoHeight != 0)
    {
      if((pageScaledHeight != 0) && (pageScaledHeight != pageInfoHeight))
      {
        y = (int)(((pageInfoHeight - (long)y) * pageScaledHeight) / pageInfoHeight);
      }
      else
      {
        y = (int)pageInfoHeight - y;
      }
    }

    return y;
  }
}
