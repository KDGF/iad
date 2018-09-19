package com.kdg.gnome.ipserver;

import java.net.UnknownHostException;

/**
 * 类描述：ip地址转换工具
 *
 * @author: niuyb
 */
public class IPConvert {
    public byte[] getIpByteArrayFromString(String ip) {
        byte[] ret = new byte[4];
        if (ip != null) {
            java.util.StringTokenizer st = new java.util.StringTokenizer(ip, ".");
            try {
                ret[0] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
                ret[1] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
                ret[2] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
                ret[3] = (byte) (Integer.parseInt(st.nextToken()) & 0xFF);
            } catch (Exception e) {
                // System.out.println(e.getMessage());
            }
        }
        return ret;
    }

    public int str2Ip(String ip) throws UnknownHostException {
        byte[] bytes = getIpByteArrayFromString(ip);
        int a, b, c, d;
        a = byte2int(bytes[0]);
        b = byte2int(bytes[1]);
        c = byte2int(bytes[2]);
        d = byte2int(bytes[3]);
        int result = (a << 24) | (b << 16) | (c << 8) | d;
        return result;
    }

    public int byte2int(byte b) {
        int l = b & 0x07f;
        if (b < 0) {
            l |= 0x80;
        }
        return l;
    }

    public long ip2long(String ip) throws UnknownHostException {
        int ipNum = str2Ip(ip);
        return int2long(ipNum);
    }

    public long int2long(int i) {
        long l = i & 0x7fffffffL;
        if (i < 0) {
            l |= 0x080000000L;
        }
        return l;
    }

    public String long2ip(long ip) {
        int[] b = new int[4];
        b[0] = (int) ((ip >> 24) & 0xff);
        b[1] = (int) ((ip >> 16) & 0xff);
        b[2] = (int) ((ip >> 8) & 0xff);
        b[3] = (int) (ip & 0xff);
        String x;
        Integer p;
        p = new Integer(0);
        x = p.toString(b[0]) + "." + p.toString(b[1]) + "." + p.toString(b[2]) + "." + p.toString(b[3]);

        return x;

    }
}
