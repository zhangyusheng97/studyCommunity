package com.second.community.quartz;


import com.second.community.entity.DiscussPost;
import com.second.community.service.DiscussPostService;
import com.second.community.service.ElasticSearchService;
import com.second.community.service.LikeService;
import com.second.community.utils.CommunityConstant;
import com.second.community.utils.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    LikeService likeService;
    @Autowired
    ElasticSearchService elasticSearchService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-08-31 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化失败",e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);
        //判断是否有跟新，如果没有就不进行操作了
        if (operations.size() == 0){
            logger.info("暂时没有需要刷新的帖子");
            return;
        }
        logger.info("正在刷新帖子热度");
        while (operations.size() > 0){
            this.refresh((Integer)operations.pop());
        }
        logger.info("刷新完毕！");
    }

    //刷新的方法
    private void refresh(int postId){
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
        if (discussPost ==null){
            logger.error("该帖子已经被删除！");
            return;
        }
        boolean good = (discussPost.getStatus() == 1);
        int commentCount = discussPost.getCommentCount();
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,postId);
        //算分
        double w = (good?75:0) + commentCount * 10 + likeCount * 2;
        double score = Math.log10(Math.max(w,1)) + ((discussPost.getCreateTime().getTime()-epoch.getTime()) / (1000*3600*24));
        //跟新帖子的分数
        discussPostService.updateDiscussPostScore(postId,score);
        //同步搜索的数据
        discussPost.setScore(score);
        elasticSearchService.saveDiscussPost(discussPost);
    }
}
