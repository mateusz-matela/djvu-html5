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
import java.util.Vector;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.4 $
 */
class JB2Shape
  extends DjVuObject
  implements Cloneable
{
  //~ Instance fields --------------------------------------------------------

  /** DOCUMENT ME! */
  public int parent;

  /** DOCUMENT ME! */
  public long userdata = 0L;

  /** DOCUMENT ME! */
  private GBitmap bits;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new JB2Shape object.
   *
   * @param parent DOCUMENT ME!
   */
  public JB2Shape(final int parent)
  {
    init(parent);
  }

  /**
   * Creates a new JB2Shape object.
   */
  public JB2Shape()
  {
    parent   = 0;
    bits     = null;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of JB2Shape with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of JB2Shape.
   */
  public static JB2Shape createJB2Shape(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (JB2Shape)create(
      options,
      options.getJB2ShapeClass(),
      JB2Shape.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public GBitmap getGBitmap()
  {
    return bits;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public Object clone()
  {
    //verbose("1. JB2Shape clone");
    JB2Shape retval = duplicate();

    if(retval.bits != null)
    {
      retval.bits = (GBitmap)retval.bits.clone();
    }

    //verbose("2. JB2Shape clone");
    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public JB2Shape duplicate()
  {
    Cloneable retval = this;

    try
    {
      retval = (JB2Shape)super.clone();
    }
    catch(CloneNotSupportedException ignored) {}

    return (JB2Shape)retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @param parent DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public JB2Shape init(final int parent)
  {
    this.parent   = parent;
    this.bits     = new GBitmap();

    return this;
  }
}
