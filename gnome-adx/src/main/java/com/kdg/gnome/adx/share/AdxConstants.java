package com.kdg.gnome.adx.share;

import com.kdg.gnome.share.Constants;

public class AdxConstants {
    /**
     * 根据新协议列出的枚举值
     */
    // 行为类型
    public final static int ACTIONTYPE_UNLIMITED = 0; // 0 —— 无限制
    public final static int ACTIONTYPE_REDIRECT = 1; // 1 —— 跳转
    public final static int ACTIONTYPE_DOWNLOAD = 2; // 2 —— 下载
    public final static int ACTIONTYPE_BRAND = 3; // 3 —— 纯曝光
    public static final int ACTION_TYPE_CMVIDEO_ID = 4;

    // 设备类型 0 – 未知,1 – PC, 2– phone,3 -pad,4– tv
    public final static int DEVICETYPE_UNKNOWN = 0;
    public final static int DEVICETYPE_PC = 1;
    public final static int DEVICETYPE_PHONE = 2;
    public final static int DEVICETYPE_PAD = 3;
    public final static int DEVICETYPE_SMARTTV = 4;

    // 屏幕方向 0－纵向,1－横向
    public final static int ORIENTATION_VERTICAL = 0;
    public final static int ORIENTATION_HORIZONTAL = 1;

    // 运营商 0－未知，1－移动，2－联通，3－电信
    public final static int CARRIER_UNKNOWN = 0;
    public final static int CARRIER_CHINAMOBILE = 1;
    public final static int CARRIER_CHINAUNICOM = 2;
    public final static int CARRIER_CHINATELECOM = 3;

    // 网络连接类型 0-未知, 1-wifi, 2-2G, 3-3G, 4–4G, 5-5G
    public final static int CONNECTION_UNKNOWN = 0;
    public final static int CONNECTION_WIFI = 1;
    public final static int CONNECTION_2G = 2;
    public final static int CONNECTION_3G = 3;
    public final static int CONNECTION_4G = 4;
    public final static int CONNECTION_5G = 5;

    // 操作系统类型
    public final static String OS_ANDROID = "android";
    public final static String OS_IOS = "ios";
    public final static String OS_WINDOWS = "WindowsPhone";

    //创意类型（png/ipg）
    public final static String  MIME_IMAGE_JPEG = "image/jpeg";
    public final static String  MIME_IMAGE_JPG = "image/jpg";
    public final static String  MIME_IMAGE_PNG = "image/png";
    public final static String  MIME_IMAGE_GIF = "image/gif";
    public final static String  MIME_VIDEO_FLV = "video/x-flv";
    public final static String  MIME_VIDEO_MP4 = "video/mp4";
    public final static String  MIME_VIDEO_FLASH="application/x-shockwave-flash";
    public final static String  MIME_TYPE_UNKNOW="text/html";
    /*****************************************/

    public final static int SERVICE_STATUS_INIT = 1;
    public final static int SERVICE_STATUS_WORK = 2;
    public final static int SERVICE_STATUS_QUIT = 3;
    public final static int SERVICE_STATUS_PERIOD = 4;

    public final static int MSG_ID_SERVICE_ADX_AD_REQ = Constants.MSG_ID_SERVICE_START;
    public final static int MSG_ID_SERVICE_ADX_AD_RSP = Constants.MSG_ID_SERVICE_START + 1;
    public final static int MSG_ID_SERVICE_ADX_AD_TIMEOUT = Constants.MSG_ID_SERVICE_START + 2;
    public final static int MSG_ID_SERVICE_ADX_AD_BROCKEN = Constants.MSG_ID_SERVICE_START + 3;
    public final static int MSG_ID_SERVICE_ADX_IMPRESS = Constants.MSG_ID_SERVICE_START + 4;
    public final static int MSG_ID_SERVICE_ADX_CLICK = Constants.MSG_ID_SERVICE_START + 5;
    public final static int MSG_ID_SERVICE_ADX_404 = Constants.MSG_ID_SERVICE_START + 6;

    /* 广告打开类型 */
    public final static int ADUNIT_OPEN_TYPE_LANDING = 1;
    public final static int ADUNIT_OPEN_TYPE_DOWNLOAD = 2;
    public final static int ADUNIT_OPEN_TYPE_DEEPLINK = 3;



    public static final String  PRICE_MACROS = "${KDG_PRICE_AES}";
    public static final String  SETTLE_TYPE = "${BID_TYPE}";

    public static final int     SETTLE_TYPE_CPM = 0;
    public static final int     SETTLE_TYPE_CPC = 1;
    public static final int     SETTLE_TYPE_CPD = 2;
    public static final int     SETTLE_TYPE_CPA = 3;

}
