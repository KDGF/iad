package com.kdg.gnome.adx.order;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.adx.share.dao.Campaign;
import com.kdg.gnome.share.UtilOper;
import com.kdg.gnome.share.redisutil.RedisClusterClient;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.List;

/**
 * Created by hbwang on 2018/3/6
 */
public class AmountControl {

    private final static String  advertiserBalance = "advertiser_asset";

    private static final String adxConfPath =
            Thread.currentThread().getContextClassLoader().getResource("adx.conf").getPath();
    private static double ratioOfBalance = UtilOper.getDoubleValue(adxConfPath, "ratio_banlance", 999.0);

    private final static String  dayImpSum = "dayImpSum";
    private final static String  dayClkSum = "dayClkSum";
    private final static String  dayBudgetSum = "dayBudgetSum";
    private final static String  cycleBudgetSum = "cycleBudgetSum";

    private final static String  CamPaignArrLimChannel = "adxController_channel";

    private final static String  CampaignArrLimStatusImpOfDay = "2";
    private final static String  CampaignArrLimStatusClkOfDay = "3";
    private final static String  CampaignArrLimStatusBudgetOfDay = "6";
    private final static String  CampaignArrLimStatusBudgetOfCycle = "7";

    private final static int     HincrOfImp = 1;
    private final static int     HincrOfClk = 2;

    public static boolean isCmpArrLimit(Campaign campaign, String devId, Double balance) {
        Calendar ca = Calendar.getInstance();
        int day = ca.get(Calendar.DAY_OF_MONTH);
        int hour = ca.get(Calendar.HOUR_OF_DAY);

        String camId = campaign.id;

        //余额减速投放
        if ((! (Double.compare(balance, ratioOfBalance * 100000.0) == 1)) && (! SlowAdvertising.isAdvertisingByBalance(balance, ratioOfBalance * 100000.0))) {
            return true;
        }

        //频次控制
        {
            int freqNum = campaign.frequency_num;
            String freqKey = null;
            if (campaign.frequency_type == 1) {
                freqKey = camId + ":cycle";
            } else if (campaign.frequency_type == 2) {
                freqKey = camId + ":day:" + day;
            } else if (campaign.frequency_type == 3) {
                freqKey = camId + ":hour:" + hour;
            }

            if (freqNum > 0 && StringUtils.isNotBlank(freqKey)) {
                String value = RedisClusterClient.hget(freqKey, devId);
                int freqSum = StringUtils.isBlank(value) ? 0 : Integer.parseInt(value);
                if (!(freqNum > freqSum)) {
                    return true;
                }

            }
        }

        //预算控制
        {
            double budgetNum = campaign.budget_num;
            String msgStatus = null;
            String budgetKey = null;
            String field = null;
            if (campaign.budget_type == 1) {
                budgetKey = camId + ":cycle";
                msgStatus = CampaignArrLimStatusBudgetOfCycle;
                field = cycleBudgetSum;
            } else if (campaign.budget_type == 2) {
                budgetKey = camId + ":day:" + day;
                msgStatus = CampaignArrLimStatusBudgetOfDay;
                field = dayBudgetSum;
            }
            if (budgetNum > 0 && StringUtils.isNotBlank(budgetKey)) {
                String value = RedisClusterClient.hget(budgetKey, field);
                double budgetSum = StringUtils.isBlank(value) ? 0 : Double.parseDouble(value);
                if (! (Double.compare(budgetNum * 100000.0, budgetSum) == 1)) {

                    CacheUtils.setCampaignSuspend(camId, 1, "预算到量");
                    // 向redis发送消息，告知到量
                    CampaignArrLimMsg msg = new CampaignArrLimMsg();
                    msg.id = camId;
                    msg.status = msgStatus;
                    RedisClusterClient.publish(CamPaignArrLimChannel, msg.toString());
                    return true;
                }

                //匀速投放相关
                if (campaign.launch_type == 2) {
                    if (! UniformSpeedAdvertising.isAdvertising(camId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_BUDGET, campaign.slow_start_uniform, campaign.slow_rate_uniform)) {
                        return true;
                    }

                } else {
                    //减速投放相关
                    if ((Double.compare(budgetSum, 0.0) == 1)
                            && (!SlowAdvertising.isCamAdvertisingByBudget(budgetSum, budgetNum * 100000.0, campaign.slow_start_whole, campaign.slow_rate_whole))) {
                        return true;
                    }
                }
            }
        }
        // 点击、曝光上限
        {
            int impNum = campaign.imp_limit;
            int clkNum = campaign.click_limit;
            String key = camId + ":day:" + day;

            int impSum = -1;
            int clkSum = -1;
            if (impNum > 0 && clkNum > 0) {
                List<String> sum = RedisClusterClient.hmget(key, dayImpSum, dayClkSum);
                if (sum == null || sum.isEmpty() || sum.size() != 2) {
                    return false;
                }
                impSum = StringUtils.isBlank(sum.get(0)) ? 0 : Integer.parseInt(sum.get(0));
                clkSum = StringUtils.isBlank(sum.get(1)) ? 0 : Integer.parseInt(sum.get(1));
            } else if (impNum > 0) {
                String value = RedisClusterClient.hget(key, dayImpSum);
                impSum = StringUtils.isBlank(value) ? 0 : Integer.parseInt(value);
            } else if (clkNum > 0) {
                String value = RedisClusterClient.hget(key, dayClkSum);
                clkSum = StringUtils.isBlank(value) ? 0 : Integer.parseInt(value);
            } else {
                return false;
            }

            if (! (impNum > impSum)) {
                String m = "日曝光到量";
                CacheUtils.setCampaignSuspend(camId, 1, m);
                // 向redis发送消息，告知到量
                CampaignArrLimMsg msg = new CampaignArrLimMsg();
                msg.id = camId;
                msg.status = CampaignArrLimStatusImpOfDay;
                RedisClusterClient.publish(CamPaignArrLimChannel, msg.toString());
                return true;
            }

            if (! (clkNum > clkSum)) {
                String m = "日点击到量";
                CacheUtils.setCampaignSuspend(camId, 1, m);
                // 向redis发送消息，告知到量
                CampaignArrLimMsg msg = new CampaignArrLimMsg();
                msg.id = camId;
                msg.status = CampaignArrLimStatusClkOfDay;
                RedisClusterClient.publish(CamPaignArrLimChannel, msg.toString());
                return true;
            }

            //进行缓速投放的判断
            if (impNum > 0) {
                if (campaign.launch_type == 2) {
                    if (! UniformSpeedAdvertising.isAdvertising(camId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_IMP, campaign.slow_start_uniform, campaign.slow_rate_uniform)) {
                        return true;
                    }
                } else {
                    if (! SlowAdvertising.isCamAdvertisingByLim(impNum, impSum, campaign.slow_start_whole, campaign.slow_rate_whole)) {
                        return true;
                    }
                }
            }
            if (clkNum > 0) {
                if (campaign.launch_type == 2) {
                    double  slowRateUniform = 1;
                    int     slowStartUniform = 1;
                    return (! UniformSpeedAdvertising.isAdvertising(camId, UniformSpeedAdvertising.UNIFORM_SPEED_BY_CLK, slowStartUniform, slowRateUniform) );
                }
            }

        }

        return false;
    }

