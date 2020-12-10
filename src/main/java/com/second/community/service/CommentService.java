package com.second.community.service;

import com.second.community.dao.CommentMapper;
import com.second.community.dao.DiscussPostMapper;
import com.second.community.entity.Comment;
import com.second.community.utils.CommunityConstant;
import com.second.community.utils.SensitiveFilterUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    @Autowired
    CommentMapper commentMapper;
    @Autowired
    DiscussPostMapper discussPostMapper;
    @Autowired
    SensitiveFilterUtil sensitiveFilterUtil;

    public List<Comment> findCommentsByEntity(int entityType, int entityId,int offset ,int limit){
        return commentMapper.selectCommentsByEntity(entityType,entityId,offset,limit);
    }

    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCountByEntity(entityType,entityId);
    }

    //发表评论
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if (comment == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //过滤html标签
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        //过滤敏感词
        comment.setContent(sensitiveFilterUtil.filter(comment.getContent()));
        //进行插入操作
        int row = commentMapper.insertComment(comment);
        //更新帖子的评论数量
        if (comment.getEntityType()== ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntity(ENTITY_TYPE_POST, comment.getEntityId());
            discussPostMapper.updateCommentCount(comment.getEntityId(),count);
        }

        return row;
    }

    //根据ID查询comment
    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }
}
