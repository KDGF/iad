package com.kdg.gnome.adx.monitor;

import com.google.gson.Gson;

/**
 * Created by hbwang on 2018/4/26
 */
public class TimeLog {

    public long  beforeReq;  //处理请求之前的耗时
    public long  handleReq;  //处理请求的耗时
    public long  loadCreatives;  //加载素材的耗时
    public long  handleResp;     //处理响应的耗时

    public long  allTime;   //从接收到请求到返回响应的耗时

    //控量连redis的耗时
    public long  ac4RedisTime;

    public long  antiCheatReqTime;
    public long  antiCheatImpTime;
    public long  antiCheatClkTime;


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
