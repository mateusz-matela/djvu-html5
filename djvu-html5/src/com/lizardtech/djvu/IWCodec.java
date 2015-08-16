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

import java.io.IOException;


final class IWCodec
  extends DjVuObject
{
  //~ Static fields/initializers ---------------------------------------------

  static final int[] iw_quant =
  {
    0x10000, 0x20000, 0x20000, 0x40000, 0x40000, 0x40000, 0x80000, 0x80000,
    0x80000, 0x100000, 0x100000, 0x100000, 0x200000, 0x100000, 0x100000,
    0x200000
  };

  static final Bucket[] bandbuckets =
  {
    new Bucket(0, 1), new Bucket(1, 1), new Bucket(2, 1), new Bucket(3, 1),
    new Bucket(4, 4), new Bucket(8, 4), new Bucket(12, 4), new Bucket(16, 16),
    new Bucket(32, 16), new Bucket(48, 16)
  };

  static final int ZERO = 1;
  static final int ACTIVE = 2;
  static final int NEW = 4;
  static final int UNK = 8;

  //~ Instance fields --------------------------------------------------------

  private BitContext     ctxMant;
  private BitContext     ctxRoot;
  private IWMap          map;
  private byte[]         bucketstate;
  private byte[]         coeffstate;
  private BitContext[][] ctxBucket;
  private BitContext[]   ctxStart;
  private int[]          quant_hi;
  private int[]          quant_lo;
  private int            curband;
  private int            curbit;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new IWCodec object.
   */
  public IWCodec()
  {
    ctxStart = new BitContext[32];

    for(int i = 0; i < 32; i++)
    {
      ctxStart[i] = new BitContext();
    }

    ctxBucket = new BitContext[10][8];

    for(int i = 0; i < 10; i++)
    {
      for(int j = 0; j < 8; j++)
      {
        ctxBucket[i][j] = new BitContext();
      }
    }

    quant_hi      = new int[10];
    quant_lo      = new int[16];
    coeffstate    = new byte[256];
    bucketstate   = new byte[16];
    curband       = 0;
    curbit        = 1;
    ctxMant       = new BitContext();
    ctxRoot       = new BitContext();
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of IWCodec with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of IWCodec.
   */
  public static IWCodec createIWCodec(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (IWCodec)create(
      options,
      options.getIWCodecClass(),
      IWCodec.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @param zp DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  int code_slice(final ZPCodec zp)
    throws IOException
  {
    if(curbit < 0)
    {
      return 0;
    }

    if(is_null_slice(curbit, curband) == 0)
    {
      for(int blockno = 0; blockno < map.nb; blockno++)
      {
        int fbucket = bandbuckets[curband].start;
        int nbucket = bandbuckets[curband].size;
        decode_buckets(
          zp,
          curbit,
          curband,
          map.blocks[blockno],
          fbucket,
          nbucket);
      }
    }

    if(++curband >= bandbuckets.length)
    {
      curband = 0;
      curbit++;

      if(next_quant() == 0)
      {
        curbit = -1;

        return 0;
      }
    }

    return 1;
  }

  /**
   * DOCUMENT ME!
   *
   * @param zp DOCUMENT ME!
   * @param bit DOCUMENT ME!
   * @param band DOCUMENT ME!
   * @param blk DOCUMENT ME!
   * @param fbucket DOCUMENT ME!
   * @param nbucket DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  void decode_buckets(
    ZPCodec zp,
    int     bit,
    int     band,
    IWBlock blk,
    int     fbucket,
    int     nbucket)
    throws IOException
  {
    int    thres   = quant_hi[band];
    int    bbstate = 0;
    byte[] cstate  = coeffstate;
    int    cidx    = 0;

    for(int buckno = 0; buckno < nbucket;)
    {
      int     bstatetmp = 0;
      short[] pcoeff = blk.getBlock(fbucket + buckno);

      if(pcoeff == null)
      {
        bstatetmp = 8;
      }
      else
      {
        for(int i = 0; i < 16; i++)
        {
          int cstatetmp = cstate[cidx + i] & 1;

          if(cstatetmp == 0)
          {
            if(pcoeff[i] != 0)
            {
              cstatetmp |= 2;
            }
            else
            {
              cstatetmp |= 8;
            }
          }

          cstate[cidx + i] = (byte)cstatetmp;
          bstatetmp |= cstatetmp;
        }
      }

      bucketstate[buckno] = (byte)bstatetmp;
      bbstate |= bstatetmp;
      buckno++;
      cidx += 16;
    }

    if((nbucket < 16) || ((bbstate & 2) != 0))
    {
      bbstate |= 4;
    }
    else if((bbstate & 8) != 0)
    {
      if(zp.decoder(ctxRoot) != 0)
      {
        bbstate |= 4;
      }
    }

    if((bbstate & 4) != 0)
    {
      for(int buckno = 0; buckno < nbucket; buckno++)
      {
        if((bucketstate[buckno] & 8) != 0)
        {
          int ctx = 0;

          if(!DjVuOptions.NOCTX_BUCKET_UPPER && (band > 0))
          {
            int     k = (fbucket + buckno) << 2;
            short[] b = blk.getBlock(k >> 4);

            if(b != null)
            {
              k &= 0xf;

              if(b[k] != 0)
              {
                ctx++;
              }

              if(b[k + 1] != 0)
              {
                ctx++;
              }

              if(b[k + 2] != 0)
              {
                ctx++;
              }

              if((ctx < 3) && (b[k + 3] != 0))
              {
                ctx++;
              }
            }
          }

          if(!DjVuOptions.NOCTX_BUCKET_ACTIVE && ((bbstate & 2) != 0))
          {
            ctx |= 4;
          }

          if(zp.decoder(ctxBucket[band][ctx]) != 0)
          {
            bucketstate[buckno] |= 4;
          }
        }
      }
    }

    if((bbstate & 4) != 0)
    {
      cstate   = coeffstate;
      cidx     = 0;

      for(int buckno = 0; buckno < nbucket;)
      {
        if((bucketstate[buckno] & 4) != 0)
        {
          short[] pcoeff = blk.getBlock(fbucket + buckno);

          if(pcoeff == null)
          {
            pcoeff = blk.getInitializedBlock(fbucket + buckno);

            for(int i = 0; i < 16; i++)
            {
              if((cstate[cidx + i] & 1) == 0)
              {
                cstate[cidx + i] = 8;
              }
            }
          }

          int gotcha    = 0;
          int maxgotcha = 7;

          if(!DjVuOptions.NOCTX_EXPECT)
          {
            for(int i = 0; i < 16; i++)
            {
              if((cstate[cidx + i] & 8) != 0)
              {
                gotcha++;
              }
            }
          }

          for(int i = 0; i < 16; i++)
          {
            if((cstate[cidx + i] & 8) != 0)
            {
              if(band == 0)
              {
                thres = quant_lo[i];
              }

              int ctx = 0;

              if(!DjVuOptions.NOCTX_EXPECT)
              {
                if(gotcha >= maxgotcha)
                {
                  ctx = maxgotcha;
                }
                else
                {
                  ctx = gotcha;
                }
              }

              if(
                !DjVuOptions.NOCTX_ACTIVE
                && ((bucketstate[buckno] & 2) != 0))
              {
                ctx |= 8;
              }

              if(zp.decoder(ctxStart[ctx]) != 0)
              {
                cstate[cidx + i] |= 4;

                int halfthres = thres >> 1;
                int coeff = (thres + halfthres) - (halfthres >> 2);

                if(zp.IWdecoder() != 0)
                {
                  pcoeff[i] = (short)(-coeff);
                }
                else
                {
                  pcoeff[i] = (short)coeff;
                }
              }

              if(!DjVuOptions.NOCTX_EXPECT)
              {
                if((cstate[cidx + i] & 4) != 0)
                {
                  gotcha = 0;
                }
                else if(gotcha > 0)
                {
                  gotcha--;
                }
              }
            }
          }
        }

        buckno++;
        cidx += 16;
      }
    }

    if((bbstate & 2) != 0)
    {
      cstate   = coeffstate;
      cidx     = 0;

      for(int buckno = 0; buckno < nbucket;)
      {
        if((bucketstate[buckno] & 2) != 0)
        {
          short[] pcoeff = blk.getBlock(fbucket + buckno);

          for(int i = 0; i < 16; i++)
          {
            if((cstate[cidx + i] & 2) != 0)
            {
              int coeff = pcoeff[i];

              if(coeff < 0)
              {
                coeff = -coeff;
              }

              if(band == 0)
              {
                thres = quant_lo[i];
              }

              if(coeff <= (3 * thres))
              {
                coeff += (thres >> 2);

                if(zp.decoder(ctxMant) != 0)
                {
                  coeff += (thres >> 1);
                }
                else
                {
                  coeff = (coeff - thres) + (thres >> 1);
                }
              }
              else
              {
                if(zp.IWdecoder() != 0)
                {
                  coeff += (thres >> 1);
                }
                else
                {
                  coeff = (coeff - thres) + (thres >> 1);
                }
              }

              if(pcoeff[i] > 0)
              {
                pcoeff[i] = (short)coeff;
              }
              else
              {
                pcoeff[i] = (short)(-coeff);
              }
            }
          }
        }

        buckno++;
        cidx += 16;
      }
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param frac DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  float estimate_decibel(float frac)
  {
    return 0.0F;
  }

  /**
   * DOCUMENT ME!
   *
   * @param map DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  IWCodec init(final IWMap map)
  {
    this.map = map;

    int   i    = 0;
    int[] q    = iw_quant;
    int   qidx = 0;

    for(int j = 0; i < 4; j++)
    {
      quant_lo[i++] = q[qidx++];
    }

    for(int j = 0; j < 4; j++)
    {
      quant_lo[i++] = q[qidx];
    }

    qidx++;

    for(int j = 0; j < 4; j++)
    {
      quant_lo[i++] = q[qidx];
    }

    qidx++;

    for(int j = 0; j < 4; j++)
    {
      quant_lo[i++] = q[qidx];
    }

    qidx++;
    quant_hi[0] = 0;

    for(int j = 1; j < 10; j++)
    {
      quant_hi[j] = q[qidx++];
    }

    while(quant_lo[0] >= 32768)
    {
      next_quant();
    }

    return this;
  }

  /**
   * DOCUMENT ME!
   *
   * @param bit DOCUMENT ME!
   * @param band DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  int is_null_slice(
    int bit,
    int band)
  {
    if(band == 0)
    {
      int is_null = 1;

      for(int i = 0; i < 16; i++)
      {
        int threshold = quant_lo[i];
        coeffstate[i] = 1;

        if((threshold > 0) && (threshold < 32768))
        {
          is_null = coeffstate[i] = 0;
        }
      }

      return is_null;
    }

    int threshold = quant_hi[band];

    if((threshold <= 0) || (threshold >= 32768))
    {
      return 1;
    }

    for(int i = 0; i < (bandbuckets[band].size << 4); i++)
    {
      coeffstate[i] = 0;
    }

    return 0;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  int next_quant()
  {
    int flag = 0;

    for(int i = 0; i < 16; i++)
    {
      if((quant_lo[i] = quant_lo[i] >> 1) != 0)
      {
        flag = 1;
      }
    }

    for(int i = 0; i < 10; i++)
    {
      if((quant_hi[i] = quant_hi[i] >> 1) != 0)
      {
        flag = 1;
      }
    }

    return flag;
  }

  //~ Inner Classes ----------------------------------------------------------

  /**
   * DOCUMENT ME!
   *
   * @author $author$
   * @version $Revision: 1.4 $
   */
  static class Bucket
  {
    //~ Static fields/initializers -------------------------------------------

    /** DOCUMENT ME! */
    protected static int sz = 8;

    //~ Instance fields ------------------------------------------------------

    /** DOCUMENT ME! */
    protected final int size;

    /** DOCUMENT ME! */
    protected final int start;

    //~ Constructors ---------------------------------------------------------

    /**
     * Creates a new Bucket object.
     *
     * @param start DOCUMENT ME!
     * @param size DOCUMENT ME!
     */
    public Bucket(
      int start,
      int size)
    {
      this.start   = start;
      this.size    = size;
    }
  }
}
