package com.github.kiulian.downloader.model.playlist;


import java.util.List;

import com.github.kiulian.downloader.model.Filter;

public class PlaylistInfo {

    private final PlaylistDetails details;
    private final List<PlaylistVideoDetails> videos;

    public PlaylistInfo(final PlaylistDetails details, final List<PlaylistVideoDetails> videos) {
        this.details = details;
        this.videos = videos;
    }

    public PlaylistDetails details() {
        return this.details;
    }

    public List<PlaylistVideoDetails> videos() {
        return this.videos;
    }

    public PlaylistVideoDetails findVideoById(final String videoId) {
        for (final PlaylistVideoDetails video : this.videos) {
            if (video.videoId().equals(videoId))
                return video;
        }
        return null;
    }

    public List<PlaylistVideoDetails> findVideos(final Filter<PlaylistVideoDetails> filter) {
        return filter.select(this.videos);
    }
}
