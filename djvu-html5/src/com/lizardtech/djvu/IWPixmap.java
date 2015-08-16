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
 *  This class represents structured wavelette data.
 */
public class IWPixmap
  extends DjVuObject
  implements Codec
{
  //~ Static fields/initializers ---------------------------------------------

  /** DOCUMENT ME! */
  public static final int CRCBfull = 5;

  /** DOCUMENT ME! */
  public static final int CRCBhalf = 3;

  /** DOCUMENT ME! */
  public static final int CRCBMode = 1;

  /** DOCUMENT ME! */
  public static final int CRCBnone = 2;

  /** DOCUMENT ME! */
  public static final int CRCBnormal = 4;

  /** DOCUMENT ME! */
  public static final float[][] rgb_to_ycc =
  {
    {0.304348F, 0.608696F, 0.086956F},
    {0.463768F, -0.405797F, -0.057971F},
    {-0.173913F, -0.347826F, 0.521739F}
  };

  //~ Instance fields --------------------------------------------------------

  /** DOCUMENT ME! */
  protected IWCodec cbcodec = null;

  /** DOCUMENT ME! */
  protected IWCodec crcodec = null;

  /** DOCUMENT ME! */
  protected IWCodec ycodec = null;

  /** DOCUMENT ME! */
  protected IWMap cbmap = null;

  /** DOCUMENT ME! */
  protected IWMap crmap = null;

  /** DOCUMENT ME! */
  protected IWMap ymap = null;

  /** DOCUMENT ME! */
  protected int cbytes = 0;

  /** DOCUMENT ME! */
  protected int crcb_delay = 10;

  /** DOCUMENT ME! */
  protected boolean crcb_half = false;

  /** DOCUMENT ME! */
  protected int cserial = 0;

  /** DOCUMENT ME! */
  protected int cslice = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new IWPixmap object.
   */
  public IWPixmap() {}

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
   * Creates an instance of IWPixmap with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of IWPixmap.
   */
  public static IWPixmap createIWPixmap(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (IWPixmap)create(
      options,
      options.getIWPixmapClass(),
      IWPixmap.class);
  }

  /**
   * DOCUMENT ME!
   */
  public void close_codec()
  {
    ycodec   = crcodec = cbcodec = null;
    cslice   = cbytes = cserial = 0;

    if(DjVuOptions.COLLECT_GARBAGE)
    {
      System.gc();
    }
  }

  /**
   * Decode this chunk.
   *
   * @param bs DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public void decode(final CachedInputStream bs)
    throws IOException
  {
    if(ycodec == null)
    {
      cslice   = cserial = 0;
      ymap     = null;
    }

    if(bs.read() != cserial)
    {
      throw new IOException(
        "(IWPixmap::decode) Chunk does not bear expected serial number");
    }

    int nslices = cslice + bs.read();

    if(cserial == 0)
    {
      int major = bs.read();
      int minor = bs.read();

      if((major & 0x7f) != 1)
      {
        throw new IOException(
          "(IWPixmap::decode) File has been compressed with an incompatible IWCodec");
      }

      if(minor > 2)
      {
        throw new IOException(
          "(IWPixmap::decode) File has been compressed with a more recent IWCodec");
      }

      int header3size = 5;

      if(minor < 2)
      {
        header3size = 4;
      }

      int w = (bs.read() << 8);
      w |= bs.read();

      int h = (bs.read() << 8);
      h |= bs.read();
      crcb_delay   = 0;
      crcb_half    = false;

      int b        = bs.read();

      if(minor >= 2)
      {
        crcb_delay = 0x7f & b;
      }

      if(minor >= 2)
      {
        crcb_half = ((0x80 & b) == 0);
      }

      if((major & 0x80) != 0)
      {
        crcb_delay = -1;
      }

      ymap     = IWMap.createIWMap(this).init(w, h);
      ycodec   = IWCodec.createIWCodec(this).init(ymap);

      if(crcb_delay >= 0)
      {
        cbmap     = IWMap.createIWMap(this).init(w, h);
        crmap     = IWMap.createIWMap(this).init(w, h);
        cbcodec   = IWCodec.createIWCodec(this).init(cbmap);
        crcodec   = IWCodec.createIWCodec(this).init(crmap);
      }
    }

    ZPCodec zp = ZPCodec.createZPCodec(this).init(bs);

    for(int flag = 1; (flag != 0) && (cslice < nslices); cslice++)
    {
      flag = ycodec.code_slice(zp);

      if((crcodec != null) && (cbcodec != null) && (crcb_delay <= cslice))
      {
        flag |= cbcodec.code_slice(zp);
        flag |= crcodec.code_slice(zp);
      }
    }

    cserial++;

//    return nslices;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public int getHeight()
  {
    return (ymap != null)
    ? ymap.ih
    : 0;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public GPixmap getPixmap()
  {
    if(ymap == null)
    {
      return null;
    }

    final int w      = ymap.iw;
    final int h      = ymap.ih;
    final int pixsep = 3;
    final int rowsep = w * pixsep;
    byte[]    bytes  = new byte[h * rowsep];

    ymap.image(0, bytes, rowsep, pixsep, false);

    if((crmap != null) && (cbmap != null) && (crcb_delay >= 0))
    {
      cbmap.image(1, bytes, rowsep, pixsep, crcb_half);
      crmap.image(2, bytes, rowsep, pixsep, crcb_half);
    }

    // Convert image to RGB
    final GPixmap         ppm   =
      GPixmap.createGPixmap(this).init(bytes, h, w);
    final GPixelReference pixel = ppm.createGPixelReference(0);

    for(int i = 0; i < h;)
    {
      pixel.setOffset(i++, 0);

      if((crmap != null) && (cbmap != null) && (crcb_delay >= 0))
      {
        pixel.YCC_to_RGB(w);
      }
      else
      {
        for(int x = w; x-- > 0; pixel.incOffset())
        {
          pixel.setGray(127 - pixel.getBlue());
        }
      }
    }

    return ppm;
  }

  /**
   * Create a pixmap with the specified subsample rate and bounds.
   *
   * @param subsample rate at which to subsample
   * @param rect Bounding box of the desired pixmap.
   * @param retval An old pixmap to try updating, or null.
   *
   * @return DOCUMENT ME!
   */
  public GPixmap getPixmap(
    int     subsample,
    GRect   rect,
    GPixmap retval)
  {
    if(ymap == null)
    {
      return null;
    }

    if(retval == null)
    {
      retval = GPixmap.createGPixmap(this);
    }

    final int    w      = rect.width();
    final int    h      = rect.height();
    final int    pixsep = 3;
    final int    rowsep = w * pixsep;
    final byte[] bytes  = retval.init(h, w, null).data;

    ymap.image(subsample, rect, 0, bytes, rowsep, pixsep, false);

    if((crmap != null) && (cbmap != null) && (crcb_delay >= 0))
    {
      cbmap.image(subsample, rect, 1, bytes, rowsep, pixsep, crcb_half);
      crmap.image(subsample, rect, 2, bytes, rowsep, pixsep, crcb_half);
    }

    final GPixelReference pixel = retval.createGPixelReference(0);

    for(int i = 0; i < h;)
    {
      pixel.setOffset(i++, 0);

      if((crmap != null) && (cbmap != null) && (crcb_delay >= 0))
      {
        pixel.YCC_to_RGB(w);
      }
      else
      {
        for(int x = w; x-- > 0; pixel.incOffset())
        {
          pixel.setGray(127 - pixel.blueByte());
        }
      }
    }

    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public int getWidth()
  {
    return (ymap != null)?ymap.iw:0;
  }

  /**
   * Set the CRCB Delay
   *
   * @param value the new CRCB delay value.
   *
   * @return the CRCB delay value
   */
  public int setCrcbDelay(int value)
  {
    if(value >= 0)
    {
      crcb_delay = value;
    }

    return crcb_delay;
  }
}
