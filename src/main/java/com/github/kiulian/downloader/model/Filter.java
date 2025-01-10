package com.github.kiulian.downloader.model;

import java.util.LinkedList;
import java.util.List;

@FunctionalInterface
public interface Filter<T> {

    boolean test(T element);

    default List<T> select(final List<T> elements) {
        final List<T> filtered = new LinkedList<>();
        for (final T element : elements) {
            if (this.test(element)) {
                filtered.add(element);
            }
        }
        return filtered;
    }
}
