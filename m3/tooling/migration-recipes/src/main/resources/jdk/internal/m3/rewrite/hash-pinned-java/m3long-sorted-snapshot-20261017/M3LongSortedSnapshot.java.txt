// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.LongPredicate;

/**
 * Detached, immutable primitive-long sorted-set snapshot with precomputed rank/select geometry.
 *
 * <p>Unlike {@link M3LongSortedViews#filter(M3LongSortedSet, LongPredicate)}, this class does not
 * track subsequent source mutations. It consumes the sorted owner once, invokes the supplied
 * predicate once per encountered value, and retains only one dense primitive array. Queries do
 * not re-evaluate predicates or traverse the original owner. Empty and bounded slices retain the
 * same array and are explicitly immutable. No boxed entries, JNI callbacks, or additional index
 * payloads are retained.</p>
 *
 * <p>Signed natural order, strictly unique values, and an interference-free source traversal
 * are required. A malformed or duplicate-bearing source fails rather than silently sorting or
 * changing its contract. Construction is O(n), rank/navigation O(log n), select O(1), and
 * zero-copy bounded views O(log n); their first construction costs exclude source traversal.</p>
 */
public final class M3LongSortedSnapshot implements M3LongSortedSet {
    private static final LongPredicate ALL = value -> true;
    private static final long[] EMPTY = new long[0];

    private final long[] values;
    private final int from;
    private final int to;

    private M3LongSortedSnapshot(long[] values, int from, int to) {
        this.values = values;
        this.from = from;
        this.to = to;
    }

    /** One-pass detached snapshot. Never retains the source or its iterator. */
    public static M3LongSortedSnapshot copyOf(M3LongSortedSet source) {
        return filtered(source, ALL);
    }

    /**
     * Snapshot of the values admitted by {@code predicate} at traversal time.
     *
     * <p>Predicates must not modify the source during traversal. No predicate is retained or
     * invoked during subsequent rank, navigation, or streaming operations.</p>
     */
    public static M3LongSortedSnapshot filtered(
            M3LongSortedSet source, LongPredicate predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        M3LongArrayList selected = new M3LongArrayList();
        M3LongIterator iterator = source.iterator();
        boolean hasPrevious = false;
        long previous = 0L;
        while (iterator.hasNext()) {
            long value = iterator.nextLong();
            if (hasPrevious && value <= previous) {
                throw new IllegalArgumentException(
                        "source iterator must be strictly increasing and unique");
            }
            hasPrevious = true;
            previous = value;
            if (predicate.test(value)) selected.add(value);
        }
        long[] detached = selected.toArray();
        return new M3LongSortedSnapshot(detached, 0, detached.length);
    }

    /** Defensive input copy; rejects duplicates or unsorted signed-natural input. */
    public static M3LongSortedSnapshot fromSorted(long[] sortedUnique) {
        Objects.requireNonNull(sortedUnique, "sortedUnique");
        validateSortedUnique(sortedUnique);
        long[] detached = sortedUnique.length == 0 ? EMPTY : sortedUnique.clone();
        return new M3LongSortedSnapshot(detached, 0, detached.length);
    }

    /**
     * Precomputed unsigned-byte row-mask selection over an already-sorted primitive value lane.
     *
     * <p>Reuses {@link M3IndexMaskCompactor#selectLong(long[], byte[], int)} instead of creating
     * a second mask engine. Selection preserves order and duplicates are rejected by the sorted
     * source contract. This Java path intentionally does not cross JNI for predicate callbacks;
     * the existing optional JNI mask kernel remains available to its original owners.</p>
     */
    public static M3LongSortedSnapshot fromAlignedMask(
            long[] sortedUnique, byte[] alignedStates, int requiredMask) {
        Objects.requireNonNull(sortedUnique, "sortedUnique");
        validateSortedUnique(sortedUnique);
        long[] detached = M3IndexMaskCompactor.selectLong(
                sortedUnique, alignedStates, requiredMask);
        return new M3LongSortedSnapshot(detached, 0, detached.length);
    }

