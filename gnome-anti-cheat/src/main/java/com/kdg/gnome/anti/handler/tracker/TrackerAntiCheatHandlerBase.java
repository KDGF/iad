package com.kdg.gnome.anti.handler.tracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kdg.gnome.anti.Constant;

/**
 * 项目描述: 监测反作弊基类  
 *
 * 项目名称: TrackerAntiCheatHandlerBase 
 * 创建日期: 2018年6月13日   
 * 修改历史：
 * 		1. [2018年6月13日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public abstract class TrackerAntiCheatHandlerBase {

	/**
	 * 功能描述: 通过广告请求时间，判断监测是否超时
	 *   
	 * @param requestTimestamp 广告请求时间
	 * @param currentTimeMillis 当前系统时间
	 * 
	 * @return true：超时，false：未超时
	 * [2018年6月12日]创建文件 by lh.qiu
	 */
	protected static boolean isTrackerTimeout(long requestTimestamp, long currentTimeMillis) {
		// 监测与请求时间的差值
		long diffVal = currentTimeMillis - requestTimestamp;

		// 当差值<0或差值>监测过期时间时为超时监测
		return diffVal < 0 || diffVal > Constant.TRACKER_TOKEN_EXPIRE_SECOND * 1000;
	}
	
	/**
	 * 功能描述: 通过token获得广告请求时间戳
	 *   
	 * @param token 监测中的token信息
	 * @return 0：token信息异常，13位数字：请求广告时间戳
	 * [2018年6月12日]创建文件 by lh.qiu
	 */
	protected static long getRequestTimestampByToken(String token) {
		// token校验格式（token格式：UUID-毫秒时间戳）
		String regex = "^\\S+-(\\d{13})$";
		// 正则匹配
		Pattern tokenPattern = Pattern.compile(regex);
		Matcher tokenTimestampMatcher = tokenPattern.matcher(token);
		
		// 当格式不符合token信息时，直接返回0
		if (!tokenTimestampMatcher.find()) {
			return 0L;
		}

		// 当格式符合token信息时，获得广告请求时间并返回
		return Long.parseLong(tokenTimestampMatcher.group(1));
	}
}
