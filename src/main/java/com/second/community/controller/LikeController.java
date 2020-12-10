package com.second.community.controller;

import com.second.community.entity.Event;
import com.second.community.entity.User;
import com.second.community.event.EventProducer;
import com.second.community.service.LikeService;
import com.second.community.utils.HostHolderUtil;
import com.second.community.utils.JsonUtil;
import com.second.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController {

    @Autowired
    LikeService likeService;
    @Autowired
    HostHolderUtil hostHolderUtil;
    @Autowired
    EventProducer eventProducer;
    @Autowired
    RedisTemplate redisTemplate;

    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType,int entityId ,int entityUserId ,int postId){
        User user = hostHolderUtil.getUser();
        //点赞
        likeService.like(user.getId(),entityType,entityId ,entityUserId);
        //数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(),entityType,entityId);
        Map<String,Object> map = new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);
        //判断是否点过赞,点赞才触发这个事件
        if (likeStatus == 1){
            Event event = new Event();
            event.setTopic("like").setUserId(hostHolderUtil.getUser().getId()).setEntityType(entityType)
                    .setEntityId(entityId).setEntityUserId(entityUserId).setData("postId",postId);
            eventProducer.fireEvent(event);
        }
        //对帖子点赞才进行加score的操作
        if (entityType == 1){
            //计算帖子的分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }
        return JsonUtil.getJsonString(0,null,map);
    }
}
