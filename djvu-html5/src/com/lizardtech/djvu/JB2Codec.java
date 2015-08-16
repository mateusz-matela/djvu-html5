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
 * @version $Revision: 1.6 $
 */
public abstract class JB2Codec
  extends DjVuObject
{
  //~ Static fields/initializers ---------------------------------------------

  /** DOCUMENT ME! */
  protected static final int BIGPOSITIVE = 262142;

  /** DOCUMENT ME! */
  protected static final int BIGNEGATIVE = -262143;

  /** DOCUMENT ME! */
  protected static final byte START_OF_DATA = 0;

  /** DOCUMENT ME! */
  protected static final byte NEW_MARK = 1;

  /** DOCUMENT ME! */
  protected static final byte NEW_MARK_LIBRARY_ONLY = 2;

  /** DOCUMENT ME! */
  protected static final byte NEW_MARK_IMAGE_ONLY = 3;

  /** DOCUMENT ME! */
  protected static final byte MATCHED_REFINE = 4;

  /** DOCUMENT ME! */
  protected static final byte MATCHED_REFINE_LIBRARY_ONLY = 5;

  /** DOCUMENT ME! */
  protected static final byte MATCHED_REFINE_IMAGE_ONLY = 6;

  /** DOCUMENT ME! */
  protected static final byte MATCHED_COPY = 7;

  /** DOCUMENT ME! */
  protected static final byte NON_MARK_DATA = 8;

  /** DOCUMENT ME! */
  protected static final byte REQUIRED_DICT_OR_RESET = 9;

  /** DOCUMENT ME! */
  protected static final byte PRESERVED_COMMENT = 10;

  /** DOCUMENT ME! */
  protected static final byte END_OF_DATA = 11;

  /** DOCUMENT ME! */
  protected static final Integer MINUS_ONE_OBJECT = new Integer(-1);

  //~ Instance fields --------------------------------------------------------

  // Library

  /** DOCUMENT ME! */
  protected final BitContext dist_refinement_flag = new BitContext();

  /** DOCUMENT ME! */
  protected final BitContext offset_type_dist = new BitContext();

  // Code pairs

  /** DOCUMENT ME! */
  protected final NumContext abs_loc_x = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext abs_loc_y = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext abs_size_x = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext abs_size_y = new NumContext();

  // Code comment

  /** DOCUMENT ME! */
  protected final NumContext dist_comment_byte = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext dist_comment_length = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext dist_match_index = new NumContext();

  // Code values

  /** DOCUMENT ME! */
  protected final NumContext dist_record_type = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext image_size_dist = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext inherited_shape_count_dist = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext rel_loc_x_current = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext rel_loc_x_last = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext rel_loc_y_current = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext rel_loc_y_last = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext rel_size_x = new NumContext();

  /** DOCUMENT ME! */
  protected final NumContext rel_size_y = new NumContext();

  /** DOCUMENT ME! */
  protected final Vector bitcells = new Vector();

  /** DOCUMENT ME! */
  protected final Vector leftcell = new Vector();

  /** DOCUMENT ME! */
  protected final Vector lib2shape = new Vector();

  /** DOCUMENT ME! */
  protected final Vector libinfo = new Vector();

  /** DOCUMENT ME! */
  protected final Vector rightcell = new Vector();

  /** DOCUMENT ME! */
  protected final Vector shape2lib = new Vector();

  /** DOCUMENT ME! */
  protected final int[] short_list = new int[3];

  /** DOCUMENT ME! */
  protected byte[] bitdist = new byte[1024];

  /** DOCUMENT ME! */
  protected byte[] cbitdist = new byte[2048];

  /** DOCUMENT ME! */
  protected boolean gotstartrecordp = false;

  /** DOCUMENT ME! */
  protected boolean refinementp = false;

  /** DOCUMENT ME! */
  protected int image_columns = 0;

  /** DOCUMENT ME! */
  protected int image_rows = 0;

  /** DOCUMENT ME! */
  protected int last_bottom = 0;

  /** DOCUMENT ME! */
  protected int last_left = 0;

  /** DOCUMENT ME! */
  protected int last_right = 0;

  /** DOCUMENT ME! */
  protected int last_row_bottom = 0;

  /** DOCUMENT ME! */
  protected int last_row_left = 0;

  /** DOCUMENT ME! */
  protected int short_list_pos = 0;

  /** DOCUMENT ME! */
  private final boolean encoding;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new JB2Codec object.
   *
   * @param encoding DOCUMENT ME!
   */
  protected JB2Codec(final boolean encoding)
  {
    this.encoding = encoding;

    for(int i = 0; i < bitdist.length;)
    {
      bitdist[i++] = 0;
    }

    for(int i = 0; i < cbitdist.length;)
    {
      cbitdist[i++] = 0;
    }

    bitcells.addElement(new BitContext());
    leftcell.addElement(new NumContext());
    rightcell.addElement(new NumContext());
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * DOCUMENT ME!
   *
   * @param low DOCUMENT ME!
   * @param high DOCUMENT ME!
   * @param ctx DOCUMENT ME!
   * @param v DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws ArrayIndexOutOfBoundsException DOCUMENT ME!
   */
  protected int codeNum(
    int        low,
    int        high,
    NumContext ctx,
    int        v)
    throws IOException
  {
    boolean negative = false;
    int     cutoff = 0;

    int     ictx = ctx.intValue();

    if(ictx >= bitcells.size())
    {
      throw new ArrayIndexOutOfBoundsException("JB2Image bad numcontext");
    }

    for(int phase = 1, range = -1; range != 1; ictx = ctx.intValue())
    {
      boolean decision;

      if(ictx == 0)
      {
        ictx = bitcells.size();
        ctx.set(ictx);

        final BitContext pbitcells  = new BitContext();
        final NumContext pleftcell  = new NumContext();
        final NumContext prightcell = new NumContext();
        bitcells.addElement(pbitcells);
        leftcell.addElement(pleftcell);
        rightcell.addElement(prightcell);
        decision =
          encoding
          ? (((low < cutoff) && (high >= cutoff))
          ? codeBit((v >= cutoff), pbitcells)
          : (v >= cutoff))
          : ((low >= cutoff)
          || ((high >= cutoff) && codeBit(false, pbitcells)));

        ctx = (decision
          ? prightcell
          : pleftcell);
      }
      else
      {
        decision =
          encoding
          ? (((low < cutoff) && (high >= cutoff))
          ? codeBit((v >= cutoff), (BitContext)bitcells.elementAt(ictx))
          : (v >= cutoff))
          : ((low >= cutoff)
          || ((high >= cutoff)
          && codeBit(false, (BitContext)bitcells.elementAt(ictx))));

        ctx =
          (NumContext)(decision
          ? rightcell.elementAt(ictx)
          : leftcell.elementAt(ictx));
      }

      switch(phase)
      {
        case 1 :
        {
          negative = !decision;

          if(negative)
          {
            if(encoding)
            {
              v = -v - 1;
            }

            final int temp = -low - 1;
            low    = -high - 1;
            high   = temp;
          }

          phase    = 2;
          cutoff   = 1;

          break;
        }
        case 2 :
        {
          if(!decision)
          {
            phase   = 3;
            range   = (cutoff + 1) / 2;

            if(range == 1)
            {
              cutoff = 0;
            }
            else
            {
              cutoff -= (range / 2);
            }
          }
          else
          {
            cutoff = (2 * cutoff) + 1;
          }

          break;
        }
        case 3 :
        {
          range /= 2;

          if(range != 1)
          {
            if(!decision)
            {
              cutoff -= (range / 2);
            }
            else
            {
              cutoff += (range / 2);
            }
          }
          else if(!decision)
          {
            cutoff--;
          }

          break;
        }
      }
    }

    return negative
    ? (-cutoff - 1)
    : cutoff;
  }

  /**
   * DOCUMENT ME!
   *
   * @param jblt DOCUMENT ME!
   * @param rows DOCUMENT ME!
   * @param columns DOCUMENT ME!
   */
  protected abstract void code_absolute_location(
    final JB2Blit jblt,
    final int     rows,
    final int     columns)
    throws IOException;

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
   */
  protected abstract void code_bitmap_by_cross_coding(
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
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param dw DOCUMENT ME!
   * @param dy DOCUMENT ME!
   * @param up2 DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   */
  protected abstract void code_bitmap_directly(
    final GBitmap bm,
    final int     dw,
    int           dy,
    int           up2,
    int           up1,
    int           up0)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_eventual_lossless_refinement()
    throws IOException
  {
    refinementp = codeBit(refinementp, dist_refinement_flag);
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   */
  protected abstract void code_inherited_shape_count(final JB2Dict jim)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected final void code_absolute_mark_size(final GBitmap bm)
    throws IOException
  {
    code_absolute_mark_size(bm, 0);
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param border DOCUMENT ME!
   */
  protected abstract void code_absolute_mark_size(
    final GBitmap bm,
    final int     border)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_image_size(final JB2Dict ignored)
    throws IOException
  {
    last_left         = 1;
    last_row_bottom   = 0;
    last_row_left     = last_right = 0;
    fill_short_list(last_row_bottom);
    gotstartrecordp = true;
  }

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_image_size(final JB2Image ignored)
    throws IOException
  {
    last_left         = 1 + image_columns;
    last_row_bottom   = image_rows;
    last_row_left     = last_right = 0;
    fill_short_list(last_row_bottom);
    gotstartrecordp = true;
  }

  /**
   * DOCUMENT ME!
   *
   * @param rectype DOCUMENT ME!
   * @param jim DOCUMENT ME!
   * @param xjshp DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalArgumentException DOCUMENT ME!
   */
  protected int code_record_A(
    int            rectype,
    final JB2Dict  jim,
    final JB2Shape xjshp)
    throws IOException
  {
    GBitmap bm      = null;
    int     shapeno = -1;
    rectype = code_record_type(rectype);

    JB2Shape jshp = xjshp;

    switch(rectype)
    {
      case NEW_MARK_LIBRARY_ONLY :
      case MATCHED_REFINE_LIBRARY_ONLY :
      {
        if(!encoding)
        {
          jshp = JB2Shape.createJB2Shape(this).init(-1);
        }
        else if(jshp == null)
        {
          jshp = JB2Shape.createJB2Shape(this);
        }

        bm = jshp.getGBitmap();

        break;
      }
    }

    switch(rectype)
    {
      case START_OF_DATA :
      {
        code_image_size(jim);
        code_eventual_lossless_refinement();

        if(!encoding)
        {
          init_library(jim);
        }

        break;
      }
      case NEW_MARK_LIBRARY_ONLY :
      {
        code_absolute_mark_size(bm, 4);
        code_bitmap_directly(bm);

        break;
      }
      case MATCHED_REFINE_LIBRARY_ONLY :
      {
        final int match = code_match_index(jshp.parent, jim);

        if(!encoding)
        {
          jshp.parent = ((Number)lib2shape.elementAt(match)).intValue();
        }

        final GBitmap cbm    = jim.get_shape(jshp.parent).getGBitmap();
        GRect       lmatch = (GRect)libinfo.elementAt(match);
        code_relative_mark_size(
          bm,
          (1 + lmatch.xmax) - lmatch.xmin,
          (1 + lmatch.ymax) - lmatch.ymin,
          4);
        code_bitmap_by_cross_coding(bm, cbm, jshp.parent);

        break;
      }
      case PRESERVED_COMMENT :
      {
        jim.comment = code_comment(jim.comment);

        break;
      }
      case REQUIRED_DICT_OR_RESET :
      {
        if(!gotstartrecordp)
        {
          code_inherited_shape_count(jim);
        }
        else
        {
          reset_numcoder();
        }

        break;
      }
      case END_OF_DATA :
        break;
      default :
        throw new IllegalArgumentException("JB2Image bad type");
    }

    if(!encoding)
    {
      switch(rectype)
      {
        case NEW_MARK_LIBRARY_ONLY :
        case MATCHED_REFINE_LIBRARY_ONLY :
        {
          if(xjshp != null)
          {
            jshp = jshp.duplicate();
          }

          shapeno = jim.add_shape(jshp);
          add_library(shapeno, jshp);

          break;
        }
      }
    }

    return rectype;
  }

  /**
   * DOCUMENT ME!
   *
   * @param rectype DOCUMENT ME!
   * @param jim DOCUMENT ME!
   * @param xjshp DOCUMENT ME!
   * @param xjblt DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   * @throws IllegalArgumentException DOCUMENT ME!
   */
  protected int code_record_B(
    int            rectype,
    final JB2Image jim,
    final JB2Shape xjshp,
    final JB2Blit  xjblt)
    throws IOException
  {
    GBitmap  bm      = null;
    int      shapeno = -1;
    JB2Shape jshp    = xjshp;
    JB2Blit  jblt    = xjblt;
    rectype = code_record_type(rectype);

    switch(rectype)
    {
      case NEW_MARK :
      case NEW_MARK_IMAGE_ONLY :
      case MATCHED_REFINE :
      case MATCHED_REFINE_IMAGE_ONLY :
      case NON_MARK_DATA :
      {
        if(jblt == null)
        {
          jblt = new JB2Blit();
        }

        // fall through
      }
      case NEW_MARK_LIBRARY_ONLY :
      case MATCHED_REFINE_LIBRARY_ONLY :
      {
        if(!encoding)
        {
          jshp =
            JB2Shape.createJB2Shape(this).init(
              (rectype == NON_MARK_DATA)
              ? (-2)
              : (-1));
        }
        else if(jshp == null)
        {
          jshp = JB2Shape.createJB2Shape(this);
        }

        bm = jshp.getGBitmap();

        break;
      }
      case MATCHED_COPY :
      {
        if(jblt == null)
        {
          jblt = new JB2Blit();
        }

        break;
      }
    }

    boolean needAddLibrary = false;
    boolean needAddBlit = false;

    switch(rectype)
    {
      case START_OF_DATA :
      {
        code_image_size(jim);
        code_eventual_lossless_refinement();

        if(!encoding)
        {
          init_library(jim);
        }

        break;
      }
      case NEW_MARK :
      {
        needAddBlit = needAddLibrary = true;
        code_absolute_mark_size(bm, 4);
        code_bitmap_directly(bm);
        code_relative_location(
          jblt,
          bm.rows(),
          bm.columns());

        break;
      }
      case NEW_MARK_LIBRARY_ONLY :
      {
        needAddLibrary = true;
        code_absolute_mark_size(bm, 4);
        code_bitmap_directly(bm);

        break;
      }
      case NEW_MARK_IMAGE_ONLY :
      {
        needAddBlit = true;
        code_absolute_mark_size(bm, 3);
        code_bitmap_directly(bm);
        code_relative_location(
          jblt,
          bm.rows(),
          bm.columns());

        break;
      }
      case MATCHED_REFINE :
      {
        needAddBlit      = true;
        needAddLibrary   = true;

        final int match  = code_match_index(jshp.parent, jim);

        if(!encoding)
        {
          jshp.parent = ((Number)lib2shape.elementAt(match)).intValue();
        }

        final GBitmap cbm    = jim.get_shape(jshp.parent).getGBitmap();
        final GRect lmatch = (GRect)libinfo.elementAt(match);
        code_relative_mark_size(
          bm,
          (1 + lmatch.xmax) - lmatch.xmin,
          (1 + lmatch.ymax) - lmatch.ymin,
          4);

//          verbose("2.d time="+System.currentTimeMillis()+",rectype="+rectype);
        code_bitmap_by_cross_coding(bm, cbm, match);

//          verbose("2.e time="+System.currentTimeMillis()+",rectype="+rectype);
        code_relative_location(
          jblt,
          bm.rows(),
          bm.columns());

        break;
      }
      case MATCHED_REFINE_LIBRARY_ONLY :
      {
        needAddLibrary = true;

        final int match = code_match_index(jshp.parent, jim);

        if(!encoding)
        {
          jshp.parent = ((Number)lib2shape.elementAt(match)).intValue();
        }

        final GBitmap cbm    = jim.get_shape(jshp.parent).getGBitmap();
        final GRect lmatch = (GRect)libinfo.elementAt(match);
        code_relative_mark_size(
          bm,
          (1 + lmatch.xmax) - lmatch.xmin,
          (1 + lmatch.ymax) - lmatch.ymin,
          4);

        break;
      }
      case MATCHED_REFINE_IMAGE_ONLY :
      {
        needAddBlit = true;

        final int match = code_match_index(jshp.parent, jim);

        if(!encoding)
        {
          jshp.parent = ((Number)lib2shape.elementAt(match)).intValue();
        }

        final GBitmap cbm    = jim.get_shape(jshp.parent).getGBitmap();
        final GRect lmatch = (GRect)libinfo.elementAt(match);
        code_relative_mark_size(
          bm,
          (1 + lmatch.xmax) - lmatch.xmin,
          (1 + lmatch.ymax) - lmatch.ymin,
          4);
        code_bitmap_by_cross_coding(bm, cbm, match);
        code_relative_location(
          jblt,
          bm.rows(),
          bm.columns());

        break;
      }
      case MATCHED_COPY :
      {
        final int match = code_match_index(
            jblt.shapeno(),
            jim);

        if(!encoding)
        {
          jblt.shapeno(((Number)lib2shape.elementAt(match)).intValue());
        }

        bm = jim.get_shape(jblt.shapeno()).getGBitmap();

        final GRect lmatch = (GRect)libinfo.elementAt(match);
        jblt.leftAdd(lmatch.xmin);
        jblt.bottomAdd(lmatch.ymin);

        if(jim.reproduce_old_bug)
        {
          code_relative_location(
            jblt,
            bm.rows(),
            bm.columns());
        }
        else
        {
          code_relative_location(
            jblt,
            (1 + lmatch.ymax) - lmatch.ymin,
            (1 + lmatch.xmax) - lmatch.xmin);
        }

        jblt.leftSubtract(lmatch.xmin);
        jblt.bottomSubtract(lmatch.ymin);

        break;
      }
      case NON_MARK_DATA :
      {
        needAddBlit = true;
        code_absolute_mark_size(bm, 3);
        code_bitmap_directly(bm);
        code_absolute_location(
          jblt,
          bm.rows(),
          bm.columns());

        break;
      }
      case PRESERVED_COMMENT :
      {
        jim.comment = code_comment(jim.comment);

        break;
      }
      case REQUIRED_DICT_OR_RESET :
      {
        if(!gotstartrecordp)
        {
          code_inherited_shape_count(jim);
        }
        else
        {
          reset_numcoder();
        }

        break;
      }
      case END_OF_DATA :
        break;
      default :
        throw new IllegalArgumentException("JB2Image unknown type");
    }

    if(!encoding)
    {
      switch(rectype)
      {
        case NEW_MARK :
        case NEW_MARK_LIBRARY_ONLY :
        case MATCHED_REFINE :
        case MATCHED_REFINE_LIBRARY_ONLY :
        case NEW_MARK_IMAGE_ONLY :
        case MATCHED_REFINE_IMAGE_ONLY :
        case NON_MARK_DATA :
        {
          if(xjshp != null)
          {
            jshp = jshp.duplicate();
          }

          shapeno = jim.add_shape(jshp);
          shape2lib(shapeno, MINUS_ONE_OBJECT);

          if(needAddLibrary)
          {
            add_library(shapeno, jshp);
          }

          if(needAddBlit)
          {
            jblt.shapeno(shapeno);

            if(xjblt != null)
            {
              jblt = (JB2Blit)xjblt.clone();
            }

            jim.add_blit(jblt);
          }

          break;
        }
        case MATCHED_COPY :
        {
          if(xjblt != null)
          {
            jblt = (JB2Blit)xjblt.clone();
          }

          jim.add_blit(jblt);

          break;
        }
      }
    }

    return rectype;
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cw DOCUMENT ME!
   * @param ch DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected final void code_relative_mark_size(
    final GBitmap bm,
    final int     cw,
    final int     ch)
    throws IOException
  {
    code_relative_mark_size(bm, cw, ch, 0);
  }

  /**
   * DOCUMENT ME!
   *
   * @param v DOCUMENT ME!
   */
  protected final void fill_short_list(final int v)
  {
    short_list[0]    = short_list[1] = short_list[2] = v;
    short_list_pos   = 0;
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cbm DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   * @param xup1 DOCUMENT ME!
   * @param xup0 DOCUMENT ME!
   * @param xdn1 DOCUMENT ME!
   * @param column DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected final int get_cross_context(
    final GBitmap bm,
    final GBitmap cbm,
    final int     up1,
    final int     up0,
    final int     xup1,
    final int     xup0,
    final int     xdn1,
    final int     column)
  {
    return ((bm.getByteAt((up1 + column) - 1) << 10)
    | (bm.getByteAt(up1 + column) << 9)
    | (bm.getByteAt(up1 + column + 1) << 8)
    | (bm.getByteAt((up0 + column) - 1) << 7)
    | (cbm.getByteAt(xup1 + column) << 6)
    | (cbm.getByteAt((xup0 + column) - 1) << 5)
    | (cbm.getByteAt(xup0 + column) << 4)
    | (cbm.getByteAt(xup0 + column + 1) << 3)
    | (cbm.getByteAt((xdn1 + column) - 1) << 2)
    | (cbm.getByteAt(xdn1 + column) << 1)
    | (cbm.getByteAt(xdn1 + column + 1)));
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param up2 DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   * @param column DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected final int get_direct_context(
    final GBitmap bm,
    final int     up2,
    final int     up1,
    final int     up0,
    final int     column)
  {
    return ((bm.getByteAt((up2 + column) - 1) << 9)
    | (bm.getByteAt(up2 + column) << 8)
    | (bm.getByteAt(up2 + column + 1) << 7)
    | (bm.getByteAt((up1 + column) - 2) << 6)
    | (bm.getByteAt((up1 + column) - 1) << 5)
    | (bm.getByteAt(up1 + column) << 4)
    | (bm.getByteAt(up1 + column + 1) << 3)
    | (bm.getByteAt(up1 + column + 2) << 2)
    | (bm.getByteAt((up0 + column) - 2) << 1)
    | (bm.getByteAt((up0 + column) - 1)));
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cw DOCUMENT ME!
   * @param ch DOCUMENT ME!
   * @param border DOCUMENT ME!
   */
  protected abstract void code_relative_mark_size(
    final GBitmap bm,
    final int     cw,
    final int     ch,
    final int     border)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param ignored DOCUMENT ME!
   * @param rel_loc DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected abstract int get_diff(
    final int  ignored,
    NumContext rel_loc)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param shapeno DOCUMENT ME!
   * @param jshp DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected int add_library(
    final int      shapeno,
    final JB2Shape jshp)
  {
    final int libno = lib2shape.size();
    lib2shape.addElement(new Integer(shapeno));
    shape2lib(
      shapeno,
      new Integer(libno));

    //final GRect r = new GRect();
    //libinfo.addElement(r);
    //jshp.getGBitmap().compute_bounding_box(r);
    libinfo.addElement(jshp.getGBitmap().compute_bounding_box());

    return libno;
  }

  /**
   * DOCUMENT ME!
   */
  protected void reset_numcoder()
  {
    dist_comment_byte.set(0);
    dist_comment_length.set(0);
    dist_record_type.set(0);
    dist_match_index.set(0);
    abs_loc_x.set(0);
    abs_loc_y.set(0);
    abs_size_x.set(0);
    abs_size_y.set(0);
    image_size_dist.set(0);
    inherited_shape_count_dist.set(0);
    rel_loc_x_current.set(0);
    rel_loc_x_last.set(0);
    rel_loc_y_current.set(0);
    rel_loc_y_last.set(0);
    rel_size_x.set(0);
    rel_size_y.set(0);
    bitcells.setSize(0);
    leftcell.setSize(0);
    rightcell.setSize(0);
    bitcells.addElement(new BitContext());
    leftcell.addElement(new NumContext());
    rightcell.addElement(new NumContext());

    if(DjVuOptions.COLLECT_GARBAGE)
    {
      System.gc();
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param shapeno DOCUMENT ME!
   * @param libno DOCUMENT ME!
   */
  protected final void shape2lib(
    final int shapeno,
    Number    libno)
  {
    int size = shape2lib.size();

    if(size <= shapeno)
    {
      while(size++ < shapeno)
      {
        shape2lib.addElement(MINUS_ONE_OBJECT);
      }

      shape2lib.addElement(libno);
    }
    else
    {
      shape2lib.setElementAt(libno, shapeno);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cbm DOCUMENT ME!
   * @param context DOCUMENT ME!
   * @param n DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   * @param xup1 DOCUMENT ME!
   * @param xup0 DOCUMENT ME!
   * @param xdn1 DOCUMENT ME!
   * @param column DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected final int shift_cross_context(
    final GBitmap bm,
    final GBitmap cbm,
    final int     context,
    final int     n,
    final int     up1,
    final int     up0,
    final int     xup1,
    final int     xup0,
    final int     xdn1,
    final int     column)
  {
    return (((context << 1) & 0x636) | (bm.getByteAt(up1 + column + 1) << 8)
    | (cbm.getByteAt(xup1 + column) << 6)
    | (cbm.getByteAt(xup0 + column + 1) << 3)
    | (cbm.getByteAt(xdn1 + column + 1)) | (n << 7));
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param context DOCUMENT ME!
   * @param next DOCUMENT ME!
   * @param up2 DOCUMENT ME!
   * @param up1 DOCUMENT ME!
   * @param up0 DOCUMENT ME!
   * @param column DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected final int shift_direct_context(
    final GBitmap bm,
    final int     context,
    final int     next,
    final int     up2,
    final int     up1,
    final int     up0,
    final int     column)
  {
    return (((context << 1) & 0x37a) | (bm.getByteAt(up1 + column + 2) << 2)
    | (bm.getByteAt(up2 + column + 1) << 7) | next);
  }

  /**
   * DOCUMENT ME!
   *
   * @param bit DOCUMENT ME!
   * @param ctx DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected abstract boolean codeBit(
    final boolean bit,
    BitContext    ctx)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param bit DOCUMENT ME!
   * @param array DOCUMENT ME!
   * @param offset DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected abstract int codeBit(
    final boolean bit,
    byte[]        array,
    int           offset)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param comment DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected abstract String code_comment(final String comment)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param index DOCUMENT ME!
   * @param jim DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected abstract int code_match_index(
    int     index,
    JB2Dict jim)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param rectype DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected abstract int code_record_type(int rectype)
    throws IOException;

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   * @param cbm DOCUMENT ME!
   * @param libno DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_bitmap_by_cross_coding(
    final GBitmap bm,
    final GBitmap cbm,
    
//      final GBitmap xcbm,
  final int       libno)
    throws IOException
  {
    // GBitmap cbm=new GBitmap();
    // synchronized(xcbm)
    // {
    //   cbm.init(xcbm);
    // }
    synchronized(bm)
    {
      final int     cw     = cbm.columns();
      final int     dw     = bm.columns();
      final int     dh     = bm.rows();
      final GRect lmatch = (GRect)libinfo.elementAt(libno);
      final int     xd2c   =
        ((1 + (dw / 2)) - dw)
        - ((((1 + lmatch.xmax) - lmatch.xmin) / 2) - lmatch.xmax);
      final int yd2c =
        ((1 + (dh / 2)) - dh)
        - ((((1 + lmatch.ymax) - lmatch.ymin) / 2) - lmatch.ymax);

      bm.setMinimumBorder(2);
      cbm.setMinimumBorder(2 - xd2c);
      cbm.setMinimumBorder((2 + dw + xd2c) - cw);

      final int dy = dh - 1;
      final int cy = dy + yd2c;
      code_bitmap_by_cross_coding(
        bm,
        cbm,
        xd2c,
        dw,
        dy,
        cy,
        bm.rowOffset(dy + 1),
        bm.rowOffset(dy),
        cbm.rowOffset(cy + 1) + xd2c,
        cbm.rowOffset(cy) + xd2c,
        cbm.rowOffset(cy - 1) + xd2c);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param bm DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  protected void code_bitmap_directly(final GBitmap bm)
    throws IOException
  {
    synchronized(bm)
    {
      bm.setMinimumBorder(3);

      final int dy = bm.rows() - 1;
      code_bitmap_directly(
        bm,
        bm.columns(),
        dy,
        bm.rowOffset(dy + 2),
        bm.rowOffset(dy + 1),
        bm.rowOffset(dy));
    }
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
  protected void code_relative_location(
    final JB2Blit jblt,
    final int     rows,
    final int     columns)
    throws IOException
  {
    if(!gotstartrecordp)
    {
      throw new IllegalStateException("JB2Image no start");
    }

    int bottom = 0;
    int left  = 0;
    int top   = 0;
    int right = 0;

    if(encoding)
    {
      left     = jblt.left() + 1;
      bottom   = jblt.bottom() + 1;
      right    = (left + columns) - 1;
      top      = (bottom + rows) - 1;
    }

    final boolean new_row = codeBit((left < last_left), offset_type_dist);

    if(new_row)
    {
      final int x_diff = get_diff(left - last_row_left, rel_loc_x_last);
      final int y_diff = get_diff(top - last_row_bottom, rel_loc_y_last);

      if(!encoding)
      {
        left     = last_row_left + x_diff;
        top      = last_row_bottom + y_diff;
        right    = (left + columns) - 1;
        bottom   = (top - rows) + 1;
      }

      last_left     = last_row_left = left;
      last_right    = right;
      last_bottom   = last_row_bottom = bottom;
      fill_short_list(bottom);
    }
    else
    {
      final int x_diff = get_diff(left - last_right, rel_loc_x_current);
      final int y_diff = get_diff(bottom - last_bottom, rel_loc_y_current);

      if(!encoding)
      {
        left     = last_right + x_diff;
        bottom   = last_bottom + y_diff;
        right    = (left + columns) - 1;
        top      = (bottom + rows) - 1;
      }

      last_left     = left;
      last_right    = right;
      last_bottom   = update_short_list(bottom);
    }

    if(!encoding)
    {
      jblt.bottom(bottom - 1);
      jblt.left(left - 1);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param jim DOCUMENT ME!
   */
  protected void init_library(final JB2Dict jim)
  {
    final int nshape = jim.get_inherited_shapes();
    shape2lib.setSize(0);
    lib2shape.setSize(0);
    libinfo.setSize(0);

    for(int i = 0; i < nshape; i++)
    {
      final Integer x = new Integer(i);
      shape2lib.addElement(x);
      lib2shape.addElement(x);

      final JB2Shape jshp = jim.get_shape(i);
      //final GRect  r = new GRect();
      //libinfo.addElement(r);
      //jshp.getGBitmap().compute_bounding_box(r);
      libinfo.addElement(jshp.getGBitmap().compute_bounding_box());
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param v DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  protected int update_short_list(final int v)
  {
    if(++short_list_pos == 3)
    {
      short_list_pos = 0;
    }

    short_list[short_list_pos] = v;

    return (short_list[0] >= short_list[1])
    ? ((short_list[0] > short_list[2])
    ? ((short_list[1] >= short_list[2])
    ? short_list[1]
    : short_list[2])
    : short_list[0])
    : ((short_list[0] < short_list[2])
    ? ((short_list[1] >= short_list[2])
    ? short_list[2]
    : short_list[1])
    : short_list[0]);
  }
}
