package com.kdg.gnome.share;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA {

  public static String SHA1(String inStr) {
    MessageDigest md = null;
    String outStr = null;
    try {
      md = MessageDigest.getInstance("SHA-1");
      byte[] digest = md.digest(inStr.getBytes());
      outStr = bytetoString(digest);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return outStr;
  }

  /**
   *
   * @param digest
   * @return
   * @deprecated Use Bytes.toString(final byte [] b)
   */
  @Deprecated
  public static String bytetoString(byte[] digest) {
    String str = "";
    String tempStr = "";

    for (int i = 0; i < digest.length; i++) {
      tempStr = (Integer.toHexString(digest[i] & 0xff));
      if (tempStr.length() == 1) {
        str = str + "0" + tempStr;
      } else {
        str = str + tempStr;
      }
    }
    return str;
  }

  public static void main(String[] args) {
    System.out.println("SHA:" + SHA.SHA1("hello sha"));
  }
}
