package com.kdg.gnome.adx.share.dao;

import java.util.List;

/**
 * Created by hbwang on 2017/12/12
 * 投放素材表  与 投放计划表关联
 */
public class Creative {

    public String  id;
    public String   name;   //创意名称
    public String   filePath;   //素材原地址
    public String   fileType;   //素材文件类型

    public int      style;  //素材的类型

    public int      width;  //宽
    public int      height; //高
    public int      type;   //素材类型（1.banner 2.video 3.native）
    public int      linkType;   //素材跳转类型
    public int      size;   //素材大小
    public int      durition; //视频播放时长
    public String   landing_page;   //落地页地址
    public String   deepLink;
    public String   templateId; //创意模板id
    public TemplateContent   templateContent;    //模板创意内容 原生类型素材
    public String   category;   //类目分类
    public int      status;     //状态（0.正常 1.删除）
    public int      flag;   //投放状态 （1.启动 2.暂停）

    public int      auditStatus;    //审核状态（0.待审核 1.审核通过 2.审核拒绝）

    public String   remark; //备注


    public List<String> impMonitor;     //曝光监测
    public String clkMonitor;     //点击监测 包括302点击监测

    public Long     updateTime; //更新时间


    public static class TemplateContent {
        public int      styleid;

        public Image    image;
        public String   title;
        public String   desc;
        public String   icon;
        public Video    video;

        public static class Video {
            public int      w;
            public int      h;
            public String   url;
            public int      duration;
            public String   type;
        }
        public static class Image {
            public int      w;
            public int      h;

            public Context  context;
            public static class Context {
                public String   img;
                public String   img1;
                public String   img2;
                public String   img3;
            }
            public String   type;
//            public List<String> urls;
        }
    }


}
