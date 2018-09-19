package com.kdg.gnome.adx.test;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
/**
 * AES加密解密类
 *
 * @author xuxiong
 */
public class AESHelper {
    /** 算法/模式/填充 **/
    private static final String CipherMode = "AES/ECB/NoPadding";
    /**
     * 创建密钥
     *
     * @param password
     *            例如："0123456701234567" 128位 16*8 所有密钥长度不能超过16字符中文占两个。192 24；
     *            256 32
     * @return SecretKeySpec 实例
     */
    private static SecretKeySpec createKey(String password) {
        byte[] data = null;
        if (password == null) {
            password = "";
        }
        StringBuffer sb = new StringBuffer(16);
        sb.append(password);
        while (sb.length() < 16) {
            sb.append("0");
        }
        if (sb.length() > 16) {
            sb.setLength(16);
        }
        try {
            data = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new SecretKeySpec(data, "AES");
    }
    /**
     * 加密字节数据
     *
     * @param content
     *            需要加密的字节数组
     * @param password
     *            密钥 128 <16个字节 192 <24,256 <32个字节
     * @return 加密完后的字节数组
     */
    public static byte[] encrypt(byte[] content, String password) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 加密(结果为16进制字符串)
     *
     * @param content
     *            要加密的字符串
     * @param password
     *            密钥
     * @return 加密后的16进制字符串
     */
    public static String encrypt(String content, String password) {
        byte[] data = null;
        try {
            data = content.getBytes("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = encrypt(data, password);
        String result = byte2hex(data);
        return result;
    }
    /** 解密字节数组 **/
    public static byte[] decrypt(byte[] content, String password) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CipherMode);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] result = cipher.doFinal(content);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /** 解密16进制的字符串为字符串 **/
    public static String decrypt(String content, String password) {
        byte[] data = null;
        try {
            data = hex2byte(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        data = decrypt(data, password);
        if (data == null)
            return null;
        String result = null;
        try {
            result = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
    /**
     * 字节数组转成16进制字符串
     *
     * @param b
     * @return 16进制字符串
     */
    public static String byte2hex(byte[] b) { // 一个字节的数，
        StringBuffer sb = new StringBuffer(b.length * 2);
        String tmp = "";
        for (int n = 0; n < b.length; n++) {
            // 整数转成十六进制表示
            tmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
        }
        return sb.toString().toUpperCase(); // 转成大写
    }
    /**
     * 将hex字符串转换成字节数组 *
     *
     * @param inputString
     * @return 字节数组
     */
    private static byte[] hex2byte(String inputString) {
        if (inputString == null || inputString.length() < 2) {
            return new byte[0];
        }
        inputString = inputString.toLowerCase();
        int l = inputString.length() / 2;
        byte[] result = new byte[l];
        for (int i = 0; i < l; ++i) {
            String tmp = inputString.substring(2 * i, 2 * i + 2);
            result[i] = (byte) (Integer.parseInt(tmp, 16) & 0xFF);
        }
        return result;
    }

    public static void main(String[] args) {

        String key = "d6797b0f95f8ead1";

        encrypt("2000|1482230774".getBytes(), key);

        System.out.println(encrypt("2000|1482230774".getBytes(), key));
    }
}