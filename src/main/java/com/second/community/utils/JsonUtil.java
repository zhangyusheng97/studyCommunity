package com.second.community.utils;


import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public class JsonUtil {
    public static String getJsonString(int code , String msg , Map<String,Object> map){
        JSONObject json = new JSONObject();
        json.put("code",code);
        json.put("msg",msg);
        if (map != null){
            for (String key : map.keySet()){
                json.put(key,map.get(key));
            }
        }
        return json.toJSONString();
    }
    //对该方法进行重载
    public static String getJsonString(int code , String msg){
        JSONObject json = new JSONObject();
        json.put("code",code);
        json.put("msg",msg);
        return json.toJSONString();
    }

    public static String getJsonString(int code){
        JSONObject json = new JSONObject();
        json.put("code",code);
        return json.toJSONString();
    }
}
