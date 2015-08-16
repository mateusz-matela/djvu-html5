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
 * This class decodes a bzz encoded InputStream.
 *
 * @author $author$
 * @version $Revision: 1.3 $
 */
public final class BSInputStream
  extends InputStream
  implements DjVuInterface
{
  //~ Static fields/initializers ---------------------------------------------

  private static final int MINBLOCK = 10;
  private static final int MAXBLOCK = 4096;

  // Sorting tresholds

  private static final int FREQMAX = 4;
  private static final int CTXIDS = 3;
  private static final byte[] MTF = new byte[256];

  static
  {
      for(int i=0;i<MTF.length;i++)
      {
        MTF[i]=(byte)i;
      }
  }
  
  //~ Instance fields --------------------------------------------------------

  /** The decoder. */
  private ZPCodec zp=null;

  /** Bits being decoded. */
  private final BitContext[] ctx = new BitContext[300];

  /** Decoded data. */
  private byte[] data=null;

  /** True if an EOF has been read. */
  private boolean eof=false;

  /** The data blocksize. */
  private int blocksize=0;

  /** Offset in the data. */
  private int bptr=0;

  /** The size of the data read. */
  private int size=0;

  // Object for holding the DjVuOptions
  private final DjVuObject djvuObject = new DjVuObject();

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new BSInputStream object.
   */
  public BSInputStream() {}

  /**
   * Creates a new BSInputStream object.
   *
   * @param input steam to decode
   *
   * @throws IOException if an error occures
   */
  public BSInputStream(final InputStream input)
    throws IOException
  {
    init(input);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Set the DjVuOptions used by this object.
   *
   * @param options The DjVuOptions used by this object.
   */
  public void setDjVuOptions(final DjVuOptions options)
  {
    djvuObject.setDjVuOptions(options);
  }

  /**
   * Query the DjVuOptions used by this object.
   *
   * @return the DjVuOptions used by this object.
   */
  public DjVuOptions getDjVuOptions()
  {
    return djvuObject.getDjVuOptions();
  }

  /**
   * Creates an instance of BSInputStream with the options interherited from
   * the specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of BSInputStream.
   */
  public static BSInputStream createBSInputStream(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (BSInputStream)DjVuObject.create(
      options,
      options.getBSInputStreamClass(),
      BSInputStream.class);
  }

  /**
   * Called to decode data.
   *
   * @return the number of bytes decoded
   *
   * @throws IOException if an error occurs
   */
  private int decode()
    throws IOException
  {
    /////////////////////////////////
    ////////////  Decode input stream
    size = decode_raw(24);

    if(size == 0)
    {
      return 0;
    }

    if(size > (MAXBLOCK * 1024))
    {
      throw new IOException("ByteStream.corrupt");
    }

    // Allocate
    if(blocksize < size)
    {
      blocksize   = size;
      data        = new byte[blocksize];
    }
    else if(data == null)
    {
      data = new byte[blocksize];
    }

    // Decode Estimation Speed
    int fshift = 0;

    if(zp.decoder() != 0)
    {
      fshift++;

      if(zp.decoder() != 0)
      {
        fshift++;
      }
    }

    // Prepare Quasi MTF
    byte[] mtf = (byte[])MTF.clone();

    int[]  freq = new int[FREQMAX];

    for(int i = 0; i < FREQMAX; freq[i++] = 0)
    {
      ;
    }

    int fadd = 4;

    // Decode
    int mtfno     = 3;
    int markerpos = -1;

    for(int i = 0; i < size; i++)
    {
      int ctxid = CTXIDS - 1;

      if(ctxid > mtfno)
      {
        ctxid = mtfno;
      }

      int ctxoff = 0;

      switch(0)
      {
        default :

          if(zp.decoder(ctx[ctxoff + ctxid]) != 0)
          {
            mtfno     = 0;
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += CTXIDS;

          if(zp.decoder(ctx[ctxoff + ctxid]) != 0)
          {
            mtfno     = 1;
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += CTXIDS;

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 2 + decode_binary(ctxoff + 1, 1);
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += (1 + 1);

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 4 + decode_binary(ctxoff + 1, 2);
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += (1 + 3);

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 8 + decode_binary(ctxoff + 1, 3);
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += (1 + 7);

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 16 + decode_binary(ctxoff + 1, 4);
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += (1 + 15);

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 32 + decode_binary(ctxoff + 1, 5);
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += (1 + 31);

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 64 + decode_binary(ctxoff + 1, 6);
            data[i]   = mtf[mtfno];

            break;
          }

          ctxoff += (1 + 63);

          if(zp.decoder(ctx[ctxoff + 0]) != 0)
          {
            mtfno     = 128 + decode_binary(ctxoff + 1, 7);
            data[i]   = mtf[mtfno];

            break;
          }

          mtfno = 256;
          data[i] = 0;
          markerpos = i;

          continue;
      }

      // Rotate mtf according to empirical frequencies (new!)
      // Adjust frequencies for overflow
      int k;
      fadd = fadd + (fadd >> fshift);

      if(fadd > 0x10000000)
      {
        fadd >>= 24;
        freq[0] >>= 24;
        freq[1] >>= 24;
        freq[2] >>= 24;
        freq[3] >>= 24;

        for(k = 4; k < FREQMAX; k++)
        {
          freq[k] >>= 24;
        }
      }

      // Relocate new char according to new freq
      int fc = fadd;

      if(mtfno < FREQMAX)
      {
        fc += freq[mtfno];
      }

      for(k = mtfno; k >= FREQMAX; k--)
      {
        mtf[k] = mtf[k - 1];
      }

      for(; (k > 0) && ((0xffffffffL & fc) >= (0xffffffffL & freq[k - 1]));
        k--)
      {
        mtf[k]    = mtf[k - 1];
        freq[k]   = freq[k - 1];
      }

      mtf[k]    = data[i];
      freq[k]   = fc;
    }

    /////////////////////////////////
    ////////// Reconstruct the string
    if((markerpos < 1) || (markerpos >= size))
    {
      throw new IOException("ByteStream.corrupt");
    }

    // Allocate pointers
    int[] pos = new int[size];

    for(int j = 0; j < size; pos[j++] = 0)
    {
      ;
    }

    // Prepare count buffer
    int[] count = new int[256];

    for(int i = 0; i < 256; count[i++] = 0)
    {
      ;
    }

    // Fill count buffer
    for(int i = 0; i < markerpos; i++)
    {
      byte c = (byte)data[i];
      pos[i] = (c << 24) | (count[0xff & c] & 0xffffff);
      count[0xff & c]++;
    }

    for(int i = markerpos + 1; i < size; i++)
    {
      byte c = (byte)data[i];
      pos[i] = (c << 24) | (count[0xff & c] & 0xffffff);
      count[0xff & c]++;
    }

    // Compute sorted char positions
    int last = 1;

    for(int i = 0; i < 256; i++)
    {
      int tmp = count[i];
      count[i] = last;
      last += tmp;
    }

    // Undo the sort transform
    int j = 0;
    last = size - 1;

    while(last > 0)
    {
      int  n       = pos[j];
      byte c = (byte)(pos[j] >> 24);
      data[--last]   = (byte)c;
      j              = count[0xff & c] + (n & 0xffffff);
    }

    // Free and check
    if(j != markerpos)
    {
      throw new IOException("ByteStream.corrupt");
    }

    return size;
  }

  /**
   * Clears any decoded data.
   */
  public void flush()
  {
    size = bptr = 0;
  }

  /**
   * Called to initialize the stream and set the data to be decoded.
   *
   * @param input stream to be decoded
   *
   * @return the initialized stream
   *
   * @throws IOException if an error occurs
   */
  public BSInputStream init(final InputStream input)
    throws IOException
  {
    zp = ZPCodec.createZPCodec(this).init(input);

    for(int i = 0; i < ctx.length;)
    {
      ctx[i++] = new BitContext();
    }

    return this;
  }

  /**
   * Called to read data into a buffer.
   *
   * @param buffer byte array to fill with data
   * @param offset to start adding data
   * @param sz maximum amount of data to read
   *
   * @return the number of bytes read
   *
   * @throws IOException if an error occurs
   */
  public int read(
    byte[] buffer,
    int    offset,
    int    sz)
    throws IOException
  {
    if(eof)
    {
      return 0;
    }

    // Loop
    int copied = 0;

    while((sz > 0) && !eof)
    {
      // Decode if needed
      if(size == 0)
      {
        bptr = 0;

        if(decode() == 0)
        {
          size   = 1;
          eof    = true;
        }

        size--;
      }

      // Compute remaining
      int bytes = (size > sz)
        ? sz
        : size;

      // Transfer
      if(bytes > 0)
      {
        System.arraycopy(data, bptr, buffer, offset, bytes);
        offset += bytes;
      }

      size -= bytes;
      bptr += bytes;
      sz -= bytes;
      copied += bytes;
    }

    // Return copied bytes
    return copied;
  }

  /**
   * Called to read the next byte of data.
   *
   * @return the next byte in a range 0 to 255 or -1 if an EOP has been read
   *
   * @throws IOException if an error occurs
   */
  public int read()
    throws IOException
  {
    byte[] buffer = new byte[1];

    return (read(buffer) == 1)
    ? (0xff & buffer[0])
    : (-1);
  }

  /**
   * Called to decode data bits.
   *
   * @param ctxoff where to start decoding
   * @param bits the number of bits to decode
   *
   * @return the decoded bits
   *
   * @throws IOException if an error occurs
   */
  private int decode_binary(
    int          ctxoff,
    final int          bits)
    throws IOException
  {
    int n = 1;
    int m = (1 << bits);
    ctxoff--;

    while(n < m)
    {
      int b = zp.decoder(ctx[ctxoff + n]);
      n = (n << 1) | b;
    }

    return n - m;
  }

  /**
   * Called to decode more bits.
   *
   * @param bits the number of bits to decode
   *
   * @return the decoded bits
   *
   * @throws IOException if an error occurs
   */
  private int decode_raw(final int bits)
    throws IOException
  {
    int       n = 1;
    final int m = (1 << bits);

    while(n < m)
    {
      final int b = zp.decoder();
      n = (n << 1) | b;
    }

    return n - m;
  }
}
