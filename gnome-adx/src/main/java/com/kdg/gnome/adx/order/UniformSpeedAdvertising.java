package com.kdg.gnome.adx.order;

import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.adx.share.dao.Campaign;
import com.kdg.gnome.share.redisutil.RedisClusterClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hbwang on 2018/8/3
 */
public class UniformSpeedAdvertising {

    private static final Logger LOGGER = LogManager.getLogger("ES_OUT_INFO");

    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat keyFormatter = new SimpleDateFormat("yyyyMMdd");

    private static Integer CURRENT_TIMESLICE = (getTimePointNow(new Date()) - 1);
    private static Double BUDGET_LIM_PER_SLICE;
    private static Integer  IMP_LIM_PER_SLICE;
    private static Integer  CLK_SUM_PER_SLICE;

    private static boolean  UPDATE_LIM_BUDGET = false;
    private static boolean  UPDATE_LIM_IMP = false;

    public static final int UNIFORM_SPEED_BY_BUDGET = 1;
    public static final int UNIFORM_SPEED_BY_IMP = 2;
    public static final int UNIFORM_SPEED_BY_CLK = 3;


    public static int getTimePointNow(Date date){
        String hourstr = getHour(date);
        String minutestr = getTime(date);

        int hour = Integer.parseInt(hourstr);
        int minute = Integer.parseInt(minutestr);
        int tmp = minute/15;
        if ((minute%15)>=0) {
            ++tmp;
        }
        int point = 0;
        if (hour>21) {
            point = 85;
        }else {
            if (tmp == 0) {
                point = hour*4+1;
            } else {
                point = hour*4+tmp;
            }

        }
        return point;
    }

    private  static String getHour(Date currentTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        String hour;
        hour = dateString.substring(11, 13);
        return hour;
    }

