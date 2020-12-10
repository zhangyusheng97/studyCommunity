package com.second.community.dao;

import com.second.community.entity.LoginTicket;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
@Deprecated
//不推荐使用
public interface LoginTicketMapper {
    int insertLoginTicket (LoginTicket loginTicket);

    LoginTicket selectByTicket(String ticket);

    int updateStatus(String ticket,int status);
}
