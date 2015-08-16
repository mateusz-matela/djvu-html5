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
import java.lang.reflect.*;
import java.net.URL;
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
  //~ Static fields/initializers ---------------------------------------------

  /** This flag indicates if references are available. */
  public static final boolean hasReferences;

  /** This constructor will be used to create soft references. */
  private static final Constructor  newSoftReference;
  
  /** This constructor will be used to create weak references. */
  private static final Constructor  newWeakReference;
  
  /** This method will be used to read references. */
  private static final Method       getMethod;

  static
  {
    Method xgetMethod=null;
    Constructor xnewSoftReference=null;
    Constructor xnewWeakReference=null;
    try
    {
      xgetMethod =
        Class.forName("java.lang.ref.Reference").getMethod("get", null);
      final Class[] params = {Object.class};
      xnewWeakReference=xnewSoftReference =
        Class.forName("java.lang.ref.SoftReference").getConstructor(params);
      xnewWeakReference =
        Class.forName("java.lang.ref.WeakReference").getConstructor(params);
    }
    catch(final Throwable ignored) {}
    getMethod=xgetMethod;
    newSoftReference=xnewSoftReference;
    newWeakReference=xnewWeakReference;
    hasReferences = (newSoftReference != null);
  }

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
    return (hasReferences &&(value != null))?invoke(getMethod, value, null):value;
  }

  /**
   * Creates the desired class.  bestClass is not null, it will be created.
   * Otherwise the named class will be created.  If the creation fails, a
   * null will be returned.
   *
   * @param options the DjVuOptions instance the new object should use.
   * @param bestClass The class we wish to create.
   * @param className The name of the class to create as default.
   *
   * @return the newly created object
   */
  public static DjVuInterface create(
    final DjVuOptions options,
    final Class       bestClass,
    final String      className)
  {
    Class defaultClass = null;

    if(
      (bestClass == null)
      || !DjVuInterface.class.isAssignableFrom(bestClass))
    {
      try
      {
        defaultClass = Class.forName(className);
      }
      catch(final Throwable exp)
      {
        return null;
      }
    }

    return create(options, bestClass, defaultClass);
  }

  /**
   * Creates the desired class.  bestClass is not null, it will be created.
   * Otherwise the named class will be created.  If the creation fails, a
   * null will be returned.
   *
   * @param options the DjVuOptions instance the new object should use.
   * @param bestClass The class we wish to create.
   * @param defaultClass The class to create as default.
   *
   * @return the newly created object
   */
  public static DjVuInterface create(
    final DjVuOptions options,
    final Class       bestClass,
    final Class       defaultClass)
  {
    DjVuInterface retval = null;
    try
    {
      if(bestClass != null)
      {
        try
        {
           retval = (DjVuInterface)bestClass.newInstance();
        }
        catch(final Throwable exp)        
        {
          retval = (DjVuInterface)defaultClass.newInstance();
        }
      }
      else if(defaultClass != null)
      {
        retval = (DjVuInterface)defaultClass.newInstance();
      }
      retval.setDjVuOptions(options);
    }
    catch(final InstantiationException exp)
    {
      exp.printStackTrace(DjVuOptions.err);
    }
    catch(final IllegalAccessException exp)
    {
      exp.printStackTrace(DjVuOptions.err);
    }

    return retval;
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
    if(hasReferences)
    {
      try
      {
        final Object[] args = {value};

        return newSoftReference.newInstance(args);
      }
      catch(final Throwable ignored) {}
    }
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
    if(hasReferences)
    {
      try
      {
        final Object[] args = {value};

        return newWeakReference.newInstance(args);
      }
      catch(final Throwable ignored) {}
    }
    return defaultValue;
  }
  
  /**
   * This is a wrapper for method.invoke that catches and reports exceptions.
   *
   * @param method class method to invoke
   * @param o the object to invoke the method on
   * @param args the arguments to send to the method
   *
   * @return the method.invoke return value or null
   */
  public static Object invoke(Method method, Object o, Object [] args)
  {
    Object retval=null;
    try
    {
      retval=method.invoke(o,args);
    }
    catch(final IllegalAccessException exp) 
    {
      exp.printStackTrace(DjVuOptions.err);
    }
    catch(final InvocationTargetException exp) 
    {
      exp.printStackTrace(DjVuOptions.err);
    }
    return retval;
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
}
