package com.kdg.gnome.ipserver;


/**
 * 类描述：ip信息基础Bean
 *
 * @author: zhangwei
 */
public class IP {
    private long ipStart;
    private long ipEnd;
    private byte type;
    private String code;//市级code
    private String city;//市名称
    private String provinceCode;//省code
    private String provinceName;//省名称
    private String countryCode;//国家code
    private String countryName;//国家名称

    @Override
    public boolean equals(Object obj) {
        IP objIP = (IP) obj;
        if (objIP.getIpEnd() == this.ipEnd && objIP.getIpStart() == this.ipStart) {
            return true;
        }
        return false;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return ipStart + "->" + ipEnd + ": countryName" + countryName + " provinceName:" + provinceName + " city:" + city ;
    }

    public long getIpStart() {
        return ipStart;
    }

    public void setIpStart(long ipStart) {
        this.ipStart = ipStart;
    }

    public long getIpEnd() {
        return ipEnd;
    }

    public void setIpEnd(long ipEnd) {
        this.ipEnd = ipEnd;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
}
