package com.second.community.service;

import com.second.community.dao.MessageMapper;
import com.second.community.entity.Message;
import com.second.community.utils.SensitiveFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    MessageMapper messageMapper;
    @Autowired
    SensitiveFilterUtil sensitiveFilterUtil;

    public List<Message> findConversations(int userId ,int offset ,int limit){
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId){
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId , int offset ,int limit){
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId){
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId,String conversationId){
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    //添加消息
    public int addMessage(Message message){
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilterUtil.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    //消息修改为已读
    public int readMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids,1);
    }

    //消息删除
    public int deleteMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids,2);
    }

    //查询最新的系统通知
    public Message findLatestNotice(int userId,String topic){
        return messageMapper.selectLatestNotice(userId, topic);
    }

    //查询某个主题的通知数量
    public int findNoticeCount(int userId,String topic){
        return messageMapper.selectNoticeCount(userId, topic);
    }

    //查询某个主题未读的系统通知数量
    public int findUnreadNoticeCount(int userId,String topic){
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    //查询某个主题下所有通知
    public List<Message> findNotices(int userId,String topic ,int offset,int limit){
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }
}
