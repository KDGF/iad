package com.iflytek;

import com.kdg.gnome.share.redisutil.RedisClusterClient;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.HostAndPort;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by hbwang on 2018/8/10
 */
public class RedisXXX {
    public static void main(String[] args) {

        Set<HostAndPort> jedisClusterNodes = getShareRedisHost("140.143.198.122:7000,140.143.198.122:7001,140.143.197.214:7000,140.143.197.214:7001,140.143.197.216:7000,140.143.197.216:7001");


        RedisClusterClient.init(jedisClusterNodes,
                1000, 100, 100, false, 100,    100, 3, "U2FsdGVkX193pmgcVZCFTp5hEBDftsgm");



        System.out.println(RedisClusterClient.getString("effectiveTask_20180810"));
    }

    private static Set<HostAndPort> getShareRedisHost(String hostAndPort) {
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
