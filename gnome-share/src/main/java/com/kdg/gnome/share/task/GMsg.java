package com.kdg.gnome.share.task;

public class GMsg {
  public int msgId;          /* 消息事件id  */
  public Object objContext;     /* 消息内容 */

  public GMsg(int msgId, Object objContext) {
    this.msgId = msgId;
    this.objContext = objContext;
  }
}
