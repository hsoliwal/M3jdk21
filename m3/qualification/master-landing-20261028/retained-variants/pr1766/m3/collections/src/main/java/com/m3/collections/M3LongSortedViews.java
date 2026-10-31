// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Primitive, live, backed sorted-set wrappers. Does not copy keys or own their payloads.
 *
 * <p>These wrappers use natural signed-long order, preserve the existing
 * {@link M3LongSortedSet} contract and do not implement JDK {@code NavigableSet}.
 * Predicates must be stable for a value during each operation. Neither the backing set
 * nor the predicate may be mutated from a predicate/iteration callback. No wrapper is
 * thread-safe unless the backing set separately guarantees the needed synchronization.
 * Range endpoints restrict membership; only {@code subSet} checks endpoint ordering.
 * This is not a JDK range-view endpoint-validity contract.
 *
 * <p>The original source determines mutation failure/partial-commit behavior.
 * Single-key lookup and navigation remain in Java; calling a Java predicate through JNI
 * for each key would add crossings without a validated semantic or performance benefit.
 */
public final class M3LongSortedViews {
    private M3LongSortedViews() { }

    /** Original read-only primitive collection filtering remains the single generic owner. */
    public static M3LongFilteredView filter(M3LongCollection source, LongPredicate predicate) {
        return new M3LongFilteredView(source, predicate);
    }

    /** Live, mutable filtered set; excluded keys are invisible but stay in the backing set. */
    public static M3LongSortedSet filter(M3LongSortedSet source, LongPredicate predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        if (source instanceof Filtered previous) {
            // Preserve predicate order and a single backing traversal, even for nested views.
            return new Filtered(previous.source, M3LongPredicates.and(previous.predicate, predicate));
        }
        return new Filtered(source, predicate);
    }

    /** Live view of [fromInclusive, toExclusive); the equal-endpoint view is empty. */
    public static M3LongSortedSet subSet(M3LongSortedSet source, long fromInclusive, long toExclusive) {
        Objects.requireNonNull(source, "source");
        if (fromInclusive > toExclusive) {
            throw new IllegalArgumentException("fromInclusive > toExclusive");
        }
        return filter(source, v -> v >= fromInclusive && v < toExclusive);
    }

    /** Live view of keys strictly below {@code toExclusive}. */
    public static M3LongSortedSet headSet(M3LongSortedSet source, long toExclusive) {
        return filter(source, v -> v < toExclusive);
    }

    /** Live view of keys at or above {@code fromInclusive}. */
    public static M3LongSortedSet tailSet(M3LongSortedSet source, long fromInclusive) {
        return filter(source, v -> v >= fromInclusive);
    }

    /** Live read-only wrapper. Every mutation method refuses, including no-op mutations. */
    public static M3LongSortedSet unmodifiable(M3LongSortedSet source) {
        Objects.requireNonNull(source, "source");
        return source instanceof Unmodifiable ? source : new Unmodifiable(source);
    }


