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
 * This is a basic implimentation of the DjVuInterface.  Children should have
 * a static method for creating an instance of the class using the
 * referenced object to initialize DjVuObjects storage.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.11 $
 */
public class DjVuObject
  implements DjVuInterface
{

  //~ Instance fields --------------------------------------------------------

  private DjVuOptions djvuOptions = null;

  //~ Methods ----------------------------------------------------------------

  public static void printStackTrace(final Throwable exp)
  {
    exp.printStackTrace(DjVuOptions.err);    
  }
  
  public static void verbose(final String message)
  {
    DjVuOptions.out.println(message);    
  }
  
  public static void logError(final String message)
  {
    DjVuOptions.err.println(message);    
  }
  
  /**
   * Set the DjVuOptions to be used by this object.
   *
   * @param options DjVuOptions to use.
   */
  public void setDjVuOptions(final DjVuOptions options)
  {
    djvuOptions = options;
  }

  /**
   * Query the DjVuOptions used by this object.
   *
   * @return DjVuOptions in use
   */
  public DjVuOptions getDjVuOptions()
  {
    DjVuOptions retval = djvuOptions;

    if(retval == null)
    {
      djvuOptions = retval = new DjVuOptions();
    }

    return retval;
  }

  /**
   * Retrieve the value from a reference.  If the specified object is not a reference then
   * that value will be returned.
   *
   * @param value The reference to query.
   *
   * @return The value contained by the reference.
   */
  public static Object getFromReference(final Object value)
  {
    return value;
  }

  /**
   * Create a new SoftReference, if supported.
   *
   * @param value The value to wrap in a soft reference.
   * @param defaultValue The value to return if soft references are not
   *        supported.
   *
   * @return The soft reference, or default value.
   */
  public static Object createSoftReference(
    final Object value,
    final Object defaultValue)
  {
    return defaultValue;
  }

  /**
   * Create a new WeakReference, if supported.
   *
   * @param value The value to wrap in a weak reference.
   * @param defaultValue The value to return if weak references are not
   *        supported.
   *
   * @return The weak reference, or default value.
   */
  public static Object createWeakReference(
    final Object value,
    final Object defaultValue)
  {
    return defaultValue;
  }
  
  public static void checkLockTime(final long lockTime,final long maxTime)
  {
    final long t=System.currentTimeMillis()-lockTime;
    if(t > maxTime)
    {
        try { 
            throw new Exception("lock held for "+t+" ms"); 
        } catch(final Throwable exp) {
            exp.printStackTrace(DjVuOptions.err);
        }
    }
  }

  /**
   * Primitive replacement for {@code new java.net.URL(URL context, String spec)}
   */
  protected static String url(String context, String spec) {
	  String base = context.replaceFirst("/[^/]+$", "/");
	  return base + spec;
  }
}
