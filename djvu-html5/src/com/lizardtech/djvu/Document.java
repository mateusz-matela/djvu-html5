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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import com.google.gwt.core.shared.GWT;
import com.lizardtech.djvu.DataSource.ReadyListener;
import com.lizardtech.djvu.DjVmDir.File;
import com.lizardtech.djvu.outline.Bookmark;


/**
 * This class represents indirect, bundled, and single page DjVu documents.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.23 $
 */
public class Document
{
  //~ Static fields/initializers ---------------------------------------------

  /** These are the magic numbers at the start of every DjVu file. */
  public static final byte[] octets = {0x41, 0x54, 0x26, 0x54};

  /** A map of saved pages for this document. */
  private HashMap<String, CachedInputStream> cachedInputStreamMap = new HashMap<>();

  // The bookmark Codec for this document.
  private Codec    bookmark = null;
  
  // The data source for this document
  private CachedInputStream pool     = null;

  // The file index for this document
  private DjVmDir dir        = null;
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new Document object.
   */
  public Document()
  {
    dir = new DjVmDir();
  }
  
  //~ Methods ----------------------------------------------------------------

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
      bookmark = retval = new Bookmark();
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
   *
   * @return the DjVuPage object
   *
   * @throws IOException if an error occurs
   */
  public DjVuPage getPage(final int pageno)
    throws IOException
  {
    final String name=getDjVmDir().page_to_file(pageno).get_load_name();
    final DjVuPage retval=getPage(name);
    return retval;
  }

  /**
   * Get the respective page and start decoding.  If synchronous decoding
   * is used, this call will block until the page is decoded.
   *
   * @param id the page name
   * @param priority decode priority
   *
   * @return the DjVuPage object
   *
   * @throws IOException if an error occurs
   */
  public DjVuPage getPage(final String id)
    throws IOException
  {
    DjVuPage page = null;

      CachedInputStream data=get_data(id, null);
      if(data.isReady())
      {
        String url=getDjVmDir().getInitURL();
        if(url != null)
        {
          url=Utils.url(url,id);
        }
        page = new DocumentDjVuPage(url);
        page.decodeStart(data);
      }

    return page;
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
    if(!cachedInputStreamMap.containsKey(id))
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
 * @param listener 
   *
   * @return the requested data
   *
   * @throws IOException if an error occurs
   */
  public CachedInputStream get_data(final String id, final ReadyListener listener)
    throws IOException
  {
    if(id == null)
    {
      throw new IOException("Can not find blank name.");
    }

    CachedInputStream pool = cachedInputStreamMap.get(id);

    final DjVmDir      djvmDir = getDjVmDir();
    if(pool == null)
    {
      final String       initURL = djvmDir.getInitURL();
      final DjVmDir.File f       = getDjVmDir().id_to_file(id);

      if(f == null)
      {
        if(initURL == null)
        {
          throw new IOException("Requested data outside document");
        }

        final String fileurl = Utils.url(initURL, id);
        final CachedInputStream pool2 = pool = new CachedInputStream();
        pool.init(fileurl, new ReadyListener() {
			
			@Override
			public void dataReady() {
				try {
					insert_file(pool2, DjVmDir.File.INCLUDE, id);
				} catch (IOException e) {
					GWT.log("Could not init page", e);
				}
				if (listener != null)
					listener.dataReady();
			}
		});
        if (pool.isReady())
          insert_file(pool, DjVmDir.File.INCLUDE, id);
      }
      else if(this.pool != null)
      {
        pool = new CachedInputStream(this.pool);
        pool.skip(f.offset);
        pool.setSize(f.size);
        cachedInputStreamMap.put(id, pool);
      }
      else if(initURL != null)
      {
        pool = new CachedInputStream().init(Utils.url(initURL, id), listener);
        cachedInputStreamMap.put(id, pool);
      }
    }

    if(!djvmDir.is_bundled() && pool.isReady())
    {
      final Enumeration<CachedInputStream> iff_in = pool.getIFFChunks();
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
 * @param listener 
   *
   * @return the requested data
   *
   * @throws IOException if an error occurs
   */
  public CachedInputStream get_data(final int page_num, ReadyListener listener)
    throws IOException
  {
    return get_data(getDjVmDir().page_to_file(page_num).get_load_name(), listener);
  }

  /**
   * Add a file to the index.
   *
   * @param pool data to add
   * @param file_type type of file to add
   * @param id load name
   *
   * @throws IOException if an error occurs
   */
  public void insert_file(
    CachedInputStream pool,
    final int      file_type,
    final String   id)
    throws IOException
  {
    final DjVmDir.File file =
      getDjVmDir().createFile(id, file_type);
    if(file == null)
    {
      throw new IOException("No zero file.");
    }

    if(cachedInputStreamMap.containsKey(file.get_load_name()))
    {
      throw new IOException("No duplicates allowed.");
    }

    CachedInputStream input = new CachedInputStream(pool);
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
      pool = input.createCachedInputStream(Integer.MAX_VALUE);
    }

    cachedInputStreamMap.put(
      file.get_load_name(),
      pool);
    getDjVmDir().insert_file(file, -1);
  }

  /**
   * Called to initialize from the specified stream.
   *
   * @param url URL to read
   *
   * @throws IOException if an error occurs
   */
  public void read(final String url)
    throws IOException
  {
    final DjVmDir djvmDir = getDjVmDir();
    djvmDir.setInitURL(null);

    final CachedInputStream pool = new CachedInputStream().init(url,null);
    final Enumeration<CachedInputStream> iff = pool.getIFFChunks();
    if((iff == null)||!iff.hasMoreElements())
    {
      throw new IOException("Invalid DjVu File Format");
    }
    final CachedInputStream formStream=iff.nextElement();
    if(!"FORM:DJVM".equals(formStream.getName()))
    {
      if(! pool.isDjVuFile())
      {
        throw new IOException("Invalid DjVu File Format");
      }
      String name = url.replaceFirst(".+/", "");
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

      insert_file(pool, DjVmDir.File.PAGE, name);
    }
    else
    {
      final Enumeration<CachedInputStream> formIff=formStream.getIFFChunks();
      if((formIff == null)||!formIff.hasMoreElements())
      {
        throw new IOException("EOF");
      }
      CachedInputStream chunk=formIff.nextElement();
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
            chunk=formIff.nextElement();
            if("NAVM".equals(chunk.getName()))
            {
              bookmark.decode(
                new CachedInputStream().init(
                new BSInputStream().init(chunk)));
            }
          }
        }

        cachedInputStreamMap.clear();
      }
    }

