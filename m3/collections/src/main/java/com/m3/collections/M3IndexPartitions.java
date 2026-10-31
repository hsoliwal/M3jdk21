/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Backed List partitions over index offsets, without element copies.
 * The lazy outer list is unmodifiable and tracks source growth. Inner
 * lists are ordinary source.subList views and inherit JDK modCount rules.
 * No third-party Guava or Apache source code is copied.
 */
public final class M3IndexPartitions {
    private M3IndexPartitions() { }

    public static <E> List<List<E>> view(List<E> source, int width) {
        Objects.requireNonNull(source, "source");
        if (width <= 0) throw new IllegalArgumentException("partition width must be positive");
        View<E> index = source instanceof RandomAccess
                ? new FastView<>(source, width) : new View<>(source, width);
        return Collections.unmodifiableList(index);
    }

    private static class View<E> extends AbstractList<List<E>> {
        final List<E> source;
        final int width;
        View(List<E> source, int width) { this.source = source; this.width = width; }

        @Override public int size() {
            int size = source.size();
            return size / width + (size % width == 0 ? 0 : 1);
        }

        @Override public List<E> get(int index) {
            Objects.checkIndex(index, size());
            int from = index * width;
            int to = (int) Math.min((long) from + width, source.size());
            return source.subList(from, to);
        }
    }

    private static final class FastView<E> extends View<E> implements RandomAccess {
        FastView(List<E> source, int width) { super(source, width); }
    }
}
