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
 * Implements an editable Integer class.
 *
 * @author $author$
 * @version $Revision: 1.3 $
 */
public final class NumContext
  extends Number
{
  //~ Instance fields --------------------------------------------------------

  // the primative value of this NumContext.
  private int value = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new NumContext object.
   */
  public NumContext()
  {
    value = 0;
  }

  /**
   * Creates a new NumContext object.
   *
   * @param value DOCUMENT ME!
   */
  public NumContext(final int value)
  {
    this.value = value;
  }

  /**
   * Creates a new NumContext object.
   *
   * @param value DOCUMENT ME!
   */
  public NumContext(final Number value)
  {
    this.value = value.intValue();
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Query the primative value as a double.
   *
   * @return double value.
   */
  public final double doubleValue()
  {
    return (double)longValue();
  }

  /**
   * Tests if this is equal.
   *
   * @param value DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final boolean equals(final Object value)
  {
    return (value instanceof NumContext)
    && (this.value == ((NumContext)value).intValue());
  }

  /**
   * Query the primative value as a float.
   *
   * @return float value.
   */
  public final float floatValue()
  {
    return (float)longValue();
  }

  /**
   * Returns a hash code for this Object.
   *
   * @return DOCUMENT ME!
   */
  public final int hashCode()
  {
    return this.value;
  }

  /**
   * Query the primative value.
   *
   * @return integer value.
   */
  public final int intValue()
  {
    return value;
  }

  /**
   * Query the primative value as a long.
   *
   * @return long value.
   */
  public final long longValue()
  {
    return 0xffffffffL & value;
  }

  /**
   * Set the primative value from a long.
   *
   * @param value long value.
   */
  public final void set(long value)
  {
    this.value = (int)value;
  }

  /**
   * Set the primative value from an int.
   *
   * @param value int value.
   */
  public final void set(int value)
  {
    this.value = value;
  }

  /**
   * Set the primative value from a Number.
   *
   * @param value Number object.
   */
  public final void set(Number value)
  {
    this.value = value.intValue();
  }
}
