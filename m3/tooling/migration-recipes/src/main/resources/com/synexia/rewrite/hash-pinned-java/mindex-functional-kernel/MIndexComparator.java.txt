/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.mindex.function;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Flat immutable lexicographic Comparator plan.
 *
 * <p>Each stage carries its own direction bit so reversed() and later thenComparing() preserve
 * exact Comparator semantics without nested comparator wrappers.</p>
 */
public final class MIndexComparator<T> implements Comparator<T> {
    private final Comparator<Object>[] stages;
    private final boolean[] reversed;

    @SuppressWarnings("unchecked")
    private MIndexComparator(Comparator<?>[] stages, boolean[] reversed) {
        this.stages = (Comparator<Object>[]) stages;
        this.reversed = reversed;
    }

    public static <T> MIndexComparator<T> of(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        if (comparator instanceof MIndexComparator<?> plan) {
            @SuppressWarnings("unchecked")
            MIndexComparator<T> typed = (MIndexComparator<T>) plan;
            return typed;
        }
        return new MIndexComparator<>(
                new Comparator<?>[] {comparator}, new boolean[] {false});
    }

    public static <T, U> MIndexComparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor,
            Comparator<? super U> keyComparator) {
        Objects.requireNonNull(keyExtractor);
        Objects.requireNonNull(keyComparator);
        return MIndexComparator.of(
                (left, right) -> keyComparator.compare(
                        keyExtractor.apply(left), keyExtractor.apply(right)));
    }

    public static <T> MIndexComparator<T> comparingInt(
            ToIntFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return MIndexComparator.of(
                (left, right) -> Integer.compare(
                        keyExtractor.applyAsInt(left), keyExtractor.applyAsInt(right)));
    }

    public static <T> MIndexComparator<T> comparingLong(
            ToLongFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return MIndexComparator.of(
                (left, right) -> Long.compare(
                        keyExtractor.applyAsLong(left), keyExtractor.applyAsLong(right)));
    }

    public static <T> MIndexComparator<T> comparingDouble(
            ToDoubleFunction<? super T> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return MIndexComparator.of(
                (left, right) -> Double.compare(
                        keyExtractor.applyAsDouble(left), keyExtractor.applyAsDouble(right)));
    }

    @Override
    public int compare(T left, T right) {
        for (int index = 0; index < stages.length; index++) {
            int result = reversed[index]
                    ? stages[index].compare(right, left)
                    : stages[index].compare(left, right);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    @Override
    public MIndexComparator<T> reversed() {
        boolean[] directions = reversed.clone();
        for (int index = 0; index < directions.length; index++) {
            directions[index] = !directions[index];
        }
        return new MIndexComparator<>(stages, directions);
    }

    @Override
    public MIndexComparator<T> thenComparing(
            Comparator<? super T> other) {
        Objects.requireNonNull(other);
        MIndexComparator<T> right = MIndexComparator.of(other);
        Comparator<?>[] joined =
                Arrays.copyOf(stages, Math.addExact(stages.length, right.stages.length));
        boolean[] directions =
                Arrays.copyOf(reversed, Math.addExact(reversed.length, right.reversed.length));
        System.arraycopy(right.stages, 0, joined, stages.length, right.stages.length);
        System.arraycopy(right.reversed, 0, directions, reversed.length, right.reversed.length);
        return new MIndexComparator<>(joined, directions);
    }

    public int stageCount() {
        return stages.length;
    }
}
