package com.second.community.controller;

import com.second.community.annotation.LoginRequired;
import com.second.community.entity.Event;
import com.second.community.entity.Page;
import com.second.community.entity.User;
import com.second.community.event.EventProducer;
import com.second.community.service.FollowService;
import com.second.community.service.UserService;
import com.second.community.utils.HostHolderUtil;
import com.second.community.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController {

    @Autowired
    FollowService followService;
    //该操作只能是当前用户进行操作，所以需要获取当前用户
    @Autowired
    HostHolderUtil hostHolderUtil;
    @Autowired
    UserService userService;
    @Autowired
    EventProducer eventProducer;

    //关注功能
    @LoginRequired
    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType,int entityId){
        User user = hostHolderUtil.getUser();
        followService.follow(user.getId(),entityType,entityId);
        //关注之后进行通知
        Event event = new Event();
        event.setTopic("follow").setEntityType(entityType).setEntityId(entityId)
                .setUserId(hostHolderUtil.getUser().getId()).setEntityUserId(entityId);
        eventProducer.fireEvent(event);
        return JsonUtil.getJsonString(0,"已关注");
    }

    //取消关注
    @LoginRequired
    @PostMapping("/unfollow")
    @ResponseBody
    public String unfollow(int entityType, int entityId){
        User user = hostHolderUtil.getUser();
        followService.unFollow(user.getId(),entityType,entityId);
        return JsonUtil.getJsonString(0,"已取消关注");
    }

    //查询我关注的人的列表
    @RequestMapping("/followees/{userId}")
    public String getFollowee(@PathVariable("userId") int userId, Page page , Model model){
        //该页面的用户
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);
        //分页的设置
        page.setLimit(5);
        page.setRows((int) followService.findFolloweeCount(userId,3));
        page.setPath("/followees/"+userId);
        //关注的人的信息
        List<Map<String, Object>> userList = followService.findFollowee(userId, page.getOffset(), page.getLimit());
        if (userList != null){
            for (Map<String ,Object> map : userList){
                User u = (User) map.get("user");
                //判断当前用户是否关注了该用户
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        return "site/followee";
    }


    //我的粉丝列表
    @RequestMapping("/followers/{userId}")
    public String getFollower(@PathVariable("userId") int userId, Page page , Model model){
        //该页面的用户
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);
        //分页的设置
        page.setLimit(5);
        page.setRows((int) followService.findFollowerCount(3,userId));
        page.setPath("/followers/"+userId);
        //关注的人的信息
        List<Map<String, Object>> userList = followService.findFollower(userId, page.getOffset(), page.getLimit());
        if (userList != null){
            for (Map<String ,Object> map : userList){
                User u = (User) map.get("user");
                //判断当前用户是否关注了该用户
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        return "site/follower";
    }


    //判断是否关注该用户
    private boolean hasFollowed(int userId){
        if (hostHolderUtil.getUser() ==null){
            return false;
        }
        return followService.hasFollow(hostHolderUtil.getUser().getId(),3,userId);
    }
}
