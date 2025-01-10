package com.github.kiulian.downloader.model.search.field;

public enum TypeField implements SearchField {
    VIDEO(2, 1),
    CHANNEL(2, 2),
    PLAYLIST(2, 3),
    MOVIE(2, 4);

    private final byte[] data;

    TypeField(final int... data) {
        this.data = SearchField.convert(data);
    }

    @Override
    public byte[] data() {
        return this.data;
    }
}
