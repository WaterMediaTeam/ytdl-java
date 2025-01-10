package com.github.kiulian.downloader.downloader.request;


public class RequestWebpage extends RequestRaw<RequestWebpage> {

    protected final String url;
    private final String method;
    private final String body;

    public RequestWebpage(final String url) {
        this(url, "GET", null);
    }

    public RequestWebpage(final String url, final String method, final String body) {
        this.url = url;
        this.method = method;
        this.body = body;
    }

    @Override
    public String getDownloadUrl() {
        return this.url;
    }

    public String getMethod() {
        return this.method;
    }

    public String getBody() {
        return this.body;
    }
}
