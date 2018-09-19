package com.kdg.gnome.adx.share;

import com.google.gson.Gson;
import com.kdg.gnome.adx.share.dao.AdxDbInfo;
import com.kdg.gnome.adx.utils.CacheUtils;
import com.kdg.gnome.share.UtilOper;
import com.kdg.gnome.share.db.DBConnect;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by WangHaibo on 2016/11/9.
 */
public class SysConfigManager {
    private static final Logger log = LogManager.getLogger("ES_OUT_INFO");

    private String configFile;
    public boolean isConfigOK;

    private static Gson gson = new Gson();

    public static Map<String, String>  platTokenMap = new HashedMap();
    {
        Map<String, String> newEffectiveUpPlats = new HashMap<>();
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("platToken.json");
        if (inputStream == null) {
            log.error("Error!!!! loading platToken.json to cache failed");
        }
        try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
            byte[] bytes = new byte[2048];
            int n;
            StringBuilder sb = new StringBuilder(2048);
            while ((n = bis.read(bytes)) != -1) {
                sb.append(new String(bytes, 0, n, "utf-8"));
            }
            newEffectiveUpPlats = gson.fromJson(sb.toString(), newEffectiveUpPlats.getClass());
        } catch (Exception e) {
            log.error("Error!!!! loading platToken.json to cache failed: ", e);
        }

        if (newEffectiveUpPlats == null || newEffectiveUpPlats.isEmpty()) {
            log.error("Error!!!!loading platToken.json to cache failed: newEffectiveUpPlats == " +
                    "null || newEffectiveUpPlats.isEmpty()");
        } else {
            platTokenMap = newEffectiveUpPlats;
        }
    }

    public void setConfigFile(String file) {
        configFile = file;
    }
    public String getConfigFile() {
        return configFile;
    }

    // 连接数据库使用
    private boolean isUpdateDBOK = false;
    private DBConnect dbConn;
    private FetchInfoFromDb fetchInfoFromDb;

    public DBConf              dbConfig;
    public ServerConfig        serverConfig;

    // ------------------------------------------------------------------------------------ adx杂项配置
    public int  dbLoadPeriod = 10;               // 定期拉取时间 默认10
