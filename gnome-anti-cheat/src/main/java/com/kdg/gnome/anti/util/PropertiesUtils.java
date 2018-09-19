package com.kdg.gnome.anti.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.kdg.gnome.anti.Constant;

/**
 * 项目描述: 配置文件工作类
 *
 * 项目名称: PropertiesUtils 
 * 创建日期: 2018年6月14日   
 * 修改历史：
 * 		1. [2018年6月14日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class PropertiesUtils {

	/**
	 * 功能描述: 初始化所有配置文件
	 *   
	 * @return true：成功，false：失败
	 * [2018年6月14日]创建文件 by lh.qiu
	 */
	public static boolean init() {
		try {
			// 初始化常量配置文件
			initConstant();
		} catch (Exception e) {
			// 有异常时返回失败
			return false;
		}
		
		// 无异常时返回成功
		return true;
	}
	
	/**
	 * 功能描述: 初始化常量配置文件
	 *   
	 * @throws ConfigurationException   
	 * [2018年6月14日]创建文件 by lh.qiu
	 */
	private static void initConstant() throws ConfigurationException {
		
		// constant配置文件路径
		String constantPath = Thread.currentThread().getContextClassLoader().getResource("anti_cheat_constant.properties").getPath();
		// 读取常量配置文件
		PropertiesConfiguration config = getPropertiesConfiguration(constantPath);

		// 监测token，过期的秒数
		Constant.TRACKER_TOKEN_EXPIRE_SECOND = config.getInt("TRACKER_TOKEN_EXPIRE_SECOND", 86400);
		// 监测token，Redis过期的秒数
		Constant.TRACKER_TOKEN_REDIS_EXPIRE_SECOND = config.getInt("TRACKER_TOKEN_REDIS_EXPIRE_SECOND", Constant.TRACKER_TOKEN_EXPIRE_SECOND + 600);
		// 曝光监测IP频次最大数量
		Constant.IMP_TRACKER_IP_FREQUENCY_MAX = config.getInt("IMP_TRACKER_IP_FREQUENCY_MAX", 100);
		// 点击监测IP频次最大数量
		Constant.CLICK_TRACKER_IP_FREQUENCY_MAX = config.getInt("CLICK_TRACKER_IP_FREQUENCY_MAX", 50);
		// 曝光监测IP频次过期秒数
		Constant.IMP_TRACKER_IP_FREQUENCY_EXPIRE = config.getInt("IMP_TRACKER_IP_FREQUENCY_EXPIRE", 10);
		// 点击监测IP频次过期秒数
		Constant.CLICK_TRACKER_IP_FREQUENCY_EXPIRE = config.getInt("CLICK_TRACKER_IP_FREQUENCY_EXPIRE", 10);
		// 曝光监测IP频次屏蔽过期秒数
		Constant.IMP_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE = config.getInt("IMP_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE", 86400);
		// 点击监测IP频次屏蔽过期秒数
		Constant.CLICK_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE = config.getInt("CLICK_TRACKER_IP_FREQUENCY_HIDEN_EXPIRE", 86400);
		// 曝光监测到点击监测的最小毫秒数
		Constant.IMP_TRACKER_2_CLICK_TRACKER_MIN = config.getInt("IMP_TRACKER_2_CLICK_TRACKER_MIN", 200);
	}
	
	/**
	 * 功能描述: 通过配置文件路径获得配置文件对象
	 *   
	 * @param path 配置文件路径 
	 * @return 配置文件对象
	 * @throws ConfigurationException   
	 * [2018年6月14日]创建文件 by lh.qiu
	 */
	private static PropertiesConfiguration getPropertiesConfiguration(String path) throws ConfigurationException {
		// 创建配置文件对象
		PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
		// 设置字符编码为默认字符编码
		propertiesConfiguration.setEncoding(Constant.DEFAULT_ENCODING);
		// 将配置文件内容读取到对象中
		propertiesConfiguration.load(path);
		// 返回配置文件对象
		return propertiesConfiguration;
	}
}
