package com.kdg.gnome.anti.valueobject;

/**
 * 项目描述: 监测反作弊实体类
 *
 * 项目名称: TrackerAntiCheatVO 
 * 创建日期: 2018年6月13日   
 * 修改历史：
 * 		1. [2018年6月13日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public class TrackerAntiCheatVO {
	private String token; // 请求token
	private String ip; // 请求IP

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}
