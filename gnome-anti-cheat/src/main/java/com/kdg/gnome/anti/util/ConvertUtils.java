package com.kdg.gnome.anti.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 项目描述: 转换工具类
 *
 * 项目名称: ConvertUtils 
 * 创建日期: 2018年6月15日   
 * 修改历史：
 * 		1. [2018年6月15日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class ConvertUtils {
	
	/**
	 * 功能描述: 将token简化（去除时间戳与-）
	 *   
	 * @param token token信息
	 * @return 简化后的token（去除时间戳与-）
	 * [2018年6月15日]创建文件 by lh.qiu
	 */
	public static String convertTokenSimple(String token) {
		String regex = "^(\\S+)-(\\d{13})$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(token);
		if(m.find()) {
			return m.group(1).replaceAll("-", "");
		}
		return token;
	}
}
