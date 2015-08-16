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
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.7 $
 */
public class Palette
  extends DjVuObject
  implements Cloneable, Codec
{
  //~ Static fields/initializers ---------------------------------------------

  /** DOCUMENT ME! */
  public static final int DJVUPALETTEVERSION = 0;

  //~ Instance fields --------------------------------------------------------

  /**
   * Contains an optional sequence of color indices.  Function \Ref{encode}
   * and \Ref{decode} also encode and decode this sequence when such a
   * sequence is provided.
   */
  public int[] colordata = null;

  /** DOCUMENT ME! */
  private final Hashtable pmap = new Hashtable();

  // Quantization data
  private GPixel[] palette = null;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Palette object.
   */
  public Palette() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query if this is image data.
   *
   * @return true
   */
  public boolean isImageData()
  { 
      return true;
  }  

  /**
   * DOCUMENT ME!
   *
   * @param palette DOCUMENT ME!
   */
  public void setPalette(final GPixel[] palette)
  {
    this.palette = palette;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public GPixel[] getPalette()
  {
    return palette;
  }

  /**
   * Creates an instance of Palette with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of Palette.
   */
  public static Palette createPalette(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (Palette)create(
      options,
      options.getPaletteClass(),
      Palette.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public Object clone()
  {
    //verbose("1. Palette clone");
    Cloneable retval = null;

    try
    {
      retval = (Palette)super.clone();

      final GPixel[] oldPalette = getPalette();

      if(oldPalette != null)
      {
        final GPixel[] palette = new GPixel[oldPalette.length];
        ((Palette)retval).setPalette(palette);

        for(int i = 0; i < palette.length;)
        {
          palette[i] = (GPixel)oldPalette[i].clone();
        }
      }

      if(colordata != null)
      {
        ((Palette)retval).colordata = (int[])colordata.clone();
      }
    }
    catch(final CloneNotSupportedException ignored) {}

    //verbose("2. Palette clone");
    return retval;
  }

  /**
   * Returns colors from the color index sequence.  Pixel #out# is
   * overwritten with the color corresponding to the #nth# element of the
   * color sequence \Ref{colordata}.
   *
   * @param nth DOCUMENT ME!
   * @param out DOCUMENT ME!
   */
  public final void get_color(
    int          nth,
    final GPixel out)
  {
    index_to_color(colordata[nth], out);
  }

  /**
   * Returns the number of colors in the palette.
   *
   * @return DOCUMENT ME!
   */
  public final int size()
  {
    return getPalette().length;
  }

  /**
   * Returns the best palette index for representing color #bgr#.
   *
   * @param p DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public int color_to_index(final GPixel p)
  {
    final Number  key =
      new Integer((p.getBlue() << 16) | (p.getGreen() << 8) | (p.getRed()));
    final Integer retval = (Integer)pmap.get(key);

    return (retval != null)
    ? (retval.intValue())
    : color_to_index_slow(p);
  }

  /**
   * Overwrites #p# with the color located at position #index# in the
   * palette.
   *
   * @param index DOCUMENT ME!
   * @param p DOCUMENT ME!
   */
  public final void index_to_color(
    final int    index,
    final GPixel p)
  {
    p.set(getPalette()[index]);
  }

  /**
   * Initializes the object by reading data from bytestream #bs#.  This
   * function reads a version byte, the palette size, the palette and the
   * color index sequence from bytestream #bs#.
   *
   * @param pool DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public void decode(final CachedInputStream pool)
    throws IOException
  {
    final InputStream input = (CachedInputStream)pool.clone();

    // Make sure that everything is clear
    pmap.clear();

    // Code version
    int version = input.read();

    if((version & 0x7f) != DJVUPALETTEVERSION)
    {
      throw new IOException("bad palette version " + version);
    }

    // Code palette
    int palettesize = (input.read() << 8);
    palettesize |= input.read();

    if(palettesize < 0)
    {
      throw new IOException("Bad palette size " + palettesize);
    }

    final GPixel[] palette = new GPixel[palettesize];

    for(int c = 0; c < palettesize; c++)
    {
      final byte b = (byte)input.read();
      final byte g = (byte)input.read();
      final byte r = (byte)input.read();
      palette[c] = new GPixel(b, g, r);
    }

    setPalette(palette);

    // Code data
    if((version & 0x80) != 0)
    {
      int datasize = (input.read() << 16);
      datasize |= (input.read() << 8);
      datasize |= input.read();

      if(datasize < 0)
      {
        throw new IOException("bad palette datasize");
      }

      colordata = new int[datasize];

      final InputStream bsinput =
        BSInputStream.createBSInputStream(this).init(input);

      for(int d = 0; d < datasize; d++)
      {
        int s = (bsinput.read() << 8);
        s |= bsinput.read();

        if((s < 0) || (s >= palettesize))
        {
          throw new IOException("bad palette data");
        }

        colordata[d] = s;
      }
    }
  }

  /**
   * Reads palette colors.  This function initializes the palette colors by
   * reading #palettesize# RGB triples from bytestream #bs#.
   *
   * @param input DOCUMENT ME!
   * @param palettesize DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public void decode_rgb_entries(
    final InputStream input,
    final int         palettesize)
    throws IOException
  {
    final GPixel[] palette = new GPixel[palettesize];

    for(int c = 0; c < palettesize; c++)
    {
      final byte r = (byte)input.read();
      final byte g = (byte)input.read();
      final byte b = (byte)input.read();
      palette[c] = new GPixel(b, g, r);
    }

    setPalette(palette);
  }

  // Helpers
  private int color_to_index_slow(final GPixel p)
  {
    final GPixel[] palette = getPalette();
    final int      ncolors = palette.length;

    // Should be able to do better
    int found     = 0;
    int founddist = 3 * 256 * 256;

    for(int i = 0; i < ncolors; i++)
    {
      final GPixel q    = palette[i];
      final int    bd   = 0xff & (p.blueByte() - q.blueByte());
      final int    gd   = 0xff & (p.greenByte() - q.greenByte());
      final int    rd   = 0xff & (p.redByte() - q.redByte());
      final int    dist = (bd * bd) + (gd * gd) + (rd * rd);

      if(dist < founddist)
      {
        found       = i;
        founddist   = dist;
      }
    }

    // Store in pmap
    if(pmap.size() < 0x8000)
    {
      final Number key =
        new Integer((p.getBlue() << 16) | (p.getGreen() << 8) | p.getRed());
      pmap.put(
        key,
        new Integer(found));
    }

    // Return
    return found;
  }
}
