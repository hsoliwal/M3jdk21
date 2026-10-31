/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Native M3 order-statistics collection over the existing canonical M3CountedBag snapshot.
 * This is NOT a Guava, Apache, Eclipse or Spring adapter. Exactly one M3 key/count
 * snapshot owns the GC-traced reference payload; this projection owns only primitive
 * int[] physical ordinals and long[] occurrence prefix geometry. Each view shares
 * those arrays with its parent and stores a half-open index window.
 *
 * <p>Construction O(d log d), rank/select O(log d), O(1) slice metadata, and
 * filtered selections O(d) plus O(k) output. Source mutations after construction
 * never affect the snapshot. Nulls require a null-aware comparator. Comparator
 * ties preserve the original distinct key occurrence identities, rather than
 * collapsing equals-distinct keys. This type is immutable and single-operation
 * callbacks must not mutate any user-defined comparators/predicates reentrantly.
 * No Java reference, callback or comparator crosses the optional JNI boundary.
 */
public final class M3BagRankIndex<E> extends AbstractCollection<E> {
    private final M3CountedBag.CountSnapshot<E> source;
    private final Comparator<? super E> comparator;
    private final int[] physical;
    private final long[] prefix;
    private final int from;
    private final int to;
    private final boolean descending;

    private M3BagRankIndex(M3CountedBag.CountSnapshot<E> source,
            Comparator<? super E> comparator, int[] physical, long[] prefix,
            int from, int to, boolean descending) {
        this.source = source;
        this.comparator = comparator;
        this.physical = physical;
        this.prefix = prefix;
        this.from = from;
        this.to = to;
        this.descending = descending;
    }

