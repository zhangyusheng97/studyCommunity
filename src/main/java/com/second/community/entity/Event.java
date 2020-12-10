package com.second.community.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {
    //事件主题
    private String topic;
    //事件的触发者
    private int userId;
    //事件发生在哪个实体上？是评论，点赞，还是关注
    private int entityType;
    private int entityId;
    //实体的作者
    private int entityUserId;
    //必要时存储其他内容
    private Map<String,Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    //进行一个简单的改造，方便数据的处理
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    //使用key ，value的形式，更加方便
    public Event setData(String key , Object value) {
        this.data.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "Event{" +
                "topic='" + topic + '\'' +
                ", userId=" + userId +
                ", entityType=" + entityType +
                ", entityId=" + entityId +
                ", entityUserId=" + entityUserId +
                ", data=" + data +
                '}';
    }
}
