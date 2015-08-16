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
 * @version $Revision: 1.5 $
 */
public class JB2Decode
  extends JB2Codec
{
  //~ Instance fields --------------------------------------------------------

  /** DOCUMENT ME! */
  protected final BitContext zpBitHolder = new BitContext();
  private GBitmap            refBitmap;
  private JB2Dict            zdict = null;
  private ZPCodec            zp    = null;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new JB2Decode object.
   */
  public JB2Decode()
  {
    super(false);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of JB2Decode with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of JB2Decode.
   */
  public static JB2Decode createJB2Decode(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (JB2Decode)create(
      options,
      options.getJB2DecodeClass(),
      JB2Decode.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  public void code(final JB2Image jim)
    throws IOException
  {
    int rectype = START_OF_DATA;

    do
    {
      rectype = code_record_B(rectype, jim, null, null);
    }
    while(rectype != END_OF_DATA);

    if(!gotstartrecordp)
    {
      throw new IllegalStateException("JB2Image no start");
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  public void code(final JB2Dict jim)
    throws IOException
  {
    int rectype = START_OF_DATA;

    do
    {
      rectype = code_record_A(rectype, jim, null);
    }
    while(rectype != END_OF_DATA);

    if(!gotstartrecordp)
    {
      throw new IllegalStateException("JB2Image no start");
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param gbs DOCUMENT ME!
   * @param zdict DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public void init(
    final InputStream gbs,
    JB2Dict           zdict)
    throws IOException
  {
    this.zdict   = zdict;
    zp           = new ZPCodec(gbs);
  }

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   * @param ctx DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected final boolean codeBit(
    final boolean    ignored,
    final BitContext ctx)
    throws IOException
  {
    return (zp.decoder(ctx) != 0);
  }

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   * @param array DOCUMENT ME!
   * @param offset DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected final int codeBit(
    final boolean ignored,
    final byte[]  array,
    final int     offset)
    throws IOException
  {
    zpBitHolder.set(array[offset]);

    final int retval = zp.decoder(zpBitHolder);
    array[offset] = zpBitHolder.bit;

    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @param low DOCUMENT ME!
   * @param high DOCUMENT ME!
   * @param ctx DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected int codeNum(
    final int        low,
    final int        high,
    final NumContext ctx)
    throws IOException
  {
    return codeNum(low, high, ctx, 0);
  }

  /**
   * DOCUMENT ME!
   *
   * @param jblt DOCUMENT ME!
   * @param rows DOCUMENT ME!
   * @param columns DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  protected void code_absolute_location(
    final JB2Blit jblt,
    final int     rows,
    final int     columns)
    throws IOException
  {
    if(!gotstartrecordp)
    {
      throw new IllegalStateException("JB2Image no start");
    }

    final int left = codeNum(1, image_columns, abs_loc_x);
    final int top = codeNum(1, image_rows, abs_loc_y);
    jblt.bottom(top - rows);
    jblt.left(left - 1);
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param border DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  protected void code_absolute_mark_size(
    final GBitmap bm,
    final int     border)
    throws IOException
  {
    final int xsize = codeNum(0, BIGPOSITIVE, abs_size_x);
    final int ysize = codeNum(0, BIGPOSITIVE, abs_size_y);

    if((xsize != (0xffff & xsize)) || (ysize != (0xffff & ysize)))
    {
      throw new IllegalStateException("JB2Image bad number");
    }

    bm.init(ysize, xsize, border);
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cbm DOCUMENT ME!
   * @param xd2c DOCUMENT ME!
   * @param dw DOCUMENT ME!
   * @param dy DOCUMENT ME!
   * @param cy DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   * @param xup1 DOCUMENT ME!
   * @param xup0 DOCUMENT ME!
   * @param xdn1 DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_bitmap_by_cross_coding(
    final GBitmap bm,
    final GBitmap cbm,
    final int     xd2c,
    final int     dw,
    int           dy,
    int           cy,
    int           up1,
    int           up0,
    int           xup1,
    int           xup0,
    int           xdn1)
    throws IOException
  {
    while(dy >= 0)
    {
      int context = get_cross_context(bm, cbm, up1, up0, xup1, xup0, xdn1, 0);

      for(int dx = 0; dx < dw;)
      {
        final int n = codeBit(false, cbitdist, context);
        bm.setByteAt(up0 + dx++, n);
        context =
          shift_cross_context(
            bm,
            cbm,
            context,
            n,
            up1,
            up0,
            xup1,
            xup0,
            xdn1,
            dx);
      }

      up1    = up0;
      up0    = bm.rowOffset(--dy);
      xup1   = xup0;
      xup0   = xdn1;
      xdn1   = cbm.rowOffset((--cy) - 1) + xd2c;
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param dw DOCUMENT ME!
   * @param dy DOCUMENT ME!
   * @param up2 DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_bitmap_directly(
    final GBitmap bm,
    final int     dw,
    int           dy,
    int           up2,
    int           up1,
    int           up0)
    throws IOException
  {
    while(dy >= 0)
    {
      int context = get_direct_context(bm, up2, up1, up0, 0);

      for(int dx = 0; dx < dw;)
      {
        final int n = codeBit(false, bitdist, context);
        bm.setByteAt(up0 + dx++, n);
        context = shift_direct_context(bm, context, n, up2, up1, up0, dx);
      }

      up2   = up1;
      up1   = up0;
      up0   = bm.rowOffset(--dy);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param comment DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected String code_comment(final String comment)
    throws IOException
  {
    final int size   = codeNum(0, BIGPOSITIVE, dist_comment_length);
    byte[]    combuf = new byte[size];

    for(int i = 0; i < size; i++)
    {
      combuf[i] = (byte)codeNum(0, 255, dist_comment_byte);
    }

    return new String(combuf);
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  protected void code_image_size(final JB2Dict jim)
    throws IOException
  {
    final int w = codeNum(0, BIGPOSITIVE, image_size_dist);
    final int h = codeNum(0, BIGPOSITIVE, image_size_dist);

    if((w != 0) || (h != 0))
    {
      throw new IllegalStateException("JB2Image bad dict 2");
    }

    super.code_image_size(jim);
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  protected void code_image_size(final JB2Image jim)
    throws IOException
  {
    image_columns   = codeNum(0, BIGPOSITIVE, image_size_dist);
    image_rows      = codeNum(0, BIGPOSITIVE, image_size_dist);

    if((image_columns == 0) || (image_rows == 0))
    {
      throw new IllegalStateException("JB2Image zero size");
    }

    jim.width    = image_columns;
    jim.height   = image_rows;
    super.code_image_size(jim);
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  protected void code_inherited_shape_count(final JB2Dict jim)
    throws IOException
  {
    final int size = codeNum(0, BIGPOSITIVE, inherited_shape_count_dist);
    JB2Dict   dict = jim.get_inherited_dict();

    if((dict == null) && (size > 0))
    {
      if(zdict != null)
      {
        dict = zdict;
        jim.set_inherited_dict(dict);
      }
      else
      {
        throw new IllegalStateException("JB2Image need dict");
      }
    }

    if((dict != null) && (size != dict.get_shape_count()))
    {
      throw new IllegalStateException("JB2Image bad dict");
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param index DOCUMENT ME!
   * @param ignored DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected int code_match_index(
    final int     index,
    final JB2Dict ignored)
    throws IOException
  {
    return codeNum(0, lib2shape.size() - 1, dist_match_index);
  }

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected int code_record_type(final int ignored)
    throws IOException
  {
    return codeNum(START_OF_DATA, END_OF_DATA, dist_record_type);
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cw DOCUMENT ME!
   * @param ch DOCUMENT ME!
   * @param border DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalStateException DOCUMENT ME!
   */
  protected void code_relative_mark_size(
    final GBitmap bm,
    final int     cw,
    final int     ch,
    final int     border)
    throws IOException
  {
    final int xdiff = codeNum(BIGNEGATIVE, BIGPOSITIVE, rel_size_x);
    final int ydiff = codeNum(BIGNEGATIVE, BIGPOSITIVE, rel_size_y);
    final int xsize = cw + xdiff;
    final int ysize = ch + ydiff;

    if((xsize != (0xffff & xsize)) || (ysize != (0xffff & ysize)))
    {
      throw new IllegalStateException("JB2Image bad number");
    }

    bm.init(ysize, xsize, border);
  }

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   * @param rel_loc DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected int get_diff(
    final int        ignored,
    final NumContext rel_loc)
    throws IOException
  {
    return codeNum(BIGNEGATIVE, BIGPOSITIVE, rel_loc);
  }
}
