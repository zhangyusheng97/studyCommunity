package com.second.community.config;

import com.second.community.utils.CommunityConstant;
import com.second.community.utils.JsonUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    /**
     * 路径：
     * CommentController:/comment/add/{discussPostId}
     * DiscussPostController:/discuss/add,/discuss/detail/{id}
     * FollowController: /follow,/unfollow ,/followees/{userId},/followers/{userId},
     * HomeController: /index,/toError
     * LikeController: /like
     * LoginController: /register,/login,激活：/activation/{userId}/{activationCode},/logout,/kaptcha
     * MessageController: /letter/list,/letter/detail/{conversationId},/letter/send,/letter/notice/list
     * /letter/notice/detail/{topic} ,/letter/notice/
     * SearchController: /search
     * UserController:  /user/upload,/user/header/{filename},/user/changePassword,/user/profile/{userId}
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        //需要登录才能执行的路径
        http.authorizeRequests()
                .antMatchers(
                        "/comment/add/**",
                        "/discuss/add",
                        "/letter/**",
                        "/user/upload",
                        "/user/setting",
                        "/like",
                        "/follow",
                        "/unfollow",
                        "/logout"
                ).hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
                //删除，加精，置顶的操作权限设置
                .antMatchers("/discuss/top",
                        "/discuss/good")
                .hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete","/data/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                //其他请求就都允许访问
                .anyRequest().permitAll()
                .and().csrf().disable();  //禁用csrf，建议开启，为了方便代码展示这里禁用掉
        //权限不够时的处理
        http.exceptionHandling().
                //未登录时如何处理
                        authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            //异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(JsonUtil.getJsonString(403, "你还没有登录！"));
                        } else {
                            //同步请求
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                //登录之后权限不足时的处理
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            //异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(JsonUtil.getJsonString(403, "你没有权限！"));
                        } else {
                            //同步请求
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        //security底层会默认拦截/logout请求，进行他自己的退出处理
        //因此需要覆盖他进行退出的逻辑
        http.logout().logoutUrl("/securityLogout").logoutSuccessUrl("/index");
    }
}
