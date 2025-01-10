package com.github.kiulian.downloader.model.search;

import java.util.List;
import java.util.Map;

import com.github.kiulian.downloader.model.search.query.*;

public class ContinuatedSearchResult extends SearchResult {

    private final SearchContinuation continuation;

    public ContinuatedSearchResult(final long estimatedResults, final List<SearchResultItem> items,
                                   final Map<QueryElementType, QueryElement> queryElements, final SearchContinuation continuation) {
        super(estimatedResults, items, queryElements);
        this.continuation = continuation;
    }

    public boolean hasContinuation() {
        return true;
    }

    public SearchContinuation continuation() {
        return this.continuation;
    }
}
