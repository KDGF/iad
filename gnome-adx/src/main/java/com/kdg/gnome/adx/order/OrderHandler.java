package com.kdg.gnome.adx.order;

import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.adx.share.dao.AdvertiseMaterial;
import com.kdg.gnome.adx.share.dao.Creative;
import com.kdg.gnome.adx.share.dao.StatusConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hbwang on 2017/11/30
 */
public class OrderHandler extends BaseOrderHandler {

    private static final Logger LOGGER = LogManager.getLogger("ES_OUT_INFO");
    /**
     * 根据定向条件 获取创意
     */
    @Override
    public   List<AdvertiseMaterial> loadOrders(GSessionInfo sessionInfo) {
        if (sessionInfo == null) {
            LOGGER.info("session is null");
            return null;
        }

        //
        List<AdvertiseMaterial> adMaterials = CacheUtils.getActiveMaterials();
        return getAdMaterialsByDirect(adMaterials, sessionInfo);
    }

/****/
    public List<AdvertiseMaterial> getAdMaterialsByDirect(List<AdvertiseMaterial> adMaterials,
                                                                        GSessionInfo sessionInfo) {


        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("目前没有在投状态的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }

        /* 2.周期 */
        adMaterials = directAdMaterialsByPeriod(adMaterials, sessionInfo);

        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("经投放周期定向条件后，目前没有正在投放的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }
        /* 2.时段 */
        adMaterials = directAdMaterialsByTime(adMaterials, sessionInfo);
        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("经投放时间段定向条件后，目前没有正在投放的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }
        /* 3.系统 ios/android */
        adMaterials = directAdMaterialsByOs(adMaterials, sessionInfo);
        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("经操作系统定向条件后，目前没有正在投放的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }
        //当流量类型是app时才进行媒体定向 且 是媒体平台 (不是媒体平台进行渠道定向)
        if (sessionInfo.mediaType == 0) {
            /* 4.媒体定向 */
            adMaterials = directAdMaterialsByMedia(adMaterials, sessionInfo);

            if (adMaterials == null || adMaterials.isEmpty()) {
                LOGGER.info("经媒体定向条件后，没有正在投放的Campaign, sid = {}", sessionInfo.sid);
                return null;
            }
        } else {
            adMaterials = directAdMaterialsByChannel(adMaterials, sessionInfo);
            if (adMaterials == null || adMaterials.isEmpty()) {
                LOGGER.info("经渠道定向条件后，没有正在投放的Campaign, sid = {}", sessionInfo.sid);
                return null;
            }
        }

        /* 6.网络定向 */
        adMaterials = directAdMaterialsByNetwork(adMaterials, sessionInfo);
        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("经网络定向条件后，目前没有正在投放的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }

        /* IP定向 */
        adMaterials = directAdMaterialsByIp(adMaterials, sessionInfo);
        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("经IP定向条件后，目前没有正在投放的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }

        /* 过滤掉控量逻辑到达的计划 */
        adMaterials = getAdMaterialsAfterAmountControl(adMaterials, sessionInfo);
        if (adMaterials == null || adMaterials.isEmpty()) {
            LOGGER.info("经计划控量和广告主余额控量条件后，目前没有正在投放的Campaign, sid = {}", sessionInfo.sid);
            return null;
        }

        return adMaterials;
    }

    //合并相同计划下的素材
    private static void mergeAds(List<AdvertiseMaterial> tmp, Map<String, AdvertiseMaterial> mapTmp) {
        if (tmp != null) {
            for (AdvertiseMaterial ad : tmp) {
                if (mapTmp.containsKey(ad.campaignId)) {
                    mapTmp.get(ad.campaignId).creatives.addAll(ad.creatives);
                } else {
                    mapTmp.put(ad.campaignId, ad);
                }
            }
        }
    }

    // 选择尺寸类型匹配的广告计划素材  包含广告位定向
    public List<AdvertiseMaterial> directAdMaterialsByOtherConditions(List<AdvertiseMaterial> adMaterials,
                                                                                          KdgProtocol.RequestBean.Impression impression,
                                                                                          GSessionInfo session) {

//        Map<String, List<AdvertiseMaterial>> map = new HashedMap();

        List<AdvertiseMaterial> materials;
        List<AdvertiseMaterial> materialsTmp = new ArrayList<>();
        Map<String, AdvertiseMaterial> materialsTmpBanner = new HashMap<>();
        Map<String, AdvertiseMaterial> materialsTmpVideo = new HashMap<>();
        Map<String, AdvertiseMaterial> materialsTmpNative = new HashMap<>();

        //广告位定向
        materials = directAdMaterialsByAdslot(adMaterials, impression);
        if (materials == null || materials.isEmpty()) {
            LOGGER.info("该广告位经广告为定向后无符合的计划. imp.tagid = {}, sid = {}", impression.tagid, session.sid);
            return null;
        }

        //检查跳转类型 区分安卓 ios
        materials = getMaterialsByAdType(materials, impression, session);
        if (materials == null || materials.isEmpty()) {
            LOGGER.info("经该广告位支持的跳转(下载)类型检查后无符合的素材. imp.tagid = {}, sid = {}", impression.tagid, session.sid);
            return null;
        }

        //检查符合的banner素材
        if (impression.banner != null && (!impression.banner.isEmpty())) {
            for (KdgProtocol.RequestBean.Impression.Banner banner : impression.banner) {

                List<AdvertiseMaterial> tmp = getMaterialsByAdunitStyle(materials, 1);

                List<AdvertiseMaterial> tmp2 = getBannerAdMaterials(tmp, banner);
                mergeAds(tmp2, materialsTmpBanner);
            }
            if (materialsTmpBanner == null || materialsTmpBanner.isEmpty()) {
                LOGGER.info("选出的banner素材为空, sid = {}, adunit = {}", session.sid, impression.adunit);
            }
        }

        //检查符合的native素材
        if (impression.native$ != null && (!impression.native$.isEmpty())) {
            for (KdgProtocol.RequestBean.Impression.Native na : impression.native$) {

                int style = makeUpNativeStyles(na);
                if (style == 0) {
                    LOGGER.info("该广告位没有指定支持的素材类型(style).  adunit = {}", impression.adunit);
                    return null;
                }

                List<AdvertiseMaterial> tmp = getMaterialsByAdunitStyle(materials, style);

                if (tmp == null || tmp.isEmpty()) {
                    LOGGER.info("根据style 获取的素材为空");
                }
                List<AdvertiseMaterial> tmp2 = null;
                try{
                    tmp2 = getNativeAdMaterials(tmp, na);
                }catch (Exception e) {
                    LOGGER.info(e.getMessage());
                }
                mergeAds(tmp2, materialsTmpNative);
            }
            if (materialsTmpNative == null || materialsTmpNative.isEmpty()) {
                LOGGER.info("选出的native素材为空, sid = {}, adunit = {}", session.sid, impression.adunit);
            }
        }
        //检查符合的video创意
        if (impression.video != null && (!impression.video.isEmpty())) {
            for (KdgProtocol.RequestBean.Impression.Video video : impression.video) {

                if (video == null) {
                    continue;
                }
                List<AdvertiseMaterial> tmp = getMaterialsByAdunitStyle(materials, 10);


                List<AdvertiseMaterial> tmp2 = getVideoAdMaterials(tmp,video);
                mergeAds(tmp2, materialsTmpVideo);
            }
        }

        if ((! materialsTmpBanner.isEmpty())) {
            materialsTmp.addAll(materialsTmpBanner.values());
        }
        if ((! materialsTmpNative.isEmpty())) {
            materialsTmp.addAll(materialsTmpNative.values());
        }
        if ((! materialsTmpVideo.isEmpty())) {
            materialsTmp.addAll(materialsTmpVideo.values());
        }

        if (! materialsTmp.isEmpty()) {
            return materialsTmp;
        } else {
            return null;
        }
    }

    private static int makeUpNativeStyles(KdgProtocol.RequestBean.Impression.Native na) {
        int style = 0;

        if (na == null) {
            return 0;
        }
        if (na.image != null && na.image.size() == 3) {
            style = 6;
            if (na.icon != null && na.icon.h != 0 && na.icon.w != 0) {
                style = 7;
            }
        } else if (na.image != null && na.image.size() == 1) {
            style  = 4;
            if (na.icon != null && na.icon.w != 0 && na.icon.h != 0) {
                style = 5;
                if (na.video != null && na.video.h != 0 && na.video.w != 0) {
                    style = 9;
                }
            } else if (na.video != null && na.video.h != 0 && na.video.w != 0) {
                style = 8;
            }
        }

        if (na.video != null) {
            style = 8;
            if (na.icon != null && na.icon.h != 0 && na.icon.w != 0) {
                style = 9;
            }

        }

        return style;

    }

    /**
     * 原生创意
     * **/
    public static List<AdvertiseMaterial> getNativeAdMaterials(List<AdvertiseMaterial> adMaterials,
                                                               KdgProtocol.RequestBean.Impression.Native na) {
        List<AdvertiseMaterial> materials = new ArrayList<>();

//        int style = na.style;

        KdgProtocol.RequestBean.Impression.Native.Image image = null;
        if (na.image != null && !na.image.isEmpty()) {
            image = na.image.get(0);
        }
        if (image != null && image.mimes != null && (! image.mimes.isEmpty()) && image.mimes.contains("image/jpeg")) {
            image.mimes.add("image/jpg");
        }

        for (AdvertiseMaterial ad : adMaterials) {
            AdvertiseMaterial tmp = new AdvertiseMaterial();
            tmp.campaign = ad.campaign;
            tmp.advertiserId = ad.advertiserId;
            tmp.accountId = ad.accountId;
            tmp.campaignId = ad.campaignId;

            List<Creative> creatives = new ArrayList<>();
            for (Creative creative : ad.creatives) {
                //检查素材类型
                if (creative.type != StatusConstants.CREATIVE_TYPE_NATIVE) {
                    continue;
                }

                if (creative.templateContent != null) {
                    //此处为素材的style,检查该类型下素材的属性是否齐全 是否与请求的尺寸匹配
                    switch (creative.style) {
                        case 4: {
                            if (creative.templateContent.image != null
                                    && StringUtils.isNotBlank(creative.templateContent.title)
                                    && StringUtils.isNotBlank(creative.templateContent.desc)
                                    && creative.templateContent.image.h == image.h
                                    && creative.templateContent.image.w == image.w
                                    && creative.templateContent.image.context != null
                                    && creative.templateContent.image.context.img != null
                                    && (image.mimes == null || image.mimes.isEmpty()
                                    || image.mimes.contains(creative.templateContent.image.type))) {

                                creatives.add(creative);
                            }
                            break;
                        }
                        case 5: {
                            if (creative.templateContent.image != null
                                    && StringUtils.isNotBlank(creative.templateContent.title)
                                    && StringUtils.isNotBlank(creative.templateContent.desc)
                                    && creative.templateContent.image.h == image.h
                                    && creative.templateContent.image.w == image.w
                                    && creative.templateContent.image.context != null
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img)
                                    && (image.mimes == null || image.mimes.isEmpty()
                                    || image.mimes.contains(creative.templateContent.image.type))
                                    && StringUtils.isNotBlank(creative.templateContent.icon)) {
                                creatives.add(creative);
                            }
                            break;
                        }
                        case 6: {
                            if (creative.templateContent.image != null
                                    && StringUtils.isNotBlank(creative.templateContent.title)
                                    && StringUtils.isNotBlank(creative.templateContent.desc)
                                    && creative.templateContent.image.h == image.h
                                    && creative.templateContent.image.w == image.w
                                    && creative.templateContent.image.context != null
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img1)
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img2)
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img3)
                                    && (image.mimes == null || image.mimes.isEmpty()
                                    || image.mimes.contains(creative.templateContent.image.type))) {

                                creatives.add(creative);
                            }
                            break;
                        }
                        case 7: {
                            if (creative.templateContent.image != null
                                    && StringUtils.isNotBlank(creative.templateContent.title)
                                    && StringUtils.isNotBlank(creative.templateContent.desc)
                                    && creative.templateContent.image.h == image.h
                                    && creative.templateContent.image.w == image.w
                                    && creative.templateContent.image.context != null
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img1)
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img2)
                                    && StringUtils.isNotBlank(creative.templateContent.image.context.img3)
                                    && (image.mimes == null || image.mimes.isEmpty()
                                    || image.mimes.contains(creative.templateContent.image.type))
                                    && StringUtils.isNotBlank(creative.templateContent.icon)) {
                                creatives.add(creative);
                            }
                            break;
                        }
                        case 8: {
                            try{

                                if (creative.templateContent.image != null
                                        && StringUtils.isNotBlank(creative.templateContent.title)
                                        && StringUtils.isNotBlank(creative.templateContent.desc)
                                        && creative.templateContent.video != null
                                        && (na.video.mimes == null || na.video.mimes.isEmpty()
                                        || na.video.mimes.contains(creative.templateContent.video.type))
                                        && StringUtils.isNotBlank(creative.templateContent.video.url)) {

                                    if ((image !=null &&  creative.templateContent.image.h == image.h && creative.templateContent.image.w == image.w
                                            && creative.templateContent.image.context != null && StringUtils.isNotBlank(creative.templateContent.image.context.img))
                                            || (image == null && creative.templateContent.video.h == na.video.h
                                            && creative.templateContent.video.w == na.video.w)) {
                                        creatives.add(creative);
                                    }
                                }
                            }catch (Exception e){
                                LOGGER.info(e.getMessage());
                            }
                            break;
                        }
                        case 9: {
                            if (creative.templateContent.image != null
                                    && StringUtils.isNotBlank(creative.templateContent.title)
                                    && StringUtils.isNotBlank(creative.templateContent.desc)
                                    && StringUtils.isNotBlank(creative.templateContent.icon)
                                    && creative.templateContent.video != null
                                    && (na.video.mimes == null || na.video.mimes.isEmpty()
                                    || na.video.mimes.contains(creative.templateContent.video.type))
                                    && StringUtils.isNotBlank(creative.templateContent.video.url)) {

                                if ((image !=null &&  creative.templateContent.image.h == image.h && creative.templateContent.image.w == image.w
                                        && creative.templateContent.image.context != null && StringUtils.isNotBlank(creative.templateContent.image.context.img))
                                        || (image == null && creative.templateContent.video.h == na.video.h
                                        && creative.templateContent.video.w == na.video.w)) {
                                    creatives.add(creative);
                                }
                            }
                            break;
                        }
                        default: {
                            LOGGER.info("error. 此广告位为原生广告位. 素材style = {}", creative.style);
                            break;
                        }
                    }
                }
            }

            if (!creatives.isEmpty()) {
                tmp.creatives = creatives;
                materials.add(tmp);
            }
        }
        return materials;

    }


    /**
     *   检查素材的style跟广告位支持的style是否匹配
     */
    protected static List<AdvertiseMaterial> getMaterialsByAdunitStyle(List<AdvertiseMaterial> adMaterials,
                                                                       int stylesTmp) {
        List<AdvertiseMaterial> materials = new ArrayList<>();

        for (AdvertiseMaterial ad : adMaterials) {
            AdvertiseMaterial tmp = new AdvertiseMaterial();
            tmp.campaign = ad.campaign;
            tmp.advertiserId = ad.advertiserId;
            tmp.accountId = ad.accountId;
            tmp.campaignId = ad.campaignId;

            List<Creative> creatives = new ArrayList<>();

            for (Creative creative : ad.creatives) {
                //检查素材是否匹配广告支持的类型
                if (stylesTmp == creative.style) {
                    creatives.add(creative);
                }
            }
            if (!creatives.isEmpty()) {
                tmp.creatives = creatives;
                materials.add(tmp);
            }
        }

        return materials;
    }
}
