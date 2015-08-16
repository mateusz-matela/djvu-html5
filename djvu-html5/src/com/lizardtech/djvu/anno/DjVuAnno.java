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
import java.io.*;
import java.util.*;


/**
 * <p>
 * DjVuAnno implement the mechanism for annotating DjVuImages. Annotations
 * are additional instructions for the plugin about how the image should be
 * displayed.  The exact format of annotations is not strictly defined. The
 * only requirement is that they have to be stored as a sequence of chunks
 * inside a FORM:ANNO.
 * </p>
 * 
 * <p>
 * This file implements annotations understood by the DjVu plugins  and
 * encoders.
 * </p>
 * 
 * <p>
 * using: contents of ANT chunks.
 * </p>
 * >
 * 
 * <p>
 * Contents of the FORM:ANNO should be passed to DjVuAnno.decode() for
 * parsing, which initializes DjVuAnno.ANT and fills them with decoded data.
 * </p>
 * 
 * <p>
 * This is a top-level class containing annotations of a DjVu document (or
 * just a page). It has only two functions: encode() and decode().  Both of
 * them work with a sequence of annotation chunks from FORM:ANNO form.
 * Basing on the name of the chunks they call encode() and decode()
 * functions of the proper annotation structure (like ANT). The real work of
 * encoding and decoding is done by lower-level classes.
 * </p>
 */
