/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import java.util.Objects;

/**
 * Distinct-count algebra with independent M3-owned outputs, not Guava live views.
 * Reuses the earlier Synexia DenseObjectBag max/min/sum/floored-difference scheme;
 * Guava null, old-count-return and int-per-element/long-total contracts stay separate.
 * Inputs, keys and callbacks must remain stable. No occurrence expansion is used.
 */
public final class M3Multisets {
    /** Ordered collector; mutable accumulation is not concurrent. */
    public static <E> java.util.stream.Collector<E, M3Multiset<E>, M3Multiset<E>> toMultiset() {
        return java.util.stream.Collector.of(M3Multiset::new, M3Multiset::add,
                (left, right) -> { left.addAll(right); return left; });
    }

    private M3Multisets() { }

    public static <E> M3Multiset<E> copyOf(Iterable<? extends E> source) {
        return M3Multiset.create(source);
    }

    public static boolean containsOccurrences(Multiset<?> available, Multiset<?> required) {
        Objects.requireNonNull(available, "available");
        Objects.requireNonNull(required, "required");
        for (Multiset.Entry<?> entry : required.entrySet()) {
            if (available.count(entry.getElement()) < entry.getCount()) return false;
        }
        return true;
    }

    public static <E> M3Multiset<E> union(Multiset<? extends E> left, Multiset<? extends E> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        M3Multiset<E> result = copyOf(left);
        right.forEachEntry((key, count) -> {
            if (count > result.count(key)) result.setCount(key, count);
        });
        return result;
    }

    public static <E> M3Multiset<E> intersection(Multiset<? extends E> left, Multiset<?> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        M3Multiset<E> result = new M3Multiset<>();
        left.forEachEntry((key, count) -> result.add(key, Math.min(count, right.count(key))));
        return result;
    }

    /** Rejects a result count above Integer.MAX_VALUE instead of wrapping. */
    public static <E> M3Multiset<E> sum(Multiset<? extends E> left, Multiset<? extends E> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        M3Multiset<E> result = copyOf(left);
        right.forEachEntry(result::add);
        return result;
    }

    public static <E> M3Multiset<E> difference(Multiset<? extends E> left, Multiset<?> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        M3Multiset<E> result = new M3Multiset<>();
        left.forEachEntry((key, count) -> result.add(key, Math.max(0, count - right.count(key))));
        return result;
    }
}
