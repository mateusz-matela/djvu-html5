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
 * This class represents 24 bit color image maps.
 *
 * @author Bill C. Riemers
 * @version $ $
 */
public class GPixmap
  extends GMap
  implements Cloneable
{
  //~ Static fields/initializers ---------------------------------------------

  /** Used to represent division as multiplication. */
  protected final static int[] invmap = new int[256];

  /** Indentity color correction table. */
  protected final static int[] ctableI = new int[256];

  /** Cached color correction table. */
  protected static int[] ctable = new int[256];

  /**
   * The color correction subsample for the cached color table. 
   */
  protected static double lgamma = -1D;

  /** Used to quickly clip out of bounds values. */
  protected final static byte[] clip = new byte[512];

  /** Used for attenuation */
  protected final static Object [] multiplierRefArray=new Object[256];
  
  /**
   * Static initializers.
   */
  static
  {
    for(int i = 0; i < clip.length; i++)
    {
      clip[i] = (byte)((i < 256)
        ? i
        : 255);
    }
    for(int i = 1; i < invmap.length; i++)
    {
      invmap[i] = 0x10000 / i;
    }
    for(int i = 0; i < ctableI.length; i++)
    {
      ctableI[i] = i;
    }
    for(int i = 1; i < multiplierRefArray.length;)
    {
      multiplierRefArray[i++] = null;
    }
  }

  //~ Instance fields --------------------------------------------------------

  private final GPixelReference ref      = createGPixelReference(-1);

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new GPixmap object.
   */
  public GPixmap() 
  {
      super(3,2,1,0, false);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of GPixmap with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of GPixmap.
   */
  public static GPixmap createGPixmap(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (GPixmap)create(
      options,
      options.getGPixmapClass(),
      GPixmap.class);
  }

  /**
   * Fill the array with color correction constants.
   * 
   * @param gamma color correction subsample
   * 
   * @return the new color correction table
   */
  public static synchronized int [] getColorCorrection(
    double  gamma)
  {
    if((gamma < 0.10000000000000001D) || (gamma > 10D))
    {
      DjVuOptions.err.println("(GPixmap::color_correct) Illegal parameter");
    }
    int [] retval;
    if((gamma < 1.0009999999999999D) && (gamma > 0.999D))
    {
      retval=ctableI;
    }
    else
    {
      if(gamma != lgamma)
      {
        ctable=new int[256];
        for(int i = 0; i < 256; i++)
        {
          double x = (double)i / 255D;

          if(DjVuOptions.BEZIERGAMMA)
          {
            double t =
              (Math.sqrt(1.0D + (((gamma * gamma) - 1.0D) * x)) - 1.0D) / (gamma
              - 1.0D);
            x = ((((1.0D - gamma) * t) + (2D * gamma)) * t) / (gamma + 1.0D);
          }
          else
          {
            x = Math.pow(x, 1.0D / gamma);
          }

          ctable[i] = (int)Math.floor((255D * x) + 0.5D);
        }
        lgamma = gamma;
      }
      retval=ctable;
    }
    return retval;
  }
  
  /**
   * Creates or retrieves a cached multiplier array to use when attenuating.
   *
   * @return attenuation array
   */
  protected static int [] getMultiplier(final int maxgray)
  {
    int [] retval=(int[])getFromReference(multiplierRefArray[maxgray]);
    if(retval == null)
    {
      retval = new int[maxgray];

      for(int i = 0; i < maxgray; i++)
      {
        retval[i] = 0x10000 - ((i << 16) / maxgray);
      }
      multiplierRefArray[maxgray]=createSoftReference(retval,null);
    }
    return retval;
  }

  /**
   * Attenuate the specified bitmap.
   * 
   * @param bm Bitmap to attenuate
   * @param xpos horizontal position
   * @param ypos vertical position
   */
  public void attenuate(
    final GBitmap bm,
    int           xpos,
    int           ypos)
  {
    // Check
    // Compute number of rows and columns
    int xrows = ypos + bm.rows();

    if(xrows > rows())
    {
      xrows = rows();
    }

    if(ypos > 0)
    {
      xrows -= ypos;
    }

    int xcolumns = xpos + bm.columns();

    if(xcolumns > columns())
    {
      xcolumns = columns();
    }

    if(xpos > 0)
    {
      xcolumns -= xpos;
    }

    if((xrows <= 0) || (xcolumns <= 0))
    {
      return;
    }

    // Precompute multiplier map
    final int   maxgray    = bm.getGrays() - 1;
    final int[] multiplier = getMultiplier(maxgray);

    // Compute starting point
    int src = bm.rowOffset((ypos < 0)
        ? (-ypos)
        : 0) - ((xpos < 0)
      ? xpos
      : 0);
    int dst = rowOffset((ypos > 0)
        ? ypos
        : 0) + ((xpos > 0)
      ? xpos
      : 0);

    final GPixelReference dstPixel = createGPixelReference(0);

    // Loop over rows
    for(int y = 0; y < xrows; y++)
    {
      // Loop over columns
      dstPixel.setOffset(dst);

      for(int x = 0; x < xcolumns; dstPixel.incOffset())
      {
        int srcpix = bm.getByteAt(src + (x++));

        // Perform pixel operation
        if(srcpix > 0)
        {
          if(srcpix >= maxgray)
          {
            dstPixel.setGray(0);
          }
          else
          {
            final int level = multiplier[srcpix];
            dstPixel.setBGR(
              (dstPixel.getBlue() * level) >> 16,
              (dstPixel.getGreen() * level) >> 16,
              (dstPixel.getRed() * level) >> 16);
          }
        }
      }

      // Next line
      dst += getRowSize();
      src += bm.getRowSize();
    }
  }

  /**
   * Insert the specified bitmap with the specified color.
   * 
   * @param bm bitmap to insert
   * @param xpos horizontal position
   * @param ypos vertical position
   * @param color color to insert bitmap with
   */
  public void blit(
    final GBitmap bm,
    int           xpos,
    int           ypos,
    final GPixel  color)
  {
    // Check
    if(color == null)
    {
      return;
    }

    // Compute number of rows and columns
    int xrows = ypos + bm.rows();

    if(xrows > rows())
    {
      xrows = rows();
    }

    if(ypos > 0)
    {
      xrows -= ypos;
    }

    int xcolumns = xpos + bm.columns();

    if(xcolumns > columns())
    {
      xcolumns = columns();
    }

    if(xpos > 0)
    {
      xcolumns -= xpos;
    }

    if((xrows <= 0) || (xcolumns <= 0))
    {
      return;
    }

    // Precompute multiplier map
    final int   maxgray    = bm.getGrays() - 1;
    final int[] multiplier = new int[maxgray];

    for(int i = 0; i < maxgray; i++)
    {
      multiplier[i] = 0x10000 - ((i << 16) / maxgray);
    }

    // Cache target color
    int gr = color.getRed();
    int gg = color.getGreen();
    int gb = color.getBlue();

    // Compute starting point
    int src = bm.rowOffset((ypos < 0)
        ? (-ypos)
        : 0) - ((xpos < 0)
      ? xpos
      : 0);
    int dst = ((ypos > 0)
      ? rowOffset(ypos)
      : 0) + ((xpos > 0)
      ? xpos
      : 0);

    final GPixelReference dstPixel = createGPixelReference(dst);

    // Loop over rows
    for(int y = 0; y < xrows; y++)
    {
      // Loop over columns
      dstPixel.setOffset(dst);

      for(int x = 0; x < xcolumns; dstPixel.incOffset())
      {
        final int srcpix = bm.getByteAt(src + (x++));

        // Perform pixel operation
        if(srcpix != 0)
        {
          if(srcpix >= maxgray)
          {
            dstPixel.setBGR(gb, gg, gr);
          }
          else
          {
            final int level0 = multiplier[srcpix];
            final int level1 = 0x10000 - level0;
            dstPixel.setBGR(
              clip[((dstPixel.getBlue() * level0) + (gb * level1)) >> 16],
              clip[((dstPixel.getGreen() * level0) + (gg * level1)) >> 16],
              clip[((dstPixel.getRed() * level0) + (gr * level1)) >> 16]);
          }
        }
      }

      // Next line
      dst += getRowSize();
      src += bm.getRowSize();
    }
  }

  /**
   * Correct the colors with a gamma subsample normalized to 1.0 for no correction.
   * 
   * @param gamma color correction
   */
  public final void applyGammaCorrection(final double gamma)
  {
    if(
      true
      || ((gamma > 0.999D)
      && (gamma < 1.0009999999999999D)))
    {
      return;
    }

    int [] gtable=getColorCorrection(gamma);

    for(int i = 0; i < data.length; i++)
    {
      data[i] = (byte)gtable[data[i]];
    }
  }

  /**
   * Fill this image from another source at reduced resolution.  Pixel 
   * averaging will be used.
   * 
   * @param src image map to reduce
   * @param subsample rate to subsample
   * @param pdr target bounds
   */
  public void downsample(
    GMap src,
    int  subsample,
    GRect   pdr)
  {
    GRect rect =
      new GRect(
        0,
        0,
        ((src.columns() + subsample) - 1) / subsample,
        ((src.rows() + subsample) - 1) / subsample);

    if(pdr != null)
    {
      if(
        (pdr.xmin < rect.xmin)
        || (pdr.ymin < rect.ymin)
        || (pdr.xmax > rect.xmax)
        || (pdr.ymax > rect.ymax))
      {
        throw new IllegalArgumentException(
          "(GPixmap::downsample) Specified rectangle overflows destination pixmap");
      }
      rect = pdr;
    }

    init(
      rect.height(),
      rect.width(),
      null);

    int                   sy   = rect.ymin * subsample;
    int                   sxz  = rect.xmin * subsample;
    int                   sidx = src.rowOffset(sy);
    int                   didx = 0;

    final GPixelReference sptr = src.createGPixelReference(0);
    final GPixelReference dptr = createGPixelReference(0);

    for(int y = 0; y < rows(); y++)
    {
      int sx = sxz;

      for(int x = columns(); x-- > 0; dptr.incOffset())
      {
        int r    = 0;
        int g    = 0;
        int b    = 0;
        int s    = 0;
        int kidx = sidx;
        int lsy  = sy + subsample;

        if(lsy > src.rows())
        {
          lsy = src.rows();
        }

        int lsx = sx + subsample;

        if(lsx > src.columns())
        {
          lsx = src.columns();
        }

        for(int rsy = sy; rsy < lsy; rsy++)
        {
          sptr.setOffset(kidx + sx);
          if(! isRampNeeded())
          {
            for(int rsx = lsx - sx; rsx-- > 0; sptr.incOffset())
            {
              r += sptr.getRed();
              g += sptr.getGreen();
              b += sptr.getBlue();
              s++;
            }
          }
          else
          {
            for(int rsx = lsx - sx; rsx-- > 0; sptr.incOffset())
            {
              final GPixel pix=src.ramp(sptr);
              r += pix.getRed();
              g += pix.getGreen();
              b += pix.getBlue();
              s++;
            }
          }

          kidx += src.getRowSize();
        }

        if(s >= invmap.length)
        {
          dptr.setBGR(b / s, g / s, r / s);
        }
        else
        {
          dptr.setBGR(
            ((b * invmap[s]) + 32768) >> 16,
            ((g * invmap[s]) + 32768) >> 16,
            ((r * invmap[s]) + 32768) >> 16);
        }

        sx += subsample;
      }

      sy += subsample;
      sidx += src.rowOffset(subsample);
      dptr.setOffset(didx += getRowSize());
    }
  }

  /**
   * Fill this image from another source at reduced resolution of 4 vertical
   * pixels to 3.  An extrapulating pixel averaging algorithm is used. 
   * 
   * @param src image map to reduce
   * @param pdr target bounds
   *
   * @throws IllegalArgumentException if the target rectangle is out of bounds
   */
  public void downsample43(
    final GMap src,
    final GRect pdr)
  {
    final int srcwidth   = src.columns();
    final int srcheight  = src.rows();
    int       destwidth  = (int)Math.ceil((double)srcwidth*0.75D);
    int       destheight = (int)Math.ceil((double)srcheight*0.75D);
    GRect     rect       = new GRect(0, 0, destwidth, destheight);

    if(pdr != null)
    {
      if(
        (pdr.xmin < rect.xmin)
        || (pdr.ymin < rect.ymin)
        || (pdr.xmax > rect.xmax)
        || (pdr.ymax > rect.ymax))
      {
        throw new IllegalArgumentException(
          "rectangle out of bounds" + "pdr=(" + pdr.xmin + "," + pdr.ymin
          + "," + pdr.xmax + "," + pdr.ymax + "),rect=(" + rect.xmin + ","
          + rect.ymin + "," + rect.xmax + "," + rect.ymax + ")");
      }

      rect         = pdr;
      destwidth    = rect.width();
      destheight   = rect.height();
    }

    init(destheight, destwidth, null);

    int sy = rect.ymin / 3;
    int dy = rect.ymin - (3 * sy);

//    if(dy < 0)
//    {
//      sy--;
//      dy += 3;
//    }

    int sxz = rect.xmin / 3;
    int dxz = rect.xmin - (3 * sxz);

    if(dxz < 0)
    {
      sxz--;
      dxz += 3;
    }

    sxz *= 4;
    sy *= 4;

    final GPixelReference spix0 = src.createGPixelReference(0);
    final GPixelReference spix1 = src.createGPixelReference(0);
    final GPixelReference spix2 = src.createGPixelReference(0);
    final GPixelReference spix3 = src.createGPixelReference(0);
    final GPixelReference dpix0 = createGPixelReference(0);
    final GPixelReference dpix1 = createGPixelReference(0);
    final GPixelReference dpix2 = createGPixelReference(0);
    while(dy < destheight)
    {
      spix0.setOffset(sy++, sxz);

      if(sy >= srcheight)
      {
        sy--;
      }

      spix1.setOffset(sy++, sxz);

      if(sy >= srcheight)
      {
        sy--;
      }

      spix2.setOffset(sy++, sxz);

      if(sy >= srcheight)
      {
        sy--;
      }

      spix3.setOffset(sy++, sxz);
      
      dpix0.setOffset((dy<0)?0:dy, dxz);

      if(++dy >= destheight)
      {
        dy--;
      }

      dpix1.setOffset((dy<0)?0:dy, dxz);

      if(++dy >= destheight)
      {
        dy--;
      }

      dpix2.setOffset(dy++, dxz);
      int             dx = dxz;
      int             sx = sxz;

      GPixel pix0=src.ramp(spix0);
      GPixel pix1=src.ramp(spix1);
      GPixel pix2=src.ramp(spix2);
      GPixel pix3=src.ramp(spix3);
      while(dx < destwidth)
      {
        final int s00b = pix0.getBlue();
        final int s00g = pix0.getGreen();
        final int s00r = pix0.getRed();
        final int s01b = pix1.getBlue();
        final int s01g = pix1.getGreen();
        final int s01r = pix1.getRed();
        final int s02b = pix2.getBlue();
        final int s02g = pix2.getGreen();
        final int s02r = pix2.getRed();
        final int s03b = pix3.getBlue();
        final int s03g = pix3.getGreen();
        final int s03r = pix3.getRed();

        if(++sx < srcwidth)
        {
          spix0.incOffset();
          spix1.incOffset();
          spix2.incOffset();
          spix3.incOffset();
          pix0=src.ramp(spix0);
          pix1=src.ramp(spix1);
          pix2=src.ramp(spix2);
          pix3=src.ramp(spix3);
        }

        final int s10b = pix0.getBlue();
        final int s10g = pix0.getGreen();
        final int s10r = pix0.getRed();
        final int s11b = pix1.getBlue();
        final int s11g = pix1.getGreen();
        final int s11r = pix1.getRed();
        final int s12b = pix2.getBlue();
        final int s12g = pix2.getGreen();
        final int s12r = pix2.getRed();
        final int s13b = pix3.getBlue();
        final int s13g = pix3.getGreen();
        final int s13r = pix3.getRed();

        if(++sx < srcwidth)
        {
          spix0.incOffset();
          spix1.incOffset();
          spix2.incOffset();
          spix3.incOffset();
          pix0=src.ramp(spix0);
          pix1=src.ramp(spix1);
          pix2=src.ramp(spix2);
          pix3=src.ramp(spix3);
        }

        final int s20b = pix0.getBlue();
        final int s20g = pix0.getGreen();
        final int s20r = pix0.getRed();
        final int s21b = pix1.getBlue();
        final int s21g = pix1.getGreen();
        final int s21r = pix1.getRed();
        final int s22b = pix2.getBlue();
        final int s22g = pix2.getGreen();
        final int s22r = pix2.getRed();
        final int s23b = pix3.getBlue();
        final int s23g = pix3.getGreen();
        final int s23r = pix3.getRed();

        if(++sx < srcwidth)
        {
          spix0.incOffset();
          spix1.incOffset();
          spix2.incOffset();
          spix3.incOffset();
          pix0=src.ramp(spix0);
          pix1=src.ramp(spix1);
          pix2=src.ramp(spix2);
          pix3=src.ramp(spix3);
        }

        final int s30b = pix0.getBlue();
        final int s30g = pix0.getGreen();
        final int s30r = pix0.getRed();
        final int s31b = pix1.getBlue();
        final int s31g = pix1.getGreen();
        final int s31r = pix1.getRed();
        final int s32b = pix2.getBlue();
        final int s32g = pix2.getGreen();
        final int s32r = pix2.getRed();
        final int s33b = pix3.getBlue();
        final int s33g = pix3.getGreen();
        final int s33r = pix3.getRed();

        if(++sx < srcwidth)
        {
          spix0.incOffset();
          spix1.incOffset();
          spix2.incOffset();
          spix3.incOffset();
          pix0=src.ramp(spix0);
          pix1=src.ramp(spix1);
          pix2=src.ramp(spix2);
          pix3=src.ramp(spix3);
        }

        dpix0.setBlue(((11*s00b)+(2*(s01b + s10b))+s11b+8) >> 4);
        dpix0.setGreen(((11*s00g)+(2*(s01g + s10g))+s11g+8) >> 4);
        dpix0.setRed(((11*s00r)+(2*(s01r + s10r))+s11r+8) >> 4);
        dpix1.setBlue(((7*(s01b+s02b))+s11b+s12b+8) >> 4);
        dpix1.setGreen(((7*(s01g+s02g))+s11g+s12g+8) >> 4);
        dpix1.setRed(((7*(s01r+s02r))+s11r+s12r+8) >> 4);
        dpix2.setBlue(((11*s03b)+(2*(s02b+s13b))+s12b+8) >> 4);
        dpix2.setGreen(((11*s03g)+(2*(s02g+s13g))+s12g+8) >> 4);
        dpix2.setRed(((11*s03r)+(2*(s02r+s13r))+s12r+8) >> 4);

        if(++dx < destwidth)
        {
          dpix0.incOffset();
          dpix1.incOffset();
          dpix2.incOffset();
        }

        dpix0.setBlue(((7*(s10b + s20b)) + s11b + s21b + 8) >> 4);
        dpix0.setGreen(((7*(s10g + s20g)) + s11g + s21g + 8) >> 4);
        dpix0.setRed(((7*(s10r + s20r)) + s11r + s21r + 8) >> 4);
        dpix1.setBlue((s12b + s22b + s11b + s21b + 2) >> 2);
        dpix1.setGreen((s12g + s22g + s11g + s21g + 2) >> 2);
        dpix1.setRed((s12r + s22r + s11r + s21r + 2) >> 2);
        dpix2.setBlue(((7 * (s13b + s23b)) + s12b + s22b + 8) >> 4);
        dpix2.setGreen(((7 * (s13g + s23g)) + s12g + s22g + 8) >> 4);
        dpix2.setRed(((7 * (s13r + s23r)) + s12r + s22r + 8) >> 4);

        if(++dx < destwidth)
        {
          dpix0.incOffset();
          dpix1.incOffset();
          dpix2.incOffset();
        }

        dpix0.setBlue(((11 * s30b) + (2 * (s31b + s20b)) + s21b + 8) >> 4);
        dpix0.setGreen(((11 * s30g) + (2 * (s31g + s20g)) + s21g + 8) >> 4);
        dpix0.setRed(((11 * s30r) + (2 * (s31r + s20r)) + s21r + 8) >> 4);
        dpix1.setBlue(((7 * (s31b + s32b)) + s21b + s22b + 8) >> 4);
        dpix1.setGreen(((7 * (s31g + s32g)) + s21g + s22g + 8) >> 4);
        dpix1.setRed(((7 * (s31r + s32r)) + s21r + s22r + 8) >> 4);
        dpix2.setBlue(((11 * s33b) + (2 * (s32b + s23b)) + s22b + 8) >> 4);
        dpix2.setGreen(((11 * s33g) + (2 * (s32g + s23g)) + s22g + 8) >> 4);
        dpix2.setRed(((11 * s33r) + (2 * (s32r + s23r)) + s22r + 8) >> 4);

        if(++dx < destwidth)
        {
          dpix0.incOffset();
          dpix1.incOffset();
          dpix2.incOffset();
        }
      }
    }
  }

  /**
   * Insert the reference map at the specified location.
   *
   * @param ref map to insert
   * @param dx horizontal position to insert at
   * @param dy vertical position to insert at
   */
  public void fill(
    final GMap ref,
    final int  dx,
    final int  dy)
  {
    final int x0 = (dx > 0)
      ? dx
      : 0;
    int       y0 = (dy > 0)
      ? dy
      : 0;
    final int x1 = (dx < 0)
      ? (-dx)
      : 0;
    int y1 = (dy < 0)
      ? (-dy)
      : 0;
    final int w0 = columns() - x0;
    final int w1 = ref.columns() - x1;
    final int w  = (w0 < w1)
      ? w0
      : w1;
    final int h0 = rows() - y0;
    final int h1 = ref.rows() - y1;
    int       h  = (h0 < h1)
      ? h0
      : h1;

    if((w > 0) && (h > 0))
    {
      final GPixelReference pixel    = createGPixelReference(0);
      final GPixelReference refPixel = ref.createGPixelReference(0);

      do
      {
        pixel.setOffset(y0++, x0);
        refPixel.setOffset(y1++, x1);


        if(! isRampNeeded())
        {
          pixel.setPixels(refPixel,w);
        }
        else
        {
          int i = w;
          do
          {
            pixel.set(ref.ramp(refPixel));
            pixel.incOffset();
            refPixel.incOffset();
          }
          while(--i > 0);
        }
      }
      while(--h > 0);
    }
  }

  /**
   * Initiallize this pixmap with a preallocated buffer.
   *
   * @param data buffer to use
   * @param arows number of rows
   * @param acolumns number of columns
   *
   * @return the initialized pixmap
   */
  GPixmap init(
    byte[] data,
    int    arows,
    int    acolumns)
  {
    nrows       = arows;
    ncolumns    = acolumns;
    this.data   = data;

    return this;
  }

  /**
   * Initialize this pixmap to the specified size and fill in the specified color.
   *
   * @param arows number of rows
   * @param acolumns number of columns
   * @param filler fill color
   *
   * @return the initialized pixmap
   */
  public GPixmap init(
    int    arows,
    int    acolumns,
    GPixel filler)
  {
//    boolean needFill=false;
    if((arows != nrows) || (acolumns != ncolumns))
    {
      data   = null;
      nrows       = arows;
      ncolumns    = acolumns;
    }

    final int npix = rowOffset(rows());

    if(npix > 0)
    {
      if(data == null)
      {
        data = new byte[npix * 3];
//        needFill=true;
      }

      if(filler != null)
      {
        final byte b = filler.blueByte();
        final byte g = filler.greenByte();
        final byte r = filler.redByte();

        for(int i = 0; i < data.length;)
        {
          data[i++]   = b;
          data[i++]   = g;
          data[i++]   = r;
        }
      }
//      else if(needFill)
//      {
//        for(int i = 0; i < data.length;)
//        {
//          data[i++] = -51;
//        }
//      }
    }

    return this;
  }

  /**
   * Initialize this pixmap from a segment of another image map.
   *
   * @param ref image map to initialize from
   * @param rect bounding rectangle to initialize from
   *
   * @return the initialized pixmap
   */
  public GPixmap init(
    final GMap  ref,
    final GRect rect)
  {
    init(
      rect.height(),
      rect.width(),
      ((GPixel)(null)));

    final GRect rect2 = new GRect(0, 0,
        ref.columns(),
        ref.rows());
    rect2.intersect(rect2, rect);
    rect2.translate(-rect.xmin, -rect.ymin);

    if(!rect2.isEmpty())
    {
      final GPixelReference pixel = createGPixelReference(0);
      final GPixelReference refPixel =ref.createGPixelReference(0);

      for(int y = rect2.ymin; y < rect2.ymax; y++)
      {
        pixel.setOffset(y, rect2.xmin);
        refPixel.setOffset(y + rect.ymin, rect.xmin + rect2.xmin);

        if(! isRampNeeded())
        {
          for(
            int x = rect2.xmax - rect2.xmin;
            x-- > 0;
            pixel.incOffset(), refPixel.incOffset())
          {
            pixel.set(refPixel);
          }
        }
        else
        {
          for(
            int x = rect2.xmax - rect2.xmin;
            x-- > 0;
            pixel.incOffset(), refPixel.incOffset())
          {
            pixel.set(ref.ramp(refPixel));
          }
        }
      }
    }

    return this;
  }

  /**
   * Initialize this pixmap from another image map.
   *
   * @param ref image map to initialize from
   *
   * @return the initialized pixmap
   */
  public GPixmap init(final GMap ref)
  {
    init(
      ref.rows(),
      ref.columns(),
      ((GPixel)(null)));

    final GPixelReference pixel = createGPixelReference(0);

    if((rows() > 0) && (columns() > 0))
    {
      final GPixelReference refPixel =
        ((GPixmap)ref).createGPixelReference(0);

      for(int y = 0; y < rows(); y++)
      {
        pixel.setOffset(y, 0);
        refPixel.setOffset(y, 0);

        if(! isRampNeeded())
        {
          for(int x = columns(); x-- > 0;
            pixel.incOffset(), refPixel.incOffset())
          {
            pixel.set(refPixel);
          }
        }
        else
        {
          for(int x = columns(); x-- > 0;
            pixel.incOffset(), refPixel.incOffset())
          {
            pixel.set(ref.ramp(refPixel));
          }
        }
      }
    }

    return this;
  }


  /**
   * Draw the foreground layer onto this background image.
   * 
   * @param mask the mask layer
   * @param foregroundMap the foreground colors
   * @param supersample rate to upsample the foreground colors
   * @param subsample rate to subsample the foreground colors
   * @param bounds the target rectangle
   * @param gamma color correction factor
   * 
   * @throws IllegalArgumentException if the specified bounds are not contained in the page
   */
  public void stencil(
    final GBitmap mask,
    final GPixmap foregroundMap,
    final int     supersample,
    final int     subsample,
    final GRect   bounds,
    final double  gamma)
  {
    // Check arguments
    GRect rect = new GRect(0, 0, (foregroundMap.columns() * supersample+subsample-1)/subsample, (foregroundMap.rows() * supersample+subsample-1)/subsample);

    if(bounds != null)
    {
      if(
        (bounds.xmin < rect.xmin)
        || (bounds.ymin < rect.ymin)
        || (bounds.xmax > rect.xmax)
        || (bounds.ymax > rect.ymax))
      {
        throw new IllegalArgumentException(
          "rectangle out of bounds" + "bounds=(" + bounds.xmin + "," + bounds.ymin
          + "," + bounds.xmax + "," + bounds.ymax + "),rect=(" + rect.xmin + ","
          + rect.ymin + "," + rect.xmax + "," + rect.ymax + ")");
      }

      rect = bounds;
    }

    // Compute number of rows
    int xrows = rows();

    if((int)mask.rows() < xrows)
    {
      xrows = mask.rows();
    }

    if(rect.height() < xrows)
    {
      xrows = rect.height();
    }

    // Compute number of columns
    int xcolumns = columns();

    if((int)mask.columns() < xcolumns)
    {
      xcolumns = mask.columns();
    }

    if(rect.width() < xcolumns)
    {
      xcolumns = rect.width();
    }

    // Precompute multiplier map
    int   maxgray    = mask.getGrays() - 1;
    int[] multiplier = new int[maxgray];

    for(int i = 1; i < maxgray; i++)
    {
      multiplier[i] = (0x10000 * i) / maxgray;
    }

    // Prepare color correction table
    int [] gtable=getColorCorrection(gamma);

    double ratioFg=(double)supersample/(double)subsample;
    // Compute starting point in blown up foreground pixmap
    int fgy  = (rect.ymin * subsample )/ supersample;
    double fgy1 = rect.ymin - ratioFg*fgy;

    if(fgy1 < 0)
    {
      fgy--;
      fgy1 += ratioFg;
    }

    int fgxz  = (rect.xmin*subsample)/ supersample;
    double fgx1z = rect.xmin - ratioFg*fgxz;

    if(fgx1z < 0)
    {
      fgxz--;
      fgx1z += ratioFg;
    }

    int             fg  = foregroundMap.rowOffset(fgy);
    GPixelReference fgx = foregroundMap.createGPixelReference(0);
    GPixelReference dst = createGPixelReference(0);

    // Loop over rows
    for(int y = 0; y < xrows; y++)
    {
      // Loop over columns
      fgx.setOffset(fg + fgxz);

      double fgx1 = fgx1z;
      dst.setOffset(y, 0);

      int src = mask.rowOffset(y);

      for(int x = 0; x < xcolumns; x++, dst.incOffset())
      {
        int srcpix = mask.getByteAt(src + x);

        // Perform pixel operation
        if(srcpix > 0)
        {
          if(srcpix >= maxgray)
          {
            dst.setBGR(
              gtable[fgx.getBlue()],
              gtable[fgx.getGreen()],
              gtable[fgx.getRed()]);
          }
          else
          {
            int level = multiplier[srcpix];
            dst.setBGR(
              ((dst.getBlue() * (0x10000 - level))
              + (level * (int)gtable[fgx.getBlue()])) >> 16,
              ((dst.getGreen() * (0x10000 - level))
              + (level * (int)gtable[fgx.getGreen()])) >> 16,
              ((dst.getRed() * (0x10000 - level))
              + (level * (int)gtable[fgx.getRed()])) >> 16);
          }
        }

        // Next column
        if(++fgx1 >= ratioFg)
        {
          fgx1 -= ratioFg;
          fgx.incOffset();
        }
      }

      // Next line
      if(++fgy1 >= ratioFg)
      {
        fgy1 -= ratioFg;
        fg += foregroundMap.getRowSize();
      }
    }
  }


  /**
   * Copy this image with a translated origin.
   *
   * @param dx horizontal distance to translate
   * @param dy vertical distance to translate
   * @param retval an old image to try and reuse for the return value
   *
   * @return the translated image
   */
  public GMap translate(
    final int dx,
    final int dy,
    GMap      retval)
  {
    if(
      !(retval instanceof GPixmap)
      || (retval.columns() != columns())
      || (retval.rows() != rows()))
    {
      retval = createGPixmap(this).init(
          rows(),
          columns(),
          null);
    }

    retval.fill(this, -dx, -dy);

    return retval;
  }  
}