    //-----------------------------------------------------------------------------------------------
    // 日曝光/点击 累计
    public static Long hincrOfDay(String  camId, int day, int type) {

        String field = null;
        if (type == HincrOfImp) {
            field = dayImpSum;
        } else if (type == HincrOfClk) {
            field = dayClkSum;
        }

        String  key = camId + ":day:" + day;

        long ret = RedisClusterClient.hincrby(key, field);
//        if (RedisClusterClient.hexists(key, field)) {
            RedisClusterClient.expire(key, 24*60*60);
//        }
        return ret;
    }

    // 日预算 累计
    public static void hincrOfDayBudget(String  camId, int day, double cost) {

        String key = camId + ":day:" + day;
        RedisClusterClient.hincrbyFloat(key, dayBudgetSum, cost);
//        if (RedisClusterClient.hexists(key, dayBudgetSum)) {
            RedisClusterClient.expire(key, 24*60*60);
//        }
    }

    // 日设备 频次
    public static void hincrDevOfDayFrequeny(String camId, int day, String devId) {

        String key = camId + ":day:" + day;
        RedisClusterClient.hincrby(key, devId);
//        if (RedisClusterClient.hexists(key, devId)) {
            RedisClusterClient.expire(key, 24*60*60);
//        }
    }

    // 小时设备 频次
    public static void hincrDevOfHourFrequeny(String camId, int hour, String devId) {

        String key = camId + ":hour:" + hour;
        RedisClusterClient.hincrby(key, devId);
//        if (RedisClusterClient.hexists(key, devId)) {
            RedisClusterClient.expire(key, 60*60);
//        }
    }

    // 周期 预算 累计
    public static void hincrOfCycleBudget(String camId, double cost) {

        String key = camId + ":cycle";
        RedisClusterClient.hincrbyFloat(key, cycleBudgetSum, cost);
    }

    // 周期设备 频次
    public static void hincrDevOfCycleFrequeny(String camId, String devId) {

        String key = camId + ":cycle";
        RedisClusterClient.hincrby(key, devId);
    }

    // 计划到量通知结构
    private static Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static class CampaignArrLimMsg {
        public String   type = "c";
        public String   id;
        public String   status;
        @Override
        public String toString() {
            try {
                return GSON.toJson(this);
            } catch (Exception e) {

                return null;
            }
        }
    }
}
