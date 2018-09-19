package com.kdg.gnome.adx.utils;

import com.kdg.gnome.adx.share.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 门面工具类，提供缓存的get方法。
 * <p>
 *
 * @author qlzhang
 */
public class CacheUtils {
    private volatile static AdxDbInfo db = null;// 在adx的MediaInfoPullingTask中进行设置
    private static Logger log = LogManager.getLogger("ES_OUT_INFO");

    private CacheUtils() {
    }

    public static void setDb(AdxDbInfo newDb) {

        if (newDb == null) {
            log.error("setDb(AdxDbInfo newDb): newDb == null");
            return;
        }
        db = newDb;
    }

    /**
     * 获取 投放状态的 订单和创意
     */
    public static List<AdvertiseMaterial> getActiveMaterials() {
        if (db == null || db.advertiseMaterials == null || db.advertiseMaterials.isEmpty()) {
            return null;
        }
        List<AdvertiseMaterial> advertiseMaterials = new ArrayList<>();

        for (AdvertiseMaterial ad : db.advertiseMaterials) {

            if (ad == null || ad.campaign == null || ad.creatives == null || ad.creatives.isEmpty()) {
                continue;
            }
            //过滤掉广告主状态关闭的计划
            if (db.advertiserInfoMap != null && !db.advertiserInfoMap.isEmpty() && StringUtils.isNotBlank(ad.advertiserId)) {
                if (!db.advertiserInfoMap.containsKey(ad.advertiserId)
                        || db.advertiserInfoMap.get(ad.advertiserId).status == StatusConstants.ADVER_STATUS_DELETE
                        || db.advertiserInfoMap.get(ad.advertiserId).flag == StatusConstants.ADVER_FLAG_SUSPEND ) {
                    continue;
                }
            }
            //检查计划创意状态
            if (ad.campaign.status == StatusConstants.CAMPAIGN_STATUS_NORMAL
                    && ad.campaign.flag == StatusConstants.CAMPAIGN_FLAG_START) {

                AdvertiseMaterial adtmp = new AdvertiseMaterial();
                List<Creative> creatives = new ArrayList<>();
                for (Creative creative : ad.creatives) {
                    if (creative.status == StatusConstants.CREATIVE_STATUS_NOMAL
                            && creative.flag == StatusConstants.CREATIVE_FLAG_START) {
                        creatives.add(creative);
                    }
                }
                if (ad.campaign != null && !creatives.isEmpty()) {
                    adtmp.campaign = ad.campaign;
                    adtmp.creatives = creatives;
                    adtmp.advertiserId = ad.advertiserId;
                    adtmp.campaignId = ad.campaignId;
                    adtmp.accountId = ad.accountId;
                    advertiseMaterials.add(adtmp);
                }
            }

        }

        return advertiseMaterials;
    }


    /**
     *  到量控量  由kafka通知
     */
    public static void setCampaignSuspend(String camId, int type, String msg) {
        if (db == null || db.advertiseMaterials == null || db.advertiseMaterials.isEmpty()) {
            return;
        }
        log.info("camId = {}, type = {}, m = {}", camId, type, msg);

        List<AdvertiseMaterial> activeADs = CacheUtils.getActiveMaterials();
        if (activeADs == null || activeADs.isEmpty()) {
            log.info("activeADs is NULL.");
            return;
        }
        for (AdvertiseMaterial ad : activeADs) {
            if (ad == null || ad.campaign == null || ad.creatives == null || ad.creatives.isEmpty()) {
                return;
            }
            if (type == 1 && StringUtils.equalsIgnoreCase(camId, String.valueOf(ad.campaign.id))) {
                ad.campaign.flag = StatusConstants.CAMPAIGN_FLAG_SUSPEND;
                break;
            } else if (type == 2 && StringUtils.equalsIgnoreCase(ad.advertiserId, camId)) {
                log.info("=================即将关闭广告主 {} , 计划 {}====================", ad.advertiserId, ad.campaign.id);
                ad.campaign.flag = StatusConstants.CAMPAIGN_FLAG_SUSPEND;
                if (db.advertiserInfoMap.containsKey(camId)) {
                    db.advertiserInfoMap.get(camId).flag = StatusConstants.ADVER_FLAG_SUSPEND;
                }
                break;
            }

        }
        log.info("-----------------修改计划状态成功...............");
    }

