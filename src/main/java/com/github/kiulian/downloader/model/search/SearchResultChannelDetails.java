package com.github.kiulian.downloader.model.search;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class SearchResultChannelDetails extends AbstractSearchResultList {

    private final String channelId;
    private final String videoCountText;
    private final String subscriberCountText;
    private final String description;

    public SearchResultChannelDetails(final JSONObject json) {
        super(json);
        this.channelId = json.getString("channelId");
        this.videoCountText = Utils.parseRuns(json.getJSONObject("videoCountText"));
        if (json.containsKey("subscriberCountText")) {
            this.subscriberCountText = json.getJSONObject("subscriberCountText").getString("simpleText");
        } else {
            this.subscriberCountText = null;
        }
        this.description = Utils.parseRuns(json.getJSONObject("descriptionSnippet"));
        this.thumbnails = Utils.parseThumbnails(json.getJSONObject("thumbnail"));
    }

    @Override
    public SearchResultItemType type() {
        return SearchResultItemType.CHANNEL;
    }

    @Override
    public SearchResultChannelDetails asChannel() {
        return this;
    }
    public String channelId() {
        return this.channelId;
    }

    public String videoCountText() {
        return this.videoCountText;
    }

    public String subscriberCountText() {
        return this.subscriberCountText;
    }

    public String description() {
        return this.description;
    }
}
