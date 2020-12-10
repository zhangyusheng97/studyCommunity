package com.second.community.dao;

import com.second.community.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserMapper {
    User selectById(int id);

    User selectByName(String username);

    User selectByEmail(String email);

    //添加用户
    int insertUser(User user);

    //进行激活
    int updateStatus(int id, int status);

    //换头像
    int updateHeader(int id, String headerUrl);

    //改密码
    int updatePassword(int id, String password);


}
