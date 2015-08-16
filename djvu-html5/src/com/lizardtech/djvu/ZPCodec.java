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


// Referenced classes of package com.lizardtech.javadjvu:
//            BitContext, DjVuStatic
public class ZPCodec
  extends DjVuObject
{
  //~ Static fields/initializers ---------------------------------------------

  /** DOCUMENT ME! */
  public static final int ARRAYSIZE = 256;

  /** DOCUMENT ME! */
  protected static final byte[] FFZT = new byte[ARRAYSIZE];

  static
  {
    for(int i = 0; i < ARRAYSIZE; i++)
    {
      FFZT[i] = 0;

      for(int j = i; (j & 0x80) > 0; j <<= 1)
      {
        FFZT[i]++;
      }
    }
  }

  //~ Instance fields --------------------------------------------------------

  /** DOCUMENT ME! */
  public final ZPTable[] defaultZtable =
  {
    new ZPTable(32768, 0, (short)84, (short)145),
    new ZPTable(32768, 0, (short)3, (short)4),
    new ZPTable(32768, 0, (short)4, (short)3),
    new ZPTable(27581, 4261, (short)5, (short)1),
    new ZPTable(27581, 4261, (short)6, (short)2),
    new ZPTable(23877, 7976, (short)7, (short)3),
    new ZPTable(23877, 7976, (short)8, (short)4),
    new ZPTable(20921, 11219, (short)9, (short)5),
    new ZPTable(20921, 11219, (short)10, (short)6),
    new ZPTable(18451, 14051, (short)11, (short)7),
    new ZPTable(18451, 14051, (short)12, (short)8),
    new ZPTable(16341, 16524, (short)13, (short)9),
    new ZPTable(16341, 16524, (short)14, (short)10),
    new ZPTable(14513, 18685, (short)15, (short)11),
    new ZPTable(14513, 18685, (short)16, (short)12),
    new ZPTable(12917, 20573, (short)17, (short)13),
    new ZPTable(12917, 20573, (short)18, (short)14),
    new ZPTable(11517, 22224, (short)19, (short)15),
    new ZPTable(11517, 22224, (short)20, (short)16),
    new ZPTable(10277, 23665, (short)21, (short)17),
    new ZPTable(10277, 23665, (short)22, (short)18),
    new ZPTable(9131, 24923, (short)23, (short)19),
    new ZPTable(9131, 24923, (short)24, (short)20),
    new ZPTable(8071, 26021, (short)25, (short)21),
    new ZPTable(8071, 26021, (short)26, (short)22),
    new ZPTable(7099, 26978, (short)27, (short)23),
    new ZPTable(7099, 26978, (short)28, (short)24),
    new ZPTable(6213, 27810, (short)29, (short)25),
    new ZPTable(6213, 27810, (short)30, (short)26),
    new ZPTable(5411, 28532, (short)31, (short)27),
    new ZPTable(5411, 28532, (short)32, (short)28),
    new ZPTable(4691, 29158, (short)33, (short)29),
    new ZPTable(4691, 29158, (short)34, (short)30),
    new ZPTable(4047, 29700, (short)35, (short)31),
    new ZPTable(4047, 29700, (short)36, (short)32),
    new ZPTable(3477, 30166, (short)37, (short)33),
    new ZPTable(3477, 30166, (short)38, (short)34),
    new ZPTable(2973, 30568, (short)39, (short)35),
    new ZPTable(2973, 30568, (short)40, (short)36),
    new ZPTable(2531, 30914, (short)41, (short)37),
    new ZPTable(2531, 30914, (short)42, (short)38),
    new ZPTable(2145, 31210, (short)43, (short)39),
    new ZPTable(2145, 31210, (short)44, (short)40),
    new ZPTable(1809, 31463, (short)45, (short)41),
    new ZPTable(1809, 31463, (short)46, (short)42),
    new ZPTable(1521, 31678, (short)47, (short)43),
    new ZPTable(1521, 31678, (short)48, (short)44),
    new ZPTable(1273, 31861, (short)49, (short)45),
    new ZPTable(1273, 31861, (short)50, (short)46),
    new ZPTable(1061, 32015, (short)51, (short)47),
    new ZPTable(1061, 32015, (short)52, (short)48),
    new ZPTable(881, 32145, (short)53, (short)49),
    new ZPTable(881, 32145, (short)54, (short)50),
    new ZPTable(729, 32254, (short)55, (short)51),
    new ZPTable(729, 32254, (short)56, (short)52),
    new ZPTable(601, 32346, (short)57, (short)53),
    new ZPTable(601, 32346, (short)58, (short)54),
    new ZPTable(493, 32422, (short)59, (short)55),
    new ZPTable(493, 32422, (short)60, (short)56),
    new ZPTable(403, 32486, (short)61, (short)57),
    new ZPTable(403, 32486, (short)62, (short)58),
    new ZPTable(329, 32538, (short)63, (short)59),
    new ZPTable(329, 32538, (short)64, (short)60),
    new ZPTable(267, 32581, (short)65, (short)61),
    new ZPTable(267, 32581, (short)66, (short)62),
    new ZPTable(213, 32619, (short)67, (short)63),
    new ZPTable(213, 32619, (short)68, (short)64),
    new ZPTable(165, 32653, (short)69, (short)65),
    new ZPTable(165, 32653, (short)70, (short)66),
    new ZPTable(123, 32682, (short)71, (short)67),
    new ZPTable(123, 32682, (short)72, (short)68),
    new ZPTable(87, 32707, (short)73, (short)69),
    new ZPTable(87, 32707, (short)74, (short)70),
    new ZPTable(59, 32727, (short)75, (short)71),
    new ZPTable(59, 32727, (short)76, (short)72),
    new ZPTable(35, 32743, (short)77, (short)73),
    new ZPTable(35, 32743, (short)78, (short)74),
    new ZPTable(19, 32754, (short)79, (short)75),
    new ZPTable(19, 32754, (short)80, (short)76),
    new ZPTable(7, 32762, (short)81, (short)77),
    new ZPTable(7, 32762, (short)82, (short)78),
    new ZPTable(1, 32767, (short)81, (short)79),
    new ZPTable(1, 32767, (short)82, (short)80),
    new ZPTable(22165, 0, (short)9, (short)85),
    new ZPTable(9454, 0, (short)86, (short)226),
    new ZPTable(32768, 0, (short)5, (short)6),
    new ZPTable(3376, 0, (short)88, (short)176),
    new ZPTable(18458, 0, (short)89, (short)143),
    new ZPTable(1153, 0, (short)90, (short)138),
    new ZPTable(13689, 0, (short)91, (short)141),
    new ZPTable(378, 0, (short)92, (short)112),
    new ZPTable(9455, 0, (short)93, (short)135),
    new ZPTable(123, 0, (short)94, (short)104),
    new ZPTable(6520, 0, (short)95, (short)133),
    new ZPTable(40, 0, (short)96, (short)100),
    new ZPTable(4298, 0, (short)97, (short)129),
    new ZPTable(13, 0, (short)82, (short)98),
    new ZPTable(2909, 0, (short)99, (short)127),
    new ZPTable(52, 0, (short)76, (short)72),
    new ZPTable(1930, 0, (short)101, (short)125),
    new ZPTable(160, 0, (short)70, (short)102),
    new ZPTable(1295, 0, (short)103, (short)123),
    new ZPTable(279, 0, (short)66, (short)60),
    new ZPTable(856, 0, (short)105, (short)121),
    new ZPTable(490, 0, (short)106, (short)110),
    new ZPTable(564, 0, (short)107, (short)119),
    new ZPTable(324, 0, (short)66, (short)108),
    new ZPTable(371, 0, (short)109, (short)117),
    new ZPTable(564, 0, (short)60, (short)54),
    new ZPTable(245, 0, (short)111, (short)115),
    new ZPTable(851, 0, (short)56, (short)48),
    new ZPTable(161, 0, (short)69, (short)113),
    new ZPTable(1477, 0, (short)114, (short)134),
    new ZPTable(282, 0, (short)65, (short)59),
    new ZPTable(975, 0, (short)116, (short)132),
    new ZPTable(426, 0, (short)61, (short)55),
    new ZPTable(645, 0, (short)118, (short)130),
    new ZPTable(646, 0, (short)57, (short)51),
    new ZPTable(427, 0, (short)120, (short)128),
    new ZPTable(979, 0, (short)53, (short)47),
    new ZPTable(282, 0, (short)122, (short)126),
    new ZPTable(1477, 0, (short)49, (short)41),
    new ZPTable(186, 0, (short)124, (short)62),
    new ZPTable(2221, 0, (short)43, (short)37),
    new ZPTable(122, 0, (short)72, (short)66),
    new ZPTable(3276, 0, (short)39, (short)31),
    new ZPTable(491, 0, (short)60, (short)54),
    new ZPTable(4866, 0, (short)33, (short)25),
    new ZPTable(742, 0, (short)56, (short)50),
    new ZPTable(7041, 0, (short)29, (short)131),
    new ZPTable(1118, 0, (short)52, (short)46),
    new ZPTable(9455, 0, (short)23, (short)17),
    new ZPTable(1680, 0, (short)48, (short)40),
    new ZPTable(10341, 0, (short)23, (short)15),
    new ZPTable(2526, 0, (short)42, (short)136),
    new ZPTable(14727, 0, (short)137, (short)7),
    new ZPTable(3528, 0, (short)38, (short)32),
    new ZPTable(11417, 0, (short)21, (short)139),
    new ZPTable(4298, 0, (short)140, (short)172),
    new ZPTable(15199, 0, (short)15, (short)9),
    new ZPTable(2909, 0, (short)142, (short)170),
    new ZPTable(22165, 0, (short)9, (short)85),
    new ZPTable(1930, 0, (short)144, (short)168),
    new ZPTable(32768, 0, (short)141, (short)248),
    new ZPTable(1295, 0, (short)146, (short)166),
    new ZPTable(9454, 0, (short)147, (short)247),
    new ZPTable(856, 0, (short)148, (short)164),
    new ZPTable(3376, 0, (short)149, (short)197),
    new ZPTable(564, 0, (short)150, (short)162),
    new ZPTable(1153, 0, (short)151, (short)95),
    new ZPTable(371, 0, (short)152, (short)160),
    new ZPTable(378, 0, (short)153, (short)173),
    new ZPTable(245, 0, (short)154, (short)158),
    new ZPTable(123, 0, (short)155, (short)165),
    new ZPTable(161, 0, (short)70, (short)156),
    new ZPTable(40, 0, (short)157, (short)161),
    new ZPTable(282, 0, (short)66, (short)60),
    new ZPTable(13, 0, (short)81, (short)159),
    new ZPTable(426, 0, (short)62, (short)56),
    new ZPTable(52, 0, (short)75, (short)71),
    new ZPTable(646, 0, (short)58, (short)52),
    new ZPTable(160, 0, (short)69, (short)163),
    new ZPTable(979, 0, (short)54, (short)48),
    new ZPTable(279, 0, (short)65, (short)59),
    new ZPTable(1477, 0, (short)50, (short)42),
    new ZPTable(490, 0, (short)167, (short)171),
    new ZPTable(2221, 0, (short)44, (short)38),
    new ZPTable(324, 0, (short)65, (short)169),
    new ZPTable(3276, 0, (short)40, (short)32),
    new ZPTable(564, 0, (short)59, (short)53),
    new ZPTable(4866, 0, (short)34, (short)26),
    new ZPTable(851, 0, (short)55, (short)47),
    new ZPTable(7041, 0, (short)30, (short)174),
    new ZPTable(1477, 0, (short)175, (short)193),
    new ZPTable(9455, 0, (short)24, (short)18),
    new ZPTable(975, 0, (short)177, (short)191),
    new ZPTable(11124, 0, (short)178, (short)222),
    new ZPTable(645, 0, (short)179, (short)189),
    new ZPTable(8221, 0, (short)180, (short)218),
    new ZPTable(427, 0, (short)181, (short)187),
    new ZPTable(5909, 0, (short)182, (short)216),
    new ZPTable(282, 0, (short)183, (short)185),
    new ZPTable(4023, 0, (short)184, (short)214),
    new ZPTable(186, 0, (short)69, (short)61),
    new ZPTable(2663, 0, (short)186, (short)212),
    new ZPTable(491, 0, (short)59, (short)53),
    new ZPTable(1767, 0, (short)188, (short)210),
    new ZPTable(742, 0, (short)55, (short)49),
    new ZPTable(1174, 0, (short)190, (short)208),
    new ZPTable(1118, 0, (short)51, (short)45),
    new ZPTable(781, 0, (short)192, (short)206),
    new ZPTable(1680, 0, (short)47, (short)39),
    new ZPTable(518, 0, (short)194, (short)204),
    new ZPTable(2526, 0, (short)41, (short)195),
    new ZPTable(341, 0, (short)196, (short)202),
    new ZPTable(3528, 0, (short)37, (short)31),
    new ZPTable(225, 0, (short)198, (short)200),
    new ZPTable(11124, 0, (short)199, (short)243),
    new ZPTable(148, 0, (short)72, (short)64),
    new ZPTable(8221, 0, (short)201, (short)239),
    new ZPTable(392, 0, (short)62, (short)56),
    new ZPTable(5909, 0, (short)203, (short)237),
    new ZPTable(594, 0, (short)58, (short)52),
    new ZPTable(4023, 0, (short)205, (short)235),
    new ZPTable(899, 0, (short)54, (short)48),
    new ZPTable(2663, 0, (short)207, (short)233),
    new ZPTable(1351, 0, (short)50, (short)44),
    new ZPTable(1767, 0, (short)209, (short)231),
    new ZPTable(2018, 0, (short)46, (short)38),
    new ZPTable(1174, 0, (short)211, (short)229),
    new ZPTable(3008, 0, (short)40, (short)34),
    new ZPTable(781, 0, (short)213, (short)227),
    new ZPTable(4472, 0, (short)36, (short)28),
    new ZPTable(518, 0, (short)215, (short)225),
    new ZPTable(6618, 0, (short)30, (short)22),
    new ZPTable(341, 0, (short)217, (short)223),
    new ZPTable(9455, 0, (short)26, (short)16),
    new ZPTable(225, 0, (short)219, (short)221),
    new ZPTable(12814, 0, (short)20, (short)220),
    new ZPTable(148, 0, (short)71, (short)63),
    new ZPTable(17194, 0, (short)14, (short)8),
    new ZPTable(392, 0, (short)61, (short)55),
    new ZPTable(17533, 0, (short)14, (short)224),
    new ZPTable(594, 0, (short)57, (short)51),
    new ZPTable(24270, 0, (short)8, (short)2),
    new ZPTable(899, 0, (short)53, (short)47),
    new ZPTable(32768, 0, (short)228, (short)87),
    new ZPTable(1351, 0, (short)49, (short)43),
    new ZPTable(18458, 0, (short)230, (short)246),
    new ZPTable(2018, 0, (short)45, (short)37),
    new ZPTable(13689, 0, (short)232, (short)244),
    new ZPTable(3008, 0, (short)39, (short)33),
    new ZPTable(9455, 0, (short)234, (short)238),
    new ZPTable(4472, 0, (short)35, (short)27),
    new ZPTable(6520, 0, (short)138, (short)236),
    new ZPTable(6618, 0, (short)29, (short)21),
    new ZPTable(10341, 0, (short)24, (short)16),
    new ZPTable(9455, 0, (short)25, (short)15),
    new ZPTable(14727, 0, (short)240, (short)8),
    new ZPTable(12814, 0, (short)19, (short)241),
    new ZPTable(11417, 0, (short)22, (short)242),
    new ZPTable(17194, 0, (short)13, (short)7),
    new ZPTable(15199, 0, (short)16, (short)10),
    new ZPTable(17533, 0, (short)13, (short)245),
    new ZPTable(22165, 0, (short)10, (short)2),
    new ZPTable(24270, 0, (short)7, (short)1),
    new ZPTable(32768, 0, (short)244, (short)83),
    new ZPTable(32768, 0, (short)249, (short)250),
    new ZPTable(22165, 0, (short)10, (short)2),
    new ZPTable(18458, 0, (short)89, (short)143),
    new ZPTable(18458, 0, (short)230, (short)246),
    new ZPTable(0, 0, (short)0, (short)0),
    new ZPTable(0, 0, (short)0, (short)0),
    new ZPTable(0, 0, (short)0, (short)0),
    new ZPTable(0, 0, (short)0, (short)0),
    new ZPTable(0, 0, (short)0, (short)0)
  };

  /** DOCUMENT ME! */
  public int bitcount;

  /** DOCUMENT ME! */
  protected final BitContext[] dn = new BitContext[ARRAYSIZE];

  /** DOCUMENT ME! */
  protected final byte[] ffzt;

  /** DOCUMENT ME! */
  protected final int[] mArray = new int[ARRAYSIZE];

  /** DOCUMENT ME! */
  protected final int[] pArray = new int[ARRAYSIZE];

  /** DOCUMENT ME! */
  protected final BitContext[] up = new BitContext[ARRAYSIZE];

  /** DOCUMENT ME! */
  protected int        aValue;
  private InputStream  ibs;
  private OutputStream obs;
  private int          buffer;
  private long         code;
  private long         fence;
  private long         nrun;
  private long         subend;
  private short        delay;
  private short        scount;
  private short        zByte;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new ZPCodec object.
   */
  public ZPCodec()
  {
    ffzt = new byte[FFZT.length];
    System.arraycopy(FFZT, 0, ffzt, 0, ffzt.length);

    for(int i = 0; i < ARRAYSIZE; i++)
    {
      up[i]   = new BitContext();
      dn[i]   = new BitContext();
    }
  }

  /**
   * Creates a new ZPCodec object.
   *
   * @param ibs DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public ZPCodec(final InputStream ibs)
    throws IOException
  {
    this();
    init(ibs);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of ZPCodec with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of ZPCodec.
   */
  public static ZPCodec createZPCodec(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (ZPCodec)create(
      options,
      options.getZPCodecClass(),
      ZPCodec.class);
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final int IWdecoder()
    throws IOException
  {
    return decode_sub_simple(0, 0x8000 + ((aValue + aValue + aValue) >> 3));
  }

  /**
   * DOCUMENT ME!
   *
   * @param ctx DOCUMENT ME!
   * @param z DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final int decode_sub(
    final BitContext ctx,
    int              z)
    throws IOException
  {
    final int bit = ctx.bit & 1;
    final int d = 24576 + ((z + aValue) >> 2);

    if(z > d)
    {
      z = d;
    }

    if(z > code)
    {
      z = 0x10000 - z;
      aValue += z;
      code += z;
      ctx.set(dn[0xff & ctx.bit]);

      final int shift = ffz(aValue);
      scount -= shift;
      aValue   = 0xffff & (aValue << shift);
      code =
        0xffff
        & ((code << shift) | (long)((buffer >> scount) & ((1 << shift) - 1)));

      if(scount < 16)
      {
        preload();
      }

      fence    = code;

      if(code >= 32768L)
      {
        fence = 32767L;
      }

      return bit ^ 1;
    }

    if((0xffffffffL & aValue) >= (0xffffffffL & mArray[0xff & ctx.bit]))
    {
      ctx.set(up[0xff & ctx.bit]);
    }

    scount--;
    aValue   = 0xffff & (z << 1);
    code     = 0xffff & ((code << 1) | (long)((buffer >> scount) & 1));

    if(scount < 16)
    {
      preload();
    }

    fence = code;

    if(code >= 32768L)
    {
      fence = 32767L;
    }

    return bit;
  }

  /**
   * DOCUMENT ME!
   *
   * @param mps DOCUMENT ME!
   * @param z DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final int decode_sub_nolearn(
    final int mps,
    int       z)
    throws IOException
  {
    final int d = 24576 + ((z + aValue) >> 2);

    if(z > d)
    {
      z = d;
    }

    if(z > code)
    {
      z = 0x10000 - z;
      aValue += z;
      code += z;

      final int shift = ffz(aValue);
      scount -= shift;
      aValue   = 0xffff & (aValue << shift);
      code =
        0xffff
        & ((code << shift) | (long)((buffer >> scount) & ((1 << shift) - 1)));

      if(scount < 16)
      {
        preload();
      }

      fence    = code;

      if(code >= 32768L)
      {
        fence = 32767L;
      }

      return mps ^ 1;
    }

    scount--;
    aValue   = 0xffff & (z << 1);
    code     = 0xffff & ((code << 1) | (long)((buffer >> scount) & 1));

    if(scount < 16)
    {
      preload();
    }

    fence = code;

    if(code >= 32768L)
    {
      fence = 32767L;
    }

    return mps;
  }

  /**
   * DOCUMENT ME!
   *
   * @param mps DOCUMENT ME!
   * @param z DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final int decode_sub_simple(
    final int mps,
    int       z)
    throws IOException
  {
    if(z > code)
    {
      z = 0x10000 - z;
      aValue += z;
      code += z;

      final int shift = ffz(aValue);
      scount -= shift;
      aValue   = 0xffff & (aValue << shift);
      code =
        0xffff
        & ((code << shift) | (long)((buffer >> scount) & ((1 << shift) - 1)));

      if(scount < 16)
      {
        preload();
      }

      fence    = code;

      if(code >= 32768L)
      {
        fence = 32767L;
      }

      return mps ^ 1;
    }

    scount--;
    aValue   = 0xffff & (z << 1);
    code     = 0xffff & ((code << 1) | (long)((buffer >> scount) & 1));

    if(scount < 16)
    {
      preload();
    }

    fence = code;

    if(code >= 32768L)
    {
      fence = 32767L;
    }

    return mps;
  }

  /**
   * DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final int decoder()
    throws IOException
  {
    return decode_sub_simple(0, 0x8000 + (aValue >> 1));
  }

  /**
   * DOCUMENT ME!
   *
   * @param ctx DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final int decoder(BitContext ctx)
    throws IOException
  {
    final int ictx = 0xff & ctx.bit;
    final int z = aValue + pArray[ictx];

    if(z <= fence)
    {
      aValue = z;

      return ictx & 1;
    }
    else
    {
      return decode_sub(ctx, z);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @param x DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   */
  public final int ffz(final int x)
  {
    return ((0xffffffffL & x) < 65280L)
    ? ffzt[0xff & (x >> 8)]
    : (ffzt[0xff & x] + 8);
  }

  /**
   * DOCUMENT ME!
   *
   * @param ibs DOCUMENT ME!
   *
   * @return DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final ZPCodec init(final InputStream ibs)
    throws IOException
  {
    this.ibs = ibs;
    dinit();

    return this;
  }

  /**
   * DOCUMENT ME!
   *
   * @param table DOCUMENT ME!
   */
  public final void newZPTable(final ZPTable[] table)
  {
    for(int i = 0; i < ARRAYSIZE; i++)
    {
      pArray[i]   = table[i].pValue;
      mArray[i]   = table[i].mValue;
      up[i].set(table[i].up);
      dn[i].set(table[i].dn);
    }
  }

  /**
   * DOCUMENT ME!
   *
   * @throws IOException DOCUMENT ME!
   */
  public final void preload()
    throws IOException
  {
    for(; scount <= 24; scount += 8)
    {
      zByte   = -1;

      zByte = (short)ibs.read();

      if(zByte == -1)
      {
        zByte = 255;

        if(--delay < 1)
        {
          throw new IOException("EOF");
        }
      }

      buffer = (buffer << 8) | zByte;
    }
  }

  private final void dinit()
    throws IOException
  {
    aValue = 0;
    newZPTable(defaultZtable);
    code = 0xff00;

    try
    {
      code &= (ibs.read() << 8);
      zByte = (short)(0xff & ibs.read());
    }
    catch(IOException exp)
    {
      zByte = 255;
    }

    code |= zByte;
    delay    = 25;
    scount   = 0;
    preload();
    fence = code;

    if(code >= 32768L)
    {
      fence = 32767L;
    }
  }
}
