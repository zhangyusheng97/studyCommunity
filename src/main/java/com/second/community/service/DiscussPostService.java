package com.second.community.service;

import com.second.community.dao.DiscussPostMapper;
import com.second.community.entity.DiscussPost;
import com.second.community.utils.SensitiveFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService {

    @Autowired
    DiscussPostMapper discussPostMapper;

    @Autowired
    SensitiveFilterUtil sensitiveFilterUtil;

    //普通的排序
    public List<DiscussPost> findDiscussPosts(int userId, int offset , int limit){
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,0);
    }
    //按热度排序
    public List<DiscussPost> findDiscussPosts(int userId, int offset , int limit ,int orderMode){
        //0为普通排序，1为按热度排序
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }
    public int findDiscussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //发布帖子
    public int addDiscussPost(DiscussPost discussPost){
        if (discussPost == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //对html标签进行转义，防止有歧义的标签
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        //进行敏感词过滤
        discussPost.setTitle(sensitiveFilterUtil.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilterUtil.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    //查看帖子详情
    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    //更新帖子的评论数
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    //修改帖子类型
    public int updateDiscussPostType(int id ,int type){
        return discussPostMapper.updateType(id, type);
    }

    //修改帖子状态
    public int updateDiscussPostStatus(int id,int status){
        return discussPostMapper.updateStatus(id, status);
    }

    //修改帖子的分数
    public int updateDiscussPostScore(int id , double score){
        return discussPostMapper.updateScore(id, score);
    }
}
