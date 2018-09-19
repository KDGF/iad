package com.kdg.gnome.adx.module;

import com.kdg.gnome.adx.handler.GAdMonitorServerHandler;
import com.kdg.gnome.adx.handler.GServiceServerHandler;
import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.scheduler.DataLoadingTask;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.adx.share.SysConfigManager;
import com.kdg.gnome.adx.share.ThreadPool;
import com.kdg.gnome.adx.utils.kafka.KafkaConsumerUtil;
import com.kdg.gnome.anti.main.AntiCheatSystem;
import com.kdg.gnome.http.GServer;
import com.kdg.gnome.ipserver.IPLocationDB;
import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.GThreadFactory;
import com.kdg.gnome.share.UtilOper;
import com.kdg.gnome.share.redisutil.RedisClusterClient;
import com.kdg.gnome.share.task.GMsg;
import com.kdg.gnome.share.task.GTaskBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.HostAndPort;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GSysMgrModule extends GTaskBase {

    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");
    private static final ScheduledExecutorService timer = Executors
            .newSingleThreadScheduledExecutor(new GThreadFactory("data-loading-task"));

    private final static int TIMER_MILL_SECOND = 1000;

    /* 系统状态控制信息 */
    private volatile int sysState;
    private volatile int sysInitStep;
    private int timerInInitState = 1;

    /* 以下是系统资源 */
    private GServer srvHttpServer;
    private GServer adMonitorHttpServer;

    private SysConfigManager sysConfigManager;

    private GServiceModule srvModule;
    private G3PlatMgrModule plat3MgrModule;

    private KafkaConsumerUtil trackingConsumer;

    public GSysMgrModule(String configFile) {
        super("sys-mgr-task", 0);

        sysConfigManager = new SysConfigManager();
        sysConfigManager.setConfigFile(configFile);

        /* 系统处于初始化状态 */
        sysState = AdxConstants.SERVICE_STATUS_INIT;

        /* 初始化步骤从0开始, 往后面逐步初始化 */
        sysInitStep = 0;
    }

    @Override
    public boolean startTask() {
        /* 第一步：初始化系统配置 */
        if (sysInitStep < 1) {
            if (!sysConfigManager.initConfig()) {
                log.error("init sysConfig fail...init_step is 1, and continue after a while.");
                return false;
            }

            log.info("init sysConfig success...init_step is 1, and continue next.");
            sysInitStep++;
        }

        /* 第二步：初始化业务模块 */
        if (sysInitStep < 2) {
            srvModule = new GServiceModule();
            if (!srvModule.startModule(sysConfigManager.serverConfig.getSrvTaskNum())) {
                log.error("init srvModule fail...init step is 2, and continue after a while.");
                return false;
            }

            log.info("init srvModule success...init_step is 2, and continue next.");
            sysInitStep++;
        }

        /* 第四步：初始化第三方平台管理模块 */
        if (sysInitStep < 3) {
            plat3MgrModule = new G3PlatMgrModule();
            if (!plat3MgrModule.startTask()) {
                log.error("init plat3MgrModule fail...init step is 4, and continue after a while.");
                return false;
            }

            log.info("init plat3MgrModule success...init_step is 4, and continue next.");
            sysInitStep++;
        }

        /* 第五步：初始化广告请求通信服务模块 */
        if (sysInitStep < 4) {
            srvHttpServer = new GServer(sysConfigManager.serverConfig.getSrvIp(), sysConfigManager
                    .serverConfig.getSrvPort(), new GServiceServerHandler(),
                    sysConfigManager.serverConfig.getServerIoThreads());
            if (!srvHttpServer.start()) {
                log.error("init srvHttpServer fail...init step is 5, and continue after a while.");
                return false;
            }

            log.info("init srvHttpServer success...init_step is 5, and continue next.");
            sysInitStep++;
        }


        /* 第六步：初始IP库*/
        if (sysInitStep < 5) {
            IPLocationDB.init();

            if (IPLocationDB.ipLocation == null) {
                log.error("init IPLocationDB fail...init step is 6, and continue after a while.");
                return false;
            }

            log.info("init IPLocationDB success...init_step is 6, and continue next.");

            sysInitStep++;
        }

        /* 第六步：初始化flume组件 */

        /* 第七步：初始化广告监控通信服务模块 */
        if (sysInitStep < 6) {
            adMonitorHttpServer = new GServer(sysConfigManager.serverConfig.getAdMonitorIp(),
                    sysConfigManager.serverConfig.getadMonitorPort(),
                    new GAdMonitorServerHandler());
            if (!adMonitorHttpServer.start()) {
                log.error("init adMonitorHttpServer fail...init step is 7, and continue after a while.");
                return false;
            }

            log.info("init srvHttpServer success...init_step is 7, and continue next.");
            sysInitStep++;
        }

        if (sysInitStep < 7) {
            try {
                String hostName = "127.0.0.1";
                try {
                    if (InetAddress.getLocalHost() != null && InetAddress.getLocalHost().getHostName() != null) {
                        hostName = InetAddress.getLocalHost().getHostName();
                    }
                } catch (Exception e) {
                    log.error("getLocalHost Exception");
                }

                trackingConsumer = new KafkaConsumerUtil(sysConfigManager.serverConfig.kafkaTrackingTopic,
                        sysConfigManager.serverConfig.kafkaTrackingThreads,
                        sysConfigManager.serverConfig.kafkaZookeepers,
                        hostName);
                new Thread(trackingConsumer).start();
                sysInitStep++;
            } catch (Exception e) {
                log.info("init kafkaConsumerUtil failed...");
                e.printStackTrace();
                return false;
            }
        }
        // 初始化反作弊系统参数
        if (sysInitStep < 8) {
            if (!AntiCheatSystem.init()) {
                log.error("init AntiCheatSystem config fail...init_step is 9, and continue after a while.");
                return false;
            }

            log.info("init AntiCheatSystem config success...init_step is 9, and continue next.");
            sysInitStep++;
        }

        // redis
        if (!initShareRedis()) {
            log.error("init redis failed.");
            return false;
        }


        ThreadPool.initPool();


        /* 至此, 系统全部初始化完成 */
        log.info("System init OK! to work!!!");
        sysState = AdxConstants.SERVICE_STATUS_WORK;

        startDataLodingTask();

        return true;
    }

    // 定时拉取数据库任务
    private void startDataLodingTask() {
        int dbLoadPeriod = sysConfigManager.dbLoadPeriod;
        if (dbLoadPeriod <= 0) {
            log.error("invalid dbLoadPeriod = {}", dbLoadPeriod);
            dbLoadPeriod = 10;
        }
        timer.scheduleAtFixedRate(new DataLoadingTask(), dbLoadPeriod, dbLoadPeriod, TimeUnit.MINUTES);

    }

    public GServiceModule getSrvModule() {
        return srvModule;
    }

    public G3PlatMgrModule get3PlatMgrModule() {
        return plat3MgrModule;
    }

    private void sysEnterToInitState() {
        addMsg(AdxConstants.SERVICE_STATUS_INIT, null);
    }

    private void sysEnterToWorkState() {
        addMsg(AdxConstants.SERVICE_STATUS_WORK, null);
    }

    private void sysEnterToQuitState() {
        addMsg(AdxConstants.SERVICE_STATUS_QUIT, null);
    }

    @Override
    protected void handlerMsg(int msgId, Object objContext) {
        switch (msgId) {
            case AdxConstants.SERVICE_STATUS_INIT: {
                sysState = AdxConstants.SERVICE_STATUS_INIT;
                sysInitStep = 0;

                /* 系统进入初始化状态时, 便定时启动系统各个模块、配置, 直到全部启动成功, 才进入工作状态 */
                while (running) {
                    if (AdxConstants.SERVICE_STATUS_WORK == sysState) {
                        sysEnterToWorkState();
                        break;
                    } else if (AdxConstants.SERVICE_STATUS_QUIT == sysState) {
                        sysEnterToQuitState();
                        break;
                    }

                    timerHandlerInit();
                }
                break;
            }

            case AdxConstants.SERVICE_STATUS_WORK: {
                while (running) {
                    if (AdxConstants.SERVICE_STATUS_INIT == sysState) {
                        sysEnterToInitState();
                        break;
                    } else if (AdxConstants.SERVICE_STATUS_QUIT == sysState) {
                        sysEnterToQuitState();
                        break;
                    }

                    UtilOper.sleep(TIMER_MILL_SECOND);
                }

                break;
            }

            case AdxConstants.SERVICE_STATUS_QUIT: {

                if (timer != null) {
                    try {
                        timer.shutdownNow();
                    } catch (Exception e) {
                        log.error("stopping timer for dataloading: ", e);
                    }
                    log.info("timer for dataloading is stopped.");
                }

                /*
                 * 做各种资源回收操作: 1 释放请求Http通信服务, 保障系统不在接收请求 2 等待业务线程把队列中的消息处理完成
                 */
                if (srvHttpServer != null) {
                    try {
                        srvHttpServer.stop();
                    } catch (Exception e) {
                        log.error("stopping srvHttpServer: ", e);
                    }
                }
                if (adMonitorHttpServer != null) {
                    try {
                        adMonitorHttpServer.stop();
                    } catch (Exception e) {
                        log.error("stopping adMonitorHttpServer: ", e);
                    }
                }

                if (srvModule != null) {
                    try {
                        srvModule.closeModule();
                    } catch (Exception e) {
                        log.error("stopping srvModule: ", e);
                    }
                }

                ThreadPool.closePool();

                if (plat3MgrModule != null) {
                    try {
                        plat3MgrModule.closeTask();
                    } catch (Exception e) {
                        log.error("stopping plat3MgrModule: ", e);
                    }
                }

                trackingConsumer.shutdown();
                try {
                    closeTask();
                    UtilOper.sleep(100);
                } catch (Exception e) {
                    log.error("stopping sysMgrModule: ", e);
                }

                sysState = AdxConstants.SERVICE_STATUS_QUIT;
                sysInitStep = 0;
                break;
            }

            default: {
                log.error("{} received a wrong msgId = {}", taskName, msgId);
                break;
            }
        }
    }

    private void timerHandlerInit() {
        if (timerInInitState > 0) {
            timerInInitState--;
            return;
        }
        timerInInitState = 5;

        startTask();
    }

    public void timerHandlerWork() {

        /* 重新拉取数据库配置 */
        boolean isInitOK;
        try {
            isInitOK = sysConfigManager.updateDataFromDb();
        } catch (Exception e) {
            log.error(e.getMessage());
            isInitOK = false;
        }

        if (isInitOK) {
            log.info("read db config to Cache success!");
        } else {
            log.error("read db config to Cache fail!");
        }

        sysConfigManager.loadSpecialAdConfig();
    }

    @Override
    public boolean closeTask() {
        addMsg(new GMsg(Constants.MSG_ID_SYS_KILL, null));
        return true;
    }

    public int getSysStatus() {
        return sysState;
    }

    public void stop() {
        if (!running) {
            log.warn("GSysMgrModule is not running.");
            return;
        }
        running = false;
        Thread thread = getThreadByName(taskName);
        if (sysState != AdxConstants.SERVICE_STATUS_WORK && thread != null) {
            thread.interrupt();
        }

        addMsg(AdxConstants.SERVICE_STATUS_QUIT, null);
    }

    private Thread getThreadByName(String name) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (StringUtils.equals(thread.getName(), name)) {
                return thread;
            }
        }
        return null;
    }

    public SysConfigManager getSysConfigManager() {
        return sysConfigManager;
    }

    /**
     * 功能描述: 初始化Share项目中的Redis
     *   
     * @return true：初始化成功，false：初始化失败
     * [2018年7月17日]创建文件 by lh.qiu
     */
	private boolean initShareRedis() {
		// 获得配置文件
		String configFile = AdxSystem.sysMgrModule().getSysConfigManager().getConfigFile();
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
