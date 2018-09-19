package com.kdg.gnome.anti.handler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;

import com.kdg.gnome.anti.main.AntiCheatSystem;
import com.kdg.gnome.ipserver.IPLocationDB;
import com.kdg.gnome.share.UtilOper;
import com.kdg.gnome.share.redisutil.RedisClusterClient;

import junit.framework.Assert;
import redis.clients.jedis.HostAndPort;

/**
 * 项目描述: 反作弊基类
 *
 * 项目名称: AntiCheatHandlerBase 
 * 创建日期: 2018年6月19日   
 * 修改历史：
 * 		1. [2018年6月19日]创建文件 by lh.qiu
 * java版本: JDK 1.8
 */
public abstract class AntiCheatHandlerBase {
	
	/**
	 * 功能描述: 调用单元测试前加载插件
	 *   
	 * @throws InterruptedException   
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	@Before
	public void init() throws InterruptedException {
		// 初始化反作弊模块内容
		boolean init = AntiCheatSystem.init();
		// 判断是否启动成功
		Assert.assertTrue("反作弊模块启动失败", init);

		// 加载IP库
		IPLocationDB.init();
		
		// 加载redis
		while (!initShareRedis()) {
			Thread.sleep(3 * 1000);
		}
	}
	
	/**
	 * 功能描述: 获得随机token
	 *   
	 * @return   
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	protected String getRandomToken() {
		return UUID.randomUUID() + "-" + System.currentTimeMillis();
	}
	
	/**
	 * 功能描述: 通过当前时间戳获得随机token
	 *   
	 * @param timestamp
	 * @return   
	 * [2018年6月19日]创建文件 by lh.qiu
	 */
	protected String getRandomToken(long timestamp) {
		return UUID.randomUUID() + "-" + timestamp;
	}
	
	/**
     * 功能描述: 初始化Share项目中的Redis
     *   
     * @return true：初始化成功，false：初始化失败
     * [2018年7月17日]创建文件 by lh.qiu
     */
	private boolean initShareRedis() {
		// 获得配置文件
		String configFile = Thread.currentThread().getContextClassLoader().getResource("adx.conf").getPath();
		// 配置文件中的Redis节点信息
		String hostAndPort = UtilOper.getStringValue(configFile, "redis_host_and_port", null);
		// 通过配置文件的内容获得Redis主机和端口列表
		Set<HostAndPort> jedisClusterNodes = getShareRedisHost(hostAndPort);
		// 最大连接数
		int maxActive = UtilOper.getIntValue(configFile, "redis_max_active", 500);
		// 最大空闲数
		int maxIdle = UtilOper.getIntValue(configFile, "redis_max_idle", 100);
		// 最大建立连接等待时间单位是毫秒
		long maxWait = UtilOper.getLongValue(configFile, "redis_max_wait", 100);
		// 在borrow一个jedis实例时，是否提前进行validate操作
		boolean testOnBorrow = UtilOper.getBooleanValue(configFile, "redis_test_on_borrow", false);
		// 客户端连接Redis超时时间
		int connectionTimeout = UtilOper.getIntValue(configFile, "redis_connection_timeout", 1);
		// Socket读取超时时间
		int socketTimeout = UtilOper.getIntValue(configFile, "redis_socket_Timeout", 1);
		// 最大重试次数
		int maxRedirection = UtilOper.getIntValue(configFile, "redis_max_redirection", 3);
		// 连接密码
		String pwd = UtilOper.getStringValue(configFile, "redis_pwd", "");

		// 初始化Redis集群结果
		return RedisClusterClient.init(jedisClusterNodes, maxActive, maxIdle, maxWait, testOnBorrow, connectionTimeout,
				socketTimeout, maxRedirection, pwd);
	}
	
	/**
	 * 功能描述: 通过配置文件的内容获得Redis主机和端口列表
	 *   
	 * @param hostAndPort 配置文件中的Redis节点信息
	 * @return Redis主机和端口列表
	 * [2018年7月17日]创建文件 by lh.qiu
	 */
	private Set<HostAndPort> getShareRedisHost(String hostAndPort) {
		// 配置文件中的Redis节点信息为空时，直接返回一个空的Set集合
		if (StringUtils.isBlank(hostAndPort)) {
			return new HashSet<>();
		}
		return Arrays.stream(hostAndPort.split(",")).map(hostAndPostStr -> {
			String[] hAp = hostAndPostStr.split(":");
			return new HostAndPort(hAp[0], Integer.valueOf(hAp[1]));
		}).collect(Collectors.toSet());
	}
}
