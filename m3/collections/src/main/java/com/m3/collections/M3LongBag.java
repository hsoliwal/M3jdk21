/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026 Hitesh Soliwal and contributors: target M3 ownership, checked mutation,
 * primitive snapshot imports, and allocation-safe capacity trimming.
 * Derived from PackedLongBag in Synexia commit 6c33a5772a12ca99c8bff6a02403b28be4202bb3.
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Primitive long-key bag with checked long counts and aggregate size. Existing
 * M3LongLongHashMap owns parallel primitive lanes; no boxed count, retained entry
 * object or occurrence expansion is introduced. Every long key is representable.
 * Mutation and traversal are not thread-safe; callbacks must not mutate inputs.
 *
 * <p>The source donor's add/remove return NEW counts and require positive amounts.
 * setCount returns the OLD count; expected-count assignment is not concurrent CAS.
 * Algebra creates independent snapshots, not Guava live views. Overflow and failed
 * insertion leave the input bag unchanged. Scans include unused backing capacity.
 */
public final class M3LongBag {
    private M3LongLongHashMap counts;
    private long totalSize;

    public M3LongBag() { this(8); }
    public M3LongBag(int expectedDistinctValues) { counts = new M3LongLongHashMap(expectedDistinctValues); }
    public long add(long value) { return add(value, 1L); }
    public long add(long value, long occurrences) {
        if (occurrences <= 0) throw new IllegalArgumentException("occurrences must be positive");
        long nextTotal = Math.addExact(totalSize, occurrences);
        long next = Math.addExact(count(value), occurrences);
        counts.put(value, next);
        totalSize = nextTotal;
        return next;
    }
    public long count(long value) { return counts.getOrDefault(value, 0L); }
    public long remove(long value, long occurrences) {
        if (occurrences <= 0) throw new IllegalArgumentException("occurrences must be positive");
        long current = count(value);
        if (current == 0) return 0;
        long removed = Math.min(current, occurrences), next = current - removed;
        if (next == 0) counts.remove(value);
        else counts.put(value, next);
        totalSize -= removed;
        return next;
    }
    public long removeAll(long value) {
        long previous = count(value);
        if (previous != 0) { counts.remove(value); totalSize -= previous; }
        return previous;
    }
    public long setCount(long value, long desired) {
        if (desired < 0) throw new IllegalArgumentException("negative count");
        long previous = count(value);
        if (previous == desired) return previous;
        long nextTotal = Math.addExact(totalSize - previous, desired);
        if (desired == 0) counts.remove(value);
        else counts.put(value, desired);
        totalSize = nextTotal;
        return previous;
    }
    public boolean setCount(long value, long expected, long desired) {
        if (expected < 0 || desired < 0) throw new IllegalArgumentException("negative count");
        if (count(value) != expected) return false;
        setCount(value, desired); return true;
    }
    public boolean contains(long value) { return counts.containsKey(value); }
    public int distinctSize() { return counts.size(); }
    public long size() { return totalSize; }
    public boolean isEmpty() { return totalSize == 0; }
    public void clear() { counts.clear(); totalSize = 0; }
    public void forEachCount(M3LongLongConsumer consumer) { counts.forEach(Objects.requireNonNull(consumer, "consumer")); }

    /** Caller-owned primitive snapshot, never expanded by multiplicity; order unspecified. */
    public long[] distinctValues() { return counts.keysToArray(); }

    /** Rebuilds safely to the M3 map's minimum required capacity (minimum eight slots). */
    public void trimToSize() {
        M3LongLongHashMap replacement = new M3LongLongHashMap(distinctSize());
        counts.forEach(replacement::put);
        counts = replacement;
    }
    public M3LongBag copy() {
        M3LongBag result = new M3LongBag(distinctSize());
        forEachCount(result::add); return result;
    }
    public boolean containsOccurrences(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        if (other.totalSize > totalSize) return false;
        boolean[] result = {true};
        other.forEachCount((key, amount) -> { if (count(key) < amount) result[0] = false; });
        return result[0];
    }
    public M3LongBag union(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        M3LongBag result = copy();
        other.forEachCount((key, amount) -> { if (amount > result.count(key)) result.setCount(key, amount); });
        return result;
    }
    public M3LongBag intersection(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        M3LongBag result = new M3LongBag(Math.min(distinctSize(), other.distinctSize()));
        // Scan the lower-capacity owner; do not expand occurrences into temporary lists.
        M3LongBag scan = counts.capacity() <= other.counts.capacity() ? this : other;
        M3LongBag lookup = scan == this ? other : this;
        scan.forEachCount((key, amount) -> {
            long retained = Math.min(amount, lookup.count(key));
            if (retained != 0) result.add(key, retained);
        });
        return result;
    }
    public M3LongBag sum(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        Math.addExact(totalSize, other.totalSize);
        M3LongBag result = copy(); other.forEachCount(result::add); return result;
    }
    public M3LongBag difference(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        M3LongBag result = new M3LongBag(distinctSize());
        forEachCount((key, amount) -> {
            long retained = Math.max(0L, amount - other.count(key));
            if (retained != 0) result.add(key, retained);
        });
        return result;
    }

    /** Produces aligned counts for an explicitly requested stable key array. */
    public long[] countsFor(long[] keys) {
        Objects.requireNonNull(keys, "keys");
        long[] result = new long[keys.length];
        for (int i = 0; i < keys.length; i++) result[i] = count(keys[i]);
        return result;
    }

    /** Imports stable parallel lanes; duplicate keys sum, zero counts are absent. */
    public static M3LongBag fromCounts(long[] keys, long[] amounts) {
        Objects.requireNonNull(keys, "keys"); Objects.requireNonNull(amounts, "amounts");
        if (keys.length != amounts.length) throw new IllegalArgumentException("lane lengths differ");
        M3LongBag result = new M3LongBag();
        for (int i = 0; i < keys.length; i++) {
            if (amounts[i] < 0) throw new IllegalArgumentException("negative count");
            if (amounts[i] != 0) result.add(keys[i], amounts[i]);
        }
        return result;
    }
}
