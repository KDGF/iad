package com.kdg.gnome.share;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Chienlung on 2/29/2016.
 */
public class AesUtil {
  private static String ALGORITHM = "AES";
  private static String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

  public static String encrypt(String plaintext, String aesToken)
    throws UnsupportedEncodingException, NoSuchPaddingException,
    NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
    IllegalBlockSizeException {
    Key key = new SecretKeySpec(aesToken.getBytes("utf-8"), ALGORITHM);
    Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] ciphertext = cipher.doFinal(plaintext.getBytes("utf-8"));
    return Base64.encodeBase64URLSafeString(ciphertext);
  }

  public static String decrypt(String ciphertext, String aesToken)
    throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
    InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
    Key key = new SecretKeySpec(aesToken.getBytes("utf-8"), ALGORITHM);
    Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, key);
    byte[] plaintext = cipher.doFinal(Base64.decodeBase64(ciphertext));
    return new String(plaintext, "utf-8");
  }

  public static void main(String[] args) throws NoSuchPaddingException, IOException,
    IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
    InvalidKeyException {
    String aesToken = "0123456789012345"; // 16字节
    String plaintext = "13800000000";
    System.out.println("明文：" + plaintext + ", aesToken: " + aesToken);
    String ciphertext = AesUtil.encrypt(plaintext, aesToken);
    System.out.println("加密后密文：" + ciphertext);
    System.out.println("解密后明文：" + AesUtil.decrypt(ciphertext, aesToken));
  }
}
