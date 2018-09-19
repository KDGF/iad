package com.kdg.gnome.anti;

import org.junit.Test;

import com.kdg.gnome.anti.main.AntiCheatSystem;

import junit.framework.Assert;

/**
 * 项目描述: 反作弊模块测试类
 *
 * 项目名称: TestAntiCheatSystem 
 * 创建日期: 2018年6月19日   
 * 修改历史：
 * 		1. [2018年6月19日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class TestAntiCheatSystem {
	
	/**
	 * 
	 * 功能描述: 测试模块加载
	 *      
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Test
	public void testSystemLoad() {
		// 初始化反作弊模块内容
		boolean init = AntiCheatSystem.init();
		// 判断是否启动成功
		Assert.assertTrue("反作弊模块启动失败", init);

		System.out.println(Constant.TRACKER_TOKEN_EXPIRE_SECOND);
		
		// 判断监测token，过期的秒数是否为空
		Assert.assertNotNull("监测token，过期的秒数为空", Constant.TRACKER_TOKEN_EXPIRE_SECOND);
	}
}
