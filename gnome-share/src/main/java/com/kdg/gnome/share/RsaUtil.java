//package com.iflytek.gnome.share;
//
//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;
//
//import javax.crypto.BadPaddingException;
//import javax.crypto.Cipher;
//import javax.crypto.IllegalBlockSizeException;
//import javax.crypto.NoSuchPaddingException;
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.security.*;
//import java.security.spec.InvalidKeySpecException;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.security.spec.X509EncodedKeySpec;
//
///**
// * Created by qlzhang on 2/27/2016.
// */
//
//public class RsaUtil {
//  private static String pubKey;
//  private static String priKey;
//  private static String ALGORITHM = "RSA";
//
//  private static BASE64Encoder encoder = new BASE64Encoder();
//  private static BASE64Decoder decoder = new BASE64Decoder();
//  /**
//   * 生成RSA对称密钥
//   * @param keyLen
//   * @throws NoSuchAlgorithmException
//   * @throws UnsupportedEncodingException
//   */
//  public static void generateKeyPair(int keyLen) throws NoSuchAlgorithmException, UnsupportedEncodingException {
//    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
//    keyPairGenerator.initialize(keyLen);
//    KeyPair keyPair = keyPairGenerator.generateKeyPair();
//    PublicKey publicKey = keyPair.getPublic();
//    PrivateKey privateKey = keyPair.getPrivate();
//
//    pubKey = encoder.encode(publicKey.getEncoded());
//    priKey = encoder.encode(privateKey.getEncoded());
//    System.out.println("key length (bit): " + keyLen);
//    System.out.println("---------------------------");
//    System.out.println("public key: " + pubKey);
//    System.out.println("---------------------------");
//    System.out.println("private key: " + priKey);
//    System.out.println("=============================");
//
//  }
//
//  /**
//   * 将字符串形式的private key转换成PrivateKey结构
//   * @param priKey
//   * @return
//   * @throws NoSuchAlgorithmException
//   * @throws IOException
//   * @throws InvalidKeySpecException
//   */
//  public static PrivateKey getPrivatekeyFromStr(String priKey) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
//    KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
//
//    PKCS8EncodedKeySpec keySpecPri = new PKCS8EncodedKeySpec(decoder.decodeBuffer(priKey));
//    return keyFactory.generatePrivate(keySpecPri);
//  }
//
//  /**
//   * 将字符串形式的public key转换成PublicKey结构
//   * @param pubKey
//   * @return
//   * @throws NoSuchAlgorithmException
//   * @throws IOException
//   * @throws InvalidKeySpecException
//   */
//  public static PublicKey getPublicKeyFromStr(String pubKey) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
//    KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
////    BASE64Decoder decoder = new BASE64Decoder();
//    X509EncodedKeySpec keySpecPub = new X509EncodedKeySpec(decoder.decodeBuffer(pubKey));
//    return keyFactory.generatePublic(keySpecPub);
//  }
//
//  /**
//   * 将明文数组根据公钥加密为密文
//   * @param text
//   * @param publicKey
//   * @return
//   * @throws NoSuchPaddingException
//   * @throws NoSuchAlgorithmException
//   * @throws InvalidKeyException
//   * @throws BadPaddingException
//   * @throws IllegalBlockSizeException
//   */
//  public static String encrypt(byte[] text, PublicKey publicKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//    final Cipher cipher = Cipher.getInstance(ALGORITHM);
//    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//    return encoder.encode(cipher.doFinal(text));
//  }
//
//  /**
//   * 将密文字节数组根据PrivateKey转换为明文
//   * @param text
//   * @param privateKey
//   * @return
//   * @throws NoSuchPaddingException
//   * @throws NoSuchAlgorithmException
//   * @throws InvalidKeyException
//   * @throws BadPaddingException
//   * @throws IllegalBlockSizeException
//   */
//  public static String decrypt(String text, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
//    final Cipher cipher = Cipher.getInstance(ALGORITHM);
//    cipher.init(Cipher.DECRYPT_MODE, privateKey);
//    return  new String(cipher.doFinal(decoder.decodeBuffer(text)),"utf-8");
//  }
//
//  public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {
//    generateKeyPair(1024);
//    PublicKey publicKey = getPublicKeyFromStr(pubKey);
//    PrivateKey privateKey = getPrivatekeyFromStr(priKey);
//
//    String text = "13705600663";
//    byte[] textBytes = text.getBytes("utf-8");
//
//    String pnumEn = encrypt(textBytes, publicKey);
//    System.out.println("crypt text: " + pnumEn);
//
//    String pnumDe = decrypt(pnumEn, privateKey);
//    System.out.println("=============================");
//
//    System.out.println("plain text: " + pnumDe);
//  }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
