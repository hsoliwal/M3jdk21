// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Live read-only projection over one M3ArrayList's dynamic Object[] owner.
 *
 * <p>Only selected physical int indexes are retained. Filtering uses the existing
 * M3IndexMaskCompactor byte-state selector; sorting uses the same stable primitive
 * index algorithm as M3IndexedListSnapshot. No payload array, per-row wrapper,
 * equality surrogate or canonical String storage is copied or retained.
 *
 * <p>Calls rebuild the index on structural or element-replacement changes. An
 * externally modified element's predicate/sort properties cannot be detected:
 * call {@link #invalidate()} after mutating such an object. No concurrency or
 * snapshot guarantee is offered; iterators fail fast on parent-list mutations.
 * Predicate/comparator reentrancy that mutates the parent is rejected during
 * preparation, and failed preparation never publishes a partial index.
 * JNI receives only primitive state bytes and ordinal indexes, never values or
 * callbacks. NATIVE_REQUIRED refuses before invoking the first predicate.
 */
public final class M3IndexedLiveView<E> extends AbstractList<E> implements RandomAccess {
    private final M3ArrayList<E> owner;
    private final Predicate<? super E> filter;
    private final Comparator<? super E> comparator;
    private final boolean sorted;
    private final M3IndexMaskCompactor.Backend backend;
    private int[] physical = new int[0];
    private int structureEpoch;
    private long valueEpoch;
    private boolean prepared;
    private boolean preparing;
    private long invalidationEpoch;

    private M3IndexedLiveView(M3ArrayList<E> owner, Predicate<? super E> filter,
            Comparator<? super E> comparator, boolean sorted,
            M3IndexMaskCompactor.Backend backend) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.filter = filter;
        this.comparator = comparator;
        this.sorted = sorted;
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    /** Parent-backed selection in encounter order; duplicates and nulls preserved. */
    public static <E> M3IndexedLiveView<E> filtered(
            M3ArrayList<E> owner, Predicate<? super E> filter) {
        return new M3IndexedLiveView<>(owner, Objects.requireNonNull(filter, "filter"),
                null, false, M3IndexMaskCompactor.Backend.JAVA);
    }

    /** Parent-backed stable sorted order (null comparator means Comparable order). */
    public static <E> M3IndexedLiveView<E> sorted(
            M3ArrayList<E> owner, Comparator<? super E> comparator) {
        return new M3IndexedLiveView<>(owner, null, comparator, true,
                M3IndexMaskCompactor.Backend.JAVA);
    }

    public static <E> M3IndexedLiveView<E> filteredSorted(
            M3ArrayList<E> owner, Predicate<? super E> filter,
            Comparator<? super E> comparator) {
        return filteredSorted(owner, filter, comparator, M3IndexMaskCompactor.Backend.JAVA);
    }

    /** Explicit JNI choice affects only primitive mask compaction, never callbacks. */
    public static <E> M3IndexedLiveView<E> filteredSorted(
            M3ArrayList<E> owner, Predicate<? super E> filter,
            Comparator<? super E> comparator, M3IndexMaskCompactor.Backend backend) {
        return new M3IndexedLiveView<>(owner, Objects.requireNonNull(filter, "filter"),
                comparator, true, backend);
    }

    /** Explicit refresh for changes to mutable referents not made through M3ArrayList.set. */
    public void invalidate() { prepared = false; invalidationEpoch++; }

    private void checkEpoch(int expectedStructure, long expectedValue) {
        if (owner.m3StructureEpoch() != expectedStructure
                || owner.m3ValueEpoch() != expectedValue) {
            throw new ConcurrentModificationException("M3 indexed view parent changed");
        }
    }

    /** A callback can also invalidate this view without changing parent epochs. */
    private void checkPreparation(int expectedStructure, long expectedValue,
            long expectedInvalidation) {
        checkEpoch(expectedStructure, expectedValue);
        if (invalidationEpoch != expectedInvalidation) {
            throw new ConcurrentModificationException("M3 indexed view invalidated during preparation");
        }
    }

    private void refresh() {
        if (preparing) {
            throw new ConcurrentModificationException("M3 indexed view reentered during preparation");
        }
        int structure = owner.m3StructureEpoch();
        long values = owner.m3ValueEpoch();
        if (prepared && structure == structureEpoch && values == valueEpoch) return;
        long expectedInvalidation = invalidationEpoch;
        preparing = true;
        try {
            if (filter != null && backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                    && !M3IndexMaskCompactor.nativeAvailable()) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            int length = owner.size();
            int[] selected;
            if (filter == null) {
                selected = new int[length];
                for (int i = 0; i < length; i++) selected[i] = i;
            } else {
                byte[] selectedMask = new byte[length];
                for (int i = 0; i < length; i++) {
                    checkPreparation(structure, values, expectedInvalidation);
                    E value = owner.get(i);
                    selectedMask[i] = (byte) (filter.test(value) ? 1 : 0);
                    checkPreparation(structure, values, expectedInvalidation);
                }
                selected = M3IndexMaskCompactor.selectPositions(selectedMask, 1, backend);
            }
            checkPreparation(structure, values, expectedInvalidation);
            if (sorted) {
                M3IndexSort.stable(selected, index -> {
                    checkPreparation(structure, values, expectedInvalidation);
                    return owner.get(index);
                }, comparator);
            }
            checkPreparation(structure, values, expectedInvalidation);
            // One publication after all callbacks, JNI selection and sorting complete.
            physical = selected;
            structureEpoch = structure;
            valueEpoch = values;
            prepared = true;
        } finally {
            preparing = false;
        }
    }

    @Override public int size() { refresh(); return physical.length; }

    @Override public E get(int index) {
        refresh();
        return owner.get(physical[Objects.checkIndex(index, physical.length)]);
    }

    /** Physical offset into the parent's Object[]; recalculated after parent mutations. */
    public int physicalIndex(int index) {
        refresh();
        return physical[Objects.checkIndex(index, physical.length)];
    }

    /** Detached primitive positions, not a payload-array copy. */
    public int[] physicalIndices() { refresh(); return physical.clone(); }

    /**
     * Copy only the required resident page of selected physical positions.
     * The parent M3ArrayList still owns the sole Object[] payload; this
     * operation neither copies values nor changes public List behavior.
     *
     * <p>Validation is complete before the first destination write.
     * Filtering/sorting refresh and callback epoch checks use the incumbent
     * logic; the visible-page transfer is one arraycopy, O(page) instead of
     * cloning the entire O(selected) index directory. The caller owns
     * synchronization against concurrent mutation, as in other live reads.</p>
     *
     * @return physical positions copied, at most maxCount
     */
    int copyPhysicalWindow(int firstOrdinal, int maxCount,
            int[] destination, int destinationOffset) {
        Objects.requireNonNull(destination, "destination");
        refresh();
        Objects.checkFromIndexSize(firstOrdinal, 0, physical.length);
        if (maxCount < 0) throw new IndexOutOfBoundsException("negative page size");
        int length = Math.min(maxCount, physical.length - firstOrdinal);
        Objects.checkFromIndexSize(destinationOffset, length, destination.length);
        System.arraycopy(physical, firstOrdinal, destination, destinationOffset, length);
        return length;
    }

    @Override public Iterator<E> iterator() { return listIterator(0); }

    @Override public ListIterator<E> listIterator(int begin) {
        refresh();
        int total = physical.length;
        Objects.checkFromIndexSize(begin, 0, total);
        int expectedStructure = structureEpoch;
        long expectedValue = valueEpoch;
        long expectedInvalidation = invalidationEpoch;
        int[] order = physical;
        return new ListIterator<>() {
            private int cursor = begin;
            private void check() {
                checkEpoch(expectedStructure, expectedValue);
                if (invalidationEpoch != expectedInvalidation) {
                    throw new ConcurrentModificationException("M3 indexed view invalidated");
                }
            }
            @Override public boolean hasNext() { check(); return cursor < total; }
            @Override public E next() {
                check();
                if (cursor == total) throw new NoSuchElementException();
                return owner.get(order[cursor++]);
            }
            @Override public boolean hasPrevious() { check(); return cursor > 0; }
            @Override public E previous() {
                check();
                if (cursor == 0) throw new NoSuchElementException();
                return owner.get(order[--cursor]);
            }
            @Override public int nextIndex() { check(); return cursor; }
            @Override public int previousIndex() { check(); return cursor - 1; }
            @Override public void remove() { throw immutable(); }
            @Override public void set(E value) { throw immutable(); }
            @Override public void add(E value) { throw immutable(); }
        };
    }

    /** Unknown-size unsplittable cursor: parent remains mutable and live. */
    @Override public Spliterator<E> spliterator() {
        return new Spliterator<>() {
            private Iterator<E> cursor;
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                Objects.requireNonNull(action, "action");
                if (cursor == null) cursor = iterator();
                if (!cursor.hasNext()) return false;
                action.accept(cursor.next());
                return true;
            }
            @Override public Spliterator<E> trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return Spliterator.ORDERED; }
        };
    }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("read-only M3 indexed live projection");
    }

    @Override public E set(int index, E value) { throw immutable(); }
    @Override public void add(int index, E value) { throw immutable(); }
    @Override public boolean add(E value) { throw immutable(); }
    @Override public boolean addAll(Collection<? extends E> values) { throw immutable(); }
    @Override public boolean addAll(int index, Collection<? extends E> values) { throw immutable(); }
    @Override public E remove(int index) { throw immutable(); }
    @Override public boolean remove(Object value) { throw immutable(); }
    @Override public boolean removeAll(Collection<?> values) { throw immutable(); }
    @Override public boolean retainAll(Collection<?> values) { throw immutable(); }
    @Override public boolean removeIf(Predicate<? super E> predicate) { throw immutable(); }
    @Override public void replaceAll(UnaryOperator<E> updater) { throw immutable(); }
    @Override public void sort(Comparator<? super E> order) { throw immutable(); }
    @Override public void clear() { throw immutable(); }
    @Override protected void removeRange(int from, int to) { throw immutable(); }
    @Override public void addFirst(E value) { throw immutable(); }
    @Override public void addLast(E value) { throw immutable(); }
    @Override public E removeFirst() { throw immutable(); }
    @Override public E removeLast() { throw immutable(); }

    /** JDK reversed view stays backed while every modification remains refused. */
    @Override public List<E> reversed() {
        return Collections.unmodifiableList(super.reversed());
    }
}
