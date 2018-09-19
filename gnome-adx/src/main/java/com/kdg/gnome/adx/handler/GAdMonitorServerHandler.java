package com.kdg.gnome.adx.handler;

import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.share.Constants;
//import com.kdg.gnome.share.MetricsHandle;
import com.kdg.gnome.share.OriginRequest;
import org.apache.commons.lang3.StringUtils;
import org.httpkit.server.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class GAdMonitorServerHandler implements IHandler {

  private static final Logger log = LogManager.getLogger("ES_OUT_INFO");

  public GAdMonitorServerHandler() {}

  public void close(int timeoutMs) {
    log.debug("close.");
  }

  public void handle(AsyncChannel channel, Frame frame) {
    log.debug("handle channel and frame");
  }

  public void handle(HttpRequest request, final RespCallback callback) {

    if (request == null || callback == null) {
      log.error("request == null || callback == null");
      return;
    }

    OriginRequest originRequest = new OriginRequest();
    if (request.method.KEY == request.method.GET.KEY) {
      originRequest.type = Constants.HTTP_REQ_TYPE_GET;
    } else if (request.method.KEY == request.method.POST.KEY) {
      originRequest.type = Constants.HTTP_REQ_TYPE_POST;
    }

    originRequest.url = request.uri;
    originRequest.getQuery = request.queryString;
    originRequest.headers = request.getHeaders();
    GSessionInfo sessionInfo = new GSessionInfo();
    sessionInfo.originReq = originRequest;
    sessionInfo.callback = callback;

    if ((StringUtils.equals("/", request.uri) ||StringUtils.isBlank(originRequest.url)) && StringUtils.isBlank(originRequest.getQuery)) {
      sessionInfo.isMonitorCheck = true;
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_404, sessionInfo);
      return;
    }

    Object userAgent = originRequest.headers.get("user-agent");
    try {
      sessionInfo.userAgent = userAgent != null ? String.valueOf(userAgent) : null;
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    sessionInfo.reqRealIp = getClientIP(request);
    log.debug("the header is:{}", originRequest.headers);
    log.debug("request ip shipped by nginx: {}", sessionInfo.reqRealIp);

    log.info("a monitor report is here: {}?{}", originRequest.url, originRequest.getQuery);

    if (StringUtils.equals(originRequest.url, "/i")) {
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_IMPRESS, sessionInfo);
    } else if (StringUtils.equals(originRequest.url, "/c")) {
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_CLICK, sessionInfo);
    } else {
      log.error("404 , request url = {}?{}", originRequest.url, originRequest.getQuery);
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_404, sessionInfo);
    }
  }

  private static  final String STR_COMMA = ",";
  private static  final String WORD_EMPTY = "";


  //获取参数IP
  private String getClientIP(HttpRequest request) {
    String ip = null;
    String ipTmp;

    ipTmp = (String) request.getHeaders().get("X-Forwarded-For");
    if (ipTmp != null && !"unknown".equals(ipTmp)) {
      ip = ipTmp.split(STR_COMMA)[0];
    }

    if (StringUtils.isBlank(ip)) {
      ipTmp = (String) request.getHeaders().get("X-Real-IP");
      if (ipTmp != null && !"unknown".equals(ipTmp)) {
        ip = ipTmp.split(STR_COMMA)[0];
      }
    }
    if (StringUtils.isBlank(ip)) {
      ipTmp = request.getRemoteAddr();
      if (ipTmp != null && !"unknown".equals(ipTmp)) {
        ip = ipTmp.split(STR_COMMA)[0];
      }
    }
    if (StringUtils.isBlank(ip)) {
      ip = WORD_EMPTY;
    }

    return ip;
  }

  public void clientClose(AsyncChannel channel, int status) {
    log.debug("handle channel and status");
  }
}
