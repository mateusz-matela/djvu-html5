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

import java.io.PrintStream;

/**
 * This is a factory which stores standard encoding options.  Each class
 * implementing DjVuInterface or dirived from DjVuObject should be added
 * here.  When a DjVuInterface class creates a new Object, it will use this
 * factory.  That way even the core decoding classes may be overloaded
 * simply by setting the DjVuOptions of the top level class. Use of this
 * factor also greatly speeds up the initialization of Microsoft's
 * implementation of Java 1.1.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.19 $
 */
public class DjVuOptions
{
  //~ Static fields/initializers ---------------------------------------------

  /** The build version of this code. */
  public static final String VERSION="0_8_09";
  
  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_BUCKET_UPPER = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_EXPECT = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_ACTIVE = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean NOCTX_BUCKET_ACTIVE = false;

  /** This should not be changed unless you know what you are doing. */
  public static boolean BEZIERGAMMA = false;

  /**
   * True if garbage collection should be invoked manually on a regular
   * basis.  This greatly slows down the code, but it may be neccessary
   * for a low memory device.
   */
  public static boolean COLLECT_GARBAGE = false;
  
  public static PrintStream out=System.out;
  public static PrintStream err=System.err;

  private DjVuOptions() {
	  //hide constructor
  }
}
