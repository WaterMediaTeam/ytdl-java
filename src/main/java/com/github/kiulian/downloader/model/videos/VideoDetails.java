package com.github.kiulian.downloader.model.videos;


import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.AbstractVideoDetails;

public class VideoDetails extends AbstractVideoDetails {

    private List<String> keywords;
    private String shortDescription;
    private long viewCount;
    private int averageRating;
    private boolean isLiveContent;
    private String liveUrl;

    public VideoDetails(final String videoId) {
        this.videoId = videoId;
    }

    public VideoDetails(final JSONObject json, final String liveHLSUrl) {
        super(json);
        this.title = json.getString("title");
        this.author = json.getString("author");
        this.isLive = json.getBooleanValue("isLive");

        this.keywords = json.containsKey("keywords") ? json.getJSONArray("keywords").toJavaList(String.class) : new ArrayList<String>();
        this.shortDescription = json.getString("shortDescription");
        this.averageRating = json.getIntValue("averageRating");
        this.viewCount = json.getLongValue("viewCount");
        this.isLiveContent = json.getBooleanValue("isLiveContent");
        this.liveUrl = liveHLSUrl;
    }

    @Override
    public boolean isDownloadable()  {
        return !this.isLive() && !(this.isLiveContent && this.lengthSeconds() == 0);
    }

    public List<String> keywords() {
        return this.keywords;
    }

    public String description() {
        return this.shortDescription;
    }

    public long viewCount() {
        return this.viewCount;
    }

    public int averageRating() {
        return this.averageRating;
    }

    public boolean isLiveContent() {
        return this.isLiveContent;
    }

    public String liveUrl() {
        return this.liveUrl;
    }
}
