package com.second.community.event;

import com.alibaba.fastjson.JSONObject;
import com.second.community.entity.DiscussPost;
import com.second.community.entity.Event;
import com.second.community.entity.Message;
import com.second.community.service.DiscussPostService;
import com.second.community.service.ElasticSearchService;
import com.second.community.service.MessageService;
import com.second.community.utils.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//事件的消费者
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    MessageService messageService;
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    ElasticSearchService elasticSearchService;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_FOLLOW,TOPIC_LIKE})
    public void handlerCommentMessage(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息的内容为空");
            return ;
        }
        //将json字符串转换为event
        Event event = JSONObject.parseObject(record.value().toString(),Event.class);
        if (event == null){
            logger.error("消息的格式错误");
            return;
        }
        //event解析正确
        Message message = new Message();
        //1代表系统
        message.setFromId(SYSTEM);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setStatus(0);
        message.setCreateTime(new Date());
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",event.getUserId());
        map.put("entityType",event.getEntityType());
        map.put("entityId",event.getEntityId());

        if (!event.getData().isEmpty()){
            //存入map中
            Set<String> set = event.getData().keySet();
            for (String key : set){
                map.put(key, event.getData().get(key));
            }
        }
        //将以上map存入content中
        message.setContent(JSONObject.toJSONString(map));
        messageService.addMessage(message);
    }

    //处理将新发布的帖子添加键elasticsearch
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlerPublishMessage(ConsumerRecord record){
        if (record ==null || record.value() == null){
            logger.error("消息内容为空");
            return ;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式不正确");
            return;
        }
        //进行处理
        //添加
        DiscussPost discussPost = discussPostService.findDiscussPostById(event.getEntityId());
        elasticSearchService.saveDiscussPost(discussPost);
    }

    //删除帖子时，将帖子从elasticSearch中删除
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handlerDelete(ConsumerRecord record){
        if (record == null || record.value() == null){
            logger.error("消息内容为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null){
            logger.error("消息格式不正确");
            return;
        }
        //进行删除处理
        elasticSearchService.deleteDiscussPost(event.getEntityId());
    }
}
