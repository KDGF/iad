package com.kdg.gnome.share.task;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * AES static method
 * <p>
 * 加密分为两部分：AES加密（模式为AES/ECB/PKCS5Padding，密钥128bit长） +
 * Base64编码(needUrlSafe为true表示URL安全编码，false表示普通编码）
 * <p>
 * 解密分为两部分：Base64解码（isUrlSafe为true表示URL安全解码，false表示普通解码） + AES解密（模式为AES/ECB/PKCS5Padding，密钥128bit长）
 *
 * @author xmzheng
 */

public class AES {
  private static String algorithm = "AES";
  private static String cipher_algorithm = "AES/ECB/PKCS5Padding";

  public static String encrypt(String plainText, String keyValue, boolean needUrlSafe)
    throws Exception {
    Key key = generateKey(keyValue);
    // Cipher chiper = Cipher.getInstance(algorithm);
    Cipher chiper = Cipher.getInstance(cipher_algorithm);
    chiper.init(Cipher.ENCRYPT_MODE, key);
    byte[] encVal = chiper.doFinal(plainText.getBytes("utf-8"));
    String encryptedValue =
      needUrlSafe ? Base64.encodeBase64URLSafeString(encVal) : Base64.encodeBase64String(encVal);
    return encryptedValue;
  }

  public static String decrypt(String encryptedText, String keyValue, boolean isUrlSafe)
    throws Exception {
    Key key = generateKey(keyValue);
    Cipher chiper = Cipher.getInstance(algorithm);
    // Cipher chiper = Cipher.getInstance(cipher_algorithm);
    chiper.init(Cipher.DECRYPT_MODE, key);
    byte[] decordedValue = Base64.decodeBase64(encryptedText);
    byte[] decValue = chiper.doFinal(decordedValue);
    String decryptedValue = new String(decValue, "utf-8");
    return decryptedValue;
  }

  private static Key generateKey(String keyValue) throws Exception {
    byte[] sk = keyValue.getBytes();
    Key key = new SecretKeySpec(sk, algorithm);
    return key;
  }

  static {
    //首次调用AES模块 系统加载加解密模块需耗时1s 会影响业务卡顿  在系统启动时加载 规避对业务的影响
    try {
      String key = "0123456789012345";
      String plain = "pre load AES module";
      // 普通BASE64
      String baseEncrypt = encrypt(plain, key, false);
      String baseDecrypt = decrypt(baseEncrypt, key, false);
      System.out.println("System pre load AES module...");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static void main(String[] args) throws Exception {
    // String key = "f36da0353fa3bc04";
    String key = "iflytekadx000001";
    String plain = "0.56";

    String ifa = "172B2933-FC0C-4BD0-B21A-452CAA0CF643";
    String pnumber = "18656537654";
    String dpid = "b35aa9bbfe2d274c39b54bd6d675055197992556";
    String did = "99000520157509";
    String mac = "68:df:dd:69:8a:bc";

    System.out.println("ifa:" + AES.encrypt(ifa, key, true));
    System.out.println("pnumber:" + AES.encrypt(pnumber, key, true));
    System.out.println("dpid:" + AES.encrypt(dpid, key, true));
    System.out.println("did:" + AES.encrypt(did, key, true));
    System.out.println("mac:" + AES.encrypt(mac, key, true));

    // 普通BASE64
    String baseEncrypt = AES.encrypt(plain, key, false);
    String baseDecrypt = AES.decrypt(baseEncrypt, key, false);
    System.out.println("baseEncrypt = " + baseEncrypt);
    System.out.println("baseDecrypt = " + baseDecrypt);

    // URL安全BASE64
    String urlsafeEncrypt = AES.encrypt(plain, key, true);
    String urlsafeDecrypt = AES.decrypt(urlsafeEncrypt, key, true);
    System.out.println("urlsafeEncrypt = " + urlsafeEncrypt);
    System.out.println("urlsafeDecrypt = " + urlsafeDecrypt);

    String wifiEncoded =
      "Jw94OzFaX8K5X5rjX1g8g8xmX8-5JVE8Nw94OzK9g1XRYIE5F5uQB1gTgUo3B_Xag_YvgIumgIK9X_c5X5xVFIg8g8F5FzKDdzkLFzk3X_r5FVE9FNKis5yDdzkLX84Gg8JUg8uVg_xwg8KrBIFQKI9MX8u3K4E1B_9qgxF5FVMpOIr3XqYiJ_awXljQX84WXIrRt8rjglYaJUaQg_kVBIXQX_umE5geOIrRB1E4X8-aX_T9XUE8BIrQX_XjX5K8X8rmF8-RBzK8XUuGF8kmg5x";


    System.out.println(new String(Base64.decodeBase64(wifiEncoded)));
    String plainStr = "GNzSWC_vzGrGRTsc8ObsSA";
    String keyStr = "6F67C6BC42DF035ABD5F3C61502F23AB";

    System.out.println(AES.decrypt(plainStr, keyStr, true));

  }
}
