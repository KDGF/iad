package com.kdg.gnome.adx.share;

import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.monitor.TimeLog;
import com.kdg.gnome.adx.share.dao.AdvertiseMaterial;
import com.kdg.gnome.anti.resp.AntiCheatStatus;
import com.kdg.gnome.share.OriginRequest;
import org.httpkit.server.RespCallback;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GSessionInfo {
    public volatile boolean isRspSent = false; // 如果处理中断则会发送BROKEN消息并给客户端下发204来中断此次session的处理，将此域赋值防止后续超时再次引发响应下发

    public String   sid;        /* 会话id, 唯一标识一次会话 */

    public long     millRecvReq;            /* 通信模块接收到请求的毫秒时间戳 */
    public String   reqRealIp; // nginx携带过来的广告请求的ip
    public String   userAgent; // 客户端请求中的user-agent
    public Integer  os;  //本次请求的系统
    public Integer  mediaType = 0;   //本次请求的媒体类型 0:app,1:web

    public OriginRequest            originReq = new OriginRequest(); /* 原始请求信息 */
    public KdgProtocol.RequestBean  requestBean;        //转为协议请求

    public ByteBuffer downRspContent;   /* 响应信息 */
    public KdgProtocol.ResponseBean responseBean;

    public Integer downPlatId = -1;    /* 下游平台ID */

    public RespCallback callback = null;

    public boolean          isRespSucc = false;
    public volatile boolean hasOrder = true;
    //自投放使用
    public Map<String, AdvertiseMaterial> adMaterial;    //自己的订单
    public Map<String, String>  creativeIds = new HashMap<>();//自己投放的素材ids key对应impId
    public Map<String, String>  impSizes = new HashMap<>();
    public Map<String, Integer> impPriceMap = new HashMap<>();
    public Map<String, Boolean> isImpDeal = new HashMap<>();
    public Map<String, String>  ctrMap;  //素材的ctr

    public AntiCheatStatus  adNormalType = AntiCheatStatus.NORMAL; // 请求的反作弊状态（默认为正常）

    public boolean isMonitorCheck = false;  //监控入口的监测  直接返回机器名

    public static GSessionInfo getNewSession(OriginRequest originReq) {
        GSessionInfo session = new GSessionInfo();
        session.millRecvReq = System.currentTimeMillis();

        session.sid = UUID.randomUUID().toString() + "-" + session.millRecvReq;
        session.originReq = originReq;

        return session;
    }

    //记录各模块处理时间
    public TimeLog  timeLog;
}
