package com.kdg.gnome.adx.share.dao;

import com.google.gson.Gson;

import java.util.List;

/**
 * Created by hbwang on 2017/12/12
 */
public class AdvertiseMaterial {

    public Campaign         campaign;       //投放计划
    public String      accountId;  //账户id
    public String      advertiserId;   //广告主id
    public String      campaignId ;    //计划id
    public List<Creative>   creatives;      // 素材

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
