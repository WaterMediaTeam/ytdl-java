package com.github.kiulian.downloader;



import static com.github.kiulian.downloader.model.Utils.createOutDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.github.kiulian.downloader.cipher.CachedCipherFactory;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.DownloaderImpl;
import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;
import com.github.kiulian.downloader.extractor.ExtractorImpl;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.parser.Parser;
import com.github.kiulian.downloader.parser.ParserImpl;

public class YoutubeDownloader {

    private final Config config;
    private final Downloader downloader;
    private final Parser parser;

    public YoutubeDownloader() {
        this(Config.buildDefault());
    }

    public YoutubeDownloader(final Config config) {
        this.config = config;
        this.downloader = new DownloaderImpl(config);
        this.parser = new ParserImpl(config, this.downloader, new ExtractorImpl(this.downloader), new CachedCipherFactory(this.downloader));
    }

    public YoutubeDownloader(final Config config, final Downloader downloader) {
        this(config, downloader, new ParserImpl(config, downloader, new ExtractorImpl(downloader), new CachedCipherFactory(downloader)));
    }

    public YoutubeDownloader(final Config config, final Downloader downloader, final Parser parser) {
        this.config = config;
        this.parser = parser;
        this.downloader = downloader;
    }

    public Config getConfig() {
        return this.config;
    }

    public Response<VideoInfo> getVideoInfo(final RequestVideoInfo request) {
        return this.parser.parseVideo(request);
    }

    public Response<List<SubtitlesInfo>> getSubtitlesInfo(final RequestSubtitlesInfo request) {
        return this.parser.parseSubtitlesInfo(request);
    }

    public Response<PlaylistInfo> getChannelUploads(final RequestChannelUploads request) {
        return this.parser.parseChannelsUploads(request);
    }

    public Response<PlaylistInfo> getPlaylistInfo(final RequestPlaylistInfo request) {
        return this.parser.parsePlaylist(request);
    }

    public Response<SearchResult> search(final RequestSearchResult request) {
        return this.parser.parseSearchResult(request);
    }

    public Response<SearchResult> searchContinuation(final RequestSearchContinuation request) {
        return this.parser.parseSearchContinuation(request);
    }

    public Response<SearchResult> search(final RequestSearchable request) {
        return this.parser.parseSearcheable(request);
    }

    public Response<File> downloadVideoFile(final RequestVideoFileDownload request) {
        final File outDir = request.getOutputDirectory();
        try {
            createOutDir(outDir);
        } catch (final IOException e) {
            return ResponseImpl.error(e);
        }

        return this.downloader.downloadVideoAsFile(request);
    }

    public Response<Void> downloadVideoStream(final RequestVideoStreamDownload request) {
        return this.downloader.downloadVideoAsStream(request);
    }

    public Response<String> downloadSubtitle(final RequestWebpage request) {
        return this.downloader.downloadWebpage(request);
    }

}
