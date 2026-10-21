/*
 * Copyright (c) 2026, Hitesh Soliwal.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Explicit, sequential M3 pipeline. Intermediate filter/map stages are fused
 * into a single traversal. Stateful distinct/sort stages are barriers.
 * No automatic rewrite of Stream: opt-in API preserves encounter order,
 * nulls, exceptions and mapper/predicate invocation order.
 *
 * Stages are callbacks in a dynamic array, with one bit per stage identifying
 * filters. This avoids recursive sink chains and per-traversal closures.
 */
public final class M3PipelinePlan<T> {
    private static final int PAGE_SHIFT = 6;
    private static final int PAGE_MASK = (1 << PAGE_SHIFT) - 1;
    private static final Object[] EMPTY_OPERATIONS = new Object[0];
    private static final Object[][] EMPTY_PAGES = new Object[0][];
    private static final long[] EMPTY_FILTER_MASK = new long[0];

    private final Iterable<?> source;
    private final Object[][] pages;
    private final long[] filterMask;
    private final int size;

    private interface Sink {
        void accept(Object value);
    }

    private M3PipelinePlan(Iterable<?> source, Object[][] pages,
                           long[] filterMask, int size) {
        this.source = source;
        this.pages = pages;
        this.filterMask = filterMask;
        this.size = size;
    }

    public static <T> M3PipelinePlan<T> from(Iterable<T> source) {
        return new M3PipelinePlan<>(Objects.requireNonNull(source),
                EMPTY_PAGES, EMPTY_FILTER_MASK, 0);
    }

    public M3PipelinePlan<T> filter(Predicate<? super T> predicate) {
        return append(Objects.requireNonNull(predicate), true);
    }

    public <R> M3PipelinePlan<R> map(Function<? super T, ? extends R> mapper) {
        return append(Objects.requireNonNull(mapper), false);
    }

    /**
     * Copy only the touched (at most 64-entry) page and the primitive page
     * directory/mask. Completed pages are shared, never modified; ancestors
     * and independently forked plans keep their original execution sequences.
     */
    private <R> M3PipelinePlan<R> append(Object operation, boolean filter) {
        int pageIndex = size >>> PAGE_SHIFT;
        int lane = size & PAGE_MASK;
        Object[][] nextPages = Arrays.copyOf(pages, pageIndex + 1);
        Object[] prior = lane == 0 ? EMPTY_OPERATIONS : pages[pageIndex];
        Object[] current = Arrays.copyOf(prior, lane + 1);
        current[lane] = operation;
        nextPages[pageIndex] = current;
        long[] nextFilterMask = Arrays.copyOf(filterMask, pageIndex + 1);
        if (filter) nextFilterMask[pageIndex] |= 1L << lane;
        return new M3PipelinePlan<>(source, nextPages, nextFilterMask, size + 1);
    }

    /** Encounter-ordered distinct, with one result list and one membership set. */
    public List<T> distinctToList() {
        HashSet<T> seen = new HashSet<>();
        ArrayList<T> result = new ArrayList<>();
        traverse(value -> {
            @SuppressWarnings("unchecked") T cast = (T) value;
            if (seen.add(cast)) result.add(cast);
        });
        return result;
    }

    public List<T> toList() {
        ArrayList<T> result = new ArrayList<>();
        traverse(value -> {
            @SuppressWarnings("unchecked") T cast = (T) value;
            result.add(cast);
        });
        return result;
    }

    public List<T> sortedToList(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        ArrayList<T> result = new ArrayList<>(toList());
        result.sort(comparator);
        return result;
    }

    /** Each source value is transformed iteratively, without one frame per stage. */
    @SuppressWarnings("unchecked")
    private void traverse(Sink terminal) {
        for (Object sourceValue : source) {
            Object value = sourceValue;
            boolean accepted = true;
            for (int pageIndex = 0; pageIndex < pages.length && accepted; pageIndex++) {
                Object[] page = pages[pageIndex];
                long bits = filterMask[pageIndex];
                for (int lane = 0; lane < page.length; lane++) {
                    Object operation = page[lane];
                    if ((bits & (1L << lane)) != 0) {
                        if (!((Predicate<Object>) operation).test(value)) {
                            accepted = false;
                            break;
                        }
                    } else {
                        value = ((Function<Object, Object>) operation).apply(value);
                    }
                }
            }
            if (accepted) terminal.accept(value);
        }
    }
}
