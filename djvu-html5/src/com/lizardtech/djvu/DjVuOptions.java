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

import java.lang.reflect.*;
import java.io.PrintStream;


/**
 * This is a factory which stores standard encoding options.  Each class
 * implementing DjVuInterface or dirived from DjVuObject should be added
 * here.  When a DjVuInterface class creates a new Object, it will use this
 * factory.  That way even the core decoding classes may be overloaded
 * simply by setting the DjVuOptions of the top level class. Use of this
 * factor also greatly speeds up the initialization of Microsoft's
 * implementation of Java 1.1.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.19 $
 */
public class DjVuOptions
{
  //~ Static fields/initializers ---------------------------------------------

  /** The build version of this code. */
  public static final String VERSION="0_8_09";
  
  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_BUCKET_UPPER = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_EXPECT = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_ACTIVE = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_BUCKET_ACTIVE = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean BEZIERGAMMA = false;

  /**
   * True if garbage collection should be invoked manually on a regular
   * basis.  This greatly slows down the code, but it may be neccessary
   * for a low memory device.
   */
  public static boolean COLLECT_GARBAGE = false;
  
  public static PrintStream out=System.out;
  public static PrintStream err=System.err;

  //~ Instance fields --------------------------------------------------------

  private Class classAnnoLine        = null;
  private Class classAnnoOval        = null;
  private Class classAnnoPoly        = null;
  private Class classAnnoText        = null;
  private Class classAnnoRect        = null;
  private Class classBSInputStream   = null;
  private Class classBookmark        = null;
  private Class classCachedInputSteam= null;
  private Class classDataPool        = null;
  private Class classDjVmDir         = null;
  private Class classDjVuAnno        = null;
  private Class classDjVuInfo        = null;
  private Class classDjVuPage        = null;
  private Class classDjVuText        = null;
  private Class classDocument        = null;
  private Class classGBitmap         = null;
  private Class classGPixmap         = null;
  private Class classIFFEnumeration  = null;
  private Class classIWBitmap        = null;
  private Class classIWBlock         = null;
  private Class classIWCodec         = null;
  private Class classIWMap           = null;
  private Class classIWPixmap        = null;
  private Class classJB2Decode       = null;
  private Class classJB2Dict         = null;
  private Class classJB2Image        = null;
  private Class classJB2Shape        = null;
  private Class classPalette         = null;
  private Class classZPCodec         = null;

  //~ Methods ----------------------------------------------------------------

  /**
   * Set the class object to use for BSInputStream.
   * 
   * @param c the class object.
   */
  public void setBSInputStreamClass(final Class c)
  {
    this.classBSInputStream = c;
  }

  /**
   * Get the class object to use for BSInputStream.
   *
   * @return BSInputStream.class or child
   */
  public Class getBSInputStreamClass()
  {
    return classBSInputStream;
  }

  /**
   * Set the class object to use for Bookmark.
   * 
   * @param c the class object.
   */
  public void setBookmarkClass(final Class c)
  {
    this.classBookmark = c;
  }

  /**
   * Get the class object to use for Bookmark.
   *
   * @return Bookmark.class or child
   */
  public Class getBookmarkClass()
  {
    return classBookmark;
  }

  /**
   * Set the class object to use for DataPool.
   * 
   * @param c the class object.
   */
  public void setCachedInputStreamClass(final Class c)
  {
    this.classCachedInputSteam = c;
  }

  /**
   * Get the class object to use for DataPool.
   *
   * @return DataPool.class or child
   */
  public Class getCachedInputStreamClass()
  {
    return classCachedInputSteam;
  }

  /**
   * Set the class object to use for DataPool.
   * 
   * @param c the class object.
   */
  public void setDataPoolClass(final Class c)
  {
    this.classDataPool = c;
  }

  /**
   * Get the class object to use for DataPool.
   *
   * @return DataPool.class or child
   */
  public Class getDataPoolClass()
  {
    return classDataPool;
  }

  /**
   * Set the class object to use for DjVmDir.
   * 
   * @param c the class object.
   */
  public void setDjVmDirClass(final Class c)
  {
    this.classDjVmDir = c;
  }

  /**
   * Get the class object to use for DjVmDir.
   *
   * @return DjVmDir.class or child
   */
  public Class getDjVmDirClass()
  {
    return classDjVmDir;
  }

  /**
   * Set the class object to use for DjVuAnno.
   * 
   * @param c the class object.
   */
  public void setDjVuAnnoClass(final Class c)
  {
    this.classDjVuAnno = c;
  }

  /**
   * Get the class object to use for DjVuAnno.
   *
   * @return DjVuAnno.class or child
   */
  public Class getDjVuAnnoClass()
  {
    return classDjVuAnno;
  }

  /**
   * Set the class object to use for DjVuInfo.
   * 
   * @param c the class object.
   */
  public void setDjVuInfoClass(final Class c)
  {
    this.classDjVuInfo = c;
  }