    /**
     *  获取pmpConfig
     */
    public static List<PmpConfig> getPmpInfos(String dealId) {
        if (db == null || db.pmpMap == null || db.pmpMap.isEmpty()) {
            return null;
        }

        return db.pmpMap.get(dealId);
    }

    /**
     * 获取广告主优先级 创意优先相关
     */
    public static Map<String, Integer> getAdverPrioritys() {
        if (db == null || db.adverPriority == null || db.adverPriority.isEmpty()) {
            return null;
        } else {
            return db.adverPriority;
        }
    }

    /**
     * 获取广告主的结算方式
     * @param adverId
     * @return
     */
    public static int getAdverSettleType(String adverId) {
        if (db == null || db.advertiserInfoMap == null || db.advertiserInfoMap.isEmpty()) {
            return 1;
        } else {
            if (db.advertiserInfoMap.containsKey(adverId)) {
                return db.advertiserInfoMap.get(adverId).settlement_type;
            } else {
                return 1;
            }
        }
    }

    //获取广告主信息
    public static AdvertiserInfo  getMapAdverInfo(String advertiserId){
        if (db == null || db.advertiserInfoMap == null || db.advertiserInfoMap.isEmpty()) {
            return null;
        } else {
            AdvertiserInfo advertiserInfo=db.advertiserInfoMap.get(advertiserId);
            if(advertiserInfo==null){
                return null;
            }
            return  advertiserInfo;
        }

    }

    //获取广告主的服务费率
    public static int  getAdverServRate(String advertiserId){
        if (db == null || db.advertiserInfoMap == null || db.advertiserInfoMap.isEmpty()) {
            return 0;
        } else {
            AdvertiserInfo advertiserInfo = db.advertiserInfoMap.get(advertiserId);
            if(advertiserInfo==null){
                return 0;
            }
            return  advertiserInfo.servicerate;
        }

    }

    //获取广告主的名称
    public static String  getAdverName(String advertiserId){
        if (db == null || db.advertiserInfoMap == null || db.advertiserInfoMap.isEmpty()) {
            return null;
        } else {
            AdvertiserInfo advertiserInfo = db.advertiserInfoMap.get(advertiserId);
            if(advertiserInfo==null){
                return null;
            }
            return  advertiserInfo.com_name;
        }

    }

    //根据计划id获取计划
    public static Campaign getCampaign(String camId) {
        if (db == null || db.advertiseMaterials == null || db.advertiseMaterials.isEmpty()) {
            return null;
        } else {
            for (AdvertiseMaterial ad : db.advertiseMaterials) {
                if (StringUtils.equalsIgnoreCase(ad.campaignId, camId)) {
                    return ad.campaign;
                }
            }
            return null;
        }
    }

    //根据计划id获取计划名称
    public static String getCampName(String camId) {
        if (db == null || db.advertiseMaterials == null || db.advertiseMaterials.isEmpty()) {
            return null;
        } else {
            for (AdvertiseMaterial ad : db.advertiseMaterials) {
                if (StringUtils.equalsIgnoreCase(ad.campaignId, camId)) {
                    return ad.campaign.name;
                }
            }
            return null;
        }
    }

    //根据素材id获取素材的名称
    public static String getCrvName(String crvId) {
        if (db == null || db.creativeMap == null || db.creativeMap.isEmpty()) {
            return null;
        } else {
            if (db.creativeMap.containsKey(crvId)) {
                return db.creativeMap.get(crvId).name;
            } else {
                return null;
            }
        }
    }

    //根据创意id获取创意style
    public static int getCreativeStyle(String crvId) {
        if (db == null || db.creativeMap == null || db.creativeMap.isEmpty()) {
            return 0;
        } else {
            Creative creative = db.creativeMap.get(crvId);
            return creative.style;
        }
    }


}
