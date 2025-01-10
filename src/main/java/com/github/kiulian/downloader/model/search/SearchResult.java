package com.github.kiulian.downloader.model.search;

import java.util.*;

import com.github.kiulian.downloader.model.search.query.*;

public class SearchResult {

    private final long estimatedResults;
    private final List<SearchResultItem> items;
    private final QuerySuggestion suggestion;
    private final QueryRefinementList refinementList;
    private final String autoCorrectedQuery;


    public SearchResult(final long estimatedResults, final List<SearchResultItem> items,
                        final Map<QueryElementType, QueryElement> queryElements) {
        this.estimatedResults = estimatedResults;
        this.items = items;
        this.suggestion = (QuerySuggestion) queryElements.get(QueryElementType.SUGGESTION);
        this.refinementList = (QueryRefinementList) queryElements.get(QueryElementType.REFINEMENT_LIST);
        if (queryElements.containsKey(QueryElementType.AUTO_CORRECTION)) {
            this.autoCorrectedQuery = ((QueryAutoCorrection) queryElements.get(QueryElementType.AUTO_CORRECTION)).query();
        } else {
            this.autoCorrectedQuery = null;
        }
    }

    public QuerySuggestion suggestion() {
        return this.suggestion;
    }

    public QueryRefinementList refinements() {
        return this.refinementList;
    }

    public boolean isAutoCorrected() {
        return this.autoCorrectedQuery != null;
    }

    public String autoCorrectedQuery() {
        return this.autoCorrectedQuery;
    }

    public List<SearchResultVideoDetails> videos() {
        final List<SearchResultVideoDetails> videos = new LinkedList<>();
        for (final SearchResultItem item : this.items) {
            if (item.type() == SearchResultItemType.VIDEO) {
                videos.add(item.asVideo());
            }
        }
        return videos;
    }

    public List<SearchResultChannelDetails> channels() {
        final List<SearchResultChannelDetails> channels = new LinkedList<>();
        for (final SearchResultItem item : this.items) {
            if (item.type() == SearchResultItemType.CHANNEL) {
                channels.add(item.asChannel());
            }
        }
        return channels;
    }

    public List<SearchResultPlaylistDetails> playlists() {
        final List<SearchResultPlaylistDetails> videos = new LinkedList<>();
        for (final SearchResultItem item : this.items) {
            if (item.type() == SearchResultItemType.PLAYLIST) {
                videos.add(item.asPlaylist());
            }
        }
        return videos;
    }

    public List<SearchResultShelf> shelves() {
        final List<SearchResultShelf> shelves = new LinkedList<>();
        for (final SearchResultItem item : this.items) {
            if (item.type() == SearchResultItemType.SHELF) {
                shelves.add(item.asShelf());
            }
        }
        return shelves;
    }

    public boolean hasContinuation() {
        return false;
    }

    public long estimatedResults() {
        return this.estimatedResults;
    }

    public List<SearchResultItem> items() {
        return this.items;
    }
}
