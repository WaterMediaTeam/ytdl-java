package com.github.kiulian.downloader.model.search;

import java.util.LinkedList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class SearchResultPlaylistDetails extends AbstractSearchResultList {

    private final String playlistId;
    private final int videoCount;

    public SearchResultPlaylistDetails(final JSONObject json) {
        super(json);
        this.playlistId = json.getString("playlistId");
        final JSONArray thumbnailGroups = json.getJSONArray("thumbnails");
        this.thumbnails = new LinkedList<>();
        for (int i = 0; i < thumbnailGroups.size(); i++) {
            this.thumbnails.addAll(Utils.parseThumbnails(thumbnailGroups.getJSONObject(i)));
        }
        if (json.containsKey("videoCount")) {
            this.videoCount = Integer.parseInt(json.getString("videoCount"));
        } else {
            this.videoCount = -1;
        }
    }

    @Override
    public SearchResultItemType type() {
        return SearchResultItemType.PLAYLIST;
    }

    @Override
    public SearchResultPlaylistDetails asPlaylist() {
        return this;
    }

    public String playlistId() {
        return this.playlistId;
    }

    public int videoCount() {
        return this.videoCount;
    }
}
