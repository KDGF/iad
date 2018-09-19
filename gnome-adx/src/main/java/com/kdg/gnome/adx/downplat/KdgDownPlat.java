package com.kdg.gnome.adx.downplat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.monitor.NewFormatLog;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.adx.share.dao.AdvertiseMaterial;
import com.kdg.gnome.adx.share.dao.Creative;
import com.kdg.gnome.adx.share.dao.StatusConstants;
import com.kdg.gnome.share.task.AES;
import com.kdg.gnome.util.IpUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hbwang on 2017/12/11
 */
public class KdgDownPlat extends BaseDownPlatHandler {

    private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");
    private static final Logger NEW_LOG_KAFKA = LogManager.getLogger("NEW_LOG_KAFKA");
    private static final SimpleDateFormat FORMAT4DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean handlerReq(GSessionInfo sessionInfo) {
        if (sessionInfo == null) {
            log.error("sessionInfo == null");
            return false;
        }
        // 解析请求
        if (sessionInfo.originReq == null) {
            log.error("sessionInfo.originRequest is null. sid = {}", sessionInfo.sid);
            return false;
        }

        String request = new String(sessionInfo.originReq.postBody);

        log.info("originRequest : {}", request.toString());


        KdgProtocol.RequestBean requestBean = gson.fromJson(request, KdgProtocol.RequestBean.class);
        // 解析request 检查参数必要性
        if (!checkParam(requestBean, sessionInfo)) {
            log.error("param check dose not pass. sid = {}", sessionInfo.sid);
            return false;
        }
        sessionInfo.requestBean = requestBean;
        log.info("request has been checked success....sid = {}", sessionInfo.sid);

        // 将媒体传的广告位id 转为科达内部 广告位id
        String os = requestBean.device.os;
        sessionInfo.os = StringUtils.equalsIgnoreCase(os, AdxConstants.OS_ANDROID) ? 1 : 2;

        if (sessionInfo.requestBean.app == null && sessionInfo.requestBean.site != null) {
            sessionInfo.mediaType = 1;
        } else {
            sessionInfo.mediaType = 0;
        }

        return true;
    }

    /**
     * 检查请求参数合法性
     */
    private boolean checkParam(KdgProtocol.RequestBean request, GSessionInfo sessionInfo) {

        if (StringUtils.isBlank(request.id)) {
            log.error("error. request.id == null, sid = {}", sessionInfo.sid);
            return false;
        }

        if (request.imp == null || request.imp.isEmpty()) {
            log.error("error. request.imp is empty. sid = {}", sessionInfo.sid);
            return false;
        }
        for (KdgProtocol.RequestBean.Impression imp : request.imp) {
            if (StringUtils.isBlank(imp.id)) {
                log.error("error. request.imp.id == null, sid = {}", sessionInfo.sid);
                return false;
            }

            if (StringUtils.isBlank(imp.tagid)) {
                log.error("error. request.imp.tagid == null, sid = {}", sessionInfo.sid);
                return false;
            }

            if (imp.opentype == null) {
                log.error("error. request.imp.openType(广告打开类型) == null, sid = {}", sessionInfo.sid);
                return false;
            }
        }

        if (request.app == null && request.site == null) {
            log.error("error. app or site == null, sid = {}", sessionInfo.sid);
            return false;
        }

        if (request.device == null) {
            log.error("error. request.device == null, sid == {}", sessionInfo.sid);
            return false;
        }

        if (StringUtils.isBlank(request.device.ip)) {
            log.error("error. device.ip == null, sid = {}", sessionInfo.sid);
            return false;
        }

        if (StringUtils.isBlank(request.device.os)) {
            log.error("error. device.os  is  NULL. sid = {}", sessionInfo.sid);
            return false;
        }
        return true;
    }

