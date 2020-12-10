package com.second.community.controller;

import com.second.community.annotation.LoginRequired;
import com.second.community.entity.User;
import com.second.community.service.FollowService;
import com.second.community.service.LikeService;
import com.second.community.service.UserService;
import com.second.community.utils.HostHolderUtil;
import com.second.community.utils.UUIDUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    //文件上传的路径
    @Value("${community.path.upload}")
    private String uploadPath;

    //访问路径
    @Value("${community.path.domain}")
    private String domain;

    @Autowired
    UserService userService;

    //获取用户信息
    @Autowired
    HostHolderUtil hostHolderUtil;

    //获取点赞数量
    @Autowired
    LikeService likeService;

    //获取关注的人数与关注我的人的人数
    @Autowired
    FollowService followService;

    //跳转到设置界面
    @LoginRequired
    @RequestMapping("/setting")
    public String getSettingPage() {
        return "site/setting";
    }

    //修改头像
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片");
            return "site/setting";
        }
        //获取原始文件名
        String fileName = headerImage.getOriginalFilename();
        //获取文件的格式
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确");
            return "site/setting";
        }
        //重新生成文件名
        fileName = UUIDUtil.getUUID() + suffix;
        //确定文件存放的路径
        try {
            File dest = new File(uploadPath + "/" + fileName);
            headerImage.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //更新当前用户头像的路径
        User user = hostHolderUtil.getUser();
        String HeaderUrl = domain + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), HeaderUrl);
        return "redirect:/index";
    }

    //获取头像
    @RequestMapping("/header/{fileName}")
    public void getHeader(@PathVariable String fileName, HttpServletResponse response) {
        //存放位置
        fileName = uploadPath + "/" + fileName;
        //获取图片的格式
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        //将图片输出
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] b = new byte[1024];
            int len;
            while ((len = fis.read(b)) != -1) {
                os.write(b, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //修改密码
    @PostMapping("/changePassword")
    public String changePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolderUtil.getUser();
        Map<String, Object> map = userService.changePassword(user.getId(), oldPassword, newPassword);
        if (map != null && map.containsKey("passwordMsg")) {
            //有报错信息
            String passwordMsg = (String) map.get("passwordMsg");
            model.addAttribute("passwordMsg", passwordMsg);
            return "site/setting";
        }
        //新密码为空时
        if (map != null && map.containsKey("newPasswordMsg")) {
            String newPasswordMsg = (String) map.get("newPasswordMsg");
            model.addAttribute("newPasswordMsg", newPasswordMsg);
            return "site/setting";
        }
        //没有错误
        model.addAttribute("msg", "密码修改成功,请重新进行登录操作");
        model.addAttribute("target", "/logout");
        return "site/operate-result";
    }

    //个人主页
    //查看用户收到的赞
    //添加关注的人数等内容
    @RequestMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        //System.out.println(userId);
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        //关注相关的内容
        long followeeCount = followService.findFolloweeCount(userId, 3);
        model.addAttribute("followeeCount", followeeCount);
        long followerCount = followService.findFollowerCount(3, userId);
        model.addAttribute("followerCount", followerCount);
        boolean hasFollow = false;
        if (hostHolderUtil.getUser() != null) {
            hasFollow = followService.hasFollow(hostHolderUtil.getUser().getId(), 3, userId);
        }
        model.addAttribute("hasFollow", hasFollow);
        return "site/profile";
    }

}
