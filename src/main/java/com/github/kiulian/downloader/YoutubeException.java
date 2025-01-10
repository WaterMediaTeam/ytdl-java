package com.github.kiulian.downloader;


public abstract class YoutubeException extends Exception {
    private YoutubeException(final String message) {
        super(message);
    }

    public static class DownloadException extends YoutubeException {

        public DownloadException(final String message) {
            super(message);
        }
    }

    public static class BadPageException extends YoutubeException {

        public BadPageException(final String message) {
            super(message);
        }
    }

    public static class CipherException extends YoutubeException {

        public CipherException(final String message) {
            super(message);
        }
    }

    public static class InvalidJsUrlException extends YoutubeException.CipherException {
        public InvalidJsUrlException(final String message) {
            super(message);
        }
    }

}
