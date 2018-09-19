package com.kdg.gnome.share.redisutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisClusterClient {

    private static JedisCluster jc;
    private final static Logger log = LoggerFactory.getLogger(RedisClusterClient.class);
    public static final String FREQ_HOUR_REACH = "hour_reach";
    public static final String FREQ_DAY_REACH = "day_reach";
    public static final String FREQ_CYCLE_REACH = "cycle_reach";

    /**
     * 功能描述: 初始化Redis集群
     *   
     * @param jedisClusterNodes Redis集群节点
     * @param maxActive 最大连接数
     * @param maxIdle 最大空闲数
     * @param maxWait 最大建立连接等待时间单位是毫秒
     * @param testOnBorrow 在borrow一个jedis实例时，是否提前进行validate操作
     * @param connectionTimeout 客户端连接Redis超时时间
     * @param socketTimeout Socket读取超时时间
     * @param maxRedirection 最大重试次数
     * @param pwd 连接密码
     * @return true：初始化成功，false：初始化失败
     * [2018年7月17日]创建文件 by lh.qiu
     */
    public static boolean init(Set<HostAndPort> jedisClusterNodes, int maxActive, int maxIdle, long maxWait,
			boolean testOnBorrow, int connectionTimeout, int socketTimeout, int maxRedirection, String pwd) {
		// 池基本配置
        JedisPoolConfig config = new JedisPoolConfig();
        
		// 最大连接数。如果赋值为-1，则表示不限制；如果pool已经分配了MaxTotal个jedis实例，则此时pool的状态为exhausted(耗尽)。
		config.setMaxTotal(maxActive);

		// 最大空闲数
		config.setMaxIdle(maxIdle);

		// 最大建立连接等待时间单位是毫秒。表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
		config.setMaxWaitMillis(maxWait);

		// 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
		config.setTestOnBorrow(testOnBorrow);

		try {
			// jc = new JedisCluster(jedisClusterNodes, 200, 200, 3, pwd, config);
			jc = new JedisCluster(jedisClusterNodes, connectionTimeout, socketTimeout, maxRedirection, pwd, config);
			return true;
		} catch (Exception e) {
			log.error("connect redis exception.");
			return false;
		}
	}

    public static void closed() {
        if (jc != null) {
            //销毁连接
            try {
                jc.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 根据key设置
     *
     * @param key
     * @return
     */
    public static void setString(String key, String value) {
        try {
            if (key != null){
                jc.set(key, value);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 根据key获取
     *
     * @param key
     * @return
     */
    public static String getString(String key) {
        String value = null;
        try {
            if (key != null){
                value = jc.get(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return value;
    }

    /**
     * 根据key获取
     *
     * @param key
     * @return Map
     */
    public static Set<String> getSet(String key) {
        Set<String> results = null;
        try {
            if (key != null){
                results = jc.smembers(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }


    /**
     * 根据key获取
     *
     * @param key
     * @return Map
     */
    public static Map<String, String> hgetCreativeCtr(String key) {
        Map<String, String> results = null;
        try {
            if (key != null){
                results = jc.hgetAll(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return results;
    }


    public static Map<String, String> hgetAll(String key) {
        Map<String, String> results = null;
        try {
            if (key != null){
                results = jc.hgetAll(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return results;
    }

    //发布消息
    public static Long publish(String channel, String msg) {
        long results = 0;
        try {
            if (channel != null){
                results =  jc.publish(channel, msg);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }


    public static Double zscore(String key, String field) {
        Double results = null;
        try {
            if (key != null) {
                results = jc.zscore(key, field);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    public static boolean hexists(String key, String field) {
        boolean results = false;
        try {
            if (key != null) {
                results = jc.hexists(key, field);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    /**
     * 根据key,field获取Hash某一属性
     *
     * @param key,field
     * @return String
     */
    public static String getHashFiled(String key, String filed) {
        String results = null;
        try {
            if (key != null) {
                results = jc.hget(key, filed);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    /**
     * 根据key,field1,field2获取Hash一组属性
     *
     * @param key,field1,field2
     * @return String
     */
    public static List<String> hmget(String key, String feild1, String feild2) {
        List<String> results = null;
        try {
            if (key != null) {
                results = jc.hmget(key, feild1, feild2);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }


    /**
     * 根据key,field获取Hash某一属性，会抛出异常，需要catch
     *
     * @param key,field
     * @return String
     */
    public static String getHashFiledThrowException(String key, String filed) {
        String results = null;
        try {
            if (key != null) {
                results = jc.hget(key, filed);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
        return results;
    }

    /**
     * 根据key,field对Hash自增
     *
     * @param key,field,num
     * @return String
     */
    public static long hincrby(String key, String field, long num) {
        long results = 0;
        try {
            if (key != null) {
                results = jc.hincrBy(key, field, num);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    public static long hincrby(String key, String field) {
        long results = 0;
        try {
            if (key != null) {
                results = jc.hincrBy(key, field, 1);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    /**
     * 根据key,field对Hash自增
     *
     * @param key,field,num
     * @return String
     */
    public static double hincrbyFloat(String key, String field, double num) {
        double results = 0;
        try {
            if (key != null) {
                results = jc.hincrByFloat(key, field, num);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    /**
     * 根据key自增
     *
     * @param key
     * @return String
     */
    public static long incrBykey(String key) {
        long results = 0;
        try {
            if (key != null) {
                results = jc.incr(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    /**
     * 为hash某一属性set新值
     *
     * @param key,field,value
     * @return long
     */
    public static long hset(String key, String filed, String value) {
        long results = 0;
        try {
            if (key != null) {
                results = jc.hset(key, filed, value);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }


    public static String hget(String key, String filed) {
        String results = null;
        try {
            if (key != null && filed != null) {
                results = jc.hget(key, filed);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    /**
     * 设置有效秒数
     *
     * @param key,sec
     * @return long
     */
    public static long expire(String key, int sec) {
        long results = 0;
        try {
            if (key != null) {
                results = jc.expire(key, sec);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    public static String setex(String key, int sec, String value) {
        String results = null;
        try {
            if (key != null) {
                results = jc.setex(key, sec, value);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }


    /**
     * 判断是否存在
     *
     * @param key,sec
     * @return long
     */
    public static boolean exits(String key) {
        boolean results = false;
        try {
            if (key != null) {
                results = jc.exists(key);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

    public static boolean isFilteredByDeviceFrequency(String camId, String field) {
        try {
            if (null != jc.zscore(FREQ_HOUR_REACH, camId + "_" + field)) {
                return true;
            } else if (null != jc.zscore(FREQ_DAY_REACH, camId + "_" + field)) {
                return true;
            } else if (null != jc.zscore(FREQ_CYCLE_REACH, camId + "_" + field)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 功能描述: 当哈希集中不存在该field时设置该值并返回1
     *   
     * @param key 哈希的key名称
     * @param field 哈希的字段
     * @param value 哈希的值
     * @return 1：如果字段是个新的字段，并成功赋值。0：如果哈希集中已存在该字段，没有操作被执行
     * [2018年6月13日]创建文件 by lh.qiu
     */
	public static Long hsetnx(String key, String field, String value) {

		long results = 0L;
		try {
			if (key != null) {
				results = jc.hsetnx(key, field, value);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return results;
	}
	
	/**
	 * 功能描述: 执行原子增加一个整数
	 *   
	 * @param key 需要增加的key
	 * @param value 增加的值
	 * @return 增加之后的value值。
	 * [2018年6月13日]创建文件 by lh.qiu
	 */
	public static Long incrBy(String key, int value) {
		long results = 0L;
		try {
			if (key != null) {
				results = jc.incrBy(key, value);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return results;
	}


    /**
     * 功能描述: 执行原子增加一个整数
     *
     * @param key 需要增加的key
     * @param value 增加的值
     * @return 增加之后的value值。
     * [2018年6月13日]创建文件 by lh.qiu
     */
    public static Double incrByFloat(String key, Double value) {
        Double results = 0.0;
        try {
            if (key != null) {
                results = jc.incrByFloat(key, value);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return results;
    }

	/**
	 * 功能描述: 设置一个key的value值
	 *   
	 * @param key 设置的key
	 * @param value 设置的值
	 * @return 如果SET命令正常执行那么回返回OK，否则如果加了NX 或者 XX选项，但是没有设置条件。那么会返回nil。
	 * [2018年6月13日]创建文件 by lh.qiu
	 */
	public static String set(String key, String value) {
		String results = null;
		try {
			if (key != null) {
				results = jc.set(key, value);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return results;
	}
}
