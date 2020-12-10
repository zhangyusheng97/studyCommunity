package com.second.community.controller;

import com.second.community.entity.DiscussPost;
import com.second.community.entity.Page;
import com.second.community.service.CommentService;
import com.second.community.service.ElasticSearchService;
import com.second.community.service.LikeService;
import com.second.community.service.UserService;
import com.second.community.utils.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements CommunityConstant {

    @Autowired
    ElasticSearchService elasticSearchService;
    @Autowired
    UserService userService;
    @Autowired
    LikeService likeService;

    //传值方式 /search?keyword=XXX
    @RequestMapping("/search")
    public String search(String keyword, Page page, Model model) {
        Map<String, Object> elasticSearchResult = elasticSearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        List<DiscussPost> searchResult = (List<DiscussPost>) elasticSearchResult.get("searchResult");
        ArrayList<Map<String, Object>> discussPosts = new ArrayList<>();
        if (!searchResult.isEmpty()) {
            for (DiscussPost post : searchResult) {
                HashMap<String, Object> map = new HashMap<>();
                //存入帖子
                map.put("discussPost", post);
                //存入帖子的作者
                map.put("user", userService.findUserById(post.getUserId()));
                //存入帖子的点赞数
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);
        //设置分页的信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows((int) elasticSearchResult.get("rows"));
        return "site/search";
    }
}
