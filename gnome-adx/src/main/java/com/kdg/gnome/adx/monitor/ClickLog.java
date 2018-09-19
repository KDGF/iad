package com.kdg.gnome.adx.monitor;

import com.google.gson.Gson;

/**
 * Created by hbwang on 2017/12/6
 */
public class ClickLog extends LogBase {
    public static String VALUE_CLICK_LOGTYPE = "ClickLog";

    public String   date;

    public String     accountId;  //账户id
    public String     advertiserId;   //广告主id
    public String     campaignId ;    //计划id
    public String     creativeId;     //素材id

    public String   mediaShowId;    //媒体外部id
    public String   channel;  //渠道Id

    public String   mediaId;
    public String   platId;
    public String   channelId;  //广告位所在频道id

    public String   deviceType; //
    public String   media;  //媒体
    public String   page;   //wap流量的当前页
    public String   refer;  //wap的来源页
    public String   adSpace;
    public String   adunit; //媒体传的原始的广告位的id
    public String   adType;

    public String   adSize;

    public String   deviceId;   //android:imei. ios:idfa
    public String   ctr;    //当前投放素材的ctr
    public String   inCost = "";  //价格
    public String   outCost = "";
    public String   adCost = "";


    public String   mcf;    //媒体一级分类
    public String   mcs;    //媒体二级分类
    public String   ip;     //客户端ip
    public String   region; //地域

    public int      status = 1; //状态:  0.时间太长失效; 1.正常
    public int      istest = 0;     //是否是测试流量   0.是正式   1.测试
    
    /**
     * @see com.kdg.gnome.anti.resp.AntiCheatStatus
     */
    public int      adNormalType;  // 监测的作弊状态

    public ClickLog() {
        this.logType = VALUE_CLICK_LOGTYPE;
    }



    private static Gson gson = new Gson();
    @Override
    public String toString() {
//        StringBuilder impress = new StringBuilder();
//        impress.append(logType).append("\t")
//                .append(accountId).append("\t")
//                .append(advertiserId).append("\t")
//                .append(campaignId).append("\t")
//                .append(creativeId).append("\t")
//                .append(channelId).append("\t")
//                .append(deviceType).append("\t")
//                .append(media).append("\t")
//                .append(adslot).append("\t")
//                .append(deviceId);
//
//        return new String(impress);


        try {
            return gson.toJson(this);
        } catch (Exception e) {

            return null;
        }

    }
}
