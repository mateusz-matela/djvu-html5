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
import java.util.HashMap;
import java.util.Vector;


/**
 * This class represents an index structure for bundled and indexed documents.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.4 $
 */
public final class DjVmDir
  implements Codec
{
  //~ Static fields/initializers ---------------------------------------------

  /** The version of this class. */
  public static final int version = 1;

  //~ Instance fields --------------------------------------------------------

  private String initURL = null;
  private final HashMap<String, File> id2file = new HashMap<>();
  private final Vector<File> files_list = new Vector<>();
  private final Vector<File> page2file = new Vector<>();

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DjVmDir object.
   */
  public DjVmDir() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Query if this is image data.
   *
   * @return false
   */
  @Override
public boolean isImageData()
  { 
      return false;
  }
  
  /**
   * Set the top level URL used to initialize this document.
   *
   * @param url DOCUMENT ME!
   */
  public void setInitURL(final String url)
  {
    initURL = url;
  }

  /**
   * Get the top level URL used to initialize this document.
   *
   * @return the top level URL
   */
  public String getInitURL()
  {
    return initURL;
  }

  /**
   * Create a copy by value of this instance.  Note that this method assumes this class is final.
   *
   * @return the newly created copy
   */
  public DjVmDir(DjVmDir toCopy)
  {
	  setInitURL(toCopy.getInitURL());

    for(File file : toCopy.get_files_list())
    {
      try
      {
        insert_file(new File(file));
      }
      catch(final IOException ignored) {}
    }
  }

  /**
   * Create a new instance of DjVmDir.File
   *
   * @return the new instance of DjVmDir.File
   */
  public File createFile()
  {
    return new File();
  }

  /**
   * Create a new instance of DjVmDir.File
   *
   * @param load_name the name used for loading
   * @param save_name the name used for saving
   * @param title the title
   * @param file_type the type of record
   *
   * @return the new instance of DjVmDir.File
   */
  public File createFile(
    final String load_name,
    final String save_name,
    final String title,
    final int    file_type)
  {
    return new File(load_name, save_name, title, file_type);
  }

  /**
   * Called to decode the data source and initialize this index.
   *
   * @param pool data source
   *
   * @throws IOException if an error occurs
   */
  @Override
public synchronized void decode(final CachedInputStream pool)
    throws IOException
  {
    final CachedInputStream str = new CachedInputStream(pool);
    files_list.setSize(0);
    page2file.setSize(0);
    id2file.clear();

    int     ver     = str.read();
    boolean bundled = (ver & 0x80) != 0;
    ver &= 0x7f;

    if(ver > version)
    {
      throw new IOException("DjVmDir.version_error " + version + " " + ver);
    }

    int files = str.read();
    files = (files << 8) | str.read();

    if(files == 0)
    	return;

      for(int i = 0; i < files; i++)
      {
        final File file = createFile();
        files_list.addElement(file);

        if(bundled)
        {
          int b = str.read();
          b             = (b << 8) | str.read();
          b             = (b << 8) | str.read();
          file.offset   = (b << 8) | str.read();

          if(ver == 0)
          {
            b           = str.read();
            b           = (b << 8) | str.read();
            file.size   = (b << 8) | str.read();
          }

          if(file.offset == 0)
          {
            throw new IOException("DjVmDir.no_indirect");
          }
        }
        else
        {
          file.offset = file.size = 0;
        }
      }

      final InputStream bs_str =
        new BSInputStream().init(str);

      if(ver > 0)
      {
        for(int i = 0; i < files_list.size(); i++)
        {
          final File file = files_list.elementAt(i);
          int        b = bs_str.read();
          b           = (b << 8) | bs_str.read();
          b           = (b << 8) | bs_str.read();
          file.size   = b;
        }
      }

      for(int i = 0; i < files_list.size(); i++)
      {
        final File file = files_list.elementAt(i);
        file.flags = (byte)bs_str.read();
      }

      if(ver == 0)
      {
        for(int i = 0; i < files_list.size(); i++)
        {
          final File file    = files_list.elementAt(i);
          final byte flags_0 = file.flags;
          byte       flags_1 =
            ((flags_0 & File.IS_PAGE_0) != 0)
            ? (File.PAGE)
            : (File.INCLUDE);

          if((flags_0 & File.HAS_NAME_0) != 0)
          {
            flags_1 |= File.HAS_NAME;
          }

          if((flags_0 & File.HAS_TITLE_0) != 0)
          {
            flags_1 |= File.HAS_TITLE;
          }

          file.flags = flags_1;
        }
      }

      if(files_list.size() > 0)
      {
        ByteArrayOutputStream bout   = new ByteArrayOutputStream();
        byte[]                buffer = new byte[1024];

        for(int length; (length = bs_str.read(buffer)) > 0;)
        {
          bout.write(buffer, 0, length);
        }

        buffer = bout.toByteArray();
        bout.close();

        Vector<String> stringList  = new Vector<>();
        int    startOffset = 0;
        int    endOffset   = 0;

        for(; endOffset < buffer.length; ++endOffset)
        {
          if(buffer[endOffset] == 0)
          {
            String s =
              new String(buffer, startOffset, endOffset - startOffset, "UTF-8");
            stringList.addElement(s);
            startOffset = endOffset + 1;
          }
        }

        if(startOffset < endOffset)
        {
          stringList.addElement(
            new String(buffer, startOffset, endOffset - startOffset));
        }

        // Copy names into the files
        for(int i = 0, stringNo = 0; i < files_list.size(); i++)
        {
          final File file = files_list.elementAt(i);
          file.id = stringList.elementAt(stringNo++);

          if((file.flags & File.HAS_NAME) != 0)
          {
            file.name = stringList.elementAt(stringNo++);
          }
          else
          {
            file.name = file.id;
          }

          if((file.flags & File.HAS_TITLE) != 0)
          {
            file.title = stringList.elementAt(stringNo++);
          }
          else
          {
            file.title = file.id;
          }
        }
      }

      // Check that there is only one file with SHARED_ANNO flag on
      int shared_anno_cnt = 0;

      for(int i = 0; i < files_list.size(); i++)
      {
        final File file = files_list.elementAt(i);

        if(file.is_shared_anno())
        {
          shared_anno_cnt++;
        }
      }

      if(shared_anno_cnt > 1)
      {
        throw new IOException("DjVmDir.corrupt");
      }

      // Now generate page=>file array for direct access
      page2file.setSize(0);
      for(int i = 0; i < files_list.size(); i++)
      {
        final File file = files_list.elementAt(i);

        if(file.is_page())
        {
          file.page_num = page2file.size();
          page2file.addElement(file);
        }
      }

      // Generate id2file map
      for(int i = 0; i < files_list.size(); i++)
      {
        final File file = files_list.elementAt(i);

        if(id2file.containsKey(file.id))
        {
          throw new IOException("DjVmDir.dupl_id " + file.id);
        }

        id2file.put(file.id, file);
      }
  }

  /**
   * Called to remove the named file from the index.
   *
   * @param id the name of the file to remove
   */
  public synchronized void delete_file(final String id)
  {
    for(int i = 0; i < files_list.size(); i++)
    {
      File f = files_list.elementAt(i);

      if(id.equals(f.id))
      {
        id2file.remove(f.id);

        if(f.is_page())
        {
          for(int page = 0; page < page2file.size(); page++)
          {
            if(f.equals(page2file.elementAt(page)))
            {
              page2file.removeElementAt(page);

              for(; page < page2file.size(); page++)
              {
                File xfile = page2file.elementAt(page);
                xfile.page_num = page;
              }

              break;
            }
          }
        }

        files_list.removeElementAt(i);

        break;
      }
    }
  }

  /**
   * Called to find the position of a File in the list
   *
   * @param f the file to search for
   *
   * @return the position of the file or -1
   */
  public synchronized int get_file_pos(final File f)
  {
    for(int i = 0; i < files_list.size(); i++)
    {
      if(f.equals(files_list.elementAt(i)))
      {
        return i;
      }
    }

    return -1;
  }

  /**
   * Query the file list.
   *
   * @return the vector of DjVmDir.File objects
   */
  public synchronized Vector<File> get_files_list()
  {
    return files_list;
  }

  /**
   * Query the size of the file list
   *
   * @return the size of the file list
   */
  public synchronized int get_files_num()
  {
    return files_list.size();
  }

  /**
   * Query the position of a page in the list
   *
   * @param page_num page number to search for
   *
   * @return the list position or -1
   */
  public synchronized int get_page_pos(int page_num)
  {
    File file = page_to_file(page_num);

    return (file != null)
    ? get_file_pos(file)
    : (-1);
  }

  /**
   * Query the number of pages in the index.
   *
   * @return the number of pages in the index
   */
  public synchronized int get_pages_num()
  {
    return page2file.size();
  }

  /**
   * Query the document shared annotation
   *
   * @return the document shared annotation
   */
  public synchronized File get_shared_anno_file()
  {
    File retval = null;

    for(int i = 0; i < files_list.size(); i++)
    {
      File frec = files_list.elementAt(i);

      if(frec.is_shared_anno())
      {
        retval = frec;

        break;
      }
    }

    return retval;
  }

  /**
   * Called to map an id to a File.
   *
   * @param id the load name to look for
   *
   * @return the DjVmDir.File record
   */
  public synchronized File id_to_file(final String id)
  {
    return id2file.get(id);
  }

  /**
   * Called to add a file to the index.
   *
   * @param file DjVmDir.File object to add
   *
   * @return the position where the File was added
   *
   * @throws IOException if an error occurs
   */
  public int insert_file(final File file)
    throws IOException
  {
    return insert_file(file, -1);
  }

  /**
   * Called to add a file to the index.
   *
   * @param file DjVmDir.File object to add
   * @param pos_num the position to insert the file
   *
   * @return the position where the File was added
   *
   * @throws IOException if an error occurs
   */
  public synchronized int insert_file(
    final File file,
    int        pos_num)
    throws IOException
  {
    if(pos_num < 0)
    {
      pos_num = files_list.size();
    }

    // Modify maps
    if(id2file.containsKey(file.id))
    {
      throw new IOException("DjVmDir.dupl_id2 " + file.id);
    }

    // Make sure that there is no more than one file with shared annotations
    if(file.is_shared_anno())
    {
      for(int i = 0; i < files_list.size(); i++)
      {
        File xfile = files_list.elementAt(i);

        if(xfile.is_shared_anno())
        {
          throw new IOException("DjVmDir.multi_save2");
        }
      }
    }

    // Add the file to the list
    files_list.insertElementAt(file, pos_num);

    if(file.is_page())
    {
      // This file is also a page
      // Count its number
      int page_num = 0;

      for(int i = 0; i < files_list.size(); i++)
      {
        File xfile = files_list.elementAt(i);

        if(xfile.equals(file))
        {
          break;
        }

        if(xfile.is_page())
        {
          page_num++;
        }
      }

      page2file.insertElementAt(file, page_num);

      for(int i = page_num; i < page2file.size(); i++)
      {
        File xfile = page2file.elementAt(i);
        xfile.page_num = i;
      }
    }

    return pos_num;
  }

  /**
   * Query if the index is for a bundled document.
   *
   * @return true if bundled 
   */
  public boolean is_bundled()
  {
    return !is_indirect();
  }

  /**
   * Query if the index is for an indirect document.
   *
   * @return true if indirect 
   */
  public synchronized boolean is_indirect()
  {
    return ((files_list.size() > 0) && (files_list.elementAt(0) != null)
    && (files_list.elementAt(0).offset == 0));
  }

  /**
   * Query the File mapped to the specified page number.
   *
   * @param page_num the page number to look for
   *
   * @return the File or null
   */
  public synchronized File page_to_file(int page_num)
  {
    return (page_num < page2file.size())
    ? page2file.elementAt(page_num)
    : null;
  }

  //~ Inner Classes ----------------------------------------------------------

  /**
   * This class stores the details about a single page of include file within
   * a document.
   *
   * @author Bill C. Riemers
   * @version $Revision: 1.4 $
   */
  public static final class File
  {
    //~ Static fields/initializers -------------------------------------------

    /** flag to indicate an include file */
    public static final byte INCLUDE = 0;

    /** flag to indicate a page */
    public static final byte PAGE = 1;

    /** flag to indicate thumbnails */
    public static final byte THUMBNAILS = 2;

    /** flag to indicate shared annotations */
    public static final byte SHARED_ANNO = 3;

    private static final byte IS_PAGE_0 = 1;
    private static final byte HAS_NAME_0 = 2;
    private static final byte HAS_TITLE_0 = 4;
    private static final byte HAS_NAME = (byte)0x80;
    private static final byte HAS_TITLE = 0x40;
    private static final byte TYPE_MASK = 0x3f;

    //~ Instance fields ------------------------------------------------------

    /** The position of this file in the main document. */
    public int offset = 0;

    /** The size of this file. */
    public int size = 0;

    /** The id, or load name of this file. */
    private String id = null;

    /** The name, or save name of this file. */
    private String name = null;

    /** The title of this page. */
    private String title = null;

    /** flags */
    private byte flags = 0;

    /** The page number represented by this file. */
    private int page_num = (-1);

    //~ Constructors ---------------------------------------------------------

    /**
     * Creates a new File object.
     */
    public File() {}

    /**
     * Creates a new File object.
     *
     * @param load_name the load name (id) of this file
     * @param save_name the save name (name) of this file
     * @param title the title of this file
     * @param file_type the type of file
     */
    public File(
      final String load_name,
      final String save_name,
      final String title,
      final int    file_type)
    {
      set_load_name(load_name);
      set_save_name(save_name);
      set_title(title);
      flags = (byte)(file_type & TYPE_MASK);
    }

    //~ Methods --------------------------------------------------------------

    /**
     * Create a copy of given object
     */
    public File(File toCopy)
    {
      this.offset = toCopy.offset;
      this.size = toCopy.size;
      this.id = toCopy.id;
      this.name = toCopy.name;
      this.title = toCopy.title;
      this.flags = toCopy.flags;
      this.page_num = toCopy.page_num;
    }

    /**
     * Query the load name (id) of this file.
     *
     * @return the load name
     */
    public final String get_load_name()
    {
      return id;
    }

    /**
     * Query the save name (name) of this file.
     *
     * @return the save name
     */
    public final String get_save_name()
    {
      return ((name != null) && (name.length() > 0))
      ? name
      : id;
    }

    /**
     * Query the title of this file.
     *
     * @return the title
     */
    public final String get_title()
    {
      return ((title != null) && (title.length() > 0))
      ? title
      : id;
    }

    /**
     * Query the page number of this file.
     *
     * @return page number or -1
     */
    public final int get_page_num()
    {
      return page_num;
    }

    /**
     * Query a description of the file type.
     * 
     * @return a String describing the file type. 
     *
     * @throws IOException if an error occurs
     */
    public String get_str_type()
      throws IOException
    {
      String type;

      switch(flags & TYPE_MASK)
      {
        case INCLUDE :
          type = "INCLUDE";

          break;
        case PAGE :
          type = "PAGE";

          break;
        case THUMBNAILS :
          type = "THUMBNAILS";

          break;
        case SHARED_ANNO :
          type = "SHARED_ANNO";

          break;
        default :
          throw new IOException("DjVmDir.get_str_type");
      }

      return type;
    }

    /**
     * Query if this is an include file.
     *
     * @return true if this is an include file
     */
    public boolean is_include()
    {
      return (flags & TYPE_MASK) == INCLUDE;
    }

    /**
     * Query if this is a page.
     *
     * @return true if this is a page
     */
    public boolean is_page()
    {
      return (flags & TYPE_MASK) == PAGE;
    }

    /**
     * Query if this is a shared annotation
     *
     * @return true if this is a shared annotation
     */
    public boolean is_shared_anno()
    {
      return (flags & TYPE_MASK) == SHARED_ANNO;
    }

    /**
     * Query if this is a thumbnail.
     *
     * @return true if this is a thumbnail
     */
    public boolean is_thumbnails()
    {
      return (flags & TYPE_MASK) == THUMBNAILS;
    }

    /**
     * Set the load name (id) of this file.
     *
     * @param id the load name to use
     */
    public void set_load_name(final String id)
    {
      int k = id.lastIndexOf('/');
      this.id = (k != -1)
        ? id.substring(k)
        : id;
    }

    /**
     * Set the title of this page.
     *
     * @param title the page title
     */
    public void set_title(final String title)
    {
      this.title = title;
    }

    /**
     * Set the save name (name) of this file.
     *
     * @param name save name to use
     */
    protected void set_save_name(final String name)
    {
      int k = name.lastIndexOf('/');
      this.name = (k != -1)
        ? name.substring(k)
        : name;
    }
  }
}
