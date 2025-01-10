package com.github.kiulian.downloader.model.search;

public class SearchContinuation {

    private final String token;
    private final String clientVersion;
    private final String clickTrackingParameters;

    public SearchContinuation(final String token, final String clientVersion, final String clickTrackingParameters) {
        this.token = token;
        this.clientVersion = clientVersion;
        this.clickTrackingParameters = clickTrackingParameters;
    }

    public String token() {
        return this.token;
    }

    public String clientVersion() {
        return this.clientVersion;
    }

    public String clickTrackingParameters() {
        return this.clickTrackingParameters;
    }
}
