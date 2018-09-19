package com.kdg.gnome.anti.handler.tracker;

import org.apache.commons.lang.StringUtils;

import com.kdg.gnome.anti.Constant;
import com.kdg.gnome.anti.resp.AntiCheatStatus;
import com.kdg.gnome.anti.util.ConvertUtils;
import com.kdg.gnome.anti.util.ValidateUtils;
import com.kdg.gnome.anti.valueobject.TrackerAntiCheatVO;
import com.kdg.gnome.share.redisutil.RedisClusterClient;
import com.kdg.gnome.util.IpUtil;

/**
 * 项目描述: 曝光监测反作弊实现类  
 *
 * 项目名称: ImpTrackerAntiCheatHandler 
 * 创建日期: 2018年6月12日   
 * 修改历史：
 * 		1. [2018年6月12日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class ImpTrackerAntiCheatHandler extends TrackerAntiCheatHandlerBase {

	/**
	 * 
	 * 功能描述: 判断曝光监测是否作弊
	 *   
	 * @param trackerAntiCheatVO 监测反作弊实体类
	 * @return 监测的作弊状态
	 * [2018年6月13日]创建文件 by lh.qiu
	 */
	public static AntiCheatStatus isTrackerCheat(TrackerAntiCheatVO trackerAntiCheatVO) {

		// 曝光监测的请求时间
		long impTrackerTimestamp = System.currentTimeMillis();
		// 通过token获得广告请求时间戳
		long requestTimestamp = getRequestTimestampByToken(trackerAntiCheatVO.getToken());
		
		// 判断监测是否过期（是否与请求超过24H）
		if (isTrackerTimeout(requestTimestamp, impTrackerTimestamp)) {
			return AntiCheatStatus.TRACKER_TIMEOUT;
		}

		// 判断IP地址是否合法
		if(!ValidateUtils.isIpLegal(trackerAntiCheatVO.getIp())) {
			return AntiCheatStatus.IP_ADD_ILLEGAL;
		}

		// 判断IP是否在广协黑名单库
		if (IpUtil.checkIpBlackList(trackerAntiCheatVO.getIp())) {
			return AntiCheatStatus.IP_IN_BLACKLIST_FILE;
		}
		
		// 通过token判断监测是否重复
		if (isTrackerRepeat(trackerAntiCheatVO.getToken(), impTrackerTimestamp)) {
			return AntiCheatStatus.IMP_TRACKER_REPEAT;
		}
		
		// 通过token判断点击监测与曝光监测时差是否过小
//		if (isImp2ClickOverMin(trackerAntiCheatVO.getToken(), impTrackerTimestamp)) {
//			return AntiCheatStatus.CLICK_TRACKER_2_FAST;
//		}

		// 通过IP判断监测是否被IP频次屏蔽
		if (isImpTrackerIpFrequency(trackerAntiCheatVO.getIp(), impTrackerTimestamp)) {
			return AntiCheatStatus.IMP_TRACKER_IP_FREQUENCY_HIDDEN;
		}
		
		return AntiCheatStatus.NORMAL;
	}
	
	/**
	 * 功能描述: 通过token判断监测是否重复
	 *   
	 * @param token 监测中的token信息
	 * @param currentTimeMillis 当前系统时间
	 * @return true：重复，false：不重复
	 * 
	 * [2018年6月12日]创建文件 by lh.qiu
	 */
	private static boolean isTrackerRepeat(String token, long currentTimeMillis) {
		// redis中，排重监测Key dsp:tracker:token
		String simpleToken = ConvertUtils.convertTokenSimple(token);
		String dspTrackerTokenKey = Constant.TRACKER_TOKEN_PRE.replace("${token}", simpleToken);

		// 通过hset设置redis中请求监测的时间戳
		Long hset = RedisClusterClient.hsetnx(dspTrackerTokenKey, Constant.TRACKER_TOKEN_IMP_KEY, String.valueOf(currentTimeMillis));

		// 当hset返回大于0时（说明此监测是第一次请求）返回false
		if(hset > 0) {
			RedisClusterClient.expire(dspTrackerTokenKey, Constant.TRACKER_TOKEN_REDIS_EXPIRE_SECOND);
			return false;
		}
		
		// 当hset返回0时（说明此监测不是第一次请求）返回true
		return true;
	}
	
	/**
	 * 功能描述: 通过token判断点击监测与曝光监测时差是否过小
	 *   
	 * @param token 监测中的token信息
	 * @param impTrackerTimestamp 曝光监测的请求时间
	 * @return true：点击监测与曝光监测时差过小，false：点击监测与曝光监测时差正常
	 * [2018年6月12日]创建文件 by lh.qiu
	 */
	@Deprecated
	@SuppressWarnings("unused")
	private static boolean isImp2ClickOverMin(String token, long impTrackerTimestamp) {
		// redis中，排重监测Key dsp:tracker:token
		String simpleToken = ConvertUtils.convertTokenSimple(token);
		// 监测token，监测key
		String trackerTokenKey = Constant.TRACKER_TOKEN_PRE.replace("${token}", simpleToken);
		// 曝光监测的请求时间
		String clickTrackerTimestampStr = RedisClusterClient.hget(trackerTokenKey, Constant.TRACKER_TOKEN_CLICK_KEY);

		// 如果点击监测的请求时间为空时，直接返回false,等两个监测同时到达时再做处理
		if (StringUtils.isBlank(clickTrackerTimestampStr)) {
			return false;
		}

		// 点击监测的请求时间
		long clickTrackerTimestamp = Long.parseLong(clickTrackerTimestampStr);

		// 当曝光监测与点击监测的时差的绝对值小于阈值时，返回true
		return Math.abs(clickTrackerTimestamp - impTrackerTimestamp) < Constant.IMP_TRACKER_2_CLICK_TRACKER_MIN;
	}
	
	/**
	 * 功能描述: 通过IP判断监测是否被IP频次屏蔽
	 *   
	 * @param ip 请求IP地址
	 * @param impTrackerTimestamp 曝光时间戳
	 * @return  true：IP被频次屏蔽，false：IP未被频次屏蔽
	 * [2018年6月12日]创建文件 by lh.qiu
	 */
	private static boolean isImpTrackerIpFrequency(String ip, long impTrackerTimestamp) {
		// 曝光监测IP频次屏蔽key
		String impFrequencyHideKey = Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_KEY.replace("${ip}", ip);
		// 曝光监测IP频次屏蔽key存在时（表示此IP已被曝光屏蔽），返回true
		if(RedisClusterClient.exits(impFrequencyHideKey)) {
			return true;
		}
		
		// 曝光监测IP频次统计key
		String impFrequencyStatisticsKey = Constant.IMP_TRACKER_IP_FREQUENCY_STATISTICS_KEY.replace("${ip}", ip);
		// 使曝光监测IP频次统计自增并获得返回值
		Long impFrequencyCount = RedisClusterClient.incrBy(impFrequencyStatisticsKey, 1);
		// 当曝光监测IP频次统计>曝光监测IP频次最大数量时，屏蔽此IP
		if(impFrequencyCount > Constant.IMP_TRACKER_IP_FREQUENCY_MAX) {
			// 将曝光屏蔽的IP写入到Redis中
			RedisClusterClient.set(impFrequencyHideKey, impTrackerTimestamp + "");
			// 设置曝光屏蔽的IP的过期时间，当超过这个时间后自动解除该IP的曝光屏蔽
			RedisClusterClient.expire(impFrequencyHideKey, Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE);
			return true;
		}
		
		// 当曝光IP频次统计为1时，说明该IP此段时间内第1次请求曝光，设置该key的过期时间
		if(impFrequencyCount == 1) {
			RedisClusterClient.expire(impFrequencyStatisticsKey, Constant.IMP_TRACKER_IP_FREQUENCY_EXPIRE);
		}
		
		// IP未被曝光频次控制，返回false
		return false;
	}

}
