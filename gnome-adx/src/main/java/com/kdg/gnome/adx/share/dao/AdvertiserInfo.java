package com.kdg.gnome.adx.share.dao;

/**
 * Created by hbwang on 2018/2/7
 */
public class AdvertiserInfo {
    public String   id;
    public String   com_name;   //公司名称
    public Integer  status; //状态（0正常，1删除）
    public Integer  flag;   //1开启。 2 暂停
    public Integer servicerate;
    public Integer priority;

    public int  settlement_type;    //结算方式 1.cpm 2.cpc

    public String firstindustry; //一级行业
    public String secondindustry;//二级行业

//    public Integer  apt_status; //资质状态（0待审核，1通过，2拒绝，3部分通过）
}
