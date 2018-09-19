package com.kdg.gnome.adx.scheduler;

import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.share.AdxConstants;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Created by hbwang on 1/6/2016.
 */
public class DataLoadingTask implements Runnable {
  private static Logger log = LogManager.getLogger("ES_OUT_INFO");
  @Override
  public void run() {
    try {
      if (AdxSystem.sysMgrModule().getSysStatus() == AdxConstants.SERVICE_STATUS_WORK) {
        log.info("loading data to cache，starting...");
        AdxSystem.sysMgrModule().timerHandlerWork();
        log.info("loading data to cache，finished...");

        AdxSystem.sysMgrModule().get3PlatMgrModule().loadEffectiveMediaPlats();
      } else {
        log.error("loading data to cache，but SysMgrModule().sysState != AdxConstants.SERVICE_STATUS_WORK");
      }
    } catch (Throwable t) {
      log.error("[FATAL ERROR] data loading task encounters exception: ", t);
    }
  }
}
