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
 * Implements elliptical map areas. The only supported types of border are
 * NO_BORDER, XOR_BORDER and SOLID_BORDER. Its contents can not be
 * highlighted either.
 */
public class Oval
  extends Rect
{
  //~ Static fields/initializers ---------------------------------------------

  /** Tag name for this map type. */
  public static final String OVAL_TAG = "oval";

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Oval object.
   */
  public Oval() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query the map type.
   *
   * @return MAP_OVAL
   */
  public int getMapType()
  {
    return MAP_OVAL;
  }

  /**
   * Creates an instance of Oval with the options interherited from the
   * specified reference.
   * 
   * @param ref Object to interherit DjVuOptions from.
   * 
   * @return a new instance of Oval.
   */
  public static Oval createOval(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (Oval)create(
      options,
      options.getAnnoRectClass(),
      Oval.class);
  }

  /**
   * Returns "oval"
   *
   * @return QVAL_TAG
   */
  public String getShapeName()
  {
    return OVAL_TAG;
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
    || (border_type != XOR_BORDER))
    && (hilite_color == 0xffffffff)
    && super.isValid();
  }

  /**
   * Check if the point is inside the hyperlink area
   *
   * @param x horizontal position
   * @param y vertical position
   *
   * @return true if the given point is inside the hyperlink area
   */
  public boolean contains(
    final int x,
    final int y)
  {
    final GRect  bounds = getBounds();
    final double a   = (double)bounds.width() / (double)2;
    final double b   = (double)bounds.height() / (double)2;
    final double xb0 = (a + (double)(x - bounds.xmin)) * b;
    final double ya0 = (b + (double)(y - bounds.ymin)) * a;

    return ((xb0 * xb0) + (ya0 * ya0)) <= ((double)a * (double)b);
  }
}
