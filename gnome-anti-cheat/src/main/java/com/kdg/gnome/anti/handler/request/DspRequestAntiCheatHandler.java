package com.kdg.gnome.anti.handler.request;

import com.kdg.gnome.anti.Constant;
import com.kdg.gnome.anti.resp.AntiCheatStatus;
import com.kdg.gnome.anti.util.ValidateUtils;
import com.kdg.gnome.anti.valueobject.RequestAntiCheatVO;
import com.kdg.gnome.share.redisutil.RedisClusterClient;
import com.kdg.gnome.util.IpUtil;

/**
 * 项目描述: 判断DSP请求是否为作弊请求
 *
 * 项目名称: DspRequestAntiCheatHandler 
 * 创建日期: 2018年6月13日   
 * 修改历史：
 * 		1. [2018年6月13日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class DspRequestAntiCheatHandler {
	
	/**
	 * 功能描述: 判断请求是否作弊
	 *   
	 * @param requestAntiCheatVO 请求反作弊实体类
	 * @return 请求的作弊类型
	 * [2018年6月13日]创建文件 by lh.qiu
	 */
	public static AntiCheatStatus isReqeustCheat(RequestAntiCheatVO requestAntiCheatVO) {

		// 判断IP地址是否合法
		if(!ValidateUtils.isIpLegal(requestAntiCheatVO.getIp())) {
			return AntiCheatStatus.IP_ADD_ILLEGAL;
		}
		
		// 判断IP是否在广协黑名单库
		if (IpUtil.checkIpBlackList(requestAntiCheatVO.getIp())) {
			return AntiCheatStatus.IP_IN_BLACKLIST_FILE;
		}
		
		// 判断IP是否被曝光频次屏蔽
		if (isIpImpFrequencyHidden(requestAntiCheatVO.getIp())) {
			return AntiCheatStatus.IMP_TRACKER_IP_FREQUENCY_HIDDEN;
		}

		// 判断IP是否被点击频次屏蔽
		if (isIpClickFrequencyHidden(requestAntiCheatVO.getIp())) {
			return AntiCheatStatus.CLICK_TRACKER_IP_FREQUENCY_HIDDEN;
		}

		return AntiCheatStatus.NORMAL;
	}

	/**
	 * 功能描述: 判断IP是否被曝光频次屏蔽
	 *   
	 * @param ip 请求IP地址
	 * @return true：被屏蔽。false：未被屏蔽
	 * [2018年6月13日]创建文件 by lh.qiu
	 */
	private static boolean isIpImpFrequencyHidden(String ip) {
		// 曝光监测IP频次屏蔽key
		String impFrequencyHideKey = Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_KEY.replace("${ip}", ip);
		// 曝光监测IP频次屏蔽key存在时（表示此IP已被曝光屏蔽），返回true
		return RedisClusterClient.exits(impFrequencyHideKey);
	}

	/**
	 * 功能描述: 判断IP是否被点击频次屏蔽
	 *   
	 * @param ip 请求IP地址
	 * @return true：被屏蔽。false：未被屏蔽
	 * [2018年6月13日]创建文件 by lh.qiu
	 */
	private static boolean isIpClickFrequencyHidden(String ip) {
		// 点击监测IP频次屏蔽key
		String clickFrequencyHideKey = Constant.CLICK_TRACKER_IP_FREQUENCY_HIDEN_KEY.replace("${ip}", ip);
		// 点击监测IP频次屏蔽key存在时（表示此IP已被点击屏蔽），返回true
		return RedisClusterClient.exits(clickFrequencyHideKey);
	}
}
