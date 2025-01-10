package com.github.kiulian.downloader.model.videos.quality;



public enum AudioQuality {
    unknown(0),
    noAudio(0),
    low(1),
    medium(2),
    high(3);

    private final Integer order;

    AudioQuality(final int order) {
        this.order = order;
    }

    public int compare(final AudioQuality quality) {
        if (this == quality) return 0;
        return this.order.compareTo(quality.order);
    }
}
