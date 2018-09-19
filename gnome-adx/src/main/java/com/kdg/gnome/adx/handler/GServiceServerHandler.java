package com.kdg.gnome.adx.handler;

import com.kdg.gnome.adx.main.AdxSystem;
import com.kdg.gnome.adx.share.AdxConstants;
import com.kdg.gnome.adx.share.GSessionInfo;
import com.kdg.gnome.share.Constants;
import com.kdg.gnome.share.OriginRequest;
import com.kdg.gnome.util.IpUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.httpkit.server.*;

//import com.kdg.gnome.share.MetricsHandle;

public class GServiceServerHandler implements IHandler {

  private static final Logger log = LogManager.getLogger("ES_OUT_INFO");

  public GServiceServerHandler() {
  }

  public void close(int timeoutMs) {
    log.debug("close.");
  }

  public void handle(AsyncChannel channel, Frame frame) {
    log.error("handle channel and frame: not support");
  }

  public void handle(HttpRequest request, final RespCallback callback) {

    OriginRequest originReq = new OriginRequest();
    originReq.url = request.uri;
    originReq.headers = request.getHeaders();

    if (request.method.KEY == request.method.GET.KEY) {
      originReq.type = Constants.HTTP_REQ_TYPE_GET;
      originReq.getQuery = request.queryString;
    } else if (request.method.KEY == request.method.POST.KEY) {
      originReq.type = Constants.HTTP_REQ_TYPE_POST;
      originReq.postBody = request.getPostBody();
      originReq.getQuery = request.queryString;
    }

    GSessionInfo sessInfo = GSessionInfo.getNewSession(originReq);
    sessInfo.callback = callback;

    //直接请求域名 作为心跳监测
    if (request.method.KEY == request.method.GET.KEY && (StringUtils.isBlank(request.uri) || StringUtils.equals("/", request.uri))) {
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_BROCKEN, sessInfo);
      return;
    }
    if (! StringUtils.equals(originReq.url, "/request")) {
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_BROCKEN, sessInfo);
      return;
    }

    sessInfo.reqRealIp = (String) originReq.headers.get("x-real-ip");

    Object kdgUa = originReq.headers.get("X-Real-UA");
    sessInfo.userAgent = (String)kdgUa;
    try {
      sessInfo.userAgent = kdgUa != null ? (String) kdgUa : null;
    } catch (Exception e) {
      log.error(e.getMessage());
    }


    if(StringUtils.isNotBlank(sessInfo.reqRealIp) && IpUtil.checkIpBlackList( sessInfo.reqRealIp)){
      log.debug("request ip in Blacklist ,IP is --> {}",sessInfo.reqRealIp);
      AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_BROCKEN, sessInfo);
      return;
    }
    log.debug("request ip shipped by nginx: {} ", sessInfo.reqRealIp);
    log.debug("remote client: {}", request.getRemoteAddr());
    AdxSystem.srvModule().addMsg(AdxConstants.MSG_ID_SERVICE_ADX_AD_REQ, sessInfo);

  }

  public void clientClose(AsyncChannel channel, int status) {
    log.debug("handle channel and status: client has closed the channel.");
  }

}