    /**
     * Emits at most {@code limit} elements of the existing live sorted or
     * filtered/sorted owner, starting after {@code skip} visible occurrences.
     * Does not call size(), toArray() or materialize off-viewport entries.
     * Retains original signed sort order, live backing and predicate order.
     * This is a bounded *read*, not an immutable snapshot: the backing
     * iterator owns interference, exceptions and post-binding behavior.
     *
     * <p>The destination must have room for the entire requested budget;
     * negative coordinates and capacity errors are rejected before reading
     * or writing. A zero budget does not bind an iterator or evaluate a
     * predicate. Source traversal is O(skip + page) visible elements,
     * possibly visiting more excluded elements; one primitive cursor is
     * allocated, never a per-element wrapper or retained payload array.</p>
     *
     * @return the number of longs emitted into the caller's existing lane
     */
    public static int copyPage(M3LongSortedSet source, int skip, int limit,
            long[] destination, int destinationOffset) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (skip < 0 || limit < 0)
            throw new IndexOutOfBoundsException("negative sorted page coordinate");
        Objects.checkFromIndexSize(destinationOffset, limit, destination.length);
        if (limit == 0) return 0;
        M3LongIterator iterator = source.iterator();
        int omitted = 0;
        while (omitted < skip && iterator.hasNext()) {
            iterator.nextLong();
            omitted++;
        }
        int written = 0;
        while (written < limit && iterator.hasNext()) {
            destination[destinationOffset + written++] = iterator.nextLong();
        }
        return written;
    }

    /**
     * A sorted live view binds its backing iterator on first traversal, not when
     * a stream/spliterator is obtained. Sorted metadata, splitting and size
     * queries remain allocation-free and do not call the backing collection.
     * The source owns post-binding interference/fail-fast behavior.
     */
    private static Spliterator.OfLong sortedCursor(M3LongCollection source) {
        return new Spliterator.OfLong() {
            private M3LongIterator cursor;
            @Override public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action, "action");
                if (cursor == null) cursor = source.iterator();
                if (!cursor.hasNext()) return false;
                action.accept(cursor.nextLong());
                return true;
            }
            @Override public OfLong trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return ORDERED | SORTED | DISTINCT | NONNULL; }
            @Override public java.util.Comparator<? super Long> getComparator() { return null; }
        };
    }

    private static final class Filtered implements M3LongSortedSet {
        private final M3LongSortedSet source;
        private final LongPredicate predicate;
        private final M3LongFilteredView selection;

        private Filtered(M3LongSortedSet source, LongPredicate predicate) {
            this.source = source;
            this.predicate = predicate;
            this.selection = new M3LongFilteredView(source, predicate);
        }

        @Override public int size() { return selection.size(); }
        @Override public boolean isEmpty() { return selection.isEmpty(); }
        @Override public boolean contains(long value) { return selection.contains(value); }
        @Override public long[] toArray() { return selection.toArray(); }
        @Override public M3LongIterator iterator() { return selection.iterator(); }
        @Override public Spliterator.OfLong longSpliterator() { return sortedCursor(this); }

        @Override public boolean add(long value) {
            if (!predicate.test(value)) {
                throw new IllegalArgumentException("value rejected by view predicate");
            }
            return source.add(value);
        }

        @Override public boolean remove(long value) {
            return contains(value) && source.remove(value);
        }

        @Override public void clear() {
            source.filterInPlace(value -> !predicate.test(value));
        }

        @Override public int filterInPlace(LongPredicate keep) {
            Objects.requireNonNull(keep, "predicate");
            return source.filterInPlace(value -> !predicate.test(value) || keep.test(value));
        }

        @Override public long first() {
            long value = source.first();
            if (predicate.test(value)) return value;
            long next = higher(value, value);
            if (next == value) throw new NoSuchElementException();
            return next;
        }

        @Override public long last() {
            long value = source.last();
            if (predicate.test(value)) return value;
            long previous = lower(value, value);
            if (previous == value) throw new NoSuchElementException();
            return previous;
        }

        /** Using pivot itself as a sentinel is sound: lower always returns a strict predecessor. */
        @Override public long lower(long pivot, long fallback) {
            long candidate = source.lower(pivot, pivot);
            while (candidate != pivot) {
                if (predicate.test(candidate)) return candidate;
                long predecessor = source.lower(candidate, candidate);
                if (predecessor == candidate) return fallback;
                candidate = predecessor;
            }
            return fallback;
        }

        /** Using pivot itself as a sentinel is sound: higher always returns a strict successor. */
        @Override public long higher(long pivot, long fallback) {
            long candidate = source.higher(pivot, pivot);
            while (candidate != pivot) {
                if (predicate.test(candidate)) return candidate;
                long successor = source.higher(candidate, candidate);
                if (successor == candidate) return fallback;
                candidate = successor;
            }
            return fallback;
        }

        @Override public long floor(long pivot, long fallback) {
            return contains(pivot) ? pivot : lower(pivot, fallback);
        }

        @Override public long ceiling(long pivot, long fallback) {
            return contains(pivot) ? pivot : higher(pivot, fallback);
        }
    }

    private static final class Unmodifiable implements M3LongSortedSet {
        private final M3LongSortedSet source;

        private Unmodifiable(M3LongSortedSet source) { this.source = source; }
        @Override public int size() { return source.size(); }
        @Override public boolean isEmpty() { return source.isEmpty(); }
        @Override public boolean contains(long value) { return source.contains(value); }
        @Override public long[] toArray() { return source.toArray(); }
        @Override public M3LongIterator iterator() { return source.iterator(); }
        @Override public Spliterator.OfLong longSpliterator() { return sortedCursor(this); }
        @Override public long first() { return source.first(); }
        @Override public long last() { return source.last(); }
        @Override public long lower(long value, long fallback) { return source.lower(value, fallback); }
        @Override public long higher(long value, long fallback) { return source.higher(value, fallback); }
        @Override public long floor(long value, long fallback) { return source.floor(value, fallback); }
        @Override public long ceiling(long value, long fallback) { return source.ceiling(value, fallback); }
        @Override public boolean add(long value) { throw new UnsupportedOperationException("unmodifiable"); }
        @Override public boolean remove(long value) { throw new UnsupportedOperationException("unmodifiable"); }
        @Override public int filterInPlace(LongPredicate predicate) {
            throw new UnsupportedOperationException("unmodifiable");
        }
        @Override public void clear() { throw new UnsupportedOperationException("unmodifiable"); }
    }
}
