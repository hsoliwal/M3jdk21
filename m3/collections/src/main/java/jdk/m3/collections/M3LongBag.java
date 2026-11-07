/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

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

    /** Removes every occurrence of value, returning the previous count. */
    public long removeAll(long value) { return setCount(value, 0L); }

    /** Installs desired only when the current count equals expected; both nonnegative. */
    public boolean setCount(long value, long expected, long desired) {
        if (expected < 0 || desired < 0) throw new IllegalArgumentException("negative count");
        if (count(value) != expected) return false;
        setCount(value, desired);
        return true;
    }

    /** Rebuilds the backing map at its minimum required capacity. */
    public void trimToSize() { counts.trimToSize(); }

    /** Independent snapshot with the same counts. */
    public M3LongBag copy() {
        M3LongBag result = new M3LongBag(distinctCount());
        forEachCount(result::add);
        return result;
    }

    /** True when every count of other is at most this bag's count of the same value. */
    public boolean containsOccurrences(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        if (other.total > total) return false;
        boolean[] result = {true};
        other.forEachCount((key, amount) -> { if (count(key) < amount) result[0] = false; });
        return result[0];
    }

    /** New bag holding, per value, the larger of the two counts. */
    public M3LongBag union(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        M3LongBag result = copy();
        other.forEachCount((key, amount) -> { if (amount > result.count(key)) result.setCount(key, amount); });
        return result;
    }

    /** New bag holding, per value, the smaller of the two counts; scans the lower-capacity side. */
    public M3LongBag intersection(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        M3LongBag result = new M3LongBag(Math.min(distinctCount(), other.distinctCount()));
        M3LongBag scan = counts.capacity() <= other.counts.capacity() ? this : other;
        M3LongBag lookup = scan == this ? other : this;
        scan.forEachCount((key, amount) -> {
            long retained = Math.min(amount, lookup.count(key));
            if (retained != 0) result.add(key, retained);
        });
        return result;
    }

    /** New bag holding the summed counts; total overflow is rejected before any copy. */
    public M3LongBag sum(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        Math.addExact(total, other.total);
        M3LongBag result = copy();
        other.forEachCount(result::add);
        return result;
    }

    /** New bag holding, per value, this count minus other's count, floored at zero. */
    public M3LongBag difference(M3LongBag other) {
        Objects.requireNonNull(other, "other");
        M3LongBag result = new M3LongBag(distinctCount());
        forEachCount((key, amount) -> {
            long retained = Math.max(0L, amount - other.count(key));
            if (retained != 0) result.add(key, retained);
        });
        return result;
    }

    /** Counts aligned with an explicitly requested key array. */
    public long[] countsFor(long[] keys) {
        Objects.requireNonNull(keys, "keys");
        long[] result = new long[keys.length];
        for (int i = 0; i < keys.length; i++) result[i] = count(keys[i]);
        return result;
    }

    /** Imports parallel key/count lanes; duplicate keys sum, zero counts stay absent. */
    public static M3LongBag fromCounts(long[] keys, long[] amounts) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(amounts, "amounts");
        if (keys.length != amounts.length) throw new IllegalArgumentException("lane lengths differ");
        M3LongBag result = new M3LongBag();
        for (int i = 0; i < keys.length; i++) {
            if (amounts[i] < 0) throw new IllegalArgumentException("negative count");
            if (amounts[i] != 0) result.add(keys[i], amounts[i]);
        }
        return result;
    }
}
