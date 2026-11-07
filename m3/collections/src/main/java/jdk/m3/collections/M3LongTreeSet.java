/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

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
 *
 * <p>Positional access ({@link #get(int)}, {@link #indexOf(long)}) binary-searches a page-prefix
 * lane (the rank of each logical page's first value) that is rebuilt lazily, in O(pages), by the
 * first positional call after a mutation (the revision is {@code modCount}); mutations pay nothing
 * for it and repeated positional calls are O(log n) and allocation-free.
 * {@link #addAllSorted(long[], int, int)} admits a sorted slice and fills fresh full pages with
 * the values above the current last one. The positional contracts are those of the m3scale donors
 * {@code LongGapSortedSet} and {@code LongSortedSet}.</p>
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
    //FLLDCIM: MRL - primitive-method-gaps G1: lazy page-prefix lane for positional access (revision = modCount)
    /** First rank of each logical page; valid only while {@code prefixRevision == modCount}. */
    private int[] pagePrefix;
    private int prefixRevision;

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
        // Evaluate against the untouched physical owner, even when callbacks read this set.
        // One bit per original value replaces the former long[size] scratch payload.
        long[] admitted = new long[(int) (((long) before + 63L) >>> 6)];
        int evaluated = 0;
        int rejected = 0;
        try {
            for (int page = 0; page < pageCount; page++) {
                int offset = pageOffset(page);
                int count = pageCounts[page];
                for (int index = 0; index < count; index++) {
                    if (predicate.test(values[offset + index])) {
                        admitted[evaluated >>> 6] |= 1L << (evaluated & 63);
                    } else {
                        rejected++;
                    }
                    evaluated++;
                }
            }
        } finally {
            // A throwing predicate leaves the current and untested suffix admitted.
            if (rejected != 0) {
                compactRetained(admitted, evaluated);
            }
        }
        return before - size;
    }

    //FLLDCIM: MRL - primitive-method-gaps G1 get(int) (donor LongGapSortedSet.get, blob 87dc58978af6)
    /**
     * Returns the value of zero-based rank {@code index} in ascending order: a binary search of the
     * page-prefix lane, then one page read. O(log n) and allocation-free while no mutation
     * intervenes; the first positional call after a mutation rebuilds the lane in O(pages).
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, size())}
     */
    public long get(int index) {
        Objects.checkIndex(index, size);
        int[] prefix = pagePrefix();
        int page = Arrays.binarySearch(prefix, 0, pageCount, index);
        if (page < 0) {
            // Absent: the rank lies in the page before the insertion point (page 0 starts at 0).
            page = -page - 2;
        }
        return values[pageOffset(page) + index - prefix[page]];
    }

    //FLLDCIM: MRL - primitive-method-gaps G1 indexOf(long) (donor LongSortedSet.indexOf, blob d2e0467cb0c4)
    /**
     * Returns the rank of {@code value} when present, else {@code -(insertion rank) - 1}: the
     * {@link Arrays#binarySearch(long[], long)} convention of the donor (unlike
     * {@link M3LongSortedSnapshot#indexOf(long)}, which returns -1). O(log n) and allocation-free
     * while no mutation intervenes.
     */
    public int indexOf(long value) {
        if (pageCount == 0) {
            return -1;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int found = Arrays.binarySearch(values, offset, offset + pageCounts[page], value);
        // Physical result to rank: shift by the page's first rank minus its physical offset.
        int shift = pagePrefix()[page] - offset;
        return found >= 0 ? found + shift : found - shift;
    }

    //FLLDCIM: MRL - primitive-method-gaps G1 addAllSorted (donor LongGapSortedSet.addAllSorted, blob 87dc58978af6)
    /**
     * Admits the nondecreasing slice {@code sorted[from, to)} and returns the number of new values;
     * duplicates in the slice and values already present collapse. The range and the order are
     * validated before any change. Values above the current last value fill the tail page and then
     * fresh full pages in one pass; the others insert one at a time (O(log n) plus one page shift
     * each). A capacity failure part-way keeps the values admitted before it.
     *
     * @throws NullPointerException if {@code sorted} is null
     * @throws IndexOutOfBoundsException if {@code [from, to)} is not a range of {@code sorted}
     * @throws IllegalArgumentException if the slice is not sorted ascending
     */
    public int addAllSorted(long[] sorted, int from, int to) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.checkFromToIndex(from, to, sorted.length);
        for (int i = from + 1; i < to; i++) {
            if (sorted[i] < sorted[i - 1]) {
                throw new IllegalArgumentException("input must be sorted ascending");
            }
        }
        int before = size;
        int tail = size == 0 ? from : M3LaneSearch.upperBound(sorted, from, to, last());
        for (int i = from; i < tail; i++) {
            add(sorted[i]);
        }
        appendAscending(sorted, tail, to);
        return size - before;
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

    /** Retained primitive lane bytes: the value lane, the four page lanes and the prefix lane once built. */
    public long payloadBytes() {
        //FLLDCIM: MRL - primitive-method-gaps G1: the lazily built prefix lane is retained payload
        long prefixBytes = pagePrefix == null ? 0L : (long) pagePrefix.length * Integer.BYTES;
        return (long) values.length * Long.BYTES + (long) pageMaxima.length * Long.BYTES
                + ((long) pageCounts.length + pageSlot.length + freeSlots.length) * Integer.BYTES
                + prefixBytes;
    }

    /** Repacks the live pages into slot order and shrinks every lane to the live page count. */
    public void trimToSize() {
        int pages = Math.max(1, pageCount);
        long[] packed = new long[pages * pageCapacity];
        long[] nextMaxima = Arrays.copyOf(pageMaxima, pages);
        int[] nextCounts = Arrays.copyOf(pageCounts, pages);
        int[] nextSlots = Arrays.copyOf(pageSlot, pages);
        int[] nextFree = new int[pages];
        //FLLDCIM: MRL - primitive-method-gaps G1: the prefix lane shrinks with the page lanes; logical counts, so
        // the prefix sums and their revision, are unchanged by a repack
        int[] nextPrefix = pagePrefix == null ? null : Arrays.copyOf(pagePrefix, pages);
        for (int page = 0; page < pageCount; page++) {
            System.arraycopy(values, pageOffset(page), packed, page * pageCapacity, pageCounts[page]);
            nextSlots[page] = page;
        }
        values = packed;
        pageMaxima = nextMaxima;
        pageCounts = nextCounts;
        pageSlot = nextSlots;
        freeSlots = nextFree;
        pagePrefix = nextPrefix;
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

    //FLLDCIM: MRL - primitive-method-gaps G1: lazy prefix rebuild, cold path, O(pages) at most once per revision
    /** The page-prefix lane at the current revision; reallocated only after the page lanes grew. */
    private int[] pagePrefix() {
        int[] prefix = pagePrefix;
        if (prefix != null && prefixRevision == modCount) {
            return prefix;
        }
        if (prefix == null || prefix.length < pageCount) {
            prefix = new int[pageCounts.length];
        }
        int rank = 0;
        for (int page = 0; page < pageCount; page++) {
            prefix[page] = rank;
            rank += pageCounts[page];
        }
        pagePrefix = prefix;
        prefixRevision = modCount;
        return prefix;
    }

    /** Appends a nondecreasing slice whose values all exceed the current last value. */
    private void appendAscending(long[] sorted, int from, int to) {
        if (from == to) {
            return;
        }
        // Count the change first: a capacity failure part-way must still invalidate the prefix lane.
        modCount++;
        for (int i = from; i < to; i++) {
            if (i == from || sorted[i] != sorted[i - 1]) {
                appendValue(sorted[i]);
            }
        }
    }

    /** Appends one value above the current last value to the tail page, opening a page when full. */
    private void appendValue(long value) {
        int page = pageCount - 1;
        if (page < 0 || pageCounts[page] == pageCapacity) {
            page = openTailPage();
        }
        int count = pageCounts[page];
        values[pageOffset(page) + count] = value;
        pageCounts[page] = count + 1;
        pageMaxima[page] = value;
        size++;
    }

    /** Opens an empty logical page after the last one, allocating as {@link #splitPage} does. */
    private int openTailPage() {
        ensurePages(pageCount + 1);
        int slot = allocateSlot();
        pageSlot[pageCount] = slot;
        pageCounts[pageCount] = 0;
        return pageCount++;
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
        return M3LaneSearch.lowerBound(pageMaxima, low, high, value);
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

    /**
     * Packs accepted ordinals into the original physical page lanes after all
     * predicate callbacks finish. Destination logical positions never lead the
     * source logical page: this remains safe even when pageSlot[] is permuted.
     * No object/entry or second long-value lane is retained.
     */
    private void compactRetained(long[] admitted, int evaluated) {
        int ordinal = 0;
        int written = 0;
        for (int readPage = 0; readPage < pageCount; readPage++) {
            int source = pageOffset(readPage);
            int count = pageCounts[readPage];
            for (int index = 0; index < count; index++, ordinal++) {
                if (ordinal >= evaluated
                        || (admitted[ordinal >>> 6] & (1L << (ordinal & 63))) != 0L) {
                    int destination = pageOffset(written / pageCapacity) + written % pageCapacity;
                    values[destination] = values[source + index];
                    written++;
                }
            }
        }
        int retainedPages = (int) (((long) written + pageCapacity - 1L) / pageCapacity);
        for (int page = 0; page < retainedPages; page++) {
            int count = Math.min(pageCapacity, written - page * pageCapacity);
            pageCounts[page] = count;
            pageMaxima[page] = values[pageOffset(page) + count - 1];
        }
        while (pageCount > retainedPages) {
            removePage(pageCount - 1);
        }
        size = written;
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
        long[] nextValues = Arrays.copyOf(values, grown * pageCapacity);
        long[] nextPageMaxima = Arrays.copyOf(pageMaxima, grown);
        int[] nextPageCounts = Arrays.copyOf(pageCounts, grown);
        int[] nextPageSlot = Arrays.copyOf(pageSlot, grown);
        int[] nextFreeSlots = Arrays.copyOf(freeSlots, grown);
        values = nextValues;
        pageMaxima = nextPageMaxima;
        pageCounts = nextPageCounts;
        pageSlot = nextPageSlot;
        freeSlots = nextFreeSlots;
    }
}
