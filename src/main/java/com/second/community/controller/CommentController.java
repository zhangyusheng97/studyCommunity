package com.second.community.controller;

import com.second.community.entity.Comment;
import com.second.community.entity.DiscussPost;
import com.second.community.entity.Event;
import com.second.community.event.EventProducer;
import com.second.community.service.CommentService;
import com.second.community.service.DiscussPostService;
import com.second.community.utils.HostHolderUtil;
import com.second.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    CommentService commentService;
    @Autowired
    HostHolderUtil hostHolderUtil;//获取发表评论的当前用户
    @Autowired
    EventProducer eventProducer;
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    RedisTemplate redisTemplate;

    //添加评论
    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolderUtil.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        //添加
        commentService.addComment(comment);
        //触发评论事件
        Event event = new Event();
        event.setTopic("comment").setEntityType(comment.getEntityType()).setEntityId(comment.getEntityId())
                .setUserId(hostHolderUtil.getUser().getId()).setData("postId",discussPostId);
        //给帖子评论时
        if (comment.getEntityType() == 1){
            DiscussPost discussPost = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(discussPost.getUserId());
            //计算帖子的分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }else if (comment.getEntityType() == 2){
            //回复评论时
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        //发布消息
        eventProducer.fireEvent(event);
        return "redirect:/discuss/detail/"+discussPostId;
    }
}
