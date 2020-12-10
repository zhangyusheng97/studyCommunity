package com.second.community.service;

import com.second.community.dao.LoginTicketMapper;
import com.second.community.dao.UserMapper;
import com.second.community.entity.LoginTicket;
import com.second.community.entity.User;
import com.second.community.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    UserMapper userMapper;
    //注册所需要的内容
    @Autowired
    MailUtil mailUtil;
    //网站的domain
    @Value("${community.path.domain}")
    String domain;
    //模板引擎
    @Autowired
    TemplateEngine templateEngine;

    //登录的相关设置
//    @Autowired
//    LoginTicketMapper loginTicketMapper;
    @Autowired
    RedisTemplate redisTemplate;

    //根据id查询用户
    //使用redis重构
    public User findUserById(int id) {
       // return userMapper.selectById(id);
        //先从缓存中查询
        User user = getCache(id);
        //缓存中不存在时
        if (user == null){
            user = initCache(id);
        }
        return user;
    }


    //用户注册
    public Map<String, Object> register(User user) {
        HashMap<String, Object> map = new HashMap<>();
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "用户账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "用户密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "用户邮箱不能为空！");
            return map;
        }
        //验证账号是否存在
        User userFind = userMapper.selectByName(user.getUsername());
        if (userFind != null) {
            map.put("userMsg", "该用户已存在");
            return map;
        }
        //验证邮箱是否已注册
        userFind = userMapper.selectByEmail(user.getEmail());
        if (userFind != null) {
            map.put("emailMsg", "该邮箱已注册");
            return map;
        }

        //注册用户
        user.setSalt(UUIDUtil.getUUID().substring(0, 5));
        user.setPassword(UUIDUtil.getMD5(user.getPassword()) + user.getSalt());
        user.setType(0);//普通用户
        user.setStatus(0);//未激活
        user.setActivationCode(UUIDUtil.getUUID());//设置激活码
        //设置头像
        user.setHeaderUrl("http://images.nowcoder.com/head/11t.png");
        //设置注册时间
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        User nowUser = userMapper.selectByName(user.getUsername());
        //发送激活邮件
        //System.out.println(nowUser);
        Context context = new Context();
        context.setVariable("email", nowUser.getEmail());
        String url = domain + "/activation/" + nowUser.getId() + "/" + nowUser.getActivationCode();
        context.setVariable("url", url);
        //利用模板引擎生成邮件内容
        String content = templateEngine.process("/mail/activation", context);
        //发送邮件
        mailUtil.sendMail(user.getEmail(), "账号激活邮件", content);

        return map;
    }


    //激活的业务
    public int activation(int userId, String activationCode) {
        User user = userMapper.selectById(userId);
        //已激活
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(activationCode)) {//激活成功
            userMapper.updateStatus(userId, 1);    //修改状态
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAIL;
        }
    }

    //登录相关的service
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "账号不存在！");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "账号未激活");
            return map;
        }
        //验证密码
        password = UUIDUtil.getMD5(password) + user.getSalt();
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码错误");
            return map;
        }
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(UUIDUtil.getUUID().substring(0, 5));
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        // loginTicketMapper.insertLoginTicket(loginTicket);
        //传入redis中
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
        //传输凭证
        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    //退出相关的功能
    public void logout(String ticket) {
        //loginTicketMapper.updateStatus(ticket,1);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
    }

    //通过ticket查询返回loginTicket
    public LoginTicket findLoginTicket(String ticket) {
       // return loginTicketMapper.selectByTicket(ticket);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket)redisTemplate.opsForValue().get(ticketKey);
        return loginTicket;
    }

    //修改密码
    public Map<String, Object> changePassword(int userId, String oldPassword, String newPassword) {
        HashMap<String, Object> map = new HashMap<>();
        //获取当前的用户
        User user = userMapper.selectById(userId);
        //判断输入的原密码是否为空
        if (StringUtils.isBlank(oldPassword)) {
            map.put("passwordMsg", "密码为空，请输入！");
            return map;
        }
        //判断原密码是否正确
        if (!(UUIDUtil.getMD5(oldPassword) + user.getSalt()).equals(user.getPassword())) {
            //不正确时
            map.put("passwordMsg", "密码输入错误");
            return map;
        }
        //判断新密码是否为空
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码为空");
            return map;
        }
        //密码不为空且密码输入正确之后进行修改密码的操作
        newPassword = UUIDUtil.getMD5(newPassword) + user.getSalt();
        userMapper.updatePassword(userId, newPassword);
        return map;
    }


    //上传文件修改头像
    public int updateHeader(int userId, String headerUrl) {
        int rows =  userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    //根据用户名查找用户
    public User findUserByUsername(String username) {
        return userMapper.selectByName(username);
    }

    //优先从缓存中取值
    private User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    //缓存中取不到值时缓存数据
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    //数据变更之后删除缓存
    private void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    //获取用户的权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = userMapper.selectById(userId);
        ArrayList<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
