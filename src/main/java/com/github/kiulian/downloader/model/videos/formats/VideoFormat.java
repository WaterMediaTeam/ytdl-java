package com.github.kiulian.downloader.model.videos.formats;




import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

public class VideoFormat extends Format {

    private final int fps;
    private final String qualityLabel;
    private final Integer width;
    private final Integer height;
    private final VideoQuality videoQuality;

    public VideoFormat(final JSONObject json, final boolean isAdaptive, final String clientVersion) {
        super(json, isAdaptive, clientVersion);
        this.fps = json.getInteger("fps");
        this.qualityLabel = json.getString("qualityLabel");
        if (json.containsKey("size")) {
            final String[] split = json.getString("size").split("x");
            this.width = Integer.parseInt(split[0]);
            this.height = Integer.parseInt(split[1]);
        } else {
            this.width = json.getInteger("width");
            this.height = json.getInteger("height");
        }
        VideoQuality videoQuality = null;
        if (json.containsKey("quality")) {
            try {
                videoQuality = VideoQuality.valueOf(json.getString("quality"));
            } catch (final IllegalArgumentException ignore) {
            }
        }
        this.videoQuality = videoQuality;
    }

    @Override
    public String type() {
        return VIDEO;
    }

    public int fps() {
        return this.fps;
    }

    public VideoQuality videoQuality() {
        return this.videoQuality != null ? this.videoQuality : this.itag.videoQuality();
    }

    public String qualityLabel() {
        return this.qualityLabel;
    }

    public Integer width() {
        return this.width;
    }

    public Integer height() {
        return this.height;
    }

}
