package com.kdg.gnome.util;

import java.nio.charset.Charset;

/**
 * @author mingzhang2
 */
public class Bytes {

  /**
   * When we encode strings, we always specify UTF8 encoding
   */
  private static final String UTF8_ENCODING = "UTF-8";

  /**
   * When we encode strings, we always specify UTF8 encoding
   */
  private static final Charset UTF8_CHARSET = Charset.forName(UTF8_ENCODING);

  /**
   * Converts a string to a UTF-8 byte array.
   *
   * @param s string
   * @return the byte array
   */
  public static byte[] toBytes(String s) {
    return s.getBytes(UTF8_CHARSET);
  }

  /**
   * @param b Presumed UTF-8 encoded byte array.
   * @return String made from <code>b</code>
   */
  public static String toString(final byte [] b) {
    if (b == null) {
      return null;
    }
    return toString(b, 0, b.length);
  }

  /**
   * This method will convert utf8 encoded bytes into a string. If the given byte array is null,
   * this method will return null.
   * @param b Presumed UTF-8 encoded byte array.
   * @param off offset into array
   * @return String made from <code>b</code> or null
   */
  public static String toString(final byte[] b, int off) {
    if (b == null) {
      return null;
    }
    int len = b.length - off;
    if (len <= 0) {
      return "";
    }
    return new String(b, off, len, UTF8_CHARSET);
  }

  /**
   * This method will convert utf8 encoded bytes into a string. If
   * the given byte array is null, this method will return null.
   *
   * @param b Presumed UTF-8 encoded byte array.
   * @param off offset into array
   * @param len length of utf-8 sequence
   * @return String made from <code>b</code> or null
   */
  public static String toString(final byte [] b, int off, int len) {
    if (b == null) {
      return null;
    }
    if (len == 0) {
      return "";
    }
    return new String(b, off, len, UTF8_CHARSET);
  }
}
