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
package com.lizardtech.djvu.text;

import com.lizardtech.djvu.*;
import java.io.*;
import java.util.*;


/**
 * <p>
 * This class implements annotations understood by the DjVu plugins  and
 * encoders. using: contents of TXT chunks. Contents of the FORM:TEXT
 * should be passed to decode for parsing, which
 * initializes this class and fills in the decoded data.
 * </p><p>
 * Description of the text contained in a DjVu page.  This class contains
 * the textual data for the page.  It describes the text as a hierarchy of
 * zones corresponding to page, column, region, paragraph, lines, words,
 * etc... The piece of text associated with each zone is represented by an
 * offset and a length describing a segment of a global UTF8 encoded
 * byteArray.
 * </p><p>
 * Constants are used to tell what a zone describes. This can be
 * useful for a copy/paste application.  The deeper we go into the
 * hierarchy, the higher the constant.
 * </p>
 */
public class DjVuText
  extends DjVuObject
  implements Codec
{
  //~ Static fields/initializers ---------------------------------------------

  /** Indicates a page zone. */
  public static final int PAGE = 1;

  /** Indicates a column zone. */
  public static final int COLUMN = 2;

  /** Indicates a region zone. */
  public static final int REGION = 3;

  /** Indicates a paragraph zone. */
  public static final int PARAGRAPH = 4;

  /** Indicates a line zone. */
  public static final int LINE = 5;

  /** Indicates a word zone. */
  public static final int WORD = 6;

  /** Indicates a character zone. */
  public static final int CHARACTER = 7;

  /** VT: Vertical Tab */
  public static final int end_of_column = 0x0b;

  /** GS: Group Separator */
  public static final int end_of_region = 0x1d;

  /** US: Unit Separator */
  public static final int end_of_paragraph = 0x1f;

  /** LF: Line Feed */
  public static final int end_of_line = 0x0a;

  //~ Instance fields --------------------------------------------------------

  /** Main zone in the document. This zone represent the page. */
  public Zone page_zone = new Zone();

  /** True if UTF8 encoded. */
  public boolean isUTF8 = true;

  /**
   * Textual data for this page.   The content of this byteArray is encoded
   * using the UTF8 code. This code corresponds to ASCII for the first 127
   * characters. Columns, regions, paragraph and lines are delimited by the
   * following control character:
   * 
   * <table>
   * <tr><th>
   * Name
   * </td><th>
   * Octal
   * </td><th>
   * Ascii name
   * </td></tr>
   * <tr><td>
   * DjVuText.end_of_column
   * </td><td>
   * 013
   * </td><td>
   * VT, Vertical Tab
   * </td></tr>
   * <tr><td>
   * DjVuText.end_of_region
   * </td><td>
   * 035
   * </td><td>
   * GS, Group Separator
   * </td></tr>
   * <tr><td>
   * DjVuText.end_of_paragraph
   * </td><td>
   * 037
   * </td><td>
   * US, Unit Separator
   * </td></tr>
   * <tr><td>
   * DjVuText.end_of_line
   * </td><td>
   * 012
   * </td><td>
   * LF: Line Feed
   * </td></tr>
   * </table>
   */
  protected byte[] textByteArray = new byte[0];

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new DjVuText object.
   */
  public DjVuText() {}

  //~ Methods ----------------------------------------------------------------

  /**
   * Creates an instance of DjVuInfo with the options interherited from the
   * specified reference.
   *
   * @param ref Object to interherit DjVuOptions from.
   *
   * @return a new instance of DjVuInfo.
   */
  public static DjVuText createDjVuText(final DjVuInterface ref)
  {
    final DjVuOptions options = ref.getDjVuOptions();

    return (DjVuText)create(
      options,
      options.getDjVuTextClass(),
      DjVuText.class);
  }

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
   * Count the number of characters.
   *
   * @param from byte position to start counting from
   * @param end byte position to stop counting
   *
   * @return The number of characters and start of characters in the range.
   */
  public int getLength(
    final int from,
    final int end)
  {
    int pos    = from;
    int retval = 0;

    for(; (pos < end) && (pos < textByteArray.length); retval++)
    {
      pos = nextChar(textByteArray, pos);
    }

    return retval;
  }

  /**
   * Query the string from the specified range of bytes.
   *
   * @param start byte position of the first character.
   * @param end byte position to end the string
   *
   * @return The converted string
   */
  public String getString(
    int start,
    int end)
  {
    if(isUTF8)
    {
      try
      {
        return new String(textByteArray, start, end - start, "UTF-8");
      }
      catch(final UnsupportedEncodingException exp)
      {
        isUTF8 = false;
      }
    }

    return new String(textByteArray, start, end - start);
  }

  /**
   * Set the text data from an array of bytes.  First try
   * interpreting the array as UTF8.  If that fails consider
   * each byte one character.
   *
   * @param textByteArray array of bytes to interpret
   */
  public void setTextByteArray(final byte[] textByteArray)
  {
    try
    {
      for(
        int pos = 0;
        pos < textByteArray.length;
        pos = nextUTF8Char(textByteArray, pos))
      {
        ;
      }

      isUTF8 = true;
    }
    catch(final UnsupportedEncodingException exp)
    {
      isUTF8 = false;
    }

    this.textByteArray = textByteArray;
  }

  /**
   * Decodes the hidden text layer TXT into internal representation. NOTE:
   * All separators (except word) are replaced with line feeds.
   *
   * @param input The chunk to decode.
   *
   * @throws IOException if an error occures.
   */
  public void decode(CachedInputStream input)
    throws IOException
  {
    if("TXTz".equals(input.getName()))
    {
      input=CachedInputStream.createCachedInputStream(this).init(
              BSInputStream.createBSInputStream(this).init(input));
      input.setName("TXTa");
    }
    // Read text
    int          textsize = input.read24();

    final byte[] textByteArray = new byte[textsize];

    int          readsize = input.read(textByteArray);

    for(int s = 0; s < readsize; s++)
    {
      byte b = textByteArray[s];

      if(b == 0)
      {
        break;
      }

      switch(b)
      {
        case DjVuText.end_of_column :
        case DjVuText.end_of_region :
        case DjVuText.end_of_paragraph :
          textByteArray[s] = '\n';
      }
    }

    if(readsize < textsize)
    {
      while(readsize < textsize)
      {
        textByteArray[readsize++] = 0;
      }

      setTextByteArray(textByteArray);
      throw new IOException("DjVuText.corrupt_chunk");
    }

    setTextByteArray(textByteArray);

    // Try reading zones
    int version = input.read();

    if(version != -1)
    {
      if(version != Zone.version)
      {
        throw new IOException("DjVuText.bad_version=" + version);
      }

      page_zone.decode(input, textsize);
    }
  }

  /**
   * Find the text specified by the rectangles.
   *
   * @param box bounding box to search
   * @param text buffer to fill with the text found
   * @param padding number of pixels to add to each rectangle
   *
   * @return a vector of the smallest level rectangles representing the text found
   */
  public Vector find_text_with_rect(
    GRect        box,
    StringBuffer text,
    int          padding)
  {
    Vector     retval     = new Vector();
    NumContext text_start = new NumContext(0);
    NumContext text_end   = new NumContext(0);
    page_zone.get_text_with_rect(box, text_start, text_end);

    if(text_start.intValue() != text_end.intValue())
    {
      Vector zones = new Vector();
      page_zone.append_zones(
        zones,
        text_start.intValue(),
        text_end.intValue());

      int pos = 0;

      if(pos < zones.size())
      {
        do
        {
          if(padding >= 0)
          {
            ((Zone)zones.elementAt(pos)).get_smallest(retval, padding);
          }
          else
          {
            ((Zone)zones.elementAt(pos)).get_smallest(retval);
          }
        }
        while((++pos) < zones.size());
      }
    }

    text.setLength(0);
    text.append(getString(
        text_start.intValue(),
        text_end.intValue()));

    return retval;
  }

  /**
   * Find the text specified by the rectangles.
   *
   * @param box bounding box to search
   * @param text buffer to fill with the text found
   *
   * @return a vector of the smallest level rectangles representing the text found
   */
  public Vector find_text_with_rect(
    GRect        box,
    StringBuffer text)
  {
    return find_text_with_rect(box, text, 0);
  }

  /**
   * Get all zones of zone type zone_type under node parent. zone_list
   * contains the return value.
   *
   * @param zone_type the zone type to list.
   * @param parent parent zone to start from
   * @param zone_list vector to add the zones to
   */
  public void get_zones(
    int    zone_type,
    Zone   parent,
    Vector zone_list)
  {
    // search all branches under parent
    Zone zone = parent;

    for(int cur_ztype = zone.ztype; cur_ztype < zone_type; ++cur_ztype)
    {
      for(int pos = 0; pos < zone.children.size(); ++pos)
      {
        Zone zcur = (Zone)zone.children.elementAt(pos);

        if(zcur.ztype == zone_type)
        {
          if(!zone_list.contains(zcur))
          {
            zone_list.addElement(zcur);
          }
        }
        else if(((Zone)zone.children.elementAt(pos)).ztype < zone_type)
        {
          get_zones(zone_type, (Zone)zone.children.elementAt(pos), zone_list);
        }
      }
    }
  }

  /**
   * Tests whether there is a meaningful zone hierarchy.
   *
   * @return true if there are valid zones
   */
  public boolean has_valid_zones()
  {
    return !((textByteArray.length == 0) || !page_zone.children.isEmpty()
    || page_zone.isEmpty());
  }

  /**
   * Searches a file for TXTz and TXTa chunks and decodes each of them.
   *
   * @param iff enumeration of CachedInputStream's to read.
   *
   * @return the initialized DjVuText object
   *
   * @throws IOException if an IO error occures.
   */
  public DjVuText init(final Enumeration iff)
    throws IOException
  {
    if(iff != null)
    {
      while(iff.hasMoreElements())
      {
        CachedInputStream chunk=(CachedInputStream)iff.nextElement();
        final String xchkid = chunk.getName();
        if(xchkid.startsWith("FORM:"))
        {
          init(chunk.getIFFChunks());
        }
        else if("TXTa".equals(xchkid)||"TXTz".equals(xchkid))
        {
          decode(chunk);
        }
      }
    }
    return this;
  }

  /**
   * Searches a file for TXTz and TXTa chunks and decodes each of them.
   *
   * @param pool input stream to read.
   *
   * @return the initialized DjVuText object
   *
   * @throws IOException if an IO error occures.
   */
  public DjVuText init(CachedInputStream pool)
    throws IOException
  {
    final Enumeration e=pool.getIFFChunks();
    if(e != null)
    {
      return init(e);
    }
    decode(pool);
    return this;
  }

  /**
   * Get the number of bytes of hidden text.
   *
   * @return number of bytes
   */
  public int length()
  {
    return textByteArray.length;
  }

  /**
   * Normalize textual data.  Assuming that a zone hierarchy has been built
   * and represents the reading order.  This function reorganizes the
   * byteArray textByteArray by gathering the highest level text available
   * in the zone hierarchy.  The text offsets and lengths are recomputed for
   * all the zones in the hierarchy. Separators are inserted where
   * appropriate.
   */
  public void normalize_text()
  {
    ByteVector newTextUTF8 = new ByteVector();
    page_zone.normtext(textByteArray, newTextUTF8);
    textByteArray = newTextUTF8.toByteArray();
  }

  /**
   * Searches the TXT chunk for the given byteArray. If the function manages
   * to find an occurrence of the string, it will return the start of the
   * text.  If no match has been found the retval will be -1.
   *
   * @param zone_list A list of smallest zones covering the text.
   * @param string String to be found. May contain spaces as word separators.
   * @param from Position returned by last search.  If from is out of bounds
   *        of textByteArray it will be set to -1 for searching forward and
   *        textByteArray.length for searching backwards.
   * @param search_fwd TRUE means to search forward. FALSE - backward.
   * @param match_case If set to FALSE the search will be case-insensitive.
   * @param whole_word If set to TRUE the function will try to find a whole
   *        word matching the passed string. The word separators are all
   *        blank and punctuation characters. The passed string may
   *        <b>not</b> contain word separators, that is it <b>must</b> be a
   *        whole word.
   *
   * @return Start of text if found, otherwise -1.
   *
   * @throws IllegalArgumentException if no none-white spaces are specified in the search string
   */
  public int search_string(
    final Vector  zone_list,
    final String  string,
    int           from,
    final boolean search_fwd,
    final boolean match_case,
    final boolean whole_word)
  {
    zone_list.setSize(0);

    byte[] byteArray = null;

    if(isUTF8)
    {
      try
      {
        byteArray =
          (match_case
          ? string
          : (string.toLowerCase())).trim().getBytes("UTF-8");
      }
      catch(final Throwable ignored) {}
    }

    if(byteArray == null)
    {
      byteArray =
        (match_case
        ? string
        : (string.toLowerCase())).trim().getBytes();
    }

    // Make sure there's something left to search for
    if(byteArray.length == 0)
    {
      throw new IllegalArgumentException("DjVuText.one_word");
    }

    if(
      (textByteArray.length == 0)
      || (byteArray.length > textByteArray.length))
    {
      return -1;
    }

    if(search_fwd)
    {
      if((from < 0) || (from >= textByteArray.length))
      {
        from = -1;
      }

      do
      {
        do
        {
          if(++from >= textByteArray.length)
          {
            return -1;
          }
        }
        while((textByteArray[from] & 0xc0) == 0x80);

        find_zones(zone_list, byteArray, from, whole_word, match_case);
      }
      while(zone_list.isEmpty());
    }
    else // search backward
    {
      if((from < 0) || (from >= textByteArray.length))
      {
        from = textByteArray.length;
      }

      while(--from >= 0)
      {
        if((textByteArray[from] & 0xc0) != 0x80)
        {
          find_zones(zone_list, byteArray, from, whole_word, match_case);

          if(!zone_list.isEmpty())
          {
            return from;
          }
        }
      }
    }

    return from;
  }

  /**
   * Searches the TXT chunk for the given byteArray. If the function manages
   * to find an occurrence of the string, it will return the start of the
   * text.  If no match has been found the retval will be -1.  Does not try
   * to match the whole word.
   *
   * @param zone_list A list of smallest zones covering the text.
   * @param string String to be found. May contain spaces as word separators.
   * @param from Position returned by last search.  If from is out of bounds
   *        of textByteArray it will be set to -1 for searching forward and
   *        textByteArray.length for searching backwards.
   * @param search_fwd TRUE means to search forward. FALSE - backward.
   * @param match_case If set to FALSE the search will be case-insensitive.
   *
   * @return Start of text if found, otherwise -1.
   *
   * @throws IllegalArgumentException if no none-white spaces are specified in the search string
   */
  public int search_string(
    final Vector  zone_list,
    final String  string,
    final int     from,
    final boolean search_fwd,
    final boolean match_case)
  {
    return search_string(
      zone_list,
      string,
      from,
      search_fwd,
      match_case,
      false);
  }

  /**
   * Returns end position of the first character in string beyond the the
   * found string, if text contains the same words as the substring in the
   * same order (but possibly with different number of separators between
   * words). The 'separators' in this function are blank and 'end_of_...'
   * characters. If the text is not found then the initial from value will
   * be returned. NOTE, that the returned position may be different from
   * (substring.length+from) because of different number of spaces between
   * words in substring and string.
   *
   * @param substring string to search for
   * @param from start position
   * @param match_case true if case sensative
   *
   * @return end position if the substring is found
   */
  public int startsWith(
    final String        substring,
    final int     from,
    final boolean match_case)
  {
    if(isUTF8)
    {
      try
      {
        return startsWith(
          (match_case
          ? substring
          : (substring.toLowerCase())).trim().getBytes("UTF-8"),
          from,
          match_case);
      }
      catch(final Throwable ignored) {}
    }

    return startsWith(
      (match_case
      ? substring
      : (substring.toLowerCase())).trim().getBytes(),
      from,
      match_case);
  }

  /**
   * Query the entire text layer as a string
   *
   * @return the converted string
   */
  public String toString()
  {
    return getString(0, textByteArray.length);
  }

  // extract a utf8 encoded character from an array of bytes
  private int getChar(
    byte[] byteArray,
    int    pos)
  {
    int value = byteArray[pos++];

    if(!isUTF8)
    {
      return value;
    }

    switch(value & 0xc0)
    {
      case 0x80 :
        throw new IllegalStateException("Invalid UTF8");
      case 0x40 :
        return value;
      default :
        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x800) == 0)
        {
          return value & 0x7ff;
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x10000) == 0)
        {
          return value & 0xffff;
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x200000) == 0)
        {
          return value & 0x1fffff;
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x4000000) == 0)
        {
          return value & 0x3ffffff;
        }

        return (value << 6) | (byteArray[pos++] & 0x7f);
    }
  }

  // Returns TRUE, if the 'ch' is a separator, or is punctuation.
  private static boolean isJavaIdentifier(final char value)
  {
    return Character.isJavaIdentifierPart(value);
  }

  // Returns TRUE, if the 'ch' is a separator, or is punctuation.
  private static boolean isJavaIdentifier(final int value)
  {
    return ((value & 0xffff) == value) && isJavaIdentifier((char)value);
  }

  //*
  // Determine if the first UTF8 character in the byteArrays are
  // identical. Assume that the leading bytes have already been
  // determined to be equal.
  ///
  private static boolean char_equal(
    byte[] first,
    int    firstPos,
    byte[] second,
    int    secondPos)
  {    
    if(first[firstPos] != second[secondPos++])
    {
      return false;
    }

    if((first[firstPos++] & 0xc0) < 0x80)
    {
      return true;
    }

    // skip the first bytes (assumed to be equal)
    while(
      ((first[firstPos] & 0xc0) == 0x80)
      && ((second[secondPos] & 0xc0) == 0x80))
    {
      // both bytes are UTF8 continuation bytes
      if(first[firstPos++] != second[secondPos++])
      {
        return false;
      }
    }

    // All continuation bytes up to this position (if any) agree and 
    // at least one of the byteArrays has run out of continuation bytes.
    // The characters are equal if the current bytes are not
    // continuation bytes.
    return (((first[firstPos] & 0xc0) != 0x80)
    && ((second[secondPos] & 0xc0) != 0x80));
  }

  // test if the given value represents a white space character
  private static boolean isspace(final int value)
  {
    return (value == 0)
    || (((value & 0xffff) == value) && Character.isWhitespace((char)value));
  }

  // Find the next character in a utf8 encoded byte array
  private static int nextChar(
    byte[] byteArray,
    int    pos)
  {
    if(pos < byteArray.length)
    {
      while(++pos < byteArray.length)
      {
        if((byteArray[pos] & 0xc0) != 0x80)
        {
          return pos;
        }
      }
    }

    return pos;
  }

  // find the next UTF8 character
  private static int nextUTF8Char(
    byte[] byteArray,
    int    pos)
    throws UnsupportedEncodingException
  {
    int value = byteArray[pos++];

    switch(value & 0xc0)
    {
      case 0x80 :
        throw new UnsupportedEncodingException("Invalid UTF8");
      case 0x40 :
        return pos;
      default :

        if((byteArray[pos] & 0xc0) != 0x80)
        {
          throw new UnsupportedEncodingException("Invalid UTF8");
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x800) == 0)
        {
          return pos;
        }

        if((byteArray[pos] & 0xc0) != 0x80)
        {
          throw new UnsupportedEncodingException("Invalid UTF8");
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x10000) == 0)
        {
          return pos;
        }

        if((byteArray[pos] & 0xc0) != 0x80)
        {
          throw new UnsupportedEncodingException("Invalid UTF8");
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x200000) == 0)
        {
          return pos;
        }

        if((byteArray[pos] & 0xc0) != 0x80)
        {
          throw new UnsupportedEncodingException("Invalid UTF8");
        }

        value = (value << 6) | (byteArray[pos++] & 0x7f);

        if((value & 0x4000000) == 0)
        {
          return pos;
        }

        if((byteArray[pos] & 0xc0) != 0x80)
        {
          throw new UnsupportedEncodingException("Invalid UTF8");
        }

        return ++pos;
    }
  }

  //*
  // Determine if the substring is contained in the string beginning at
  // location "from". whole_word indicates whether the located substring
  // must begin and end on a word boundary.  If there is a match, return
  // the list of zones that contain the found copy; otherwise, return an
  // empty list.
  ///
  private void find_zones(
    final Vector zone_list,
    final byte[] substring,
    int          from,
    boolean      whole_word,
    boolean      match_case)
  {
    zone_list.setSize(0);

    // startsWith() will return true if the substring beginning at "from" begins
    // with punctuation. This can result in a false match, so we first check
    // that the leading characters are equal before we do the whole substring check.
    if(!char_equal(substring, 0, textByteArray, from))
    {
      if(!match_case)
      {
        final int c0 = getChar(substring, 0);
        final int c1 = getChar(textByteArray, from);
        if(((c0 & 0xffff) != c0) 
            || ((c1 & 0xffff) != c1)
            || (Character.toUpperCase((char)c0) != Character.toUpperCase((char)c1)))
        {
          return;
        }
      }
      else
      {
        return;
      }
    }

    int end = startsWith(substring, from, match_case);

    if(end > from)
    {
      // Match found at this location, get a list of the zones
      // covering the substring.
      find_smallest_zones(zone_list, from, end - from);

      if(!whole_word || (zone_list.size() == 0))
      {
        // Not a whole word search, so we're done. Return the
        // list of zones.
        return;
      }

      // It's a whole word search and the zone isn't empty
      // Get the WORD zone that contains the beginning of the substring
      Zone first = (Zone)zone_list.elementAt(0);

      if(first == null)
      {
        // If we don't have a first element, then either we didn't get a
        // zone list, so return the empty list.
        return;
      }

      // The string may be defined at the character level, if so move
      // up to the WORD level if possible.
      if(first.ztype > WORD)
      {
        first = first.get_parent();

        if(first == null)
        {
          // The character level zone didn't have a parent. This should not happen...
          return;
        }
      }

      if(
        (first.text_start == from)
        || ((first.ztype != WORD)
        && !isJavaIdentifier(
          getChar(
            textByteArray,
            prevChar(textByteArray, from)))))
      // What does this test?
      //    The zone begins at the beginning of the substring OR
      //    if we don't have a WORD zone then we had a separator preceding the
      //    the substring.
      // In either of these cases, we'll say the beginning of the substring is okay and
      // go on to check the end.
      {
        // Get the last WORD zone covering the substring
        Zone last = (Zone)zone_list.elementAt(zone_list.size() - 1);

        if((last != null) && (last.ztype > WORD))
        {
          last = last.get_parent();
        }

        if(
          ((last != null) && ((last.text_start + last.text_length) == end))
          || !isJavaIdentifier(getChar(textByteArray, end)))
        {
          return;
        }
      }

      zone_list.setSize(0);
    }

    return;
  }

  // Find the character after the first non-white place character.
  private int firstEndSpace(
    final byte[] byteArray,
    int          start,
    final int    length)
  {
    for(int pos = start + length; --pos >= start;)
    {
      if(
        ((byteArray[pos] & 0xc0) != 0x80)
        && !isspace(getChar(byteArray, pos)))
      {
        return nextChar(byteArray, pos);
      }
    }

    return start;
  }

  // find the start of the previous character
  private static int prevChar(
    byte[] byteArray,
    int    pos)
  {
    if(pos >= 0)
    {
      while(--pos >= 0)
      {
        if((byteArray[pos] & 0xc0) != 0x80)
        {
          return pos;
        }
      }
    }

    return pos;
  }

  // skip whitespace characters
  private static int skipSpaces(
    final String string,
    int          pos)
  {
    final int length = string.length();

    while(pos < length)
    {
      if(!isspace(string.charAt(pos++)))
      {
        return pos - 1;
      }
    }

    return length;
  }

  //*
  // For the byteArray starting at byteArray_start of length length
  // the function will generate a list of smallest zones of the
  // same type that covers the byteArray and will return it.
  // The list of zones in order.
  ///
  private Vector find_smallest_zones(
    Vector zone_list,
    int    start,
    int    length)
  {
    if(zone_list == null)
    {
      zone_list = new Vector();
    }
    else
    {
      zone_list.setSize(0);
    }

    int end = start + length;
    start   = skipSpaces(textByteArray, start, length);
    end     = firstEndSpace(textByteArray, start, end - start);

    if(start == end)
    {
      return zone_list; // nothing left, return the empty list
    }

    length = end - start;

    for(int zone_type = CHARACTER; zone_type >= PAGE;)
    {
      for(int xstart = start; xstart < end;)
      {
        // Locate the next non-space character. If none, we're
        // finished with the sweep of the byteArray.
        xstart = skipSpaces(textByteArray, xstart, length);

        if(xstart == end)
        {
          break;
        }

        // Locate the smallest zone of the type we're looking at
        // that begins at the current position.
        Zone zone = get_smallest_zone(zone_type, xstart);

        if((zone == null) || (zone_type != zone.ztype))
        {
          // We didn't find one. Move up the type hierarchy and try
          // again. Empty the zone list, first, though.
          zone_type--;
          zone_list.setSize(0);

          break;
        }

        // We found one. Append it to the list and update the
        // the description.
        zone_list.addElement(zone);
        xstart = zone.text_start + zone.text_length;
      }

      if(zone_list.size() != 0)
      {
        // We got all the way through and produced a zone list
        // so we can stop hunting
        return zone_list;
      }
    }

    return zone_list;
  }

  // Find the smallest zone containing the start position.
  private Zone get_smallest_zone(
    int max_type,
    int start)
  {
    if(search_zone(page_zone, start) == start)
    {
      return null;
    }

    Zone zone = page_zone;

    while(zone.ztype < max_type)
    {
      int          pos      = 0;
      final Vector children = zone.children;

      for(; pos < children.size(); ++pos)
      {
        if(search_zone((Zone)children.elementAt(pos), start) > start)
        {
          break;
        }
      }

      if(pos >= children.size())
      {
        break;
      }

      zone = (Zone)children.elementAt(pos);
    }

    return (Zone)zone;
  }

  // Find the next java identifier character
  private int nextJavaIdentifier(
    final byte[] byteArray,
    int          pos)
  {
    while(
      (pos < byteArray.length)
      && !isJavaIdentifier(getChar(byteArray, pos)))
    {
      pos = nextChar(byteArray, pos);
    }

    return pos;
  }

  // find the next space character
  private int nextSpace(
    final String string,
    int          pos)
  {
    final int length = string.length();

    while((pos < length) && !Character.isWhitespace(string.charAt(pos)))
    {
      pos++;
    }

    return pos;
  }

  // find the next space character
  private int nextSpace(
    final byte[] byteArray,
    int          pos)
  {
    while((pos < byteArray.length) && !isspace(getChar(byteArray, pos)))
    {
      pos = nextChar(byteArray, pos);
    }

    return pos;
  }

  //*
  // Will return the position of the first character beyond
  // the zone if the zone contains beginning of the text start,
  // otherwise will return the start position.
  ///
  private int search_zone(
    final Zone zone,
    final int  start)
  {
    final int zoneEnd = zone.text_start + zone.text_length;

    return ((start < zone.text_start) || (start >= zoneEnd))
    ? start
    : zoneEnd;
  }

  // find the next non-space
  private int skipSpaces(
    final byte[] byteArray,
    int          pos,
    final int    length)
  {
    while((pos < length) && isspace(getChar(byteArray, pos)))
    {
      pos = nextChar(byteArray, pos);
    }

    return pos;
  }

  // find the next non-space
  private int skipSpaces(
    final byte[] byteArray,
    int          pos)
  {
    return skipSpaces(byteArray, pos, byteArray.length);
  }

  // This is a more efficient internal version of startsWith, as it
  // takes as input the string already converted to a byte array.
  private int startsWith(
    byte[]        substring,
    final int     from,
    final boolean match_case)
  {
    if(substring.length == 0)
    {
      return from;
    }

    int end = from;
    int pos = 0;

    for(int c0 = getChar(substring, 0); end < textByteArray.length;)
    {
      int c1 = getChar(textByteArray, end);

      if(
        (c0 != c1)
        && (match_case
        || (((c0 & 0xffff) != c0) || ((c1 & 0xffff) != c1)
        || (Character.toUpperCase((char)c0) != Character.toUpperCase(
          (char)c1)))))
      {
        return from;
      }

      pos   = nextChar(substring, pos);
      end   = nextChar(textByteArray, end);

      if(pos >= substring.length)
      {
        return end;
      }

      c0 = getChar(substring, pos);

      if(!isJavaIdentifier(c0))
      {
        c1 = getChar(textByteArray, end);

        if(isJavaIdentifier(c1))
        {
          return from;
        }

        if(isspace(c0))
        {
          pos = skipSpaces(substring, pos);

          if(pos >= substring.length)
          {
            return end;
          }

          c0 = getChar(substring, pos);

          if(isJavaIdentifier(c0))
          {
            end = nextJavaIdentifier(textByteArray, end);
          }
          else
          {
            end = skipSpaces(textByteArray, end);
          }
        }
      }
    }

    return from;
  }

  //~ Inner Classes ----------------------------------------------------------

  /**
   * Data structure representing document textual components. The text
   * structure is represented by a hierarchy of rectangular zones.
   */
  public static class Zone
    extends GRect
  {
    //~ Static fields/initializers -------------------------------------------

    // I think this indicates the version of text decoding being used.
    private static final int version = 1;

    //~ Instance fields ------------------------------------------------------

    /** List of children zone. */
    public Vector children = new Vector();

    /**
     * Controls whether separators are added between lexical elements. This
     * is included to handle differences in languages. In English, for
     * example, words are separated by spaces and when searching, the spaces
     * are significant. In Japanese, there are no spaces and words may also
     * be broken between lines.  We would expect add_separators to be true
     * for English (default) and false for Japanese.
     */
    public boolean add_separators = true;

    /** Length of the zone text in substring textByteArray. */
    public int text_length = 0;

    /** Position of the zone text in substring textByteArray. */
    public int text_start = 0;

    /** Type of the zone. */
    public int   ztype       = DjVuText.PAGE;
    private Zone zone_parent = null;

    //~ Constructors ---------------------------------------------------------

    /**
     * Creates a new Zone object.
     */
    public Zone() {}

    //~ Methods --------------------------------------------------------------

    /**
     * Appends another subzone inside this zone.  The new zone is initialized
     * with an empty rectangle, empty text, and has the same type as this
     * zone.
     *
     * @return DOCUMENT ME!
     */
    public Zone append_child()
    {
      final Zone empty = new Zone();
      empty.ztype            = ztype;
      empty.add_separators   = add_separators; // This level's value is the next's default
      empty.zone_parent      = this;
      children.addElement(empty);

      return empty;
    }

    /**
     * Find the zones used by the specified substring and append them to the
     * list.
     *
     * @param list vector to append zones to
     * @param start byte position to list
     * @param end byte position to list
     */
    public void append_zones(
      final Vector list,
      final int    start,
      final int    end)
    {
      final int text_end = text_start + text_length;

      if(text_start >= start)
      {
        if(text_end <= end)
        {
          list.addElement(this);
        }
        else if(text_start < end)
        {
          if(children.size() > 0)
          {
            int pos = 0;

            do
            {
              ((Zone)children.elementAt(pos++)).append_zones(
                list,
                start,
                end);
            }
            while(children.size() > pos);
          }
          else
          {
            list.addElement(this);
          }
        }
      }
      else if(text_end > start)
      {
        for(int pos = 0; pos < children.size();)
        {
          ((Zone)children.elementAt(pos++)).append_zones(list, start, end);
        }
      }
    }

    /**
     * Find out this Zone's parent.
     *
     * @return the parent Zone
     */
    public Zone get_parent()
    {
      return zone_parent;
    }

    /**
     * Finds the smallest rectangles and appends them to the list.
     *
     * @param list vector to append zones to
     */
    public void get_smallest(final Vector list)
    {
      if(children.size() > 0)
      {
        int pos = 0;

        do
        {
          ((Zone)children.elementAt(pos++)).get_smallest(list);
        }
        while(children.size() > pos);
      }
      else
      {
        list.addElement(this);
      }
    }

    /**
     * Finds the smallest rectangles and appends them to the list after
     * padding the smallest unit to fit width or height for the parent
     * rectangle and adding the number of specified pixels.
     *
     * @param list vector to append zones to
     * @param padding number of pixels to expand each zone by
     */
    public void get_smallest(
      Vector    list,
      final int padding)
    {
      if(children.size() > 0)
      {
        int pos = 0;

        do
        {
          ((Zone)children.elementAt(pos++)).get_smallest(list, padding);
        }
        while(children.size() > pos);
      }
      else if((zone_parent != null) && (zone_parent.ztype >= PARAGRAPH))
      {
        final GRect xrect = zone_parent;

        if(xrect.height() < xrect.width())
        {
          list.addElement(
            new GRect(
              xmin - padding,
              xrect.ymin - padding,
              width() + (2 * padding),
              xrect.height() + (2 * padding)));
        }
        else
        {
          list.addElement(
            new GRect(
              xrect.xmin - padding,
              ymin - padding,
              xrect.width() + (2 * padding),
              height() + (2 * padding)));
        }
      }
      else
      {
        list.addElement(
          new GRect(
            xmin - padding,
            ymin - padding,
            width() + (2 * padding),
            height() + (2 * padding)));
      }
    }

    /**
     * Find the text_start and text_end indicated by the given box.
     *
     * @param box DOCUMENT ME!
     * @param byteArray_start DOCUMENT ME!
     * @param byteArray_end DOCUMENT ME!
     */
    public void get_text_with_rect(
      final GRect box,
      NumContext  byteArray_start,
      NumContext  byteArray_end)
    {
      final boolean hasChildren = (children.size() > 0);

      if(hasChildren
        ? box.contains(this)
        : intersects_zone(box, this))
      {
        final int text_end = text_start + text_length;

        if(byteArray_start.intValue() == byteArray_end.intValue())
        {
          byteArray_start.set(text_start);
          byteArray_end.set(text_end);
        }
        else
        {
          if(byteArray_end.intValue() < text_end)
          {
            byteArray_end.set(text_end);
          }

          if(text_start < byteArray_start.intValue())
          {
            byteArray_start.set(text_start);
          }
        }
      }
      else if(hasChildren && intersects_zone(box, this))
      {
        int pos = 0;

        do
        {
          ((Zone)children.elementAt(pos)).get_text_with_rect(
            box,
            byteArray_start,
            byteArray_end);
        }
        while((++pos) < children.size());
      }
    }

    // Query if a zone and a rectangle intersect
    private static boolean intersects_zone(
      final GRect box,
      final GRect zone)
    {
      return ((box.xmin < zone.xmin)
      ? (box.xmax >= zone.xmin)
      : (box.xmin <= zone.xmax))
      && ((box.ymin < zone.ymin)
      ? (box.ymax >= zone.ymin)
      : (box.ymin <= zone.ymax));
    }

    // Clear all the text in this zone
    private void cleartext()
    {
      text_start    = 0;
      text_length   = 0;

      for(int i = 0; i < children.size(); ++i)
      {
        ((Zone)children.elementAt(i)).cleartext();
      }
    }

    // decode this zone from the text data
    private void decode(
      InputStream bs,
      int         maxtext,
      Zone        parent,
      Zone        prev)
      throws IOException
    {
      // Decode type
      ztype = bs.read();

      if((ztype < PAGE) || (ztype > CHARACTER))
      {
        throw new IOException("DjVuText.corrupt_text");
      }

      // Decode coordinates
      xmin = bs.read() << 8;
      xmin |= bs.read();
      xmin -= 0x8000;

      ymin = bs.read() << 8;
      ymin |= bs.read();
      ymin -= 0x8000;

      int width = bs.read() << 8;
      width |= bs.read();
      width -= 0x8000;

      int height = bs.read() << 8;
      height |= bs.read();
      height -= 0x8000;

      // Decode text info
      text_start = bs.read() << 8;
      text_start |= bs.read();
      text_start -= 0x8000;

      // int start=text_start;
      text_length = bs.read() << 16;
      text_length |= (bs.read() << 8);
      text_length |= bs.read();

      if(prev != null)
      {
        if((ztype == PAGE) || (ztype == PARAGRAPH) || (ztype == LINE))
        {
          xmin += prev.xmin;
          ymin = prev.ymin - (ymin + height);
        }
        else // Either COLUMN or WORD or CHARACTER
        {
          xmin += prev.xmax;
          ymin += prev.ymin;
        }

        text_start += (prev.text_start + prev.text_length);
      }
      else if(parent != null)
      {
        xmin += parent.xmin;
        ymin = parent.ymax - (ymin + height);
        text_start += parent.text_start;
      }

      xmax   = xmin + width;
      ymax   = ymin + height;

      // Get children size
      int size = bs.read() << 16;
      size |= (bs.read() << 8);
      size |= bs.read();

      // Checks
      if(
        isEmpty()
        || (text_start < 0)
        || ((text_start + text_length) > maxtext))
      {
        throw new IOException("DjVuText.corrupt_text");
      }

      // Process children
      Zone prev_child = null;
      children.setSize(0);

      while(size-- > 0)
      {
        Zone z = append_child();
        z.decode(bs, maxtext, this, prev_child);
        prev_child = z;
      }
    }

    private void decode(
      InputStream bs,
      int         maxtext,
      Zone        parent)
      throws IOException
    {
      decode(bs, maxtext, parent, null);
    }

    // decode this zone from the text data
    private void decode(
      InputStream bs,
      int         maxtext)
      throws IOException
    {
      decode(bs, maxtext, null, null);
    }

    // convert text to use standard separators
    private void normtext(
      final byte[]     instr,
      final ByteVector outstr)
    {
      if(text_length == 0)
      {
        // Descend collecting text below
        text_start = outstr.size();

        for(int i = 0; i < children.size(); ++i)
        {
          ((Zone)children.elementAt(i)).normtext(instr, outstr);
        }

        text_length = outstr.size() - text_start;

        // Ignore empty zones
        if(text_length == 0)
        {
          return;
        }
      }
      else
      {
        final int outLength = outstr.size();

        for(int i = 0, j = text_start; (i++) < text_length;)
        {
//          outstr.addByte(instr[j++]);
          outstr.write(instr[j++]);
        }

        text_start = outLength;

        // Clear textual information on lower level nodes
        for(int i = 0; i < children.size(); ++i)
        {
          ((Zone)children.elementAt(i)).cleartext();
        }
      }

      // Determine standard separator
      int sep;

      switch(ztype)
      {
        case COLUMN :
          sep = end_of_column;

          break;
        case REGION :
          sep = end_of_region;

          break;
        case PARAGRAPH :
          sep = end_of_paragraph;

          break;
        case LINE :
          sep = end_of_line;

          break;
        case WORD :
          sep = ' ';

          break;
        case CHARACTER :default :
          return;
      }

      // Add separator if not present yet.
      if(outstr.getByte(outstr.size() - 1) != sep)
      {
        outstr.write((byte)sep);
        text_length++;
      }
    }
  }
  
  private class ByteVector extends ByteArrayOutputStream
  {
    public int size() { return count; }
    public int getByte(int loc) { return buf[loc]; }
  }
}

