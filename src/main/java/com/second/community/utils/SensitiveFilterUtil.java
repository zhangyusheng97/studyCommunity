package com.second.community.utils;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilterUtil {

    //用于替换的常量
    private static final String REPLACEMENT = "***";

    //根节点
    private TrieNode rootNode = new TrieNode();

    //该注解表示在构造前就被调用的方法
    @PostConstruct
    private void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

        ) {
            String keyword;
            while ((keyword = bufferedReader.readLine()) != null) {
                //将敏感词添加到前缀树中
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //将一个敏感词添加到前缀树中去
    private void addKeyword(String keyword) {
        //默认一个临时节点指向根节点
        TrieNode temp = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = temp.getSubNode(c);
            if (subNode == null){
                //初始化子节点
                subNode = new TrieNode();
                temp.addSubNode(c,subNode);
            }
            temp = subNode;
            //设置结束的标识
            if (i == keyword.length() - 1){
                temp.setKeywordEnd(true);
            }
        }
    }


    /**
     * 进行敏感词过滤的方法
     * @param text 待过滤的文本
     * @return    过滤好的文本
     */
    public String filter(String text){
        //判断输入是否为空
        if (StringUtils.isBlank(text)){
            return null;
        }
        //设置指针，1:指向前缀树
        TrieNode temp = rootNode;
        //2:指向输入的字符串的首
        int start = 0;
        //3:指向输入的字符串，会来回移动
        int end = 0;
        //最后输出的结果
        StringBuilder sb = new StringBuilder();
        while (end != text.length()){
            char c = text.charAt(end);
            //判断是不是特殊字符
            if (isSymbol(c)){
                //若此时为根节点
                if (temp == rootNode){
                    sb.append(c);
                    start++;
                }
                end++;
                continue;
            }
            //不是特殊字符时,检查下一级的节点
            temp = temp.getSubNode(c);
            if (temp == null){
                //该词不是敏感词
                sb.append(text.charAt(start));
                end = ++start;
                temp = rootNode; //重新指向根节点
            }else if (temp.isKeywordEnd){
                sb.append(REPLACEMENT);
                start = ++ end;
                temp = rootNode;
            }else {
                //到了结尾
                end++;//结束循环
            }
        }
        //将最后一批字符加入
        sb.append(text.substring(start));
        return sb.toString();
    }

    //判断是否为特殊符号
    private boolean isSymbol(Character c){
        //判断是否为合法的字符
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);//该范围为东亚的文字范围
    }

    //定义内部类(前缀树)
    private class TrieNode {
        //关键词结束的标识
        private boolean isKeywordEnd = false;

        //子节点(key是子节点的字符，value是子节点)
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        //判断是否是结尾
        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        //设置是否是结束
        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点的方法
        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }

        //获取子节点的方法
        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }


}