    /** Canonical M3 owner, not an independent element/Entry store. */
    public static <E> M3BagRankIndex<E> snapshot(M3CountedBag<E> owner,
            Comparator<? super E> comparator) {
        Objects.requireNonNull(owner, "owner");
        M3CountedBag.CountSnapshot<E> source = owner.sortedCounts(comparator);
        int[] order = new int[source.distinctSize()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        return create(source, comparator, order, false);
    }

    private static <E> M3BagRankIndex<E> create(M3CountedBag.CountSnapshot<E> source,
            Comparator<? super E> comparator, int[] order, boolean reverse) {
        long[] countsPrefix = new long[order.length + 1];
        for (int i = 0; i < order.length; i++) {
            int n = source.countAt(order[i]);
            if (n <= 0) throw new IllegalStateException("nonpositive admitted M3 count");
            countsPrefix[i + 1] = Math.addExact(countsPrefix[i], n);
        }
        return new M3BagRankIndex<>(source, comparator, order,
                countsPrefix, 0, order.length, reverse);
    }

    public int distinctSize() { return to - from; }
    public long exactSize() { return prefix[to] - prefix[from]; }
    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, exactSize()); }
    @Override public boolean isEmpty() { return from == to; }
    @Override public boolean contains(Object value) { return exactCount(value) != 0; }

    private int physical(int ordinal) {
        return physical[from + Objects.checkIndex(ordinal, distinctSize())];
    }
    public E distinctAt(int ordinal) { return source.elementAt(physical(ordinal)); }
    public int occurrencesAtDistinct(int ordinal) { return source.countAt(physical(ordinal)); }
    public int physicalOrdinalAt(int ordinal) { return physical(ordinal); }
    public int[] selectedPhysicalOrdinals() { return Arrays.copyOfRange(physical, from, to); }
    public boolean descending() { return descending; }
    public Comparator<? super E> sourceComparator() { return comparator; }

    /** Share snapshot keys, group ordinals and prefix sums; do not materialize. */
    public M3BagRankIndex<E> slice(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, distinctSize());
        return new M3BagRankIndex<>(source, comparator, physical, prefix,
                from + fromInclusive, from + toExclusive, descending);
    }

    /** Reverse only physical-index order; no reference or count copying. */
    public M3BagRankIndex<E> reversed() {
        int length = distinctSize();
        int[] order = new int[length];
        for (int i = 0; i < length; i++) order[i] = physical[to - i - 1];
        return create(source, comparator, order, !descending);
    }

    @SuppressWarnings("unchecked")
    private int compare(E left, E right) {
        int result = comparator == null
                ? ((Comparable<? super E>) left).compareTo(right)
                : comparator.compare(left, right);
        // Invert sign without overflowing for an arbitrary comparator returning MIN_VALUE.
        return descending ? (result > 0 ? -1 : result < 0 ? 1 : 0) : result;
    }

    /** First distinct rank with an element comparator >= requested key. */
    public int lowerBound(E key) {
        int low = 0, high = distinctSize();
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (compare(distinctAt(mid), key) < 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** First distinct rank with an element comparator > requested key. */
    public int upperBound(E key) {
        int low = 0, high = distinctSize();
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (compare(distinctAt(mid), key) <= 0) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** Exact occurrence rank, not distinct-key rank. */
    public long occurrencesBefore(E key) { return prefix[from + lowerBound(key)] - prefix[from]; }
    public long occurrencesThrough(E key) { return prefix[from + upperBound(key)] - prefix[from]; }

    /** O(log distinct) selection of one occurrence without allocating an Entry. */
    public E occurrenceAt(long rank) {
        long size = exactSize();
        if (rank < 0 || rank >= size) throw new IndexOutOfBoundsException("occurrence " + rank);
        long ordinal = prefix[from] + rank;
        int low = from, high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (prefix[mid + 1] <= ordinal) low = mid + 1;
            else high = mid;
        }
        return source.elementAt(physical[low]);
    }

    /** Strict comparator-equivalent subwindow with backed immutable index geometry. */
    public M3BagRankIndex<E> subRange(E low, boolean lowInclusive,
            E high, boolean highInclusive) {
        if (compare(low, high) > 0) throw new IllegalArgumentException("inverted comparator bounds");
        int a = lowInclusive ? lowerBound(low) : upperBound(low);
        int b = highInclusive ? upperBound(high) : lowerBound(high);
        return slice(a, Math.max(a, b));
    }

    /** Sum all comparator-equivalent counts, even when equals-distinct keys tie. */
    public long comparatorCount(E element) {
        int a = lowerBound(element), b = upperBound(element);
        return prefix[from + b] - prefix[from + a];
    }

    /** Equality-based count: comparator must be consistent with equality. */
    public int exactCount(Object element) {
        @SuppressWarnings("unchecked") E key = (E) element;
        int a = lowerBound(key), b = upperBound(key);
        for (int i = a; i < b; i++) {
            if (Objects.equals(element, distinctAt(i))) return occurrencesAtDistinct(i);
        }
        return 0;
    }

    /** Evaluate a Java predicate once per distinct key, never per occurrence. */
    public M3BagRankIndex<E> filtered(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int[] selected = new int[distinctSize()];
        int kept = 0;
        for (int i = 0; i < distinctSize(); i++) {
            if (predicate.test(distinctAt(i))) selected[kept++] = physical[from + i];
        }
        return create(source, comparator, Arrays.copyOf(selected, kept), descending);
    }

    /**
     * Filter precomputed per-group flags. JAVA performs a two-pass exact primitive
     * selection; NATIVE_REQUIRED reuses the existing C/JNI M3IndexMaskCompactor.
     * The aligned input flags are not retained. No silent Java fallback from JNI.
     */
    public M3BagRankIndex<E> selected(byte[] states, int required, int forbidden,
            M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        if (states.length != distinctSize()) throw new IllegalArgumentException("misaligned state vector");
        if (((required | forbidden) & ~0xff) != 0 || (required & forbidden) != 0) {
            throw new IllegalArgumentException("invalid required/forbidden mask");
        }
        int count = 0;
        for (byte state : states) {
            int bits = state & 0xff;
            if ((bits & required) == required && (bits & forbidden) == 0) count++;
        }
        int[] indexes;
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            int[] ordered = selectedPhysicalOrdinals();
            byte[] binaryMask = new byte[states.length];
            for (int i = 0; i < states.length; i++) {
                int bits = states[i] & 0xff;
                binaryMask[i] = (byte) ((bits & required) == required
                        && (bits & forbidden) == 0 ? 1 : 0);
            }
            indexes = M3IndexMaskCompactor.select(ordered, binaryMask, 1, backend);
        } else {
            indexes = new int[count];
            int out = 0;
            for (int i = 0; i < states.length; i++) {
                int bits = states[i] & 0xff;
                if ((bits & required) == required && (bits & forbidden) == 0) {
                    indexes[out++] = physical[from + i];
                }
            }
        }
        return create(source, comparator, indexes, descending);
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int ordinal;
            private int repetitions;
            @Override public boolean hasNext() { return repetitions > 0 || ordinal < distinctSize(); }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                if (repetitions == 0) repetitions = occurrencesAtDistinct(ordinal);
                E result = distinctAt(ordinal);
                if (--repetitions == 0) ordinal++;
                return result;
            }
            @Override public void remove() { throw readonly(); }
        };
    }

    /** Unknown split size: no fabricated SIZED if the exact total exceeds int. */
    @Override public Spliterator<E> spliterator() {
        Iterator<E> cursor = iterator();
        return new Spliterator<>() {
            private long remaining = exactSize();
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                Objects.requireNonNull(action, "action");
                if (!cursor.hasNext()) return false;
                action.accept(cursor.next());
                remaining--;
                return true;
            }
            @Override public Spliterator<E> trySplit() { return null; }
            @Override public long estimateSize() { return remaining; }
            @Override public int characteristics() { return ORDERED | IMMUTABLE; }
        };
    }

    private static UnsupportedOperationException readonly() {
        return new UnsupportedOperationException("immutable M3 occurrence index");
    }
    @Override public boolean add(E element) { throw readonly(); }
    @Override public boolean addAll(Collection<? extends E> input) { throw readonly(); }
    @Override public boolean remove(Object value) { throw readonly(); }
    @Override public boolean removeAll(Collection<?> input) { throw readonly(); }
    @Override public boolean retainAll(Collection<?> input) { throw readonly(); }
    @Override public boolean removeIf(Predicate<? super E> predicate) { throw readonly(); }
    @Override public void clear() { throw readonly(); }
}
