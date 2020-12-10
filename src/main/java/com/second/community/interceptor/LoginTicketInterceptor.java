package com.second.community.interceptor;

import com.second.community.entity.LoginTicket;
import com.second.community.entity.User;
import com.second.community.service.UserService;
import com.second.community.utils.CookieUtil;
import com.second.community.utils.HostHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;
    @Autowired
    HostHolderUtil hostHolderUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket != null) {
            //不为null即为登录了
            //通过ticket获取到当前的用户id，然后拿到这个用户
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            //判断凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                //判断为有效
                int userId = loginTicket.getUserId();
                User user = userService.findUserById(userId);
                //将请求持有的用户加入线程中
                hostHolderUtil.setUser(user);
                //构建用户认证的结果，并存入SecurityContext,便于security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(user,user.getPassword(),userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //从当前线程中获取user
        User user = hostHolderUtil.getUser();
        if (user != null && modelAndView != null){
            modelAndView.addObject("loginUser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolderUtil.clear();
        SecurityContextHolder.clearContext();
    }
}
