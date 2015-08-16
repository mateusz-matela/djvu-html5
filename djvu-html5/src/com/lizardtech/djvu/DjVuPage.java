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

import java.beans.*;
import java.io.*;
import java.net.URL;
import java.util.*;


/**
 * <p>
 * DjVuPage decodes single page DjVu files.  This class is appropriate for
 * decoding a single page.  If an INCL chunk is contained in the page, then
 * either decoding must be done from a URL or createDataPool must be
 * overloaded as in the case of the Document class.  The general usage of
 * this class is to create an instance, set any desired values, and then
 * call decode with the relevant data. Decoding may be done either
 * asynchronously or synchronously.  By default decoding is synchronous, but
 * this may be changed by calling setAsync(true) before calling decode. If
 * you will be accessing this class from another thread while decoding is in
 * progress  then you should use asynchronous mode.
 * </p>
 * 
 * <p>
 * In synchronous mode, the decode method will not return until decoding  is
 * complete.  In asynchronous mode decode returns right away as decoding
 * continues in a separate thread.  One method to track asynchronous decode
 * progress is to register a PropertyChangeListener.  Each codec will
 * generate a signal when created.  If you need to wait for a particular
 * codec, rather than polling, use the waitForCodec method.
 * </p>
 * 
 * <p>
 * The updateMap method should be used for progressive rendering. The
 * following example shows how to progressively update an image map in a
 * separate thread.
 * </p>
 * <pre>
 * class Progressive extends Thread
 * {
 *   GMap map = null;
 *   GRect segment;
 *   int subsample;
 *   final DjVuPage page;
 * 
 *   ... define constructors and other methods here ...
 * 
 *   public void run()
 *   { 
 *     boolean repeat;
 *     do
 *     {
 *       repeat=page.isDecoding();
 *       map=page.updateMap(map,segment,subsample); // get the map
 *       // We limit our wait to 200 ms, to query updates for new
 *       // segment or subsample values.  This also limits our wait
 *       // wait time when the page is updated before waitForCodec 
 *       // obtains a lock.
 *       page.waitForCodec(progressLock,200L);
 *     } while(repeat);
 *   }
 * }
 * </pre>
 */