  /**
   * Get the class object to use for DjVuInfo.
   *
   * @return DjVuInfo or child
   */
  public Class getDjVuInfoClass()
  {
    return classDjVuInfo;
  }

  /**
   * Set the class object to use for DjVuPage.
   * 
   * @param c the class object.
   */
  public void setDjVuPageClass(final Class c)
  {
    this.classDjVuPage = c;
  }

  /**
   * Get the class object to use for DjVuPage.
   *
   * @return DjVuPage.class or child
   */
  public Class getDjVuPageClass()
  {
    return classDjVuPage;
  }

  /**
   * Set the class object to use for DjVuText.
   * 
   * @param c the class object.
   */
  public void setDjVuTextClass(final Class c)
  {
    this.classDjVuText = c;
  }

  /**
   * Get the class object to use for DjVuText.
   *
   * @return DjVuText.class or child
   */
  public Class getDjVuTextClass()
  {
    return classDjVuText;
  }

  /**
   * Set the class object to use for Document
   * 
   * @param c the class object.
   */
  public void setDocumentClass(final Class c)
  {
    this.classDocument = c;
  }

  /**
   * Get the class object to use for Document
   *
   * @return Document.class or child
   */
  public Class getDocumentClass()
  {
    return classDocument;
  }

  /**
   * Set the class object to use for GBitmap.
   * 
   * @param c the class object.
   */
  public void setGBitmapClass(final Class c)
  {
    this.classGBitmap = c;
  }

  /**
   * Get the class object to use for GBitmap.
   *
   * @return GBitmap.class or child
   */
  public Class getGBitmapClass()
  {
    return classGBitmap;
  }

  /**
   * Set the class object to use for AnnoLine.
   * 
   * @param c the class object.
   */
  public void setAnnoLineClass(final Class c)
  {
    this.classAnnoLine = c;
  }

  /**
   * Get the class object to use for AnnoLine.
   *
   * @return AnnoLine.class or child
   */
  public Class getAnnoLineClass()
  {
    return classAnnoLine;
  }

  /**
   * Set the class object to use for AnnoOval.
   * 
   * @param c the class object.
   */
  public void setAnnoOvalClass(final Class c)
  {
    this.classAnnoOval = c;
  }

  /**
   * Get the class object to use for AnnoOval.
   *
   * @return AnnoOval.class or child
   */
  public Class getAnnoOvalClass()
  {
    return classAnnoOval;
  }

  /**
   * Set the class object to use for Poly.
   * 
   * @param c the class object.
   */
  public void setAnnoPolyClass(final Class c)
  {
    this.classAnnoPoly = c;
  }

  /**
   * Get the class object to use for Poly.
   *
   * @return Poly.class or child
   */
  public Class getAnnoPolyClass()
  {
    return classAnnoPoly;
  }

  /**
   * Set the class object to use for anno.Rect.
   * 
   * @param c the class object.
   */
  public void setAnnoRectClass(final Class c)
  {
    this.classAnnoRect = c;
  }

  /**
   * Get the class object to use for AnnoRect.
   *
   * @return anno.Rect.class or child
   */
  public Class getAnnoRectClass()
  {
    return classAnnoRect;
  }

  /**
   * Set the class object to use for anno.Text.
   * 
   * @param c the class object.
   */
  public void setAnnoTextClass(final Class c)
  {
    this.classAnnoText = c;
  }

  /**
   * Get the class object to use for anno.Text.
   *
   * @return anno.Text.class or child
   */
  public Class getAnnoTextClass()
  {
    return classAnnoText;
  }

  /**
   * Set the class object to use for GPixmap.
   * 
   * @param c the class object.
   */
  public void setGPixmapClass(final Class c)
  {
    this.classGPixmap = c;
  }

  /**
   * Get the class object to use for GPixmap.
   *
   * @return GPixmap.class or child
   */
  public Class getGPixmapClass()
  {
    return classGPixmap;
  }

  /**
   * Set the class object to use for IFFEnumeration.
   * 
   * @param c the class object.
   */
  public void setIFFEnumerationClass(final Class c)
  {
    this.classIFFEnumeration = c;
  }

  /**
   * Get the class object to use for IFFEnumeration.
   *
   * @return IFFEnumeration.class or child
   */
  public Class getIFFEnumerationClass()
  {
    return classIFFEnumeration;
  }

  /**
   * Set the class object to use for IWBitmap.
   * 
   * @param c the class object.
   */
  public void setIWBitmapClass(final Class c)
  {
    this.classIWBitmap = c;
  }

  /**
   * Get the class object to use for IWBitmap.
   *
   * @return IWBitmap.class or child
   */
  public Class getIWBitmapClass()
  {
    return classIWBitmap;
  }

