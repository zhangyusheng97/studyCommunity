package com.second.community.service;

import com.second.community.dao.elasticsearch.DiscussPostRepository;
import com.second.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticSearchService {
    @Autowired
    DiscussPostRepository discussPostRepository;
    @Autowired
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    //提交新产生的帖子
    public void saveDiscussPost(DiscussPost discussPost){
        discussPostRepository.save(discussPost);
    }

    //删除帖子
    public void deleteDiscussPost(int id){
        discussPostRepository.deleteById(id);
    }

    //搜索功能
    public Map<String,Object> searchDiscussPost(String keyword , int current , int limit){
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //获取查询到的discussPost
        ArrayList<DiscussPost> list = new ArrayList<>();
        SearchHits<DiscussPost> hits = elasticsearchRestTemplate.search(searchQuery, DiscussPost.class);
        List<SearchHit<DiscussPost>> searchHits = hits.getSearchHits();
        for (SearchHit hit : searchHits){
            DiscussPost discussPost = new DiscussPost();
            discussPost = (DiscussPost) hit.getContent();
            list.add(discussPost);
            //处理高亮显示
            List<String> title = hit.getHighlightField("title");
            if (!title.isEmpty()){
                discussPost.setTitle(title.get(0));
            }
            List<String> content = hit.getHighlightField("content");
            if (!content.isEmpty()){
                discussPost.setContent(content.get(0));
            }
        }
        int  rows = (int)hits.getTotalHits();
        HashMap<String, Object> map = new HashMap<>();
        map.put("searchResult",list);
        map.put("rows",rows);
        return map;
    }
}
