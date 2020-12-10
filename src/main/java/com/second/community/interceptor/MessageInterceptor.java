package com.second.community.interceptor;

import com.second.community.entity.User;
import com.second.community.service.MessageService;
import com.second.community.utils.HostHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    @Autowired
    HostHolderUtil hostHolderUtil;
    @Autowired
    MessageService messageService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolderUtil.getUser();
        if (user != null && modelAndView != null){
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int unreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), null);
            modelAndView.addObject("allUnreadCount",letterUnreadCount+unreadNoticeCount);
        }
    }

}
