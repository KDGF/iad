package com.kdg.gnome.adx.share;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kdg.gnome.adx.share.dao.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * Created by hbwang.
 */
public class FetchInfoFromDb {

    private static Logger logger = LogManager.getLogger("ES_OUT_INFO");
    private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private Connection conn = null;
    private Statement statement = null;
    private String dbName = null;


    public FetchInfoFromDb() {

    }

    public FetchInfoFromDb setConn(Connection conn) {
        this.conn = conn;
        return this;
    }

    public FetchInfoFromDb setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    public void releaseStatement() {
        if (statement == null) {
            return;
        }
        try {
            if (statement.isClosed()) {
                return;
            }
            statement.close();
        } catch (SQLException e) {
            logger.error("close statement failed: ", e);
        }
    }

    public AdxDbInfo fetchInfo() {
        if (conn == null) {
            logger.error("JDBC connection is null.");
            return null;
        }
        try {
            if (conn.isClosed()) {
                logger.error("conn.isClosed() == true, JDBC connection is closed!");
                return null;
            }

            statement = conn.createStatement();
            if (statement.isClosed()) {
                logger.error("statement.isClosed() == true, JDBC statement is closed!");
                return null;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return null;
        }

        AdxDbInfo info = new AdxDbInfo();

        info.advertiseMaterials = getAdvertiseMaterials();
        info.pmpMap = getPmpConfigMap();
        info.adverPriority = getAdverPrioritys();
        info.advertiserInfoMap = getAdverInfoMap();
        info.creativeMap = getCreatives();

        logger.debug("db loading succeeds!");

        return info;
    }


    /**
     * 获取广告主状态
     */
    private Map<String, AdvertiserInfo> getAdverInfoMap() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }

        Map<String, AdvertiserInfo> advertiserInfoMap = new HashedMap();
        ResultSet rs = null;

        try {
            String sql = "select * from " + dbName + ".advertiser_info";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {
                AdvertiserInfo adInfo = new AdvertiserInfo();
                adInfo.id = rs.getString("id");
                adInfo.com_name = rs.getString("com_name");
                adInfo.status = rs.getInt("status");
                adInfo.flag = rs.getInt("flag");
                adInfo.servicerate = rs.getInt("servicerate");
                adInfo.priority = rs.getInt("priority");
                adInfo.settlement_type = rs.getInt("settlement_type");
                adInfo.firstindustry = rs.getString("firstindustry");
                adInfo.secondindustry = rs.getString("secondindustry");
//                adInfo.apt_status = rs.getInt("apt_status");

                advertiserInfoMap.put(adInfo.id, adInfo);
            }
        } catch ( Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            advertiserInfoMap = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }

