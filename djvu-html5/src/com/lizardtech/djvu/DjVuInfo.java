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


/**
 * This codec represents global information about a page.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.5 $
 */
public class DjVuInfo
  extends DjVuObject
  implements Cloneable, Codec
{
  //~ Instance fields --------------------------------------------------------

  /** The gamma correction factor. */
  public double gamma = 2.2D;

  /** The scanned resolution. */
  public int dpi = 300;

  /** Flags with addition details. */
  public int flags = 0;

  /** The height of the page in pixels at scanned resolution. */
  public int height = 0;

  /** The version of the format specification this page complies with. */
  public int version = 20;

  /** The width of the page in pixels at scanned resolution. */
  public int width = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DjVuInfo object.
   */
  public DjVuInfo() {}

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
   * Creates an instance of DjVuInfo with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of DjVuInfo.
   */
  public static DjVuInfo createDjVuInfo(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (DjVuInfo)create(
      options,
      options.getDjVuInfoClass(),
      DjVuInfo.class);
  }

  /**
   * Create a clone of this object.
   *
   * @return the newly created object
   */
  public Object clone()
  {
    //verbose("1. DjVuInfo clone");
    Cloneable retval = null;

    try
    {
      retval = (DjVuInfo)super.clone();
    }
    catch(CloneNotSupportedException ignored) {}

    //verbose("2. DjVuInfo clone");
    return retval;
  }

  /**
   * Decode this codec from the gived data source.
   *
   * @param pool data source to decode
   *
   * @throws IOException if an error occurs
   */
  public void decode(final CachedInputStream pool)
    throws IOException
  {
    final InputStream bs     = (CachedInputStream)pool.clone();
    final byte[]      buffer = new byte[10];
    final int         size   = bs.read(buffer);

    if(size < 5)
    {
      throw new IOException(
        "DjVuInfo: Corrupted file (truncated INFO chunk)");
    }

    width     = ((0xff & buffer[0]) << 8) | (0xff & buffer[1]);
    height    = ((0xff & buffer[2]) << 8) | (0xff & buffer[3]);
    version   = 0xff & buffer[4];

    if((size >= 6) && (buffer[5] != -1))
    {
      version = ((0xff & buffer[5]) << 8) | version;
    }

    if((size >= 8) && (buffer[7] != -1))
    {
      dpi = ((0xff & buffer[7]) << 8) | (0xff & buffer[6]);

      if((dpi < 25) || (dpi > 6000))
      {
        dpi = 300;
      }
    }

    if(size >= 9)
    {
      if((buffer[8] >= 3) || (buffer[8] <= 50))
      {
        gamma = 0.1D * (double)(buffer[8]);
      }
    }

    if(size >= 10)
    {
      flags = buffer[9];
    }

    if((width < 0) || (height < 0))
    {
      throw new IOException(
        "DjVu Decoder: Corrupted file (image size is zero)");
    }
  }
}
