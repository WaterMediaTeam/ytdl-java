package com.github.kiulian.downloader.model.search;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.AbstractListVideoDetails;
import com.github.kiulian.downloader.model.Utils;

public class SearchResultVideoDetails extends AbstractListVideoDetails implements SearchResultItem {

    private final boolean isMovie;
    private String description;
    private String viewCountText;
    private long viewCount;
    // Scheduled diffusion (seconds time stamp)
    private long startTime;
    // Subtitled, CC, ...
    private List<String> badges;
    // Animated images
    private List<String> richThumbnails;

    public SearchResultVideoDetails(final JSONObject json, final boolean isMovie) {
        super(json);
        this.isMovie = isMovie;
        if (json.containsKey("lengthText")) {
            final String lengthText = json.getJSONObject("lengthText").getString("simpleText");
            this.lengthSeconds = Utils.parseLengthSeconds(lengthText);
        }
        if (isMovie) {
            this.description = Utils.parseRuns(json.getJSONObject("descriptionSnippet"));
        } else if (json.containsKey("detailedMetadataSnippets")) {
            this.description = Utils.parseRuns(json.getJSONArray("detailedMetadataSnippets")
                    .getJSONObject(0)
                    .getJSONObject("snippetText"));
        }
        if (json.containsKey("upcomingEventData")) {
            final String startTimeText = json.getJSONObject("upcomingEventData").getString("startTime");
            this.startTime = Long.parseLong(startTimeText);
            this.viewCount = -1;
        } else if (json.containsKey("viewCountText")) {
            final JSONObject jsonCount = json.getJSONObject("viewCountText");
            if (jsonCount.containsKey("simpleText")) {
                this.viewCountText = jsonCount.getString("simpleText");
                this.viewCount = Utils.parseViewCount(this.viewCountText);
            } else if (jsonCount.containsKey("runs")) {
                this.viewCountText = Utils.parseRuns(jsonCount);
                this.viewCount = -1;
            }
        }
        if (json.containsKey("badges")) {
            final JSONArray jsonBadges = json.getJSONArray("badges");
            this.badges = new ArrayList<>(jsonBadges.size());
            for (int i = 0; i < jsonBadges.size(); i++) {
                final JSONObject jsonBadge = jsonBadges.getJSONObject(i);
                if (jsonBadge.containsKey("metadataBadgeRenderer")) {
                    this.badges.add(jsonBadge.getJSONObject("metadataBadgeRenderer").getString("label"));
                }
            }
        }
        if (json.containsKey("richThumbnail")) {
            try {
                final JSONArray jsonThumbs = json.getJSONObject("richThumbnail")
                        .getJSONObject("movingThumbnailRenderer")
                        .getJSONObject("movingThumbnailDetails")
                        .getJSONArray("thumbnails");
                this.richThumbnails = new ArrayList<>(jsonThumbs.size());
                for (int i = 0; i < jsonThumbs.size(); i++) {
                    this.richThumbnails.add(jsonThumbs.getJSONObject(i).getString("url"));
                }
            } catch (final NullPointerException ignored) {}
        }
    }

    @Override
    public SearchResultItemType type() {
        return SearchResultItemType.VIDEO;
    }

    @Override
    public SearchResultVideoDetails asVideo() {
        return this;
    }

    public boolean isMovie() {
        return this.isMovie;
    }

    public boolean isLive() {
        return this.viewCount == -1;
    }

    public String viewCountText() {
        return this.viewCountText;
    }

    public long viewCount() {
        return this.viewCount;
    }

    public long startTime() {
        return this.startTime;
    }

    public List<String> badges() {
        return this.badges;
    }

    public List<String> richThumbnails() {
        return this.richThumbnails;
    }

    public String description() {
        return this.description;
    }
}
