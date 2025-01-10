package com.github.kiulian.downloader.model.search;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public abstract class AbstractSearchResultList implements SearchResultItem {

    private String title;
    protected List<String> thumbnails;
    private String author;

    public AbstractSearchResultList() {}

    public AbstractSearchResultList(final JSONObject json) {
        this.title = json.getJSONObject("title").getString("simpleText");
        this.author = Utils.parseRuns(json.getJSONObject("shortBylineText"));
    }

    @Override
    public String title() {
        return this.title;
    }

    public List<String> thumbnails() {
        return this.thumbnails;
    }

    public String author() {
        return this.author;
    }
}
