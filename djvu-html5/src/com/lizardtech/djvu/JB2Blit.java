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
 * This class represent a JB2 encoded shape.
 *
 * @author $author$
 * @version $Revision: 1.5 $
 */
class JB2Blit
  implements Cloneable
{
  //~ Instance fields --------------------------------------------------------

  /** Index of the shape to blit. */
  protected int shapeno = 0;

  /** Vertical coordinate of the blit. */
  protected short bottom = 0;

  /** Horizontal coordinate of the blit. */
  protected short left = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new JB2Blit object.
   */
  JB2Blit() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final int bottom()
  {
    return 0xffff & bottom;
  }

  /**
   * DOCUMENT ME!
   *
   * @param bottom DOCUMENT ME!
   */
  public final void bottom(int bottom)
  {
    this.bottom = (short)bottom;
  }

  /**
   * DOCUMENT ME!
   *
   * @param bottom DOCUMENT ME!
   */
  public final void bottomAdd(int bottom)
  {
    this.bottom += (short)bottom;
  }

  /**
   * DOCUMENT ME!
   *
   * @param bottom DOCUMENT ME!
   */
  public final void bottomSubtract(int bottom)
  {
    this.bottom -= (short)bottom;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public Object clone()
  {
    //DjVuOptions.out.println("1. JB2Blit clone");
    Cloneable retval = null;

    try
    {
      retval = (JB2Blit)super.clone();
    }
    catch(final CloneNotSupportedException ignored) {}

    //DjVuOptions.out.println("2. JB2Blit clone");
    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final int left()
  {
    return 0xffff & left;
  }

  /**
   * DOCUMENT ME!
   *
   * @param left DOCUMENT ME!
   */
  public final void left(int left)
  {
    this.left = (short)left;
  }

  /**
   * DOCUMENT ME!
   *
   * @param left DOCUMENT ME!
   */
  public final void leftAdd(int left)
  {
    this.left += (short)left;
  }

  /**
   * DOCUMENT ME!
   *
   * @param left DOCUMENT ME!
   */
  public final void leftSubtract(int left)
  {
    this.left -= (short)left;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final int shapeno()
  {
    return shapeno;
  }

  /**
   * DOCUMENT ME!
   *
   * @param shapeno DOCUMENT ME!
   */
  public final void shapeno(int shapeno)
  {
    this.shapeno = shapeno;
  }
}