    public static ByteBuffer handlerErrorRsp(GSessionInfo sessionInfo) {

        sessionInfo.responseBean = KdgProtocol.ErrorResponse(sessionInfo);
        if (sessionInfo.requestBean == null) {
            return null;
        }
        String rspStr = gson.toJson(sessionInfo.responseBean);

        try {
            return ByteBuffer.wrap(rspStr.getBytes("utf-8"));
//            return rspStr;
        } catch (Exception e) {
            log.error("ByteBuffer.wrap(rspStr.getBytes(\"utf-8\")): [{}], sid = {}", e.getMessage(), sessionInfo.sid);
            return null;
        }
    }

    @Override
    public ByteBuffer handlerRsp(KdgProtocol.ResponseBean responseBean, GSessionInfo sessionInfo) {

        KdgProtocol.ResponseBean response = handleRespDefault(sessionInfo, sessionInfo.adMaterial);

        if (response == null) {
            log.debug("handle response failed(无响应). sid = {}", sessionInfo.sid);
            return null;
        } else {
            log.debug("response : " + response.toString());
            sessionInfo.isRespSucc = true;
        }

        try {
            return ByteBuffer.wrap(response.toString().getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ThreadLocal<Random> randomThreadLocal = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random(System.nanoTime());
        }
    };

    public static Creative getCreative(AdvertiseMaterial adMaterial) {
        List<Creative> creatives = adMaterial.creatives;
        Creative creative = null;
        if (creatives == null || creatives.isEmpty()) {
            return null;
        } else if (creatives.size() == 1) {
            creative = creatives.get(0);
        } else {
            //随机获取订单
            List<Creative> nativeCres = new ArrayList<>();
            for (Creative c : creatives) {
                if (c.type == StatusConstants.CREATIVE_TYPE_NATIVE) {
                    nativeCres.add(c);
                }
            }
            if (!nativeCres.isEmpty()) {
                creatives = nativeCres;
            }
            int count = creatives.size();
            int num = randomThreadLocal.get().nextInt(count);
            creative = creatives.get(num);
        }
        return creative;
    }
    /**
     * 处理组装响应 KdgProtocol
     */
    public KdgProtocol.ResponseBean handleRespDefault(GSessionInfo sessionInfo, Map<String, AdvertiseMaterial> adMaterial) {

        if (adMaterial == null) {
            log.debug("adSourceMaterial order is null");
            return null;
        }

        KdgProtocol.ResponseBean responseBean = new KdgProtocol.ResponseBean();
        responseBean.id = sessionInfo.requestBean.id;
        responseBean.buyerid = "优智";

        KdgProtocol.ResponseBean.Seat seat = new KdgProtocol.ResponseBean.Seat();
        seat.bids = new ArrayList<>();

        for (KdgProtocol.RequestBean.Impression imp : sessionInfo.requestBean.imp) {
            KdgProtocol.ResponseBean.Seat.Bid bid = new KdgProtocol.ResponseBean.Seat.Bid();
            bid.id = sessionInfo.sid;
            bid.impid = imp.id;
            AdvertiseMaterial ad = adMaterial.get(imp.id);
            if(ad == null){
                continue;
            }
            Creative creative = getCreative(ad);
            if (creative == null) {
                continue;
            }
            bid.opentype = creative.linkType + "";
            bid.price = sessionInfo.impPriceMap.get(imp.id);
            if (sessionInfo.isImpDeal.containsKey(imp.id) && sessionInfo.isImpDeal.get(imp.id)) {
                bid.dealid = imp.pmp.deals.get(0).id;
            }

            bid.adverid = ad.advertiserId;
            bid.crid = creative.id;
            sessionInfo.creativeIds.put(imp.id,creative.id);
            switch (creative.type) {
                case 1: {
                    bid.banner = new KdgProtocol.ResponseBean.Seat.Bid.Banner();
                    bid.banner.curl = creative.filePath;
                    bid.banner.h = creative.height;
                    bid.banner.w = creative.width;
                    sessionInfo.impSizes.put(imp.id, creative.width + "*" + creative.height);
                    bid.styleid = creative.style;

                    if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                        bid.banner.curl = transHttpOrHttps(bid.banner.curl, sessionInfo.requestBean.secure);
                    }
                    break;
                }
                case 2: {
                    bid.video = new KdgProtocol.ResponseBean.Seat.Bid.Video();
                    bid.video.curl = creative.filePath;
                    bid.video.h = creative.height;
                    bid.video.w = creative.width;
                    bid.video.duration = 0;
                    bid.styleid = creative.style;

                    if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                        bid.video.curl = transHttpOrHttps(bid.video.curl, sessionInfo.requestBean.secure);
                    }
                    break;
                }
                case 3: {
                    bid.native$ = new KdgProtocol.ResponseBean.Seat.Bid.Native();
                        switch (creative.style) {
                            case 4: {
                                bid.native$.title = creative.templateContent.title;
                                bid.native$.description = creative.templateContent.desc;
                                bid.native$.image = new ArrayList<>();
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image.url = creative.templateContent.image.context.img;
                                image.w = creative.templateContent.image.w;
                                image.h = creative.templateContent.image.h;


                                if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                                    image.url = transHttpOrHttps(image.url, sessionInfo.requestBean.secure);
                                }

                                sessionInfo.impSizes.put(imp.id, image.w + "*" + image.h);
                                bid.styleid = 4;
                                bid.native$.image.add(image);
                                break;
                            }
                            case 5: {
                                bid.native$.title = creative.templateContent.title;
                                bid.native$.description = creative.templateContent.desc;
                                bid.native$.icon = new KdgProtocol.ResponseBean.Seat.Bid.Native.Icon();
                                bid.native$.icon.url = creative.templateContent.icon;
                                bid.native$.image = new ArrayList<>();
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image.url = creative.templateContent.image.context.img;
                                image.w = creative.templateContent.image.w;
                                image.h = creative.templateContent.image.h;

                                if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                                    image.url = transHttpOrHttps(image.url, sessionInfo.requestBean.secure);
                                }


                                sessionInfo.impSizes.put(imp.id, image.w + "*" + image.h);
                                bid.native$.image.add(image);
                                bid.styleid = 5;
                                break;
                            }
                            case 6: {
                                bid.native$.title = creative.templateContent.title;
                                bid.native$.description = creative.templateContent.desc;
                                bid.native$.image = new ArrayList<>();
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image1 = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image1.url = creative.templateContent.image.context.img1;
                                image1.w = creative.templateContent.image.w;
                                image1.h = creative.templateContent.image.h;
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image2 = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image2.url = creative.templateContent.image.context.img2;
                                image2.w = creative.templateContent.image.w;
                                image2.h = creative.templateContent.image.h;
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image3 = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image3.url = creative.templateContent.image.context.img3;
                                image3.w = creative.templateContent.image.w;
                                image3.h = creative.templateContent.image.h;

                                if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                                    image1.url = transHttpOrHttps(image1.url, sessionInfo.requestBean.secure);
                                    image2.url = transHttpOrHttps(image2.url, sessionInfo.requestBean.secure);
                                    image3.url = transHttpOrHttps(image3.url, sessionInfo.requestBean.secure);
                                }


                                sessionInfo.impSizes.put(imp.id, image1.w + "*" + image1.h);
                                bid.native$.image.add(image1);
                                bid.native$.image.add(image2);
                                bid.native$.image.add(image3);
                                bid.styleid = 6;
                                break;
                            }
                            case 7: {
                                bid.native$.title = creative.templateContent.title;
                                bid.native$.description = creative.templateContent.desc;
                                bid.native$.icon = new KdgProtocol.ResponseBean.Seat.Bid.Native.Icon();
                                bid.native$.icon.url = creative.templateContent.icon;
                                bid.native$.image = new ArrayList<>();
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image1 = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image1.url = creative.templateContent.image.context.img1;
                                image1.w = creative.templateContent.image.w;
                                image1.h = creative.templateContent.image.h;
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image2 = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image2.url = creative.templateContent.image.context.img2;
                                image2.w = creative.templateContent.image.w;
                                image2.h = creative.templateContent.image.h;
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image3 = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image3.url = creative.templateContent.image.context.img3;
                                image3.w = creative.templateContent.image.w;
                                image3.h = creative.templateContent.image.h;

                                if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                                    image1.url = transHttpOrHttps(image1.url, sessionInfo.requestBean.secure);
                                    image2.url = transHttpOrHttps(image2.url, sessionInfo.requestBean.secure);
                                    image3.url = transHttpOrHttps(image3.url, sessionInfo.requestBean.secure);
                                }

                                sessionInfo.impSizes.put(imp.id, image1.w + "*" + image1.h);
                                bid.native$.image.add(image1);
                                bid.native$.image.add(image2);
                                bid.native$.image.add(image3);
                                bid.styleid = 7;
                                break;
                            }
                            case 8: {
                                bid.native$.title = creative.templateContent.title;
                                bid.native$.description = creative.templateContent.desc;
                                bid.native$.image = new ArrayList<>();
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image.url = creative.templateContent.image.context.img;
                                image.w = creative.templateContent.image.w;
                                image.h = creative.templateContent.image.h;

                                bid.native$.video = new KdgProtocol.ResponseBean.Seat.Bid.Native.Video();
                                bid.native$.video.url = creative.templateContent.video.url;
                                bid.native$.video.h = creative.templateContent.video.h;
                                bid.native$.video.w = creative.templateContent.video.w;
                                bid.native$.video.size = creative.size / 1024.0F;
                                if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                                    image.url = transHttpOrHttps(image.url, sessionInfo.requestBean.secure);
                                    bid.native$.video.url = transHttpOrHttps(bid.native$.video.url, sessionInfo.requestBean.secure);
                                }
                                bid.native$.image.add(image);
                                sessionInfo.impSizes.put(imp.id, bid.native$.video.w + "*" + bid.native$.video.h);
                                bid.styleid = 8;
                                break;
                            }
                            case 9: {
                                bid.native$.title = creative.templateContent.title;
                                bid.native$.description = creative.templateContent.desc;
                                bid.native$.icon = new KdgProtocol.ResponseBean.Seat.Bid.Native.Icon();
                                bid.native$.icon.url = creative.templateContent.icon;
                                bid.native$.image = new ArrayList<>();
                                KdgProtocol.ResponseBean.Seat.Bid.Native.Image image = new KdgProtocol.ResponseBean.Seat.Bid.Native.Image();
                                image.url = creative.templateContent.image.context.img;
                                image.w = creative.templateContent.image.w;
                                image.h = creative.templateContent.image.h;

                                bid.native$.video = new KdgProtocol.ResponseBean.Seat.Bid.Native.Video();
                                bid.native$.video.url = creative.templateContent.video.url;
                                bid.native$.video.h = creative.templateContent.video.h;
                                bid.native$.video.w = creative.templateContent.video.w;
                                bid.native$.video.size = creative.size / 1024 / 1024.0F;
                                if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                                    image.url = transHttpOrHttps(image.url, sessionInfo.requestBean.secure);
                                    bid.native$.video.url = transHttpOrHttps(bid.native$.video.url, sessionInfo.requestBean.secure);
                                }
                                bid.native$.image.add(image);

                                sessionInfo.impSizes.put(imp.id, image.w + "*" + image.h);
                                bid.styleid = 9;
                                break;
                            }
                            default: {
                                log.error("error. native.style = {}, sid = {}", creative.style, sessionInfo.sid);
                                return null;
                            }
                        }
                        if(bid.native$!=null){
                            break;
                        }
