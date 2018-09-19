package com.kdg.gnome.anti.main;

import com.kdg.gnome.anti.util.PropertiesUtils;

/**
 * 项目描述: 反作弊系统类
 *
 * 项目名称: AntiCheatSystem 
 * 创建日期: 2018年6月14日   
 * 修改历史：
 * 		1. [2018年6月14日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class AntiCheatSystem {

	/**
	 * 功能描述: 初始化该模块内容
	 *   
	 * @return true：初始化成功，false：初始化失败
	 * [2018年6月14日]创建文件 by lh.qiu
	 */
	public static boolean init() {
		// 初始化配置文件
		return PropertiesUtils.init();
	}
}
