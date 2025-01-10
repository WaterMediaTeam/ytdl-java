package com.github.kiulian.downloader.model.search.query;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class QueryRefinement extends Searchable {

    private final List<String> thumbnails;

    public QueryRefinement(final JSONObject json) {
        super(json);
        this.thumbnails = Utils.parseThumbnails(json.getJSONObject("thumbnail"));
    }

    public List<String> thumbnails() {
        return this.thumbnails;
    }

    @Override
    protected String extractQuery(final JSONObject json) {
        return Utils.parseRuns(json.getJSONObject("query"));
    }

    @Override
    protected String extractSearchPath(final JSONObject json) {
        return json.getJSONObject("searchEndpoint")
                .getJSONObject("commandMetadata")
                .getJSONObject("webCommandMetadata")
                .getString("url");
    }

}
