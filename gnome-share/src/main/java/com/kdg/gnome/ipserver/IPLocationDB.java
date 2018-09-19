package com.kdg.gnome.ipserver;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 类描述：ip库实现类，创建和查询
 *
 * @author: zhangwei
 */
public class IPLocationDB implements IPLocation {
    private static IPConvert ipConvert = new IPConvert();
    private ConcurrentSkipListMap<Long, IP> db = new ConcurrentSkipListMap<Long, IP>();
    private ConcurrentSkipListMap<String, City> dbC = new ConcurrentSkipListMap<String, City>();
    private static ConcurrentMap<String, String> dbBlack = new ConcurrentHashMap<String, String>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock writeLocks = rwl.writeLock();
    private final Lock readLocks = rwl.readLock();
    private final static Logger log = Logger.getLogger(IPLocationDB.class);
    public static IPLocation ipLocation;

    public static void init() {
        if (ipLocation == null) {
            synchronized (ipConvert) {
                ipLocation = new IPLocationDB();

                String path = Thread.currentThread().getContextClassLoader().getResource("ipbasics.csv").getPath();
                String pathC = Thread.currentThread().getContextClassLoader().getResource("city.csv").getPath();
                String pathBlack = Thread.currentThread().getContextClassLoader().getResource("IPBlackList.csv").getPath();
//                String pathBlack2 = Thread.currentThread().getContextClassLoader().getResource("IPBlackList2.csv").getPath();


                InputStream is = null;
                InputStreamReader isr = null;
                LineNumberReader in = null;
                try {
                    String lineB;
                    is = new FileInputStream(pathBlack);
                    isr = new InputStreamReader(is, "utf-8");
                    in = new LineNumberReader(isr);
                    while ((lineB = in.readLine()) != null) {
                        String[] ipInfo = lineB.split(",");
                        ipLocation.addBlackIP(ipInfo[0].toString());
                    }

//                    String lineB2;
//                    is = new FileInputStream(pathBlack2);
//                    isr = new InputStreamReader(is, "utf-8");
//                    in = new LineNumberReader(isr);
//                    while ((lineB2 = in.readLine()) != null) {
//                        String[] ipInfo = lineB2.split(",");
//                        ipLocation.addBlackIP(ipInfo[0].toString());
//                    }

                    String lineC;
                    is = new FileInputStream(pathC);
                    isr = new InputStreamReader(is, "utf-8");
                    in = new LineNumberReader(isr);
                    while ((lineC = in.readLine()) != null) {
                        String[] cityInfo = lineC.split(",");
                        City city = new City();
                        city.setCode(cityInfo[0]);
                        city.setCity(cityInfo[1]);
                        city.setProvince(cityInfo[2]);
                        ipLocation.addC(city);
                    }

                    String line;
                    is = new FileInputStream(path);
                    isr = new InputStreamReader(is, "utf-8");
                    in = new LineNumberReader(isr);
                    while ((line = in.readLine()) != null) {
                        String[] ipInfo = line.split(",");
                        IP ip = new IP();
                        ip.setIpStart(ipConvert.ip2long(ipInfo[0]));
                        ip.setIpEnd(ipConvert.ip2long(ipInfo[1]));
                        ip.setCode(ipInfo[2]);
                        ip.setCity(ipLocation.getCity(ipInfo[2]).getCity());
                        ip.setProvinceCode(ipInfo[2].substring(0, 6));
                        ip.setProvinceName(ipLocation.getCity(ip.getProvinceCode() + "0000").getCity());
                        ip.setCountryCode(ipInfo[2].substring(0, 4));
                        ip.setCountryName(ipLocation.getCity(ip.getCountryCode() + "000000").getCity());
                        ip.setType((byte) 1);
                        ipLocation.add(ip);
                    }
                } catch (FileNotFoundException e) {
                    log.error("isp ipdata FileNotFoundException");
                } catch (IOException e) {
                    log.error("read isp ipdata io error", e);
                } catch (Exception e) {
                    log.error("build isp ip db error", e);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                        if (isr != null) {
                            isr.close();
                        }
                        if (is != null) {
                            is.close();
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
//        Iterator<Map.Entry<String, String>> it = dbBlack.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<String, String> entry = it.next();
//            System.out.println(entry.getKey() + ":" + entry.getValue());
//        }
    }

    @Override
    public void add(IP ip) {
        writeLocks.lock();
        try {
            db.put(ip.getIpStart(), ip);
            db.put(ip.getIpEnd(), ip);
        } finally {
            writeLocks.unlock();
        }
    }

    @Override
    public void addC(City city) {
        writeLocks.lock();
        try {
            dbC.put(city.getCode(), city);
        } finally {
            writeLocks.unlock();
        }
    }

    @Override
    public City getCity(String code) {

        try {

            readLocks.lock();
            try {

                City city = dbC.get(code);
                if (city != null) {
                    return city;
                } else {
                    return null;
                }
            } finally {
                readLocks.unlock();
            }
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public IP getLocation(String ip) {
        long seek;
        try {
            seek = ipConvert.ip2long(ip);
            readLocks.lock();
            try {
                Entry<Long, IP> floor = db.floorEntry(seek);
                Entry<Long, IP> ceiling = db.ceilingEntry(seek);
                if (floor == null || ceiling == null) {
                    return null;
                }
                IP floorIP = floor.getValue();
                IP ceilingIP = ceiling.getValue();
                if (floorIP.getIpStart() == ceilingIP.getIpStart()) {
                    return floorIP;
                } else {
                    return null;
                }
            } finally {
                readLocks.unlock();
            }
        } catch (UnknownHostException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public void clear() {
        writeLocks.lock();
        try {
            db.clear();
            // table.clear();
        } finally {
            writeLocks.unlock();
        }
    }

    @Override
    public Boolean exitInBlackList(String ip) {
        Boolean result = dbBlack.containsKey(ip);
        return result;
    }

    @Override
    public void addBlackIP(String ip) {
        writeLocks.lock();
        try {
            dbBlack.put(ip, ip);
        } catch (Exception e) {
            log.error(e);
        } finally {
            writeLocks.unlock();
        }
    }
}
