package com.github.kiulian.downloader.model.subtitles;



import com.github.kiulian.downloader.model.Extension;


public class Subtitles {

    private final String url;
    private final boolean fromCaptions;
    private Extension format;
    private String translationLanguage;

    Subtitles(final String url, final boolean fromCaptions) {
        this.url = url;
        this.fromCaptions = fromCaptions;
    }

    public Subtitles formatTo(final Extension extension) {
        this.format = extension;
        return this;
    }

    public Subtitles translateTo(final String language) {
        // currently translation is supported only for subtitles from captions
        if (this.fromCaptions) {
            this.translationLanguage = language;
        }
        return this;
    }

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
