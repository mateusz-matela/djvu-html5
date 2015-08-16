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
 * This class represents a single pixel.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.7 $
 */
public class GPixel
  implements Cloneable
{
  //~ Static fields/initializers ---------------------------------------------

  /** A white pixel. */
  public static final GPixel WHITE = new GPixel((byte)-1, (byte)-1, (byte)-1);

  /** A black pixel. */
  public static final GPixel BLACK = new GPixel((byte)0, (byte)0, (byte)0);

  /** A blue pixel. */
  public static final GPixel BLUE = new GPixel((byte)0, (byte)0, (byte)-1);

  /** A green pixel. */
  public static final GPixel GREEN = new GPixel((byte)0, (byte)-1, (byte)0);

  /** A red pixel. */
  public static final GPixel RED = new GPixel((byte)-1, (byte)0, (byte)0);

  /** Number of colors in a pixel.  */
  public static final int NUMELEMS = 3;

  //~ Instance fields --------------------------------------------------------

  private byte blue;
  private byte green;
  private byte red;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new GPixel object.
   */
  public GPixel()
  {
    blue = green = red = -51;
  }

  /**
   * Creates a new GPixel object.
   *
   * @param blue pixel value
   * @param green pixel value
   * @param red pixel value
   */
  public GPixel(
    byte blue,
    byte green,
    byte red)
  {
    setBlue(blue);
    setGreen(green);
    setRed(red);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Initialize a pixel with bgr values.
   *
   * @param blue pixel value
   * @param green pixel value
   * @param red pixel value
   */
  public void setBGR(
    int blue,
    int green,
    int red)
  {
    setBlue(blue);
    setRed(red);
    setGreen(green);
  }

  /**
   * Set the blue color
   *
   * @param blue pixel value
   */
  public void setBlue(final byte blue)
  {
    this.blue = blue;
  }

  /**
   * Set the blue color
   *
   * @param blue pixel value
   */
  public final void setBlue(final int blue)
  {
    setBlue((byte)blue);
  }

  /**
   * Query the blue color.
   *
   * @return pixel value
   */
  public final int getBlue()
  {
    return 0xff & blueByte();
  }

  /**
   * Query the blue color.
   *
   * @return pixel value
   */
  public byte blueByte()
  {
    return blue;
  }

  /**
   * Create a clone of this pixel.
   *
   * @return the cloned pixel
   */
  public Object clone()
  {
    //DjVuOptions.out.println("1. GPixel clone");
    Cloneable retval = null;

    try
    {
      retval = (GPixel)super.clone();
    }
    catch(final CloneNotSupportedException ignored) {}

    //DjVuOptions.out.println("2. GPixel clone");
    return retval;
  }

  /**
   * Test if two pixels are equal.
   *
   * @param object pixel to compare to
   *
   * @return true if red, green, and blue values are all equal
   */
  public final boolean equals(Object object)
  {
    if(!(object instanceof GPixel))
    {
      return false;
    }

    GPixel ref = (GPixel)object;

    return (ref.blueByte() == blueByte()) && (ref.greenByte() == greenByte())
    && (ref.redByte() == redByte());
  }

  /**
   * Set the gray color.
   *
   * @param gray pixel value
   */
  public final void setGray(final byte gray)
  {
    setBlue(gray);
    setRed(gray);
    setGreen(gray);
  }

  /**
   * Set the gray color.
   *
   * @param gray pixel value
   */
  public final void setGray(final int gray)
  {
    setGray((byte)gray);
  }

  /**
   * Set the gray color.
   *
   * @param green pixel value
   */
  public void setGreen(final byte green)
  {
    this.green = green;
  }

  /**
   * Set the green color.
   *
   * @param green pixel value
   */
  public final void setGreen(final int green)
  {
    setGreen((byte)green);
  }

  /**
   * Query the green color.
   *
   * @return green pixel value
   */
  public final int getGreen()
  {
    return 0xff & greenByte();
  }

  /**
   * Query the green color.
   *
   * @return green pixel value
   */
  public byte greenByte()
  {
    return green;
  }

  /**
   * Generates a hashCode equal to 0xffRRGGBB.
   *
   * @return hashCode of 0xffRRGGBB
   */
  public int hashCode()
  {
    return 0xff000000 | (getRed() << 16) | (getGreen() << 8) | getBlue();
  }

  /**
   * Set the red color.
   *
   * @param red pixel value
   */
  public void setRed(final byte red)
  {
    this.red = red;
  }

  /**
   * Set the red color.
   *
   * @param red pixel value
   */
  public final void setRed(final int red)
  {
    setRed((byte)red);
  }

  /**
   * Query the red color.
   *
   * @return red pixel value
   */
  public final int getRed()
  {
    return 0xff & redByte();
  }

  /**
   * Query the red color.
   *
   * @return red pixel value
   */
  public byte redByte()
  {
    return red;
  }

  /**
   * Copy the pixel values.
   *
   * @param ref pixel to copy
   */
  public final void set(final GPixel ref)
  {
    setBlue(ref.blueByte());
    setGreen(ref.greenByte());
    setRed(ref.redByte());
  }
}
