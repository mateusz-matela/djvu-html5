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
import java.net.*;


/**
 * This class implements a random access mapping of input streams and URL 
 * connections.  For input streams and non-http connections, the mapping 
 * is achieved by reading in all the data into memory.  Http streams are
 * mapped by reading in data blocks when needed.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.13 $
 */
public class DataPool
  extends DjVuObject
{
  //~ Static fields/initializers ---------------------------------------------

  /** The default size of each block. */
  public static final int BLOCKSIZE = 8192;
    
  /** Object for caching raw data. ! */
  public static Hashtable cache = new Hashtable();

  //~ Instance fields --------------------------------------------------------

  // This contains the data we a buffering.
  private final Vector buffer = new Vector();

  // The end of the stream, or a number larger than the end of the stream.
  private int endOffset=Integer.MAX_VALUE;
  
  // The url we are reading.
  private URL url=null;
  
  // The input stream we are reading.
  private InputStream input=null;
  
  // True if we might be able random access memory blocks on a server.
  private boolean rangeAccepted=true;
  
  // The pointer for a simple cache of blocks reciently accessed.
  private int cacheAccessIndex=0;

  // A simple cache of blocks accessed.
  private Object [] cacheAccessArray=new Object[256];
  
  // The pointer for a simple cache of blocks created.
  private int cacheCreatedIndex=0;
  
  // A simple cache of blocks created.
  private Object [] cacheCreatedArray=new Object[256];
  
  // The largest end offset of read data.
  private int currentSize=0;
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DataPool object.
   */
  public DataPool()
  {
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of DataPool with the options interherited from the
   * specified reference.
   * 
   * @param ref Object to interherit DjVuOptions from.
   * 
   * @return a new instance of DataPool.
   */
  public static DataPool createDataPool(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (DataPool)create(
      options,
      options.getDataPoolClass(),
      DataPool.class);
  }

  /**
   * Initialize this map to read the specified URL. If a cached DataPool for this 
   * URL exists, it will be returned.
   * 
   * @param url the URL to read
   * 
   * @return an initialized DataPool
   */
  public DataPool init(final URL url)
  {
    this.url=url;
    DataPool retval=this;
    if(url != null)
    {
      retval=(DataPool)getFromReference(cache.get(url));
      if(retval == null)
      {
        retval=this;
        cache.put(
          url,
          createSoftReference(this, this));
      }
    }
    return retval;
  }
  
  /**
   * Initialize this map to read the specified stream
   * 
   * @param input the InputStream to read
   * 
   * @return the initialized DataPool
   */
  public DataPool init(final InputStream input)
  {
    this.input=input;
    rangeAccepted=false;
    return this;
  }
  
  /** 
   * Query the largest read end offset.
   *
   * @return the largest read end offset
   */
  public int getCurrentSize()
  {
    return currentSize;
  }
  
  /**
   * Request the specified block of data. Data may be buffered, or read.
   *
   * @param index the position of the block start position divided by BLOCKSIZE.
   * @param read True if unavailable blocks should be read from the data source.
   *
   * @return a byte array up to size BLOCKSIZE, or null if no data is available.
   */
  public byte [] getBlock(final int index, final boolean read)
  {
    int start=index*BLOCKSIZE;
    if((index < 0)||(start >= endOffset))
    {
      return null;
    }
    if(index < buffer.size())
    {
      Object block=buffer.elementAt(index);
      if(block != null)
      {
        if(block.getClass().isArray())
        {
          return (byte[])block;
        }
        block=getFromReference(block);
        if(block != null)
        {
          if(read && (cacheAccessArray[cacheAccessIndex%cacheAccessArray.length] != block))
          {
            cacheAccessArray[cacheAccessIndex++%cacheAccessArray.length]=block;
          }
          return (byte[])block;
        }
      }
    }
    return read?readBlock(index):null;
  }

  // Read the specified block of data.  Synchronization should happen prior to calling this
  // routine.  Data may be read either sequentially, or in random order if the server supports
  // http 1.1 range specifiers.
  private synchronized byte [] readBlock(final int index)
  {
    byte [] retval=getBlock(index,false);
    if(retval == null)
    {
      int retry=0;
      int start=index*BLOCKSIZE;
      int end=(index+1)*BLOCKSIZE;
      InputStream input=this.input;
      for(;(input != null)&&(buffer.size() < index);input=this.input)
      {
        if(getBlock(buffer.size(), true) == null)
        {
          return null;
        }
      }
      while((start < endOffset) && (start < end) )
      {
        if(input == null)
        {
          if(rangeAccepted&&(url != null))
          {
            try
            {
              URLConnection connection=url.openConnection();
              if(connection instanceof HttpURLConnection)
              {
                connection.setRequestProperty("Range", "bytes="+start+"-"+(end-1));
                connection.connect();
                final int response=((HttpURLConnection)connection).getResponseCode();
                if(response == 206)
                {
                  input=connection.getInputStream();  
                }
                else if ((response / 100 == 2)&&(start == 0))
                {
                  this.input=input=connection.getInputStream();
                  rangeAccepted=false;
                }
                else if(end < currentSize)
                {
                  connection=null;
                  System.gc();
                  try
                  {
                    Thread.sleep(200L);
                  }
                  catch(final InterruptedException ignored) {}
                  continue;
                }
                else
                {
                  DjVuOptions.out.println("Server response "+response+" requested "+start+","+end);                  
                }
              }
              else if((start == 0)&&(connection != null))
              {
                this.input=input=connection.getInputStream();
                rangeAccepted=false;
              }
            }
            catch(final IOException exp)
            {
              printStackTrace(exp);
              if(input != null)
              {
                try
                {
                  input.close();
                }
                catch(final Throwable ignored) {}
                input=null;
              }
              System.gc();
              try
              {
                  Thread.sleep(200L);
              }
              catch(final Throwable ignored) {}
              if(rangeAccepted&&(++retry < 10))
              {
                continue;
              }
            }
          }
          if(input == null)
          {
            end=start;
            setEndOffset(end);
            break;
          }
        }
        if(retval == null)
        {
          retval=new byte[BLOCKSIZE]; 
        }
        for(int size = end-start;size > 0;size=start-end)
        {
          int offset=start%BLOCKSIZE;
          int len=0;
          try
          {
            len = input.read(retval, offset, size);
          }
          catch(final Throwable exp)
          {
            printStackTrace(exp);
            if(rangeAccepted&&(++retry < 10))
            {
              try
              {
                input.close();
              } catch(final Throwable ignored) {}
              input=null;
              continue;
            }
            len=0;
          }
          retry=0;
          if(len <= 0)
          {
            try
            {
              input.close();
            }
            catch(final IOException ignored) {}
            input=null;
            this.input=null;
            end=start;
            setEndOffset(end);
            if(offset > 0)
            {
              byte [] xretval=new byte[offset];
              System.arraycopy(retval, 0, xretval, 0, offset);
              retval=xretval;
            }
            else
            {
              retval=null;
            }
            break;
          }
          start+=len;
        }
      }
      if(retval != null)
      {
        if(buffer.size() <= index)
        {
          buffer.setSize(index+1);
        }
        if(rangeAccepted&&(index > 0))
        {
          buffer.setElementAt(createSoftReference(retval, retval), index);
          cacheCreatedArray[cacheCreatedIndex++%cacheCreatedArray.length]=retval;
        }
        else
        {
          buffer.setElementAt(retval, index);          
        }
        if(end > currentSize)
        {
          currentSize=end;
        }
      }
    }
    return retval;
  }

  /**
   * Set the end position.  This value may only be reduced, never increased.
   *
   * @param offset new end offset
   */
  protected synchronized void setEndOffset(final int offset)
  {
    if(offset < endOffset)
    {
      endOffset=offset;
      final int size=(offset+BLOCKSIZE-1)/BLOCKSIZE;
      if(size > buffer.size())
      {
        buffer.setSize(size);
      }
    }
  }
  
  /**
   * Query the size of this vector.
   *
   * @return the size of this vector
   */
  public int getEndOffset()
  {
    return endOffset;
  }

}
