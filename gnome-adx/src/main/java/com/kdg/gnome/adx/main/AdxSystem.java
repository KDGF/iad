package com.kdg.gnome.adx.main;

import com.kdg.gnome.adx.module.G3PlatMgrModule;
import com.kdg.gnome.adx.module.GServiceModule;
import com.kdg.gnome.adx.module.GSysMgrModule;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.UtilOper;
import com.kdg.gnome.share.task.AES;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class AdxSystem {
  private static final Logger log = LogManager.getLogger("ES_OUT_INFO");

  public static GSysMgrModule sysMgrTask;


  public static GSysMgrModule sysMgrModule() {
    return sysMgrTask;
  }

  public static GServiceModule srvModule() {
    return sysMgrTask.getSrvModule();
  }

  public static G3PlatMgrModule plat3MgrModule() {
    return sysMgrTask.get3PlatMgrModule();
  }

  public static void main(String[] args) throws Exception {
    Thread.currentThread().setName("SystemMainTask");


    String path = Thread.currentThread().getContextClassLoader().getResource("adx.conf").getPath();
    sysMgrTask = new GSysMgrModule(path);
    sysMgrTask.addMsg(Constants.MSG_ID_SYS_INIT, null);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        sysMgrModule().stop();
        log.info("ADX is stopping");
        while (AdxConstants.SERVICE_STATUS_QUIT != sysMgrTask.getSysStatus()) {
          UtilOper.sleep(5);
        }
        log.info("ADX is stopped.");
      }
    });
    //首次调用AES模块 系统加载加解密模块需耗时1s 会影响业务卡顿  在系统启动时加载 规避对业务的影响
    Class.forName(AES.class.getName());

    while (true) {
      if (AdxConstants.SERVICE_STATUS_QUIT == sysMgrTask.getSysStatus()) {
        break;
      }

      UtilOper.sleep(5000); // 周期性拉取数据库到本地缓存
      // log.debug("main...thead...sleep...5...second.");
    }

    log.info("AdxSystem quit normally!");
    return;
  }
}
