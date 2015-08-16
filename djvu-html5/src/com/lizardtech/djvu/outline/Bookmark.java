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
package com.lizardtech.djvu.outline;

import com.lizardtech.djvu.*;
import java.io.*;
import java.util.*;


/**
 * This class decodes outline (bookmarks) contained within a chunk and
 * represents them in a tree structure.
 *
 * @author $author$
 * @version $Revision: 1.8 $
 */
public class Bookmark
  implements Codec, DjVuInterface
{
  //~ Instance fields --------------------------------------------------------

  // DjVmDir object
  private DjVmDir djvmDir = null;

  // Reference for inheriting DjVuOptions.
  private DjVuObject djvuObject = new DjVuObject();

  // Object: If String relative or blank url.  If Number, page number.
  private Object object = null;

  // example:  "Section 3.5 - Encryption"
  private String  displayName             = null;
  
  // The childen for this bookmark
  private Vector  children                = new Vector();
  
  // True if the URL should be used as the display name.
  private boolean urlAsDefaultDisplayName = false;

  // indicates if this bookmark is valid
  private boolean valid = true;

  //~ Methods ----------------------------------------------------------------

  /**
   * Query if this is image data.
   *
   * @return false
   */
  public boolean isImageData()
  { 
      return false;
  }

  /**
   * Query the Vector of children bookmarks.
   *
   * @return the Vector of children
   */
  public Vector getChildren()
  {
    return children;
  }

  /**
   * Set the display name for this bookmark.
   *
   * @param displayName the name to be displayed for this bookmark
   */
  public void setDisplayName(final String displayName)
  {
    this.displayName = displayName;
  }

  /**
   * Query the display name.
   *
   * @return the display name for this bookmark
   */
  public String getDisplayName()
  {
    String retval = displayName;

    if((retval == null) && urlAsDefaultDisplayName)
    {
      final DjVmDir djvmDir = getDjVmDir();

      if(djvmDir != null)
      {
        Object url = djvmDir.getInitURL();

        if(url != null)
        {
          retval = url.toString();

          final int i = retval.lastIndexOf('/');

          if(i >= 0)
          {
            retval = retval.substring(i + 1);
          }
        }
      }
    }

    return retval;
  }

  /**
   * Set the document directory.
   *
   * @param djvmDir the document directory
   */
  public void setDjVmDir(final DjVmDir djvmDir)
  {
    if(djvmDir != null)
    {
      if(size() == 0)
      {
        urlAsDefaultDisplayName = true;

        int          pageno     = 0;
        final Vector files_list = djvmDir.get_files_list();

        for(Enumeration e = files_list.elements(); e.hasMoreElements();)
        {
          final Bookmark bookmark = createBookmark(this);
          DjVmDir.File   file = (DjVmDir.File)e.nextElement();

          if(file.is_page())
          {
            bookmark.setDisplayName(file.get_title());
            bookmark.setObject(new Integer(pageno++));
            addElement(bookmark);
          }
        }
      }
    }

    setDjVmDir(
      djvmDir,
      elements());
  }

  /**
   * Query the document directory.
   *
   * @return the document directory.
   */
  public DjVmDir getDjVmDir()
  {
    return djvmDir;
  }

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
   * Associate an object with this bookmark.
   *
   * @param object associated object
   */
  public void setObject(final Object object)
  {
    this.object = object;
  }

  /**
   * Query the object associated with this bookmark.
   *
   * @return the object associated with this bookmark
   */
  public Object getObject()
  {
    return object;
  }

  /**
   * Query the page number linked to this bookmark.
   *
   * @return the page number linked to this bookmark
   */
  public int getPageno()
  {
    final Object object = getObject();

    if(object instanceof Number)
    {
      return ((Number)object).intValue();
    }

    if(!(object instanceof String))
    {
      return -1;
    }

    final DjVmDir djvmDir = getDjVmDir();

    if(djvmDir == null)
    {
      return -1;
    }

    final int retval = djvmDir.getPageno((String)object);

    if(retval >= 0)
    {
      setObject(new Integer(retval));
    }

    return retval;
  }

  /**
   * Add a child bookmark.
   *
   * @param child bookmark to add
   */
  public void addElement(final Bookmark child)
  {
    getChildren().addElement(child);
  }

  /**
   * Creates an instance of Bookmark with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of Bookmark.
   */
  public static Bookmark createBookmark(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (Bookmark)DjVuObject.create(
      options,
      options.getBookmarkClass(),
      Bookmark.class);
  }

  /**
   * Sets the size of this vector.
   *
   * @param size DOCUMENT ME!
   */
  public synchronized void setSize(final int size)
  {
    setValid(false);
    getChildren().setSize(size);
    setValid(true);
  }

  /**
   * Set whether a bookmark is considered valid.
   *
   * @param valid true if valid
   */
  public void setValid(final boolean valid)
  {
    this.valid = valid;
  }

  /**
   * Check if a bookmark is valid.
   *
   * @return true if valid
   */
  public boolean isValid()
  {
    return valid;
  }

  /**
   * Decodes the directory from the specified datapool.
   *
   * @param input - the BSInputStream to read from.
   */
  public void decode(final CachedInputStream input)
  {
    setSize(0);
    setObject(null);
    setDisplayName(null);
    setValid(false);

    try
    {
      for(int count = input.read16(); count > 0;)
      {
        count -= readElement(input);
      }

      flatten();
      setValid(true);
    }
    catch(final Throwable exp)
    {
      exp.printStackTrace(DjVuOptions.err);
      System.gc();
    }
  }

  /**
   * Query a child bookmark.
   *
   * @param item child number
   *
   * @return the child bookmark
   */
  public Bookmark elementAt(final int item)
  {
    return (Bookmark)getChildren().elementAt(item);
  }

  /**
   * Query the Enumeration of childen.
   *
   * @return the Enumeration of childen
   */
  public Enumeration elements()
  {
    return getChildren().elements();
  }

  /**
   * Query the number of children.
   *
   * @return the number of children
   */
  public int size()
  {
    return getChildren().size();
  }

  /**
   * Recursively move childrens children as children.
   */
  protected void flatten()
  {
    for(Enumeration e = elements(); e.hasMoreElements();)
    {
      ((Bookmark)e.nextElement()).flatten();
    }

    while(size() == 1)
    {
      final String   displayName = getDisplayName();
      final Bookmark child = elementAt(0);

      if(displayName == null)
      {
        setDisplayName(child.getDisplayName());
        setObject(child.getObject());
      }
      else if(child.getDisplayName() != null)
      {
        break;
      }

      setSize(0);

      for(Enumeration e = child.elements(); e.hasMoreElements();)
      {
        addElement((Bookmark)e.nextElement());
      }
    }
  }

  // Set the document directory and all the bookmarks with an Enumeration.
  private void setDjVmDir(
    final DjVmDir     djvmDir,
    final Enumeration e)
  {
    this.djvmDir = djvmDir;

    while(e.hasMoreElements())
    {
      final Bookmark bookmark = (Bookmark)e.nextElement();
      bookmark.setDjVmDir(
        djvmDir,
        bookmark.elements());
    }
  }

  // read in a bookmark and all of its children.
  private int readElement(final CachedInputStream input)
    throws IOException
  {
    final Bookmark bookmark = createBookmark(this);
    int            count = input.read();

    if(count < 0)
    {
      throw new EOFException("Unexpected EOF");
    }

    int textsize = input.read24();

    if(textsize < 0)
    {
      throw new EOFException("Unexpected EOF");
    }

    if(textsize > 0)
    {
      bookmark.setDisplayName(input.readSizedUTF(textsize));
    }

    textsize = input.read24();

    if(textsize < 0)
    {
      throw new EOFException("Unexpected EOF");
    }

    if(textsize > 0)
    {
      bookmark.setObject(input.readSizedUTF(textsize));
    }

    int retval = 1;

    try
    {
      if(count > 0)
      {
        bookmark.setValid(false);

        for(int i = 0; i < count; i++)
        {
          retval += bookmark.readElement(input);
        }

        bookmark.setValid(true);
      }
    }
    finally
    {
      addElement(bookmark);
    }

    return retval;
  }
}
