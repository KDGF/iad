package com.kdg.gnome.adx.ad;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.kdg.gnome.adx.share.GSessionInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hbwang on 2017/12/9
 * 1.request.imp.native.image  改为数组
 * 2.Bid Response加入price、adm属性
 * 3.取消landpage字段，对于点击跳转地址，统一使用clickthrough字段；
 */
public class KdgProtocol {

    public static class RequestBean {
        public String id;
        public boolean istest;//否
        public Integer secure; //响应是否https 0 不需要；1 需要

        public List<Impression> imp;    // 广告位对象数组 至少一个

        public Site site;   //针对网页流量
        public App app;    //针对移动app流量
        public Device device; //设备对象
        public Geo geo;    //地域对象
        public User user;   //用户对象

        public static class Impression {
            public String id; //imp的序号，1、2、3...
            public String channelid;    // 广告位所属频道id
            public String tagid;  //广告位id
            public String adunit;
            public Integer bidfloor;
            public Integer w;
            public Integer h;
            public List<Banner> banner;
            public List<Video> video;
            @SerializedName("native")
            public List<Native> native$;
            public Pmp  pmp;
            public List<String> opentype;   //广告打开类型

            public static class Banner {
                public Integer w;
                public Integer h;
                public List<String> mimes;  //支持的mime类型
            }

            public static class Video {
                public Integer w;
                public Integer h;

                public Integer maxduration;    //最大播放时长 秒
                public Integer minduration;
                public Integer diffduration;   //允许播放时长的误差值 ms

                public Integer protocol;       //视频支持的协议
                public List<String> mimes;  //支持的mime类型
            }

            public static class Native {
                public List<Image> image;
                public Icon icon;
                public Video video;

                public Integer title;  //允许的标题长度
                public Integer description;

                public static class Image {
                    public Integer w;
                    public Integer h;
                    public List<String> mimes;
                }

                public static class Icon {
                    public Integer w;
                    public Integer h;
                    public List<String> mimes;
                }

                public static class Video {
                    public String ratio; //4:3 或 16：9
                    public List<String> mimes;
                    public Integer w;
                    public Integer h;
                }

            }

            public static class Pmp {
                public List<Deal> deals;
                public static class Deal {
                    public String   id; //deal id
                    public Float    price;  //该deal的结算价格
                    public Integer  type;//交易类型，0：PD，1：PDB，默认为0
                }


            }

        }

        public static class Site {
            public String pageurl;    //当前页的url;
            public String referrer;   //当前页的前导url
            public String cat;        //网站所属类别
            public Content content;    //媒体当前展示内容
        }

        public static class App {
            public String id;
            public String bundle;     //包名
            public String name;
            public String cat;
            public Content content;
        }

        public static class Content {
            public String title;  //标题 如：指环王
            public String channel;    //当前频道/栏目/页面信息
            public List<String> keyword;//当前页面的关键词关键字
        }

        public static class Device {
            public String ua;
            public String ip;
            public Integer devicetype;
            public String make;   //设备制造商
            public String model;  //设备型号
            public String os;
            public String osv;
            public Integer w;
            public Integer h;
            public Integer carrier;    //运营商
            public Integer connectiontype; //网络连接类型
            public String ifa;    //
            public String imeisha1;
            public String imeimd5;
            public String imeiplain;
            public String aidsha1;    //android id
            public String aidmd5;
            public String aidplain;
            public String macsha1;
            public String macmd5;
            public String macplain;
            public String openudidsha1;
            public String openudidmd5;
            public String openudidplain;
        }

        public static class Geo {
            public float lat;    //纬度
            public float lon;    //经度
            public String city;
        }

        public static class User {
            public String id;
            public String gender;//性别 male男。female 女 unknow未知
            public String minage;//最小年龄
            public String maxage;//最大年龄
            public List<String> tags; //平台人群标签 [001, 101]
            public String ext;
        }

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

    public static class ResponseBean {
        public String id; //对应request的id

        public List<Seat> seatbid;    //DSP席位
        public String buyerid;

        public static class Seat {
            public List<Bid> bids;

            public static class Bid {
                public String id;
                public String impid;
                public String dealid;

                public Integer  price;   //CPM出价 分
                public String   adm;    //DSP自己渲染的代码片段
                public String   opentype;
                public String   adverid;
                public String   crid;   //DSP提供的创意id
                public Integer  styleid;   //素材类型

                public Banner banner;
                public Video video;
                @SerializedName("native")
                public Native native$;

                public String clickthrough;   //点击跳转地址 与downloadurl、dplurl互斥---------->落地页
                public List<String> trackurls;  //曝光监测
                public List<String> clicktracking;
                public String       downloadurl;
                public String       bundle;
                public String       appname;
                public String       dplurl;


                public static class Banner {
                    public String   curl;   //创意地址
                    public Integer  w;
                    public Integer  h;
                }

                public static class Native {
                    public String title;
                    public String description;

                    public List<Image>  image;
                    public Icon         icon;
                    public Video        video;

                    public static class Image {
                        public Integer w;
                        public Integer h;
                        public String url;
                    }

                    public static class Icon {
                        public String w;
                        public String h;
                        public String url;
                    }

                    public static class Video {
                        public Integer w;
                        public Integer h;
                        public String url;
                        public Float    size;   //视频大小
                    }
                }

                public static class Video {
                    public String   curl;   //创意地址
                    public Integer  w;
                    public Integer  h;
                    public Integer  duration;
                }
            }
        }

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

    public static ResponseBean ErrorResponse(GSessionInfo sessionInfo) {
        ResponseBean responseBean = new ResponseBean();

        if (sessionInfo.requestBean == null) {
            return null;
        }

        responseBean.id = sessionInfo.requestBean.id;
        responseBean.seatbid = new ArrayList<>();
        responseBean.seatbid.add(new ResponseBean.Seat());

        return responseBean;
    }
}
