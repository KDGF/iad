package com.kdg.gnome.ipserver;

/**
 * 类描述：city信息基础Bean
 *
 * @author: zhangwei
 */
public class City {
    private String code;
    private String city;
    private String province;

    @Override
    public String toString() {
        return code + "->" + code + ": province" + province + " city:" + city;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
