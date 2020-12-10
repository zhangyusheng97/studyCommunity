package com.second.community.controller;

import com.second.community.entity.*;
import com.second.community.event.EventProducer;
import com.second.community.service.CommentService;
import com.second.community.service.DiscussPostService;
import com.second.community.service.LikeService;
import com.second.community.service.UserService;
import com.second.community.utils.CommunityConstant;
import com.second.community.utils.HostHolderUtil;
import com.second.community.utils.JsonUtil;
import com.second.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    UserService userService;

    //获取当前用户
    @Autowired
    HostHolderUtil hostHolderUtil;

    //关于评论的service
    @Autowired
    CommentService commentService;

    @Autowired
    LikeService likeService;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    RedisTemplate redisTemplate;

    //发布帖子
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        //判断是否登录
        User user = hostHolderUtil.getUser();
        if (user == null) {
            return JsonUtil.getJsonString(403, "您还未进行登陆操作");
        }
        //设置discussPost
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setType(0);
        discussPost.setStatus(0);
        discussPost.setCreateTime(new Date());
        discussPost.setScore(0.0);
        //添加
        discussPostService.addDiscussPost(discussPost);
        //将帖子存入elasticsearch
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,discussPost.getId());

        return JsonUtil.getJsonString(0, "发布成功");
    }

    //查看帖子的详情
    @GetMapping("/detail/{id}")
    public String getDiscussPost(@PathVariable("id") int id, Model model, Page page) {
        //查出帖子
        DiscussPost discussPost = discussPostService.findDiscussPostById(id);
        model.addAttribute("discussPost", discussPost);
        //查出用户
        User user = userService.findUserById(discussPost.getUserId());
        model.addAttribute("user", user);
        //查出该帖子的赞
        long likeCount = likeService.findEntityLikeCount(1, id);
        model.addAttribute("likeCount", likeCount);
        //当前用户的点赞状态
        int likeStatus = 0;
        if (hostHolderUtil.getUser() != null) {
            likeStatus = likeService.findEntityLikeStatus(hostHolderUtil.getUser().getId(), 1, id);
        }
        model.addAttribute("likeStatus", likeStatus);

        //用户评论的分页
        page.setLimit(5);
        page.setPath("/discuss/detail/" + id);
        page.setRows(discussPost.getCommentCount());
        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, discussPost.getId(), page.getOffset(), page.getLimit());
        ArrayList<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                map.put("user", userService.findUserById(comment.getUserId()));
                //添加回复
                List<Comment> replyComments = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //回复的Vo列表
                //点赞数量
                long commentLikeCount = likeService.findEntityLikeCount(2, comment.getId());
                map.put("likeCount",commentLikeCount);
                //点赞状态
                int commentLikeStatus = hostHolderUtil.getUser() == null? 0:
                        likeService.findEntityLikeStatus(hostHolderUtil.getUser().getId(),2,comment.getId());
                map.put("likeStatus",commentLikeStatus);
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyComments != null) {
                    for (Comment reply : replyComments) {
                        HashMap<String, Object> replyVo = new HashMap<>();
                        //回复
                        replyVo.put("reply", reply);
                        //作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //  System.out.println(userService.findUserById(reply.getUserId()));
                        //点赞数量
                        long replyLikeCount = likeService.findEntityLikeCount(2, reply.getId());
                        replyVo.put("likeCount",replyLikeCount);
                        //点赞状态
                        int replyLikeStatus = hostHolderUtil.getUser() == null? 0:
                                likeService.findEntityLikeStatus(hostHolderUtil.getUser().getId(),2,reply.getId());
                        replyVo.put("likeStatus",replyLikeStatus);
                        //回复的目标
                        User targetUser = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", targetUser);
                        replyVoList.add(replyVo);
                    }
                }
                map.put("replys", replyVoList);
                //回复的数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                map.put("replyCount", replyCount);
                commentVoList.add(map);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "site/discuss-detail";
    }

    //置顶
    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateDiscussPostType(id,1);//1为置顶
        //同步到elasticsearch
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolderUtil.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return JsonUtil.getJsonString(0);
    }

    //加精
    @PostMapping("/good")
    @ResponseBody
    public String setGood(int id){
        discussPostService.updateDiscussPostStatus(id,1);//1为精华帖
        //同步到elasticsearch
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolderUtil.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);
        return JsonUtil.getJsonString(0);
    }

    //删除
    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateDiscussPostStatus(id,2);//2为删除
        //同步到elasticsearch
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolderUtil.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return JsonUtil.getJsonString(0);
    }



}
