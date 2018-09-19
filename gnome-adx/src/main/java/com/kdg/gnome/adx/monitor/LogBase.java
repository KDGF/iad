package com.kdg.gnome.adx.monitor;


/**
 * Created by hbwang on 2017/12/6
 */
public class LogBase {
    public Long     timestamp;  //当前时间戳
    public String   logType;    //日志类型
    public String   token;  //唯一标识 用于串联请求与监控
}
