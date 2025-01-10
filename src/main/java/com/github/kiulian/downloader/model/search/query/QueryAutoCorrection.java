package com.github.kiulian.downloader.model.search.query;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class QueryAutoCorrection implements QueryElement {

    private final String query;

    public QueryAutoCorrection(final JSONObject json) {
        this.query = Utils.parseRuns(json.getJSONObject("correctedQuery"));
    }

    @Override
    public String title() {
        return null;
    }

    public String query() {
        return this.query;
    }

    @Override
    public QueryElementType type() {
        return QueryElementType.AUTO_CORRECTION;
    }
}