  /**
   * Set the class object to use for IWBlock.
   * 
   * @param c the class object.
   */
  public void setIWBlockClass(final Class c)
  {
    this.classIWBlock = c;
  }

  /**
   * Get the class object to use for IWBlock.
   *
   * @return IWBlock.class or child
   */
  public Class getIWBlockClass()
  {
    return classIWBlock;
  }

  /**
   * Set the class object to use for IWCodec.
   * 
   * @param c the class object.
   */
  public void setIWCodecClass(final Class c)
  {
    this.classIWCodec = c;
  }

  /**
   * Get the class object to use for IWCodec.
   *
   * @return IWCodec.class or child
   */
  public Class getIWCodecClass()
  {
    return classIWCodec;
  }

  /**
   * Set the class object to use for IWMap.
   * 
   * @param c the class object.
   */
  public void setIWMapClass(final Class c)
  {
    this.classIWMap = c;
  }

  /**
   * Get the class object to use for IWMap.
   *
   * @return IWMap.class or child
   */
  public Class getIWMapClass()
  {
    return classIWMap;
  }

  /**
   * Set the class object to use for IWPixmap.
   * 
   * @param c the class object.
   */
  public void setIWPixmapClass(final Class c)
  {
    this.classIWPixmap = c;
  }

  /**
   * Get the class object to use for IWPixmap.
   *
   * @return IWPixmap.class or child
   */
  public Class getIWPixmapClass()
  {
    return classIWPixmap;
  }

  /**
   * Set the class object to use for JB2Decode.
   *
   * @param classJB2Decode the class object.
   */
  public void setJB2DecodeClass(final Class classJB2Decode)
  {
    this.classJB2Decode = classJB2Decode;
  }

  /**
   * Get the class object to use for JB2Decode.
   *
   * @return The new instance of the JB2Decode class.
   */
  public Class getJB2DecodeClass()
  {
    return classJB2Decode;
  }

  /**
   * Set the class object to use for JB2Dict.
   * 
   * @param c the class object.
   */
  public void setJB2DictClass(final Class c)
  {
    this.classJB2Dict = c;
  }

  /**
   * Get the class object to use for JB2Dict.
   *
   * @return The new instance of the JB2Dict class.
   */
  public Class getJB2DictClass()
  {
    return classJB2Dict;
  }

  /**
   * Set the class object to use for JB2Image.
   * 
   * @param c the class object.
   */
  public void setJB2ImageClass(final Class c)
  {
    this.classJB2Image = c;
  }

  /**
   * Get the class object to use for JB2Image.
   *
   * @return The new instance of the JB2Image class.
   */
  public Class getJB2ImageClass()
  {
    return classJB2Image;
  }

  /**
   * Set the class object to use for JB2Shape.
   * 
   * @param c the class object.
   */
  public void setJB2ShapeClass(final Class c)
  {
    this.classJB2Shape = c;
  }

  /**
   * Get the class object to use for JB2Shape.
   *
   * @return The new instance of the JB2Shape class.
   */
  public Class getJB2ShapeClass()
  {
    return classJB2Shape;
  }

  /**
   * Set the class object to use for Palette.
   * 
   * @param c the class object.
   */
  public void setPaletteClass(final Class c)
  {
    this.classPalette = c;
  }

  /**
   * Get the class object to use for Palette.
   *
   * @return The new instance of the Palette class.
   */
  public Class getPaletteClass()
  {
    return classPalette;
  }

  /**
   * Set the class object to use for ZPCodec.
   * 
   * @param c the class object.
   */
  public void setZPCodecClass(final Class c)
  {
    this.classZPCodec = c;
  }

  /**
   * Get the class object to use for ZPCodec.
   *
   * @return The new instance of the ZPCodec class.
   */
  public Class getZPCodecClass()
  {
    return classZPCodec;
  }

  /**
   * Create the an instance of the Bookmark class without loading the class
   * until runtime.
   *
   * @return The new instance of the Bookmark class.
   */
  public Codec createBookmark()
  {
    return (Codec)DjVuObject.create(
      this,
      getBookmarkClass(),
      "com.lizardtech.djvu.outline.Bookmark");
  }

  /**
   * Create the an instance of the DjVuAnno class without loading the class
   * until runtime.
   *
   * @return The new instance of the DjVuAnno class.
   */
  public Codec createDjVuAnno()
  {
    return (Codec)DjVuObject.create(
      this,
      getDjVuAnnoClass(),
      "com.lizardtech.djvu.anno.DjVuAnno");
  }

  /**
   * Create the an instance of the DjVuText class without loading the class
   * until runtime.
   *
   * @return The new instance of the DjVuText class.
   */
  public Codec createDjVuText()
  {
    return (Codec)DjVuObject.create(
      this,
      getDjVuTextClass(),
      "com.lizardtech.djvu.text.DjVuText");
  }
}
