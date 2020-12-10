package com.second.community;

import com.alibaba.fastjson.JSONObject;
import com.second.community.config.ElasticSearchConfig;
import com.second.community.dao.CommentMapper;
import com.second.community.dao.DiscussPostMapper;
import com.second.community.dao.elasticsearch.DiscussPostRepository;
import com.second.community.entity.Comment;
import com.second.community.entity.DiscussPost;
import com.second.community.service.ElasticSearchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import javax.naming.directory.SearchResult;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;


@SpringBootTest
class CommunityApplicationTests {

    @Autowired
    DiscussPostMapper discussPostMapper;
    @Autowired
    DiscussPostRepository discussRepository;
    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    ElasticSearchService service;

    private static final Logger logger = LoggerFactory.getLogger(CommunityApplicationTests.class);
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);


    //处理sleep
    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //JDK普通线程池
    @Test
    void testExecutorService() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello");
            }
        };
        for (int i = 0; i < 10 ;i++){
            executorService.submit(runnable);
        }
        sleep(10000);
    }

    @Test
    void testScheduledExecutorService(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello");
            }
        };
        //第二个参数：延迟多少毫秒在执行，第三个参数：执行的时间间隔，时间的单位
       scheduledExecutorService.scheduleAtFixedRate(runnable,10000,1000, TimeUnit.MILLISECONDS);
       sleep(30000);
       //scheduledExecutorService.scheduleWithFixedDelay()已固定的延迟去执行，就执行一次
    }

    //spring可执行线程池
    @Autowired
    ThreadPoolTaskExecutor executor;
    //spring定时任务线程池
    @Autowired
    ThreadPoolTaskScheduler scheduler;
    @Test
    void testThreadPoolTaskExecutor(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("test");
            }
        };
        for (int i=0;i<10;i++){
            executor.submit(runnable);
        }
        sleep(10000);
    }
    @Test
    void testThreadPoolTaskScheduler(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("test");
            }
        };
        Date date = new Date(System.currentTimeMillis()+10000);
        scheduler.scheduleAtFixedRate(runnable,date,1000);
        //scheduler.scheduleAtFixedRate(runnable,1000);
        sleep(30000);
    }

    @Autowired
    RedisTemplate redisTemplate;

    @Test
    void testElasticSearch() {
        discussRepository.save(discussPostMapper.selectDiscussPostById(286));
    }

    @Test
    void addAll() {
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100,0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100,0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(111, 0, 100,0));
        discussRepository.saveAll(discussPostMapper.selectDiscussPosts(149, 0, 100,0));
    }

    @Test
    void search() {
        //搜索功能
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //elasticsearchRestTemplate.search()
        //获取查询到的discussPost
        ArrayList<DiscussPost> list = new ArrayList<>();
        SearchHits<DiscussPost> hits = elasticsearchRestTemplate.search(searchQuery, DiscussPost.class);
        System.out.println(hits);
        List<SearchHit<DiscussPost>> searchHits = hits.getSearchHits();
        for (SearchHit hit : searchHits) {
            DiscussPost discussPost = new DiscussPost();
            discussPost = (DiscussPost) hit.getContent();
            list.add(discussPost);
            //处理高亮显示
            List<String> title = hit.getHighlightField("title");
            if (!title.isEmpty()) {
                discussPost.setTitle(title.get(0));
            }
            List<String> content = hit.getHighlightField("content");
            if (!content.isEmpty()) {
                discussPost.setContent(content.get(0));
            }
        }
        // System.out.println(list);
    }

    @Test
    void testSearch() {
        Map<String, Object> map = service.searchDiscussPost("张", 0, 10);
        Set<String> set = map.keySet();
        for (String s : set) {
            System.out.println(map.get(s));
        }
        System.out.println("------------------------");
        System.out.println(map);
    }

    @Test
    void testHyperLogLog() {
        String redisKey = "test:hll:01";
        for (int i = 0; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }
        for (int i = 0; i <= 100000; i++) {
            int j = (int) (Math.random() * 100000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, j);
        }
        System.out.println(redisTemplate.opsForHyperLogLog().size(redisKey));
    }

    //将三组数据，在统计合并后的重复数据的独立总数
    @Test
    void testHLL() {
        String redisKey1 = "test:hll:01";
        for (int i = 0; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey1, i);
        }
        String redisKey2 = "test:hll:02";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }
        String redisKey3 = "test:hll:03";
        for (int i = 10001; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }
        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey1, redisKey2, redisKey3);
        System.out.println(redisTemplate.opsForHyperLogLog().size(unionKey));
    }

    //统计一组数据的布尔值
    @Test
    void testBitmap() {
        String redisKey = "test:bm:01";
        //存入
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 2, true);
        //取出
        redisTemplate.opsForValue().getBit(redisKey, 1);
        redisTemplate.opsForValue().getBit(redisKey, 3);
        //统计
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }


}