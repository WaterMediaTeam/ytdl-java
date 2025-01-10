package com.github.kiulian.downloader.model.search;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class SearchResultShelf implements SearchResultItem {

    private final String title;
    private final List<SearchResultVideoDetails> videos;

    public SearchResultShelf(final JSONObject json) {
        this.title = json.getJSONObject("title").getString("simpleText");
        final JSONObject jsonContent = json.getJSONObject("content");
        
        // verticalListRenderer / horizontalMovieListRenderer
        final String contentRendererKey = jsonContent.keySet().iterator().next();
        final boolean isMovieShelf = contentRendererKey.contains("Movie");
        final JSONArray jsonItems = jsonContent.getJSONObject(contentRendererKey).getJSONArray("items");
        this.videos = new ArrayList<>(jsonItems.size());
        for (int i = 0; i < jsonItems.size(); i++) {
            final JSONObject jsonItem = jsonItems.getJSONObject(i);
            final String itemRendererKey = jsonItem.keySet().iterator().next();
            this.videos.add(new SearchResultVideoDetails(jsonItem.getJSONObject(itemRendererKey), isMovieShelf));
        }
    }

    @Override
    public SearchResultItemType type() {
        return SearchResultItemType.SHELF;
    }

    @Override
    public SearchResultShelf asShelf() {
        return this;
    }

    @Override
    public String title() {
        return this.title;
    }

    public List<SearchResultVideoDetails> videos() {
        return this.videos;
    }

}
