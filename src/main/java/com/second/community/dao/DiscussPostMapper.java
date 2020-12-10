package com.second.community.dao;

import com.second.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface DiscussPostMapper {
    //offset起始索引，limit每页限定的页数
    List<DiscussPost> selectDiscussPosts(int userId,int offset, int limit ,int orderMode);

    //查该id总共有多少内容
    int selectDiscussPostRows(int userId);

    //发布帖子
    int insertDiscussPost(DiscussPost discussPost);

    //查询帖子详情
    DiscussPost selectDiscussPostById(int id);

    //修改帖子的评论数量，便于查询
    int updateCommentCount(int id,int commentCount);

    //修改帖子类型
    int updateType(int id , int type);

    //修改帖子状态
    int updateStatus(int id , int status);

    //跟新帖子的分数
    int updateScore(int id,double score);
}
