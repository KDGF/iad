package com.kdg.gnome.adx.module;

import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.downplat.BaseDownPlatHandler;
import com.kdg.gnome.adx.downplat.KdgDownPlat;
import com.kdg.gnome.adx.monitor.NewFormatLog;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.UtilOper;
import com.kdg.gnome.share.task.GMsg;
import com.kdg.gnome.share.task.GTaskBase;
import com.kdg.gnome.util.IpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.net.InetAddress.getByName;

public class G3PlatMgrModule extends GTaskBase {

    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");
    private static final Logger NEW_LOG_KAFKA = LogManager.getLogger("NEW_LOG_KAFKA");
    private static final SimpleDateFormat FORMAT4DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final static int UP_PLAT_MGR_TIMER_MILL_SECOND = 1000;
    private Map<Integer, BaseDownPlatHandler> mapDownPlats = null;

    public G3PlatMgrModule() {
        super("up-plat-mgr-task", 0);
    }


    @Override
    public boolean startTask() {

        /* 创建下游平台协议处理对象 */
        mapDownPlats = new HashMap<Integer, BaseDownPlatHandler>() {
            {
                //添加的是使用媒体方协议的处理类

                put(1, new KdgDownPlat());

            }
        };

        loadEffectiveMediaPlats();
        return true;
    }

    //动态加载新增加的媒体平台
    public void loadEffectiveMediaPlats() {
        if (mapDownPlats == null || mapDownPlats.isEmpty()) {
            return;
        }
        mapDownPlats.put(1, new KdgDownPlat());
    }

    public boolean handlerDownPlatReq(GSessionInfo sessionInfo) {
        if (sessionInfo == null) {
            log.error("sessInfo == null");
            return false;
        }
        BaseDownPlatHandler downPlatHandler = null;
        downPlatHandler = mapDownPlats.get(1);
        if (null == downPlatHandler) {
            log.error("null == downPlatHandler, sid = {}", sessionInfo.sid);
            return false;
        }
        log.debug("BaseDownPlatHandler = {}, sid = {}", sessionInfo.downPlatId, sessionInfo.sid);
        boolean status = downPlatHandler.handlerReq(sessionInfo);
        if (!status) {
            log.error("downPlatHandler.handlerReq(sessInfo) returned false, sid = {}", sessionInfo.sid);
           return false;
        }

        if (sessionInfo.requestBean == null) {
            return false;
        } else {
            log.info("requestBean : " + sessionInfo.requestBean.toString());
        }

        //记录唯一请求日志

        String ip = sessionInfo.requestBean.device.ip;
        String country = IpUtil.getCountryName(ip);
        String province = IpUtil.getProvinceName(ip);
        String city = IpUtil.getCityName(ip);
        NewFormatLog newLog = new NewFormatLog();


        newLog.event_date =FORMAT4DATE.format(new Date());

        newLog.media_id = sessionInfo.downPlatId + "";
        newLog.plat_id = "1";
        newLog.plat_name = "科达-优智";
        if (sessionInfo.requestBean != null && sessionInfo.requestBean.app != null) {
            newLog.media_name = sessionInfo.requestBean.app.name;
            newLog.package_name = sessionInfo.requestBean.app.bundle;
        }

        newLog.media_ad_name = "";
        newLog.os = sessionInfo.requestBean.device.os;
        newLog.dev_type = sessionInfo.requestBean.device.devicetype + "";
        newLog.country = country;
        newLog.city = city;
        newLog.province = province;
        newLog.ip = ip;
        for (KdgProtocol.RequestBean.Impression imp : sessionInfo.requestBean.imp) {
            newLog.channel_id = imp.channelid;
            newLog.uniq_req = 1;
            newLog.media_ad_id = imp.tagid;
            newLog.token = imp.tagid + "_" + sessionInfo.sid;
            newLog.ad_id = "";
            newLog.ad_name = "";
            NEW_LOG_KAFKA.info(newLog.toString());
        }
        return true;
    }

    public boolean handlerDownPlatRsp(GSessionInfo sessInfo) {

        if (sessInfo == null) {
            log.error("sessInfo == null");
            return false;
        }

        ByteBuffer strRsp = null;
        //目前不处理流量外发
        BaseDownPlatHandler downPlatHandler = null;
        downPlatHandler = mapDownPlats.get(1);

        if (null == downPlatHandler) {
            log.error("null == downPlatHandler, sid = {}", sessInfo.sid);
            return false;
        }

        if (sessInfo.hasOrder) {
            strRsp = mapDownPlats.get(1).handlerRsp(null, sessInfo);
        } else {
            strRsp = KdgDownPlat.handlerErrorRsp(sessInfo);
        }

        if (strRsp != null) {
            sessInfo.downRspContent = strRsp;
            return true;
        } else {
            return false;
        }

    }

    @Override
    protected void handlerMsg(int msgId, Object objContext) {
        while (running) {
            UtilOper.sleep(UP_PLAT_MGR_TIMER_MILL_SECOND);
        }
    }

    @Override
    public boolean closeTask() {

        addMsg(new GMsg(Constants.MSG_ID_SYS_KILL, null));
        return true;
    }

    public static void main(String[] args) throws URISyntaxException, UnknownHostException {
        String url = "http://rtb.yooshu.cn";
        // String url = "http://api.snmi.cn";
        // String url = "http://bdsp.x.jd.com";


        System.out.println("before URI: " + System.currentTimeMillis());
        URI uri = new URI(url);

        System.out.println("before getHost: " + System.currentTimeMillis());
        String name = uri.getHost();

        System.out.println("before getByName(" + name + "): " + System.currentTimeMillis());
        InetAddress host = getByName(name);

        System.out.println("before InetSocketAddress: " + System.currentTimeMillis());
        InetSocketAddress addr = new InetSocketAddress(host, 80);

        System.out.println("end: " + System.currentTimeMillis());
    }
}