//    public int  invalidTime = 43200;    //投放后点击和曝光的失效时间 单位s
    public int  timeOut = 500;          //收到请求后500ms不再进行投放
    public double   defaultCtr;     //素材默认的ctr
    public String   adxToken;       //adx平台先默认一个token

    public final boolean loadSpecialAdConfig() {
        dbLoadPeriod = UtilOper.getIntValue(configFile, "dbLoadPeriod", 10);
//        invalidTime = UtilOper.getIntValue(configFile, "invalid_time", 43200);
        defaultCtr = UtilOper.getDoubleValue(configFile, "default_ctr", 0.01);
        adxToken = UtilOper.getStringValue(configFile, "adxplat_token", "bed5bdea10da60dc");
        timeOut = UtilOper.getIntValue(configFile, "time_out", 500);
        return true;
    }

    public DBConf initDbConf() {
        DBConf dbConf = new DBConf();
        boolean isDbConfOk = false;
//        log.info("configFile : " + configFile);
        String drive = UtilOper.getStringValue(configFile, "db_drive", "");
//        log.info("drive : " + drive);
        isDbConfOk = dbConf.setDbDrive(UtilOper.getStringValue(configFile, "db_drive", ""));
        if (!isDbConfOk) {
            log.error("db　config error : dbDriver error....");
            return null;
        }

        isDbConfOk = dbConf.setDbIp(UtilOper.getStringValue(configFile, "db_ip", ""));
        if (!isDbConfOk) {
            log.error("db　config error : dbIp error....");
            return null;
        }
        isDbConfOk = dbConf.setDbPort(UtilOper.getLongValue(configFile, "db_port", 0L));
        if (!isDbConfOk) {
            log.error("db　config error : dbPort error....");
            return null;
        }
        isDbConfOk = dbConf.setDbName(UtilOper.getStringValue(configFile, "db_name", ""));
        if (!isDbConfOk) {
            log.error("db　config error : dbName error....");
            return null;
        }
        isDbConfOk = dbConf.setDbUserName(UtilOper.getStringValue(configFile, "db_userName", ""));
        if (!isDbConfOk) {
            log.error("db　config error : dbUserName error....");
            return null;
        }
        isDbConfOk = dbConf.setDbPasswd(UtilOper.getStringValue(configFile, "db_passwd", ""));
        if (!isDbConfOk) {
            log.error("db　config error : dbPasswd error....");
//            return null;
        }

        return dbConf;
    }

    public ServerConfig initServerConf() {

        ServerConfig serverConf = new ServerConfig();
        serverConf.setSrvIp(UtilOper.getStringValue(configFile, "service_server_ip", ""));
        if (StringUtils.isBlank(serverConf.getSrvIp())) {
            log.error("config: service_server_ip error.");
            return null;
        }

        serverConf.setSrvPort(UtilOper.getIntValue(configFile, "service_server_port", 0));
        if (0 == serverConf.getSrvPort()) {
            log.error("config: service_server_port error.");
            return null;
        }

        serverConf.setAdMonitorIp(UtilOper.getStringValue(configFile, "ad_monitor_ip", ""));
        if (StringUtils.isBlank(serverConf.getAdMonitorIp())) {
            log.error("config: ad_monitor_ip error.");
            return null;
        }

        serverConf.setAdMonitorPort(UtilOper.getIntValue(configFile, "ad_monitor_port", 0));
        if (0 == serverConf.getadMonitorPort()) {
            log.error("config: ad_monitor_ip error.");
            return null;
        }

        serverConf.setSrvTaskNum(UtilOper.getIntValue(configFile, "service_task_num", 0));
        if (0 == serverConf.getSrvTaskNum()) {
            log.error("config: service_task_num error.");
            return null;
        }
        serverConf.setServerIoThreads(UtilOper.getIntValue(configFile, "serverIoThreads", 16));
        if (0 == serverConf.getServerIoThreads()) {
            log.error("config: serverIoThreads error.");
            return null;
        }
        serverConf.setOvertimeQueueLen(UtilOper.getIntValue(configFile, "overtime_queue_len", 0));
        if (0 == serverConf.getOvertimeQueueLen()) {
            log.error("config: overtime_queue_len error.");
            return null;
        }

        serverConf.setAdxImpressUrlPrefix(UtilOper.getStringValue(configFile, "adx_impress_url", ""));
        if (StringUtils.isBlank(serverConf.getAdxImpressUrlPrefix())) {
            log.error("config: adx_impress_url is blank");
            return null;
        }

        serverConf.setAdxClickUrlPrefix(UtilOper.getStringValue(configFile, "adx_click_url", ""));
        if (StringUtils.isBlank(serverConf.getAdxImpressUrlPrefix())) {
            log.error("config: adx_click_url is blank");
            return null;
        }


        String aesToken = UtilOper.getStringValue(configFile, "aestoken", "");
        if (StringUtils.isBlank(aesToken)) {
            log.error("WARN. aesToken is BLANK. ");
        } else  {
            serverConf.aesToken = aesToken;
        }

        String kafkaZookeepers = UtilOper.getStringValue(configFile, "kafka_zookeeper", "");
        if (StringUtils.isBlank(kafkaZookeepers) ) {
            log.error("config. kafka zookeeper is null.");
            return null;
        }
        serverConf.kafkaZookeepers = kafkaZookeepers;
        String kafkaTrackingTopic = UtilOper.getStringValue(configFile, "kafka_tracking_topic", "");
        if (StringUtils.isBlank(kafkaTrackingTopic)) {
            log.error("config. kafka tracking topic is null.");
            return null;
        }
        serverConf.kafkaTrackingTopic = kafkaTrackingTopic;
        serverConf.kafkaTrackingThreads = UtilOper.getIntValue(configFile, "kafka_tracking_threads", 2);


        return serverConf;
    }

    // 初始化 连接数据库
    public boolean connDataBase() {
        dbConn = new DBConnect(dbConfig.getDbDrive(), dbConfig.getDbIp(), dbConfig.getDbPort(), dbConfig.getDbUserName(), dbConfig.getDbpasswd());
        fetchInfoFromDb = new FetchInfoFromDb();
        if (dbConn != null && fetchInfoFromDb != null) {
            return updateDataFromDb();
        } else {
            return false;
        }

    }

    // 更新数据库中的数据
    public boolean updateDataFromDb() {

        try {
            if (!dbConn.isConnOk()) {
                log.debug("dbConn.isConnOk() == false, retrieving a new connection...");
                dbConn.setConnected(dbConn.getConnection());
                if (!dbConn.isConnOk()) {
                    log.debug("dbConn.getConnection(); then dbConn.isConnOk() == false");
                    return false;
                }
                log.debug("dbConn.isConnOk() == true, retrieved a new connection...");
            }

            AdxDbInfo newDb = fetchInfoFromDb.setConn(dbConn.getConn()).setDbName(dbConfig.getDbName()).fetchInfo();
            fetchInfoFromDb.releaseStatement();
            if (newDb == null) {
                log.error("db may be closed or crashed. Just close it and reconnect it on next round...");
                dbConn.closeConn();
                dbConn.setConnected(false);
                return false;
            }
            //db = newDb;
            CacheUtils.setDb(newDb);
            dbConn.commit();

            isUpdateDBOK = true;

            log.info("init dbConfig success..., and continue next.");
            //sysInitStep++;
        } catch (Exception e) {
            log.error("init dbConfig exception..., and continue after a while.");
            return false;
        }

        return isUpdateDBOK;
    }

    /* 初始化 系统配置数据 */
    public boolean initConfig() {
        isConfigOK = false;

        // 初始化 配置文件数据
        dbConfig = initDbConf();
        // 连接 数据库
        if (!connDataBase()) {
            log.error("config init error : updateData from db error");
            return false;
        }
        // 初始化 其他配置
        serverConfig    = initServerConf();
        if (serverConfig == null) {
            log.error("config error : serverConfig error...");
            return false;
        }

        // 拉取特殊广告配置
        if (!loadSpecialAdConfig()) {
            log.error("loadSpecialAdConfig error ");
            return false;
        }
        isConfigOK = true;
        return isConfigOK;
    }

    public static class DBConf {
        // 配置文件中的数据  连接数据库需要
        private String dbDrive;
        private String dbIp;
        private Long   dbPort;
        private String dbName;
        private String dbUserName;
        private String dbPasswd;

        public boolean setDbDrive (String dbDrive) {
            this.dbDrive = dbDrive;
            return StringUtils.isBlank(this.dbDrive) ? false : true;
        }
        public String getDbDrive () {
            return dbDrive;
        }
        public boolean setDbIp(String ip) {
            this.dbIp = ip;
            return StringUtils.isBlank(this.dbIp) ? false : true;
        }
        public String getDbIp() {
            return dbIp;
        }

        public boolean setDbPort (Long port) {
            this.dbPort = port;
            return (this.dbPort == 0) ? false : true;
        }
        public Long getDbPort() {
            return dbPort;
        }

        public boolean setDbName(String dbName) {
            this.dbName = dbName;
            return StringUtils.isBlank(this.dbName) ? false : true;
        }
        public String getDbName() {
            return dbName;
        }

        public boolean setDbUserName(String userName) {
            this.dbUserName = userName;
            return StringUtils.isBlank(this.dbUserName) ? false : true;
        }
        public String getDbUserName() {
            return dbUserName;
        }

        public boolean setDbPasswd(String passwd) {
            this.dbPasswd = passwd;
            return StringUtils.isBlank(this.dbPasswd) ? false : true;
        }
        public String getDbpasswd() {
            return dbPasswd;
        }
    }

    public static class ServerConfig {
        // adx服务ip、端口--------------------------- 从配置文件
        private String srvIp;
        private int    srvPort;
//    public  String srvPortStr;

        private String adMonitorIp;             // 服务监控相关
        private int    adMonitorPort;


        private int    srvTaskNum;
        private int    serverIoThreads;
        private int    overtimeQueueLen;
        private String adxImpressUrlPrefix;
//        private String adxImpressUrlPrefixWithHttps;
        private String adxClickUrlPrefix;
//        private String adxClickUrlPrefixWithHttps;
        private String adxInstallUrlPrefix;
//        private String adxInstallUrlPrefixWithHttps;

//        public String   adxWinUrl;
        public String   aesToken;

//        public String   kafkaSlaves;
//        public String   kafkaMonitorTopic;
//        public String   kafkaReqTopic;
//        public int      kafkaMonitorThreads;

        public String   kafkaZookeepers;
//        public String   groupId;
        public String   kafkaTrackingTopic;
        public int      kafkaTrackingThreads;


        public void setSrvIp (String ip) {
            this.srvIp = ip;
        }
        public String getSrvIp() {
            return srvIp;
        }
        public void setSrvPort(int port) {
            this.srvPort = port;
        }
        public int getSrvPort() {
            return srvPort;
        }

        public void setSrvTaskNum(int taskNum) {
            this.srvTaskNum = taskNum;
        }
        public int getSrvTaskNum() {
            return srvTaskNum;
        }
        public void setServerIoThreads(int threads) {
            this.serverIoThreads = threads;
        }
        public int getServerIoThreads() {
            return serverIoThreads;
        }
        public void setOvertimeQueueLen(int overtimeQueueLen) {
            this.overtimeQueueLen = overtimeQueueLen;
        }
        public int getOvertimeQueueLen() {
            return overtimeQueueLen;
        }
        public void setAdxImpressUrlPrefix(String impressUrlPrefix) {
            this.adxImpressUrlPrefix = impressUrlPrefix;
        }
        public String getAdxImpressUrlPrefix() {
            return adxImpressUrlPrefix;
        }
//        public void setAdxImpressUrlPrefixWithHttps(String impressUrlPrefixWithHttps){
//            this.adxImpressUrlPrefixWithHttps = impressUrlPrefixWithHttps;
//        }
//        public String getAdxImpressUrlPrefixWithHttps() {
//            return adxImpressUrlPrefixWithHttps;
//        }
        public void setAdxClickUrlPrefix(String clickUrlPrefix) {
            this.adxClickUrlPrefix = clickUrlPrefix;
        }
        public String getAdxClickUrlPrefix() {
            return adxClickUrlPrefix;
        }
//        public void setAdxClickUrlPrefixWithHttps(String clickUrlPrefixWithHttps) {
//            this.adxClickUrlPrefixWithHttps = clickUrlPrefixWithHttps;
//        }
//        public String getAdxClickUrlPrefixWithHttps() {
//            return adxClickUrlPrefixWithHttps;
//        }
        public void setAdxInstallUrlPrefix(String installUrlPrefix) {
            this.adxInstallUrlPrefix = installUrlPrefix;
        }
        public String getAdxInstallUrlPrefix() {
            return adxInstallUrlPrefix;
        }
//        public void setAdxInstallUrlPrefixWithHttps(String installUrlPrefixWithHttps) {
//            this.adxInstallUrlPrefixWithHttps = installUrlPrefixWithHttps;
//        }
//        public String getAdxInstallUrlPrefixWithHttps() {
//            return adxInstallUrlPrefixWithHttps;
//        }
        public void setAdMonitorIp(String ip) {
            this.adMonitorIp = ip;
        }
        public String getAdMonitorIp() {
            return adMonitorIp;
        }
        public void setAdMonitorPort(int port) {
            this.adMonitorPort = port;
        }
        public int getadMonitorPort() {
            return adMonitorPort;
        }

    }

    public static void main (String[] args) {


        SysConfigManager s = new SysConfigManager();
        s.configFile = "H://ORI//m//gnome-adx//src//main//resources//adx.conf";
        s.initConfig();
//        System.out.println(s);
    }
}
