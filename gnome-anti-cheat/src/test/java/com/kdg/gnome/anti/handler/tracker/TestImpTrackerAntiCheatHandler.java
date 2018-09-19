package com.kdg.gnome.anti.handler.tracker;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.kdg.gnome.anti.Constant;
import com.kdg.gnome.anti.handler.AntiCheatHandlerBase;
import com.kdg.gnome.anti.resp.AntiCheatStatus;
import com.kdg.gnome.anti.valueobject.TrackerAntiCheatVO;

/**
 * 项目描述: 曝光监测测试类
 *
 * 项目名称: TestImpTrackerAntiCheatHandler 
 * 创建日期: 2018年6月19日   
 * 修改历史：
 * 		1. [2018年6月19日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class TestImpTrackerAntiCheatHandler extends AntiCheatHandlerBase {

	/**
	 * 功能描述: 测试曝光监测过期
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testTrackerTimeout() {
		// 创建监测反作弊实体类
		TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
		// 获得广告请求时间=当前时间-监测过期秒数+1（使监测过期）
		long currentMills = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(Constant.TRACKER_TOKEN_EXPIRE_SECOND + 1);
		// 设置IP地址
		trackerAntiCheatVO.setIp("192.168.188.100");
		// 设置token为过期token
		trackerAntiCheatVO.setToken(getRandomToken(currentMills));

		// 判断监测是否过期
		AntiCheatStatus trackerCheat = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
		assertEquals(AntiCheatStatus.TRACKER_TIMEOUT, trackerCheat);
	}
	
	/**
	 * 功能描述: 测试非法IP
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testTrackerIpIllegal() {
		// 创建监测反作弊实体类
		TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
		// 设置IP地址为非法IP地址
		trackerAntiCheatVO.setIp("192.168.188a100");
		// 设置随机token
		trackerAntiCheatVO.setToken(getRandomToken());

		// 判断监测IP是否合法
		AntiCheatStatus trackerCheat = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
		assertEquals(AntiCheatStatus.IP_ADD_ILLEGAL, trackerCheat);
	}
	
	/**
	 * 功能描述: 测试IP地址黑名单
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testTrackerIpBlackList() {
		// 创建监测反作弊实体类
		TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
		// 设置IP地址为非法IP地址
		trackerAntiCheatVO.setIp("120.24.39.222");
		// 设置随机token
		trackerAntiCheatVO.setToken(getRandomToken());

		// 判断监测IP是否在黑名单中
		AntiCheatStatus trackerCheat = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
		assertEquals(AntiCheatStatus.IP_IN_BLACKLIST_FILE, trackerCheat);
	}

	/**
	 * 功能描述: 测试曝光监测重复
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testTrackerRepeat() {
		// 创建监测反作弊实体类
		TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
		// 设置IP地址
		trackerAntiCheatVO.setIp("192.168.188.100");
		// 设置随机token
		trackerAntiCheatVO.setToken(getRandomToken());

		// 使同一个token请求2次
		for (int times = 0; times < 2; times++) {
			AntiCheatStatus trackerCheat = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
			assertEquals("曝光监测重复", AntiCheatStatus.NORMAL, trackerCheat);
		}
	}
	
	/**
	 * 功能描述: 测试点击与曝光时差过小
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 * @throws InterruptedException 
	 */
	@Test
	public void testImp2ClickOverMin() throws InterruptedException {
		// 创建监测反作弊实体类
		TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
		// 设置IP地址
		trackerAntiCheatVO.setIp("192.168.188.105");
		// 设置token
		trackerAntiCheatVO.setToken(getRandomToken());

		// 调用点击监测
		AntiCheatStatus clickAntiStatus = ClickTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
		assertEquals("调用点击监测失败", AntiCheatStatus.NORMAL, clickAntiStatus);

		// 使线程休眠超过曝光监测到点击监测的最小毫秒数+1
		// Thread.sleep(Constant.IMP_TRACKER_2_CLICK_TRACKER_MIN + 1);

		// 调用曝光监测的反作弊
		AntiCheatStatus impAntiStatus = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
		assertEquals(AntiCheatStatus.CLICK_TRACKER_2_FAST, impAntiStatus);
	}

	/**
	 * 功能描述: 测试IP被频次控制
	 *   
	 * @throws InterruptedException   
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testTrackerIpFrequency() throws InterruptedException {
		// 创建监测反作弊实体类
		TrackerAntiCheatVO trackerAntiCheatVO = new TrackerAntiCheatVO();
		// 设置IP地址
		trackerAntiCheatVO.setIp("192.168.188.105");

		// 使IP被频次控制
		testTrackerIpFrequencyHidden(trackerAntiCheatVO);
		
		// 使线程休眠曝光监测IP频次屏蔽过期秒数+1（使屏蔽过期）
		Thread.sleep(TimeUnit.SECONDS.toMillis(Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE + 1));

		// 再次请求曝光监测
		trackerAntiCheatVO.setToken(getRandomToken());
		// 判断曝光监测是否作弊
		AntiCheatStatus trackerCheat = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
		// 输出监测状态
		System.out.println(String.format("曝光频次过期后再次请求，监测状态：%s", trackerCheat));
		// 判断监测判断是否为normal
		assertEquals(AntiCheatStatus.IMP_TRACKER_IP_FREQUENCY_HIDDEN, trackerCheat);
	}
	
	/**
	 * 功能描述: 使IP被频次控制
	 *   
	 * @param trackerAntiCheatVO 监测反作弊实体类
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	private void testTrackerIpFrequencyHidden(TrackerAntiCheatVO trackerAntiCheatVO) {

		// 曝光监测IP频次最大数量+1（使请求超过限制）
		int maxTimes = Constant.IMP_TRACKER_IP_FREQUENCY_MAX + 1;

		// 单IP循环请求曝光监测P频次最大数量+1
		for (int times = 0; times < maxTimes; times++) {
			// 获得随机token
			trackerAntiCheatVO.setToken(getRandomToken());
			// 判断曝光监测是否作弊
			AntiCheatStatus trackerCheat = ImpTrackerAntiCheatHandler.isTrackerCheat(trackerAntiCheatVO);
			// 输出监测状态
			System.out.println(String.format("曝光监测次数：%s，监测状态：%s", times + 1, trackerCheat));
			// 如果非正常跳出循环
			if (!AntiCheatStatus.NORMAL.equals(trackerCheat)) {
				break;
			}
		}
	}
	
}
