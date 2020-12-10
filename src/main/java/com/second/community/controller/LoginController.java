package com.second.community.controller;

import com.google.code.kaptcha.Producer;
import com.second.community.annotation.LoginRequired;
import com.second.community.entity.User;
import com.second.community.service.UserService;
import com.second.community.utils.CommunityConstant;
import com.second.community.utils.CookieUtil;
import com.second.community.utils.RedisKeyUtil;
import com.second.community.utils.UUIDUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    UserService userService;
    //验证码的bean
    @Autowired
    Producer kaptchaProducer;
    @Autowired
    RedisTemplate redisTemplate;

    //注册
    @GetMapping("/register")
    public String getRegisterPage() {
        return "site/register";
    }

    //登录界面
    @GetMapping("/login")
    public String getLoginPage() {
        return "site/login";
    }

    //注册的具体流程
    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        //注册成功
        if (map == null || map.isEmpty()) {
            //页面跳转
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送激活邮件,请尽快激活");
            model.addAttribute("target", "/index");
            return "site/operate-result";
        } else {
            model.addAttribute("userMsg", map.get("userMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "site/register";
        }
    }

    //激活的具体流程
    @RequestMapping("/activation/{userId}/{activationCode}")
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("activationCode") String activationCode) {
        int result = userService.activation(userId, activationCode);
        if (result == ACTIVATION_SUCCESS) {//激活成功
            model.addAttribute("msg", "激活成功，请前往登录界面进行登录");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "操作无效，您已经激活过");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败，您的激活码不正确");
            model.addAttribute("target", "/index");
        }
        return "site/operate-result";
    }


    //获取验证码图片的路径
    //使用redis来进行缓存，重写代码
    @RequestMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response /*, HttpSession session*/) {
        //生成验证码字符串
        String text = kaptchaProducer.createText();
        //通过生成的字符串生成图片
        BufferedImage image = kaptchaProducer.createImage(text);

        //将验证码传入session便于验证
        //session.setAttribute("kaptcha",text);

        //将验证码的对应标识传入cookie中
        String kaptchaOwner = UUIDUtil.getUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath("/");
        response.addCookie(cookie);
        //将验证码传入redis中
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        //设置为60s失效
        redisTemplate.opsForValue().set(kaptchaKey, text, 60, TimeUnit.SECONDS);

        //图片输入给浏览器
        //设置类型
        response.setContentType("image/png");
        //将图片输出
        try {
            ImageIO.write(image, "png", response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //登录的controller
    //session中获取验证码的key为：kaptcha
    //使用redis进行重构
    @PostMapping("/login")
    public String login(Model model, String username, String password, String verifycode, boolean remember
            /*,HttpSession session*/, HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner) {
        //首先判断验证码是否正确
        // verifycode为从界面获取的验证码
        /*
        String kaptcha = session.getAttribute("kaptcha").toString();
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(verifycode) || !kaptcha.equalsIgnoreCase(verifycode)){
            model.addAttribute("codeMsg","验证码不正确");
            return "site/login";
        }
        */
        //从redis中取出进行判断
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(verifycode) || !kaptcha.equalsIgnoreCase(verifycode)) {
            model.addAttribute("codeMsg", "验证码不正确");
            return "site/login";
        }

        //验证码没有错误时检验账号密码
        int expiredSeconds = remember ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            //包含ticket的key即为成功
            //向客户端发送cookie
            Cookie cookie = new Cookie("ticket", (String) map.get("ticket"));
            //设置cookie存在的路径
            cookie.setPath("/");
            //设置cookie存在的时间
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:index";
        } else {
            //登录失败
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "site/login";
        }
    }

    //注销账号
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}
