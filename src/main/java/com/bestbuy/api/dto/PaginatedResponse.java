package com.bestbuy.api.dto;

import java.util.List;

public class PaginatedResponse<T> {

    private long total;
    private int limit;
    private int skip;
    private List<T> data;

    public PaginatedResponse(List<T> data, long total, int limit, int skip) {
        this.data = data;
        this.total = total;
        this.limit = limit;
        this.skip = skip;
    }

    public long getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getSkip() { return skip; }
    public List<T> getData() { return data; }
}
