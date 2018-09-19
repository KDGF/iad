package com.kdg.gnome.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.kdg.gnome.ipserver.IPLocation;
import com.kdg.gnome.ipserver.IPLocationDB;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class IpUtil {

    private static IPLocation ipLocation;


    //IP地址返回国家编码
    public static String getCountryCode(String ip) {
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }

        return ipLocation.getLocation(ip).getCountryCode();
    }

    // IP地址返回国家名称
    public static String getCountryName(String ip) {
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }
        return ipLocation.getLocation(ip).getCountryName();
    }

    // IP地址返回省编码
    public static String getProvinceCode(String ip) {
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }

        return ipLocation.getLocation(ip).getProvinceCode();
    }

    // IP地址返回省名称
    public static String getProvinceName(String ip) {
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }
        return ipLocation.getLocation(ip).getProvinceName();
    }

    // IP地址返回城市编码
    public static String getCityCode(String ip) {
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }

        return ipLocation.getLocation(ip).getCode();
    }
    
    //IP地址返回城市名称
    public static String getCityName(String ip) {
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }
        return ipLocation.getLocation(ip).getCity();
    }

    // IP地址返回前三位IP段
    public static String getSectionIp(String ip) {
        String ips = null;
        if (ip != null) {
            ips = ip.substring(ip.lastIndexOf(".") + 1,ip.length());
        }
        return ips;
    }


    public static  Boolean checkIpBlackList(String ip){
        if (ipLocation == null) {
            ipLocation = IPLocationDB.ipLocation;
        }
        Boolean result = ipLocation.exitInBlackList(ip);
        return result;
    }
    
    /** 
     * 日志 
     */  
    private static final Logger log = Logger.getLogger(IpUtil.class);
  
    /** 
     * 单网卡名称 
     */  
    private static final String NETWORK_CARD = "eth0";
    private static final String NETWORK_CARD1 = "eth1";
  
    /** 
     * 绑定网卡名称 
     */  
    private static final String NETWORK_CARD_BAND = "bond0";  
  
    /** 
     *  
     * Description: 得到本机名<br> 
     * @return  
     * @see 
     */  
    public static String getLocalHostName() {  
        try {  
            InetAddress addr = InetAddress.getLocalHost();  
            return addr.getHostName();  
        }  
        catch (Exception e) {  
            log.error("IpGetter.getLocalHostName出现异常！异常信息：" + e.getMessage());  
            return "";  
        }  
    }  
  
    /** 
     * Description: linux下获得本机IPv4 IP<br> 
     * @return  
     * @see 
     */  
    public static String getLocalIP() {  
        String ip = "";  
        try {
            Enumeration<NetworkInterface> e1 = (Enumeration<NetworkInterface>)NetworkInterface.getNetworkInterfaces();  
            while (e1.hasMoreElements()) {
                NetworkInterface ni = e1.nextElement();  
  
                //单网卡或者绑定双网卡  
                if ((NETWORK_CARD.equals(ni.getName()))  
                	|| (NETWORK_CARD1.equals(ni.getName()))  
                    || (NETWORK_CARD_BAND.equals(ni.getName()))) {  
                    Enumeration<InetAddress> e2 = ni.getInetAddresses();  
                    while (e2.hasMoreElements()) {  
                        InetAddress ia = e2.nextElement();  
                        if (ia instanceof Inet6Address) {  
                            continue;  
                        }  
                        ip = ia.getHostAddress();  
                    }
                    if(StringUtils.isNotEmpty(ip)) {
                    	break;  
                    }
                }  
                else {  
                    continue;  
                }  
            } 
            return ip;  
        }  
        catch (SocketException e) {  
            log.error("IpGetter.getLocalIP出现异常！异常信息：" + e.getMessage());  
            return ip;
        }  
    } 
}
