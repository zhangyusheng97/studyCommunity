package com.second.community.event;

import com.alibaba.fastjson.JSONObject;
import com.second.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

//事件的生产者
@Component
public class EventProducer {
    @Autowired
    KafkaTemplate kafkaTemplate;

    //处理事件
    public void fireEvent(Event event){
        //将事件所包含的内容发布到指定的主题
        //以json的字符串形式传递
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
