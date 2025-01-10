package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

public class RequestSubtitlesDownload extends RequestWebpage {

    private Extension format;
    private String translationLanguage;
    private final boolean fromCaptions;

    public RequestSubtitlesDownload(final SubtitlesInfo subtitlesInfo) {
        super(subtitlesInfo.getUrl());
        this.fromCaptions = subtitlesInfo.isFromCaptions();
    }

    public RequestSubtitlesDownload formatTo(final Extension extension) {
        this.format = extension;
        return this;
    }

    public RequestSubtitlesDownload translateTo(final String language) {
        if (this.fromCaptions) {
            this.translationLanguage = language;
        }
        return this;
    }

    @Override
    public String getDownloadUrl() {
        String downloadUrl = this.url;
        if (this.format != null && this.format.isSubtitle()) {
            downloadUrl += "&fmt=" + this.format.value();
        }
        if (this.translationLanguage != null && !this.translationLanguage.isEmpty()) {
            downloadUrl += "&tlang=" + this.translationLanguage;
        }
        return downloadUrl;
    }

}
