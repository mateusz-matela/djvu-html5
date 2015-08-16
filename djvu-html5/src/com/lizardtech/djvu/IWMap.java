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
 *  This class represents structured wavelette data.
 */
final class IWMap
  extends DjVuObject
  implements Cloneable
{
  //~ Instance fields --------------------------------------------------------

  /** DOCUMENT ME! */
  protected IWBlock[] blocks;

  /** DOCUMENT ME! */
  protected int bh;

  /** DOCUMENT ME! */
  protected int bw;

  /** DOCUMENT ME! */
  protected int ih;

  /** DOCUMENT ME! */
  protected int iw;

  /** DOCUMENT ME! */
  protected int nb;

  /** DOCUMENT ME! */
  protected int top;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new IWMap object.
   */
  public IWMap() {}

  /**
   * Creates a new IWMap object.
   *
   * @param w DOCUMENT ME!
   * @param h DOCUMENT ME!
   */
  public IWMap(
    final int w,
    final int h)
  {
    init(w, h);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * DOCUMENT ME!
   *
   * @param p DOCUMENT ME!
   * @param pidx DOCUMENT ME!
   * @param w DOCUMENT ME!
   * @param h DOCUMENT ME!
   * @param rowsize DOCUMENT ME!
   * @param begin DOCUMENT ME!
   * @param end DOCUMENT ME!
   */
  static void backward(
    short[] p,
    int     pidx,
    int     w,
    int     h,
    int     rowsize,
    int     begin,
    int     end)
  {
    for(int scale = begin >> 1; scale >= end; scale >>= 1)
    {
      for(int j = 0; j < w; j += scale)
      {
        backward_filter(p, pidx, j, j + (h * rowsize), j, scale * rowsize);
      }

      for(int i = 0; i < h; i += scale)
      {
        backward_filter(
          p,
          pidx,
          i * rowsize,
          (i * rowsize) + w,
          i * rowsize,
          scale);
      }
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param p DOCUMENT ME!
   * @param pidx DOCUMENT ME!
   * @param b DOCUMENT ME!
   * @param e DOCUMENT ME!
   * @param z DOCUMENT ME!
   * @param s DOCUMENT ME!
   */
  static void backward_filter(
    short[] p,
    int     pidx,
    int     b,
    int     e,
    int     z,
    int     s)
  {
    final int s3 = 3 * s;

    if((z < b) || (z > e))
    {
      logError(
        "(_IWCoeff::backward_filter) Out of bounds [b<=z<=e]");
    }

    int n  = z;
    int bb;
    int cc;
    int aa = bb = cc = 0;
    int dd = ((n + s) >= e)
      ? 0
      : ((int)(p[pidx + n + s]));

    for(; (n + s3) < e; n = (n + s3) - s)
    {
      aa   = bb;
      bb   = cc;
      cc   = dd;
      dd   = p[pidx + n + s3];
      p[pidx + n] -= (((9 * (bb + cc)) - (aa + dd)) + 16) >> 5;
    }

    for(; n < e; n = n + s + s)
    {
      aa   = bb;
      bb   = cc;
      cc   = dd;
      dd   = 0;
      p[pidx + n] -= (((9 * (bb + cc)) - (aa + dd)) + 16) >> 5;
    }

    n    = z + s;
    aa   = 0;
    bb   = p[(pidx + n) - s];
    cc   = ((n + s) >= e)
      ? 0
      : ((int)(p[pidx + n + s]));
    dd = ((n + s3) >= e)
      ? 0
      : ((int)(p[pidx + n + s3]));

    if(n < e)
    {
      int x = bb;

      if((n + s) < e)
      {
        x = (bb + cc + 1) >> 1;
      }

      p[pidx + n] += x;
      n = n + s + s;
    }

    for(; (n + s3) < e; n = (n + s3) - s)
    {
      aa   = bb;
      bb   = cc;
      cc   = dd;
      dd   = p[pidx + n + s3];

      int x = (((9 * (bb + cc)) - (aa + dd)) + 8) >> 4;
      p[pidx + n] += x;
    }

    if((n + s) < e)
    {
      aa   = bb;
      bb   = cc;
      cc   = dd;
      dd   = 0;

      int x = (bb + cc + 1) >> 1;
      p[pidx + n] += x;
      n = n + s + s;
    }

    if(n < e)
    {
      aa   = bb;
      bb   = cc;
      cc   = dd;
      dd   = 0;

      int x = bb;
      p[pidx + n] += x;
    }
  }

  /**
   * Creates an instance of IWMap with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of IWMap.
   */
  public static IWMap createIWMap(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (IWMap)create(
      options,
      options.getIWMapClass(),
      IWMap.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public Object clone()
  {
    //verbose("1. IWMap clone");
    Cloneable retval = null;

    try
    {
      retval = (IWMap)super.clone();

      final IWBlock[] blocks = (IWBlock[])this.blocks.clone();
      ((IWMap)retval).blocks = blocks;

      for(int i = 0; i < nb; i++)
      {
        blocks[i] = (IWBlock)blocks[i].clone();
      }
    }
    catch(final CloneNotSupportedException ignored) {}

    //verbose("2. IWMap clone");
    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  int get_bucket_count()
  {
    int buckets = 0;

    for(int blockno = 0; blockno < nb; blockno++)
    {
      for(int buckno = 0; buckno < 64; buckno++)
      {
        if(blocks[blockno].getBlock(buckno) != null)
        {
          buckets++;
        }
      }
    }

    return buckets;
  }

  /**
   * DOCUMENT ME!
   *
   * @param index DOCUMENT ME!
   * @param img8 DOCUMENT ME!
   * @param rowsize DOCUMENT ME!
   * @param pixsep DOCUMENT ME!
   * @param fast DOCUMENT ME!
   */
  void image(
    int          index,
    final byte[] img8,
    int          rowsize,
    int          pixsep,
    boolean          fast)
  {
    final short[] data16    = new short[bw * bh];
    final short[] liftblock = new short[1024];
    int           pidx      = 0;
    IWBlock[]     block     = blocks;
    int           blockidx  = 0;
    int           ppidx     = 0;

    for(int i = 0; i < bh; i += 32, pidx += (32 * bw))
    {
      for(int j = 0; j < bw; j += 32)
      {
        block[blockidx].write_liftblock(liftblock, 0, 64);
        blockidx++;

        ppidx = pidx + j;

        for(int ii = 0, p1idx = 0; ii++ < 32; p1idx += 32, ppidx += bw)
        {
          System.arraycopy(liftblock, p1idx, data16, ppidx, 32);
        }
      }
    }

    if(fast)
    {
      backward(data16, 0, iw, ih, bw, 32, 2);
      pidx = 0;

      for(int i = 0; i < bh; i += 2, pidx += bw)
      {
        for(int jj = 0; jj < bw; jj += 2, pidx += 2)
        {
          data16[pidx + bw] =
            data16[pidx + bw + 1] = data16[pidx + 1] = data16[pidx];
        }
      }
    }
    else
    {
      backward(data16, 0, iw, ih, bw, 32, 1);
    }

    pidx = 0;

    for(int i = 0, rowidx = index; i++ < ih; rowidx += rowsize, pidx += bw)
    {
      for(int j = 0, pixidx = rowidx; j < iw; pixidx += pixsep)
      {
        int x = (data16[pidx + (j++)] + 32) >> 6;

        if(x < -128)
        {
          x = -128;
        }
        else if(x > 127)
        {
          x = 127;
        }

        img8[pixidx] = (byte)x;
      }
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param subsample DOCUMENT ME!
   * @param rect DOCUMENT ME!
   * @param index DOCUMENT ME!
   * @param img8 DOCUMENT ME!
   * @param rowsize DOCUMENT ME!
   * @param pixsep DOCUMENT ME!
   * @param fast DOCUMENT ME!
   *
   * @throws IllegalArgumentException DOCUMENT ME!
   */
  void image(
    int          subsample,
    GRect        rect,
    int          index,
    final byte[] img8,
    int          rowsize,
    int          pixsep,
    boolean          fast)
  {
    int nlevel = 0;

    while((nlevel < 5) && ((32 >> nlevel) > subsample))
    {
      nlevel++;
    }

    final int boxsize = 1 << nlevel;

    if(subsample != (32 >> nlevel))
    {
      throw new IllegalArgumentException(
        "(IWMap::image) Unsupported subsampling factor");
    }

    if(rect.isEmpty())
    {
      throw new IllegalArgumentException("(IWMap::image) GRect is empty");
    }

    GRect irect =
      new GRect(
        0,
        0,
        ((iw + subsample) - 1) / subsample,
        ((ih + subsample) - 1) / subsample);

    if(
      (rect.xmin < 0)
      || (rect.ymin < 0)
      || (rect.xmax > irect.xmax)
      || (rect.ymax > irect.ymax))
    {
      throw new IllegalArgumentException(
        "(IWMap::image) GRect is out of bounds: " + rect.xmin + ","
        + rect.ymin + "," + rect.xmax + "," + rect.ymax + "," + irect.xmax
        + "," + irect.ymax);
    }

    GRect[] needed = new GRect[8];
    GRect[] recomp = new GRect[8];

    for(int i = 0; i < 8;)
    {
      needed[i]     = new GRect();
      recomp[i++]   = new GRect();
    }

    int r = 1;
    needed[nlevel]   = (GRect)rect.clone();
    recomp[nlevel]   = (GRect)rect.clone();

    for(int i = nlevel - 1; i >= 0; i--)
    {
      needed[i] = recomp[i + 1];
      needed[i].inflate(3 * r, 3 * r);
      needed[i].intersect(needed[i], irect);
      r += r;
      recomp[i].xmin   = ((needed[i].xmin + r) - 1) & ~(r - 1);
      recomp[i].xmax   = needed[i].xmax & ~(r - 1);
      recomp[i].ymin   = ((needed[i].ymin + r) - 1) & ~(r - 1);
      recomp[i].ymax   = needed[i].ymax & ~(r - 1);
    }

    GRect work = new GRect();
    work.xmin   = needed[0].xmin & ~(boxsize - 1);
    work.ymin   = needed[0].ymin & ~(boxsize - 1);
    work.xmax   = ((needed[0].xmax - 1) & ~(boxsize - 1)) + boxsize;
    work.ymax   = ((needed[0].ymax - 1) & ~(boxsize - 1)) + boxsize;

    final int     dataw = work.width();
    final short[] data   = new short[dataw * work.height()];
    int           blkw   = bw >> 5;
    int           lblock =
      ((work.ymin >> nlevel) * blkw) + (work.xmin >> nlevel);

    final short[] liftblock = new short[1024];

    for(
      int by = work.ymin, ldata = 0;
      by < work.ymax;
      by += boxsize, ldata += (dataw << nlevel), lblock += blkw)
    {
      for(
        int bx = work.xmin, bidx = lblock, rdata = ldata;
        bx < work.xmax;
        bx += boxsize, bidx++, rdata += boxsize)
      {
        IWBlock block  = blocks[bidx];
        int     mlevel = nlevel;

        if(
          (nlevel > 2)
          && (((bx + 31) < needed[2].xmin) || (bx > needed[2].xmax)
          || ((by + 31) < needed[2].ymin) || (by > needed[2].ymax)))
        {
          mlevel = 2;
        }

        final int bmax   = ((1 << (mlevel + mlevel)) + 15) >> 4;
        final int ppinc  = 1 << (nlevel - mlevel);
        final int ppmod1 = dataw << (nlevel - mlevel);
        final int ttmod0 = 32 >> mlevel;
        final int ttmod1 = ttmod0 << 5;
        block.write_liftblock(liftblock, 0, bmax);

        for(
          int ii = 0, tt = 0, pp = rdata;
          ii < boxsize;
          ii += ppinc, pp += ppmod1, tt += (ttmod1 - 32))
        {
          for(int jj = 0; jj < boxsize; jj += ppinc, tt += ttmod0)
          {
            data[pp + jj] = liftblock[tt];
          }
        }
      }
    }

    r = boxsize;

    for(int i = 0; i < nlevel; i++)
    {
      GRect comp = needed[i];
      comp.xmin   = comp.xmin & ~(r - 1);
      comp.ymin   = comp.ymin & ~(r - 1);
      comp.translate(-work.xmin, -work.ymin);

      if(fast&& (i >= 4))
      {
        for(
          int ii = comp.ymin, pp = (comp.ymin * dataw);
          ii < comp.ymax;
          ii += 2, pp += (dataw + dataw))
        {
          for(int jj = comp.xmin; jj < comp.xmax; jj += 2)
          {
            data[pp + jj + dataw] =
              data[pp + jj + dataw + 1] = data[pp + jj + 1] = data[pp + jj];
          }
        }

        break;
      }

      backward(
        data,
        (comp.ymin * dataw) + comp.xmin,
        comp.width(),
        comp.height(),
        dataw,
        r,
        r >> 1);
      r >>= 1;
    }

    GRect nrect = (GRect)rect.clone();
    nrect.translate(-work.xmin, -work.ymin);

    for(
      int i = nrect.ymin, pidx = (nrect.ymin * dataw), ridx = index;
      i++ < nrect.ymax;
      ridx += rowsize, pidx += dataw)
    {
      for(
        int j = nrect.xmin, pixidx = ridx;
        j < nrect.xmax;
        j++, pixidx += pixsep)
      {
        int x = (data[pidx + j] + 32) >> 6;

        if(x < -128)
        {
          x = -128;
        }
        else if(x > 127)
        {
          x = 127;
        }

        img8[pixidx] = (byte)x;
      }
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param w DOCUMENT ME!
   * @param h DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  IWMap init(
    final int w,
    final int h)
  {
    iw       = w;
    ih       = h;
    bw       = ((w + 32) - 1) & 0xffffffe0;
    bh       = ((h + 32) - 1) & 0xffffffe0;
    nb       = (bw * bh) / 1024;
    blocks   = new IWBlock[nb];

    for(int i = 0; i < nb; i++)
    {
      blocks[i] = IWBlock.createIWBlock(this);
    }

    return this;
  }

  /**
   * DOCUMENT ME!
   *
   * @param res DOCUMENT ME!
   */
  void slashres(int res)
  {
    int minbucket = 1;

    if(res < 2)
    {
      return;
    }

    if(res < 4)
    {
      minbucket = 16;
    }
    else if(res < 8)
    {
      minbucket = 4;
    }

    for(int blockno = 0; blockno < nb; blockno++)
    {
      for(int buckno = minbucket; buckno < 64; buckno++)
      {
        blocks[blockno].clearBlock(buckno);
      }
    }
  }
}
