package com.second.community.controller.advice;

import com.second.community.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

//只扫描带有controller注解的类
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    //处理所有异常的方法
    @ExceptionHandler({Exception.class})
    public void handlerException(Exception e , HttpServletRequest request , HttpServletResponse response) throws IOException {
        logger.error("服务器发生了异常：" + e.getMessage());
        for (StackTraceElement ste: e.getStackTrace()){
            logger.error(ste.toString());
        }
        //判断是否是异步请求，从而确实是返回界面还是json数据
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            //异步请求
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(JsonUtil.getJsonString(1,"服务器异常！"));
        }else {
            response.sendRedirect(request.getContextPath()+"/toError");
        }
    }
}
