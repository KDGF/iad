package com.kdg.gnome.adx.share.dao;

import java.util.List;

/**
 * Created by hbwang on 2018/1/4
 */
public class PmpConfig {

    public  int     id;
    public String   name;
    public int      plat_id;    //渠道id
    public int      deal_type;  //deal类型：1pd，2pdb
    public String   deal_id;

    public Double   consume;    //虚拟消耗
    public String   refund_lve; //退量比例

    public List<String> advers; //广告主


    public int      status; //状态：0正常，1删除
    public int      flag;   //状态，1开启，2关闭
}