    djvmDir.setInitURL(url);
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

    final Enumeration<CachedInputStream> iff = data_pool.getIFFChunks();
    if((iff == null)||!iff.hasMoreElements())
    {
        throw new IOException("EOF");
    }
    final CachedInputStream formStream = iff.nextElement();
    if(! "FORM:DJVM".equals(formStream.getName()))
    {
      insert_file(data_pool, DjVmDir.File.PAGE, "noname.djvu");
      return;
    }

    final Enumeration<CachedInputStream> formIff=formStream.getIFFChunks();
    if((formIff != null)&&!formIff.hasMoreElements())
    {
      throw new IOException("EOF");
    }
    final CachedInputStream dirmStream = formIff.nextElement();
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

    Vector<File> files_list = djvmDir.get_files_list();

    for(int i = 0; i < files_list.size(); i++)
    {
      final DjVmDir.File f = files_list.elementAt(i);

      final CachedInputStream filePool = new CachedInputStream(data_pool);
      filePool.skip(f.offset);
      filePool.setSize(f.size);
      cachedInputStreamMap.put(
        f.get_load_name(),
        filePool);
    }

    final Codec bookmark = getBookmark();

      while(formIff.hasMoreElements())
      {
        CachedInputStream chunk=formIff.nextElement();
        final String name=chunk.getName();
        if((name == null)|| name.startsWith("FORM"))
        {
          break;
        }
        if(name.equals("NAVM"))
        {
          bookmark.decode(new CachedInputStream().init(new BSInputStream().init(chunk)));
        }
      }
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

    DocumentDjVuPage(final String url)
    {
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
    @Override
	CachedInputStream createCachedInputStream(final String id)
      throws IOException
    {
      return Document.this.get_data(id, null);
    }    
  }
}
