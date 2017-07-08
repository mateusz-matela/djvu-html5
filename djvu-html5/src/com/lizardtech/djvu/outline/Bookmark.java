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

import java.io.IOException;
import java.util.ArrayList;

import com.lizardtech.djvu.CachedInputStream;
import com.lizardtech.djvu.Codec;
import com.lizardtech.djvu.DjVuOptions;


/**
 * This class decodes outline (bookmarks) contained within a chunk and
 * represents them in a tree structure.
 *
 * @author $author$
 * @version $Revision: 1.8 $
 */
public class Bookmark
  implements Codec
{
  //~ Instance fields --------------------------------------------------------

  /** grelative or blank url. */
  private String url = null;

  // example:  "Section 3.5 - Encryption"
  private String  displayName             = null;
  
  // The childen for this bookmark
  private ArrayList<Bookmark> children = new ArrayList<>();

  // indicates if this bookmark is valid
  private boolean valid = true;

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
   * Query the Vector of children bookmarks.
   *
   * @return the Vector of children
   */
  public ArrayList<Bookmark> getChildren()
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
    return displayName;
  }

  /**
   * Associate an object with this bookmark.
   *
   * @param object associated object
   */
  public void setObject(final String object)
  {
    this.url = object;
  }

  /**
   * Query the object associated with this bookmark.
   *
   * @return the object associated with this bookmark
   */
  public String getObject()
  {
    return url;
  }

  /**
   * Add a child bookmark.
   *
   * @param child bookmark to add
   */
  public void addElement(final Bookmark child)
  {
    getChildren().add(child);
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
  @Override
public void decode(final CachedInputStream input)
  {
    children.clear();
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
    return children.get(item);
  }

  /**
   * Recursively move childrens children as children.
   */
  protected void flatten()
  {
    for(Bookmark child : children)
    {
      child.flatten();
    }

    while(children.size() == 1)
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

      children.clear();
      children.addAll(child.getChildren());
    }
  }

  // read in a bookmark and all of its children.
  private int readElement(final CachedInputStream input)
    throws IOException
  {
    final Bookmark bookmark = new Bookmark();
    int            count = input.read();

    if(count < 0)
    {
      throw new IllegalStateException("Unexpected EOF");
    }

    int textsize = input.read24();

    if(textsize < 0)
    {
      throw new IllegalStateException("Unexpected EOF");
    }

    if(textsize > 0)
    {
      bookmark.setDisplayName(input.readSizedUTF(textsize));
    }

    textsize = input.read24();

    if(textsize < 0)
    {
      throw new IllegalStateException("Unexpected EOF");
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
