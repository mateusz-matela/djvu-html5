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
//C- Developed by Bill C. Riemers, Foxtrot Technologies Inc.
//C- -------------------------------------------------------------------
//
package com.lizardtech.djvu;

import java.io.*;
import java.util.*;

/**
 * This Enumeration iterates over a CachedInputStream as an ordered list of
 * CachedInputStream's.  The chunk id's are stored in the CachedInputStream
 * as the name value.
 * 
 * IFF files were originally intended for audio.  But is well suited for any 
 * streaming data.
 */
public class IFFEnumeration
  extends DjVuObject
  implements Enumeration
{
  //~ Static fields/initializers ---------------------------------------------

  /** chunk names which can contain other chunks */
  static String[] szComposite = {"FORM", "LIST", "PROP", "CAT "};
  /** chunk names which should not be used */
  static String[] szReserved = {"FOR", "LIS", "CAT"};

  //~ Instance fields --------------------------------------------------------

  /** The raw input stream. */
  protected CachedInputStream input;

  /** The next CachedInputStream object on the list */
  protected CachedInputStream next=null;

  // Buffer used for opening the chunk
  private byte[] bufA = new byte[4];
  
  // Buffer used for opening the chunk
  private byte[] bufB = new byte[4];
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new IFFEnumeration object.
   */
  public IFFEnumeration() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of IFFEnumeration with the options interherited from
   * the specified reference.
   * 
   * @param ref Object to interherit DjVuOptions from.
   * 
   * @return a new instance of IFFEnumeration.
   */
  public static IFFEnumeration createIFFEnumeration(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (IFFEnumeration)DjVuObject.create(
      options,
      options.getIFFEnumerationClass(),
      IFFEnumeration.class);
  }

  /**
   * Initialize this stream.
   *
   * @param input data source
   *
   * @return the initialized stream
   */
  public IFFEnumeration init(final CachedInputStream input)
  {
    this.input = (CachedInputStream)input.clone();
    return this;
  }

  /**
   * Query if there are more CachedInputStream's available.
   *
   * @return True if there is another CachedInputStream available.
   */
  public boolean hasMoreElements()
  {
    if(next == null)
    {
      try
      {
        next=openChunk();
      }
      catch(final IOException exp) 
      {
        exp.printStackTrace(DjVuOptions.err);
        next=null;   
      }
    }
    return (next != null);
  }
  
  /**
   * Query the next CachedInputStream.
   *
   * @return The next CachedInputStream.
   * 
   * @throws NoSuchElementException if there are no more streams available.
   */
  public Object nextElement()
    throws NoSuchElementException
  {
    if(!hasMoreElements())
    {
      throw new NoSuchElementException("EOF");
    }
    final CachedInputStream retval=next;
    next=null;
    return retval;
  }
  
  /*
   * Get the next chunk id.
   *
   * @return the newly created CachedInputStream
   *
   * @throws IOException if an error occurs
   */
  private CachedInputStream openChunk()
    throws IOException
  {

    do
    {
      if(input.read(bufA) < 4)
      {
        return null;
      }
    }
    while(
      (bufA[0] == 65)
      && (bufA[1] == 84)
      && (bufA[2] == 38)
      && (bufA[3] == 84));
    
    if(input.read(bufB) < 4)
    {
      return null;
    }
    int size =
      ((0xff & bufB[0]) << 24) | ((0xff & bufB[1]) << 16)
      | ((0xff & bufB[2]) << 8) | (0xff & bufB[3]);
    if(size < 0)
    {
      return null;
    }
    final String id1=genId(bufA);
    String id2=null;
    if(check_id(id1))
    {
      if(size < 4)
      {
        return null;
      }
      if(input.read(bufB) < 4)
      {
        return null;
      }
      size-=4;
      id2=genId(bufB);
      check_id(id2);
    }
    CachedInputStream retval=input.createCachedInputStream(size);
    retval.setName((id2 != null)?(id1+":"+id2):id1);
    input.skip(size+(size&1));
    return retval;
  }

  /*
   * Read the an id.
   *
   * @param data byte array to read
   *
   * @return the id string
   */
  private static String genId(byte[] data)
  {
    return ((data != null) && (data.length > 0))
    ? ((new String(data,0, data.length)) + "\0\0\0\0").substring(0, 4)
    : "\0\0\0\0";
  }
  
  /**
   * Check for the specified id
   *
   * @param id id to check for
   *
   * @return true if the id was found
   *
   * @throws IOException if an error occurs
   */
  public static boolean check_id(final String id)
    throws IOException
  {
    for(int i = 0; i < 4; i++)
    {
      if((id.charAt(i) < ' ') || (id.charAt(i) > '~'))
      {
        throw new IOException("Illegal chunk id");
      }
    }
    for(int i = 0; i < szComposite.length; i++)
    {
      if(id.equals(szComposite[i]))
      {
        return true;
      }
    }
    final String s = id.substring(0, 3);
    for(int i = 0; i < szReserved.length; i++)
    {
      if(
        s.equals(szReserved[i])
        && (id.charAt(3) >= '1')
        && (id.charAt(3) <= '9'))
      {
        throw new IOException("Illegal chunk id");
      }
    }
    return false;
  }
}
