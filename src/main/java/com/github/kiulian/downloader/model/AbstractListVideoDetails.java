package com.github.kiulian.downloader.model;

import com.alibaba.fastjson.JSONObject;

// Video item of a list (playlist, or search result).
public class AbstractListVideoDetails extends AbstractVideoDetails {

    public AbstractListVideoDetails(final JSONObject json) {
        super(json);
        this.author = Utils.parseRuns(json.getJSONObject("shortBylineText"));
        final JSONObject jsonTitle = json.getJSONObject("title");
        if (jsonTitle.containsKey("simpleText")) {
            this.title = jsonTitle.getString("simpleText");
        } else {
            this.title = Utils.parseRuns(jsonTitle);
        }
    }
}
