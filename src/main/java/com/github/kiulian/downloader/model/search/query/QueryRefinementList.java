package com.github.kiulian.downloader.model.search.query;

import java.util.ArrayList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@SuppressWarnings("serial")
public class QueryRefinementList extends ArrayList<QueryRefinement> implements QueryElement {

    private final String title;

    public QueryRefinementList(final JSONObject json) {
        super(json.getJSONArray("cards").size());
        this.title = json.getJSONObject("header")
                .getJSONObject("richListHeaderRenderer")
                .getJSONObject("title")
                .getString("simpleText");
        final JSONArray jsonCards = json.getJSONArray("cards");
        for (int i = 0; i < jsonCards.size(); i++) {
            final JSONObject jsonRenderer = jsonCards.getJSONObject(i).getJSONObject("searchRefinementCardRenderer");
            this.add(new QueryRefinement(jsonRenderer));
        }
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public QueryElementType type() {
        return QueryElementType.REFINEMENT_LIST;
    }
}
