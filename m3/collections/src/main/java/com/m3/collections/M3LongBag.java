/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Counted primitive long values backed by the existing M3LongLongHashMap's
 * parallel primitive arrays and occupancy lane. No per-occurrence object.
 *
 * <p>Counts and total cardinality are nonnegative longs and checked for
 * overflow before mutation. Not an implementation of Guava Multiset,
 * Eclipse MutableLongBag, Apache Bag or the JDK Collection interface.
 * Distinct-key iteration order is unspecified. Not thread-safe.
 */
public final class M3LongBag {
    private final M3LongLongHashMap counts;
    private long total;

    public M3LongBag() { this(0); }
    public M3LongBag(int expectedDistinct) {
        counts = new M3LongLongHashMap(expectedDistinct);
    }

    public long count(long value) { return counts.getOrDefault(value, 0L); }

    /** Adds a nonnegative multiplicity and returns the previous multiplicity. */
    public long add(long value, long amount) {
        if (amount < 0) throw new IllegalArgumentException("negative count");
        long before = count(value);
        if (amount == 0) return before;
        long next = Math.addExact(before, amount);
        long newTotal = Math.addExact(total, amount);
        counts.put(value, next);
        total = newTotal;
        return before;
    }

    /** Removes up to amount occurrences, returning the actual number removed. */
    public long remove(long value, long amount) {
        if (amount < 0) throw new IllegalArgumentException("negative count");
        long before = count(value);
        long removed = Math.min(amount, before);
        if (removed != 0) {
            long next = before - removed;
            if (next == 0) counts.remove(value);
            else counts.put(value, next);
            total -= removed;
        }
        return removed;
    }

    /** Installs a nonnegative exact count, returning the previous count. */
    public long setCount(long value, long amount) {
        if (amount < 0) throw new IllegalArgumentException("negative count");
        long before = count(value);
        if (amount == before) return before;
        long nextTotal = Math.addExact(total - before, amount);
        if (amount == 0) counts.remove(value);
        else counts.put(value, amount);
        total = nextTotal;
        return before;
    }

    public boolean contains(long value) { return counts.containsKey(value); }
    public long totalCount() { return total; }
    public int distinctCount() { return counts.size(); }
    public boolean isEmpty() { return total == 0L; }
    public long[] distinctValues() { return counts.keysToArray(); }
    public void forEachCount(M3LongLongConsumer consumer) {
        counts.forEach(Objects.requireNonNull(consumer, "consumer"));
    }
    public void clear() { counts.clear(); total = 0L; }
}
