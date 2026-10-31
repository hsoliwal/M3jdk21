/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Sorted primitive-long set in natural signed order: fixed-capacity sorted pages inside flat
 * primitive lanes, with no per-element node.
 *
 * <p>All {@code long} values, including zero and the extrema, are legal members. Values live in
 * one {@code long[]} lane cut into physical pages of {@code pageCapacity} slots; logical page order
 * is a slot lane, with a maxima lane and a count lane per logical page, so a page split or merge
 * moves one page of values plus the page lanes, never the whole value lane. {@code contains} and
 * every navigation method ({@code first}/{@code last}/{@code floor}/{@code ceiling}/{@code lower}/
 * {@code higher}) are two binary searches, page maxima then one page: O(log n). {@code add} and
 * {@code remove} shift at most one page; a full page splits in half, a page that empties is
 * released to a free-slot stack, and adjacent pages whose counts fit one page are merged after a
 * removal, so the page count follows the size downward (compaction) and {@link #trimToSize()}
 * repacks the lanes. Adapted from the first-party Synexia donors {@code PackedLongSortedSet}
 * (sorted-lane navigation contract and the {@link #filterInPlace} partial-commit rule) and
 * {@code PackedLongPagedSortedSet} (page lanes, split and merge mechanics); the Abseil-derived
 * sibling-borrow heuristic of the paged donor is not carried. The iterator is fail-fast. This is
 * an M3 proving-ground backend, not a {@code java.util.TreeSet} replacement; live range views are
 * {@link M3LongSortedViews}.</p>
 */
public final class M3LongTreeSet implements M3LongSortedSet {
    /** Donor page size: 64 longs, so an interior insert shifts at most 512 bytes. */
    public static final int DEFAULT_PAGE_CAPACITY = 64;
    private static final int MIN_PAGE_CAPACITY = 4;
    private static final int MAX_PAGE_CAPACITY = 1 << 24;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private final int pageCapacity;
    private long[] values;
    private long[] pageMaxima;
    private int[] pageCounts;
    private int[] pageSlot;
    private int[] freeSlots;
    private int pageCount;
    private int slotsUsed;
    private int freeCount;
    private int size;
    private int modCount;

    public M3LongTreeSet() {
        this(0, DEFAULT_PAGE_CAPACITY);
    }

    public M3LongTreeSet(int expectedSize) {
        this(expectedSize, DEFAULT_PAGE_CAPACITY);
    }

    public M3LongTreeSet(int expectedSize, int pageCapacity) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        if (pageCapacity < MIN_PAGE_CAPACITY || pageCapacity > MAX_PAGE_CAPACITY) {
            throw new IllegalArgumentException("pageCapacity out of range");
        }
        this.pageCapacity = pageCapacity;
        long wanted = ((long) expectedSize + pageCapacity - 1) / pageCapacity;
        int pages = (int) Math.max(1, Math.min(MAX_ARRAY_SIZE / pageCapacity, wanted));
        values = new long[pages * pageCapacity];
        pageMaxima = new long[pages];
        pageCounts = new int[pages];
        pageSlot = new int[pages];
        freeSlots = new int[pages];
    }

    @Override
    public boolean add(long value) {
        if (pageCount == 0) {
            return addFirstPage(value);
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(values, offset, offset + count, value);
        if (index >= 0) {
            return false;
        }
        if (count == pageCapacity) {
            splitPage(page);
            page = locatePage(value);
            offset = pageOffset(page);
            count = pageCounts[page];
            index = Arrays.binarySearch(values, offset, offset + count, value);
        }
        int insertion = -index - 1;
        System.arraycopy(values, insertion, values, insertion + 1, offset + count - insertion);
        values[insertion] = value;
        pageCounts[page] = count + 1;
        pageMaxima[page] = values[offset + count];
        size++;
        modCount++;
        return true;
    }

    private boolean addFirstPage(long value) {
        int slot = allocateSlot();
        pageSlot[0] = slot;
        values[slot * pageCapacity] = value;
        pageCounts[0] = 1;
        pageMaxima[0] = value;
        pageCount = 1;
        size = 1;
        modCount++;
        return true;
    }

    @Override
    public boolean remove(long value) {
        if (pageCount == 0) {
            return false;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(values, offset, offset + count, value);
        if (index < 0) {
            return false;
        }
        int moved = offset + count - index - 1;
        if (moved > 0) {
            System.arraycopy(values, index + 1, values, index, moved);
        }
        count--;
        pageCounts[page] = count;
        size--;
        modCount++;
        if (count == 0) {
            removePage(page);
        } else {
            pageMaxima[page] = values[offset + count - 1];
            mergeNeighbours(page);
        }
        return true;
    }

    @Override
    public boolean contains(long value) {
        if (pageCount == 0) {
            return false;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        return Arrays.binarySearch(values, offset, offset + pageCounts[page], value) >= 0;
    }

    @Override
    public long first() {
        if (size == 0) {
            throw new NoSuchElementException("empty set");
        }
        return values[pageOffset(0)];
    }

    @Override
    public long last() {
        if (size == 0) {
            throw new NoSuchElementException("empty set");
        }
        return lastOf(pageCount - 1);
    }

    @Override
    public long floor(long value, long fallback) {
        if (pageCount == 0) {
            return fallback;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int index = Arrays.binarySearch(values, offset, offset + pageCounts[page], value);
        return index >= 0 ? values[index] : predecessor(page, offset, -index - 2, fallback);
    }

    @Override
    public long ceiling(long value, long fallback) {
        if (pageCount == 0) {
            return fallback;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int end = offset + pageCounts[page];
        int index = Arrays.binarySearch(values, offset, end, value);
        return index >= 0 ? values[index] : successor(page, end, -index - 1, fallback);
    }

    @Override
    public long lower(long value, long fallback) {
        if (pageCount == 0) {
            return fallback;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int index = Arrays.binarySearch(values, offset, offset + pageCounts[page], value);
        return predecessor(page, offset, index >= 0 ? index - 1 : -index - 2, fallback);
    }

    @Override
    public long higher(long value, long fallback) {
        if (pageCount == 0) {
            return fallback;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int end = offset + pageCounts[page];
        int index = Arrays.binarySearch(values, offset, end, value);
        return successor(page, end, index >= 0 ? index + 1 : -index - 1, fallback);
    }

    /**
     * Keeps the values the predicate accepts and returns the number removed. When the predicate
     * throws, prior rejections are committed while the throwing value and the untested suffix stay
     * in the set (donor rule). The predicate must not mutate this set.
     */
    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (size == 0) {
            return 0;
        }
        int before = size;
        long[] kept = new long[size];
        int write = 0;
        int page = 0;
        int index = 0;
        try {
            for (; page < pageCount; page++, index = 0) {
                int offset = pageOffset(page);
                int count = pageCounts[page];
                for (; index < count; index++) {
                    long value = values[offset + index];
                    if (predicate.test(value)) {
                        kept[write++] = value;
                    }
                }
            }
        } finally {
            write = appendUntested(kept, write, page, index);
            if (write != before) {
                rebuild(kept, write);
            }
        }
        return before - size;
    }

    @Override
    public int size() {
        return size;
    }

    /** Value slots currently allocated: physical pages times page capacity. */
    public int capacity() {
        return values.length;
    }

    /** Live (logical) pages; zero when empty. */
    public int pageCount() {
        return pageCount;
    }

    public int pageCapacity() {
        return pageCapacity;
    }

    /** Retained primitive lane bytes: the value lane plus the four page lanes. */
    public long payloadBytes() {
        return (long) values.length * Long.BYTES + (long) pageMaxima.length * Long.BYTES
                + ((long) pageCounts.length + pageSlot.length + freeSlots.length) * Integer.BYTES;
    }

    /** Repacks the live pages into slot order and shrinks every lane to the live page count. */
    public void trimToSize() {
        int pages = Math.max(1, pageCount);
        long[] packed = new long[pages * pageCapacity];
        for (int page = 0; page < pageCount; page++) {
            System.arraycopy(values, pageOffset(page), packed, page * pageCapacity, pageCounts[page]);
            pageSlot[page] = page;
        }
        values = packed;
        pageMaxima = Arrays.copyOf(pageMaxima, pages);
        pageCounts = Arrays.copyOf(pageCounts, pages);
        pageSlot = Arrays.copyOf(pageSlot, pages);
        freeSlots = new int[pages];
        slotsUsed = pageCount;
        freeCount = 0;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        pageCount = 0;
        slotsUsed = 0;
        freeCount = 0;
        size = 0;
        modCount++;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        int write = 0;
        for (int page = 0; page < pageCount; page++) {
            int count = pageCounts[page];
            System.arraycopy(values, pageOffset(page), result, write, count);
            write += count;
        }
        return result;
    }

    @Override
    public M3LongIterator iterator() {
        int expectedModCount = modCount;
        return new M3LongIterator() {
            private int page;
            private int index;

            @Override
            public boolean hasNext() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                return page < pageCount;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                long value = values[pageOffset(page) + index];
                if (++index == pageCounts[page]) {
                    page++;
                    index = 0;
                }
                return value;
            }
        };
    }

    /** Sorted, distinct, sized primitive cursor over the fail-fast iterator; it cannot split. */
    @Override
    public Spliterator.OfLong longSpliterator() {
        M3LongIterator cursor = iterator();
        return new Spliterator.OfLong() {
            @Override public boolean tryAdvance(LongConsumer action) {
                Objects.requireNonNull(action, "action");
                if (!cursor.hasNext()) {
                    return false;
                }
                action.accept(cursor.nextLong());
                return true;
            }
            @Override public OfLong trySplit() { return null; }
            @Override public long estimateSize() { return size; }
            @Override public int characteristics() {
                return ORDERED | SORTED | DISTINCT | NONNULL | SIZED;
            }
            @Override public java.util.Comparator<? super Long> getComparator() { return null; }
        };
    }

    /** Value at {@code candidate} when it lies in the page, else the last value of the page before. */
    private long predecessor(int page, int offset, int candidate, long fallback) {
        if (candidate >= offset) {
            return values[candidate];
        }
        return page == 0 ? fallback : lastOf(page - 1);
    }

    /** Value at {@code candidate} when below {@code end}, else the first value of the page after. */
    private long successor(int page, int end, int candidate, long fallback) {
        if (candidate < end) {
            return values[candidate];
        }
        return page + 1 < pageCount ? values[pageOffset(page + 1)] : fallback;
    }

    private long lastOf(int page) {
        return values[pageOffset(page) + pageCounts[page] - 1];
    }

    /** First logical page whose maximum is at least {@code value}; the last page when none is. */
    private int locatePage(long value) {
        int low = 0;
        int high = pageCount - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (value <= pageMaxima[mid]) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private int pageOffset(int page) {
        return pageSlot[page] * pageCapacity;
    }

    private void splitPage(int page) {
        ensurePages(pageCount + 1);
        int target = allocateSlot();
        int after = pageCount - page - 1;
        if (after > 0) {
            System.arraycopy(pageSlot, page + 1, pageSlot, page + 2, after);
            System.arraycopy(pageCounts, page + 1, pageCounts, page + 2, after);
            System.arraycopy(pageMaxima, page + 1, pageMaxima, page + 2, after);
        }
        int count = pageCounts[page];
        int left = count >>> 1;
        int right = count - left;
        int source = pageOffset(page);
        int destination = target * pageCapacity;
        System.arraycopy(values, source + left, values, destination, right);
        pageSlot[page + 1] = target;
        pageCounts[page] = left;
        pageCounts[page + 1] = right;
        pageMaxima[page] = values[source + left - 1];
        pageMaxima[page + 1] = values[destination + right - 1];
        pageCount++;
    }

    private void mergeNeighbours(int page) {
        if (page > 0 && mergeIfFits(page - 1, page)) {
            return;
        }
        if (page + 1 < pageCount) {
            mergeIfFits(page, page + 1);
        }
    }

    private boolean mergeIfFits(int left, int right) {
        int leftCount = pageCounts[left];
        int rightCount = pageCounts[right];
        if (leftCount + rightCount > pageCapacity) {
            return false;
        }
        System.arraycopy(values, pageOffset(right), values, pageOffset(left) + leftCount, rightCount);
        pageCounts[left] = leftCount + rightCount;
        pageMaxima[left] = pageMaxima[right];
        removePage(right);
        return true;
    }

    private void removePage(int page) {
        freeSlots[freeCount++] = pageSlot[page];
        int after = pageCount - page - 1;
        if (after > 0) {
            System.arraycopy(pageSlot, page + 1, pageSlot, page, after);
            System.arraycopy(pageCounts, page + 1, pageCounts, page, after);
            System.arraycopy(pageMaxima, page + 1, pageMaxima, page, after);
        }
        pageCount--;
    }

    /** Copies the untested tail, from (page, index) onward, behind the kept prefix. */
    private int appendUntested(long[] kept, int write, int page, int index) {
        for (int p = page; p < pageCount; p++) {
            int from = p == page ? index : 0;
            int count = pageCounts[p] - from;
            System.arraycopy(values, pageOffset(p) + from, kept, write, count);
            write += count;
        }
        return write;
    }

    /** Repacks a sorted distinct run into full pages in slot order (donor rule after a bulk edit). */
    private void rebuild(long[] run, int length) {
        int pages = Math.max(1, (length + pageCapacity - 1) / pageCapacity);
        ensurePages(pages);
        pageCount = 0;
        slotsUsed = 0;
        freeCount = 0;
        for (int from = 0, page = 0; from < length; from += pageCapacity, page++) {
            int count = Math.min(pageCapacity, length - from);
            int slot = allocateSlot();
            System.arraycopy(run, from, values, slot * pageCapacity, count);
            pageSlot[page] = slot;
            pageCounts[page] = count;
            pageMaxima[page] = run[from + count - 1];
            pageCount++;
        }
        size = length;
        modCount++;
    }

    private int allocateSlot() {
        if (freeCount > 0) {
            return freeSlots[--freeCount];
        }
        ensurePages(slotsUsed + 1);
        return slotsUsed++;
    }

    /** Grows every lane together by 1.5x (pages), so free slots and live pages always fit. */
    private void ensurePages(int minimumPages) {
        int current = pageCounts.length;
        if (minimumPages <= current) {
            return;
        }
        int limit = MAX_ARRAY_SIZE / pageCapacity;
        if (minimumPages > limit) {
            throw new IllegalStateException("maximum page-lane capacity reached");
        }
        int grown = (int) Math.min(limit, Math.max(minimumPages, current + ((long) current >> 1)));
        values = Arrays.copyOf(values, grown * pageCapacity);
        pageMaxima = Arrays.copyOf(pageMaxima, grown);
        pageCounts = Arrays.copyOf(pageCounts, grown);
        pageSlot = Arrays.copyOf(pageSlot, grown);
        freeSlots = Arrays.copyOf(freeSlots, grown);
    }
}
