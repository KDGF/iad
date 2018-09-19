package com.kdg.gnome.adx.monitor;

import com.google.gson.Gson;

/**
 * Created by hbwang on 2018/4/13
 */
public class NewFormatLog {

    public String   token;
    public String   event_date;

    // 查詢維度相關
    public String   plat_name;      //平台名称
    public String   plat_id;
    public String   media_id;        //媒體id
    public String   media_ad_id;    //媒体对外id
    public String   media_name;      //媒體名稱
    public String   media_ad_name;    //媒體廣告位名稱

    public String   package_name;
    public String   channel_id;
    public String   style_id;

    public String   ad_id;           //廣告位id
    public String   ad_name;         //廣告位名稱


    public String   os;             //系統類型
    public String   dev_type;        //設備類型
    public String   ad_size;         //廣告位尺寸
    public String   country;        //國家
    public String   province;       //省
    public String   city;           //市
    public String   ip;             //ip


    //反作弊标识
    public int      ad_nomal_type;

    //投放相关
    public String   adver_id;        //廣告主id
    public String   adver_name;      //廣告主名稱
    public String   cam_id;          //計劃id
    public String   cam_name;        //計劃名稱
    public String   crv_id;          //素材id
    public String   crv_name;        //素材名稱

    // 統計相關
    public int      uniq_req;        //唯一请求
    public int      adver_rec_req;    //广告主收到的请求
    public int      adver_rsp_req;    //广告主响应请求
    public int      adver_rsp_succ_req;    //广告主响应成功的请求

    public int      adver_imp;   //廣告主曝光
    public int      adver_clk;   //廣告主點擊

    public int      adver_arr;   //廣告主到達(落地頁)
    public int      adver2jump; //廣告主二跳(落地页效果交互)

    public int      register;   //註冊
    public int      active;     //激活
    public int      download;   //下載

    public int      dplink_succ; //deeplink吊起成功
    public int      dplink_fail; //deeplink吊起失敗

    // 價格相關
    public double   ori_cost;    //媒体价格(成本价/成交价）
    public double   deal_price;   //广告主结算价
    public double   bid_price;   //出價價格
    public double   wastage_price;  //  消耗

//    public String   ctr;        //ctr
//    public int      mediaSettleType;    //媒体结算类型(1.cpm; 2.cpc)
//    public int      adverSettleType;    //广告主结算类型(1.cpm; 2.cpc)

//    public int      addShopCart;    //添加購物車
//    public int      placeOrder; //下單
//    public int      collection; //收藏
//    public int      pay;        //付款

    private static Gson gson = new Gson();
    @Override
    public String toString() {
        try {
            return gson.toJson(this);
        } catch (Exception e) {
            return null;
        }

    }
}
