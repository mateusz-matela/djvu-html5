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
public class JB2Dict
  extends DjVuObject
  implements Cloneable, Codec
{
  //~ Instance fields --------------------------------------------------------

  /** Comment string coded by JB2 file. */
  public String comment = "";

  /** DOCUMENT ME! */
  private JB2Dict inherited_dict = null;

  /** DOCUMENT ME! */
  private Vector shapes = new Vector();

  /** DOCUMENT ME! */
  private int inherited_shapes = 0;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new JB2Dict object.
   */
  public JB2Dict() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query if this is image data.  Note that even though this data effects
   * rendering, the effect is indirect.  This class itself does not produce
   * an image, so the return value is false.
   *
   * @return false
   */
  public boolean isImageData()
  { 
      return false;
  }  

  /**
   * Creates an instance of JB2Dict with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of JB2Dict.
   */
  public static JB2Dict createJB2Dict(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (JB2Dict)create(
      options,
      options.getJB2DictClass(),
      JB2Dict.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public Object clone()
  {
    //verbose("1. JB2Dict clone");
    Cloneable retval = this;

    try
    {
      retval = (JB2Dict)super.clone();

      if(get_inherited_dict() != null)
      {
        ((JB2Dict)retval).set_inherited_dict(
          (JB2Dict)get_inherited_dict().clone(),
          true);
      }

      if(this.shapes != null)
      {
        final Vector shapes = new Vector();
        ((JB2Dict)retval).shapes = shapes;

        for(Enumeration e = this.shapes.elements(); e.hasMoreElements();)
        {
          shapes.addElement(((JB2Shape)e.nextElement()).clone());
        }
      }
    }
    catch(final CloneNotSupportedException ignored) {}

    //verbose("2. JB2Dict clone");
    return retval;
  }

  /**
   * Returns the inherited dictionary.
   *
   * @return DOCUMENT ME!
   */
  public final JB2Dict get_inherited_dict()
  {
    return inherited_dict;
  }

  /**
   * Returns the number of inherited shapes.
   *
   * @return DOCUMENT ME!
   */
  public final int get_inherited_shapes()
  {
    return inherited_shapes;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final int get_shape_count()
  {
    return get_inherited_shapes() + shapes.size();
  }

  /**
   * DOCUMENT ME!
   *
   * @param shape DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IllegalArgumentException DOCUMENT ME!
   */
  public int add_shape(final JB2Shape shape)
  {
    if(shape.parent >= get_shape_count())
    {
      throw new IllegalArgumentException("JB2Image bad parent shape");
    }

    final int retval = get_inherited_shapes() + shapes.size();
    shapes.addElement(shape);

    return retval;
  }

  /**
   * DOCUMENT ME!
   *
   * @param pool DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final void decode(final CachedInputStream pool)
    throws IOException
  {
    decode(
      (CachedInputStream)pool.clone(),
      null);
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
    JB2Dict           zdict)
    throws IOException
  {
    init();

    final JB2Decode codec = JB2Decode.createJB2Decode(this);
    codec.init(gbs, zdict);
    codec.code(this);
  }

  /**
   * DOCUMENT ME!
   *
   * @param shapeno DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IllegalStateException DOCUMENT ME!
   */
  public JB2Shape get_shape(final int shapeno)
  {
    JB2Shape retval;

    if(shapeno >= get_inherited_shapes())
    {
      retval = (JB2Shape)shapes.elementAt(shapeno - get_inherited_shapes());
    }
    else if(get_inherited_dict() != null)
    {
      retval = get_inherited_dict().get_shape(shapeno);
    }
    else
    {
      throw new IllegalStateException("JB2Image bad number");
    }

    return retval;
  }

  /**
   * DOCUMENT ME!
   */
  public void init()
  {
    set_inherited_dict(null);
    shapes.setSize(0);
  }

  /**
   * DOCUMENT ME!
   *
   * @param dict DOCUMENT ME!
   */
  public void set_inherited_dict(final JB2Dict dict)
  {
    set_inherited_dict(dict, false);
  }

  /**
   * Sets the inherited dictionary.
   *
   * @param dict DOCUMENT ME!
   * @param force DOCUMENT ME!
   *
   * @throws IllegalStateException DOCUMENT ME!
   */
  public void set_inherited_dict(
    final JB2Dict dict,
    final boolean force)
  {
    if(dict == null)
    {
      inherited_dict     = null;
      inherited_shapes   = 0;

      return;
    }

    if(!force)
    {
      if(shapes.size() > 0)
      {
        throw new IllegalStateException("JB2Image cannot set");
      }

      if(get_inherited_dict() != null)
      {
        throw new IllegalStateException("JB2Image cannot change");
      }
    }

    inherited_dict     = dict;
    inherited_shapes   = dict.get_shape_count();

//    for (int i=0; i<inherited_shapes; i++)
//    {
//      JB2Shape jshp = dict.get_shape(i);
//      if (jshp.bits != null) jshp.bits.share();
//    }
  }
}
