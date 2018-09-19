package com.kdg.gnome.adx.test;

/**
 * Created by hbwang on 2018/1/8
 */

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class Main {




    public static String ALGORITHM = "AES";
    private static String AES_CBS_PADDING = "AES/CBC/PKCS5Padding";

    public static byte[] encrypt(final byte[] key, final byte[] IV, final byte[] message) throws Exception {
        return encryptDecrypt(Cipher.ENCRYPT_MODE, key, IV, message);
    }

    public static byte[] decrypt(final byte[] key, final byte[] IV, final byte[] message) throws Exception {
        return encryptDecrypt(Cipher.DECRYPT_MODE, key, IV, message);
    }

    private static byte[] encryptDecrypt(final int mode, final byte[] key, final byte[] IV, final byte[] message)
            throws Exception {
        final Cipher cipher = Cipher.getInstance(AES_CBS_PADDING);
        final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        final IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(mode, keySpec, ivSpec);
        return cipher.doFinal(message);
    }

    private static int AES_128 = 128;
    public static void main(String[] args) throws Exception {
//        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
//        keyGenerator.init(AES_128);
//        //Generate Key
//        SecretKey key = keyGenerator.generateKey();
//        //Initialization vector
//        SecretKey IV = keyGenerator.generateKey();
//
////        String randomString = UUID.randomUUID().toString().substring(0, 16);
//        String  randomString = "2000|1482230774";
//        System.out.println("1. Message to Encrypt: " + randomString);
//
//        byte[] cipherText = encrypt(key.getEncoded(), IV.getEncoded(), randomString.getBytes());
//        System.out.println("2. Encrypted Text: " + Base64.getEncoder().encodeToString(cipherText));
//
//        byte[] decryptedString = decrypt(key.getEncoded(), IV.getEncoded(), cipherText);
//        System.out.println("3. Decrypted Message : " + new String(decryptedString));
//
//
//
//        System.out.println("y16x+dDPjq8YQ58MSqRans5K2Ykmk24oS1+dx12eLcg=".length());


        Date date = new Date();



        System.out.println(date );
    }
}