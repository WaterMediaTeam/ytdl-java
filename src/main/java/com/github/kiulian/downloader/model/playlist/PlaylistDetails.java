package com.github.kiulian.downloader.model.playlist;


public class PlaylistDetails {

    private final String playlistId;
    private final String title;
    private final String author;
    private final int videoCount;
    private final long viewCount;

    public PlaylistDetails(final String playlistId, final String title, final String author, final int videoCount, final long viewCount) {
        super();
        this.playlistId = playlistId;
        this.title = title;
        this.author = author;
        this.videoCount = videoCount;
        this.viewCount = viewCount;
    }

    public String playlistId() {
        return this.playlistId;
    }

    public String title() {
        return this.title;
    }

    public String author() {
        return this.author;
    }

    public int videoCount() {
        return this.videoCount;
    }

    public long viewCount() {
        return this.viewCount;
    }
}
