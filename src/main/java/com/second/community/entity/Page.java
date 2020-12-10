package com.second.community.entity;

//封装分页的内容
public class Page {
    //当前的页码
    private int current = 1;
    //显示的上限
    private int limit = 10;
    //数据的总数
    private int rows;
    //查询路径(复用分页的路径)
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 50) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    //返回当前页的起始页码
    public int getOffset() {
        return (current - 1) * limit;
    }

    //获取总的页数
    public int getTotal() {
        return rows % limit == 0 ? rows / limit : rows / limit + 1;
    }


    //获取显示出来的起始页码和终于页码
    public int getFrom() {
        return this.getTo()>4 ?this.getTo()-4:1;
    }

    public int getTo() {
        int to  = current + 2;
        int total = this.getTotal();
        return total > 5 && to <5 ? 5: (total > to?to:total);
    }
}
