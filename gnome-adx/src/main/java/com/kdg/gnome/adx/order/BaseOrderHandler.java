package com.kdg.gnome.adx.order;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.adx.share.dao.*;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.share.redisutil.RedisClusterClient;
import com.kdg.gnome.util.IpUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * Created by xyzhao on 2018/05/09
 */

public abstract  class BaseOrderHandler {
    private static final Logger LOGGER = LogManager.getLogger("ES_OUT_INFO");
    private static Gson gson = new Gson();
    private static ThreadLocal<Random> randomThreadLocal = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random(System.nanoTime());
        }
    };
    private static final int LINK_TYPE_H5 = 1;
    private static final int LINK_TYPE_DOWNLOAD = 2;
    private static final int LINK_TYPE_DEEPLINK = 3;
    public  abstract List<AdvertiseMaterial> loadOrders(GSessionInfo sessionInfo);
    public  abstract List<AdvertiseMaterial> directAdMaterialsByOtherConditions(List<AdvertiseMaterial> adMaterials,
                                                                                KdgProtocol.RequestBean.Impression impression,
                                                                                GSessionInfo session);

    /**
     * 获取创意
     * */
    public static List<AdvertiseMaterial> getAdverAdMaterials(List<AdvertiseMaterial> adMaterials, List<String> ads) {
        List<AdvertiseMaterial> adAssMat = new ArrayList<>();
        for (AdvertiseMaterial ad : adMaterials) {
            if (ads.contains(ad.advertiserId)) {
                adAssMat.add(ad);
            }
        }

        return adAssMat;
    }
    /**
     * 视频创意
     * **/
    public static List<AdvertiseMaterial> getVideoAdMaterials(List<AdvertiseMaterial> adMaterials,
                                                              KdgProtocol.RequestBean.Impression.Video video) {
        List<AdvertiseMaterial> materials = new ArrayList<>();

        int imgH = video.h;
        int imgW = video.w;


        for (AdvertiseMaterial ad : adMaterials) {
            AdvertiseMaterial tmp = new AdvertiseMaterial();
            tmp.campaign = ad.campaign;
            tmp.advertiserId = ad.advertiserId;
            tmp.accountId = ad.accountId;
            tmp.campaignId = ad.campaignId;

            List<Creative> creatives = new ArrayList<>();

            for (Creative creative : ad.creatives) {
                //先看素材类型
                if (creative.type != StatusConstants.CREATIVE_TYPE_VIDEO) {
                    continue;
                }

                if (creative.width == imgW && creative.height == imgH) {

                    if (video.mimes != null && !video.mimes.isEmpty()) {
                        if (video.mimes.contains(creative.fileType)) {
                            creatives.add(creative);
                        }
                    } else {
                        creatives.add(creative);
                    }
                } else {
                    continue;
                }

            }

            if (!creatives.isEmpty()) {
                tmp.creatives = new ArrayList<>();
                tmp.creatives = creatives;
                materials.add(tmp);
            }
        }

        return materials;
    }

    /**
     * IP定向
     */
    public static List<AdvertiseMaterial> directAdMaterialsByIp(List<AdvertiseMaterial> adMaterials,
                                                                GSessionInfo sessionInfo) {
        List<AdvertiseMaterial> materials = new ArrayList<>();
        String area = null;

        String ip = sessionInfo.requestBean.device.ip;

        String ipCityCode = IpUtil.getCityCode(ip);
        String ipProvinceCode = IpUtil.getProvinceCode(ip);
        String ipCountryCode = IpUtil.getCountryCode(ip);

        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByIp error. here's a material is null");
                continue;
            }
            area = ad.campaign.orientAear;
            if (StringUtils.isBlank(area)) {
                materials.add(ad);
                continue;
            }
            JsonObject json = gson.fromJson(area, JsonElement.class).getAsJsonObject();

            JsonObject jsonc = json.get("orient_aear").getAsJsonObject();
            JsonArray arr = jsonc.get("info").getAsJsonArray();

            String ar = null;
            if (arr.size() == 0) {
                materials.add(ad);
                continue;
            } else {
                for (int i = 0; i < arr.size(); i++) {
                    ar = arr.get(i).getAsString();
                    if(ar.length()==4&&ipCountryCode.equals(ar)){
                        materials.add(ad);
                        break;
                    }else if(ar.length()==6&&ipProvinceCode.equals(ar)){
                        materials.add(ad);
                        break;
                    }else{
                        if(ipCityCode.equals(ar)){
                            materials.add(ad);
                            break;
                        }
                    }

                }
            }

        }
        return materials;
    }

    /**
     *过滤掉控量到达的计划
     */

    public static List<AdvertiseMaterial> getAdMaterialsAfterAmountControl(List<AdvertiseMaterial> adMaterials,
                                                                           GSessionInfo session) {
        List<AdvertiseMaterial> materials = new ArrayList<>();

        String deviceId = null;
        if (StringUtils.equalsIgnoreCase(session.requestBean.device.os, AdxConstants.OS_ANDROID)) {
            deviceId = StringUtils.isNotBlank(session.requestBean.device.imeiplain) ? session.requestBean.device.imeiplain :
                    (StringUtils.isNotBlank(session.requestBean.device.imeimd5) ? session.requestBean.device.imeimd5 : session.requestBean.device.imeisha1);
        } else if (StringUtils.equalsIgnoreCase(session.requestBean.device.os, AdxConstants.OS_IOS)) {
            deviceId = session.requestBean.device.ifa;
        }

        long beforeAcRedis = System.currentTimeMillis();

        for (AdvertiseMaterial ad : adMaterials) {

            String banlanceTmp = RedisClusterClient.getString("advertiser:asset:" + ad.advertiserId);
            Double balance = StringUtils.isBlank(banlanceTmp) ? 0.0 : Double.valueOf(banlanceTmp);
            if (balance.compareTo(0.0) > 0) {
                // 控量 针对计划
                if (AmountControl.isCmpArrLimit(ad.campaign, deviceId, balance)) {
                    continue;
                }
                materials.add(ad);
            } else {
                // 关闭广告主下的相关计划
                String m = "广告主余额不足";
                CacheUtils.setCampaignSuspend(ad.advertiserId, 2, m);
            }
        }

        long afterAcRedis = System.currentTimeMillis();
        session.timeLog.ac4RedisTime = afterAcRedis - beforeAcRedis;

        return materials;
    }

    /**
     * 过滤掉设备频次达到上限的计划
     */
    public static List<AdvertiseMaterial> getAdMaterialsByDeviceFrequency(List<AdvertiseMaterial> adMaterials,
                                                                          GSessionInfo session) {

        List<AdvertiseMaterial> materials = new ArrayList<>();
        String deviceId = null;
        if (StringUtils.equalsIgnoreCase(session.requestBean.device.os, AdxConstants.OS_ANDROID)) {
            deviceId = StringUtils.isNotBlank(session.requestBean.device.imeiplain) ? session.requestBean.device.imeiplain :
                    (StringUtils.isNotBlank(session.requestBean.device.imeimd5) ? session.requestBean.device.imeimd5 : session.requestBean.device.imeisha1);
        } else if (StringUtils.equalsIgnoreCase(session.requestBean.device.os, AdxConstants.OS_IOS)) {
            deviceId = session.requestBean.device.ifa;
        }

        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("getAdMaterialsByDeviceFrequency error. here's a material is null");
                continue;
            }

            //去redis查询频次有没有达到上限
            if (!RedisClusterClient.isFilteredByDeviceFrequency(ad.campaignId, deviceId)) {
                materials.add(ad);
            }
        }

        return materials;
    }

    /**
     * 创意优选
     */
    public static AdvertiseMaterial getOptimalAdMaterial(List<AdvertiseMaterial> adMaterials) {

        //先进行广告主优选
        List<AdvertiseMaterial> adMaterialsAfterAdverPriority = new ArrayList<>();
        Map<String, Integer> adverPriority = CacheUtils.getAdverPrioritys();
        int weight = 0;
        if (adverPriority != null && !adverPriority.isEmpty()) {
            for (AdvertiseMaterial ad : adMaterials) {
                if (adverPriority.containsKey(ad.advertiserId)) {
                    if (adverPriority.get(ad.advertiserId) > weight) {
                        adMaterialsAfterAdverPriority.clear();
                        adMaterialsAfterAdverPriority.add(ad);
                        weight = adverPriority.get(ad.advertiserId);
                    } else if (adverPriority.get(ad.advertiserId) == weight) {
                        adMaterialsAfterAdverPriority.add(ad);
                    }
                }
            }
        }

        if (!adMaterialsAfterAdverPriority.isEmpty()) {
            adMaterials = adMaterialsAfterAdverPriority;
        }

            int count = adMaterials.size();
            int num = randomThreadLocal.get().nextInt(count);
            return adMaterials.get(num);
    }
    /**
     * 网络过滤
     * **/
    public static List<AdvertiseMaterial> directAdMaterialsByNetwork(List<AdvertiseMaterial> adMaterials,
                                                                     GSessionInfo session) {
        List<AdvertiseMaterial> materials = new ArrayList<>();
        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByNetwork error. here's a material is null");
                continue;
            }

            if (session.requestBean.device.connectiontype == null) {
                session.requestBean.device.connectiontype = AdxConstants.CONNECTION_UNKNOWN;
            }

            if (ad.campaign.orientNetwork == null || ad.campaign.orientNetwork.network == null
                    || ad.campaign.orientNetwork.network.isEmpty()) {
                materials.add(ad);
            } else {

                switch (session.requestBean.device.connectiontype) {
                    case 1: {
                        if (ad.campaign.orientNetwork.network.contains("wifi")) {
                            materials.add(ad);
                        }
                        break;
                    }
                    default: {
                        if (ad.campaign.orientNetwork.network.contains("other")
                                || ad.campaign.orientNetwork.network.contains("5G")
                                || ad.campaign.orientNetwork.network.contains("4G")
                                || ad.campaign.orientNetwork.network.contains("3G")
                                || ad.campaign.orientNetwork.network.contains("2G")) {
                            materials.add(ad);
                        }

                        break;
                    }
                }
            }
        }

        return materials;
    }

    /**
     * 排期过滤
     * **/
    public static List<AdvertiseMaterial> directAdMaterialsByPeriod(List<AdvertiseMaterial> adMaterials,
                                                                    GSessionInfo session) {

        Date date = new Date();

        List<AdvertiseMaterial> materials = new ArrayList<>();

        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByPeriod error. here's a material is null");
                continue;
            }

            //增加对周期定向无限制的处理
            if ((ad.campaign.startDate == null && ad.campaign.endDate ==null)
                    || (ad.campaign.startDate.before(date) && ad.campaign.endDate.after(date))) {
                materials.add(ad);
            }
        }

        return materials;
    }

    /**
     * 投放时间段定向
     */
    public static List<AdvertiseMaterial> directAdMaterialsByTime(List<AdvertiseMaterial> adMaterials,
                                                                  GSessionInfo sessionInfo) {
        Calendar ca = Calendar.getInstance();
        int day = ca.get(Calendar.DAY_OF_WEEK);
        int hour = ca.get(Calendar.HOUR_OF_DAY);

        String content = null;
        List<AdvertiseMaterial> materials = new ArrayList<>();

        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByTime error. here's a material is null");
                continue;
            }
            content = ad.campaign.orientTime;
            if (StringUtils.isBlank(content)) {
                materials.add(ad);
                continue;
            }
            JsonObject json = gson.fromJson(content, JsonElement.class).getAsJsonObject();

            JsonArray dayArray = null;
            switch (day) {
                case 2: {
                    dayArray = json.get("Mon").getAsJsonArray();
                    break;
                }
                case 3: {
                    dayArray = json.get("Tues").getAsJsonArray();
                    break;
                }
                case 4: {
                    dayArray = json.get("Wed").getAsJsonArray();
                    break;
                }
                case 5: {
                    dayArray = json.get("Thur").getAsJsonArray();
                    break;
                }
                case 6: {
                    dayArray = json.get("Fri").getAsJsonArray();
                    break;
                }
                case 7: {
                    dayArray = json.get("Sat").getAsJsonArray();
                    break;
                }
                case 1: {
                    dayArray = json.get("Sun").getAsJsonArray();
                    break;
                }
            }
            if (dayArray == null) {
                continue;
            }
            if (dayArray.get(hour).getAsInt() == 1) {

                materials.add(ad);
            }

        }
        return materials;
    }

    /**
     * 操作系统定向
     */
    public static List<AdvertiseMaterial> directAdMaterialsByOs(List<AdvertiseMaterial> adMaterials,
                                                                GSessionInfo sessionInfo) {

        List<AdvertiseMaterial> materials = new ArrayList<>();
        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByOs error. here's a material is null");
                continue;
            }

            if (ad.campaign.orientSystem == null || ad.campaign.orientSystem.system == null
                    || ad.campaign.orientSystem.system.isEmpty()) {
                materials.add(ad);
            } else if (StringUtils.isNotBlank(sessionInfo.requestBean.device.os)) {
                for (String system : ad.campaign.orientSystem.system)
                    if (StringUtils.containsIgnoreCase(system, sessionInfo.requestBean.device.os)) {
                        materials.add(ad);
                    }
            } else if (StringUtils.isNotBlank(sessionInfo.requestBean.device.ua)) {
//                String[] oss = ad.campaign.orientSystem.split(",");
                for (String os : ad.campaign.orientSystem.system) {
                    for (String system : ad.campaign.orientSystem.system)
                        if (StringUtils.containsIgnoreCase(system, sessionInfo.requestBean.device.os)) {
                            materials.add(ad);
                            break;
                        }
                }
            }
        }

        return materials;
    }

    /**
     * 媒体定向
     */
    public static List<AdvertiseMaterial> directAdMaterialsByMedia(List<AdvertiseMaterial> adMaterials,
                                                                   GSessionInfo sessionInfo) {

        List<AdvertiseMaterial> materials = new ArrayList<>();
        for (AdvertiseMaterial ad : adMaterials) {
            //渠道定向不包含科达下的媒体 跳过
            if (ad.campaign == null || ad.campaign.orient_plats == null || ad.campaign.orient_plats.isEmpty() || ( !ad.campaign.orient_plats.contains(1))) {
                LOGGER.debug("directAdMaterialsByMedia error. here's a material is null");
                continue;
            }
            String pkgName = null;
            if( sessionInfo.requestBean.app != null) {
                pkgName = sessionInfo.requestBean.app.bundle;
            }

            if (ad.campaign.orientMedia == null || ad.campaign.orientMedia.orient_mediaStr == null
                    || ad.campaign.orientMedia.orient_mediaStr.isEmpty()
                    || ad.campaign.orientMedia.orient_mediaStr.contains(pkgName)
                    || (ad.campaign.orientMedia.orient_mediaStr.size() == 1
                    && ad.campaign.orientMedia.orient_mediaStr.contains(""))) {
                materials.add(ad);
            }
        }

        return materials;
    }

    /**
     * 渠道定向
     */
    public static List<AdvertiseMaterial> directAdMaterialsByChannel(List<AdvertiseMaterial> adMaterials,
                                                                   GSessionInfo sessionInfo) {

        List<AdvertiseMaterial> materials = new ArrayList<>();
        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByChannel error. here's a material is null");
                continue;
            }

            //没有渠道定向或者定向到当前渠道
            if (ad.campaign.orient_plats == null ||
                    (! ad.campaign.orient_plats.isEmpty() && ad.campaign.orient_plats.contains(sessionInfo.downPlatId))) {
                materials.add(ad);
            }
        }

        return materials;
    }

    /**
     * 广告位定向
     */
    public static List<AdvertiseMaterial> directAdMaterialsByAdslot(List<AdvertiseMaterial> adMaterials, KdgProtocol.RequestBean.Impression impression) {


        List<AdvertiseMaterial> materials = new ArrayList<>();
        for (AdvertiseMaterial ad : adMaterials) {
            if (ad.campaign == null) {
                LOGGER.error("directAdMaterialsByAdslot error. here's a material is null");
                continue;
            }
            if (ad.campaign.orientAdslot == null || ad.campaign.orientAdslot.orient_adslotStr == null
                    || ad.campaign.orientAdslot.orient_adslotStr.isEmpty()
                    || ad.campaign.orientAdslot.orient_adslotStr.contains(impression.tagid)
                    || (ad.campaign.orientAdslot.orient_adslotStr.size() == 1
                    && ad.campaign.orientAdslot.orient_adslotStr.contains(""))) {
                materials.add(ad);
            }
        }

        return materials;
    }

    /**
     *根据广告类型获取创意
     * */
    public static List<AdvertiseMaterial> getMaterialsByAdType(List<AdvertiseMaterial> adMaterials,
                                                               KdgProtocol.RequestBean.Impression impression,
                                                               GSessionInfo session) {
        List<AdvertiseMaterial> materials = new ArrayList<>();

        String os = session.requestBean.device.os;
        for (AdvertiseMaterial ad : adMaterials) {
            AdvertiseMaterial tmp = new AdvertiseMaterial();
            tmp.campaign = ad.campaign;
            tmp.advertiserId = ad.advertiserId;
            tmp.accountId = ad.accountId;
            tmp.campaignId = ad.campaignId;

            List<Creative> creatives = new ArrayList<>();

            for (Creative creative : ad.creatives) {
                List<String> opentype = impression.opentype;
                //检查跳转类型
                if (opentype != null && !opentype.isEmpty() && !opentype.contains(String.valueOf(creative.linkType))) {
                    continue;
                }

                //根据系统类型匹配
                if (creative.linkType == LINK_TYPE_H5 && StringUtils.isBlank(creative.landing_page)) {
                    continue;
                } else if (creative.linkType == LINK_TYPE_DOWNLOAD) {
                    if (StringUtils.equalsIgnoreCase(os, AdxConstants.OS_IOS) && StringUtils.isBlank(creative.deepLink)) {
                        continue;
                    } else if (StringUtils.equalsIgnoreCase(os, AdxConstants.OS_ANDROID) && StringUtils.isBlank(creative.landing_page)) {
                        continue;
                    }
                } else if (creative.linkType == LINK_TYPE_DEEPLINK) {
                    if (StringUtils.isBlank(creative.landing_page) && StringUtils.isBlank(creative.deepLink)) {
                        continue;
                    }
                }

                creatives.add(creative);
            }

            if (!creatives.isEmpty()) {
                tmp.creatives = creatives;
                materials.add(tmp);
            }
        }
        return materials;
    }
    /***
     * 查询合适的banner素材
     * **/
    public static List<AdvertiseMaterial> getBannerAdMaterials(List<AdvertiseMaterial> adMaterials,
                                                               KdgProtocol.RequestBean.Impression.Banner banner) {
        List<AdvertiseMaterial> materials = new ArrayList<>();

        int imgH = banner.h;
        int imgW = banner.w;

        if (banner != null && banner.mimes != null && (!banner.mimes.isEmpty()) && banner.mimes.contains("image/jpeg")) {
            banner.mimes.add("image/jpg");
        }

        for (AdvertiseMaterial ad : adMaterials) {
            AdvertiseMaterial tmp = new AdvertiseMaterial();
            tmp.campaign = ad.campaign;
            tmp.advertiserId = ad.advertiserId;
            tmp.accountId = ad.accountId;
            tmp.campaignId = ad.campaignId;

            List<Creative> creatives = new ArrayList<>();

            for (Creative creative : ad.creatives) {
                //先看素材类型
                if (creative.type != StatusConstants.CREATIVE_TYPE_BANNER) {
                    continue;
                }
                if (creative.width == imgW && creative.height == imgH) {

                    if (banner.mimes != null && !banner.mimes.isEmpty()) {
                        if (banner.mimes.contains(creative.fileType)) {
                            creatives.add(creative);
                        }
                    } else {
                        creatives.add(creative);
                    }
                } else {
                    continue;
                }
            }

            if (!creatives.isEmpty()) {
                tmp.creatives = creatives;
                materials.add(tmp);
            }
        }
        return materials;
    }

    public List<AdvertiseMaterial> getFitAdsByDeal(List<AdvertiseMaterial> ads, GSessionInfo session, KdgProtocol.RequestBean.Impression imp) {

        if (imp.pmp == null || imp.pmp.deals == null || imp.pmp.deals.isEmpty()) {
            return getGainsByCampaignAndMaterial(ads, session, imp);
        }

        String dealId = imp.pmp.deals.get(0).id;
        List<PmpConfig> pmpConfs = CacheUtils.getPmpInfos(dealId);

        if (pmpConfs == null || pmpConfs.isEmpty()) {
            return getGainsByCampaignAndMaterial(ads, session, imp);
        }

        List<String> advers4Pmp = new ArrayList<>();
        for (PmpConfig pmpTmp : pmpConfs) {
            if (pmpTmp != null) {
                if (pmpTmp.deal_type == 2) {
                    advers4Pmp.clear();
                    advers4Pmp.addAll(pmpTmp.advers);
                    break;
                } else {
                    advers4Pmp.addAll(pmpTmp.advers);
                }
            }
        }

        List<AdvertiseMaterial> adsTmp = new ArrayList<>();
        for (AdvertiseMaterial ad : ads) {
            if (advers4Pmp.contains(ad.advertiserId)) {
                adsTmp.add(ad);
            }
        }

        if (adsTmp.isEmpty()) {
            return getGainsByCampaignAndMaterial(ads, session, imp);
        } else {
            List<AdvertiseMaterial> retAds = new ArrayList<>();
            for (AdvertiseMaterial ad : adsTmp) {
                if (retAds.isEmpty()) {
                    retAds.add(ad);
                } else {
                    if (Double.compare(ad.campaign.price, retAds.get(0).campaign.price) == 1) {
                        retAds.clear();
                        retAds.add(ad);
                    } else if (Double.compare(ad.campaign.price, retAds.get(0).campaign.price) == 0) {
                        retAds.add(ad);
                    }
                }
            }

            Double price = retAds.get(0).campaign.price * 100;
            //跟底价比较大小
            session.impPriceMap.put(imp.id, price.intValue());
            session.isImpDeal.put(imp.id, true);

            return retAds;
        }
    }

    /*
     * @author xyzhao
     * @desc  : 获取收益最高的广告投放计划，第一步判断优先广告主、第二优先创意出价。
     * @date 2018/5/22 11:43
     * @param [ads]
     * @return java.util.List<com.kdg.gnome.adx.share.dao.AdvertiseMaterial>
     */
    public  List<AdvertiseMaterial> getGainsByCampaignAndMaterial (List<AdvertiseMaterial> ads, GSessionInfo sessionInfo, KdgProtocol.RequestBean.Impression imp) {
        List<AdvertiseMaterial> adMaterialsFinal = new ArrayList<>();
        //以下代码是查找redis中的创意的ctr集合
        Double bidfloor = 0.0;
        if (imp.bidfloor != null) {
            bidfloor = Double.parseDouble(imp.bidfloor.toString());
        }
        Map<String, String> ctrMap = RedisClusterClient.hgetAll("CTR");
        sessionInfo.ctrMap = ctrMap;
        Double peakvalue = null;//中间变量用于记录每个计划中出的最高的price
        if (ads.size()>1) {

            List<AdvertiseMaterial> greaterbidfloor = new ArrayList<>();
            for (AdvertiseMaterial ad:ads) {
                peakvalue= getPeakPrice(ad,ctrMap);
                if (Double.compare(peakvalue, bidfloor)>0) {
                    greaterbidfloor.add(ad);
                }
            }

            Map<Integer,List<AdvertiseMaterial>> adMateMap = getPriority(greaterbidfloor);

            Iterator iterator = adMateMap.keySet().iterator();
            List<AdvertiseMaterial> adMates = new ArrayList<>();

            while(iterator.hasNext()){
                Integer adkey = (Integer)iterator.next();
                adMates = adMateMap.get(adkey);
                break;
            }
            MyComparatorDouble comparatorDouble = new MyComparatorDouble();
            Map<Double,List<AdvertiseMaterial>> adMaterialMapTmp = new TreeMap<>(comparatorDouble);
            List<AdvertiseMaterial> admtmp  = null;
            for (AdvertiseMaterial ad : adMates){
                admtmp  = new ArrayList<>();
                peakvalue= getPeakPrice(ad,ctrMap);
                if ((Double.compare(bidfloor, peakvalue) == 1)){
                    continue;
                }

                if (!adMaterialMapTmp.containsKey(peakvalue)) {
                    admtmp.add(ad);
                    adMaterialMapTmp.put(peakvalue,admtmp);
                } else {
                    adMaterialMapTmp.get(peakvalue).add(ad);
                }

            }
            Iterator iteratorfinal = adMaterialMapTmp.keySet().iterator();

            while(iteratorfinal.hasNext()){
                Double adkey = (Double)iteratorfinal.next();
                int retPrice = adkey.intValue();
                try{
                    sessionInfo.impPriceMap.put(imp.id, retPrice);
                    adMaterialsFinal = adMaterialMapTmp.get(adkey);
                } catch (Exception e) {
                    LOGGER.info("Compare the settlement_type then get data from the map is wrong ,the Execption is {}",e);
                }
                break;
            }
            return adMaterialsFinal;
        } else if (ads.size() == 1) {
            Double priceTmp = getPeakPrice(ads.get(0), ctrMap);
            //增加判断如果计划中的出价价格小于请求中的底价，返回空
            if (priceTmp == null || Double.compare(bidfloor, priceTmp) == 1){
                LOGGER.info("this price is smaller than request's bidfloor,sid{}",sessionInfo.sid);
                return null;
            } else {
                sessionInfo.impPriceMap.put(imp.id, priceTmp.intValue());
            }
            return ads;
        } else {
            return null;
        }
    }

    /*
     * @author xyzhao
     * @desc  : 获取最高级别的广告主优先级
     * @date 2018/5/22 11:42
     * @param [ads]
     * @return java.util.Map<java.lang.Integer,java.util.List<com.kdg.gnome.adx.share.dao.AdvertiseMaterial>>
     */
    private   Map<Integer,List<AdvertiseMaterial>> getPriority (List<AdvertiseMaterial> ads) {
        MyComparator comparator = new MyComparator();
        Map<Integer,List<AdvertiseMaterial>>  priorityMap = new TreeMap<Integer,List<AdvertiseMaterial>>(comparator);
        if(ads != null && ads.size() > 1) {
            List<AdvertiseMaterial> Prioritys  =  null;
            for (AdvertiseMaterial ad : ads) {
                Prioritys =  new ArrayList<>();
                AdvertiserInfo adverInfo =  CacheUtils.getMapAdverInfo(ad.advertiserId);
                if (priorityMap.containsKey(adverInfo.priority)) {
                    priorityMap.get(adverInfo.priority).add(ad);
                } else {
                    Prioritys.add(ad);
                    try{
                        priorityMap.put(adverInfo.priority,Prioritys);
                    }catch (Exception e) {
                        LOGGER.info("getPriority error {}",e);
                    }
                }
            }
        } else if (ads.size() == 1) {
            AdvertiserInfo adverInfo =  CacheUtils.getMapAdverInfo(ads.get(0).advertiserId);
            priorityMap.put(adverInfo.priority,ads);
        }
        return priorityMap;
    }

    /*
     * @author xyzhao
     * @desc  : 获取计划中可以得到的最高的price
     * @date 2018/5/22 11:42
     * @param [ad, creativeCtrs]
     * @return java.lang.Double
     */
    private Double getPeakPrice (AdvertiseMaterial ad,  Map<String, String> ctrMap) {

        Double peakvalue = null;
        if (ad.campaign != null
                && ad.campaign.moneyType != null
                && ad.campaign.price != null ) {
            switch (ad.campaign.moneyType) {
                case "1": {
                    peakvalue = Double.valueOf(ad.campaign.price) * 100;    // CPM 分
                    break;
                }
                case "2" :{
                    Double ctr = null;
                    for (Creative creative:ad.creatives) {
                        if (ctr == null) {
                            if (ctrMap != null &&
                                    ctrMap.containsKey(creative.id) && StringUtils.isNotBlank(ctrMap.get(creative.id ))){
                                String tmp = ctrMap.get(creative.id); //creativeCtr.getDouble(creative.id);
                                ctr = Double.valueOf(tmp);
                                if (Double.compare(ctr, 0.0) == 0) {
                                    ctr = 0.01;
                                }
                            } else {
                                ctr = 0.01;
                            }
                        } else {
                            if (ctrMap != null &&
                                    ctrMap.containsKey(creative.id) && StringUtils.isNotBlank(ctrMap.get(creative.id ))) {
                                String tmp = ctrMap.get(creative.id); //creativeCtr.getDouble(creative.id);
                                Double ctrTmp = Double.valueOf(tmp);
                                if (Double.compare(ctr, ctrTmp)<0) {
                                    ctr = ctrTmp;
                                    if (Double.compare(ctr, 0.0) == 0) {
                                        ctr = 0.01;
                                    }
                                }
                            }else {
                                ctr = 0.01;
                            }
                        }
                    }
                    if (ctr != null) {
                        peakvalue = ad.campaign.price*ctr*1000 * 100;
                    }
                }
            }
            return peakvalue;
        } else {
            LOGGER.error("the campaign is wrong plrease check it ,it's advertiserId is {}",ad.advertiserId);
            return peakvalue;
        }
    }

    public static void main (String[] args) {
//        Date date=new Date();
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
//        calendar.add(Calendar.DAY_OF_MONTH, -1);
//        date = calendar.getTime();
//        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
//        StringBuffer key = new StringBuffer("ctr_");
//        key.append(sdf.format(date).toString());
//        System.out.println(key);
    }


    /*
     * @author xyzhao
     * @desc  : 内部类用于实现treemap的倒排序
     * @date 2018/5/22 11:40
     */
    static class MyComparator implements Comparator{
        @Override
        public int compare(Object o1, Object o2) {
            // TODO Auto-generated method stub
            Integer param1 = (Integer)o1;
            Integer param2 = (Integer)o2;
            return -param1.compareTo(param2);
        }
    }
    /************两个内部类的区别只是类型不同*************/
    /*
     * @author xyzhao
     * @desc  : 内部类用于实现treemap的倒排序
     * @date 2018/5/22 11:40
     */
    static class MyComparatorDouble implements Comparator{
        @Override
        public int compare(Object o1, Object o2) {
            // TODO Auto-generated method stub
            Double param1 = (Double)o1;
            Double param2 = (Double)o2;
            return -param1.compareTo(param2);
        }
    }
}



