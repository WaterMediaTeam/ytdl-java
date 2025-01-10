package com.github.kiulian.downloader.model.search.query;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class QuerySuggestion extends Searchable implements QueryElement {

    private final String title;

    public QuerySuggestion(final JSONObject json) {
        super(json);
        this.title = Utils.parseRuns(json.getJSONObject("didYouMean"));
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public QueryElementType type() {
        return QueryElementType.SUGGESTION;
    }

    @Override
    protected String extractQuery(final JSONObject json) {
        return Utils.parseRuns(json.getJSONObject("correctedQuery"));
    }

    @Override
    protected String extractSearchPath(final JSONObject json) {
        return json.getJSONObject("correctedQueryEndpoint")
                .getJSONObject("commandMetadata")
                .getJSONObject("webCommandMetadata")
                .getString("url");
    }
}
