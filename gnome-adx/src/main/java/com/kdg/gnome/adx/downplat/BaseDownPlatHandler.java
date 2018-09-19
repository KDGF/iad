package com.kdg.gnome.adx.downplat;

import com.kdg.gnome.adx.ad.KdgProtocol;
import com.kdg.gnome.adx.share.GSessionInfo;

import java.nio.ByteBuffer;

public abstract class BaseDownPlatHandler {

  public abstract boolean handlerReq(GSessionInfo sessInfo);

  public abstract ByteBuffer handlerRsp(KdgProtocol.ResponseBean responseBean, GSessionInfo sessInfo);


}

