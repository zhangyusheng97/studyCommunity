package com.second.community.utils;

import com.second.community.entity.User;
import org.springframework.stereotype.Component;

//用于代替session对象来持有用户信息
@Component
public class HostHolderUtil {
    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    public void clear(){
        users.remove();
    }
}
