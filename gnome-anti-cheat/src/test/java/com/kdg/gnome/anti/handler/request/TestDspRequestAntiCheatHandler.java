package com.kdg.gnome.anti.handler.request;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.kdg.gnome.anti.Constant;
import com.kdg.gnome.anti.handler.AntiCheatHandlerBase;
import com.kdg.gnome.anti.resp.AntiCheatStatus;
import com.kdg.gnome.anti.valueobject.RequestAntiCheatVO;
import com.kdg.gnome.share.redisutil.RedisClusterClient;

/**
 * 项目描述: 广告请求测试类  
 *
 * 项目名称: TestDspRequestAntiCheatHandler 
 * 创建日期: 2018年6月19日   
 * 修改历史：
 * 		1. [2018年6月19日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class TestDspRequestAntiCheatHandler extends AntiCheatHandlerBase {
	
	/**
	 * 功能描述: 测试非法IP
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testRequestIpIllegal() {
		// 创建监测反作弊实体类
		RequestAntiCheatVO requestAntiCheatVO = new RequestAntiCheatVO();
		// 设置IP地址为非法IP地址
		requestAntiCheatVO.setIp("192.168.188a100");
		
		// 判断监测IP是否合法
		AntiCheatStatus reqeustCheat = DspRequestAntiCheatHandler.isReqeustCheat(requestAntiCheatVO);
		assertEquals(AntiCheatStatus.IP_ADD_ILLEGAL, reqeustCheat);
	}

	/**
	 * 功能描述: 测试IP地址黑名单
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testRequestIpBlackList() {
		// 创建请求反作弊实体类
		RequestAntiCheatVO requestAntiCheatVO = new RequestAntiCheatVO();
		// 设置IP地址为非法IP地址
		requestAntiCheatVO.setIp("120.24.39.222");

		// 判断监测IP是否在黑名单中
		AntiCheatStatus reqeustCheat = DspRequestAntiCheatHandler.isReqeustCheat(requestAntiCheatVO);
		assertEquals(AntiCheatStatus.IP_IN_BLACKLIST_FILE, reqeustCheat);
	}
	
	/**
	 * 功能描述: 测试IP是否被曝光频次作弊
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testReqeustIpImpHiden() {
		// 创建请求反作弊实体类
		RequestAntiCheatVO requestAntiCheatVO = new RequestAntiCheatVO();
		String ipAdd = "192.168.188.100";
		// 设置IP地址
		requestAntiCheatVO.setIp(ipAdd);
		// 使IP被曝光频次作弊
		makeIpImpFrequencyHiden(ipAdd);
		// 判断请求是否作弊
		AntiCheatStatus reqeustCheat = DspRequestAntiCheatHandler.isReqeustCheat(requestAntiCheatVO);
		assertEquals(AntiCheatStatus.IMP_TRACKER_IP_FREQUENCY_HIDDEN, reqeustCheat);
	}
	
	/**
	 * 功能描述: 测试IP是否被曝光频次作弊
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testReqeustIpClickHiden() {
		// 创建请求反作弊实体类
		RequestAntiCheatVO requestAntiCheatVO = new RequestAntiCheatVO();
		String ipAdd = "192.168.188.101";
		// 设置IP地址
		requestAntiCheatVO.setIp(ipAdd);
		// 使IP被曝光频次作弊
		makeIpClickFrequencyHiden(ipAdd);
		// 判断请求是否作弊
		AntiCheatStatus reqeustCheat = DspRequestAntiCheatHandler.isReqeustCheat(requestAntiCheatVO);
		assertEquals(AntiCheatStatus.CLICK_TRACKER_IP_FREQUENCY_HIDDEN, reqeustCheat);
	}
	
	/**
	 * 功能描述: 使IP被曝光频次作弊
	 *   
	 * @param ip 请求中参数的IP地址
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	private void makeIpImpFrequencyHiden(String ip) {
		// 曝光监测IP频次屏蔽key
		String impFrequencyHideKey = Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_KEY.replace("${ip}", ip);
		// 将曝光屏蔽的IP写入到Redis中
		RedisClusterClient.set(impFrequencyHideKey, "");
		// 设置曝光屏蔽的IP的过期时间，当超过这个时间后自动解除该IP的曝光屏蔽
		RedisClusterClient.expire(impFrequencyHideKey, Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE);
	}
	
	/**
	 * 功能描述: 使IP被点击频次作弊
	 *   
	 * @param ip 请求中参数的IP地址
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	private void makeIpClickFrequencyHiden(String ip) {
		// 点击监测IP频次屏蔽key
		String impFrequencyHideKey = Constant.CLICK_TRACKER_IP_FREQUENCY_HIDEN_KEY.replace("${ip}", ip);
		// 将点击屏蔽的IP写入到Redis中
		RedisClusterClient.set(impFrequencyHideKey, "");
		// 设置点击屏蔽的IP的过期时间，当超过这个时间后自动解除该IP的点击屏蔽
		RedisClusterClient.expire(impFrequencyHideKey, Constant.CLICK_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE);
	}

}
