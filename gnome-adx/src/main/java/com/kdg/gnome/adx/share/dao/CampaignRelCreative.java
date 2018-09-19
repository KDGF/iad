package com.kdg.gnome.adx.share.dao;

import java.util.List;

/**
 * Created by hbwang on 2017/12/12
 */
public class CampaignRelCreative {

    public String      id;
    public String      accountId;  //账户id
    public String      advertiserId;   //广告主id
    public String      campaignId ;    //计划id
    public String      creativeId;     //素材id

    public Integer      flag;   //1开启，2暂停
//    public List<String> impMonitor;     //曝光监测
//    public String clkMonitor;     //点击监测 包括302跳转到落地页
}
