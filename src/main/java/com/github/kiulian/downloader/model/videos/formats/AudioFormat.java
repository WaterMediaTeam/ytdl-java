package com.github.kiulian.downloader.model.videos.formats;


import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;

public class AudioFormat extends Format {

    private final Integer averageBitrate;
    private final Integer audioSampleRate;
    private final AudioQuality audioQuality;

    public AudioFormat(final JSONObject json, final boolean isAdaptive, final String clientVersion) {
        super(json, isAdaptive, clientVersion);
        this.audioSampleRate = json.getInteger("audioSampleRate");
        this.averageBitrate = json.getInteger("averageBitrate");

        AudioQuality audioQuality = null;
        if (json.containsKey("audioQuality")) {
            final String[] split = json.getString("audioQuality").split("_");
            final String quality = split[split.length - 1].toLowerCase();
            try {
                audioQuality = AudioQuality.valueOf(quality);
            } catch (final IllegalArgumentException ignore) {
            }
        }
        this.audioQuality = audioQuality;
    }

    @Override
    public String type() {
        return AUDIO;
    }

    public Integer averageBitrate() {
        return this.averageBitrate;
    }

    public AudioQuality audioQuality() {
        return this.audioQuality != null ? this.audioQuality : this.itag.audioQuality();
    }

    public Integer audioSampleRate() {
        return this.audioSampleRate;
    }
}
