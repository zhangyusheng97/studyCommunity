package com.second.community.controller;

import com.second.community.entity.DiscussPost;
import com.second.community.entity.Page;
import com.second.community.entity.User;
import com.second.community.service.DiscussPostService;
import com.second.community.service.LikeService;
import com.second.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    UserService userService;
    //查看赞的数量
    @Autowired
    LikeService likeService;

    @RequestMapping({"/index","/"})
    public String getIndexPage(Model model,Page page,@RequestParam(name = "orderMode",defaultValue = "0") int orderMode ) {
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);
        List<DiscussPost> list = null;
        if (orderMode ==0){
            list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        }else if (orderMode == 1){
            list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        }
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                //添加赞的数量
                long likeCount = likeService.findEntityLikeCount(1, post.getId());
                map.put("likeCount",likeCount);

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("orderMode",orderMode);
        return "index";
    }

    //进入错误界面
    @RequestMapping("/toError")
    public String getErrorPage(){
        return "error/500";
    }

    @RequestMapping("/denied")
    public String getDenied(){
        return "error/404";
    }


}
