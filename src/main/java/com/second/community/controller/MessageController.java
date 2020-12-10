package com.second.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.second.community.entity.Message;
import com.second.community.entity.Page;
import com.second.community.entity.User;
import com.second.community.service.MessageService;
import com.second.community.service.UserService;
import com.second.community.utils.CommunityConstant;
import com.second.community.utils.HostHolderUtil;
import com.second.community.utils.JsonUtil;
import org.apache.kafka.clients.admin.TopicListing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
@RequestMapping("/letter")
public class MessageController implements CommunityConstant {

    @Autowired
    MessageService messageService;
    @Autowired
    HostHolderUtil hostHolderUtil;
    @Autowired
    UserService userService;

    //私信列表
    @RequestMapping("/list")
    public String getLetterList(Model model, Page page) {
        User user = hostHolderUtil.getUser();
        //设置page相关的信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        //查询会话列表
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        //用于封装所需对象
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                int targetId = (user.getId() == message.getFromId() ? message.getToId() : message.getFromId());
                map.put("target", userService.findUserById(targetId));
                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);
        //查询所有未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        //查询未读系统通知的数量
        int noticeUnreadCount = messageService.findUnreadNoticeCount(user.getId(),null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);
        return "site/letter";
    }

    //查看私信详情
    @RequestMapping("/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        User user = hostHolderUtil.getUser();
        //设置分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        //获取私信
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);
        model.addAttribute("target", getLetterTarget(conversationId));
        //将该详情页面的消息修改为已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }
        return "site/letter-detail";
    }

    //发送私信
    @PostMapping("/send")
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByUsername(toName);
        if (target == null) {
            return JsonUtil.getJsonString(1, "该用户不存在");
        }
        //创建message
        Message message = new Message();
        message.setFromId(hostHolderUtil.getUser().getId());
        message.setToId(target.getId());
        String conversationId = "";
        if (message.getFromId() <= message.getToId()) {
            conversationId = message.getFromId() + "_" + message.getToId();
        } else {
            conversationId = message.getToId() + "_" + message.getFromId();
        }
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(new Date());
        //加入message
        messageService.addMessage(message);
        return JsonUtil.getJsonString(0);
    }

    //查询会话的对象是谁
    private User getLetterTarget(String conversationId) {
        User user = hostHolderUtil.getUser();
        String[] s = conversationId.split("_");
        int first = Integer.parseInt(s[0]);
        int second = Integer.parseInt(s[1]);
        int target = (user.getId() == first ? second : first);
        return userService.findUserById(target);
    }

    //获取未读消息的id
    private List<Integer> getLetterIds(List<Message> messages) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (messages != null) {
            for (Message message : messages) {
                if (hostHolderUtil.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    @RequestMapping("/notice/list")
    public String getNoticeList(Model model){
        User user = hostHolderUtil.getUser();
        //查询评论类的通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if (message != null){
            HashMap<String, Object> messageVO = new HashMap<>();
            messageVO.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String ,Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));
            int count = messageService.findNoticeCount(user.getId(),TOPIC_COMMENT);
            int unreadCount = messageService.findUnreadNoticeCount(user.getId(),TOPIC_COMMENT);
            messageVO.put("count",count);
            messageVO.put("unread",unreadCount);
            model.addAttribute("commentNotice",messageVO);
        }
        //查询点赞类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        if (message != null){
            HashMap<String, Object> messageVO = new HashMap<>();
            messageVO.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String ,Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));
            int count = messageService.findNoticeCount(user.getId(),TOPIC_LIKE);
            int unreadCount = messageService.findUnreadNoticeCount(user.getId(),TOPIC_LIKE);
            messageVO.put("count",count);
            messageVO.put("unread",unreadCount);
            model.addAttribute("likeNotice",messageVO);
        }
        //查询关注类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if (message != null){
            HashMap<String, Object> messageVO = new HashMap<>();
            messageVO.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String ,Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(),TOPIC_FOLLOW);
            int unreadCount = messageService.findUnreadNoticeCount(user.getId(),TOPIC_FOLLOW);
            messageVO.put("count",count);
            messageVO.put("unread",unreadCount);
            model.addAttribute("followNotice",messageVO);
        }
        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询未读系统通知的数量
        int noticeUnreadCount = messageService.findUnreadNoticeCount(user.getId(),null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "site/notice";
    }

    @RequestMapping("/notice/detail/{topic}")
    public String getNoticeDetail(@PathVariable("topic")String topic,Model model ,Page page){
        User user = hostHolderUtil.getUser();
        page.setPath("/letter/notice/detail/"+topic);
        page.setLimit(5);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));
        List<Message> notices = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String,Object>> noticeVoList = new ArrayList<>();
        if (notices != null){
            for (Message notice : notices){
                HashMap<String, Object> map = new HashMap<>();
                map.put("notice",notice);
                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object> data = JSONObject.parseObject(content,HashMap.class);
                map.put("user",userService.findUserById((Integer) data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId",data.get("entityId"));
                map.put("postId",data.get("postId"));
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices",noticeVoList);
        //设置已读
        List<Integer> ids = getLetterIds(notices);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "site/notice-detail";
    }
}
