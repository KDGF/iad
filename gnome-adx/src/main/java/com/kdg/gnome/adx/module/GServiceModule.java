package com.kdg.gnome.adx.module;

import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.share.task.GMsg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GServiceModule {

  private static final Logger log =LogManager.getLogger("ES_OUT_INFO");

  private long handlerMsgNum = 0;
  private int srvTaskNum;
  private List<GServiceTask> lstSrvTask = new ArrayList<GServiceTask>();

  public GServiceModule() {
  }

  public static void brokenSigh(GSessionInfo gSessionInfo) {
    // 会话进行不下去了，所以需要将会话从超时队列摘下来
    AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_BROCKEN, gSessionInfo);
  }

  public boolean startModule(int taskNum) {
    handlerMsgNum = 0;
    srvTaskNum = taskNum;
    for (int i = 0; i < srvTaskNum; i++) {
      GServiceTask srvTask = new GServiceTask("service-task-" + i);
      srvTask.startTask();

      lstSrvTask.add(srvTask);
    }
    return true;
  }

  public void addMsg(int msgId, Object objContext) {
    ++handlerMsgNum;

    int taskIdx = (int) (handlerMsgNum % srvTaskNum);
    if (taskIdx < 0) {
      taskIdx = 0 - taskIdx;
    }

    lstSrvTask.get(taskIdx).addMsg(new GMsg(msgId, objContext));
  }

  public void closeModule() {
    if (lstSrvTask != null) {
      for (GServiceTask srvTask : lstSrvTask) {
        srvTask.closeTask();
      }
      log.info("all service tasks are stopped.");
    }
  }
}
