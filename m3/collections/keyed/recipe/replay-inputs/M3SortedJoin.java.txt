// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.parallel;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Validated inner merge join with comparator equality, not Object.equals. Duplicate runs
 * produce their complete Cartesian product in key order, then left encounter/right encounter
 * order. Source references and key references are captured, not deep-copied; neither may change.
 * Planning validates sortedness and counts pairs before any output callback is invoked.
 */
public final class M3SortedJoin {
    private M3SortedJoin() { }
    public static <L, R, K> Plan<L, R> by(Collection<? extends L> left,
            Function<? super L, ? extends K> leftKey, Collection<? extends R> right,
            Function<? super R, ? extends K> rightKey, Comparator<? super K> order) {
        Objects.requireNonNull(left); Objects.requireNonNull(right);
        Objects.requireNonNull(leftKey); Objects.requireNonNull(rightKey); Objects.requireNonNull(order);
        Object[] l = left.toArray(), r = right.toArray();
        Object[] lk = keys(l, leftKey, order), rk = keys(r, rightKey, order);
        // At most min(left size,right size) matching runs; grow primitive lanes on demand.
        int[] runs = new int[0]; int count = 0, i = 0, j = 0; long pairs = 0;
        while (i < l.length && j < r.length) {
            int c = order.compare(element(lk, i), element(rk, j));
            if (c < 0) { i++; continue; }
            if (c > 0) { j++; continue; }
            int ie = i + 1, je = j + 1;
            while (ie < l.length && order.compare(element(lk, i), element(lk, ie)) == 0) ie++;
            while (je < r.length && order.compare(element(rk, j), element(rk, je)) == 0) je++;
            int needed = Math.addExact(count, 4);
            if (needed > runs.length) {
                long grown = Math.max(needed, Math.max(16L, runs.length + (runs.length >>> 1)));
                if (grown > Integer.MAX_VALUE - 8L) grown = needed;
                if (grown > Integer.MAX_VALUE - 8L) throw new IllegalArgumentException("too many join runs");
                runs = java.util.Arrays.copyOf(runs, (int) grown);
            }
            runs[count++] = i; runs[count++] = ie; runs[count++] = j; runs[count++] = je;
            pairs = Math.addExact(pairs, Math.multiplyExact((long) ie - i, (long) je - j));
            i = ie; j = je;
        }
        return new Plan<>(l, r, java.util.Arrays.copyOf(runs, count), pairs);
    }
    private static <T, K> Object[] keys(Object[] items, Function<? super T, ? extends K> key,
                                        Comparator<? super K> order) {
        Object[] keys = new Object[items.length];
        for (int i = 0; i < items.length; i++) {
            keys[i] = key.apply(element(items, i));
            if (i > 0 && order.compare(element(keys, i - 1), element(keys, i)) > 0)
                throw new IllegalArgumentException("join input is not sorted");
        }
        return keys;
    }
    @SuppressWarnings("unchecked") private static <T> T element(Object[] items, int index) { return (T) items[index]; }
    /** Read-only matching-run plan; no retained pair objects or output-sized allocation. */
    public static final class Plan<L, R> {
        private final Object[] left, right;
        private final int[] runs;
        private final long pairs;
        private Plan(Object[] left, Object[] right, int[] runs, long pairs) {
            this.left = left; this.right = right; this.runs = runs; this.pairs = pairs;
        }
        public long count() { return pairs; }
        /**
         * Serial output traversal. Rejects a negative/exceeded budget before any callback.
         * A callback failure stops traversal; previously completed side effects are not rolled back.
         */
        public void forEach(long maxPairs, BiConsumer<? super L, ? super R> action) {
            Objects.requireNonNull(action);
            if (maxPairs < 0 || pairs > maxPairs) throw new IllegalArgumentException("join pair budget exceeded");
            for (int g = 0; g < runs.length; g += 4)
                for (int i = runs[g]; i < runs[g + 1]; i++)
                    for (int j = runs[g + 2]; j < runs[g + 3]; j++)
                        action.accept(element(left, i), element(right, j));
        }
    }
}
