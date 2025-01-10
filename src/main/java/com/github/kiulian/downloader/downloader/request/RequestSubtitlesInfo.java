package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;

import java.util.List;

public class RequestSubtitlesInfo extends Request<RequestSubtitlesInfo, List<SubtitlesInfo>> {

    private final String videoId;

    public RequestSubtitlesInfo(final String videoId) {
        this.videoId = videoId;
    }

    public String getVideoId() {
        return this.videoId;
    }
}
