package com.github.kiulian.downloader.downloader.request;

import java.util.*;

import com.github.kiulian.downloader.base64.Base64Encoder;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.field.*;

public class RequestSearchResult extends Request<RequestSearchResult, SearchResult> {

    private static final byte[] FORCED_DATA = new byte[] { 66, 2, 8, 1 };

    private final String query;
    private boolean forceExactQuery;
    private final Map<Integer, SearchField> filterFields = new HashMap<>();
    private SortField sortField;

    public RequestSearchResult(final String query) {
        super();
        this.query = query;
    }

    public String encodeParameters() {
        if (this.sortField == null && this.filterFields.isEmpty() && !this.forceExactQuery) {
            return null;
        }
        
        int filterLength = 0;
        List<SearchField> filters = null;
        if (!this.filterFields.isEmpty()) {
            filters = new ArrayList<>(this.filterFields.values());
            filters.sort(Comparator.comparingInt(SearchField::category));
            for (final SearchField filter : filters) {
                filterLength += filter.length();
            }
        }
        
        int length = filterLength;
        if (this.sortField != null) {
            length += 2;
        }
        if (filters != null) {
            length += 2;
        }
        if (this.forceExactQuery) {
            length += FORCED_DATA.length;
        }
        
        final byte[] bytes = new byte[length];
        int i = 0;
        if (this.sortField != null) {
            bytes[i++] = 8;
            bytes[i++] = this.sortField.value();
        }
        if (filters != null) {
            bytes[i++] = 18;
            bytes[i++] = (byte) filterLength;
            for (final SearchField filter : filters) {
                System.arraycopy(filter.data(), 0, bytes, i, filter.length());
                i += filter.length();
            }
        }
        if (this.forceExactQuery) {
            System.arraycopy(FORCED_DATA, 0, bytes, i, FORCED_DATA.length);
        }
        
        final String encoded = Base64Encoder.getInstance().encodeToString(bytes);
        return encoded.replace("=", "%253D");
    }

    public String query() {
        return this.query;
    }

    public RequestSearchResult forceExactQuery(final boolean forceExactQuery) {
        this.forceExactQuery = forceExactQuery;
        return this;
    }

    public RequestSearchResult filter(final SearchField... field) {
        for (final SearchField filter : field) {
            this.filterFields.put(filter.category(), filter);
        }
        return this;
    }

    public RequestSearchResult uploadedThis(final UploadDateField uploadDateField) {
        this.put(uploadDateField);
        return this;
    }

    public RequestSearchResult type(final TypeField typeField) {
        this.put(typeField);
        return this;
    }

    public RequestSearchResult during(final DurationField durationField) {
        this.put(durationField);
        return this;
    }

    public RequestSearchResult match(final FeatureField... featuresField) {
        return this.filter(featuresField);
    }

    public RequestSearchResult format(final FormatField... formatsField) {
        return this.filter(formatsField);
    }

    public RequestSearchResult sortBy(final SortField sortField) {
        this.sortField = sortField;
        return this;
    }

    private void put(final SearchField field) {
        this.filterFields.put(field.category(), field);
    }
}
