package com.kdg.gnome.adx.share.dao;

/**
 * Created by hbwang on 2017/12/12
 */
public class StatusConstants {

    public final static int CAMPAIGN_STATUS_NORMAL = 0;
    public final static int CAMPAIGN_STATUS_DELETE = 1;

    public final static int CAMPAIGN_FLAG_START = 1;
    public final static int CAMPAIGN_FLAG_SUSPEND = 2;

    public final static int CREATIVE_TYPE_BANNER = 1;
    public final static int CREATIVE_TYPE_VIDEO = 2;
    public final static int CREATIVE_TYPE_NATIVE = 3;

    public final static int CREATIVE_STATUS_NOMAL = 0;
    public final static int CREATIVE_STATUS_DELETE = 1;

    public final static int CREATIVE_FLAG_START = 1;
    public final static int CREATIVE_FLAG_SUSPEND = 2;

    public final static int CREATIVE_AUDIT_STATUS_PEND_WAIT = 0;
    public final static int CREATIVE_AUDIT_STATUS_PEND_PASS = 1;    //审核通过
    public final static int CREATIVE_AUDIT_STATUS_PEND_REFUSED = 2; //审核拒绝

    public final static int ADVER_STATUS_NORMAL = 0;
    public final static int ADVER_STATUS_DELETE = 1;

    public final static int ADVER_FLAG_START = 1;
    public final static int ADVER_FLAG_SUSPEND = 2;
}
