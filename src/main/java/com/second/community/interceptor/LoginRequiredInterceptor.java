package com.second.community.interceptor;

import com.second.community.annotation.LoginRequired;
import com.second.community.utils.HostHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

//用于拦截@LoginRequired注解注释过的请求，使其需要登录才能访问

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    HostHolderUtil hostHolderUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断拦截的是否是方法
        if (handler instanceof HandlerMethod){
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHolderUtil.getUser() == null){
                //错误的情况，进行拦截，返回false
                //如果错误就返回到登录界面
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }
        return true;
    }
}
