package com.github.kiulian.downloader.model;


import java.util.List;

import com.alibaba.fastjson.JSONObject;

public abstract class AbstractVideoDetails {

    protected String videoId;
    private List<String> thumbnails;

    // Subclass specific extraction
    protected int lengthSeconds;
    protected String title;
    protected String author;
    protected boolean isLive;

    protected boolean isDownloadable() {
        return (!this.isLive() && this.lengthSeconds() != 0);
    }

    public AbstractVideoDetails() {
    }

    public AbstractVideoDetails(final JSONObject json) {
        this.videoId = json.getString("videoId");
        if (json.containsKey("lengthSeconds")) {
            this.lengthSeconds = json.getIntValue("lengthSeconds");
        }
        this.thumbnails = Utils.parseThumbnails(json.getJSONObject("thumbnail"));
    }

    public String videoId() {
        return this.videoId;
    }

    public String title() {
        return this.title;
    }

    public int lengthSeconds() {
        return this.lengthSeconds;
    }

    public List<String> thumbnails() {
        return this.thumbnails;
    }

    public String author() {
        return this.author;
    }

    public boolean isLive() {
        return this.isLive;
    }
}
