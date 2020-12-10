package com.second.community.dao;

import com.second.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MessageMapper {
    //查询当前用户的会话列表，针对每个会话只返回一条最新的私信
    List<Message> selectConversations(int userId , int offset , int limit);

    //查询当前用户的会话数量
    int selectConversationCount(int userId);

    //查询某个会话所包含的会话列表
    List<Message> selectLetters(String conversationId,int offset,int limit);

    //查询某个会话所包含的消息数量
    int selectLetterCount(String conversationId);

    //查询未读私信的数量
    int selectLetterUnreadCount(int userId,String conversationId);

    //添加私信
    int insertMessage(Message message);

    //更改消息的状态(设置已读未读和设置是否删除)
    int updateStatus(List<Integer> ids,int status);

    //查询某个主题下的最新的通知会话
    Message selectLatestNotice(int userId,String topic);

    //查询某个主题下的通知数量
    int selectNoticeCount(int userId,String topic);

    //查询某个主题下的未读的通知数量
    int selectNoticeUnreadCount(int userId,String topic);

    //查询某个主题下的所有通知
    List<Message> selectNotices(int userId,String topic,int offset,int limit);
}
