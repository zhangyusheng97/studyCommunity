package com.second.community.dao;

import com.second.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommentMapper {

    //分页操作
    List<Comment> selectCommentsByEntity(int entityType , int entityId, int offset ,int limit);

    //查询总数
    int selectCountByEntity(int entityType,int entityId);

    //添加帖子
    int insertComment(Comment comment);

    //根据id查询comment
    Comment selectCommentById(int id);
}
