package com.kdg.gnome.adx.share.dao;

import java.sql.Date;
import java.util.List;

/**
 * Created by hbwang on 2017/12/12
 * 投放计划表
 */
public class Campaign {

    public String  id;
    public String  accountId;      //所属账户id
    public String  advertiserId;   //广告主id
    public String   name;       //计划名称
    public int  status;         //计划状态（0、正常；1、删除）
    public int flag;    // 投放状态（1.开启  2.暂停）
    public int is_status;   //操作来源  暂不考虑

    public Date startDate;      //开始日期
    public Date endDate;       //结束日期

    public int  launch_type;    //投放类型  1 尽快投放  2 匀速投放

    public int  budget_type;    //预算控制（0不限，1周预算，2日预算）
    public double  budget_num;     //预算金额
    public int  frequency_type; //频次控制：（0不限，1周期内单人曝光不超过，2每天单人曝光不超过，3每小时单人曝光不超过
    public int  frequency_num;  //频次
    public int  imp_limit;      //曝光上限0表示不限
    public int  click_limit;    //点击上限0表示不限

    public OrientNetwork    orientNetwork;  //网络定向（wifi，5G，4G，3G，2G，gprs）
    public List<Integer>    orient_plats;   //渠道定向
    public static class OrientNetwork {
        public List<String>     network;
    }

    public OrientSystem     orientSystem;   //系统定向 ios/android
    public static class OrientSystem {
        public List<String>     system;
    }

    public OrientMedia      orientMedia;    //媒体定向
    public static class OrientMedia {
        public List<String>     orient_mediaStr;
    }

    public OrientAdslot     orientAdslot;   //广告位定向
    public static class OrientAdslot {
        public List<String>     orient_adslotStr;
    }

    //减速投放相关配置
    public double   slow_rate_whole;
    public int      slow_start_whole;
    public double   slow_rate_uniform;
    public int      slow_start_uniform;

    public String   orientTime; //时段定向
    public String   orientAear; //地域定向
    public String moneyType; //出价方式 1 cpm 2 cpc cpm = cpc*ctr*1000
    public Double price;
    public Long    updateTime; //更新时间

}