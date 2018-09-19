package com.kdg.gnome.adx.monitor;

import com.google.gson.Gson;
import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.anti.resp.AntiCheatStatus;

/**
 * Created by hbwang on 2017/12/28
 */
public class RequestLog2 extends LogBase {

    public static String VALUE_REQUEST_LOGTYPE = "RequestLog";

    public String   date;
    public KdgProtocol.RequestBean  kdgRequest; //kdg请求（转为）
    public KdgProtocol.ResponseBean kdgResponse;    //kdg响应(转为)

    public int isRespSucc = 0;

    public String   accountId;
    public String   advertiserId;
    public String   campaignId;
    public String   creativeId;
    public String   channel;

    public String   mediaId;
    public String   platId;
    public String   channelId;  //广告位所在频道id

    public String   deviceType;
    public String   adSize;

    public String   page;   //wap流量的当前页
    public String   refer;  //wap的来源页
    public String   ctr;    //当前投放素材的ctr
    public String   media = "";
    public String   adSpace;
    public String   adunit = ""; //媒体传的原始的广告位的id
    public int      adType;

    public int      istest = 0;     //是否是测试流量   0.是正式   1.测试

    public String   ip;
    public String   mcf;    //媒体一级分类
    public String   mcs;    //媒体二级分类
    public String   region; //地域
    public int adNormalType = AntiCheatStatus.NORMAL.value(); // 请求的反作弊状态，默认为正常

    private static Gson gson = new Gson();
    @Override
    public String toString() {
        try {
            return gson.toJson(this);
        } catch (Exception e) {

            return null;
        }
    }
}