    /**
     * @author xyzhao
     * @desc  : 获取当前时间的分钟数
     * @date 2018/7/19 18:18
     * @return
     */
    private  static String getTime(Date currentTime) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        String min ;
        min = dateString.substring(14, 16);
        return min;
    }

    //获取当前时间片和field
    private static int getTimePointNowOld(Date date) {
        //获取当前的日期的字符串形式
        //截取当前时间的分钟
        String dateString = formatter.format(date);
        String min ;
        min = dateString.substring(14, 16);
        String minutestr = min;
        //截取当前时间的小时
        dateString = formatter.format(date);
        String hourstr = dateString.substring(11, 13);
        int hour = Integer.parseInt(hourstr);
        int minute = Integer.parseInt(minutestr);
        int tmp = minute / 15;
        if ((minute % 15) > 0) {
            ++tmp;
        }
        int point = 0;
        if (hour > 20) {
            point = 85;
        } else {
            point = hour*4+tmp;
        }

        return point;
    }

    //type 1.预算  2.曝光上限  3.点击上限
    static boolean isAdvertising(String camId, int type,  int ratio4Start, double ratioOfSpeed) {

        Date date = new Date();
        String key = null;
        if (type == UNIFORM_SPEED_BY_BUDGET) {
            key = "timepoint:" + keyFormatter.format(date);
        } else if (type == UNIFORM_SPEED_BY_IMP) {
            key = "campaignImp:" + keyFormatter.format(date);
        } else if (type == UNIFORM_SPEED_BY_CLK) {
            key = "campaignClk:" + keyFormatter.format(date);
        }

        int timeSlice = getTimePointNow(date);
        key = key + ":" + camId + ":" + timeSlice;
        String value = RedisClusterClient.getString(key);

        if (StringUtils.isBlank(value) || StringUtils.equalsIgnoreCase(value, "0")) {
            return false;
        }

        if (type == UNIFORM_SPEED_BY_BUDGET) {

            double bugget = Double.valueOf(value);

            //换切片时更新当前片的总量
            if (timeSlice != CURRENT_TIMESLICE) {
                BUDGET_LIM_PER_SLICE = bugget;
                CURRENT_TIMESLICE = timeSlice;
                UPDATE_LIM_BUDGET = true;
            }

            //减速投放
            LOGGER.info("timeSlice = {}, budgetLim = {}, budgetSum = {}", timeSlice, BUDGET_LIM_PER_SLICE, BUDGET_LIM_PER_SLICE - bugget);
            return (SlowAdvertising.isCamAdvertisingByBudget(BUDGET_LIM_PER_SLICE - bugget, BUDGET_LIM_PER_SLICE, ratio4Start, ratioOfSpeed)
                    && (Double.compare(bugget, 0.0) == 1));

        } else if (type == UNIFORM_SPEED_BY_IMP) {
            int impNum = Integer.parseInt(value);

            //换切片时更新当前片的总量
            if((UPDATE_LIM_BUDGET) || timeSlice != CURRENT_TIMESLICE) {
                IMP_LIM_PER_SLICE = impNum;
                CURRENT_TIMESLICE = timeSlice;
                UPDATE_LIM_IMP = true;
                UPDATE_LIM_BUDGET = false;
            }

            //减速投放
            LOGGER.info("timeSlice = {}, impLim = {}, impSum = {}", timeSlice, IMP_LIM_PER_SLICE, IMP_LIM_PER_SLICE - impNum);
            return ( SlowAdvertising.isCamAdvertisingByLim(IMP_LIM_PER_SLICE, IMP_LIM_PER_SLICE - impNum, ratio4Start, ratioOfSpeed)
                    && (impNum > 0));

        } else if (type == UNIFORM_SPEED_BY_CLK) {
            int clkNum = Integer.parseInt(value);

            //换切片时更新当前片的总量
            return (clkNum > 0);
        }

        return true;
    }

    public static void reduceBugget(String camId, int type, double bugget) {

        Campaign cam = CacheUtils.getCampaign(camId);
        if (cam == null || cam.launch_type != 2) {
            return;
        }

        if ((cam.budget_type) != 0 && (Double.compare(cam.budget_num, 0.0) == 1)) {
            Date date = new Date();
            String key = "timepoint:" + keyFormatter.format(date);
            String tmp = getTimePointNow(date) + "";
            key = key + ":" + camId + ":" + tmp;

            RedisClusterClient.incrByFloat(key, bugget * -1);
        }
    }

    public static void reduceImpOrClk(String camId, int type) {

        Campaign cam = CacheUtils.getCampaign(camId);
        if (cam == null || cam.launch_type != 2) {
            return;
        }

        if ( (type == UNIFORM_SPEED_BY_IMP && cam.imp_limit > 0) || (type == UNIFORM_SPEED_BY_CLK && cam.click_limit > 0) ) {

            Date date = new Date();
            String key = null;
            if (type == UNIFORM_SPEED_BY_IMP) {
                key = "campaignImp:" + keyFormatter.format(date);
            } else if (type == UNIFORM_SPEED_BY_CLK) {
                key = "campaignClk:" + keyFormatter.format(date);
            }
            String tmp = getTimePointNow(date) + "";
            key = key + ":" + camId + ":" + tmp;

            RedisClusterClient.incrBy(key, -1);
        }
    }


    public static void main(String[] args) {
        String camId = "409";
        Date date = new Date();
        System.out.println(getTimePointNow(new Date()));
        String key = "timepoint:" + keyFormatter.format(date);
        String key2 = "campaignImp:" + keyFormatter.format(date);
        String tmp = getTimePointNow(date) + "";
        key = key + ":" + camId + ":" + tmp;
        key2 = key2 + ":" + camId + ":" + tmp;

        System.out.println(key);
        System.out.println(key2);
        System.out.println(getTimePointNowOld(new Date()));

        System.out.println("73af8f4c2f1639baf953fbe5bd7401f8�\u0002\u0015\n\u00013\u001a\u0010com.lianzainovel�\u0002�\u0002\n");
    }

}
