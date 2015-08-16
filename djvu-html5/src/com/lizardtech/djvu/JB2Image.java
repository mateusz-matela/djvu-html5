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
 * @version $Revision: 1.4 $
 */
public class JB2Image
  extends JB2Dict
{
  //~ Instance fields --------------------------------------------------------

  /** DOCUMENT ME! */
  public boolean reproduce_old_bug = false;

  /** DOCUMENT ME! */
  public int height = 0;

  /** DOCUMENT ME! */
  public int     width = 0;
  private Vector blits = new Vector();

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new JB2Image object.
   */
  public JB2Image() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of JB2Image with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of JB2Image.
   */
  public static JB2Image createJB2Image(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (JB2Image)create(
      options,
      options.getJB2ImageClass(),
      JB2Image.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public Object clone()
  {
    //DjVuOptions.out.println("1. JB2Image clone");
    Cloneable retval = (JB2Image)super.clone();

    if(this.blits != null)
    {
      final Vector blits = new Vector();
      ((JB2Image)retval).blits = blits;

      for(Enumeration e = this.blits.elements(); e.hasMoreElements();)
      {
        blits.addElement(((JB2Blit)e.nextElement()).clone());
      }
    }

    //DjVuOptions.out.println("2. JB2Image clone");
    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final GBitmap get_bitmap()
  {
    return get_bitmap(1);
  }

  /**
   * DOCUMENT ME!
   *
   * @param subsample DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final GBitmap get_bitmap(final int subsample)
  {
    return get_bitmap(subsample, 1);
  }

  /**
   * DOCUMENT ME!
   *
   * @param subsample DOCUMENT ME!
   * @param align DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IllegalStateException DOCUMENT ME!
   */
  public final GBitmap get_bitmap(
    final int subsample,
    final int align)
  {
    if((width == 0) || (height == 0))
    {
      throw new IllegalStateException("JB2Image can not create bitmap");
    }

    final int     swidth  = ((width + subsample) - 1) / subsample;
    final int     sheight = ((height + subsample) - 1) / subsample;
    final int     border  = (((swidth + align) - 1) & ~(align - 1)) - swidth;
    final GBitmap bm      = GBitmap.createGBitmap(this);
    bm.init(sheight, swidth, border);
    bm.setGrays(1 + (subsample * subsample));

    for(int blitno = 0; blitno < get_blit_count(); blitno++)
    {
      final JB2Blit  pblit  = get_blit(blitno);
      final JB2Shape pshape = get_shape(pblit.shapeno());

      if(pshape.getGBitmap() != null)
      {
        bm.blit(
          pshape.getGBitmap(),
          pblit.left(),
          pblit.bottom(),
          subsample);
      }
    }

    return bm;
  }

  /**
   * DOCUMENT ME!
   *
   * @param rect DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final GBitmap get_bitmap(final GRect rect)
  {
    return get_bitmap(rect, 1);
  }

  /**
   * DOCUMENT ME!
   *
   * @param rect DOCUMENT ME!
   * @param subsample DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final GBitmap get_bitmap(
    final GRect rect,
    final int   subsample)
  {
    return get_bitmap(rect, subsample, 1);
  }

  /**
   * DOCUMENT ME!
   *
   * @param rect DOCUMENT ME!
   * @param subsample DOCUMENT ME!
   * @param align DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final GBitmap get_bitmap(
    final GRect rect,
    final int   subsample,
    final int   align)
  {
    return get_bitmap(rect, subsample, align, 0);
  }

  /**
   * DOCUMENT ME!
   *
   * @param rect DOCUMENT ME!
   * @param subsample DOCUMENT ME!
   * @param align DOCUMENT ME!
   * @param dispy DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IllegalStateException DOCUMENT ME!
   */
  public final GBitmap get_bitmap(
    final GRect rect,
    final int   subsample,
    final int   align,
    final int   dispy)
  {
    if((width == 0) || (height == 0))
    {
      throw new IllegalStateException("JB2Image can not create bitmap");
    }

    final int     rxmin   = rect.xmin * subsample;
    final int     rymin   = rect.ymin * subsample;
    final int     swidth  = rect.width();
    final int     sheight = rect.height();
    final int     border  = (((swidth + align) - 1) & ~(align - 1)) - swidth;
    final GBitmap bm      = GBitmap.createGBitmap(this);
    bm.init(sheight, swidth, border);
    bm.setGrays(1 + (subsample * subsample));

    for(int blitno = 0; blitno < get_blit_count();)
    {
      final JB2Blit  pblit  = get_blit(blitno++);
      final JB2Shape pshape = get_shape(pblit.shapeno());

      if(pshape.getGBitmap() != null)
      {
        bm.blit(
          pshape.getGBitmap(),
          pblit.left() - rxmin,
          (dispy + pblit.bottom()) - rymin,
          subsample);
      }
    }

    return bm;
  }

  /**
   * DOCUMENT ME!
   *
   * @param rect DOCUMENT ME!
   * @param subsample DOCUMENT ME!
   * @param align DOCUMENT ME!
   * @param dispy DOCUMENT ME!
   * @param components DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IllegalStateException DOCUMENT ME!
   */
  public final GBitmap get_bitmap(
    final GRect  rect,
    final int    subsample,
    final int    align,
    final int    dispy,
    final Vector components)
  {
    if(components == null)
    {
      return get_bitmap(rect, subsample, align, dispy);
    }

    if((width == 0) || (height == 0))
    {
      throw new IllegalStateException("JB2Image can not create bitmap");
    }

    final int     rxmin   = rect.xmin * subsample;
    final int     rymin   = rect.ymin * subsample;
    final int     swidth  = rect.width();
    final int     sheight = rect.height();
    final int     border  = (((swidth + align) - 1) & ~(align - 1)) - swidth;
    final GBitmap bm      = GBitmap.createGBitmap(this);
    bm.init(sheight, swidth, border);
    bm.setGrays(1 + (subsample * subsample));

    for(int blitno = 0; blitno < get_blit_count();)
    {
      final JB2Blit  pblit  = get_blit(blitno++);
      final JB2Shape pshape = get_shape(pblit.shapeno());

      if(pshape.getGBitmap() != null)
      {
        if(
          bm.blit(
            pshape.getGBitmap(),
            pblit.left() - rxmin,
            (dispy + pblit.bottom()) - rymin,
            subsample))
        {
          components.addElement(new Integer(blitno - 1));
        }
      }
    }

    return bm;
  }

  /**
   * DOCUMENT ME!
   *
   * @param blitno DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final JB2Blit get_blit(int blitno)
  {
    return (JB2Blit)blits.elementAt(blitno);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final int get_blit_count()
  {
    return blits.size();
  }

  /**
   * DOCUMENT ME!
   *
   * @param blit DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IllegalArgumentException DOCUMENT ME!
   */
  public int add_blit(final JB2Blit blit)
  {
    if(blit.shapeno() >= get_shape_count())
    {
      throw new IllegalArgumentException("JB2Image bad shape");
    }

    final int retval = blits.size();

//    blits.addElement(new JB2Blit(blit));
    blits.addElement(blit);

    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @param gbs DOCUMENT ME!
   * @param zdict DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public void decode(
    final InputStream gbs,
    final JB2Dict     zdict)
    throws IOException
  {
    init();

    final JB2Decode codec = JB2Decode.createJB2Decode(this);
    codec.init(gbs, zdict);
    codec.code(this);
  }

  /**
   * DOCUMENT ME!
   */
  public void init()
  {
    width = height = 0;
    blits.setSize(0);
    super.init();
  }
}
