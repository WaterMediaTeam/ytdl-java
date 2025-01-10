package com.github.kiulian.downloader.extractor;



import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractorImpl implements Extractor {
    private static final String DEFAULT_CLIENT_VERSION = "2.20200720.00.02";

    private static final List<Pattern> YT_PLAYER_CONFIG_PATTERNS = Arrays.asList(
            Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;ytplayer"),
            Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;"),
            Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*\\;")
    );
    private static final List<Pattern> YT_INITIAL_DATA_PATTERNS = Arrays.asList(
            Pattern.compile("window\\[\"ytInitialData\"\\] = (\\{.*?\\});"),
            Pattern.compile("ytInitialData = (\\{.*?\\});")
    );

    private static final Pattern SUBTITLES_LANG_CODE_PATTERN = Pattern.compile("lang_code=\"(.{2,3})\"");
    private static final Pattern TEXT_NUMBER_REGEX = Pattern.compile("[0-9]+[0-9, ']*");
    private static final Pattern ASSETS_JS_REGEX = Pattern.compile("\"assets\":.+?\"js\":\\s*\"([^\"]+)\"");
    private static final Pattern EMB_JS_REGEX = Pattern.compile("\"jsUrl\":\\s*\"([^\"]+)\"");

    private final Downloader downloader;

    public ExtractorImpl(final Downloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public JSONObject extractInitialDataFromHtml(final String html) throws YoutubeException {
        String ytInitialData = null;

        for (final Pattern pattern : YT_INITIAL_DATA_PATTERNS) {
            final Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                ytInitialData = matcher.group(1);
            }
        }
        if (ytInitialData == null) {
            throw new YoutubeException.BadPageException("Could not find initial data on web page");
        }
        try {
            return JSON.parseObject(ytInitialData);
        } catch (final Exception e) {
            throw new YoutubeException.BadPageException("Initial data contains invalid json");
        }
    }

    @Override
    public JSONObject extractPlayerConfigFromHtml(final String html) throws YoutubeException {
        String ytPlayerConfig = null;
        for (final Pattern pattern : YT_PLAYER_CONFIG_PATTERNS) {
            final Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                ytPlayerConfig = matcher.group(1);
                break;
            }
        }
        if (ytPlayerConfig == null) {
            throw new YoutubeException.BadPageException("Could not find player config on web page");
        }

        try {
            final JSONObject config = JSON.parseObject(ytPlayerConfig);
            if (config.containsKey("args")) {
                return config;
            } else {
                return new JSONObject().fluentPut("args", new JSONObject().fluentPut("player_response", config));
            }
        } catch (final Exception e) {
            throw new YoutubeException.BadPageException("Player config contains invalid json");
        }
    }

    @Override
    public List<String> extractSubtitlesLanguagesFromXml(final String xml) throws YoutubeException {
        final Matcher matcher = SUBTITLES_LANG_CODE_PATTERN.matcher(xml);

        if (!matcher.find()) {
            throw new YoutubeException.BadPageException("Could not find any language code in subtitles xml");
        }

        final List<String> languages = new ArrayList<>();
        do {
            final String language = matcher.group(1);
            languages.add(language);
        } while (matcher.find());
        return languages;
    }

    @Override
    public String extractJsUrlFromConfig(final JSONObject config, final String videoId) throws YoutubeException {
        String js = null;
        if (config.containsKey("assets")) {
            js = config.getJSONObject("assets").getString("js");
        } else {
            // if assets not found - download embed webpage and search there
            final Response<String> response = this.downloader.downloadWebpage(new RequestWebpage("https://www.youtube.com/embed/" + videoId));
            final String html = response.data();
            Matcher matcher = ASSETS_JS_REGEX.matcher(html);
            if (matcher.find()) {
                js = matcher.group(1).replace("\\", "");
            } else {
                matcher = EMB_JS_REGEX.matcher(html);
                if (matcher.find()) {
                    js = matcher.group(1).replace("\\", "");
                }
            }
        }
        if (js == null) {
            throw new YoutubeException.BadPageException("Could not extract js url: assets not found");
        }
        return "https://youtube.com" + js;
    }

    @Override
    public String extractClientVersionFromContext(final JSONObject context) {
        final JSONArray trackingParams = context.getJSONArray("serviceTrackingParams");
        if (trackingParams == null) {
            return DEFAULT_CLIENT_VERSION;
        }
        for (int ti = 0; ti < trackingParams.size(); ti++) {
            final JSONArray params = trackingParams.getJSONObject(ti).getJSONArray("params");
            for (int pi = 0; pi < params.size(); pi++) {
                if (params.getJSONObject(pi).getString("key").equals("cver")) {
                    return params.getJSONObject(pi).getString("value");
                }
            }
        }
        return DEFAULT_CLIENT_VERSION;
    }

    @Override
    public int extractIntegerFromText(final String text) {
        final Matcher matcher = TEXT_NUMBER_REGEX.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(0).replaceAll("[, ']", ""));
        }
        return 0;
    }

    @Override
    public long extractLongFromText(final String text) {
        final Matcher matcher = TEXT_NUMBER_REGEX.matcher(text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(0).replaceAll("[, ']", ""));
        }
        return 0;
    }

}
