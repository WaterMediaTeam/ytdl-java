package com.github.kiulian.downloader.model.search.query;

import com.alibaba.fastjson.JSONObject;

public abstract class Searchable {

    protected final String query;
    protected final String searchPath;

    protected abstract String extractQuery(JSONObject json);
    protected abstract String extractSearchPath(JSONObject json);

    public Searchable(final JSONObject json) {
        super();
        this.query = this.extractQuery(json);
        this.searchPath = this.extractSearchPath(json);
    }

    public String query() {
        return this.query;
    }

    public String searchPath() {
        return this.searchPath;
    }

}
