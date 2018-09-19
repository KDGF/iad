package com.kdg.gnome.anti.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * 项目描述: 校验工具类
 *
 * 项目名称: ValidateUtils 
 * 创建日期: 2018年6月15日   
 * 修改历史：
 * 		1. [2018年6月15日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class ValidateUtils {

	/**
	 * 功能描述: 判断IP是否合法
	 *   
	 * @param ipAdd IP地址
	 * @return true：合法，false：不合法
	 * [2018年6月15日]创建文件 by lh.qiu
	 */
	public static boolean isIpLegal(String ipAdd) {
		// IP为空直接返回false
		if (StringUtils.isEmpty(ipAdd)) {
			return false;
		}
		// IP正则表达式
		String ipRegex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
		// 对IP进行正则匹配
		Pattern pattern = Pattern.compile(ipRegex);
		Matcher matcher = pattern.matcher(ipAdd);
		return matcher.matches();
	}
}
