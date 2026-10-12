/*
 * Copyright (c) 2026, Hitesh Soliwal.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.util;

import java.util.ArrayList;
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
 */
public final class M3PipelinePlan<T> {
    private final Iterable<?> source;
    private final List<Stage> stages;

    private interface Stage {
        void accept(Object value, Sink next);
    }
    private interface Sink {
        void accept(Object value);
    }

    private M3PipelinePlan(Iterable<?> source, List<Stage> stages) {
        this.source = source;
        this.stages = stages;
    }

    public static <T> M3PipelinePlan<T> from(Iterable<T> source) {
        return new M3PipelinePlan<>(Objects.requireNonNull(source), List.of());
    }

    public M3PipelinePlan<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return append((value, next) -> {
            @SuppressWarnings("unchecked") T cast = (T) value;
            if (predicate.test(cast)) next.accept(value);
        });
    }

    public <R> M3PipelinePlan<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper);
        return append((value, next) -> {
            @SuppressWarnings("unchecked") T cast = (T) value;
            next.accept(mapper.apply(cast));
        });
    }

    private <R> M3PipelinePlan<R> append(Stage stage) {
        ArrayList<Stage> copy = new ArrayList<>(stages);
        copy.add(stage);
        return new M3PipelinePlan<>(source, List.copyOf(copy));
    }

    /**
     * Encounter-ordered distinct. Materializes a single result list and a
     * membership set, rather than per-stage intermediate collections.
     */
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

    private void traverse(Sink terminal) {
        Sink chain = terminal;
        for (int i = stages.size() - 1; i >= 0; i--) {
            Stage stage = stages.get(i);
            Sink next = chain;
            chain = value -> stage.accept(value, next);
        }
        for (Object value : source) chain.accept(value);
    }
}