public class DjVuPage
  extends DjVuObject
  implements Runnable
{
  //~ Static fields/initializers ---------------------------------------------

  /** This is the version of DjVu this code was written for. */
  public static final int DJVUVERSION = 20;

  /** This is the oldest version of DjVu we can hope to decode. */
  public static final int DJVUVERSION_TOO_OLD = 15;

  /** This is the newest version of DjVu we should attempt to decode. */
  public static final int DJVUVERSION_TOO_NEW = 22;
  
  /** The thread used for asynchronious decoding. */
  protected static Thread           queueThread = null;
  
  /** An array of vectors listing pages which need decoding. */
  protected static Vector[] queueVector = {null,null};
  protected static int queueCount=0;
  public static int MAX_PRIORITY=1;
  public static int MIN_PRIORITY=0;

  /**
   * This number is incremented for each instance and used for logging.  Note
   * that there is no locking done when incrementing, so this number is not
   * guarenteed to be unique.
   */
  private static int idCount = 1;
  
  static
  {
    queueVector=new Vector[1+MAX_PRIORITY-MIN_PRIORITY];
    for(int i=0;i<queueVector.length;)
    {
      queueVector[i++]=new Vector();
    }
  }

  //~ Instance fields --------------------------------------------------------

  /** Lock used for accessing the annotation codec. */
  public final Object annoLock = new String("anno");

  /** Lock used for accessing the background IWPixmap codec. */
  public final Object bgIWPixmapLock = new String("bgIWPixmap");

  /** Lock to signal decoding is done. */
  public final Object doneLock = new String("done");

  /** Lock used for accessing the foreground IWPixmap codec. */
  public final Object fgIWPixmapLock = new String("fgIWPixmap");

  /** Lock used for accessing the foreground JB2Dict codec. */
  public final Object fgJb2DictLock = new String("fgJb2Dict");

  /** Lock used for accessing the foreground JB2 codec. */
  public final Object fgJb2Lock = new String("fgJb2");

  /** Lock used for accessing the foreground Palette codec. */
  public final Object fgPaletteLock = new String("fgPalette");

  /** Lock used for accessing the info codec. */
  public final Object infoLock = new String("info");

  /** Lock to signal any image update, for progressive rendering. */
  public final Object progressiveLock = new String("progressive");

  /** Lock used for accessing the text codec. */
  public final Object textLock = new String("text");

  /** The mimetype of this document. */
  public String mimetype = null;

  /** The shared dictionary data. */
  protected CachedInputStream sjbzChunk = null;

  /** Number incremented each time the image pixels are updated. */
  protected Number progressiveCount = new Integer(0);

  /** The creation time of this instance.  Used for logging purposes. */
  long startTime = System.currentTimeMillis();

  /** Sets the data pool for this page. */
  private CachedInputStream pool=null;

  // The status string.
  private String status=null;

  /**
   * All the codec are stored in a hash table to make adding new codecs
   * easier.
   */
  private Hashtable codecTable = new Hashtable();

  /**
   * A reference to the foreground pixmap.  This will be regenerated if the
   * if the garbage collector removes it.
   */
  private Object fgPixmapReference = null;

  // Used to propigate change events
  private final PropertyChangeSupport change;

  /** In an exception is thrown in the decoding thread, we save it here. */
  private Throwable caughtException = null;

  /** The URL for this page. */
  protected URL url = null;

  /** True if a separate thread should be used for decoding. */
  private boolean asyncValue = false;

  /** True if decode has been called. */
  private boolean decodeCalled = false;

  /** True until the document has been decoded. */
  private boolean lockWait = true;

  /** A unique number assign from idCount used for logging. */
  private final long id;
  
  private int priority=MAX_PRIORITY;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DjVuPage object.
   */
  public DjVuPage()
  {
    change   = new PropertyChangeSupport(this);
    id       = idCount++;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Set the flag to allow or disallow asynchronous operations.
   *
   * @param value true if asynchronous operations should be used.
   */
  public final synchronized void setAsync(final boolean value)
  {
    asyncValue = value;
  }

  /**
   * Query if the asynchronous flag is set.
   *
   * @return true if the asynchronous operations are set.
   */
  public final boolean isAsync()
  {
    return asyncValue;
  }

  /**
   * Set the decode priority.  Must be called prior to decoding.
   *
   * @param value the decode priority
   */
  public final synchronized void setPriority(final int value)
  {
    this.priority = Math.max(MIN_PRIORITY,Math.min(value,MAX_PRIORITY));
  }

  /**
   * Query if the asynchronous flag is set.  Must be called prior to decoding.
   *
   * @return true if the asynchronous operations are set.
   */
  public final int getPriority()
  {
    return priority;
  }

  /**
   * Get the background pixmap.
   *
   * @param rect area to render
   * @param subsample rate to subsample
   * @param gamma color correction factor
   * @param retval an old pixmap to reuse rather than allocating more memory
   *
   * @return the pixmap of interest
   */
  public GPixmap getBgPixmap(
    GRect   rect,
    int     subsample,
    double  gamma,
    GPixmap retval)
  {
    GPixmap        pm    = null;
    final DjVuInfo info  = getInfo();
    int            width = (info == null)
      ? 0
      : info.width;
    int height = (info == null)
      ? 0
      : info.height;

    if((width <= 0) || (height <= 0) || (info == null))
    {
      return null;
    }

    double gamma_correction = 1.0D;

    if((gamma > 0.0D) && (info != null))
    {
      gamma_correction = gamma / info.gamma;
    }

    if(gamma_correction < 0.10000000000000001D)
    {
      gamma_correction = 0.10000000000000001D;
    }
    else if(gamma_correction > 10D)
    {
      gamma_correction = 10D;
    }

    final IWPixmap bgIWPixmap = getBgIWPixmap();

    if(bgIWPixmap != null)
    {
      final int w = bgIWPixmap.getWidth();
      final int h = bgIWPixmap.getHeight();

      if((w == 0) || (h == 0) || (width == 0) || (height == 0))
      {
        return null;
      }

      final int red = compute_red(width, height, w, h);

      if((red < 1) || (red > 12))
      {
        return null;
      }

      if(subsample == red)
      {
        pm = bgIWPixmap.getPixmap(1, rect, retval);
      }
      else if(subsample == (2 * red))
      {
        pm = bgIWPixmap.getPixmap(2, rect, retval);
      }
      else if(subsample == (4 * red))
      {
        pm = bgIWPixmap.getPixmap(4, rect, retval);
      }
      else if(subsample == (8 * red))
      {
        pm = bgIWPixmap.getPixmap(8, rect, retval);
      }
      else if((red * 4) == (subsample * 3))
      {
        GRect xrect = new GRect();
        xrect.xmin   = (int)Math.floor(rect.xmin*4D/3D);
        xrect.ymin   = (int)Math.floor(rect.ymin*4D/3D);
        xrect.xmax   = (int)Math.ceil((double)rect.xmax*4D/3D);
        xrect.ymax   = (int)Math.ceil((double)rect.ymax*4D/3D);
        final GRect nrect=new GRect(0,0, rect.width(), rect.height());
        if(xrect.xmax > w)
        {
          xrect.xmax = w;
        }

        if(xrect.ymax > h)
        {
          xrect.ymax = h;
        }

        GPixmap ipm = bgIWPixmap.getPixmap(1, xrect, null);
        pm = (retval != null)
          ? retval
          : GPixmap.createGPixmap(this);
        pm.downsample43(ipm, nrect);
      }
      else
      {
        int po2 = 16;

        while((po2 > 1) && (subsample < (po2 * red)))
        {
          po2 >>= 1;
        }

        final int           inw  = ((w + po2) - 1) / po2;
        final int           inh  = ((h + po2) - 1) / po2;
        final int           outw = ((width + subsample) - 1) / subsample;
        final int           outh = ((height + subsample) - 1) / subsample;
        final GPixmapScaler ps   = createGPixmapScaler(inw, inh, outw, outh);
        ps.setHorzRatio(red * po2, subsample);
        ps.setVertRatio(red * po2, subsample);

        final GRect   xrect = ps.getRequiredRect(rect);
        final GPixmap ipm = bgIWPixmap.getPixmap(po2, xrect, null);
        pm = (retval != null)
          ? retval
          : GPixmap.createGPixmap(this);
        ps.scale(xrect, ipm, rect, pm);
      }

      if((pm != null) && (gamma_correction != 1.0D))
      {
        pm.applyGammaCorrection(gamma_correction);

        for(int i = 0; i < 9; i++)
        {
          pm.applyGammaCorrection(gamma_correction);
        }
      }

      if(DjVuOptions.COLLECT_GARBAGE)
      {
        System.gc();
      }

      return pm;
    }
    else
    {
      return null;
    }
  }

  /**
   * Get the foreground bitmap.
   *
   * @param rect area of interest
   * @param subsample rate to subsample
   * @param align number of alignment pixels
   * @param retval an old image to fill rather than creating a new image
   *
   * @return the bitmap of interest
   */
  public final GBitmap getBitmap(
    final GRect   rect,
    final int     subsample,
    final int     align,
    final GBitmap retval)
  {
    return get_bitmap(rect, subsample, align, null);
  }

  /**
   * Query if this is a compound or photo DjVu page.
   *
   * @return true if color.
   */
  public boolean isColor()
  {
    return (is_legal_compound() || !is_legal_bilevel());
  }

  /**
   * Query if the thread is alive and processing.
   *
   * @return true if the thread is running and still processing.
   */
  public final boolean isDecoding()
  {
    return lockWait && decodeCalled;
  }

  /**
   * Query the Anno Codec for this page.
   *
   * @return Annotation for this page.
   */
  public Codec getAnno()
  {
    return getCodec(annoLock);
  }

  /**
   * Query the background IWPixmap codec for this page.
   *
   * @return the background IWPixmap codec for this page.
   */
  public IWPixmap getBgIWPixmap()
  {
    return (IWPixmap)getCodec(bgIWPixmapLock);
  }

  /**
   * Query the named Codec for this page.
   *
   * @param nameLock DOCUMENT ME!
   *
   * @return the named Codec for this page.
   */
  public Codec getCodec(final Object nameLock)
  {
    synchronized(nameLock)
    {
      return (Codec)codecTable.get(nameLock);
    }
  }

  /**
   * Create a new map.  The rectangle of interest and subsample rate are
   * looked up from the old map, and stored in the new map.  A null may be
   * returned if the subsample rate is not legal or there is no valid image.
   *
   * @param segment The bounding rectangle of the subsampled segment.
   * @param subsample The subsample rate.
   * @param retval an old image to fill rather than creating a new image
   *
   * @return The newly created image map.
   */
  public final GMap getMap(
    final GRect segment,
    final int   subsample,
    GMap        retval)
  {
    Number count = progressiveCount;
    retval =
      isColor()
      ? (GMap)getPixmap(
        segment,
        subsample,
        0.0D,
        (retval instanceof GPixmap)
        ? (GPixmap)retval
        : null)
      : (GMap)getBitmap(
        segment,
        subsample,
        1,
        (retval instanceof GBitmap)
        ? (GBitmap)retval
        : null);

    if(retval != null)
    {
      retval.properties.put(progressiveLock, count);
      retval.properties.put(
        "rect",
        segment.clone());
      retval.properties.put(
        "subsample",
        new Integer(subsample));
    }

    return retval;
  }

  /**
   * Query the foreground IWPixmap for this page.
   *
   * @return The foreground IWPixmap for this page.
   */
  public IWPixmap getFgIWPixmap()
  {
    // There is no need to synchronize since we won't access data which could be updated.
    return (IWPixmap)codecTable.get(fgIWPixmapLock);
  }

  /**
   * Query the foreground Jb2 codec for this page.
   *
   * @return the foreground Jb2 codec for this page.
   */
  public JB2Image getFgJb2()
  {
    // There is no need to synchronize since we won't access data which could be updated.
    return (JB2Image)codecTable.get(fgJb2Lock);
  }

  /**
   * Query the foreground Jb2 Dict codec for this page.
   *
   * @return the foreground Jb2 Dict codec for this page.
   */
  public JB2Dict getFgJb2Dict()
  {
    // There is no need to synchronize since we won't access data which could be updated.
    return (JB2Dict)codecTable.get(fgJb2DictLock);
  }

  /**
   * Query the foreground palette codec for this page.
   *
   * @return foreground palette codec for this page.
   */
  public Palette getFgPalette()
  {
    // There is no need to synchronize since we won't access data which could be updated.
    return (Palette)codecTable.get(fgPaletteLock);
  }

  /**
   * Query the foreground pixmap for this page.
   *
   * @return The foreground pixmap for this page.
   */
  public GPixmap getFgPixmap()
  {
    GPixmap fgPixmap = (GPixmap)getFromReference(fgPixmapReference);

    if(fgPixmap == null)
    {
      final IWPixmap fgIWPixmap = getFgIWPixmap();

      if(fgIWPixmap != null)
      {
        synchronized(fgIWPixmapLock)
        {
          fgPixmap = (GPixmap)getFromReference(fgPixmapReference);

          if(fgPixmap == null)
          {
            fgPixmap            = fgIWPixmap.getPixmap();
            fgPixmapReference   = createSoftReference(fgPixmap, fgPixmap);
          }
        }
      }
    }

    return fgPixmap;
  }

  /**
   * Query the DjVuInfo for this page.
   *
   * @return DjVuInfo for this page.
   */
  public DjVuInfo getInfo()
  {
    // There is no need to synchronize since we won't access data which could be updated.
    return (DjVuInfo)codecTable.get(infoLock);
  }

  /**
   * Query the DjVuInfo for this page.
   *
   * @return DjVuInfo for this page.
   */
  public DjVuInfo getInfoWait()
  {
    return (DjVuInfo)waitForCodec(infoLock, 0L);
  }

  /**
   * Query the progressive count.
   *
   * @return the named Codec for this page.
   */
  public int getProgressiveCount()
  {
    return progressiveCount.intValue();
  }

  /**
   * Query the hidden text Codec for this page.
   *
   * @return Hidden text for this page.
   */
  public Codec getText()
  {
    // There is no need to synchronize since we won't access data which could be updated.
    return (Codec)codecTable.get(textLock);
  }

  /**
   * Add a listener for property change events.
   *
   * @param listener to add
   */
  public void addPropertyChangeListener(
    final PropertyChangeListener listener)
  {
    change.addPropertyChangeListener(listener);
  }

  /**
   * Compute the subsample rate used.
   *
   * @param w full size width
   * @param h fill size height
   * @param rw reduced width
   * @param rh reduced height
   *
   * @return subsample rate, or 16
   */
  public static int compute_red(
    int w,
    int h,
    int rw,
    int rh)
  {
    for(int red = 1; red < 16; red++)
    {
      if(((((w + red) - 1) / red) == rw) && ((((h + red) - 1) / red) == rh))
      {
        return red;
      }
    }

    return 16;
  }

  /**
   * Creates an instance of DjVuPage with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of DjVuPage.
   */
  public static DjVuPage createDjVuPage(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (DjVuPage)create(
      options,
      options.getDjVuPageClass(),
      DjVuPage.class);
  }

  /**
   * Called to create an instance of GPixmapScaler
   *
   * @param inw Source image width.
   * @param inh Source image height.
   * @param outw Destination image width.
   * @param outh Destination image height.
   *
   * @return DOCUMENT ME!
   */
  public GPixmapScaler createGPixmapScaler(
    final int inw,
    final int inh,
    final int outw,
    final int outh)
  {
    return new GPixmapScaler(inw, inh, outw, outh);
  }

  /**
   * Decode the specified URL.
   *
   * @param url URL to decode.
   *
   * @throws IOException if an error occures.
   */
  public void decode(final URL url)
    throws IOException
  {
    this.url = url;
    decode(CachedInputStream.createCachedInputStream(this).init(url,false));
  }

  /**
   * Decode the specified CachedInputStream.
   * 
   * @param pool CachedInputStream to decode.
   * 
   * @throws IOException if an error occures.
   * @throws IllegalStateException DOCUMENT ME!
   */
  public void decode(final CachedInputStream pool)
    throws IOException
  {
    synchronized(progressiveLock)
    {
      if(getProgressiveCount() != 0)
      {
        throw new IllegalStateException(
          DjVuPage.class.getName() + " decode already called.");
      }

      progressiveCount   = new Integer(1);
      decodeCalled       = true;
    }

    this.pool = pool;

    if(isAsync())
    {
      synchronized(queueVector)
      {
        queueVector[getPriority()].addElement(createWeakReference(this, this));
        queueCount++;

        if(queueThread == null)
        {
          queueThread = new Thread(createDjVuPage(this));
          queueThread.start();
        }
        else
        {
          queueVector.notifyAll();
        }
      }
    }
    else
    {
      decode();

      if(caughtException instanceof IOException)
      {
        throw (IOException)caughtException;
      }
      else if(caughtException instanceof RuntimeException)
      {
        throw (RuntimeException)caughtException;
      }
    }
  }

  /**
   * Query if the specified codec is available.
   *
   * @param nameLock DOCUMENT ME!
   *
   * @return true if the specified codec is available.
   */
  public boolean hasCodec(final Object nameLock)
  {
    return (codecTable.get(nameLock) instanceof Codec);
  }

  /**
   * Test if this is a photo image.
   *
   * @return true if a photo image
   */
  public boolean is_legal_photo()
  {
    final DjVuInfo info = getInfo();

    if(info == null)
    {
      return false;
    }

    final int width  = info.width;
    final int height = info.height;

    if((width <= 0) || (height <= 0))
    {
      return false;
    }

    return hasCodec(bgIWPixmapLock) && !hasCodec(fgJb2Lock)
    && !hasCodec(fgIWPixmapLock);
  }

  /**
   * Remove a listener for PropertyChangeEvent.
   *
   * @param listener to remove
   */
  public void removePropertyChangeListener(
    final PropertyChangeListener listener)
  {
    change.removePropertyChangeListener(listener);
  }

  /**
   * Update an image map based on an existing one.  If the existing map is
   * null  or anything has been updated, a new image map will be fetched.
   * Otherwise, the existing image will be returned.  A null may be returned
   * if the subsample  rate is not legal, or no pixels are available.
   *
   * @param map The existing map.
   * @param segment The bounding rectangle of the subsampled segment.
   * @param subsample The subsample rate.
   *
   * @return The newly created image map.
   */
  public final GMap updateMap(
    final GMap  map,
    final GRect segment,
    final int   subsample)
  {
    if(map != null)
    {
      final Object subsampleO = map.properties.get("subsample");

      if(
        (subsampleO instanceof Number)
        && (((Number)subsampleO).intValue() == subsample)
        && progressiveCount.equals(map.properties.get(progressiveLock))
        && segment.equals(map.properties.get("rect")))
      {
        return map;
      }
    }

    return getMap(segment, subsample, null);
  }

  /**
   * Get the pixmap for the desired region.
   *
   * @param rect area of interest
   * @param subsample rate to subsample
   * @param gamma color correction factor
   * @param retval an old image to use instead of creating a new one
   *
   * @return the pixmap of interest
   */
  public GPixmap getPixmap(
    final GRect  rect,
    final int    subsample,
    final double gamma,
    GPixmap      retval)
  {
    if(rect.isEmpty())
    {
      return (retval == null)
      ? (GPixmap.createGPixmap(this))
      : retval.init(0, 0, null);
    }

    GPixmap bg=getBgPixmap(rect, subsample, gamma, retval);
    if(hasCodec(fgJb2Lock))
    {
      if(bg == null)
      {
          bg=(retval == null)?GPixmap.createGPixmap(this):retval;
          bg.init(
           rect.height(), 
           rect.width(), GPixel.WHITE);
      }
      if(stencil(bg, rect, subsample, gamma))
      {
        retval=bg;
      }
    }
    else
    {
        retval=bg;
    }
    return retval;
  }

  /**
   * Get a bitmap for the specifed region
   *
   * @param rect area of interest
   * @param subsample subsample rate
   * @param align border alignment
   * @param components a list of components
   *
   * @return the newly created image
   */
  public GBitmap get_bitmap(
    final GRect  rect,
    final int    subsample,
    final int    align,
    final Vector components)
  {
    if(rect.isEmpty())
    {
      return GBitmap.createGBitmap(this);
    }

    final DjVuInfo info = getInfo();

    if(info != null)
    {
      final int      width  = info.width;
      final int      height = info.height;

      final JB2Image fgJb2 = getFgJb2();

      if(
        (width != 0)
        && (height != 0)
        && (fgJb2 != null)
        && (fgJb2.width == width)
        && (fgJb2.height == height))
      {
        return fgJb2.get_bitmap(rect, subsample, align, 0, components);
      }
    }

    return null;
  }

  /**
   * Query if this is a bitonal image.
   *
   * @return true if bitonal
   */
  public boolean is_legal_bilevel()
  {
    final DjVuInfo info = getInfo();

    if(info == null)
    {
      return false;
    }

    final int width  = info.width;
    final int height = info.height;

    if((width <= 0) || (height <= 0))
    {
      return false;
    }

    final JB2Image fgJb2 = getFgJb2();

    if((fgJb2 == null) || (fgJb2.width != width) || (fgJb2.height != height))
    {
      return false;
    }

    return !(hasCodec(bgIWPixmapLock) || hasCodec(fgIWPixmapLock)
    || hasCodec(fgPaletteLock));
  }

  /**
   * Query if this is a compound image.
   *
   * @return true if a compound image
   */
  public boolean is_legal_compound()
  {
    final DjVuInfo info = getInfo();

    if(info == null)
    {
      return false;
    }

    final int width  = info.width;
    final int height = info.height;

    if((width <= 0) || (height <= 0))
    {
      return false;
    }

    final JB2Image fgJb2 = getFgJb2();

    if((fgJb2 == null) || (fgJb2.width != width) || (fgJb2.height != height))
    {
      return false;
    }

    // There is no need to synchronize since we won't access data which could be updated.
    final IWPixmap bgIWPixmap = (IWPixmap)codecTable.get(bgIWPixmapLock);
    int            bgred = 0;

    if(bgIWPixmap != null)
    {
      bgred =
        compute_red(
          width,
          height,
          bgIWPixmap.getWidth(),
          bgIWPixmap.getHeight());
    }

    if((bgred < 1) || (bgred > 12))
    {
      return false;
    }

    int fgred = 0;

    if(hasCodec(fgIWPixmapLock))
    {
      final GPixmap fgPixmap = getFgPixmap();
      fgred = compute_red(
          width,
          height,
          fgPixmap.columns(),
          fgPixmap.rows());
    }

    return ((fgred >= 1) && (fgred <= 12));
  }

  /**
   * This thread processes the asynchronous queue.  Only one page is decoded
   * at a time, with the most recient requested page processed  first.
   */
  public void run()
  {
    final Thread current = Thread.currentThread();
    int priority=MIN_PRIORITY;
//    logError("queue + "+this);
    while(queueThread == current)
    {
      DjVuPage page = null;

      synchronized(queueVector)
      {
        if(queueCount == 0)
        {
          try
          {
            queueVector.wait(5000L);
          }
          catch(final Throwable ignored) {}
          if(queueCount == 0)
          {
            queueThread = null;

            break;
          }
        }
        priority=queueVector.length;
        while(priority>0)
        {
            final Vector queue=queueVector[--priority];
            if(queue.size() > 0)
            {
              final Object ref=queue.lastElement();
              page = (DjVuPage)getFromReference(ref);
              queue.removeElementAt(queue.size() - 1);
              queueCount--;
              if(page != null)
              {
                if(priority < MAX_PRIORITY)
                {
                  final Vector q=queueVector[priority+1];
                  q.addElement(ref);
                  queueCount++;
                  page=null;
                  try { queueVector.wait(200L); } catch(final Throwable ignored) {}
                }
                break;
              }
            }
        }
      }

      if(page != null)
      {
        try
        {
//          logError("queue decode + "+page);
          page.decode();
        }
        catch(final Throwable exp)
        {
          printStackTrace(exp);
        }
      }
    }
//    logError("queue - "+this);
  }

  /**
   * Set the status string and fire a property change event "status".
   *
   * @param status new status string
   */
  public void setStatus(final String status)
  {
    final String s=this.status;
    this.status=status;
    change.firePropertyChange("status", s, status);
  }

  /**
   * Query the status string.
   *
   * @return the status string
   */
  public String getStatus()
  {
      return status;
  }

  /**
   * Create an image by stenciling the foreground onto the background.
   *
   * @param pm the background image to stencil
   * @param rect the area of the page being stenciled
   * @param subsample the subsample rate of the area being stenciled
   * @param gamma the color correction factor
   *
   * @return true if successfull
   */
  public boolean stencil(
    GPixmap pm,
    GRect   rect,
    int     subsample,
    double  gamma)
  {
    final DjVuInfo info = getInfo();

    if(info == null)
    {
      return false;
    }

    final int width  = info.width;
    final int height = info.height;

    if((width <= 0) || (height <= 0))
    {
      return false;
    }

    double gamma_correction = 1.0D;

    if(gamma > 0.0D)
    {
      gamma_correction = gamma / info.gamma;
    }

    if(gamma_correction < 0.10000000000000001D)
    {
      gamma_correction = 0.10000000000000001D;
    }
    else if(gamma_correction > 10D)
    {
      gamma_correction = 10D;
    }

    final JB2Image fgJb2 = getFgJb2();

    if(fgJb2 != null)
    {
      final Palette fgPalette = getFgPalette();

      if(fgPalette != null)
      {
        Vector  components = new Vector();
        GBitmap bm = get_bitmap(rect, subsample, 1, components);

        if(fgJb2.get_blit_count() != fgPalette.colordata.length)
        {
          pm.attenuate(bm, 0, 0);

          return false;
        }

        GPixmap colors =
          GPixmap.createGPixmap(this).init(
            1,
            fgPalette.size(),
            null);
        final GPixelReference color = colors.createGPixelReference(0);

        for(int i = 0; i < colors.columns(); color.incOffset())
        {
          fgPalette.index_to_color(i++, color);
        }

        colors.applyGammaCorrection(gamma_correction);

        Vector compset = new Vector();

        while(components.size() > 0)
        {
          int       lastx      = 0;
          final int colorindex =
            fgPalette.colordata[((Number)components.elementAt(0)).intValue()];
          GRect     comprect = new GRect();
          compset.setSize(0);

          for(int pos = 0; pos < components.size();)
          {
            final int     blitno =
              ((Number)components.elementAt(pos)).intValue();
            final JB2Blit pblit = fgJb2.get_blit(blitno);

            if(pblit.left < lastx)
            {
              break;
            }

            lastx = pblit.left;

            if(fgPalette.colordata[blitno] == colorindex)
            {
              final JB2Shape pshape = fgJb2.get_shape(pblit.shapeno);
              final GRect    xrect =
                new GRect(
                  pblit.left,
                  pblit.bottom,
                  pshape.getGBitmap().columns(),
                  pshape.getGBitmap().rows());
              comprect.recthull(comprect, xrect);
              compset.addElement(components.elementAt(pos));
              components.removeElementAt(pos);
            }
            else
            {
              pos++;
            }
          }

          comprect.xmin /= subsample;
          comprect.ymin /= subsample;
          comprect.xmax   = ((comprect.xmax + subsample) - 1) / subsample;
          comprect.ymax   = ((comprect.ymax + subsample) - 1) / subsample;
          comprect.intersect(comprect, rect);

          if(comprect.isEmpty())
          {
            continue;
          }

          //        bm   = getBitmap(comprect, subsample, 1);
          bm = GBitmap.createGBitmap(this);
          bm.init(
            comprect.height(),
            comprect.width(),
            0);
          bm.setGrays(1 + (subsample * subsample));

          final int rxmin = comprect.xmin * subsample;
          final int rymin = comprect.ymin * subsample;

          for(int pos = 0; pos < compset.size(); ++pos)
          {
            final int      blitno =
              ((Number)compset.elementAt(pos)).intValue();
            final JB2Blit  pblit  = fgJb2.get_blit(blitno);
            final JB2Shape pshape = fgJb2.get_shape(pblit.shapeno);
            bm.blit(
              pshape.getGBitmap(),
              pblit.left - rxmin,
              pblit.bottom - rymin,
              subsample);
          }

          color.setOffset(colorindex);
          pm.blit(
            bm,
            comprect.xmin - rect.xmin,
            comprect.ymin - rect.ymin,
            color);
        }

        return true;
      }

      // Three layer model.
      final IWPixmap fgIWPixmap = getFgIWPixmap();

      if(fgIWPixmap != null)
      {
        GBitmap bm = getBitmap(rect, subsample, 1, null);

        if((bm != null) && (pm != null))
        {
          final GPixmap fgPixmap = getFgPixmap();
          int           w   = fgPixmap.columns();
          int           h   = fgPixmap.rows();
          int           red = compute_red(width, height, w, h);

//          if((red < 1) || (red > 12))
          if((red < 1) || (red > 16))
          {
            return false;
          }
//
//          int supersample = (red <= subsample)
//            ? 1
//            : (red / subsample);
//          int wantedred = supersample * subsample;
//
//          if(red == wantedred)
//          {
//            pm.stencil(bm, fgPixmap, supersample, rect, gamma_correction);
//
//            return 1;
//          }
          pm.stencil(bm, fgPixmap, red, subsample, rect, gamma_correction);
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Query the named Codec for this page.  If the Codec is not available, and
   * decoding is in progress then wait for it.
   *
   * @param nameLock the name lock of the codec to wait for
   * @param maxTime the maximum amount of time to wait for the specified
   *        codec
   *
   * @return the named Codec for this page.
   */
  public Codec waitForCodec(
    final Object nameLock,
    final long   maxTime)
  {
    boolean active;
    Codec   retval;

//    logError("wait + "+this+" "+nameLock);
    do
    {
      synchronized(nameLock)
      {
        active   = isDecoding();
        retval   = (Codec)codecTable.get(nameLock);

        if(active && (retval == null))
        {
          active = isDecoding();

          if(active)
          {
            // We wait a maximum of 5 seconds instead of forever to avoid deadlocks...
            try
            {
              nameLock.wait((maxTime <= 0L)
                ? 5000L
                : maxTime);
            }
            catch(final Throwable ignored) {}

            retval = (Codec)codecTable.get(nameLock);
          }
        }
      }
    }
    while(
      active
      && (retval == null)
      && (maxTime <= 0L)
      && (nameLock != progressiveLock));

//    logError("wait - "+this+" "+nameLock);
    return retval;
  }

  /**
   * Called after processing each chunk to log progress and optionally run
   * the garbage collector.
   *
   * @param chkid name of chunk processed
   */
  protected void clean(final String chkid)
  {
    final Runtime run  = Runtime.getRuntime();
    final long    used = run.totalMemory() - run.freeMemory();
    final long    t    = System.currentTimeMillis() - startTime;
    final String  d    = "000" + t;
    verbose(
      id + ". chkid=" + chkid + ",memory=" + used + " time=" + (t / 1000L)
      + "." + d.substring(d.length() - 3));

    if(DjVuOptions.COLLECT_GARBAGE)
    {
      System.gc();
    }
  }

  /**
   * Called to decode a chunk.
   *
   * @param iff stream being processed
   * @param isInclude true if this is an include file stream
   *
   * @throws IOException if an error occurs
   * @throws IllegalStateException if an error occurs
   */
  protected void decodeChunk(
    final CachedInputStream iff,
    final boolean        isInclude)
    throws IOException
  {
    final String chkid=iff.getName();
    try
    {
      if(chkid.equals("Djbz"))
      {
        addCodecChunk(
          fgJb2DictLock,
          JB2Dict.createJB2Dict(this),
          iff);

        if(sjbzChunk != null)
        {
          parseSjbz((CachedInputStream)sjbzChunk.clone());
        }
      }
      else if(chkid.equals("ANTa")||chkid.equals("ANTz"))
      {
        synchronized(annoLock)
        {
          Codec anno = getCodec(annoLock);

          if(anno == null)
          {
            anno = getDjVuOptions().createDjVuAnno();
          }

          addCodecChunk(annoLock, anno, iff);
        }
      }
      else if(!isInclude)
      {
        if(chkid.equals("INFO"))
        {
          throw new IllegalStateException(
            "DjVu Decoder: Corrupted file (Duplicate INFO chunk)");
        }
        else if(chkid.equals("INCL"))
        {
//          ByteArrayOutputStream bout   = new ByteArrayOutputStream();
//          byte[]                buffer = new byte[1024];
//
//          for(int i; (i = iff.read(buffer)) > 0;)
//          {
//            bout.write(buffer, 0, i);
//          }
//
//         decodeInclude(createCachedInputStream(new String(bout.toByteArray())));
         decodeInclude(createCachedInputStream(iff.readFullyUTF()));
        }
        else if(chkid.equals("FGbz"))
        {
          if(hasCodec(fgIWPixmapLock))
          {
            throw new IllegalStateException("Duplicate foreground");
          }

          addCodecChunk(
            fgPaletteLock,
            Palette.createPalette(this),
            iff);
        }
        else if(chkid.equals("TXTa")||chkid.equals("TXTz"))
        {
          addCodecChunk(
            textLock,
            getDjVuOptions().createDjVuText(),
            iff);
        }
        else if(chkid.equals("Sjbz"))
        {
          if(this.sjbzChunk != null)
          {
            throw new IllegalStateException("Duplicate Sjbz chunk");
          }

//          final CachedInputStream sjbzChunk = iff.getCachedInputStream();

          try
          {
            iff.mark(Integer.MAX_VALUE);
            parseSjbz(iff);
          }
          catch(IllegalStateException exp)
          {
//            if(hasCodec(fgJb2DictLock) || (sjbzChunk == null))
            if(hasCodec(fgJb2DictLock))
            {
              throw exp;
            }
            iff.reset();
            this.sjbzChunk = iff;
          }
        }
        else if(chkid.equals("BG44"))
        {
//bcr          if(getInfo().width > 600){verbose("bcr extra wait");try {Thread.sleep(5000L);}catch(final Throwable ignored) {}}
          synchronized(bgIWPixmapLock)
          {
            Codec bgIWPixmap = getCodec(bgIWPixmapLock);

            if(bgIWPixmap == null)
            {
              bgIWPixmap = IWPixmap.createIWPixmap(this);
            }

            addCodecChunk(bgIWPixmapLock, bgIWPixmap, iff);
          }
        }
        else if(chkid.equals("FG44"))
        {
          if(hasCodec(fgPaletteLock))
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Duplicate foreground layer)");
          }

          addCodecChunk(
            fgIWPixmapLock,
            IWPixmap.createIWPixmap(this),
            iff);
        }
        else if(chkid.equals("BG2k"))
        {
          if(hasCodec(bgIWPixmapLock))
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Duplicate background layer)");
          }
        }
        else if(chkid.equals("FG2k"))
        {
          if(hasCodec(fgIWPixmapLock) || hasCodec(fgPaletteLock))
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Duplicate foreground layer)");
          }
        }
        else if(chkid.equals("Smmr"))
        {
          if(hasCodec(fgJb2Lock))
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Duplicate background layer)");
          }
        }
        else if(chkid.equals("BGjp"))
        {
          if(hasCodec(bgIWPixmapLock))
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Duplicate background layer)");
          }
        }
        else if(chkid.equals("FGjp") && hasCodec(fgIWPixmapLock))
        {
          throw new IllegalStateException(
            "DjVu Decoder: Corrupted data (Duplicate foreground layer)");
        }
      }
    }
    finally
    {
//      iff.chunkClose();
      clean(chkid);
    }
  }

  /**
   * Called to decode chunks.
   *
   * @param iff an Enumeration of CachedInputStream's to decode
   * @param isInclude true if this is an include file stream
   *
   * @throws IOException if an error occurs
   */
  protected void decodeChunks(
    final Enumeration iff,
    final boolean     isInclude)
    throws IOException
  {
    if((iff == null)||!iff.hasMoreElements())
    {
      return;
    }

    if(!hasCodec(infoLock))
    {

      final CachedInputStream chunk=(CachedInputStream)iff.nextElement();
      if(!"INFO".equals(chunk.getName()))
      {
        throw new IOException(
          "DjVuDecoder:: Corrupted file (Does not start with INFO chunk)");
      }

      addCodecChunk(
        infoLock,
        DjVuInfo.createDjVuInfo(this),
        chunk);
      clean(chunk.getName());
    }
    while(iff.hasMoreElements())
    {
      decodeChunk(
        (CachedInputStream)iff.nextElement(),
        isInclude);
    }
  }

  /**
   * Called to decode an include chunk.
   *
   * @param pool chunk to be read
   *
   * @throws IOException if an error occurs
   * @throws IllegalStateException if an error occurs
   */
  protected void decodeInclude(CachedInputStream pool)
    throws IOException
  {
    final Enumeration iff = pool.getIFFChunks();
      if((iff == null)||!iff.hasMoreElements())
      {
        throw new IOException("EOF");
      }
      final CachedInputStream formStream=(CachedInputStream)iff.nextElement();
      final Enumeration formIff=formStream.getIFFChunks();
      if((formIff != null)&&"FORM:DJVI".equals(formStream.getName()))
      {
        decodeChunks(formIff, true);
      }
      else
      {
        throw new IllegalStateException(
          "DejaVu decoder: a DJVI include was expected");
      }
  }

  /**
   * Called to parse jb2 data.
   *
   * @param input stream to parse
   *
   * @throws IOException if an error occurs
   * @throws IllegalStateException if an error occurs
   */
  protected void parseSjbz(final InputStream input)
    throws IOException
  {
    sjbzChunk = null;

    final JB2Image fgJb2 = JB2Image.createJB2Image(this);

    final DjVuInfo info = getInfo();

    if((info != null) && (info.version < 19))
    {
      fgJb2.reproduce_old_bug = true;
    }

    fgJb2.decode(
      input,
      getFgJb2Dict());

    if(setCodec(fgJb2Lock, fgJb2) != null)
    {
      throw new IllegalStateException(
        "DjVu Decoder: Corrupted data (Duplicate FGxx chunk)");
    }
  }

  /**
   * Called to create a CachedInputStream of the given id.
   * 
   * @param id name of the CachedInputStream to create
   * 
   * @return the newly created CachedInputStream
   * 
   * @throws IOException if an error occurs
   */
  CachedInputStream createCachedInputStream(final String id)
    throws IOException
  {
    return CachedInputStream.createCachedInputStream(this).init(new URL(url, id), false);
  }

  /**
   * Set the named Codec for this page.
   *
   * @param nameLock Name of the codec to set.
   * @param codec Named codec for for this page.
   *
   * @return Old codec of this name for this page.
   */
  private Codec setCodec(
    final Object nameLock,
    final Codec  codec)
  {
    Codec        retval;
    final String name = (String)nameLock;

    synchronized(nameLock)
    {
      retval =
        (Codec)((codec == null)
        ? codecTable.remove(nameLock)
        : codecTable.put(nameLock, codec));
      nameLock.notifyAll();

      if(retval != codec)
      {
        change.firePropertyChange(name, retval, codec);
      }
      else if(nameLock.equals(annoLock))
      {
        change.firePropertyChange((String)annoLock, null, codec);
      }
    }

    if((codec != null)&&codec.isImageData())
    {
      synchronized(progressiveLock)
      {
        final Number count = progressiveCount;
        progressiveCount = new Integer(count.intValue() + 1);
        progressiveLock.notifyAll();
        change.firePropertyChange(
          (String)progressiveLock,
          count,
          progressiveCount);
      }
    }
    return retval;
  }

  // Decode the specified codec from the datapool.
  private void addCodecChunk(
    final Object   nameLock,
    final Codec    codec,
    final CachedInputStream pool)
    throws IOException
  {
    if(codec != null)
    {
      codec.decode(pool);

      final Codec old = setCodec(nameLock, codec);

      if((old != null) && (old != codec))
      {
        throw new IllegalStateException("Duplicate " + nameLock);
      }
    }
  }

  /**
   * Decode the document from the already opened datapool.  Normally this
   * method is called the queue.
   *
   * @throws IOException if an error occurs
   * @throws IllegalStateException if an error occurs
   */
  private void decode()
  {
    try
    {
      startTime = System.currentTimeMillis();
      final URL url=this.url;
      if(url != null)
      {
        setStatus("decoding "+url); 
      }
      final CachedInputStream pool = this.pool;
      this.pool = null;

      final Enumeration iff = pool.getIFFChunks();
      try
      {
        if((iff == null)||!iff.hasMoreElements())
        {
          throw new IOException("EOF");
        }
        CachedInputStream formStream=(CachedInputStream)iff.nextElement();
        if("FORM:DJVU".equals(formStream.getName()))
        {
          mimetype = "image/djvu";
          decodeChunks(formStream.getIFFChunks(), false);
          if(sjbzChunk != null)
          {
            parseSjbz((CachedInputStream)sjbzChunk.clone());
            clean("Sjbz");
          }
          final IWPixmap bgIWPixmap = getBgIWPixmap();
          if(bgIWPixmap != null)
          {
            bgIWPixmap.close_codec();
          }
          final DjVuInfo info = getInfo();
          if(info == null)
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Missing INFO chunk)");
          }
        }
        else if("FORM:PM44".equals(formStream.getName())
          || "FORM:BM44".equals(formStream.getName()))
        {
          mimetype = "image/iw44";
          IWPixmap img44 = null;
          final Enumeration formIff=formStream.getIFFChunks();
          while((formIff != null)&&formIff.hasMoreElements())
          {
            CachedInputStream chunk=(CachedInputStream)formIff.nextElement();
            if("PM44".equals(chunk.getName()) || "BM44".equals(chunk.getName()))
            {
              if(img44 != null)
              {
                img44 = IWPixmap.createIWPixmap(this);
                img44.decode(chunk);
                final DjVuInfo info = DjVuInfo.createDjVuInfo(this);
                info.width    = img44.getWidth();
                info.height   = img44.getHeight();
                info.dpi      = 100;
                setCodec(infoLock, info);
                setCodec(bgIWPixmapLock, img44);
              }
              else
              {
                img44.decode(chunk);
              }
            }
            else if("ANTa".equals(chunk.getName())||"ANTz".equals(chunk.getName()))
            {
              synchronized(annoLock)
              {
                Codec anno = getCodec(annoLock);

                if(anno == null)
                {
                  anno = getDjVuOptions().createDjVuAnno();
                }
                addCodecChunk(annoLock, anno, chunk);
              }
            }
          }

          if(!hasCodec(infoLock))
          {
            throw new IllegalStateException(
              "DjVu Decoder: Corrupted data (Missing IW44 data chunks)");
          }
        }
        else
        {
          throw new IllegalStateException(
            "DejaVu decoder: a DJVU or IW44 image was expected");
        }
      }
      finally
      {
        if(url != null)
        {
          setStatus("decoded "+url); 
        }
      }
    }
    catch(final Throwable exp)
    {
      caughtException = exp;
      printStackTrace(exp);
    }
    lockWait = false;

    try
    {
//        logError("0. finish "+this);
      final Object[] lockArray =
      {
        infoLock, fgIWPixmapLock, annoLock, textLock, bgIWPixmapLock,
        fgJb2Lock, fgPaletteLock, fgJb2DictLock, progressiveLock, doneLock
      };

      for(int i = 0; i < lockArray.length; i++)
      {
//          logError(i+". finish "+this);
        synchronized(lockArray[i])
        {
          lockArray[i].notifyAll();
        }
      }
//        logError("x. finish "+this);
      change.firePropertyChange(
        doneLock.toString(),
        null,
        this);

//        logError("xx. finish "+this);
    }
    catch(final Throwable exp)
    {
      printStackTrace(exp);
    }
  }
}