    private static void validateSortedUnique(long[] values) {
        for (int index = 1; index < values.length; index++) {
            if (values[index] <= values[index - 1]) {
                throw new IllegalArgumentException(
                        "sorted input must have strictly increasing unique long values");
            }
        }
    }

    @Override public int size() { return to - from; }

    @Override public boolean isEmpty() { return from == to; }

    /** Rank of {@code value}: number of selected values strictly smaller than it. */
    public int rank(long value) { return lowerBound(value) - from; }

    /** Select zero-based rank in the bounded snapshot. */
    public long select(int rank) {
        return values[from + Objects.checkIndex(rank, size())];
    }

    /** Signed-natural indexed position, or -1 if absent. */
    public int indexOf(long value) {
        int position = lowerBound(value);
        return position < to && values[position] == value ? position - from : -1;
    }

    /** Count values in the half-open signed-natural interval [low, high). */
    public int countRange(long lowInclusive, long highExclusive) {
        if (lowInclusive > highExclusive) {
            throw new IllegalArgumentException("lowInclusive > highExclusive");
        }
        return lowerBound(highExclusive) - lowerBound(lowInclusive);
    }

    /** Shared-backing immutable bounded snapshot of [low, high). */
    public M3LongSortedSnapshot subSet(long lowInclusive, long highExclusive) {
        if (lowInclusive > highExclusive) {
            throw new IllegalArgumentException("lowInclusive > highExclusive");
        }
        return new M3LongSortedSnapshot(
                values, lowerBound(lowInclusive), lowerBound(highExclusive));
    }

    public M3LongSortedSnapshot headSet(long highExclusive) {
        return new M3LongSortedSnapshot(values, from, lowerBound(highExclusive));
    }

    public M3LongSortedSnapshot tailSet(long lowInclusive) {
        return new M3LongSortedSnapshot(values, lowerBound(lowInclusive), to);
    }

    /** Number of retained bytes in the shared primitive backing array. */
    public long retainedPrimitiveBytes() { return (long) values.length * Long.BYTES; }

    @Override public boolean contains(long value) { return indexOf(value) >= 0; }

    @Override public long[] toArray() { return Arrays.copyOfRange(values, from, to); }

    @Override public M3LongIterator iterator() {
        return new M3LongIterator() {
            private int cursor = from;
            @Override public boolean hasNext() { return cursor < to; }
            @Override public long nextLong() {
                if (!hasNext()) throw new NoSuchElementException();
                return values[cursor++];
            }
        };
    }

    @Override public Spliterator.OfLong longSpliterator() {
        return Spliterators.spliterator(
                values, from, to,
                Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT
                        | Spliterator.NONNULL | Spliterator.IMMUTABLE);
    }

    @Override public long first() {
        if (isEmpty()) throw new NoSuchElementException();
        return values[from];
    }

    @Override public long last() {
        if (isEmpty()) throw new NoSuchElementException();
        return values[to - 1];
    }

    @Override public long lower(long value, long fallback) {
        int position = lowerBound(value) - 1;
        return position >= from ? values[position] : fallback;
    }

    @Override public long floor(long value, long fallback) {
        int position = upperBound(value) - 1;
        return position >= from ? values[position] : fallback;
    }

    @Override public long ceiling(long value, long fallback) {
        int position = lowerBound(value);
        return position < to ? values[position] : fallback;
    }

    @Override public long higher(long value, long fallback) {
        int position = upperBound(value);
        return position < to ? values[position] : fallback;
    }

    private int lowerBound(long value) {
        int low = from;
        int high = to;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (values[middle] < value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private int upperBound(long value) {
        int low = from;
        int high = to;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            if (values[middle] <= value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("immutable M3 sorted snapshot");
    }

    @Override public boolean add(long value) { throw immutable(); }

    @Override public boolean remove(long value) { throw immutable(); }

    @Override public int filterInPlace(LongPredicate predicate) { throw immutable(); }

    @Override public void clear() { throw immutable(); }
}
