package com.github.kiulian.downloader.model.playlist;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.AbstractListVideoDetails;

public class PlaylistVideoDetails extends AbstractListVideoDetails {

    private int index;
    private final boolean isPlayable;

    public PlaylistVideoDetails(final JSONObject json) {
        super(json);
        if (!this.thumbnails().isEmpty()) {
            // Otherwise, contains "/hqdefault.jpg?"
            this.isLive = this.thumbnails().get(0).contains("/hqdefault_live.jpg?");
        }

        if (json.containsKey("index")) {
            this.index = json.getJSONObject("index").getIntValue("simpleText");
        }
        this.isPlayable = json.getBooleanValue("isPlayable");
    }

    @Override
    protected boolean isDownloadable() {
        return this.isPlayable && super.isDownloadable();
    }

    public int index() {
        return this.index;
    }

    public boolean isPlayable() {
        return this.isPlayable;
    }
}
