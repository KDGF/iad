package com.kdg.gnome.adx.share.dao;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

public class AdxDbInfo {

    private static Gson gson = new Gson();

    public String toString() {
        return gson.toJson(this);
    }

    public Map<String, AdvertiserInfo> advertiserInfoMap;   //广告主状态表，用于筛选创意时先判断广告主状态，关闭的不选
    public List<AdvertiseMaterial> advertiseMaterials;  //素材

    public Map<String, List<PmpConfig>> pmpMap;    //pmp Config  pdb/pd相关

    public Map<String, Integer> adverPriority;  //广告主优先级
    public Map<String,Creative> creativeMap;
}
