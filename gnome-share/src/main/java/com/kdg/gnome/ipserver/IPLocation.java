package com.kdg.gnome.ipserver;

/**
 * 类描述：ip库接口
 *
 * @author: niuyb
 * @modify: zhangwei
 */
public interface IPLocation {
    public void clear();

    public void add(IP ip);

    public void addBlackIP(String ip);

    public void addC(City city);

    /**
     * 根据ip查询ip地址来源
     */
    public IP getLocation(String ip);

    public Boolean  exitInBlackList(String ip);

    /**
     * 根据code查询城市
     */
    public City getCity(String code);
}
