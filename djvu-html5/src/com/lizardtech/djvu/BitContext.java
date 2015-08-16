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
 * This class impliments a mutable Byte type class.  The byte is reported in the
 * range of 0 to 255.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.7 $
 */
public class BitContext
  extends Number
  implements Cloneable
{
  //~ Instance fields --------------------------------------------------------

  /**
   * The raw value.  Try to use the methods, unless speed is of the essence.
   */
  public byte bit = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new BitContext object.
   */
  public BitContext()
  {
    bit = 0;
  }

  /**
   * Creates a new BitContext object.
   *
   * @param s DOCUMENT ME!
   */
  public BitContext(final short s)
  {
    bit = (byte)s;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Query the raw byte value.
   *
   * @return the raw byte value
   */
  public final byte byteValue()
  {
    return bit;
  }

  /**
   * Clone this object.
   *
   * @return the newly created clone
   */
  public Object clone()
  {
    //DjVuOptions.out.println("1. BitContext clone");
    Cloneable retval = null;

    try
    {
      retval = (BitContext)super.clone();
    }
    catch(final CloneNotSupportedException ignored) {}

    //DjVuOptions.out.println("2. BitContext clone");
    return retval;
  }

  /**
   * Query the double value.
   *
   * @return (double)intValue()
   */
  public final double doubleValue()
  {
    return (double)intValue();
  }

  /**
   * Query the float value.
   *
   * @return (float)intValue()
   */
  public final float floatValue()
  {
    return (float)intValue();
  }

  /**
   * Query the integer value.
   *
   * @return the byte value ranged 0 to 255
   */
  public final int intValue()
  {
    return 0xff & bit;
  }

  /**
   * Query the long value.
   *
   * @return the byte value ranged 0L to 255L
   */
  public final long longValue()
  {
    return 0xff & bit;
  }

  /**
   * Set the byte value.
   *
   * @param value the raw value to set
   */
  public final void set(final int value)
  {
    bit = (byte)value;
  }

  /**
   * Set the byte value from a Number object.
   *
   * @param value the raw value to set
   */
  public final void set(final Number value)
  {
    bit = value.byteValue();
  }

  /**
   * Query the short value.
   *
   * @return the byte value ranged 0 to 255
   */
  public final short shortValue()
  {
    return (short)(0xff & bit);
  }
}
