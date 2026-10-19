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
    private static final Object[] EMPTY_OPERATIONS = new Object[0];
    private static final long[] EMPTY_FILTER_MASK = new long[0];

    private final Iterable<?> source;
    private final Object[] operations;
    private final long[] filterMask;

    private interface Sink {
        void accept(Object value);
    }

    private M3PipelinePlan(Iterable<?> source, Object[] operations, long[] filterMask) {
        this.source = source;
        this.operations = operations;
        this.filterMask = filterMask;
    }

    public static <T> M3PipelinePlan<T> from(Iterable<T> source) {
        return new M3PipelinePlan<>(Objects.requireNonNull(source),
                EMPTY_OPERATIONS, EMPTY_FILTER_MASK);
    }

    public M3PipelinePlan<T> filter(Predicate<? super T> predicate) {
        return append(Objects.requireNonNull(predicate), true);
    }

    public <R> M3PipelinePlan<R> map(Function<? super T, ? extends R> mapper) {
        return append(Objects.requireNonNull(mapper), false);
    }

    /** New plans own their metadata arrays; parent plans stay immutable. */
    private <R> M3PipelinePlan<R> append(Object operation, boolean filter) {
        int offset = operations.length;
        Object[] nextOperations = Arrays.copyOf(operations, offset + 1);
        nextOperations[offset] = operation;
        long[] nextFilterMask = Arrays.copyOf(filterMask, (offset >>> 6) + 1);
        if (filter) nextFilterMask[offset >>> 6] |= 1L << (offset & 63);
        return new M3PipelinePlan<>(source, nextOperations, nextFilterMask);
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
            for (int offset = 0; offset < operations.length; offset++) {
                Object operation = operations[offset];
                if ((filterMask[offset >>> 6] & (1L << (offset & 63))) != 0) {
                    if (!((Predicate<Object>) operation).test(value)) {
                        accepted = false;
                        break;
                    }
                } else {
                    value = ((Function<Object, Object>) operation).apply(value);
                }
            }
            if (accepted) terminal.accept(value);
        }
    }
}
