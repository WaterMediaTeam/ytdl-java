package com.github.kiulian.downloader.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.BadPageException;
import com.github.kiulian.downloader.cipher.Cipher;
import com.github.kiulian.downloader.cipher.CipherFactory;
import com.github.kiulian.downloader.cipher.CipherFunction;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.client.Client;
import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;
import com.github.kiulian.downloader.extractor.Extractor;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.search.*;
import com.github.kiulian.downloader.model.search.query.*;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ParserImpl implements Parser {
    private static final String ANDROID_APIKEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    private static final String BASE_API_URL = "https://www.youtube.com/youtubei/v1";


    private static class DelegatedCipherFactory implements CipherFactory {
        Cipher lastCipher;
        final CipherFactory factory;

        DelegatedCipherFactory(final CipherFactory factory) {
            this.factory = factory;
        }

        @Override
        public Cipher createCipher(final String jsUrl) throws YoutubeException {
            if (jsUrl == null)
                return this.lastCipher;
            return this.lastCipher = this.factory.createCipher(jsUrl);

        }

        @Override
        public void addInitialFunctionPattern(final int priority, final String regex) {
            this.factory.addInitialFunctionPattern(priority, regex);
        }

        @Override
        public void addFunctionEquivalent(final String regex, final CipherFunction function) {
            this.factory.addFunctionEquivalent(regex, function);
        }

        Cipher getLastCipher() {
            return this.lastCipher;
        }

        void invalidateLastCipher() {
            this.lastCipher = null;
        }
    }

    private final Config config;
    private final Downloader downloader;
    private final Extractor extractor;
    private final DelegatedCipherFactory cipherFactory;


    public ParserImpl(final Config config, final Downloader downloader, final Extractor extractor, final CipherFactory cipherFactory) {
        this.config = config;
        this.downloader = downloader;
        this.extractor = extractor;
        this.cipherFactory = new DelegatedCipherFactory(cipherFactory);


    }

    @Override
    public Response<VideoInfo> parseVideo(final RequestVideoInfo request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<VideoInfo> result = executorService.submit(() -> this.parseVideo(request.getVideoId(), request.getCallback(), request.getClient()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final VideoInfo result = this.parseVideo(request.getVideoId(), request.getCallback(), request.getClient());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private VideoInfo parseVideo(final String videoId, final YoutubeCallback<VideoInfo> callback, final Client client) throws YoutubeException {
        // try to spoof android
        // workaround for issue https://github.com/sealedtx/java-youtube-downloader/issues/97
        VideoInfo videoInfo = this.parseVideoAndroid(videoId, callback, client);
        if (videoInfo == null) {
            videoInfo = this.parseVideoWeb(videoId, callback);
        }
        if (callback != null) {
            callback.onFinished(videoInfo);
        }
        return videoInfo;
    }

    private VideoInfo parseVideoAndroid(final String videoId, final YoutubeCallback<VideoInfo> callback, final Client client) throws YoutubeException {
        final String url = BASE_API_URL + "/player?key=" + ANDROID_APIKEY;


        final RequestWebpage request = new RequestWebpage(url, "POST", client.getBody().fluentPut("videoId", videoId).toJSONString())
                .header("Content-Type", "application/json");

        final Response<String> response = this.downloader.downloadWebpage(request);
        if (!response.ok()) {
            return null;
        }

        final JSONObject playerResponse;
        try {
            playerResponse = JSONObject.parseObject(response.data());
        } catch (final Exception ignore) {
            return null;
        }

        final VideoDetails videoDetails = this.parseVideoDetails(videoId, playerResponse);
        if (videoDetails.isDownloadable()) {
            final JSONObject context = playerResponse.getJSONObject("responseContext");
            final String clientVersion = this.extractor.extractClientVersionFromContext(context);
            List<Format> formats;
            try {
                formats = this.parseFormats(playerResponse, null, clientVersion);
            } catch (final YoutubeException.InvalidJsUrlException e) {
                final JSONObject playerConfig = this.downloadPlayerConfig(videoId, callback);
                final String jsUrl;
                try {
                    jsUrl = this.extractor.extractJsUrlFromConfig(playerConfig, videoId);
                    formats = this.parseFormats(playerResponse, jsUrl, clientVersion);
                } catch (final YoutubeException ex) {
                    if (callback != null) {
                        callback.onError(ex);
                    }
                    throw ex;
                }
            } catch (final YoutubeException e) {
                if (callback != null) {
                    callback.onError(e);
                }
                throw e;
            }

            final List<SubtitlesInfo> subtitlesInfo = this.parseCaptions(playerResponse);
            return new VideoInfo(videoDetails, formats, subtitlesInfo);
        } else {
            return new VideoInfo(videoDetails, Collections.emptyList(), Collections.emptyList());
        }

    }

    private JSONObject downloadPlayerConfig(final String videoId, final YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        final String htmlUrl = "https://www.youtube.com/watch?v=" + videoId;

        final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage(htmlUrl));
        if (!response.ok()) {
            final YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", htmlUrl, response.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        final String html = response.data();

        final JSONObject playerConfig;
        try {
            playerConfig = this.extractor.extractPlayerConfigFromHtml(html);
        } catch (final YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        return playerConfig;
    }

    private VideoInfo parseVideoWeb(final String videoId, final YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        final JSONObject playerConfig = this.downloadPlayerConfig(videoId, callback);

        final JSONObject args = playerConfig.getJSONObject("args");
        final JSONObject playerResponse = args.getJSONObject("player_response");

        if (!playerResponse.containsKey("streamingData") && !playerResponse.containsKey("videoDetails")) {
            final YoutubeException e = new YoutubeException.BadPageException("streamingData and videoDetails not found");
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        final VideoDetails videoDetails = this.parseVideoDetails(videoId, playerResponse);
        if (videoDetails.isDownloadable()) {
            final String jsUrl;
            try {
                jsUrl = this.extractor.extractJsUrlFromConfig(playerConfig, videoId);
            } catch (final YoutubeException e) {
                if (callback != null) {
                    callback.onError(e);
                }
                throw e;
            }
            final JSONObject context = playerConfig.getJSONObject("args").getJSONObject("player_response").getJSONObject("responseContext");
            final String clientVersion = this.extractor.extractClientVersionFromContext(context);
            final List<Format> formats;
            try {
                formats = this.parseFormats(playerResponse, jsUrl, clientVersion);
            } catch (final YoutubeException e) {
                if (callback != null) {
                    callback.onError(e);
                }
                throw e;
            }
            final List<SubtitlesInfo> subtitlesInfo = this.parseCaptions(playerResponse);
            return new VideoInfo(videoDetails, formats, subtitlesInfo);
        } else {
            return new VideoInfo(videoDetails, Collections.emptyList(), Collections.emptyList());
        }
    }

    private VideoDetails parseVideoDetails(final String videoId, final JSONObject playerResponse) {
        if (!playerResponse.containsKey("videoDetails")) {
            return new VideoDetails(videoId);
        }

        final JSONObject videoDetails = playerResponse.getJSONObject("videoDetails");
        String liveHLSUrl = null;
        if (videoDetails.getBooleanValue("isLive")) {
            if (playerResponse.containsKey("streamingData")) {
                liveHLSUrl = playerResponse.getJSONObject("streamingData").getString("hlsManifestUrl");
            }
        }
        return new VideoDetails(videoDetails, liveHLSUrl);
    }

    private List<Format> parseFormats(final JSONObject playerResponse, final String jsUrl, final String clientVersion) throws YoutubeException {
        if (!playerResponse.containsKey("streamingData")) {
            throw new YoutubeException.BadPageException("streamingData not found");
        }

        final JSONObject streamingData = playerResponse.getJSONObject("streamingData");
        final JSONArray jsonFormats = new JSONArray();
        if (streamingData.containsKey("formats")) {
            jsonFormats.addAll(streamingData.getJSONArray("formats"));
        }
        final JSONArray jsonAdaptiveFormats = new JSONArray();
        if (streamingData.containsKey("adaptiveFormats")) {
            jsonAdaptiveFormats.addAll(streamingData.getJSONArray("adaptiveFormats"));
        }

        final List<Format> formats = new ArrayList<>(jsonFormats.size() + jsonAdaptiveFormats.size());
        this.populateFormats(formats, jsonFormats, jsUrl, false, clientVersion);
        this.populateFormats(formats, jsonAdaptiveFormats, jsUrl, true, clientVersion);
        return formats;
    }

    private void populateFormats(final List<Format> formats, final JSONArray jsonFormats, final String jsUrl, final boolean isAdaptive, final String clientVersion) throws YoutubeException.CipherException {
        for (int i = 0; i < jsonFormats.size(); i++) {
            final JSONObject json = jsonFormats.getJSONObject(i);
            if ("FORMAT_STREAM_TYPE_OTF".equals(json.getString("type")))
                continue; // unsupported otf formats which cause 404 not found

            final int itagValue = json.getIntValue("itag");
            final Itag itag;
            try {
                itag = Itag.valueOf("i" + itagValue);
            } catch (final IllegalArgumentException e) {
                System.err.println("Error parsing format: unknown itag " + itagValue);
                continue;
            }

            try {
                final Format format = this.parseFormat(json, jsUrl, itag, isAdaptive, clientVersion);
                formats.add(format);
            } catch (final YoutubeException.CipherException e) {
                throw e;
            } catch (final YoutubeException e) {
                System.err.println("Error " + e.getMessage() + " parsing format: " + json);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }


    private Format parseFormat(final JSONObject json, final String jsUrl, final Itag itag, final boolean isAdaptive, final String clientVersion) throws YoutubeException {
        if (json.containsKey("signatureCipher")) {
            final JSONObject jsonCipher = new JSONObject();
            final String[] cipherData = json.getString("signatureCipher").replace("\\u0026", "&").split("&");
            for (final String s : cipherData) {
                final String[] keyValue = s.split("=");
                jsonCipher.put(keyValue[0], keyValue[1]);
            }
            if (!jsonCipher.containsKey("url")) {
                throw new YoutubeException.BadPageException("Could not found url in cipher data");
            }
            String urlWithSig = jsonCipher.getString("url");
            try {
                urlWithSig = URLDecoder.decode(urlWithSig, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (urlWithSig.contains("signature")
                    || (!jsonCipher.containsKey("s") && (urlWithSig.contains("&sig=") || urlWithSig.contains("&lsig=")))) {
                // do nothing, this is pre-signed videos with signature
            } else if (jsUrl != null || this.cipherFactory.getLastCipher() != null) {
                String s = jsonCipher.getString("s");
                try {
                    s = URLDecoder.decode(s, "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                final Cipher cipher = this.cipherFactory.createCipher(jsUrl);

                final String signature = cipher.getSignature(s);
                final String decipheredUrl = urlWithSig + "&sig=" + signature;
                json.put("url", decipheredUrl);
            } else {

                throw new YoutubeException.InvalidJsUrlException("deciphering is required but no js url");

            }
        }

        final boolean hasVideo = itag.isVideo() || json.containsKey("size") || json.containsKey("width");
        final boolean hasAudio = itag.isAudio() || json.containsKey("audioQuality");

        if (hasVideo && hasAudio)
            return new VideoWithAudioFormat(json, isAdaptive, clientVersion);
        else if (hasVideo)
            return new VideoFormat(json, isAdaptive, clientVersion);
        return new AudioFormat(json, isAdaptive, clientVersion);
    }

    private List<SubtitlesInfo> parseCaptions(final JSONObject playerResponse) {
        if (!playerResponse.containsKey("captions")) {
            return Collections.emptyList();
        }
        final JSONObject captions = playerResponse.getJSONObject("captions");

        final JSONObject playerCaptionsTracklistRenderer = captions.getJSONObject("playerCaptionsTracklistRenderer");
        if (playerCaptionsTracklistRenderer == null || playerCaptionsTracklistRenderer.isEmpty()) {
            return Collections.emptyList();
        }

        final JSONArray captionsArray = playerCaptionsTracklistRenderer.getJSONArray("captionTracks");
        if (captionsArray == null || captionsArray.isEmpty()) {
            return Collections.emptyList();
        }

        final List<SubtitlesInfo> subtitlesInfo = new ArrayList<>();
        for (int i = 0; i < captionsArray.size(); i++) {
            final JSONObject subtitleInfo = captionsArray.getJSONObject(i);
            final String language = subtitleInfo.getString("languageCode");
            final String url = subtitleInfo.getString("baseUrl");
            final String vssId = subtitleInfo.getString("vssId");

            if (language != null && url != null && vssId != null) {
                final boolean isAutoGenerated = vssId.startsWith("a.");
                subtitlesInfo.add(new SubtitlesInfo(url, language, isAutoGenerated, true));
            }
        }
        return subtitlesInfo;
    }

    @Override
    public Response<PlaylistInfo> parsePlaylist(final RequestPlaylistInfo request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<PlaylistInfo> result = executorService.submit(() -> this.parsePlaylist(request.getPlaylistId(), request.getCallback(), request.getClient()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final PlaylistInfo result = this.parsePlaylist(request.getPlaylistId(), request.getCallback(), request.getClient());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }

    }

    private PlaylistInfo parsePlaylist(final String playlistId, final YoutubeCallback<PlaylistInfo> callback, final Client client) throws YoutubeException {
        final String htmlUrl = "https://www.youtube.com/playlist?list=" + playlistId;

        final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage(htmlUrl));
        if (!response.ok()) {
            final YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", htmlUrl, response.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        final String html = response.data();

        final JSONObject initialData;
        try {
            initialData = this.extractor.extractInitialDataFromHtml(html);
        } catch (final YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        if (!initialData.containsKey("metadata")) {
            throw new YoutubeException.BadPageException("Invalid initial data json");
        }

        final PlaylistDetails playlistDetails = this.parsePlaylistDetails(playlistId, initialData);

        final List<PlaylistVideoDetails> videos;
        try {
            videos = this.parsePlaylistVideos(initialData, playlistDetails.videoCount(), client);
        } catch (final YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        return new PlaylistInfo(playlistDetails, videos);
    }

    private PlaylistDetails parsePlaylistDetails(final String playlistId, final JSONObject initialData) {
        final String title = initialData.getJSONObject("metadata")
                .getJSONObject("playlistMetadataRenderer")
                .getString("title");
        final JSONArray sideBarItems = initialData.getJSONObject("sidebar").getJSONObject("playlistSidebarRenderer").getJSONArray("items");
        String author = null;
        try {
            // try to retrieve author, some playlists may have no author
            author = sideBarItems.getJSONObject(1)
                    .getJSONObject("playlistSidebarSecondaryInfoRenderer")
                    .getJSONObject("videoOwner")
                    .getJSONObject("videoOwnerRenderer")
                    .getJSONObject("title")
                    .getJSONArray("runs")
                    .getJSONObject(0)
                    .getString("text");
        } catch (final Exception ignored) {
        }
        final JSONArray stats = sideBarItems.getJSONObject(0)
                .getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats");
        final int videoCount = this.extractor.extractIntegerFromText(stats.getJSONObject(0).getJSONArray("runs").getJSONObject(0).getString("text"));
        final long viewCount = this.extractor.extractLongFromText(stats.getJSONObject(1).getString("simpleText"));

        return new PlaylistDetails(playlistId, title, author, videoCount, viewCount);
    }

    private List<PlaylistVideoDetails> parsePlaylistVideos(final JSONObject initialData, final int videoCount, final Client client) throws YoutubeException {
        final JSONObject content;

        try {
            content = initialData.getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs").getJSONObject(0)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents").getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents").getJSONObject(0)
                    .getJSONObject("playlistVideoListRenderer");
        } catch (final NullPointerException e) {
            throw new YoutubeException.BadPageException("Playlist initial data not found");
        }

        final List<PlaylistVideoDetails> videos;
        if (videoCount > 0) {
            videos = new ArrayList<>(videoCount);
        } else {
            videos = new LinkedList<>();
        }


        this.populatePlaylist(content, videos, client);
        return videos;
    }

    private void populatePlaylist(final JSONObject content, final List<PlaylistVideoDetails> videos, final Client client) throws YoutubeException {
        final JSONArray contents;
        if (content.containsKey("contents")) { // parse first items (up to 100)
            contents = content.getJSONArray("contents");
        } else if (content.containsKey("continuationItems")) { // parse continuationItems
            contents = content.getJSONArray("continuationItems");
        } else if (content.containsKey("continuations")) { // load continuation
            final JSONObject nextContinuationData = content.getJSONArray("continuations")
                    .getJSONObject(0)
                    .getJSONObject("nextContinuationData");
            final String continuation = nextContinuationData.getString("continuation");
            final String ctp = nextContinuationData.getString("clickTrackingParams");
            this.loadPlaylistContinuation(continuation, ctp, videos, client);
            return;
        } else { // nothing found
            return;
        }

        for (int i = 0; i < contents.size(); i++) {
            final JSONObject contentsItem = contents.getJSONObject(i);
            if (contentsItem.containsKey("playlistVideoRenderer")) {
                videos.add(new PlaylistVideoDetails(contentsItem.getJSONObject("playlistVideoRenderer")));
            } else {
                if (contentsItem.containsKey("continuationItemRenderer")) {
                    final JSONObject continuationEndpoint = contentsItem.getJSONObject("continuationItemRenderer")
                            .getJSONObject("continuationEndpoint");
                    final String continuation = continuationEndpoint.getJSONObject("continuationCommand").getString("token");
                    final String ctp = continuationEndpoint.getString("clickTrackingParams");
                    this.loadPlaylistContinuation(continuation, ctp, videos, client);
                }
            }
        }
    }

    private void loadPlaylistContinuation(final String continuation, final String ctp, final List<PlaylistVideoDetails> videos, final Client client) throws YoutubeException {
        final JSONObject content;
        final String url = BASE_API_URL + "/browse?key=" + ANDROID_APIKEY;
        final JSONObject body = client.getBody()
                .fluentPut("continuation", continuation)
                .fluentPut("clickTracking", new JSONObject()
                        .fluentPut("clickTrackingParams", ctp));


        final RequestWebpage request = new RequestWebpage(url, "POST", body.toJSONString())
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", client.getVersion())
                .header("Content-Type", "application/json");

        final Response<String> response = this.downloader.downloadWebpage(request);
        if (!response.ok()) {
            throw new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", url, response.error().getMessage()));
        }
        final String html = response.data();

        try {
            final JSONObject jsonResponse = JSON.parseObject(html);

            if (jsonResponse.containsKey("continuationContents")) {
                content = jsonResponse
                        .getJSONObject("continuationContents")
                        .getJSONObject("playlistVideoListContinuation");
            } else {
                content = jsonResponse.getJSONArray("onResponseReceivedActions")
                        .getJSONObject(0)
                        .getJSONObject("appendContinuationItemsAction");
            }
            this.populatePlaylist(content, videos, client);
        } catch (final YoutubeException e) {
            throw e;
        } catch (final Exception e) {
            throw new YoutubeException.BadPageException("Could not parse playlist continuation json");
        }
    }

    @Override
    public Response<PlaylistInfo> parseChannelsUploads(final RequestChannelUploads request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<PlaylistInfo> result = executorService.submit(() -> this.parseChannelsUploads(request.getChannelId(), request.getCallback(), request.getClient()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final PlaylistInfo result = this.parseChannelsUploads(request.getChannelId(), request.getCallback(), request.getClient());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private PlaylistInfo parseChannelsUploads(final String channelId, final YoutubeCallback<PlaylistInfo> callback, final Client client) throws YoutubeException {
        String playlistId = null;
        if (channelId.length() == 24 && channelId.startsWith("UC")) { // channel id pattern
            playlistId = "UU" + channelId.substring(2); // replace "UC" with "UU"
        } else { // channel name
            final String channelLink = "https://www.youtube.com/c/" + channelId + "/videos?view=57";

            final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage(channelLink));
            if (!response.ok()) {
                final YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", channelLink, response.error().getMessage()));
                if (callback != null) {
                    callback.onError(e);
                }
                throw e;
            }
            final String html = response.data();

            final Scanner scan = new Scanner(html);
            scan.useDelimiter("list=");
            while (scan.hasNext()) {
                final String pId = scan.next();
                if (pId.startsWith("UU")) {
                    playlistId = pId.substring(0, 24);
                    break;
                }
            }
        }
        if (playlistId == null) {
            final YoutubeException e = new YoutubeException.BadPageException("Upload Playlist not found");
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        return this.parsePlaylist(playlistId, callback, client);
    }

    @Override
    public Response<List<SubtitlesInfo>> parseSubtitlesInfo(final RequestSubtitlesInfo request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<List<SubtitlesInfo>> result = executorService.submit(() -> this.parseSubtitlesInfo(request.getVideoId(), request.getCallback()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final List<SubtitlesInfo> result = this.parseSubtitlesInfo(request.getVideoId(), request.getCallback());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private List<SubtitlesInfo> parseSubtitlesInfo(final String videoId, final YoutubeCallback<List<SubtitlesInfo>> callback) throws YoutubeException {
        final String xmlUrl = "https://video.google.com/timedtext?hl=en&type=list&v=" + videoId;

        final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage(xmlUrl));
        if (!response.ok()) {
            final YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", xmlUrl, response.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        final String xml = response.data();
        final List<String> languages;
        try {
            languages = this.extractor.extractSubtitlesLanguagesFromXml(xml);
        } catch (final YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        final List<SubtitlesInfo> subtitlesInfo = new ArrayList<>();
        for (final String language : languages) {
            final String url = String.format("https://www.youtube.com/api/timedtext?lang=%s&v=%s",
                    language, videoId);
            subtitlesInfo.add(new SubtitlesInfo(url, language, false));
        }

        return subtitlesInfo;
    }

    @Override
    public Response<SearchResult> parseSearchResult(final RequestSearchResult request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<SearchResult> result = executorService.submit(() -> this.parseSearchResult(request.query(), request.encodeParameters(), request.getCallback()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final SearchResult result = this.parseSearchResult(request.query(), request.encodeParameters(), request.getCallback());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    @Override
    public Response<SearchResult> parseSearchContinuation(final RequestSearchContinuation request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<SearchResult> result = executorService.submit(() -> this.parseSearchContinuation(request.continuation(), request.getCallback(), request.getClient()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final SearchResult result = this.parseSearchContinuation(request.continuation(), request.getCallback(), request.getClient());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    @Override
    public Response<SearchResult> parseSearcheable(final RequestSearchable request) {
        if (request.isAsync()) {
            final ExecutorService executorService = this.config.getExecutorService();
            final Future<SearchResult> result = executorService.submit(() -> this.parseSearchable(request.searchPath(), request.getCallback()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            final SearchResult result = this.parseSearchable(request.searchPath(), request.getCallback());
            return ResponseImpl.from(result);
        } catch (final YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private SearchResult parseSearchResult(final String query, final String parameters, final YoutubeCallback<SearchResult> callback) throws YoutubeException {
        String searchQuery;
        try {
            searchQuery = URLEncoder.encode(query, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            searchQuery = query;
            e.printStackTrace();
        }
        String url = "https://www.youtube.com/results?search_query=" + searchQuery;
        if (parameters != null) {
            url += "&sp=" + parameters;
        }
        try {
            return this.parseHtmlSearchResult(url);
        } catch (final YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
    }

    private SearchResult parseSearchable(final String searchPath, final YoutubeCallback<SearchResult> callback) throws YoutubeException {
        final String url = "https://www.youtube.com" + searchPath;
        try {
            return this.parseHtmlSearchResult(url);
        } catch (final YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
    }

    private SearchResult parseHtmlSearchResult(final String url) throws YoutubeException {
        final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage(url));
        if (!response.ok()) {
            throw new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", url, response.error().getMessage()));
        }

        final String html = response.data();

        final JSONObject initialData = this.extractor.extractInitialDataFromHtml(html);
        final JSONArray rootContents;
        try {
            rootContents = initialData.getJSONObject("contents")
                    .getJSONObject("twoColumnSearchResultsRenderer")
                    .getJSONObject("primaryContents")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents");
        } catch (final NullPointerException e) {
            throw new YoutubeException.BadPageException("Search result root contents not found");
        }

        final long estimatedCount = this.extractor.extractLongFromText(initialData.getString("estimatedResults"));
        final String clientVersion = this.extractor.extractClientVersionFromContext(initialData.getJSONObject("responseContext"));
        final SearchContinuation continuation = this.getSearchContinuation(rootContents, clientVersion);
        return this.parseSearchResult(estimatedCount, rootContents, continuation);
    }

    private SearchResult parseSearchContinuation(final SearchContinuation continuation, final YoutubeCallback<SearchResult> callback, final Client client) throws YoutubeException {
        final String url = BASE_API_URL + "/search?key=" + ANDROID_APIKEY + "&prettyPrint=false";
        final JSONObject body = client.getBody()
                .fluentPut("continuation", continuation.token())
                .fluentPut("clickTracking", new JSONObject()
                        .fluentPut("clickTrackingParams", continuation.clickTrackingParameters()));


        final RequestWebpage request = new RequestWebpage(url, "POST", body.toJSONString())
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", continuation.clientVersion())
                .header("Content-Type", "application/json");

        final Response<String> response = this.downloader.downloadWebpage(request);
        if (!response.ok()) {
            final YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", url, response.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        final String html = response.data();

        final JSONObject jsonResponse;
        final JSONArray rootContents;
        try {
            jsonResponse = JSON.parseObject(html);
            if (jsonResponse.containsKey("onResponseReceivedCommands")) {
                rootContents = jsonResponse.getJSONArray("onResponseReceivedCommands")
                        .getJSONObject(0)
                        .getJSONObject("appendContinuationItemsAction")
                        .getJSONArray("continuationItems");
            } else {
                throw new YoutubeException.BadPageException("Could not find continuation data");
            }
        } catch (final YoutubeException e) {
            throw e;
        } catch (final Exception e) {
            throw new YoutubeException.BadPageException("Could not parse search continuation json");
        }

        final long estimatedResults = this.extractor.extractLongFromText(jsonResponse.getString("estimatedResults"));
        final SearchContinuation nextContinuation = this.getSearchContinuation(rootContents, continuation.clientVersion());
        return this.parseSearchResult(estimatedResults, rootContents, nextContinuation);
    }

    private SearchContinuation getSearchContinuation(final JSONArray rootContents, final String clientVersion) {
        if (rootContents.size() > 1) {
            if (rootContents.getJSONObject(1).containsKey("continuationItemRenderer")) {
                final JSONObject endPoint = rootContents.getJSONObject(1)
                        .getJSONObject("continuationItemRenderer")
                        .getJSONObject("continuationEndpoint");
                final String token = endPoint.getJSONObject("continuationCommand").getString("token");
                final String ctp = endPoint.getString("clickTrackingParams");
                return new SearchContinuation(token, clientVersion, ctp);
            }
        }
        return null;
    }

    private SearchResult parseSearchResult(final long estimatedResults, final JSONArray rootContents, final SearchContinuation continuation) throws BadPageException {
        final JSONArray contents;

        try {
            contents = rootContents.getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents");
        } catch (final NullPointerException e) {
            throw new YoutubeException.BadPageException("Search result contents not found");
        }

        final List<SearchResultItem> items = new ArrayList<>(contents.size());
        final Map<QueryElementType, QueryElement> queryElements = new HashMap<>();
        for (int i = 0; i < contents.size(); i++) {
            final SearchResultElement element = parseSearchResultElement(contents.getJSONObject(i));
            if (element != null) {
                if (element instanceof SearchResultItem) {
                    items.add((SearchResultItem) element);
                } else {
                    final QueryElement queryElement = (QueryElement) element;
                    queryElements.put(queryElement.type(), queryElement);
                }
            }
        }
        if (continuation == null) {
            return new SearchResult(estimatedResults, items, queryElements);
        } else {
            return new ContinuatedSearchResult(estimatedResults, items, queryElements, continuation);
        }
    }

    private static SearchResultElement parseSearchResultElement(final JSONObject jsonItem) {
        final String rendererKey = jsonItem.keySet().iterator().next();
        final JSONObject jsonRenderer = jsonItem.getJSONObject(rendererKey);
        switch (rendererKey) {
            case "videoRenderer":
                return new SearchResultVideoDetails(jsonRenderer, false);
            case "movieRenderer":
                return new SearchResultVideoDetails(jsonRenderer, true);
            case "playlistRenderer":
                return new SearchResultPlaylistDetails(jsonRenderer);
            case "channelRenderer":
                return new SearchResultChannelDetails(jsonRenderer);
            case "shelfRenderer":
                return new SearchResultShelf(jsonRenderer);
            case "showingResultsForRenderer":
                return new QueryAutoCorrection(jsonRenderer);
            case "didYouMeanRenderer":
                return new QuerySuggestion(jsonRenderer);
            case "horizontalCardListRenderer":
                return new QueryRefinementList(jsonRenderer);
            default:
                System.out.println("Unknown search result element type " + rendererKey);
                System.out.println(jsonItem);
                return null;
        }
    }
}