//                    }
                    break;
                }
                default: {
                    log.error("error.不支持的广告类型.sid = {}", sessionInfo.sid);
                    return null;
                }
            }

            String impress = AdxSystem.sysMgrModule().getSysConfigManager().serverConfig.getAdxImpressUrlPrefix();
            String click = AdxSystem.sysMgrModule().getSysConfigManager().serverConfig.getAdxClickUrlPrefix();

            if (sessionInfo.requestBean != null && sessionInfo.requestBean.secure != null) {
                impress = transHttpOrHttps(impress, sessionInfo.requestBean.secure);
                click = transHttpOrHttps(click, sessionInfo.requestBean.secure);
            }

            if (StringUtils.isBlank(impress) || StringUtils.isBlank(click)) {
                log.error("error. 加载科达监控为空");
                return null;
            }


            //添加科达监控
            String size = creative.width + "*" + creative.height;
            impress = makupMonitor(impress, sessionInfo, ad, imp, bid.styleid, size, bid.crid, MONITOR_IMPRESS);
            click = makupMonitor(click, sessionInfo, ad, imp, bid.styleid, size, bid.crid, MONITOR_CLICK);
            if (StringUtils.isBlank(impress) || StringUtils.isBlank(click)) {
                log.error("组装监控参数失败.");
                return null;
            }

            bid.trackurls = new ArrayList<>();
            bid.trackurls.add(impress);
            bid.trackurls.addAll(creative.impMonitor);

            if (creative.linkType == AdxConstants.ADUNIT_OPEN_TYPE_LANDING) {
                if (StringUtils.isBlank(creative.landing_page)) {
                    log.error("error. creative.landingPage is NULL. sid = {}", sessionInfo.sid);
                    return null;
                }
                click += "&redirect=" + URLEncoder.encode(creative.landing_page);
                if (StringUtils.isNotBlank(creative.clkMonitor)) {
                    bid.clicktracking = new ArrayList<>();
                    bid.clicktracking.add(creative.clkMonitor);
                    //添加302点击监控
                }

                //替换王新的宏
                click = click.replace("_MEDIA_", sessionInfo.downPlatId + "");     //媒体id
                click = click.replace("_PLAT_", "1");                              //平台id
                String devId = null;
                if (StringUtils.equalsIgnoreCase(AdxConstants.OS_ANDROID, sessionInfo.requestBean.device.os)) {
                    devId = sessionInfo.requestBean.device.imeiplain;
                }else if (StringUtils.equalsIgnoreCase(AdxConstants.OS_IOS, sessionInfo.requestBean.device.os)) {
                    devId = sessionInfo.requestBean.device.ifa;
                }

                if (StringUtils.isNotBlank(devId)) {
                    click = click.replace("_DEVICE_", devId);                    //设备id
                }
                bid.clickthrough = click;
            } else if (creative.linkType == AdxConstants.ADUNIT_OPEN_TYPE_DOWNLOAD) {
                if (StringUtils.isBlank(creative.landing_page)) {
                    log.error("error. creative.landingPage is NULL. sid = {}", sessionInfo.sid);
                    return null;
                }

                String downLoadStr = sessionInfo.os == 1 ? creative.landing_page : creative.deepLink;
                if (StringUtils.isBlank(downLoadStr)) {
                    log.error("os = {}, but deeplink(landing_page或deepLink字段) is NULL.", sessionInfo.os);
                    return null;
                }
                DLOrDLJson downLoadJson = gson.fromJson(downLoadStr, DLOrDLJson.class);
                String ldpage = downLoadJson.landing_page;
                bid.bundle = downLoadJson.pkgname;
                if (StringUtils.isBlank(ldpage)) {
                    log.error("下载类素材的落地页结构错误, creative.id = {}, session.os = {}", creative.id, sessionInfo.os);
                    return null;
                }
                bid.downloadurl = ldpage;
                bid.clicktracking = new ArrayList<>();
                bid.clicktracking.add(click);

                if (StringUtils.isNotBlank(creative.clkMonitor)) {
                    bid.clicktracking.add(creative.clkMonitor);
                }
            } else if (creative.linkType == AdxConstants.ADUNIT_OPEN_TYPE_DEEPLINK) {
                if (StringUtils.isBlank(creative.landing_page)) {
                    log.error("error. creative.landingPage is NULL. sid = {}", sessionInfo.sid);
                    return null;
                }

                String deepLinkStr = sessionInfo.os == 1 ? creative.landing_page : creative.deepLink;
                if (StringUtils.isBlank(deepLinkStr)) {
                    log.error("os = {}, but deeplink(landing_page或deepLink字段) is NULL.", sessionInfo.os);
                    return null;
                }
                DLOrDLJson deepLinkJson = gson.fromJson(deepLinkStr, DLOrDLJson.class);
                bid.clickthrough = deepLinkJson.landing_page;
                bid.dplurl = deepLinkJson.deep_link;

                bid.clicktracking = new ArrayList<>();
                bid.clicktracking.add(click);

                if (StringUtils.isNotBlank(creative.clkMonitor)) {
                    bid.clicktracking.add(creative.clkMonitor);
                }
            }

            String ip = sessionInfo.requestBean.device.ip;
            String country = IpUtil.getCountryName(ip);
            String province = IpUtil.getProvinceName(ip);
            String city = IpUtil.getCityName(ip);

            //看收到请求后是否经过了500ms
            int timeout  = AdxSystem.sysMgrModule().getSysConfigManager().timeOut;
            long time = System.currentTimeMillis();
            if (time - sessionInfo.millRecvReq >= timeout) {
                sessionInfo.isRespSucc = false;
                log.warn("收到请求后已经过了 {} ms, 超过 {} ms, 本次不再投放", time - sessionInfo.millRecvReq, timeout);
                return null;
            }

            NewFormatLog newLog = new NewFormatLog();
            newLog.adver_rsp_succ_req = 1;

            newLog.event_date = FORMAT4DATE.format(new Date());
            newLog.token = imp.tagid + "_" + sessionInfo.sid;

            newLog.media_id = sessionInfo.downPlatId + "";
            newLog.ad_name = "";
            newLog.plat_name = "科达-优智";
            newLog.plat_id = "1";
            if (sessionInfo.requestBean != null && sessionInfo.requestBean.app != null) {
                newLog.media_name = sessionInfo.requestBean.app.name;
                newLog.package_name = sessionInfo.requestBean.app.bundle;
            }
            newLog.channel_id = imp.channelid;
            newLog.media_ad_id = imp.adunit;
            newLog.media_ad_name = "";
            newLog.ad_id = imp.tagid;

            newLog.adver_id = ad.advertiserId;
            newLog.adver_name = CacheUtils.getAdverName(ad.advertiserId);
            newLog.cam_id = ad.campaignId;
            newLog.cam_name = ad.campaign.name;
            newLog.crv_id = creative.id;
            newLog.style_id = creative.style + "";
            newLog.crv_name = creative.name;
            newLog.os = sessionInfo.requestBean.device.os;
            newLog.dev_type = sessionInfo.requestBean.device.devicetype + "";
            newLog.ad_size = sessionInfo.impSizes.get(imp.id);
            newLog.country = country;
            newLog.city = city;
            newLog.province = province;
            newLog.ip = ip;

            NEW_LOG_KAFKA.info(newLog.toString());

            seat.bids.add(bid);
        }

        responseBean.seatbid = new ArrayList<>();
        responseBean.seatbid.add(seat);


        sessionInfo.responseBean = responseBean;
        return responseBean;

    }

    //根据请求中的https要求转换url的格式
    public String transHttpOrHttps(String url, int isHttps) {

        if (isHttps == 1 && (! url.contains("https"))) {
            url = url.replaceFirst("http", "https");
        } else if (isHttps == 0 && url.contains("https")) {
            url = url.replaceFirst("https", "http");
        }
        return url;
    }

    public final static int MONITOR_IMPRESS = 1;
    public final static int MONITOR_CLICK = 2;

    public String makupMonitor(String   base, GSessionInfo sessionInfo, AdvertiseMaterial ad,
                               KdgProtocol.RequestBean.Impression imp, int style, String size, String crid, int type) {

        StringBuilder tmp = new StringBuilder();
        tmp.append(base).append("?p=");

        StringBuilder monitor = new StringBuilder();
        if (type == MONITOR_IMPRESS) {
            monitor.append("impressId=");
        } else if (type == MONITOR_CLICK) {
            monitor.append("clickId=");
        }
        monitor.append(sessionInfo.sid);
        monitor.append("&accId=").append(ad.accountId);
        monitor.append("&advId=").append(ad.advertiserId);
        monitor.append("&camId=").append(ad.campaignId);
        monitor.append("&crtId=").append(crid);
        monitor.append("&tagid=").append(imp.tagid);
        monitor.append("&chn=").append(sessionInfo.downPlatId);
        monitor.append("&adunit=").append(imp.adunit);
        monitor.append("&size=").append(size);
        monitor.append("&style=").append(style);

        if (sessionInfo.mediaType == 0) {
            if (sessionInfo.requestBean.app != null) {
                monitor.append("&pkg=").append(sessionInfo.requestBean.app.bundle);
                monitor.append("&mna=").append(sessionInfo.requestBean.app.name);
            }
        } else {
            String pageUrl = "";
            String refer = "";
            if(sessionInfo.requestBean.site != null) {
                 pageUrl = sessionInfo.requestBean.site.pageurl;
                 refer  = sessionInfo.requestBean.site.referrer;
            }
            monitor.append("&page=").append(pageUrl);
            monitor.append("&refer=").append(refer);
        }


        //标识测试流量
        if (sessionInfo.requestBean != null && sessionInfo.requestBean.istest) {
            monitor.append("&test=").append(1);
        }

        String deviceId = null;
        if (StringUtils.equalsIgnoreCase(sessionInfo.requestBean.device.os, AdxConstants.OS_ANDROID)) {
            deviceId = StringUtils.isNotBlank(sessionInfo.requestBean.device.imeiplain) ? sessionInfo.requestBean.device.imeiplain :
                    (StringUtils.isNotBlank(sessionInfo.requestBean.device.imeimd5) ? sessionInfo.requestBean.device.imeimd5 : sessionInfo.requestBean.device.imeisha1);
            monitor.append("&os=").append(AdxConstants.OS_ANDROID);
        } else if (StringUtils.equalsIgnoreCase(sessionInfo.requestBean.device.os, AdxConstants.OS_IOS)) {
            deviceId = sessionInfo.requestBean.device.ifa;
            monitor.append("&os=").append(AdxConstants.OS_IOS);
        } else {
            monitor.append("&os=others");
        }

        monitor.append("&devId=").append(deviceId);

        //广告位所在频道id
        if (StringUtils.isNotBlank(imp.channelid)) {
            monitor.append("&chnId=").append(imp.channelid);
        }

        //先将ctr记录下来
        Double ctr = AdxSystem.sysMgrModule().getSysConfigManager().defaultCtr;
        if (sessionInfo.ctrMap != null && sessionInfo.ctrMap.containsKey(sessionInfo.creativeIds.get(imp.id))
                && (!StringUtils.equalsIgnoreCase(sessionInfo.ctrMap.get(sessionInfo.creativeIds.get(imp.id)), "0.0"))) {

            ctr = Double.valueOf(sessionInfo.ctrMap.get(sessionInfo.creativeIds.get(imp.id)));
        }
        monitor.append("&ctr=").append(ctr);



        String aesToken = AdxSystem.sysMgrModule().getSysConfigManager().serverConfig.aesToken;
        String info = null;
        try {
            info = AES.encrypt(monitor.toString(), aesToken, true);
        } catch (Exception e) {
            log.error("monitor encrypt Exception.");
        }

        info = info + "&price=" + AdxConstants.PRICE_MACROS;
        info = info + "$bt=" + AdxConstants.SETTLE_TYPE;

        if (StringUtils.isBlank(info)) {
            log.error("monitor is BLANK.");
        }

        return tmp.append(info).toString();
    }

    public static class DLOrDLJson {
        public String landing_page;
        public String deep_link;
        public String pkgname;
    }
}
