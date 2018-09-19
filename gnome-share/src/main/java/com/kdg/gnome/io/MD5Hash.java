package com.kdg.gnome.io;

import com.kdg.gnome.util.Bytes;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author mingzhang2
 */
public class MD5Hash {
  public static final int MD5_LEN = 16;

  private static final ThreadLocal<MessageDigest> DIGESTER_FACTORY = new ThreadLocal<MessageDigest>() {
    @Override
    protected MessageDigest initialValue() {
      try {
        return MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
  };

  private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6',
      '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  private static final int charToNibble(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'a' && c <= 'f') {
      return 0xa + (c - 'a');
    } else if (c >= 'A' && c <= 'F') {
      return 0xA + (c - 'A');
    } else {
      throw new RuntimeException("Not a hex character: " + c);
    }
  }

  private byte[] digest;

  /**
   * Constructs an MD5Hash.
   */
  public MD5Hash() {
    this.digest = new byte[MD5_LEN];
  }

  /**
   * Constructs an MD5Hash from a hex string.
   */
  public MD5Hash(String hex) {
    setDigest(hex);
  }

  /**
   * Constructs an MD5Hash with a specified value.
   */
  public MD5Hash(byte[] digest) {
    if (digest.length != MD5_LEN)
      throw new IllegalArgumentException("Wrong length: " + digest.length);
    this.digest = digest;
  }

  /**
   * Create a thread local MD5 digester
   */
  public static MessageDigest getDigester() {
    MessageDigest digester = DIGESTER_FACTORY.get();
    digester.reset();
    return digester;
  }

  /**
   * Construct a hash value for a byte array.
   */
  public static MD5Hash digest(byte[] data) {
    return digest(data, 0, data.length);
  }

  /**
   * Construct a hash value for a byte array.
   */
  public static MD5Hash digest(byte[] data, int start, int len) {
    byte[] digest;
    MessageDigest digester = getDigester();
    digester.update(data, start, len);
    digest = digester.digest();
    return new MD5Hash(digest);
  }

  /**
   * Construct a hash value for a String.
   */
  public static MD5Hash digest(String string) {
    return digest(Bytes.toBytes(string));
  }

  /**
   * Get a MD5 string value
   * @param string
   * @return
   */
  public static String Md5(String string) {
    if (StringUtils.isBlank(string))
      return "";
    try {
      return digest(Bytes.toBytes(string)).toString();
    }catch (Exception e) {
      return "";
    }
  }

  /**
   * Sets the digest value from a hex string.
   */
  public void setDigest(String hex) {
    if (hex.length() != MD5_LEN * 2)
      throw new IllegalArgumentException("Wrong length: " + hex.length());
    byte[] digest = new byte[MD5_LEN];
    for (int i = 0; i < MD5_LEN; i++) {
      int j = i << 1;
      digest[i] = (byte) (charToNibble(hex.charAt(j)) << 4 | charToNibble(
          hex.charAt(j + 1)));
    }
    this.digest = digest;
  }

  /**
   * Returns a string representation of this object.
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(MD5_LEN * 2);
    for (int i = 0; i < MD5_LEN; i++) {
      int b = digest[i];
      buf.append(HEX_DIGITS[(b >> 4) & 0xf]);
      buf.append(HEX_DIGITS[b & 0xf]);
    }
    return buf.toString();
  }

  /**
   * Returns true iff <code>o</code> is an MD5Hash whose digest contains the
   * same values.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MD5Hash)) return false;
    MD5Hash other = (MD5Hash) o;
    return Arrays.equals(this.digest, other.digest);
  }

  /**
   * Returns a hash code value for this object.
   * Only uses the first 4 bytes, since md5s are evenly distributed.
   */
  @Override
  public int hashCode() {
    return quarterDigest();
  }

  /**
   * Return a 32-bit digest of the MD5.
   *
   * @return the first 4 bytes of the md5
   */
  public int quarterDigest() {
    int value = 0;
    for (int i = 0; i < 4; i++)
      value |= ((digest[i] & 0xff) << (8 * (3 - i)));
    return value;
  }
}