public class DjVuAnno
  extends DjVuObject
  implements Codec
{
  //~ Static fields/initializers ---------------------------------------------

  private static final String PNOTE_TAG            = "pnote";
  private static final String BACKGROUND_COLOR_TAG = "background";
  private static final String ZOOM_TAG             = "zoom";
  private static final String MODE_TAG             = "mode";
  private static final String ALIGN_TAG            = "align";

  /** The default background color. */
  public static final long DEFAULT_BG_COLOR = 0xffffffff;

  /** The array of legal zoom specifications. */
  public static final String[] ZOOM_STRING_ARRAY =
  {"default", "page", "width", "one2one", "stretch"};

  /** The array of legal mode specifications. */
  public static final String[] MODE_STRING_ARRAY =
  {"default", "color", "fore", "back", "bw"};

  /** The array of align specifications. */
  public static final String[] ALIGN_STRING_ARRAY =
  {"default", "left", "center", "right", "top", "bottom"};

  /** Used for unspecifed. */
  public static final int UNSPEC = 0;

  /** Color mode. */
  public static final int COLOR = 1;

  /** Foreground mode. */
  public static final int FOREGROUND = 2;

  /** Background mode. */
  public static final int BACKGROUND = 3;

  /** Bitonal mode. */
  public static final int BITONAL = 4;

  /** Display stretched. */
  public static final int STRETCH = -4;

  /** Display 1 to 1. */
  public static final int ONE2ONE = -3;

  /** Display fit width. */
  public static final int FIT_WIDTH = -2;

  /** Display fit page. */
  public static final int FIT_PAGE = -1;

  /** Align left. */
  public static final int LEFT = 1;

  /** Align center. */
  public static final int CENTER = 2;

  /** Align right. */
  public static final int RIGHT = 3;

  /** Align top. */
  public static final int TOP = 4;

  /** Align bottom. */
  public static final int BOTTOM = 5;

  //~ Instance fields --------------------------------------------------------

  /** The parser used to create this object. */
  private String raw = null;

  // List of defined map areas. They may be just areas of highlighting or
  // hyperlink. Please refer to Rect, Poly and Oval for details.
  private Vector map_area = new Vector();

  // Horizontal page alignment. Possible values are LEFT, CENTER,
  // RIGHT and UNSPEC.
  private int hor_align = UNSPEC;

  // Initial display mode.
  private int mode = UNSPEC;

  // Vertical page alignment. Possible values are TOP, CENTER,
  // BOTTOM and UNSPEC.
  private int ver_align = UNSPEC;

  // Initial zoom.
  private int zoom = UNSPEC;

  // NOTE: Do not change the values for the enumeration. They are assumed
  //       in other declarations and the code.
  // Background color in #0x00RRBBGG# format unless DEFAULT_BG_COLOR is used
  // because there was no background color records in the annotation chunk.
  private long bg_color = DEFAULT_BG_COLOR;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DjVuAnno object.
   */
  public DjVuAnno() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query if this is image data.
   *
   * @return false
   */
  public boolean isImageData()
  { 
      return false;
  }  

  /**
   * Creates an instance of DjVuAnno with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of DjVuAnno.
   */
  public static DjVuAnno createDjVuAnno(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (DjVuAnno)create(
      options,
      options.getDjVuAnnoClass(),
      DjVuAnno.class);
  }

  /**
   * Set the horizontal alignment. Allowed values are LEFT, CENTER, RIGHT and
   * UNSPEC.
   *
   * @param hor_align desired horizontal alignment
   */
  public void setHorAlign(final int hor_align)
  {
    switch(hor_align)
    {
      case LEFT :
      case CENTER :
      case RIGHT :
        this.hor_align = hor_align;

        return;
      default :
        this.hor_align = UNSPEC;
    }
  }

  /**
   * Get the horizontal alignment. Possible values are LEFT, CENTER, RIGHT
   * and UNSPEC.
   *
   * @return horizontal alignment.
   */
  public int getHorAlign()
  {
    return hor_align;
  }

  /**
   * Set the vector of Rect, Oval, and Poly objects.
   * 
   * @param map_area Vector of area maps.
   */
  public void setMapArea(final Vector map_area)
  {
    this.map_area = map_area;
  }

  /**
   * Get the vector of Rect, Oval, and Poly objects.
   * 
   * @return the vector of area maps.
   */
  public Vector getMapArea()
  {
    return map_area;
  }

  /**
   * Set the display mode value. Allowed values are UNSPEC, COLOR,
   * FOREGROUND, BACKGROUND, BITONAL, and UNSPEC.
   *
   * @param mode the desired display mode
   */
  public void setMode(final int mode)
  {
    switch(mode)
    {
      case COLOR :
      case FOREGROUND :
      case BACKGROUND :
      case BITONAL :
        this.mode = mode;

        return;
      default :
        this.mode = UNSPEC;
    }
  }

  /**
   * Get the display mode value. Possible values are UNSPEC, COLOR,
   * FOREGROUND, BACKGROUND, BITONAL, and UNSPEC.
   *
   * @return the desired display mode
   */
  public int getMode()
  {
    return mode;
  }

  /**
   * Set the raw annotation string.
   *
   * @param raw a buffer with the raw annotations
   */
  public void setRaw(final StringBuffer raw)
  {
    final String oldRaw = this.raw;
    int          i = (raw != null)
      ? raw.length()
      : 0;

    while(i > 0)
    {
      if(!Character.isWhitespace(raw.charAt(--i)))
      {
        ++i;

        break;
      }
    }

    if(i == 0)
    {
      this.raw = null;
    }
    else
    {
      raw.setLength(i);
      this.raw = raw.toString();
    }

    if((oldRaw != this.raw) && ((oldRaw == null) || !oldRaw.equals(this.raw)))
    {
      decode(new LispParser(this.raw));
    }
  }

  /**
   * Set the raw annotation string.
   *
   * @param raw The raw annotation.
   */
  public void setRaw(final String raw)
  {
    setRaw((raw != null)
      ? (new StringBuffer(raw))
      : null);
  }

  /**
   * Query the raw annotation string.
   *
   * @return the raw annotation string.
   */
  public String getRaw()
  {
    return this.raw;
  }

  /**
   * Set the vertical alignment. Allowed values are TOP, CENTER, BOTTOM and
   * UNSPEC.
   *
   * @param ver_align the desired vertical alignment
   */
  public void setVerAlign(final int ver_align)
  {
    switch(ver_align)
    {
      case TOP :
      case CENTER :
      case BOTTOM :
        this.ver_align = ver_align;

        return;
      default :
        this.ver_align = UNSPEC;
    }
  }

  /**
   * Get the vertical alignment. Possible values are TOP, CENTER, BOTTOM and
   * UNSPEC.
   *
   * @return  the desired vertical alignment
   */
  public int getVerAlign()
  {
    return ver_align;
  }

  /**
   * Set the zoom value. Allowed values are STRETCH, ONE2ONE, FIT_WIDTH,
   * FIT_PAGE, UNSPEC, and any posative number.
   *
   * @param zoom the desired zoom mode
   */
  public void setZoom(final int zoom)
  {
    switch(zoom)
    {
      case STRETCH :
      case ONE2ONE :
      case FIT_WIDTH :
      case FIT_PAGE :
        this.zoom = zoom;

        return;
      default :
        this.zoom = (zoom <= 0)
          ? UNSPEC
          : zoom;
    }
  }

  /**
   * Get the zoom value. Possible values are STRETCH, ONE2ONE, FIT_WIDTH,
   * FIT_PAGE, UNSPEC, and any posative number.
   *
   * @return  the desired zoom mode
   */
  public int getZoom()
  {
    return zoom;
  }

  /**
   * Converts color from string in #RRGGBB notation to a long.
   *
   * @param color the string with the formatted color.
   * @param retval the default value to use if the string can not be converted
   *
   * @return The color value.
   */
  public static int cvt_color(
    final String color,
    int retval)
  {
    if((color != null) && (color.length() >= 2) && (color.charAt(0) == '#'))
    {
      try
      {
        retval = (int)Long.parseLong(
            color.substring(1),
            16);
      }
      catch(final Throwable ignored) {}
    }

    return retval;
  }

  /**
   * Same as init() but adds the new data to what has been decoded before.
   *
   * @param input the annotation chunk.
   *
   * @throws IOException if an error occures
   */
  public void decode(CachedInputStream input)
    throws IOException
  {
    if("ANTz".equals(input.getName()))
    {
      input=CachedInputStream.createCachedInputStream(this).init(
              BSInputStream.createBSInputStream(this).init(input));
      input.setName("ANTa");
    }
    final String raw = input.readFullyUTF();
    if((raw != null) && (raw.length() > 0))
    {
      final String encode_raw = getRaw();
      setRaw((encode_raw != null)
        ? (encode_raw + raw)
        : raw);
    }
  }

  /**
   * Get background color in #0x00RRBBGG# format unless DEFAULT_BG_COLOR is
   * used because there was no background color records in the annotation
   * chunk.
   *
   * @return the desired background color.
   */
  public long get_bg_color()
  {
    return bg_color;
  }

  /**
   * Decodes contents of annotation chunk ANTa. The chunk data is read from
   * InputStream bs until reaching an end-of-stream marker.
   *
   * @param pool Stream to read data from.
   *
   * @return The decoded annotations.
   *
   * @throws IOException if an error occurs reading the stream.
   */
  public DjVuAnno init(final CachedInputStream pool)
    throws IOException
  {
    setRaw(((CachedInputStream)pool.clone()).readFullyUTF());

    return this;
  }

  /**
   * Returns TRUE if no features are specified or specified features are not
   * different from default ones
   *
   * @return true if no features have bees specified.
   */
  public boolean is_empty()
  {
    return (getRaw() == null);
  }

  /**
   * Set background color in #0x00RRBBGG# format unless DEFAULT_BG_COLOR is
   * used because there was no background color records in the annotation
   * chunk.
   *
   * @param bg_color the desired background color
   */
  public void set_bg_color(final long bg_color)
  {
    this.bg_color = bg_color;
  }

  // Convert a background color string.
  private static long convert_bg_color(final String bg_color)
  {
    return ((bg_color == null) || (bg_color.length() == 0))
    ? DEFAULT_BG_COLOR
    : cvt_color(bg_color, 0xffffff);
  }

  // Read hex digits
  private static int decode_comp(
    char ch1,
    char ch2)
  {
    int dig1 = 0;

    switch(ch1)
    {
      case '0' :
      case '1' :
      case '2' :
      case '3' :
      case '4' :
      case '5' :
      case '6' :
      case '7' :
      case '8' :
      case '9' :
        dig1 = ch1 - '0';

        break;
      case 'A' :
      case 'B' :
      case 'C' :
      case 'D' :
      case 'E' :
      case 'F' :
        dig1 = (10 + ch1) - 'A';

        break;
      case 'a' :
      case 'b' :
      case 'c' :
      case 'd' :
      case 'e' :
      case 'f' :
        dig1 = (10 + ch1) - 'a';

        break;
      default :
        return 0;
    }

    switch(ch2)
    {
      case '0' :
      case '1' :
      case '2' :
      case '3' :
      case '4' :
      case '5' :
      case '6' :
      case '7' :
      case '8' :
      case '9' :
        return (dig1 << 4) | (ch2 - '0');
      case 'A' :
      case 'B' :
      case 'C' :
      case 'D' :
      case 'E' :
      case 'F' :
        return (dig1 << 4) | ((10 + ch2) - 'A');
      case 'a' :
      case 'b' :
      case 'c' :
      case 'd' :
      case 'e' :
      case 'f' :
        return (dig1 << 4) | ((10 + ch2) - 'a');
      default :
        return dig1;
    }
  }

  // Remove all items with the specified name
  private static void del_all_items(
    final String     name,
    final LispParser parser)
  {
    for(int pos = parser.size(); pos > 0;)
    {
      final Object object = parser.elementAt(--pos);

      if(object instanceof NamedVector)
      {
        final NamedVector obj = (NamedVector)object;

        if(name.equals(obj.getName()))
        {
          parser.removeElementAt(pos);
        }
      }
    }
  }

  // parse an alignment value
  private static int lookupAlign(final String align)
  {
    for(int i = UNSPEC; ++i < ALIGN_STRING_ARRAY.length;)
    {
      if(ALIGN_STRING_ARRAY[i].equals(align))
      {
        return i;
      }
    }

    return UNSPEC;
  }

  // parse a mode value
  private static int lookupMode(final String mode)
  {
    for(int i = UNSPEC; (++i < MODE_STRING_ARRAY.length);)
    {
      if(MODE_STRING_ARRAY[i].equals(mode))
      {
        return i;
      }
    }

    return UNSPEC;
  }

  // parse a zoom value
  private static int lookupZoom(
    final String  zoom,
    final boolean nothrow)
  {
    for(int i = UNSPEC; ++i < ZOOM_STRING_ARRAY.length;)
    {
      if(ZOOM_STRING_ARRAY[i].equals(zoom))
      {
        return (-i);
      }
    }

    try
    {
      if(zoom.charAt(0) == 'd')
      {
        return Integer.parseInt(zoom.substring(1));
      }
      else if(nothrow)
      {
        return Integer.parseInt(zoom);
      }

      throw new IllegalArgumentException("DjVuAnno.bad_zoom");
    }
    catch(final RuntimeException exp)
    {
      if(!nothrow)
      {
        throw exp;
      }
    }

    return UNSPEC;
  }

  // parse a horizontal alignment value
  private static int parseHorAlign(final LispParser parser)
  {
    final NamedVector list = parser.getNamedVector(ALIGN_TAG);

    return ((list != null) && (list.size() > 0))
    ? lookupAlign(list.elementAt(0).toString())
    : UNSPEC;
  }

  // decode from the specified parser
  private void decode(final LispParser parser)
  {
    set_bg_color(parse_bg_color(parser));
    setZoom(parseZoom(parser));
    setMode(parseMode(parser));
    setHorAlign(parseHorAlign(parser));
    setVerAlign(parseVerAlign(parser));
    setMapArea(parseMapArea(parser));
  }

  // parse vertical alignment from the specified parser.
  private static int parseVerAlign(final LispParser parser)
  {
    final NamedVector obj = parser.getNamedVector(ALIGN_TAG);

    return ((obj != null) && (obj.size() > 1))
    ? lookupAlign(obj.elementAt(1).toString())
    : UNSPEC;
  }

  // parse zoom from the specified parser.
  private static int parseZoom(final LispParser parser)
  {
    final NamedVector obj = parser.getNamedVector(ZOOM_TAG);

    return ((obj != null) && (obj.size() > 0))
    ? lookupZoom(
      obj.elementAt(0).toString(),
      true)
    : UNSPEC;
  }

  // parse background color from the specified parser.
  private static long parse_bg_color(final LispParser parser)
  {
    long retval = DEFAULT_BG_COLOR;

    try
    {
      NamedVector list = parser.getNamedVector(BACKGROUND_COLOR_TAG);

      if((list != null) && (list.size() == 1))
      {
        retval = convert_bg_color(list.elementAt(0).toString());
      }
    }
    catch(final Throwable ignored) {}

    return retval;
  }

  // parse map areas from the specified parser.
  private Vector parseMapArea(final LispParser parser)
  {
    final Vector map_area = new Vector();

    for(int pos = 0; pos < parser.size(); ++pos)
    {
      final Object object = parser.elementAt(pos);

      if(object instanceof NamedVector)
      {
        final NamedVector obj  = (NamedVector)object;
        final String      name = obj.getName();

        if(Rect.MAPAREA_TAG.equals(name))
        {
          try
          {
            // Getting the url
            String url        = null;
            String target     = Rect.TARGET_SELF;
            Object url_object = obj.elementAt(0);

            if(url_object instanceof NamedVector)
            {
              final NamedVector url_obj = (NamedVector)url_object;

              if(!Rect.URL_TAG.equals(url_obj.getName()))
              {
                throw new IllegalArgumentException("DjVuAnno.bad_url");
              }

              url      = url_obj.elementAt(0).toString();
              target   = url_obj.elementAt(1).toString();
            }
            else
            {
              url = (String)url_object;
            }

            // Getting the comment
            String   comment     = (String)obj.elementAt(1);
            Object   shapeObject = obj.elementAt(2);
            Rect xmap_area   = null;

            if(shapeObject instanceof NamedVector)
            {
              final NamedVector shape = (NamedVector)shapeObject;

              if(Rect.RECT_TAG.equals(shape.getName()))
              {
                final GRect grect =
                  new GRect(
                    ((Number)shape.elementAt(0)).intValue(),
                    ((Number)shape.elementAt(1)).intValue(),
                    ((Number)shape.elementAt(2)).intValue(),
                    ((Number)shape.elementAt(3)).intValue());
                xmap_area = Rect.createRect(this).init(grect);
              }
              else if(Line.LINE_TAG.equals(shape.getName()))
              {
                final int x0=((Number)shape.elementAt(0)).intValue();
                final int y0=((Number)shape.elementAt(1)).intValue();
                final int x1=((Number)shape.elementAt(2)).intValue();
                final int y1=((Number)shape.elementAt(3)).intValue();
                xmap_area = Line.createLine(this).init(x0,y0,x1,y1);
              }
              else if(Poly.POLY_TAG.equals(shape.getName()))
              {
                final int   points = shape.size() / 2;
                final int[] xx = new int[points];
                final int[] yy = new int[points];
                
                for(int i = 0, j = 0; j < shape.size(); i++)
                {
                  xx[i]   = ((Number)shape.elementAt(j++)).intValue();
                  yy[i]   = ((Number)shape.elementAt(j++)).intValue();
                }

                xmap_area =
                  Poly.createPoly(this).init(xx, yy, points);
              }
              else if(Oval.OVAL_TAG.equals(shape.getName()))
              {
                final GRect grect =
                  new GRect(
                    ((Number)shape.elementAt(0)).intValue(),
                    ((Number)shape.elementAt(1)).intValue(),
                    ((Number)shape.elementAt(2)).intValue(),
                    ((Number)shape.elementAt(3)).intValue());
                xmap_area = Oval.createOval(this).init(grect);
              }
              else if(Text.TEXT_TAG.equals(shape.getName()))
              {
                final GRect grect =
                  new GRect(
                    ((Number)shape.elementAt(0)).intValue(),
                    ((Number)shape.elementAt(1)).intValue(),
                    ((Number)shape.elementAt(2)).intValue(),
                    ((Number)shape.elementAt(3)).intValue());
                xmap_area = Text.createText(this).init(grect);
              }
            }

            if((xmap_area != null) && !xmap_area.isEmpty())
            {
              xmap_area.setURL(url);
              xmap_area.setTarget(target);
              xmap_area.setComment(comment);

              for(int obj_num = 3; obj_num < obj.size(); obj_num++)
              {
                final Object elobject = obj.elementAt(obj_num);

                if(elobject instanceof NamedVector)
                {
                  final NamedVector el    = (NamedVector)elobject;
                  final String      xname = el.getName();

                  if(Rect.BORDER_AVIS_TAG.equals(xname))
                  {
                    xmap_area.setBorderAlwaysVisible(true);
                  }
                  else if(Rect.ARROW_TAG.equals(xname))
                  {
                    xmap_area.setArrow(true);
                  }
                  else if(Rect.PUSHPIN_TAG.equals(xname))
                  {
                    xmap_area.setPushpin(true);
                  }
                  else if(Rect.HILITE_TAG.equals(xname))
                  {
                    Object xobject = el.elementAt(0);

                    if(xobject instanceof Symbol)
                    {
                      xmap_area.setHiliteColor(
                        new Integer(cvt_color(
                            xobject.toString(),
                            0xff)));
                    }
                  }
                  else if(Rect.LINECOLOR_TAG.equals(xname))
                  {
                    Object xobject = el.elementAt(0);

                    if(xobject instanceof Symbol)
                    {
                      xmap_area.setLineColor(
                        new Integer(cvt_color(
                            xobject.toString(),
                            0xff)));
                    }
                  }
                  else if(Rect.BGCOLOR_TAG.equals(xname))
                  {
                    Object xobject = el.elementAt(0);

                    if(xobject instanceof Symbol)
                    {
                      xmap_area.setBgColor(
                        new Integer(cvt_color(
                            xobject.toString(),
                            0xff)));
                    }
                  }
                  else if(Rect.TEXTCOLOR_TAG.equals(xname))
                  {
                    Object xobject = el.elementAt(0);

                    if(xobject instanceof Symbol)
                    {
                      xmap_area.setTextColor(
                        new Integer(cvt_color(
                            xobject.toString(),
                            0xff)));
                    }
                  }
                  else if(Rect.OPACITY_TAG.equals(xname))
                  {
                    Object xobject = el.elementAt(0);

                    if(xobject instanceof Integer)
                    {
                      xmap_area.setOpacity((Integer)xobject);
                    }
                  }
                  else if(Rect.LINE_WIDTH_TAG.equals(xname))
                  {
                    Object xobject = el.elementAt(0);

                    if(xobject instanceof Integer)
                    {
                      xmap_area.setLineWidth((Integer)xobject);
                    }
                  }
                  else
                  {
                    final int border_type =
                      (Rect.NO_BORDER_TAG.equals(xname)
                      ? Rect.NO_BORDER
                      : (Rect.XOR_BORDER_TAG.equals(xname)
                      ? Rect.XOR_BORDER
                      : (Rect.SOLID_BORDER_TAG.equals(xname)
                      ? Rect.SOLID_BORDER
                      : (Rect.SHADOW_IN_BORDER_TAG.equals(xname)
                      ? Rect.SHADOW_IN_BORDER
                      : (Rect.SHADOW_OUT_BORDER_TAG.equals(xname)
                      ? Rect.SHADOW_OUT_BORDER
                      : (Rect.SHADOW_EIN_BORDER_TAG.equals(xname)
                      ? Rect.SHADOW_EIN_BORDER
                      : (Rect.SHADOW_EOUT_BORDER_TAG.equals(xname)
                      ? Rect.SHADOW_EOUT_BORDER
                      : (-1))))))));

                    if(border_type >= 0)
                    {
                      xmap_area.setBorderType(border_type);

                      for(int xpos = 0; xpos < el.size(); ++xpos)
                      {
                        Object xobject = el.elementAt(xpos);

                        if(xobject instanceof Symbol)
                        {
                          xmap_area.setBorderColor(
                            new Integer(cvt_color(
                                xobject.toString(),
                                0xff)));
                        }
                        else if(xobject instanceof Number)
                        {
                          xmap_area.setBorderWidth(
                            ((Number)xobject).intValue());
                        }
                      }
                    }
                  }
                }
              }

              map_area.addElement(xmap_area);
            }
          }
          catch(final Throwable exp)
          {
            printStackTrace(exp);
          }
        }
      }
    }

    return map_area;
  }

  // parse displaf mode from the specified parser.
  private static int parseMode(final LispParser parser)
  {
    final NamedVector obj = parser.getNamedVector(MODE_TAG);

    return ((obj != null) && (obj.size() > 0))
    ? lookupMode(obj.elementAt(0).toString())
    : UNSPEC;
  }

  //~ Inner Classes ----------------------------------------------------------

  /**
   * The class converts strings in a lisp like syntax into a tree of tokens.
   *
   * @author $author$
   * @version $Revision: 1.10 $
   */
  static class LispParser
    extends NamedVector
  {
    //~ Constructors ---------------------------------------------------------

    /**
     * Creates a new LispParser object.
     *
     * @param lispString the string to be parsed
     */
    public LispParser(final String lispString)
    {
      super("toplevel");

      final NumContext start = new NumContext(0);
      parse(this, lispString, start);
    }

    //~ Methods --------------------------------------------------------------

    /**
     * Get the first or last NameVector with the specified name.
     *
     * @param name Name to look for.
     * @param last true if the last value should be returned
     *
     * @return the found value
     */
    public NamedVector getNamedVector(
      final String  name,
      final boolean last)
    {
      if(last)
      {
        for(int pos = size(); --pos >= 0;)
        {
          final Object object = elementAt(pos);

          if(
            (object instanceof NamedVector)
            && name.equals(((NamedVector)object).getName()))
          {
            return (NamedVector)object;
          }
        }
      }
      else
      {
        for(int pos = 0; pos < size(); ++pos)
        {
          final Object object = elementAt(pos);

          if(object instanceof NamedVector)
          {
            final NamedVector retval = (NamedVector)object;

            if(name.equals(retval.getName()))
            {
              return retval;
            }
          }
        }
      }

      return null;
    }

    /**
     * Get the last NameVector with the specified name.
     *
     * @param name Name to look for.
     *
     * @return the found value
     */
    public NamedVector getNamedVector(final String name)
    {
      return getNamedVector(name, true);
    }

    // Get the next token starting from start.
    private static Token getToken(
      final String     string,
      final NumContext start)
    {
      int pos = skip_white_space(
          string,
          start.intValue());
      final char c = string.charAt(pos);

      switch(c)
      {
        case '(' :
        {
          start.set(pos + 1);

          return new Token(Token.OPEN_PAR, null);
        }
        case ')' :
        {
          start.set(pos + 1);

          return new Token(Token.CLOSE_PAR, null);
        }
        case '-' :
        case '0' :
        case '1' :
        case '2' :
        case '3' :
        case '4' :
        case '5' :
        case '6' :
        case '7' :
        case '8' :
        case '9' :
        {
          int startNumber = pos;
          int endNumber = startNumber + 1;

          for(; endNumber < string.length(); endNumber++)
          {
            if(!Character.isDigit(string.charAt(endNumber)))
            {
              break;
            }
          }

          start.set(endNumber);

          return new Token(
            Token.OBJECT,
            new Integer(string.substring(startNumber, endNumber)));
        }
        case '"' :
        {
          StringBuffer str = new StringBuffer();
          char         ch = 0;

          for(int esc = 0;; str.append(ch))
          {
            ch = string.charAt(++pos);

            if(ch == '\\')
            {
              ++esc;
            }
            else if(ch == '"')
            {
              str.setLength(str.length() - ((esc + 1) / 2));

              if((esc & 1) == 0)
              {
                break;
              }

              esc = 0;
            }
            else
            {
              esc = 0;
            }
          }

          start.set(pos + 1);

          return new Token(
            Token.OBJECT,
            str.toString());
        }
        default :
        {
          StringBuffer str = new StringBuffer();
          str.append(c);

          for(;;)
          {
            final char ch = string.charAt(++pos);

            if(ch == ')')
            {
              pos--;

              break;
            }

            if(Character.isWhitespace(ch))
            {
              break;
            }

            str.append(ch);
          }

          start.set(pos + 1);

          return new Token(
            Token.OBJECT,
            new Symbol(str.toString()));
        }
      }
    }

    // Parse the specified string for tokens, starting from start and add them to the
    // NamedVector list.
    private static void parse(
      final NamedVector list,
      final String      string,
      final NumContext  start)
    {
      try
      {
        while(start.intValue() < string.length())
        {
          final Token token = getToken(string, start);

          switch(token.type)
          {
            case Token.OPEN_PAR :
            {
              if(Character.isWhitespace(string.charAt(start.intValue())))
              {
                throw new IllegalArgumentException("Expected Token");
              }

              final Token tok = getToken(string, start);

              // We will convert it to LIST later
              final Symbol symbol = (Symbol)tok.object; // This object should be OBJECT

              // OK. Get the object contents
              final NamedVector xlist = new NamedVector(symbol.toString());
              parse(xlist, string, start);
              list.addElement(xlist);

              break;
            }
            case Token.CLOSE_PAR :
              return;
            default :
              list.addElement(token.object);
          }
        }
      }
      catch(final Throwable exp)
      {
        printStackTrace(exp);
      }
    }

    // Called to skip white space in a string.
    private static int skip_white_space(
      final String str,
      int          pos)
    {
      for(;; pos++)
      {
        if(!Character.isWhitespace(str.charAt(pos)))
        {
          return pos;
        }
      }
    }

    //~ Inner Classes --------------------------------------------------------

    /**
     * This class represents a lisp token.
     */
    static final class Token
    {
      //~ Static fields/initializers -----------------------------------------

      /** Value to indicate an open parethesis. */
      static final int OPEN_PAR = 0;

      /** Value to indicate an close parethesis. */
      static final int CLOSE_PAR = 1;

      /** Value to indicate any other token. */
      static final int OBJECT = 2;

      //~ Instance fields ----------------------------------------------------

      /** The token value. */
      final Object object;

      /** The type of token. */
      final int type;

      //~ Constructors -------------------------------------------------------

      /**
       * Creates a new Token object.
       *
       * @param type token type.
       * @param object object represented by the token
       */
      Token(
        int    type,
        Object object)
      {
        this.type     = type;
        this.object   = object;
      }
    }
  }

  /**
   * This class is an extension of Vector which is assigned a name.
   */
  static class NamedVector
    extends Vector
  {
    //~ Instance fields ------------------------------------------------------

    private final String name;

    //~ Constructors ---------------------------------------------------------

    /**
     * Creates a new NamedVector object.
     *
     * @param name the name for this vector
     */
    NamedVector(final String name)
    {
      this.name = name;
    }

    //~ Methods --------------------------------------------------------------

    /**
     * Query the vector name.
     *
     * @return the vector name.
     */
    final String getName()
    {
      return name;
    }
  }

  /**
   * A symbol is just a wrapped string.
   */
  static final class Symbol
  {
    //~ Instance fields ------------------------------------------------------

    private final String symbol;

    //~ Constructors ---------------------------------------------------------

    /**
     * Creates a new Symbol object.
     *
     * @param symbol DOCUMENT ME!
     */
    Symbol(String symbol)
    {
      this.symbol = symbol;
    }

    //~ Methods --------------------------------------------------------------

    /**
     * Reports the symbol string.
     *
     * @return the wrapped string.
     */
    public String toString()
    {
      return symbol;
    }
  }
}
