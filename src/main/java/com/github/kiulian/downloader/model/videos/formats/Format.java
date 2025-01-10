package com.github.kiulian.downloader.model.videos.formats;




import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Extension;

public abstract class Format {

    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String AUDIO_VIDEO = "audio/video";

    private final boolean isAdaptive;

    protected final Itag itag;
    protected final String url;
    protected final String mimeType;
    protected final Extension extension;
    protected final Integer bitrate;
    protected final Long contentLength;
    protected final Long lastModified;
    protected final Long approxDurationMs;
    protected final String clientVersion;

    protected Format(final JSONObject json, final boolean isAdaptive, final String clientVersion) {
        this.isAdaptive = isAdaptive;
        this.clientVersion = clientVersion;

        Itag itag;
        try {
            itag = Itag.valueOf("i" + json.getInteger("itag"));
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
            itag = Itag.unknown;
            itag.setId(json.getIntValue("itag"));
        }
        this.itag = itag;

        this.url = json.getString("url").replace("\\u0026", "&");
        this.mimeType = json.getString("mimeType");
        this.bitrate = json.getInteger("bitrate");
        this.contentLength = json.getLong("contentLength");
        this.lastModified = json.getLong("lastModified");
        this.approxDurationMs = json.getLong("approxDurationMs");

        if (this.mimeType == null || this.mimeType.isEmpty()) {
            this.extension = Extension.UNKNOWN;
        } else if (this.mimeType.contains(Extension.MPEG4.value())) {
            if (this instanceof AudioFormat)
                this.extension = Extension.M4A;
            else
                this.extension = Extension.MPEG4;
        } else if (this.mimeType.contains(Extension.WEBM.value())) {
            if (this instanceof AudioFormat)
                this.extension = Extension.WEBA;
            else
                this.extension = Extension.WEBM;
        } else if (this.mimeType.contains(Extension.FLV.value())) {
            this.extension = Extension.FLV;
        } else if (this.mimeType.contains(Extension._3GP.value())) {
            this.extension = Extension._3GP;
        } else {
            this.extension = Extension.UNKNOWN;
        }
    }

    public abstract String type();

    public boolean isAdaptive() {
        return this.isAdaptive;
    }

    public String clientVersion() {
        return this.clientVersion;
    }

    public Itag itag() {
        return this.itag;
    }

    public Integer bitrate() {
        return this.bitrate;
    }

    public String mimeType() {
        return this.mimeType;
    }

    public String url() {
        return this.url;
    }

    public Long contentLength() {
        return this.contentLength;
    }

    public long lastModified() {
        return this.lastModified;
    }

    public Long duration() {
        return this.approxDurationMs;
    }

    public Extension extension() {
        return this.extension;
    }
}