        return advertiserInfoMap;
    }

    private Map<String, String>  getMediaPidRelateMap() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }

        Map<String, String>  mediaPidRelateMap = new HashedMap();
        ResultSet rs = null;

        try {
            String sql = "select * from " + dbName + ".media_pid_relate";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {

//                MediaPidRelate  mediaPidRel = new MediaPidRelate();
                int mediaId = rs.getInt("plat_id");
                String adunitId = rs.getString("ad_id");
                String relAdunit   = rs.getString("rel_ad_id");
                int os = rs.getInt("system");   //1：Android，2：iOS

                if (StringUtils.isNotBlank(adunitId) && StringUtils.isNotBlank(relAdunit)
                        && mediaId != 0 && os != 0) {
                    mediaPidRelateMap.put(mediaId + "_" + adunitId + "_" + os, relAdunit);
                }

            }
        } catch ( Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            mediaPidRelateMap = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }

        return mediaPidRelateMap;
    }

    private Map<String, List<PmpConfig>> getPmpConfigMap() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }

        Map<String, List<PmpConfig>>     pmpMap = new HashedMap();
        ResultSet rs = null;
        try {
            String sql = "select * from " + dbName + ".pmp_config";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {
                PmpConfig pmpConfig = new PmpConfig();
                int plat_id = rs.getInt("plat_id");
                int dealType = rs.getInt("deal_type");

                String  dealId = rs.getString("deal_id");
                Double consume = rs.getDouble("consume");

                String adversStr = rs.getString("advertiser_isd");
                if (StringUtils.isNotBlank(adversStr)) {
                    String[] advers = adversStr.split(",");
                    if (advers.length > 0) {
                        pmpConfig.advers = Arrays.asList(advers);
                    }
                }
                int status  = rs.getInt("status");
                int flag    = rs.getInt("flag");


                // 不拉取状态删除的 deal
                if (StringUtils.isNotBlank(dealId) && plat_id != 0 && dealType != 0
                        && status == 0 && flag == 1) {
                    pmpConfig.deal_id = dealId;
                    pmpConfig.deal_type = dealType;
                    pmpConfig.plat_id = plat_id;
                    pmpConfig.consume = consume;


                    if (pmpMap.containsKey(dealId)) {
                        pmpMap.get(dealId).add(pmpConfig);
                    } else {
                        List<PmpConfig> pmpConfigs = new ArrayList<>();
                        pmpConfigs.add(pmpConfig);
                        pmpMap.put(dealId, pmpConfigs);
                    }

                }
            }
        } catch ( Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            pmpMap = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }

        return pmpMap;
    }


    private List<AdvertiseMaterial> getAdvertiseMaterials() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }


        List<AdvertiseMaterial> list = new ArrayList<>();

        List<Campaign> campaigns = getCampaigns();
        Map<String, List<CampaignRelCreative>> relations = getRelations();
        Map<String, Creative>   creativeMap = getCreatives();

        if (campaigns == null || campaigns.isEmpty() || creativeMap == null || creativeMap.isEmpty()) {
            return null;
        }
        for (Campaign campaign : campaigns) {
            if (campaign == null) {
                continue;
            }
            AdvertiseMaterial ad = new AdvertiseMaterial();
            List<Creative> creatives = new ArrayList<>();
            ad.campaign = campaign;
            if (relations == null || relations.isEmpty() || !relations.containsKey(campaign.id)) {
                continue;
            }

            for (CampaignRelCreative relation : relations.get(campaign.id)) {
                Creative creative = creativeMap.get(relation.creativeId);
                if (creative == null || relation.flag == StatusConstants.CREATIVE_FLAG_SUSPEND) {
                    continue;
                }
                ad.accountId = relation.accountId;
                ad.advertiserId = relation.advertiserId;
                ad.campaignId = relation.campaignId;
                creatives.add(creative);
            }
            ad.creatives = creatives;
            list.add(ad);
        }

        return list;
    }

    private Map<String, Creative> getCreatives() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }
        Map<String, Creative> creativeMap = new HashedMap();
        ResultSet rs = null;
        try {
            String sql = "select * from " + dbName + ".creative";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {
                Creative creative = new Creative();
                creative.id = rs.getString("id");
                creative.name = rs.getString("name");

                creative.type = rs.getInt("type");
//                creative.linkType = rs.getInt("link_type");
                creative.style = rs.getInt("style_id");
                creative.width = rs.getInt("width");
                creative.height = rs.getInt("height");
                creative.durition = rs.getInt("duration");

                if (creative.type == 1) {
                    creative.filePath = rs.getString("file_path");
//                    creative.fileType
                    String type = rs.getString("file_type");
                    if (StringUtils.equalsIgnoreCase("png", type)) {
                        creative.fileType = "image/png";
                    } else if (StringUtils.equalsIgnoreCase("jpeg", type)) {
                        creative.fileType = "image/jpeg";
                    }else if (StringUtils.equalsIgnoreCase("jpg", type)) {
                        creative.fileType = "image/jpg";
                    } else if (StringUtils.equalsIgnoreCase("gif", type)) {
                        creative.fileType = "image/gif";
                    }
//                    creative.width = rs.getInt("width");
//                    creative.height = rs.getInt("height");
                }else if (creative.type == 2 ) {
                    creative.filePath = rs.getString("file_path");
                    String type = rs.getString("file_type");
                    if (StringUtils.equalsIgnoreCase("mp4", type)) {
                        creative.fileType = "video/mp4";
                    }else if (StringUtils.equalsIgnoreCase("flv", type)) {
                        creative.fileType = "video/x-flv";
                    }else if (StringUtils.equalsIgnoreCase("wmv", type)) {
                        creative.fileType = "video/x-ms-wmv";
                    }
//                    creative.width = rs.getInt("width");
//                    creative.height = rs.getInt("height");
                }else {
                    try {

                        String template = rs.getString("template_content");
                        Creative.TemplateContent content = gson.fromJson(template, Creative.TemplateContent.class);
//                    if (content.image!= null  && StringUtils.equalsIgnoreCase("jpg", content.image.type) ) {
//                        content.image.type = "jpeg";
//
//                    }
                        creative.templateContent = content;
                    } catch (Exception e) {
                        logger.error("read creative table ERROR. creative.id = {}", creative.id);
                    }
                }
                creative.linkType = rs.getInt("link_type");
                creative.size = rs.getInt("size");
                creative.landing_page = rs.getString("landing_page");
                creative.deepLink = rs.getString("deep_link");
                creative.templateId = rs.getString("template_id");
                creative.category = rs.getString("category");
                creative.status = rs.getInt("status");
                creative.flag = rs.getInt("flag");
                creative.auditStatus = rs.getInt("audit_status");
                creative.remark = rs.getString("remark");
                creative.updateTime = rs.getLong("update_time");

                creative.impMonitor = new ArrayList<>();
                String imp = rs.getString("imp_monitor");
                if (StringUtils.isNotBlank(imp)) {
//                    imp = imp.substring(1, imp.length() - 1);
                    String[] imps = imp.split(",");
                    for (String s : imps) {
                        creative.impMonitor.add(s);
                    }
                }

                String clk = rs.getString("click_monitor");
                creative.clkMonitor = clk;


                creativeMap.put(creative.id, creative);
            }
        } catch ( Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            creativeMap = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }
        return creativeMap;
    }

    private Map<String, List<CampaignRelCreative>> getRelations() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }

        Map<String, List<CampaignRelCreative>> relations = new HashedMap();
        ResultSet rs = null;
        try {
            String sql = "select * from " + dbName + ".campaign_rel_creative";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {
                CampaignRelCreative relation = new CampaignRelCreative();
                relation.id = rs.getString("id");
                relation.accountId = rs.getString("account_id");
                relation.advertiserId = rs.getString("advertiser_id");
                relation.campaignId = rs.getString("campaign_id");
                relation.creativeId = rs.getString("creative_id");

                relation.flag = rs.getInt("flag");
//                relation.impMonitor = new ArrayList<>();
//                String imp = rs.getString("imp_monitor");
//                imp = imp.substring(1, imp.length() - 1);
//                String[] imps = imp.split(",");
//                for (String s : imps) {
//                    relation.impMonitor.add(s.substring(1, s.length() - 1));
//                }
//
//                String clk = rs.getString("click_monitor");
//                relation.clkMonitor = clk;
//
                if (relations.containsKey(relation.campaignId)) {
                    relations.get(relation.campaignId).add(relation);
                } else {
                    List<CampaignRelCreative> relationList = new ArrayList<>();
                    relationList.add(relation);
                    relations.put(relation.campaignId, relationList);
                }
            }
        } catch (Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            relations = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }
        return relations;
    }

    private List<Campaign> getCampaigns() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }

        List<Campaign> campaigns = new ArrayList<>();
        ResultSet   rs = null;
        try {
            String sql = "select * from " + dbName + ".campaign";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {
                Campaign campaign = new Campaign();
                try {

                    campaign.id = rs.getString("id");
                    campaign.accountId  = rs.getString("account_id");
                    campaign.advertiserId = rs.getString("advertiser_id");
                    campaign.name = rs.getString("name");
                    campaign.status = rs.getInt("status");
                    campaign.flag = rs.getInt("flag");
                    campaign.startDate = rs.getDate("start_date");
                    campaign.endDate = rs.getDate("end_date");

                    campaign.launch_type = rs.getInt("launch_type");

                    //控量的相关配置
                    campaign.frequency_type = rs.getInt("frequency_type");
                    campaign.frequency_num = rs.getInt("frequency_num");
                    campaign.budget_type = rs.getInt("budget_type");
                    campaign.budget_num = rs.getDouble("budget_num");
                    campaign.imp_limit = rs.getInt("imp_limit");
                    campaign.click_limit = rs.getInt("click_limit");
                    campaign.moneyType = rs.getString("settlement_type");
                    campaign.price = rs.getDouble("settlement_price");

                    Calendar   calendar   =   new   GregorianCalendar();
                    if (campaign.endDate != null) {
                        calendar.setTime(campaign.endDate);
                        calendar.add(Calendar.DATE, 1);//把日期往后增加一天.正数往后推,负数往前推
                        java.util.Date utilDate = (java.util.Date) calendar.getTime();
                        campaign.endDate = new Date(utilDate.getTime());    //这个时间就是日期往后推一天的结果
                    }
                    campaign.orientTime = rs.getString("orient_time");
                    campaign.orientAear = rs.getString("orient_aear");

                    String  orientPlat = rs.getString("orient_plat");
                    //过滤掉渠道定向为空时写入的 {} 数据
                    if ((! StringUtils.equalsIgnoreCase(orientPlat, "{}")) && StringUtils.isNotBlank(orientPlat)) {
                        String tmp = orientPlat.substring(1, orientPlat.length() - 1);
                        String[] plats = tmp.split(",");
                        campaign.orient_plats = new ArrayList<>();
                        for (String t : plats) {
                            campaign.orient_plats.add(Integer.parseInt(t));
                        }
                    } else {
                        campaign.orient_plats = new ArrayList<>();
                        campaign.orient_plats.add(1);
                    }
                    String  orientSystem = rs.getString("orient_system");
                    Campaign.OrientSystem   os = gson.fromJson(orientSystem, Campaign.OrientSystem.class);
                    campaign.orientSystem = os;

                    String  orientNet = rs.getString("orient_network");
                    Campaign.OrientNetwork  net = gson.fromJson(orientNet, Campaign.OrientNetwork.class);
                    campaign.orientNetwork = net;

                    String  orientMedia = rs.getString("orient_media");
                    Campaign.OrientMedia media = gson.fromJson(orientMedia, Campaign.OrientMedia.class);
                    campaign.orientMedia = media;

                    String  orientAd = rs.getString("orient_adslot");
                    Campaign.OrientAdslot ad = gson.fromJson(orientAd, Campaign.OrientAdslot.class);
                    campaign.orientAdslot = ad;

                    campaign.slow_rate_whole = rs.getDouble("slow_rate_whole");
                    campaign.slow_start_whole = rs.getInt("slow_start_whole");
                    campaign.slow_rate_uniform = rs.getDouble("slow_rate_uniform");
                    campaign.slow_start_uniform = rs.getInt("slow_start_uniform");

                    if (campaign != null) {
                        campaigns.add(campaign);
                    }
                } catch (Exception e) {
                    logger.error("Load Campaigns Error. camaign.id = {}, statement.next triggered an exception: ", campaign.id, e);
                }
            }
        } catch (Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            campaigns = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }
        return campaigns;
    }

    /**
     * 获取广告主优先级
     */
    private Map<String, Integer> getAdverPrioritys() {
        if (statement == null) {
            logger.error("statement is null.");
            return null;
        }

        Map<String, Integer> adverPrioritys = new HashedMap();
        ResultSet   rs = null;
        try {
            String sql = "select * from " + dbName + ".adver_priority";
            rs = statement.executeQuery(sql);
            while (!Thread.currentThread().isInterrupted() && rs.next()) {
               String adverID = rs.getString("adver_id");
               int weight = rs.getInt("weight");

               if (StringUtils.isNotBlank(adverID) && weight != 0) {
                   adverPrioritys.put(adverID, weight);
               }
            }
        } catch (Exception e) {
            logger.error("statement.executeQuery(sql) triggered an exception: ", e);
            adverPrioritys = null;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("close ResultSet failed: ", e);
                }
            }
        }
        return adverPrioritys;
    }

    public static void main(String[] args) {
        FetchInfoFromDb fetchInfo = new FetchInfoFromDb();
        fetchInfo.fetchInfo();
        fetchInfo.releaseStatement();
    }
}
