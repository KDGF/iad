package com.kdg.gnome.adx.module;

import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.downplat.KdgDownPlat;
import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.monitor.*;
import com.kdg.gnome.adx.order.AmountControl;
import com.kdg.gnome.adx.order.BaseOrderHandler;
import com.kdg.gnome.adx.order.OrderHandler;
import com.kdg.gnome.adx.order.UniformSpeedAdvertising;
import com.kdg.gnome.adx.share.*;
import com.kdg.gnome.adx.share.dao.AdvertiseMaterial;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.anti.handler.request.DspRequestAntiCheatHandler;
import com.kdg.gnome.anti.handler.tracker.ClickTrackerAntiCheatHandler;
import com.kdg.gnome.anti.handler.tracker.ImpTrackerAntiCheatHandler;
import com.kdg.gnome.anti.resp.AntiCheatStatus;
import com.kdg.gnome.anti.valueobject.RequestAntiCheatVO;
import com.kdg.gnome.anti.valueobject.TrackerAntiCheatVO;
import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.redisutil.RedisClusterClient;
import com.kdg.gnome.share.task.AES;
import com.kdg.gnome.share.task.GMsg;
import com.kdg.gnome.share.task.GTaskBase;
import com.kdg.gnome.util.IpUtil;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.httpkit.HeaderMap;

import javax.mail.MessagingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

import static org.httpkit.HttpUtils.HttpEncode;

public class GServiceTask extends GTaskBase {

    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");
    private static final Logger REQ_LOG = LogManager.getLogger("ES_OUT_REQ");
    private static final Logger IMP_LOG = LogManager.getLogger("ES_OUT_IMP");
    private static final Logger CLK_LOG = LogManager.getLogger("ES_OUT_CLK");
    private static final Logger TIME_LOG = LogManager.getLogger("ES_OUT_TIME");

    private static final Logger REQ_KAFKA = LogManager.getLogger("REQ_KAFKA");
    private static final Logger MONITOR_KAFKA = LogManager.getLogger("MONITOR_KAFKA");

    private static final Logger NEW_LOG_KAFKA = LogManager.getLogger("NEW_LOG_KAFKA");
    private static final SimpleDateFormat FORMAT4TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    private static final SimpleDateFormat FORMAT4DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //曝光达到这个量级发邮件
    private static final int IMP_SUM_4_SEND_EMAIL = 1000000;

    public GServiceTask(String taskName) {
        super(taskName, 0);
    }

    private boolean downReq2InnerReq(GSessionInfo sessInfo) {
        boolean isSuccess;
        try {
            isSuccess = AdxSystem.plat3MgrModule().handlerDownPlatReq(sessInfo);
        } catch (Exception e) {
            log.error("AdxSystem.plat3MgrModule().handlerDownPlatReq failed: [{}], sid = {}", e.getMessage(), sessInfo.sid);
            isSuccess = false;
        }

        if (!isSuccess) {
            log.debug("调用对应下游平台[{}], handler将客户端请求转成GInnerRequest. handlerDownPlatReq fail. sid = {}", sessInfo.downPlatId,  sessInfo.sid);
            return false;
        }
        return true;
    }

