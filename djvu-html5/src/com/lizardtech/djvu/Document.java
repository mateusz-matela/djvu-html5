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
 * This class represents indirect, bundled, and single page DjVu documents.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.23 $
 */
public class Document
  extends DjVuObject
  implements Cloneable, Runnable
{
  //~ Static fields/initializers ---------------------------------------------

  /** These are the magic numbers at the start of every DjVu file. */
  public static final byte[] octets = {0x41, 0x54, 0x26, 0x54};
  public static int MAX_PRIORITY=1;
  public static int MIN_PRIORITY=0;

  //~ Instance fields --------------------------------------------------------

  /** Used to keep track of the prefetchThread. */
  public Thread prefetchThread=null;
  
  private static Vector[] prefetchVector = {new Vector(),new Vector()};
  private static int prefetchCount=0;

  
  // Used to propigate change events
  private final PropertyChangeSupport change;
  
  // The status string.
  private String status=null;
  
  /** A map of saved pages for this document. */
  protected Hashtable cachedInputStreamMap = new Hashtable();

  /** A Vector of soft links to decoded pages. */
  protected Hashtable pageMap = new Hashtable();
  
  // The bookmark Codec for this document.
  private Codec    bookmark = null;
  
  // The data source for this document
  private CachedInputStream pool     = null;

  // The file index for this document
  private DjVmDir dir        = null;
  
  // A reference to a parent object used in the prefetching thread
  private Object  parentRef  = null;
  
  // true if we should use asynchronous decoding
  private boolean asyncValue = false;
  
  

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Document object.
   */
  public Document()
  {
    dir = DjVmDir.createDjVmDir(this);
    change=new PropertyChangeSupport(this);
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
   * Creates a new Document object.
   *
   * @param url DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public Document(final URL url)
    throws IOException
  {
    this();
    init(url);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of Document with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of Document.
   */
  public static Document createDocument(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();
    return (Document)create(
      options,
      options.getDocumentClass(),
      Document.class);
  }

  /**
   * Set the flag to allow or disallow asynchronous operations.
   *
   * @param value true if asynchronous operations should be used.
   */
  public final void setAsync(final boolean value)
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
   * Query the bookmark codec for this document
   *
   * @return the bookmark codec or null
   */
  public Codec getBookmark()
  {
    Codec retval = bookmark;

    if(retval == null)
    {
      bookmark = retval = getDjVuOptions().createBookmark();
    }

    return retval;
  }

  /**
   * Query the index for this document.
   *
   * @return the index
   */
  public DjVmDir getDjVmDir()
  {
    return dir;
  }

  /**
   * Get the respective page and start decoding.  If synchronous decoding
   * is used, this call will block until the page is decoded.
   *
   * @param pageno page number to get
   * @param priority decode priority
   * @param dataWait True if bundled pages should be opened even when the 
   *        data is not ready.
   *
   * @return the DjVuPage object
   *
   * @throws IOException if an error occurs
   */
  public DjVuPage getPage(final int pageno,final int priority,final boolean dataWait)
    throws IOException
  {
    final String name=getDjVmDir().page_to_file(pageno).get_load_name();
    final DjVuPage retval=getPage(name,priority,dataWait);
    return retval;
  }

  /**
   * Get the respective page and start decoding.  If synchronous decoding
   * is used, this call will block until the page is decoded.
   *
   * @param id the page name
   * @param priority decode priority
   * @param dataWait True if bundled pages should be opened even when the 
   *        data is not ready.
   *
   * @return the DjVuPage object
   *
   * @throws IOException if an error occurs
   */
  public DjVuPage getPage(final String id,final int priority,final boolean dataWait)
    throws IOException
  {
    DjVuPage page = null;

    if(hasReferences)
    {
      try
      {
        page = (DjVuPage)getFromReference(pageMap.get(id));
      }
      catch(final ArrayIndexOutOfBoundsException exp){}
    }

    if(page == null)
    {
      CachedInputStream data=get_data(id);
      if(! dataWait)
      {
        prefetch(id,MAX_PRIORITY);
      }
      if(dataWait || (data.available() > 0))
      {
        URL url=getDjVmDir().getInitURL();
        if(url != null)
        {
          url=new URL(url,id);
        }
        page = createDjVuPage(url);
        page.setAsync(isAsync());
        page.setPriority(priority);
        page.decode(data);
        if(hasReferences)
        {
          Object ref = createSoftReference(page, null);

          if(ref != null)
          {
            try
            {
              pageMap.put(id, ref);
            }
            catch(final Throwable ignored) {}
          }
        }
      }
    }
    if((page != null)&&!dataWait)
    {
      int p=getPageno('#'+id);
      synchronized(prefetchVector)
      {
        prefetchVector[MAX_PRIORITY].setSize(0);
        prefetchVector[MIN_PRIORITY].setSize(0);
        prefetchCount=0;
        prefetch(id, MAX_PRIORITY);
        prefetch(0, MIN_PRIORITY);
        prefetch(size()-1, MIN_PRIORITY);
        prefetch(p-2, MIN_PRIORITY);
        prefetch(p+2, MIN_PRIORITY);
        prefetch(p-1, MIN_PRIORITY);
        prefetch(p+1, MIN_PRIORITY);
      }        
    }
    return page;
  }

  /**
   * Convert a relative url to a page number. The first page number is page
   * 0.
   *
   * @param url the relative url
   *
   * @return the page number
   */
  public int getPageno(final String url)
  {
    return getDjVmDir().getPageno(url);
  }

  /**
   * Create a copy by value.
   *
   * @return the newly created copy
   */
  public Object clone()
  {
    Cloneable retval = null;

    try
    {
      retval = (Document)super.clone();

      final DjVmDir djvmDir = getDjVmDir();

      if(djvmDir != null)
      {
        ((Document)retval).dir = (DjVmDir)djvmDir.clone();
      }

      if(cachedInputStreamMap != null)
      {
        ((Document)retval).cachedInputStreamMap = (Hashtable)cachedInputStreamMap.clone();
      }
    }
    catch(final CloneNotSupportedException ignored) {}

    return retval;
  }

  /**
   * Create an overloaded DjVuPage object
   *
   * @return the newly created object
   */
  public DjVuPage createDjVuPage(final URL url)
  {
    return new DocumentDjVuPage(url);
  }

  /**
   * Remove the named file from the index
   *
   * @param id name to remove
   *
   * @throws IOException if an error occurs
   */
  public void delete_file(final String id)
    throws IOException
  {
    if(!cachedInputStreamMap.contains(id))
    {
      throw new IOException("Can not delete " + id);
    }

    cachedInputStreamMap.remove(id);
    getDjVmDir().delete_file(id);
  }

  /**
   * Query the data for the specified name.
   *
   * @param id name of the file
   *
   * @return the requested data
   *
   * @throws IOException if an error occurs
   */
  public CachedInputStream get_data(final String id)
    throws IOException
  {
    if(id == null)
    {
      throw new IOException("Can not find blank name.");
    }

    CachedInputStream pool = (CachedInputStream)cachedInputStreamMap.get(id);

    final DjVmDir      djvmDir = getDjVmDir();
    if(pool == null)
    {
      final Vector       files_list = djvmDir.get_files_list();

      final URL          initURL = djvmDir.getInitURL();
      final DjVmDir.File f       = getDjVmDir().id_to_file(id);

      if(f == null)
      {
        if(initURL == null)
        {
          throw new IOException("Requested data outside document");
        }

        final URL fileurl = new URL(initURL, id);
        pool = CachedInputStream.createCachedInputStream(this).init(fileurl,false);
        insert_file(pool, DjVmDir.File.INCLUDE, id, id);
      }
      else if(this.pool != null)
      {
        pool = (CachedInputStream)this.pool.clone();
        pool.skip(f.offset);
        pool.setSize(f.size);
        cachedInputStreamMap.put(id, pool);
      }
      else if(initURL != null)
      {
        pool = CachedInputStream.createCachedInputStream(this).init(new URL(initURL, id), false);
        cachedInputStreamMap.put(id, pool);
      }
    }

    if(!djvmDir.is_bundled())
    {
      final Enumeration iff_in = pool.getIFFChunks();
      if((iff_in == null)||!iff_in.hasMoreElements())
      {
        throw new IOException("EOF");
      }
    }
    return pool;
  }

  /**
   * Query the data for a page.
   *
   * @param page_num the page number to request
   *
   * @return the requested data
   *
   * @throws IOException if an error occurs
   */
  public CachedInputStream get_data(final int page_num)
    throws IOException
  {
    return get_data(getDjVmDir().page_to_file(page_num).get_load_name());
  }

  /**
   * Initialize this document from the specified URL.
   *
   * @param url the url to initialize from
   *
   * @return the initialized document
   *
   * @throws IOException if an error occurs
   */
  public Document init(final URL url)
    throws IOException
  {
    read(url);

    return this;
  }

  /**
   * Add a file to the index.
   *
   * @param input data to add
   * @param file_type type of file to add
   * @param name saved name 
   * @param id load name
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    final InputStream input,
    final int         file_type,
    final String      name,
    final String      id)
    throws IOException
  {
    insert_file(input, file_type, name, id, "");
  }

  /**
   * Add a file to the index.
   *
   * @param input data to add
   * @param file_type type of file to add
   * @param name saved name 
   * @param id load name
   * @param title file title
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    final InputStream input,
    final int         file_type,
    final String      name,
    final String      id,
    final String      title)
    throws IOException
  {
    insert_file(input, file_type, name, id, title, -1);
  }

  /**
   * Add a file to the index.
   *
   * @param input data to add
   * @param file_type type of file to add
   * @param name saved name 
   * @param id load name
   * @param title file title
   * @param pos position to insert
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    final InputStream input,
    final int         file_type,
    final String      name,
    final String      id,
    final String      title,
    int               pos)
    throws IOException
  {
    final DjVmDir.File file =
      getDjVmDir().createFile(name, id, title, file_type);
    final CachedInputStream pool = CachedInputStream.createCachedInputStream(this).init(input);
    insert_file(file, pool, pos);
  }

  /**
   * Add a file to the index.
   *
   * @param pool data to add
   * @param file_type type of file to add
   * @param name saved name 
   * @param id load name
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    final CachedInputStream pool,
    final int      file_type,
    final String   name,
    final String   id)
    throws IOException
  {
    insert_file(pool, file_type, name, id, "");
  }

  /**
   * Add a file to the index.
   *
   * @param pool data to add
   * @param file_type type of file to add
   * @param name saved name 
   * @param id load name
   * @param title file title
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    final CachedInputStream pool,
    final int      file_type,
    final String   name,
    final String   id,
    final String   title)
    throws IOException
  {
    insert_file(pool, file_type, name, id, title, -1);
  }

  /**
   * Add a file to the index.
   *
   * @param pool data to add
   * @param file_type type of file to add
   * @param name saved name 
   * @param id load name
   * @param title file title
   * @param pos position to insert
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    final CachedInputStream pool,
    final int      file_type,
    final String   name,
    final String   id,
    final String   title,
    int            pos)
    throws IOException
  {
    final DjVmDir.File file =
      getDjVmDir().createFile(name, id, title, file_type);
    insert_file(file, pool, pos);
  }

  /**
   * Insert a file.
   *
   * @param f File to add
   * @param data_pool data to add
   *
   * @throws IOException DOCUMENT ME!
   */
  public void insert_file(
    final DjVmDir.File f,
    CachedInputStream data_pool)
    throws IOException
  {
    insert_file(f, data_pool, -1);
  }

  /**
   * Insert a file.
   *
   * @param f File to add
   * @param data_pool data to add
   * @param pos position to insert
   *
   * @throws IOException DOCUMENT ME!
   */
  public void insert_file(
    final DjVmDir.File f,
    CachedInputStream data_pool,
    int                pos)
    throws IOException
  {
    if(f == null)
    {
      throw new IOException("No zero file.");
    }

    if(cachedInputStreamMap.contains(f.get_load_name()))
    {
      throw new IOException("No duplicates allowed.");
    }

    CachedInputStream input = (CachedInputStream)data_pool.clone();
    final int                  b0 = input.read();
    final int                  b1 = input.read();
    final int                  b2 = input.read();
    final int                  b3 = input.read();

    if(
      (b0 != octets[0])
      || (b1 != octets[1])
      || (b2 != octets[2])
      || (b3 != octets[3]))
    {
//      data_pool = data_pool.duplicate(4);
      data_pool = input.createCachedInputStream(Integer.MAX_VALUE);
    }

    cachedInputStreamMap.put(
      f.get_load_name(),
      data_pool);
    getDjVmDir().insert_file(f, pos);
  }
  
  public void prefetch(final int pageno,final int priority)
  {
    try
    {
      prefetch(getDjVmDir().page_to_file(pageno).get_load_name(), priority);
    }
    catch(final Throwable ignored) {}
  }
  
  /**
   * Call to prefetch all the data for this document.
   */
  public void prefetch(final String id,final int priority)
  {
    if(size() > 0)
    {
      synchronized(prefetchVector)
      {
        prefetchVector[priority].addElement(id);
        prefetchCount++;
        prefetchVector.notifyAll();
        Thread thread=prefetchThread;  
        if((thread == null)||!prefetchThread.isAlive())
        {
          final Document runnable = createDocument(new DjVuObject());
          runnable.parentRef      = createWeakReference(this, this);
          runnable.dir            = getDjVmDir();
          runnable.cachedInputStreamMap    = cachedInputStreamMap;
          runnable.prefetchVector = prefetchVector;
          prefetchThread=thread=new Thread(runnable);
          thread.start();
        }
      }
    }
  }

  /**
   * Called to initialize from the specified stream.
   *
   * @param input stream to read
   *
   * @throws IOException if an error occurs
   */
  public void read(final InputStream input)
    throws IOException
  {
    read(CachedInputStream.createCachedInputStream(this).init(input));
  }

  /**
   * Called to initialize from the specified stream.
   *
   * @param data_pool data to read
   *
   * @throws IOException if an error occurs
   */
  public void read(final CachedInputStream data_pool)
    throws IOException
  {
    final DjVmDir djvmDir = getDjVmDir();
    djvmDir.setInitURL(null);

    final Enumeration iff = data_pool.getIFFChunks();
    if((iff == null)||!iff.hasMoreElements())
    {
        throw new IOException("EOF");
    }
    final CachedInputStream formStream=(CachedInputStream)iff.nextElement();
    if(! "FORM:DJVM".equals(formStream.getName()))
    {
      insert_file(data_pool, DjVmDir.File.PAGE, "noname.djvu", "noname.djvu");
      return;
    }

    final Enumeration formIff=formStream.getIFFChunks();
    if((formIff != null)&&!formIff.hasMoreElements())
    {
      throw new IOException("EOF");
    }
    final CachedInputStream dirmStream=(CachedInputStream)formIff.nextElement();
    if(! "DIRM".equals(dirmStream.getName()))
    {
      throw new IOException("No DIRM chunk");
    }
    
    djvmDir.decode(dirmStream);

    cachedInputStreamMap.clear();

    if(djvmDir.is_indirect())
    {
      throw new IOException("Cannot read indirect chunk.");
    }

    this.pool = data_pool;

    Vector files_list = djvmDir.get_files_list();

    for(int i = 0; i < files_list.size(); i++)
    {
      final DjVmDir.File f = (DjVmDir.File)files_list.elementAt(i);

      final CachedInputStream filePool = (CachedInputStream)data_pool.clone();
      filePool.skip(f.offset);
      filePool.setSize(f.size);
      cachedInputStreamMap.put(
        f.get_load_name(),
        filePool);
    }

    final Codec bookmark = getBookmark();

    if(bookmark != null)
    {
      while(formIff.hasMoreElements())
      {
        CachedInputStream chunk=(CachedInputStream)formIff.nextElement();
        final String name=chunk.getName();
        if((name == null)|| name.startsWith("FORM"))
        {
          break;
        }
        if(name.equals("NAVM"))
        {
          bookmark.decode(CachedInputStream.createCachedInputStream(this).init(BSInputStream.createBSInputStream(this).init(chunk)));
        }
      }
    }
  }

  /**
   * Called to initialize from the specified stream.
   *
   * @param url URL to read
   *
   * @throws IOException if an error occurs
   */
  public void read(final URL url)
    throws IOException
  {
    setStatus("Read URL "+url);
    final DjVmDir djvmDir = getDjVmDir();
    djvmDir.setInitURL(null);

    final CachedInputStream pool = CachedInputStream.createCachedInputStream(this).init(url,false);
    final Enumeration iff = pool.getIFFChunks();
    if((iff == null)||!iff.hasMoreElements())
    {
      throw new IOException("Invalid DjVu File Format");
    }
    final CachedInputStream formStream=(CachedInputStream)iff.nextElement();
    if(!"FORM:DJVM".equals(formStream.getName()))
    {
      if(! pool.isDjVuFile())
      {
        throw new IOException("Invalid DjVu File Format");
      }
      String name = url.getFile();
      int    s = name.indexOf('?');

      if(s > 0)
      {
        name = name.substring(0, s);
      }

      s = name.lastIndexOf('/');

      if(s > 0)
      {
        name = name.substring(s);
      }

      insert_file(pool, DjVmDir.File.PAGE, name, name);
    }
    else
    {
      final Enumeration formIff=formStream.getIFFChunks();
      if((formIff == null)||!formIff.hasMoreElements())
      {
        throw new IOException("EOF");
      }
      CachedInputStream chunk=(CachedInputStream)formIff.nextElement();
      if(!"DIRM".equals(chunk.getName()))
      {
        throw new IOException("No DIRM chunk");
      }
      djvmDir.decode(chunk);
      if(djvmDir.is_bundled())
      {
        read(pool);
      }
      else
      {
        final Codec bookmark = getBookmark();

        if(bookmark != null)
        {
          while(formIff.hasMoreElements())
          {
            chunk=(CachedInputStream)formIff.nextElement();
            if("NAVM".equals(chunk.getName()))
            {
              bookmark.decode(
                CachedInputStream.createCachedInputStream(this).init(
                BSInputStream.createBSInputStream(this).init(chunk)));
            }
          }
        }

        cachedInputStreamMap.clear();
      }
    }

    djvmDir.setInitURL(url);
    setStatus("URL "+url+" initialized");
  }

  /**
   * This run method is called by the thread created with prefetch()
   */
  public void run()
  {
    try
    {
      final Thread current = Thread.currentThread();
      int priority=MIN_PRIORITY;
      String last=null;
//    logError("queue + "+this);
      while( ((Document)getFromReference(parentRef)).prefetchThread == current)
      {
        String id = null;

        synchronized(prefetchVector)
        {
          if(((Document)getFromReference(parentRef)).prefetchCount == 0)
          {
            try
            {
              prefetchVector.wait(5000L);
            }
            catch(final Throwable ignored) {}
            if(((Document)getFromReference(parentRef)).prefetchCount == 0)
            {
              ((Document)getFromReference(parentRef)).prefetchThread = null;

              break;
            }
          }
          priority=prefetchVector.length;
          while(priority>0)
          {
            final Vector prefetch=prefetchVector[--priority];
            if(prefetch.size() > 0)
            {
              id=(String)prefetch.lastElement();
              prefetch.removeElementAt(prefetch.size() - 1);
              ((Document)getFromReference(parentRef)).prefetchCount--;
              if(id != null)
              {
                if(id.equals(last))
                {
                  id=null;  
                }
                else
                {
                  if(priority < MAX_PRIORITY)
                  {
                    final Vector q=prefetchVector[priority+1];
                    q.addElement(id);
                    ((Document)getFromReference(parentRef)).prefetchCount++;
                    id=null;
                    try { prefetchVector.wait(200L); } catch(final Throwable ignored) {}
                  }
                  break;
                }
              }
            }
          }
        }
        if(id != null)
        {
          last=id;
          try
          {
            ((Document)getFromReference(parentRef)).setStatus("fetching "+id);
            ((Document)getFromReference(parentRef)).get_data(id).prefetchWait();
            ((Document)getFromReference(parentRef)).setStatus("fetched "+id);
          }
          catch(final Throwable ignored) {}
        }
 
      }
    }
    catch(final Throwable ignored) {}
//    if(bundled && (getFromReference(parentRef) == null))
//    {
//      CachedInputStream.cache.setSize(0);
//    }
  }

  /**
   * Query the number of pages.
   *
   * @return the number of pages.
   */
  public int size()
  {
    return getDjVmDir().get_pages_num();
  }

  //~ Inner Classes ----------------------------------------------------------

  /**
   * An overloaded version of DjVuPage which can read pages from the index.
   *
   * @author Bill C. Riemers
   * @version $Revision: 1.23 $
   */
  class DocumentDjVuPage
    extends DjVuPage
  {
    //~ Constructors ---------------------------------------------------------

    DocumentDjVuPage(final URL url)
    {
      setDjVuOptions(Document.this.getDjVuOptions());
      this.url=url;
    }

    //~ Methods --------------------------------------------------------------

    /**
     * Overloaded version of createCachedInputStream which calls get_data.
     *
     * @param id name to read
     *
     * @return the requested data
     *
     * @throws IOException if an error occurs
     */
    CachedInputStream createCachedInputStream(final String id)
      throws IOException
    {
      return Document.this.get_data(id);
    }    
  }
}
