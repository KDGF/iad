package com.kdg.gnome.anti;


/**
 * 项目描述: 常量类  
 *
 * 项目名称: Constant 
 * 创建日期: 2018年6月12日   
 * 修改历史：
 * 		1. [2018年6月12日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class Constant {
	
	/** 默认字符编码 **/
	public static final String DEFAULT_ENCODING = "UTF-8";

	/*********************************************************************************
     * 唯一性校验与扣费的相关字段
     *********************************************************************************/
	
	/** 监测token，监测key **/
	public static final String TRACKER_TOKEN_PRE = "dsp:tracker:${token}";
	
	/** 监测token，曝光监测的时间戳key **/
	public static final String TRACKER_TOKEN_IMP_KEY = "imp:tracker:time";
	
	/** 监测token，点击监测的时间戳key **/
	public static final String TRACKER_TOKEN_CLICK_KEY = "click:tracker:time";
	
	/** 监测token，是否扣费标识key **/
	public static final String TRACKER_TOKEN_DEDUCTION_KEY = "deduction:flag";
	
	
	/*********************************************************************************
     * 屏蔽频次的相关字段
     *********************************************************************************/

	/** 曝光监测IP频次屏蔽key **/
	public static final String IMP_TRACKER_IP_FREQUENCY_HIDEN_KEY = "dsp:imp:frequency:hidden:${ip}";
	
	/** 点击监测IP频次屏蔽key **/
	public static final String CLICK_TRACKER_IP_FREQUENCY_HIDEN_KEY = "dsp:click:frequency:hidden:${ip}";
	
	/** 曝光监测IP频次统计key **/
	public static final String IMP_TRACKER_IP_FREQUENCY_STATISTICS_KEY = "dsp:imp:frequency:statistics:${ip}";
	
	/** 点击监测IP频次统计key **/
	public static final String CLICK_TRACKER_IP_FREQUENCY_STATISTICS_KEY = "dsp:click:frequency:statistics:${ip}";
	
	
	/*********************************************************************************
     * 过期时间的相关字段
     *********************************************************************************/
	
	/** 监测token，过期的秒数（默认24H） **/
	public static int TRACKER_TOKEN_EXPIRE_SECOND = 86400;
	
	/** 监测token，Redis过期的秒数（监测的过期时间+10min） **/
	public static int TRACKER_TOKEN_REDIS_EXPIRE_SECOND = TRACKER_TOKEN_EXPIRE_SECOND + 600;
	
	/** 曝光监测IP频次最大数量 **/
	public static int IMP_TRACKER_IP_FREQUENCY_MAX = 100;
	
	/** 点击监测IP频次最大数量 **/
	public static int CLICK_TRACKER_IP_FREQUENCY_MAX = 50;
	
	/** 曝光监测IP频次过期秒数 **/
	public static int IMP_TRACKER_IP_FREQUENCY_EXPIRE = 10;

	/** 点击监测IP频次过期秒数 **/
	public static int CLICK_TRACKER_IP_FREQUENCY_EXPIRE = 10;

	/** 曝光监测IP频次屏蔽过期秒数（默认24H） **/
	public static int IMP_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE = 86400;

	/** 点击监测IP频次屏蔽过期秒数（默认24H） **/
	public static int CLICK_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE = 86400;
	
	/** 曝光监测到点击监测的最小毫秒数 **/
	public static int IMP_TRACKER_2_CLICK_TRACKER_MIN = 200;
	
}
