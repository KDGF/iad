package com.kdg.gnome.adx.module;

import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.monitor.NewFormatLog;
import com.kdg.gnome.adx.order.BaseOrderHandler;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.adx.share.dao.AdvertiseMaterial;
import com.kdg.gnome.util.IpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by hbwang on 2018/5/10
 */
public class MergeReqHandler {
    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");

    protected static class MakeUpImp implements Callable<AdvertiseMaterial> {

        private KdgProtocol.RequestBean.Impression imp;

        private GSessionInfo sessInfo;
        private BaseOrderHandler baseOrderHandler;
        private List<AdvertiseMaterial> ads;

        MakeUpImp(KdgProtocol.RequestBean.Impression imp,
                  BaseOrderHandler baseOrderHandler,
                  List<AdvertiseMaterial> ads,
                  GSessionInfo sessInfo) {
            this.sessInfo = sessInfo;
            this.imp = imp;
            this.baseOrderHandler = baseOrderHandler;
            this.ads = ads;
        }

        @Override
        public AdvertiseMaterial call() throws Exception {
            KdgProtocol.RequestBean.Impression impNew = this.imp;
            if (impNew == null) {
                return null;
            }
            synchronized (MakeUpImp.class) {
                sessInfo.requestBean.imp.add(impNew);
            }

            if (ads == null || ads.isEmpty()) {
                return null;
            }
            List<AdvertiseMaterial> ads4Imp = null;
            try{
               ads4Imp = baseOrderHandler.directAdMaterialsByOtherConditions(ads, impNew, sessInfo);
            } catch (RuntimeException  e){
                log.error(e.getMessage());
            }

            //pmp 或 比价
            List<AdvertiseMaterial> hightextValue = baseOrderHandler.getFitAdsByDeal(ads4Imp, sessInfo, impNew);
            if (hightextValue == null || hightextValue.isEmpty()) {
                return null;
            }

            makeUpNewLog(hightextValue, sessInfo, impNew);
            AdvertiseMaterial ad = BaseOrderHandler.getOptimalAdMaterial(hightextValue);
            return ad;
        }

        private static final SimpleDateFormat FORMAT4DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private static final Logger NEW_LOG_KAFKA = LogManager.getLogger("NEW_LOG_KAFKA");

        private void makeUpNewLog(List<AdvertiseMaterial> ads, GSessionInfo sessionInfo, KdgProtocol.RequestBean.Impression imp) {
            NewFormatLog newLog = new NewFormatLog();

            //记录唯一请求日志

            String ip = sessionInfo.requestBean.device.ip;
            String country = IpUtil.getCountryName(ip);
            String province = IpUtil.getProvinceName(ip);
            String city = IpUtil.getCityName(ip);

            newLog.event_date = FORMAT4DATE.format(new Date());
            newLog.token = imp.tagid + "_" + sessionInfo.sid;

            newLog.media_id = sessionInfo.downPlatId + "";
            newLog.ad_name = "";
            newLog.plat_id = "1";
            newLog.plat_name = "科达-优智";
            if (sessionInfo.requestBean != null && sessionInfo.requestBean.app != null) {
                newLog.media_name = sessionInfo.requestBean.app.name;
                newLog.package_name = sessionInfo.requestBean.app.bundle;
            }

            newLog.channel_id = imp.channelid;
            newLog.os = sessionInfo.requestBean.device.os;
            newLog.dev_type = sessionInfo.requestBean.device.devicetype + "";
            newLog.country = country;
            newLog.city = city;
            newLog.province = province;
            newLog.ip = ip;

            newLog.media_ad_id = imp.adunit;
            newLog.media_ad_name = "";
            newLog.ad_id = imp.tagid;

            //先ads将按广告主聚合
            List<String> advers = new ArrayList<>();
            for (AdvertiseMaterial ad : ads) {
                if (! advers.contains(ad.advertiserId)) {
                    advers.add(ad.advertiserId);
                }
            }

            for (String adver : advers) {
                newLog.adver_id = adver;
                newLog.adver_name = CacheUtils.getAdverName(adver);
                newLog.adver_rec_req = 1;

                NEW_LOG_KAFKA.info(newLog.toString());

                newLog.adver_rec_req = 0;
                newLog.adver_rsp_req = 1;

                NEW_LOG_KAFKA.info(newLog.toString());
            }
        }
    }
}
