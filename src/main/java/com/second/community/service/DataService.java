package com.second.community.service;

import com.second.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    RedisTemplate redisTemplate;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    //将指定的ip计入UV
    public void recordUV(String ip){
        String date = sdf.format(new Date());
        String redisKey = RedisKeyUtil.getUVKey(date);
        redisTemplate.opsForHyperLogLog().add(redisKey,ip);
    }

    //统计指定日期范围内的UV
    public long calculateUV(Date start ,Date end){
        if (start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //整理该日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getUVKey(sdf.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE,1);
        }
        //合并数据
        String key = RedisKeyUtil.getUVKey(sdf.format(start), sdf.format(end));
        redisTemplate.opsForHyperLogLog().union(key, keyList.toArray());
        return redisTemplate.opsForHyperLogLog().size(key);
    }

    //将指定用户加入DAU
    public void recordDAU(int userId){
        String DAUKey = RedisKeyUtil.getDAU(sdf.format(new Date()));
        redisTemplate.opsForValue().setBit(DAUKey,userId,true);
    }

    //统计指定日期范围内的DAU
    public long calculateDAU(Date start,Date end){
        if (start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //整理该端日期中的key
        ArrayList<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getDAU(sdf.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE,1);
        }
        //进行OR运算
        return (long)redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAU(sdf.format(start), sdf.format(end));
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return redisConnection.bitCount(redisKey.getBytes());
            }
        });
    }
}