    private void processAdRequest(int msgId, GSessionInfo sessInfo) {
        long timeBeforeHandlerReq = System.currentTimeMillis();
        sessInfo.timeLog = new TimeLog();
        sessInfo.timeLog.beforeReq = timeBeforeHandlerReq - sessInfo.millRecvReq;

        // 调用对应下游平台handler将客户端请求转成协议对应的request
        if (!downReq2InnerReq(sessInfo)) {
            GServiceModule.brokenSigh(sessInfo);
            return;
        }
        long timeHanderReq = System.currentTimeMillis();
        sessInfo.timeLog.handleReq = timeHanderReq - timeBeforeHandlerReq;
        
        // 请求如果作弊直接返回不做后续处理
        AntiCheatStatus requestCheatStatus = isRequestCheat(sessInfo.requestBean);
        long timeAntiCHeatReqOver = System.currentTimeMillis();
        sessInfo.timeLog.antiCheatReqTime = timeAntiCHeatReqOver - timeHanderReq;
		if (!AntiCheatStatus.NORMAL.equals(requestCheatStatus)) {
			log.warn("requestAntiCheat failed, sid={}", sessInfo.sid);
			sessInfo.adNormalType = requestCheatStatus;
		}

        log.info("首先查看自己有没有可以投的素材...sid = {}", sessInfo.sid);
        BaseOrderHandler baseOrderHandler = new OrderHandler();;

        //默认不传secure参数下发的是http的素材
        if (sessInfo.requestBean.secure == null) {
            sessInfo.requestBean.secure = 0;
        }

        Map<String, Callable> mapTmp = new HashedMap();
        List<KdgProtocol.RequestBean.Impression> imps = sessInfo.requestBean.imp;
        sessInfo.requestBean.imp = new ArrayList<>();

        List<AdvertiseMaterial> ads = baseOrderHandler.loadOrders(sessInfo);

        for (KdgProtocol.RequestBean.Impression imp : imps) {
            mapTmp.put(imp.id, new MergeReqHandler.MakeUpImp(imp, baseOrderHandler, ads, sessInfo));
        }
        Map<String, Object> impMap = ThreadPool.getThreadPool().runMap(mapTmp);

        Map<String, AdvertiseMaterial> adMaterial = new HashedMap();
        for (Map.Entry entry : impMap.entrySet()) {
            AdvertiseMaterial ad = (AdvertiseMaterial) entry.getValue();
            String key = (String) entry.getKey();
            adMaterial.put(key, ad);
        }

        long timeLoadCrvs = System.currentTimeMillis();
        sessInfo.timeLog.loadCreatives = timeLoadCrvs - timeHanderReq;

        if (adMaterial.size() > 0) {
            sessInfo.adMaterial = adMaterial;
            sessInfo.hasOrder = true;
            log.info("查到符合的素材可以投放...sid = {}", sessInfo.sid);
            AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_RSP, sessInfo);
        } else {
            AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_RSP, sessInfo);
            sessInfo.hasOrder = false;
        }

    }

    @Override
    protected void handlerMsg(int msgId, Object objContext) {

        int systemStatus = AdxSystem.sysMgrModule().getSysStatus();
        if (systemStatus != AdxConstants.SERVICE_STATUS_WORK) {
            log.error("AdxSystem.sysMgrModule().getSysStatus() 值为：{}，服务不处于工作状态！", systemStatus);
            return;
        }

        if (null == objContext) {
            log.error("error: null == objContext");
            return;
        }

        if (!(objContext instanceof GSessionInfo)) {
            log.error("objContext is not an instance of GSessionInfo");
            return;
        }

        // TODO 将每种消息类型的处理逻辑封装成独立的方法
        switch (msgId) {
            case AdxConstants.MSG_ID_SERVICE_ADX_AD_REQ: {

                GSessionInfo sessInfo = (GSessionInfo) objContext;
                processAdRequest(msgId, sessInfo);
                break;
            }
            case AdxConstants.MSG_ID_SERVICE_ADX_AD_RSP:
            case AdxConstants.MSG_ID_SERVICE_ADX_AD_TIMEOUT: {
                GSessionInfo sessInfo = (GSessionInfo) objContext;
                if (sessInfo.isRspSent) {
                    log.warn("sessInfo.isRspSent is true. PLEASE CHECK. sid = {}", sessInfo.sid);
                    break;
                }
                sessInfo.isRspSent = true;

                long beforeHandlerResp = System.currentTimeMillis();
                log.debug("handler rsp, sid = {}", sessInfo.sid);

                /* 组装对应的下游响应并下发 */
                ByteBuffer content;
                HeaderMap header = new HeaderMap();
//        header.put("Connection", "Keep-Alive");
                header.put("Access-Control-Allow-Origin", "*");

                ByteBuffer[] bytes = null;
                /* 按照下游平台方式进行相应处理 */
                try {
                    if (AdxSystem.plat3MgrModule().handlerDownPlatRsp(sessInfo)) {
                        /* 向下游发送响应 */
                        content = sessInfo.downRspContent;
                        bytes = form200Resp(sessInfo, header, content);
                    } else {
                        /* 没有有效的广告, 则下发204 */
                        bytes = form204Resp(sessInfo, header);
                    }
                } catch (Exception e) {
                    log.error("AdxSystem.plat3MgrModule().handlerDownPlatRsp failed: [{}], sid = {}", e.getMessage(), sessInfo.sid);
                    sessInfo.isRespSucc = false;
                    bytes = form204Resp(sessInfo, header);
                }
                sessInfo.callback.run(bytes);
                long handlerResp = System.currentTimeMillis();
                sessInfo.timeLog.handleResp = handlerResp - beforeHandlerResp;
                sessInfo.timeLog.allTime = handlerResp - sessInfo.millRecvReq;
                TIME_LOG.info(sessInfo.timeLog.toString());

                //向kafka发送日志
                for (KdgProtocol.RequestBean.Impression imp : sessInfo.requestBean.imp) {
                    sendReqMsg(sessInfo, imp);
                }
                log.debug("response to client, httpReqId = {}, sid = {}", sessInfo.callback.getHttpReqId(), sessInfo.sid);
                break;
            }
            case AdxConstants.MSG_ID_SERVICE_ADX_AD_BROCKEN: {
                GSessionInfo sessInfo = (GSessionInfo) objContext;
                if (sessInfo.isRspSent) {
                    log.warn("sessInfo.isRspSent is true. PLEASE CHECK. sid = {}", sessInfo.sid);
                    break;
                }
                sessInfo.isRspSent = true;
                HeaderMap header = new HeaderMap();
                header.put("Access-Control-Allow-Origin", "*");

                /* 没有有效的广告, 则下发204 */
                ByteBuffer[] bytes = form204Resp(sessInfo, header);
                sessInfo.callback.run(bytes);

                if (sessInfo.requestBean != null && sessInfo.requestBean.imp != null
                        && (!sessInfo.requestBean.imp.isEmpty())) {
                    for (KdgProtocol.RequestBean.Impression imp : sessInfo.requestBean.imp) {
                        sendReqMsg(sessInfo, imp);
                    }
                }
                log.debug("response to client, httpReqId = {}, sid = {}", sessInfo.callback.getHttpReqId(), sessInfo.sid);
                break;
            }
            case AdxConstants.MSG_ID_SERVICE_ADX_IMPRESS: {
                if (!(objContext instanceof GSessionInfo)) {
                    log.error("!(objContext instanceof GSessionInfo)");
                    break;
                }

                GSessionInfo sessionInfo = (GSessionInfo) objContext;
                if (sessionInfo.originReq != null && sessionInfo.originReq.type != Constants.HTTP_REQ_TYPE_GET
                        && sessionInfo.originReq.type != Constants.HTTP_REQ_TYPE_HEAD
                        && sessionInfo.originReq.type != Constants.HTTP_REQ_TYPE_POST) {
                    log.error("NOT ALLOWED METHOD! method[0,GET;1,POST;2,OTHER] is {}, sid = {}", sessionInfo.originReq.type, sessionInfo.sid);
                    sendResp(sessionInfo, 405);
                    break;
                }

                // parse the url parameters
                Map<String, String> infoMap = parseMonitorPara(sessionInfo);
                if (infoMap == null || infoMap.isEmpty()) {
                    log.error("BAD REQUEST! parseMonitorPara(sessionInfo) is null, i.e., request's parameters is empty, sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String infoEnpt = infoMap.get("p");
                String aesToken = AdxSystem.sysMgrModule().getSysConfigManager().serverConfig.aesToken;
                String info = null;
                try {
                    info = AES.decrypt(infoEnpt, aesToken, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (StringUtils.isBlank(info)) {
                    log.error("BAD REQUEST! info Decrypt ERROR., sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                Map<String, String> paraMap = parseInfoPara(info);
                if (infoMap == null || infoMap.isEmpty()) {
                    log.error("BAD REQUEST! parseInfoPara(info) is null, i.e., request's parameters is empty, sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                // redirect
                String redirect = infoMap.get("redirect");
                if (StringUtils.isNotBlank(redirect)) {
                    sendRedirectMonitorResp(sessionInfo, redirect);
                } else {
                    sendMonitorResp(sessionInfo);
                }

                // impressId
                String impressId = paraMap.get("impressId");
                if (StringUtils.isBlank(impressId)) {
                    log.error("BAD REQUEST! paraMap.get(\"impressId\") is null, sid = {}", impressId);
                    sendResp(sessionInfo, 400);
                    break;
                }
                sessionInfo.sid = impressId;
                log.info("a new impress log will be recorded. impressId = {}", impressId);

                // timestamp
                Long timestamp = System.currentTimeMillis();

                String creativeId = paraMap.get("crtId");
                if (StringUtils.isBlank(creativeId)) {
                    log.error("BAD REQUEST! creativeId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String accountId = paraMap.get("acctId");
                if (StringUtils.isBlank(accountId)) {
                    log.error("BAD REQUEST! accountId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String advertiserId = paraMap.get("advId");
                if (StringUtils.isBlank(advertiserId)) {
                    log.error("BAD REQUEST! advertiseId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String campaignId = paraMap.get("camId");
                if (StringUtils.isBlank(campaignId)) {
                    log.error("BAD REQUEST! campaignId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String tagid = paraMap.get("tagid");
                if (StringUtils.isBlank(tagid)) {
                    log.error("BAD REQUEST! tagid is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String channel = paraMap.get("chn");
                if (StringUtils.isBlank(channel)) {
                    log.error("BAD REQUEST! channel is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String os = paraMap.get("os");
                if (StringUtils.isBlank(os)) {
                    log.error("BAD REQUEST! os is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String style = paraMap.get("style");

                String deviceId = paraMap.get("devId");
                if (StringUtils.isBlank(deviceId)) {
                    log.error("BAD REQUEST! deviceId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String size = paraMap.get("size");
                if (StringUtils.isBlank(size)) {
                    log.error("BAD REQUEST! size is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String ctrStr = paraMap.get("ctr");
                if (StringUtils.isBlank(ctrStr)) {
                    log.error("BAD REQUEST! ctr is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String pkg = paraMap.get("pkg");
                String mediaName = paraMap.get("mna");
                String pageUrl = paraMap.get("page");
                String refer = paraMap.get("refer");
                String adunit = paraMap.get("adunit");
                String isTest = paraMap.get("test");
                String channelId = paraMap.get("chnId");

                ImpressLog impressLog = new ImpressLog();

                impressLog.date = FORMAT4TIME.format(new Date());
                impressLog.token = impressId;
                impressLog.timestamp = timestamp;
                impressLog.creativeId = creativeId;
                impressLog.accountId = accountId;
                impressLog.advertiserId = advertiserId;
                impressLog.campaignId = campaignId;
                impressLog.ctr = ctrStr;
                impressLog.ip = sessionInfo.reqRealIp;
                impressLog.deviceType = os;
                impressLog.platId = "1";
                impressLog.channel = channel;
                impressLog.channelId = StringUtils.isBlank(channelId) ? "" : channelId;
                impressLog.media = StringUtils.isBlank(pkg) ? "" : pkg;
                impressLog.page = StringUtils.isBlank(pageUrl) ? "" : pageUrl;
                impressLog.refer = StringUtils.isBlank(refer) ? "" : refer;
                impressLog.adSpace = tagid;
                impressLog.adunit = adunit;
                impressLog.deviceId = deviceId;
                impressLog.adSize = size;
                impressLog.adType = style;
                impressLog.region = getRegion(impressLog.ip);
                impressLog.mcf = "";
                impressLog.mcs = "";

                if (StringUtils.isNotBlank(isTest)) {
                    impressLog.istest = 1;
                }

                impressLog.inCost = "";
                impressLog.outCost = "";
                impressLog.adCost = "";

                int adverSettleType = CacheUtils.getAdverSettleType(advertiserId);
                String priceMacro = infoMap.get("price");
                if (StringUtils.isBlank(infoEnpt)) {
                    log.error("BAD REQUEST! paraMap.get(\"info\") is null, sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String  bidTypeStr = infoMap.get("bt");
                int bidType = StringUtils.isBlank(bidTypeStr) ? AdxConstants.SETTLE_TYPE_CPM : Integer.parseInt(bidTypeStr);

                String priceStr = handleAdxPrice(priceMacro, channel);
                if (StringUtils.isBlank(priceStr)) {
                    log.error("price is NULL. sid = {}", sessionInfo.sid);
                }
                switch (bidType) {
                    case AdxConstants.SETTLE_TYPE_CPM: {
                        impressLog.inCost = priceStr;
                        if (StringUtils.isNotBlank(priceStr)) {
                            if (adverSettleType == 1) {
                                //按照广告主服务费率进行提价
                                String p = handleAdverPrice4Adx(priceStr, advertiserId);
                                if (StringUtils.isNotBlank(p)) {
                                    impressLog.outCost = p;
                                }
                            } else if (adverSettleType == 2) {
                                //广告主按照cpc结算,将曝光的价格写入到redis
                                RedisClusterClient.setex(tagid + impressId, 24*60*60, priceStr);
                                log.debug("广告主 {} 按照 cpc 结算, 将曝光的价格写入 redis 供点击时查价格使用", advertiserId);
                            }
                        }

                        break;
                    }
                    case AdxConstants.SETTLE_TYPE_CPC: {
//                        impressLog.inCost = priceStr;
                        if (adverSettleType == 1 && StringUtils.isNotBlank(priceStr)) {
                            //按照广告主服务费率进行提价
                            String p = handleAdverPrice4Adx(priceStr, advertiserId);
                            if (StringUtils.isNotBlank(p)) {
                                impressLog.outCost = p;
                            }
                        }
                        break;
                    }
                    case AdxConstants.SETTLE_TYPE_CPD: {
                        break;
                    }
                    case AdxConstants.SETTLE_TYPE_CPA: {
                        break;
                    }
                    default: {
                        break;
                    }
                }

                // 判断监测是否作弊
                long antiCheatImpTIme = System.currentTimeMillis();
				AntiCheatStatus trackerCheat = isTrackerCheat(impressLog);
				long antiCheatImpTImeOver = System.currentTimeMillis();
				TimeLog timeLog = new TimeLog();
				timeLog.antiCheatImpTime = antiCheatImpTImeOver - antiCheatImpTIme;
				TIME_LOG.info(timeLog.toString());
				// 设置监测的状态
				impressLog.adNormalType = trackerCheat.value();
				
				// 如果监测正常，扣费广告主费用、记录正常日志、发送到kafka中
				//控量统计
	            Calendar ca = Calendar.getInstance();
	            int day = ca.get(Calendar.DAY_OF_MONTH);
	            int hour = ca.get(Calendar.HOUR_OF_DAY);
	            if (StringUtils.isNotBlank(impressLog.outCost)  && adverSettleType == 1) {
	                AmountControl.hincrOfDayBudget(campaignId, day, Double.valueOf(impressLog.outCost));
	                AmountControl.hincrOfCycleBudget(campaignId, Double.valueOf(impressLog.outCost));
                    UniformSpeedAdvertising.reduceBugget(campaignId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_BUDGET, Double.valueOf(impressLog.outCost));
	            }
	            //对匀速投放时间片的统计减值
	            UniformSpeedAdvertising.reduceImpOrClk(campaignId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_IMP);

	            AmountControl.hincrOfDay(campaignId, day, 1);
	            AmountControl.hincrDevOfDayFrequeny(campaignId, day, deviceId);
	            AmountControl.hincrDevOfHourFrequeny(campaignId, hour, deviceId);
	            AmountControl.hincrDevOfCycleFrequeny(campaignId, deviceId);


               //判断该计划有没有到达1000CPM
               arr1000Cpm(campaignId, day);

                IMP_LOG.info(impressLog.toString());
                MONITOR_KAFKA.info(impressLog.toString());
                log.debug("a new impress log has been flushed. sid = {}", sessionInfo.sid);

                //新的日志
                NewFormatLog newLog = new NewFormatLog();
                newLog.adver_imp = 1;

                newLog.event_date = FORMAT4DATE.format(new Date());
                newLog.token = tagid + "_" + impressId;
                newLog.ad_nomal_type = trackerCheat.value();

                newLog.package_name = pkg;
                newLog.media_id = channel;
                newLog.media_name = mediaName;
                newLog.ad_name = "";
                newLog.plat_id = "1";
                newLog.plat_name = "科达-优智";

                newLog.media_ad_id = adunit;
                newLog.media_ad_name = "";
                newLog.ad_id = tagid;

                newLog.channel_id = channelId;
                newLog.adver_id = advertiserId;
                newLog.adver_name = CacheUtils.getAdverName(advertiserId);
                newLog.cam_id = campaignId;
                newLog.cam_name = CacheUtils.getCampName(campaignId);
                newLog.crv_id = creativeId;
                newLog.style_id = style;
                newLog.crv_name = CacheUtils.getCrvName(creativeId);
                newLog.os = os;
                newLog.dev_type = "app";
                newLog.ad_size = size;
                newLog.country = IpUtil.getCountryName(sessionInfo.reqRealIp);
                newLog.city = IpUtil.getCityName(sessionInfo.reqRealIp);
                newLog.province = IpUtil.getProvinceName(sessionInfo.reqRealIp);
                newLog.ip = sessionInfo.reqRealIp;

                if (StringUtils.isNotBlank(impressLog.inCost)) {
                    newLog.ori_cost = Double.parseDouble(impressLog.inCost);
                }
                if (StringUtils.isNotBlank(impressLog.outCost)) {
                    newLog.wastage_price = Double.parseDouble(impressLog.outCost);
                }

                NEW_LOG_KAFKA.info(newLog.toString());

                break;
            }
            case AdxConstants.MSG_ID_SERVICE_ADX_CLICK: {
                if (!(objContext instanceof GSessionInfo)) {
                    log.error("!(objContext instanceof GSessionInfo)");
                    break;
                }
                GSessionInfo sessionInfo = (GSessionInfo) objContext;
                if (sessionInfo.originReq != null && sessionInfo.originReq.type != Constants.HTTP_REQ_TYPE_GET
                        && sessionInfo.originReq.type != Constants.HTTP_REQ_TYPE_HEAD
                        && sessionInfo.originReq.type != Constants.HTTP_REQ_TYPE_POST) {
                    log.error("NOT ALLOWED METHOD! method[0,GET;1,POST;2,OTHER] is {}, sid = {}", sessionInfo.originReq.type, sessionInfo.sid);
                    sendResp(sessionInfo, 405);
                    break;
                }
                // parse the url parameters
                Map<String, String> infoMap = parseMonitorPara(sessionInfo);
                if (infoMap == null || infoMap.isEmpty()) {
                    log.error("BAD REQUEST! parseMonitorPara(sessionInfo) is null, i.e., request's parameters are empty. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String infoEnpt = infoMap.get("p");
                if (StringUtils.isBlank(infoEnpt)) {
                    log.error("BAD REQUEST! paraMap.get(\"info\") is null, sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String aesToken = AdxSystem.sysMgrModule().getSysConfigManager().serverConfig.aesToken;
                String info = null;
                try {
                    info = AES.decrypt(infoEnpt, aesToken, true);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (StringUtils.isBlank(info)) {
                    log.error("BAD REQUEST! info Decrypt ERROR., sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                Map<String, String> paraMap = parseInfoPara(info);
                if (infoMap == null || infoMap.isEmpty()) {
                    log.error("BAD REQUEST! parseInfoPara(info) is null, i.e., request's parameters is empty");
                    sendResp(sessionInfo, 400);
                    break;
                }
                // other redirects
                String redirect = infoMap.get("redirect");
                if (StringUtils.isNotBlank(redirect)) {
                    sendRedirectMonitorResp(sessionInfo, redirect);
                } else {
                    sendMonitorResp(sessionInfo);
                }

                // clickId
                String clickId = paraMap.get("clickId");
                if (StringUtils.isBlank(clickId)) {
                    log.error("BAD REQUEST! paraMap.get(\"clickId\") is null, sid = {}", clickId);
                    sendResp(sessionInfo, 400);
                    break;
                }
                sessionInfo.sid = clickId;
                log.info("a new click log will be recorded. clickId = {}, sid = {}", clickId, sessionInfo.sid);


                String creativeId = paraMap.get("crtId");
                if (StringUtils.isBlank(creativeId)) {
                    log.error("BAD REQUEST! creativeId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String accountId = paraMap.get("accId");
                if (StringUtils.isBlank(accountId)) {
                    log.error("BAD REQUEST! accountId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String advertiserId = paraMap.get("advId");
                if (StringUtils.isBlank(advertiserId)) {
                    log.error("BAD REQUEST! advertiseId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String campaignId = paraMap.get("camId");
                if (StringUtils.isBlank(campaignId)) {
                    log.error("BAD REQUEST! campaignId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String tagid = paraMap.get("tagid");
                if (StringUtils.isBlank(tagid)) {
                    log.error("BAD REQUEST! tagid is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String channel = paraMap.get("chn");
                if (StringUtils.isBlank(channel)) {
                    log.error("BAD REQUEST! channel is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String os = paraMap.get("os");
                if (StringUtils.isBlank(os)) {
                    log.error("BAD REQUEST! os is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String style = paraMap.get("style");
                if (StringUtils.isBlank(style)) {
                    log.error("BAD REQUEST! style is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String deviceId = paraMap.get("devId");
                if (StringUtils.isBlank(deviceId)) {
                    log.error("BAD REQUEST! deviceId is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String size = paraMap.get("size");
                if (StringUtils.isBlank(size)) {
                    log.error("BAD REQUEST! size is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String ctrStr = paraMap.get("ctr");
                if (StringUtils.isBlank(ctrStr)) {
                    log.error("BAD REQUEST! ctr is null. sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }
                String pkg = paraMap.get("pkg");
                String mediaName = paraMap.get("mna");
                String pageUrl = paraMap.get("page");
                String refer = paraMap.get("refer");

                String channelId = paraMap.get("chnId");
                String adunit = paraMap.get("adunit");
                String isTest = paraMap.get("test");

                ClickLog clickLog = new ClickLog();
                clickLog.date = FORMAT4TIME.format(new Date());
                clickLog.logType = ClickLog.VALUE_CLICK_LOGTYPE;
                clickLog.token = clickId;
                clickLog.timestamp = System.currentTimeMillis();
                clickLog.creativeId = creativeId;
                clickLog.accountId = accountId;
                clickLog.advertiserId = advertiserId;
                clickLog.campaignId = campaignId;
                clickLog.ip = sessionInfo.reqRealIp;
                clickLog.deviceType = os;
                clickLog.adType = style;
                clickLog.ctr = ctrStr;
                clickLog.channel = channel;
                clickLog.platId = "1";
                clickLog.mediaId = channel;
                clickLog.channelId = StringUtils.isBlank(channelId) ? "" : channelId;
                clickLog.media = StringUtils.isBlank(pkg) ? "" : pkg;
                clickLog.page = StringUtils.isBlank(pageUrl) ? "" : pageUrl;
                clickLog.refer = StringUtils.isBlank(refer) ? "" : refer;
                clickLog.adSpace = tagid;
                clickLog.adunit = adunit;
                clickLog.deviceId = deviceId;
                clickLog.adSize = size;
                clickLog.region = getRegion(clickLog.ip);
                clickLog.mcf = "";
                clickLog.mcs = "";

                if (StringUtils.isNotBlank(isTest)) {
                    clickLog.istest = 1;
                }

                clickLog.inCost = "";
                clickLog.outCost = "";
                clickLog.adCost = "";

                int adverSettleType = CacheUtils.getAdverSettleType(advertiserId);

                String priceMacro = infoMap.get("price");
                if (StringUtils.isBlank(infoEnpt)) {
                    log.error("BAD REQUEST! paraMap.get(\"info\") is null, sid = {}", sessionInfo.sid);
                    sendResp(sessionInfo, 400);
                    break;
                }

                String  bidTypeStr = infoMap.get("bt");
                int bidType = StringUtils.isBlank(bidTypeStr) ? AdxConstants.SETTLE_TYPE_CPM : Integer.parseInt(bidTypeStr);

                String priceStr = handleAdxPrice(priceMacro, channel);
                if (StringUtils.isBlank(priceStr)) {
                    log.error("price is NULL. sid = {}", sessionInfo.sid);
                }
                switch (bidType) {
                    case AdxConstants.SETTLE_TYPE_CPM: {
                        if (StringUtils.isNotBlank(priceStr)) {
                            if (adverSettleType == 2) {
                                //按照广告主服务费率进行提价
                                String pTmp = RedisClusterClient.getString(tagid + clickId);
                                if (StringUtils.isNotBlank(pTmp)) {
                                    //将CPM转为CPC
                                    Double pCpc = cpm2CpcPrice(pTmp, ctrStr);
                                    //按照广告主服务费率进行提价
                                    String p = handleAdverPrice4Adx(pCpc.toString(), advertiserId);
                                    if (StringUtils.isNotBlank(p)) {
                                        clickLog.outCost = p;
                                    }
                                }
                            }
                        }

                        break;
                    }
                    case AdxConstants.SETTLE_TYPE_CPC: {
                        clickLog.inCost = priceStr;
                        if (adverSettleType == 2 && StringUtils.isNotBlank(priceStr)) {
                            //按照广告主服务费率进行提价
                            String p = handleAdverPrice4Adx(priceStr, advertiserId);
                            if (StringUtils.isNotBlank(p)) {
                                clickLog.outCost = p;
                            }
                        }
                        break;
                    }
                    case AdxConstants.SETTLE_TYPE_CPD: {
                        break;
                    }
                    case AdxConstants.SETTLE_TYPE_CPA: {
                        break;
                    }
                    default: {
                        break;
                    }
                }

                // 判断监测是否作弊
                long antiCheatClkTime = System.currentTimeMillis();
                AntiCheatStatus trackerCheat = isTrackerCheat(clickLog);
                long antiCheatClkTimeOver = System.currentTimeMillis();
                TimeLog timeLog = new TimeLog();
                timeLog.antiCheatClkTime = antiCheatClkTimeOver - antiCheatClkTime;
                TIME_LOG.info(timeLog.toString());
                // 设置监测的状态
                clickLog.adNormalType = trackerCheat.value();
                
                // 如果监测正常，扣费广告主费用、记录正常日志、发送到kafka中
//                if(AntiCheatStatus.NORMAL.equals(trackerCheat)) {
                	 Calendar ca = Calendar.getInstance();
                     int day = ca.get(Calendar.DAY_OF_MONTH);
                     AmountControl.hincrOfDay(campaignId, day, 2);

                     //对匀速投放时间片的控制值进行减操作
                     UniformSpeedAdvertising.reduceImpOrClk(campaignId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_CLK);

                    if (StringUtils.isNotBlank(clickLog.outCost) && adverSettleType == 2) {
                        AmountControl.hincrOfDayBudget(campaignId, day, Double.valueOf(clickLog.outCost) * 1000);
                        AmountControl.hincrOfCycleBudget(campaignId, Double.valueOf(clickLog.outCost) * 1000);

                        UniformSpeedAdvertising.reduceBugget(campaignId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_BUDGET, Double.valueOf(clickLog.outCost) * 1000);
                    }

                CLK_LOG.info(clickLog.toString());
                MONITOR_KAFKA.info(clickLog.toString());
                log.debug("a new click log has been flushed. sid = {}", sessionInfo.sid);

                //新的日志
                NewFormatLog newLog = new NewFormatLog();
                newLog.adver_clk = 1;

                newLog.event_date = FORMAT4DATE.format(new Date());
                newLog.token = tagid + "_" + clickId;
                newLog.ad_nomal_type = trackerCheat.value();
                newLog.package_name = clickLog.media;
                newLog.plat_id = "1";
                newLog.plat_name = "科达-优智";
                newLog.media_id = channel;
                newLog.media_name = mediaName;
                newLog.ad_name = "";
                newLog.media_ad_name = "";
                newLog.media_ad_id = adunit;
                newLog.ad_id = tagid;
                newLog.channel_id = channelId;
                newLog.adver_id = advertiserId;
                newLog.adver_name = CacheUtils.getAdverName(advertiserId);
                newLog.cam_id = campaignId;
                newLog.cam_name = CacheUtils.getCampName(campaignId);
                newLog.crv_id = creativeId;
                newLog.style_id = style;
                newLog.crv_name = CacheUtils.getCrvName(creativeId);
                newLog.os = os;
                newLog.dev_type = "app";
                newLog.ad_size = size;
                newLog.country = IpUtil.getCountryName(sessionInfo.reqRealIp);
                newLog.city = IpUtil.getCityName(sessionInfo.reqRealIp);
                newLog.province = IpUtil.getProvinceName(sessionInfo.reqRealIp);
                newLog.ip = sessionInfo.reqRealIp;

                if (StringUtils.isNotBlank(clickLog.inCost)) {
                    newLog.ori_cost = Double.parseDouble(clickLog.inCost);
                }
                if (StringUtils.isNotBlank(clickLog.outCost)) {
                    newLog.wastage_price = Double.parseDouble(clickLog.outCost);
                }

                NEW_LOG_KAFKA.info(newLog.toString());

                break;
            }
            case AdxConstants.MSG_ID_SERVICE_ADX_404: {
                if (!(objContext instanceof GSessionInfo)) {
                    log.error("!(objContext instanceof GSessionInfo)");
                    break;
                }
                GSessionInfo sessionInfo = (GSessionInfo) objContext;
                send404Resp(sessionInfo);
                break;
            }
            default: {
                log.error("invalid msgId = {}", msgId);
                break;
            }
        }
    }

    //计划每曝光1000个cpm都发一次邮件
    private void arr1000Cpm(String camId, int day) {
        String key = camId + ":day:" + day;
        String tmp = RedisClusterClient.hget(key, "dayImpSum");
        int impSum = StringUtils.isBlank(tmp) ? 0 : Integer.parseInt(tmp);

        if (impSum > IMP_SUM_4_SEND_EMAIL) {
            String mailKey = camId + ":" + "mail" + ":" + day;
            String countTmp = RedisClusterClient.getString(mailKey);
            int count = StringUtils.isBlank(countTmp) ? 0 : Integer.parseInt(countTmp);
            int lev = impSum / IMP_SUM_4_SEND_EMAIL;
            if (lev > count) {
                try {
                    RedisClusterClient.setex(mailKey, 24 * 60 * 60, count + 1 + "");
                    String camName = CacheUtils.getCampName(camId);
                    String msg = "计划 " + camId + " 曝光到达 " + lev*1000 + "CPM. 计划名为 " + camName;
                    SendEmail.sendEmail(msg);
                } catch (MessagingException e) {
                    log.error("send 1000CPM mail Exception. e = {}", e.getCause());
                }
            }
        }
    }

    private Double cpm2CpcPrice(String pTmp, String ctrStr) {
        //将CPM转为CPC
        Double ctr = Double.parseDouble(ctrStr);
        Double pCpm = Double.parseDouble(pTmp);
        Double pCpc = pCpm / ctr / 1000;
        //按照广告主服务费率进行提价

        return pCpc;
    }

    //按照广告主服务费率对Adx平台进行提价
    private String handleAdverPrice4Adx(String price, String advertiserId) {
        String pTmp = "";
        int rate = CacheUtils.getAdverServRate(advertiserId);
        Double tmp = Double.valueOf(price);
        tmp = tmp + tmp * rate / 100;
        pTmp = tmp.toString();
        return pTmp;
    }

    //处理ADX平台的价格
    private String handleAdxPrice(String priceMacro, String mediaShowId) {
        String pTmp = "";

        switch (mediaShowId) {
            default: {
                //对字符串priceMacro进行解密
                if (SysConfigManager.platTokenMap != null && (! SysConfigManager.platTokenMap.isEmpty()) && SysConfigManager.platTokenMap.containsKey(mediaShowId)) {
                    String token = SysConfigManager.platTokenMap.get(mediaShowId);
                    String price = null;
                    try {
                        price = AES.decrypt(priceMacro, token, true);
                    } catch (Exception e) {
                        log.error("价格解密失败.");
                    }
                    return price;
                } else {
                    log.error("没有在配置文件配置该平台的解密token, plat = {}", mediaShowId);
                    return "";
                }
            }
        }
    }

    //通过ip获取地域
    private String getRegion(String ip) {
        String region = "unknown";
        if (StringUtils.isNotBlank(ip)) {
            String regTmp = IpUtil.getCityName(ip);
            if (StringUtils.isBlank(regTmp) || StringUtils.equalsIgnoreCase("未知", regTmp)) {
                regTmp = IpUtil.getProvinceName(ip);
                if (StringUtils.isBlank(regTmp) || StringUtils.equalsIgnoreCase("未知", regTmp)) {
                    regTmp = IpUtil.getCountryName(ip);
                }
            }
            if (StringUtils.isNotBlank(regTmp) && (!StringUtils.equalsIgnoreCase("未知", regTmp))) {
                region = regTmp;
            }
        }

        return region;
    }

    @Override
    public boolean startTask() {
        return true;
    }

    @Override
    public boolean closeTask() {
        addMsg(new GMsg(Constants.MSG_ID_SYS_KILL, null));
        return true;
    }

    private void sendReqMsg(GSessionInfo sessionInfo, KdgProtocol.RequestBean.Impression imp) {
        if (imp == null) {
            log.error("广告位信息 imp 为 NULL. sid = {}", sessionInfo.sid);
            return;
        }
        RequestLog2 reqLog = new RequestLog2();
        reqLog.date = FORMAT4TIME.format(new Date());

        reqLog.timestamp = System.currentTimeMillis();
        reqLog.token = sessionInfo.sid;
        reqLog.logType = RequestLog2.VALUE_REQUEST_LOGTYPE;
        reqLog.channel = sessionInfo.downPlatId + "";
        reqLog.platId = sessionInfo.downPlatId + "";
        reqLog.channelId = StringUtils.isBlank(imp.channelid) ? "" : imp.channelid;
        reqLog.adSpace = imp.tagid;
        reqLog.adunit = imp.adunit;
        reqLog.kdgRequest = sessionInfo.requestBean;
        reqLog.kdgResponse = sessionInfo.responseBean;
        reqLog.mcf = "";
        reqLog.mcs = "";
        reqLog.adNormalType = sessionInfo.adNormalType.value(); // 设置请求反作弊状态

        reqLog.deviceType = StringUtils.equalsIgnoreCase(sessionInfo.requestBean.device.os, AdxConstants.OS_ANDROID) ? AdxConstants.OS_ANDROID :
                (StringUtils.equalsIgnoreCase(sessionInfo.requestBean.device.os, AdxConstants.OS_IOS) ? AdxConstants.OS_IOS : "others");


        if (sessionInfo.requestBean != null) {

            reqLog.mediaId = (sessionInfo.requestBean.app != null) ? sessionInfo.requestBean.app.id : "";
            reqLog.istest = (sessionInfo.requestBean.istest) ? 1 : 0;
            reqLog.ip = (sessionInfo.requestBean.device != null) ? sessionInfo.requestBean.device.ip : "";
            reqLog.region = getRegion(reqLog.ip);
        }

        if (sessionInfo.mediaType == 0) {
            String pkgName = "";
            reqLog.media = (StringUtils.isBlank(pkgName) ? sessionInfo.requestBean.app.bundle : pkgName);
        } else {
            if (sessionInfo.requestBean != null && sessionInfo.requestBean.site != null) {
                reqLog.page = StringUtils.isBlank(sessionInfo.requestBean.site.pageurl) ? "" : sessionInfo.requestBean.site.pageurl;
                reqLog.refer = StringUtils.isBlank(sessionInfo.requestBean.site.referrer) ? "" : sessionInfo.requestBean.site.referrer;
            } else {
                reqLog.page = "";
                reqLog.refer = "";
            }
        }

        Double ctr = 0.01;
        if (sessionInfo.ctrMap != null && sessionInfo.ctrMap.containsKey(sessionInfo.creativeIds.get(imp.id))
                && (!StringUtils.equalsIgnoreCase(sessionInfo.ctrMap.get(sessionInfo.creativeIds.get(imp.id)), "0.0"))) {

            ctr = Double.valueOf(sessionInfo.ctrMap.get(sessionInfo.creativeIds.get(imp.id)));
        }
        reqLog.ctr = ctr.toString();

        reqLog.adType = 0;
        reqLog.adSize = sessionInfo.impSizes.get(imp.id);
        if (sessionInfo.adMaterial != null && sessionInfo.isRespSucc) {
            AdvertiseMaterial ad = sessionInfo.adMaterial.get(imp.id);
            //过滤掉没有响应的imp
            if (ad == null) {
                return;
            }
            reqLog.accountId = ad.accountId;
            reqLog.advertiserId = ad.advertiserId;
            reqLog.campaignId = ad.campaignId;
            reqLog.creativeId = sessionInfo.creativeIds.get(imp.id);
            reqLog.adType = CacheUtils.getCreativeStyle(reqLog.creativeId);
            reqLog.isRespSucc = 1;
        } else {
            reqLog.adType = 0;
            reqLog.accountId = "";
            reqLog.advertiserId = "";
            reqLog.campaignId = "";
            reqLog.creativeId = "";
            reqLog.adSize = "";
        }

        REQ_LOG.info(reqLog.toString());
        log.info("send reqLog to kafka...");
        REQ_KAFKA.info(reqLog.toString());
    }

    private Map<String, String> parseInfoPara(String query) {

        if (StringUtils.isBlank(query)) {
            log.error("query is NULL, mid is NULL");
            return null;
        }
        log.debug("来自媒体的请求 query：{}", query);

        String[] urlParams = query.split("&");
        if (urlParams.length == 0) {
            log.error("query.split(\"&\") is null");
            return null;
        }
        Map<String, String> paraMap = new HashMap<>();
        for (String para : urlParams) {
            String[] kv = para.split("=");
            if (kv.length != 2) {
                log.warn("kv == null ||  kv.length != 2");
                continue;
            }
            paraMap.put(kv[0].trim(), kv[1].trim());
        }
        if (paraMap.isEmpty()) {
            return null;
        }
        return paraMap;
    }

    // parse the url parameters
    private Map<String, String> parseMonitorPara(GSessionInfo sessionInfo) {
        if (sessionInfo == null) {
            log.error("sessionInfo == null");
            return null;
        }
        if (sessionInfo.originReq == null) {
            log.error("sessionInfo.originReq == null, sid = {}", sessionInfo.sid);
            return null;
        }
        if (StringUtils.isBlank(sessionInfo.originReq.getQuery)) {
            log.error("StringUtils.isBlank(sessionInfo.originReq.getQuery), sid = {}", sessionInfo.sid);
            return null;
        }
        log.debug("来自下游的原始监控query：{},httpMethod={}, sid = {}", sessionInfo.originReq.getQuery, sessionInfo.originReq.type, sessionInfo.sid);

        String[] urlParams = sessionInfo.originReq.getQuery.split("&");
        if (urlParams.length == 0) {
            log.error("sessionInfo.originReq.getQuery.split(\"&\") is null, sid = {}", sessionInfo.sid);
            return null;
        }
        Map<String, String> paraMap = new HashMap<>();
        for (String para : urlParams) {
            String[] kv = para.split("=");
            if (kv.length != 2) {
                log.warn("kv == null || kv.length != 2, sid = {}", sessionInfo.sid);
                continue;
            }
            paraMap.put(kv[0].trim(), kv[1].trim());
        }
        if (paraMap.isEmpty()) {
            return null;
        }
        return paraMap;
    }

    private ByteBuffer[] form200Resp(GSessionInfo sessionInfo, HeaderMap header, ByteBuffer content) {

        if (sessionInfo == null) {
            log.error("form200Resp(GSessionInfo sessionInfo, HeaderMap header, ByteBuffer content),the sessionInfo is null!");
            return null;
        }

        if (header == null) {
            log.error("form200Resp(GSessionInfo sessionInfo, HeaderMap header, ByteBuffer content),the header is null!");
            return null;
        }

        switch (sessionInfo.downPlatId) {
            default: {
                log.info("70200, reqUrl({}) , downPlat = {}, sid = {}", sessionInfo.originReq.url, sessionInfo.downPlatId, sessionInfo.sid);
                header.put("Content-Type", "text/html; charset=UTF-8");
                return HttpEncode(200, header, content);
            }
        }

    }

    private ByteBuffer[] form204Resp(GSessionInfo sessionInfo, HeaderMap header) {

        switch (sessionInfo.downPlatId) {
            default: {
                log.info("70204, reqUrl({}) match to KDG PLAT, downPlat = {}, sid = {}", sessionInfo.originReq.url, sessionInfo.downPlatId, sessionInfo.sid);
                header.put("Content-Type", "application/json; charset=utf-8");
                return HttpEncode(204, header, KdgDownPlat.handlerErrorRsp(sessionInfo));
            }
        }
    }

    private void sendResp(GSessionInfo sessionInfo, int httpCode) {
        if (sessionInfo == null) {
            log.error("sessionInfo == null");
            return;
        }
        if (sessionInfo.callback == null) {
            log.error("sessionInfo.callback == null, sid = {}", sessionInfo.sid);
            return;
        }
        log.info("httpCode = {}, reqUrl({}), sid = {}", httpCode, sessionInfo.originReq.url, sessionInfo.sid);
        HeaderMap header = new HeaderMap();
//    header.put("Connection", "close");
        header.put("Access-Control-Allow-Origin", "*");

        ByteBuffer[] bytes = HttpEncode(httpCode, header, null);
        sessionInfo.callback.run(bytes);
    }

    private void send404Resp(GSessionInfo sessionInfo) {
        if (sessionInfo == null) {
            log.error("sessionInfo == null");
            return;
        }
        if (sessionInfo.callback == null) {
            log.error("sessionInfo.callback == null, sid = {}", sessionInfo.sid);
            return;
        }
        log.info("httpCode = 404, reqUrl({}), sid = {}", sessionInfo.originReq.url, sessionInfo.sid);
        HeaderMap header = new HeaderMap();
        header.put("Access-Control-Allow-Origin", "*");

        String body = null;
        if (sessionInfo.isMonitorCheck) {
            String hostName = "127.0.0.1";
            try {
                header.put("host-name", InetAddress.getLocalHost());
                if (InetAddress.getLocalHost() != null && InetAddress.getLocalHost().getHostName() != null) {
                    hostName = InetAddress.getLocalHost().getHostName();
                }
            } catch (Exception e) {
                log.error(" getLocalHost  Exception");
            }
            body = "hello adx, " + hostName + " " + new Date().getTime();
        }

        ByteBuffer[] bytes = StringUtils.isBlank(body) ? HttpEncode(404, header, body) : HttpEncode(200, header, body);
        sessionInfo.callback.run(bytes);
    }

    private void sendMonitorResp(GSessionInfo sessionInfo) {
        if (sessionInfo == null) {
            log.error("sessionInfo == null");
            return;
        }
        if (sessionInfo.callback == null) {
            log.error("sessionInfo.callback == null, sid = {}", sessionInfo.sid);
            return;
        }
        log.info("httpCode = 200, reqUrl({})->MonitorResp, sid = {}", sessionInfo.originReq.url, sessionInfo.sid);
        HeaderMap header = new HeaderMap();
//    header.put("Connection", "Keep-Alive");
//    header.put("Connection", "close");
        header.put("Access-Control-Allow-Origin", "*");

        ByteBuffer[] bytes = HttpEncode(200, header, null);
        sessionInfo.callback.run(bytes);
    }

    private void sendRedirectMonitorResp(GSessionInfo sessionInfo, String redirectedUrl) {
        if (sessionInfo == null) {
            log.error("sessionInfo == null");
            return;
        }
        if (sessionInfo.callback == null) {
            log.error("sessionInfo.callback == null, sid = {}", sessionInfo.sid);
            return;
        }

        log.info("httpCode = 200, reqUrl({})->RedirectMonitorResp, sid = {}", sessionInfo.originReq.url, sessionInfo.sid);

        if (StringUtils.isBlank(redirectedUrl)) {
            log.error("StringUtils.isBlank(redirectedUrl),sid = {}", sessionInfo.sid);
            return;// todo 不能直接返回无论如何要给客户端响应
        }
        String targetUrl = URLDecoder.decode(redirectedUrl);
        log.info("重定向地址 = {}, sid = {}", targetUrl, sessionInfo.sid);
        HeaderMap header = new HeaderMap();
//    header.put("Connection", "Keep-Alive");
//    header.put("Connection", "close");
        header.put("Access-Control-Allow-Origin", "*");
        header.put("Location", targetUrl);
        ByteBuffer[] bytes = HttpEncode(302, header, null);
        sessionInfo.callback.run(bytes);
    }

    /**
     * 功能描述: 判断监测是否作弊
     *   
     * @param impressLog 曝光监测
     * @return 监测作弊状态{@com.kdg.gnome.anti.resp.AntiCheatStatus}
     * [2018年6月14日]创建文件 by lh.qiu
     */
    private AntiCheatStatus isTrackerCheat(ImpressLog impressLog) {
    	// 监测反作弊实体类
    	TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
        trackerAntiCheatVO.setToken(impressLog.token);
        trackerAntiCheatVO.setIp(impressLog.ip);
        // 返回曝光监测是否作弊
		return ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
    }
    
    /**
     * 功能描述: 判断监测是否作弊
     *   
     * @param clickLog 点击监测
     * @return 监测作弊状态{@com.kdg.gnome.anti.resp.AntiCheatStatus}
     * [2018年6月14日]创建文件 by lh.qiu
     */
    private AntiCheatStatus isTrackerCheat(ClickLog clickLog) {
    	// 监测反作弊实体类
    	TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
    	trackerAntiCheatVO.setToken(clickLog.token);
    	trackerAntiCheatVO.setIp(clickLog.ip);
    	// 返回曝光监测是否作弊
    	return ClickTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
    }
    
    /**
     * 功能描述: 判断请求是否作弊
     *   
     * @param requestBean 请求参数
     * @return 请求作弊状态{@com.kdg.gnome.anti.resp.AntiCheatStatus}
     * [2018年6月19日]创建文件 by lh.qiu
     */
    private AntiCheatStatus isRequestCheat(KdgProtocol.RequestBean requestBean) {
    	// 创建请求反作弊实体类
    	RequestAntiCheatVO requestAntiCheatVO = new RequestAntiCheatVO();
    	// 获得请求参数中的IP地址
    	String requestParamIp = requestBean == null ? "" : requestBean.device == null ? "" : requestBean.device.ip;
    	requestAntiCheatVO.setIp(requestParamIp);
    	
    	// 判断请求是否作弊
		return DspRequestAntiCheatHandler.isReqeustCheat(requestAntiCheatVO);
    }
    
}
