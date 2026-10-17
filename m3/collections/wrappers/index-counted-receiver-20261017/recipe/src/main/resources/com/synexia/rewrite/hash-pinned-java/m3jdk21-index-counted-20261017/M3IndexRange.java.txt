// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

/** Bounds are comparator values and O(1) view metadata; they never admit domain IDs. */
final class M3IndexRange<K> {
    final M3IndexOrderedStore<K> store;
    final boolean hasLow, lowInclusive, hasHigh, highInclusive, descending;
    final K low, high;

    M3IndexRange(M3IndexOrderedStore<K> store) {
        this(store, false, null, false, false, null, false, false);
    }

    private M3IndexRange(M3IndexOrderedStore<K> store, boolean hasLow, K low, boolean lowInclusive,
            boolean hasHigh, K high, boolean highInclusive, boolean descending) {
        this.store = store;
        this.hasLow = hasLow; this.low = low; this.lowInclusive = lowInclusive;
        this.hasHigh = hasHigh; this.high = high; this.highInclusive = highInclusive;
        this.descending = descending;
    }

    Comparator<? super K> comparator() {
        return descending ? Collections.reverseOrder(store.comparator) : store.comparator;
    }

    int start() { return hasLow ? store.bound(low, !lowInclusive) : 0; }
    int end() { return hasHigh ? store.bound(high, highInclusive) : store.size; }
    int size() { return Math.max(0, end() - start()); }

    boolean inRange(K key) {
        if (hasLow) {
            int result = store.compare(key, low);
            if (result < 0 || result == 0 && !lowInclusive) return false;
        }
        if (hasHigh) {
            int result = store.compare(key, high);
            if (result > 0 || result == 0 && !highInclusive) return false;
        }
        return true;
    }

    void requireKey(K key) {
        store.validate(key);
        if (!inRange(key)) throw new IllegalArgumentException("key outside view bounds");
    }

    private void requireEndpoint(K key, boolean inclusive) {
        store.validate(key);
        if (hasLow) {
            int result = store.compare(key, low);
            if (result < 0 || result == 0 && inclusive && !lowInclusive)
                throw new IllegalArgumentException("endpoint outside lower bound");
        }
        if (hasHigh) {
            int result = store.compare(key, high);
            if (result > 0 || result == 0 && inclusive && !highInclusive)
                throw new IllegalArgumentException("endpoint outside upper bound");
        }
    }

    int find(K key) { return inRange(key) ? store.find(key) : -1; }

    int first() {
        int start = start(), end = end();
        return start >= end ? -1 : descending ? end - 1 : start;
    }

    int last() {
        int start = start(), end = end();
        return start >= end ? -1 : descending ? start : end - 1;
    }

    /** relation: -2 lower, -1 floor, +1 ceiling, +2 higher in this view's order. */
    int seek(K key, int relation) {
        int actual = descending ? -relation : relation;
        int rank = store.bound(key, actual == -1 || actual == 2);
        int start = start(), end = end();
        if (start >= end) return -1;
        if (actual < 0) {
            rank = Math.min(rank - 1, end - 1);
            return rank < start ? -1 : rank;
        }
        rank = Math.max(rank, start);
        return rank >= end ? -1 : rank;
    }

    M3IndexRange<K> reverse() {
        return new M3IndexRange<>(store, hasLow, low, lowInclusive,
                hasHigh, high, highInclusive, !descending);
    }

    M3IndexRange<K> sub(K from, boolean fromInclusive, K to, boolean toInclusive) {
        requireEndpoint(from, fromInclusive); requireEndpoint(to, toInclusive);
        if ((!descending && store.compare(from, to) > 0)
                || (descending && store.compare(to, from) > 0))
            throw new IllegalArgumentException("reversed endpoints");
        return descending
                ? new M3IndexRange<>(store, true, to, toInclusive, true, from, fromInclusive, true)
                : new M3IndexRange<>(store, true, from, fromInclusive, true, to, toInclusive, false);
    }

    M3IndexRange<K> head(K to, boolean inclusive) {
        requireEndpoint(to, inclusive);
        return descending
                ? new M3IndexRange<>(store, true, to, inclusive, hasHigh, high, highInclusive, true)
                : new M3IndexRange<>(store, hasLow, low, lowInclusive, true, to, inclusive, false);
    }

    M3IndexRange<K> tail(K from, boolean inclusive) {
        requireEndpoint(from, inclusive);
        return descending
                ? new M3IndexRange<>(store, hasLow, low, lowInclusive, true, from, inclusive, true)
                : new M3IndexRange<>(store, true, from, inclusive, hasHigh, high, highInclusive, false);
    }

    void clear() {
        int start = start(), end = end();
        if (start < end) store.removeRange(start, end);
    }

    <T> Iterator<T> iterator(IntFunction<T> projection) {
        int start = start(), end = end();
        return new Iterator<>() {
            private int cursor = descending ? end - 1 : start;
            private int remaining = Math.max(0, end - start);
            private int last = -1;
            private long expected = store.structure;
            @Override public boolean hasNext() {
                store.checkStructure(expected); return remaining != 0;
            }
            @Override public T next() {
                store.checkStructure(expected);
                if (remaining == 0) throw new NoSuchElementException();
                T result = projection.apply(cursor);
                store.checkStructure(expected);
                last = cursor; cursor += descending ? -1 : 1; remaining--;
                return result;
            }
            @Override public void remove() {
                store.checkStructure(expected);
                if (last < 0) throw new IllegalStateException();
                store.remove(last);
                expected = store.structure;
                if (!descending) cursor--;
                last = -1;
            }
        };
    }
}
