/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongArrayList;
import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongSortedSet;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Mutable sorted primitive-long set using fixed-capacity pages inside flat primitive arrays.
 *
 * <p>Lookup is a binary search over primitive page maxima followed by a binary search
 * inside one page. Interior insertion shifts only one page in the common case. Full
 * pages split mechanically into two adjacent primitive pages. No tree node or entry
 * object is retained per value.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongPagedSortedSet.java}
 * blob {@code 46ca4ddf4af039e903cbc995b1dc998c2e211133} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections receipt-aware batch (v6, RECEIPT_MAP): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongPagedSortedSet implements LongSortedSet {
    public static final int DEFAULT_PAGE_CAPACITY = 64;

    private final int pageCapacity;
    private long[] values;
    private int[] pageCounts;
    private long[] pageMaxima;
    private int pageCount;
    private int size;

    public PackedLongPagedSortedSet() {
        this(0, DEFAULT_PAGE_CAPACITY);
    }

    public PackedLongPagedSortedSet(int expectedSize) {
        this(expectedSize, DEFAULT_PAGE_CAPACITY);
    }

    public PackedLongPagedSortedSet(int expectedSize, int pageCapacity) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (pageCapacity < 4) {
            throw new IllegalArgumentException("pageCapacity must be >= 4");
        }
        this.pageCapacity = pageCapacity;
        int pages = Math.max(1, divideCeiling(Math.max(1, expectedSize), pageCapacity));
        this.values = new long[Math.multiplyExact(pages, pageCapacity)];
        this.pageCounts = new int[pages];
        this.pageMaxima = new long[pages];
    }

    public boolean add(long value) {
        if (pageCount == 0) {
            pageCount = 1;
            values[0] = value;
            pageCounts[0] = 1;
            pageMaxima[0] = value;
            size = 1;
            return true;
        }

        int page = locatePage(value);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(values, offset, offset + count, value);
        if (index >= 0) {
            return false;
        }

        if (count == pageCapacity) {
            int insertionInPage = -index - 1 - offset;
            if (!rebalanceFullPageForInsert(page, insertionInPage)) {
                splitPage(page);
            }
            page = locatePage(value);
            offset = pageOffset(page);
            count = pageCounts[page];
            index = Arrays.binarySearch(values, offset, offset + count, value);
        }

        int insertion = -index - 1;
        System.arraycopy(
                values,
                insertion,
                values,
                insertion + 1,
                offset + count - insertion);
        values[insertion] = value;
        pageCounts[page] = count + 1;
        pageMaxima[page] = values[offset + count];
        size++;
        return true;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && add(value);
    }

    public int addAll(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) {
            return 0;
        }

        long[] incoming = Arrays.copyOf(source, source.length);
        Arrays.sort(incoming);
        int unique = 1;
        for (int read = 1; read < incoming.length; read++) {
            if (incoming[read] != incoming[unique - 1]) {
                incoming[unique++] = incoming[read];
            }
        }
        incoming = Arrays.copyOf(incoming, unique);

        int before = size;
        long[] merged = PackedLongSetOps.unionSorted(toArray(), incoming);
        rebuildFromSortedUnique(merged);
        return size - before;
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
        pageCounts[page] = --count;
        size--;

        if (count == 0) {
            removePage(page);
        } else {
            pageMaxima[page] = values[offset + count - 1];
            rebalanceAdjacentPagesAfterRemove(page);
        }
        return true;
    }

    public long first() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return values[0];
    }

    public long last() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        int page = pageCount - 1;
        return values[pageOffset(page) + pageCounts[page] - 1];
    }

    public long floor(long value, long defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(values, offset, offset + count, value);
        if (index >= 0) {
            return values[index];
        }
        int insertion = -index - 1;
        if (insertion > offset) {
            return values[insertion - 1];
        }
        if (page == 0) {
            return defaultValue;
        }
        int previous = page - 1;
        return values[pageOffset(previous) + pageCounts[previous] - 1];
    }

    public long ceiling(long value, long defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(values, offset, offset + count, value);
        if (index >= 0) {
            return values[index];
        }
        int insertion = -index - 1;
        if (insertion < offset + count) {
            return values[insertion];
        }
        if (page + 1 >= pageCount) {
            return defaultValue;
        }
        return values[pageOffset(page + 1)];
    }

    public long lower(long value, long defaultValue) {
        long floor = floor(value, defaultValue);
        if (floor == defaultValue || floor != value) {
            return floor;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int index = Arrays.binarySearch(values, offset, offset + pageCounts[page], value);
        if (index > offset) {
            return values[index - 1];
        }
        if (page == 0) {
            return defaultValue;
        }
        int previous = page - 1;
        return values[pageOffset(previous) + pageCounts[previous] - 1];
    }

    public long higher(long value, long defaultValue) {
        long ceiling = ceiling(value, defaultValue);
        if (ceiling == defaultValue || ceiling != value) {
            return ceiling;
        }
        int page = locatePage(value);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(values, offset, offset + count, value);
        if (index + 1 < offset + count) {
            return values[index + 1];
        }
        return page + 1 < pageCount ? values[pageOffset(page + 1)] : defaultValue;
    }

    /**
     * Copies values in the half-open key range [fromInclusive, toExclusive).
     */
    public long[] range(long fromInclusive, long toExclusive) {
        if (fromInclusive >= toExclusive || size == 0) {
            return new long[0];
        }
        LongArrayList result = new LongArrayList();
        forEachLong(value -> {
            if (value >= fromInclusive && value < toExclusive) {
                result.add(value);
            }
        });
        return result.toArray();
    }

    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long[] retained = toArray();
        clear();
        int removed = 0;
        for (long value : retained) {
            if (predicate.test(value)) {
                add(value);
            } else {
                removed++;
            }
        }
        return removed;
    }

    @Override
    public int size() {
        return size;
    }

    public int pageCount() {
        return pageCount;
    }

    public int pageCapacity() {
        return pageCapacity;
    }

    @Override
    public void clear() {
        Arrays.fill(pageCounts, 0);
        pageCount = 0;
        size = 0;
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
    public LongIterator iterator() {
        return new LongIterator() {
            private int page;
            private int indexInPage;

            @Override
            public boolean hasNext() {
                return page < pageCount && (page < pageCount - 1
                        || indexInPage < pageCounts[page]);
            }

            @Override
            public long nextLong() {
                while (page < pageCount && indexInPage >= pageCounts[page]) {
                    page++;
                    indexInPage = 0;
                }
                if (page >= pageCount) {
                    throw new NoSuchElementException();
                }
                return values[pageOffset(page) + indexInPage++];
            }
        };
    }

    private void rebuildFromSortedUnique(long[] sortedUnique) {
        int requiredPages = Math.max(1, divideCeiling(Math.max(1, sortedUnique.length), pageCapacity));
        ensurePageCapacity(requiredPages);
        Arrays.fill(pageCounts, 0);
        pageCount = sortedUnique.length == 0 ? 0 : requiredPages;
        size = sortedUnique.length;

        int source = 0;
        for (int page = 0; page < pageCount; page++) {
            int count = Math.min(pageCapacity, sortedUnique.length - source);
            int offset = pageOffset(page);
            System.arraycopy(sortedUnique, source, values, offset, count);
            pageCounts[page] = count;
            pageMaxima[page] = values[offset + count - 1];
            source += count;
        }
    }

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

    /**
     * Abseil B-tree adaptation: redistribute with a non-full sibling before allocating a split.
     *
     * <p>Only the primitive value lane moves. Ordering and the public set contract remain
     * unchanged; linear-vs-binary node search remains a separate benchmark decision.</p>
     */
    private boolean rebalanceFullPageForInsert(int page, int insertionInPage) {
        int leftCount = page > 0 ? pageCounts[page - 1] : -1;
        int rightCount = page + 1 < pageCount ? pageCounts[page + 1] : -1;
        int move = PackedSortedPageRebalanceAtoms.planMove(
                insertionInPage, pageCapacity, leftCount, rightCount);
        if (move < 0) {
            rebalanceToLeft(page, -move);
            return true;
        }
        if (move > 0) {
            rebalanceToRight(page, move);
            return true;
        }
        return false;
    }

    private void rebalanceToLeft(int page, int move) {
        int left = page - 1;
        int leftOffset = pageOffset(left);
        int offset = pageOffset(page);
        int leftCount = pageCounts[left];
        int count = pageCounts[page];

        PackedSortedPageRebalanceAtoms.movePrefixToLeft(
                values, leftOffset, leftCount, offset, count, move);

        pageCounts[left] = leftCount + move;
        pageCounts[page] = count - move;
        pageMaxima[left] = values[leftOffset + leftCount + move - 1];
        pageMaxima[page] = values[offset + count - move - 1];
    }

    private void rebalanceToRight(int page, int move) {
        int right = page + 1;
        int offset = pageOffset(page);
        int rightOffset = pageOffset(right);
        int count = pageCounts[page];
        int rightCount = pageCounts[right];

        PackedSortedPageRebalanceAtoms.moveSuffixToRight(
                values, offset, count, rightOffset, rightCount, move);

        pageCounts[page] = count - move;
        pageCounts[right] = rightCount + move;
        pageMaxima[page] = values[offset + count - move - 1];
        pageMaxima[right] = values[rightOffset + rightCount + move - 1];
    }

    private void splitPage(int page) {
        ensurePageCapacity(pageCount + 1);
        int sourceOffset = pageOffset(page + 1);
        int pagesAfter = pageCount - page - 1;
        if (pagesAfter > 0) {
            System.arraycopy(
                    values,
                    sourceOffset,
                    values,
                    sourceOffset + pageCapacity,
                    Math.multiplyExact(pagesAfter, pageCapacity));
            System.arraycopy(pageCounts, page + 1, pageCounts, page + 2, pagesAfter);
            System.arraycopy(pageMaxima, page + 1, pageMaxima, page + 2, pagesAfter);
        }

        int count = pageCounts[page];
        int leftCount = count >>> 1;
        int rightCount = count - leftCount;
        int leftOffset = pageOffset(page);
        int rightOffset = pageOffset(page + 1);
        System.arraycopy(values, leftOffset + leftCount, values, rightOffset, rightCount);

        pageCounts[page] = leftCount;
        pageCounts[page + 1] = rightCount;
        pageMaxima[page] = values[leftOffset + leftCount - 1];
        pageMaxima[page + 1] = values[rightOffset + rightCount - 1];
        pageCount++;
    }

    private void removePage(int page) {
        if (pageCount == 1) {
            pageCount = 0;
            return;
        }
        int pagesAfter = pageCount - page - 1;
        if (pagesAfter > 0) {
            System.arraycopy(
                    values,
                    pageOffset(page + 1),
                    values,
                    pageOffset(page),
                    Math.multiplyExact(pagesAfter, pageCapacity));
            System.arraycopy(pageCounts, page + 1, pageCounts, page, pagesAfter);
            System.arraycopy(pageMaxima, page + 1, pageMaxima, page, pagesAfter);
        }
        pageCount--;
        pageCounts[pageCount] = 0;
    }

    /**
     * Abseil delete-side adaptation: merge an adjacent pair when possible, otherwise borrow from
     * a fuller sibling when this page is below half capacity.
     */
    private void rebalanceAdjacentPagesAfterRemove(int page) {
        if (pageCount < 2) {
            return;
        }

        if (page > 0 && mergePagesIfFits(page - 1, page)) {
            return;
        }
        if (page + 1 < pageCount && mergePagesIfFits(page, page + 1)) {
            return;
        }

        int count = pageCounts[page];
        int offset = pageOffset(page);
        if (page + 1 < pageCount) {
            int right = page + 1;
            int rightCount = pageCounts[right];
            int move = PackedSortedPageRebalanceAtoms.planDeleteMove(
                    count, rightCount, pageCapacity);
            if (move > 0) {
                int rightOffset = pageOffset(right);
                PackedSortedPageRebalanceAtoms.movePrefixToLeft(
                        values, offset, count, rightOffset, rightCount, move);
                pageCounts[page] = count + move;
                pageCounts[right] = rightCount - move;
                pageMaxima[page] = values[offset + count + move - 1];
                pageMaxima[right] = values[rightOffset + rightCount - move - 1];
                return;
            }
        }

        if (page > 0) {
            int left = page - 1;
            int leftCount = pageCounts[left];
            int move = PackedSortedPageRebalanceAtoms.planDeleteMove(
                    count, leftCount, pageCapacity);
            if (move > 0) {
                int leftOffset = pageOffset(left);
                PackedSortedPageRebalanceAtoms.moveSuffixToRight(
                        values, leftOffset, leftCount, offset, count, move);
                pageCounts[left] = leftCount - move;
                pageCounts[page] = count + move;
                pageMaxima[left] = values[leftOffset + leftCount - move - 1];
                pageMaxima[page] = values[offset + count + move - 1];
            }
        }
    }

    private boolean mergePagesIfFits(int left, int right) {
        if (right != left + 1) {
            throw new IllegalArgumentException("pages must be adjacent");
        }
        int leftCount = pageCounts[left];
        int rightCount = pageCounts[right];
        if (leftCount + rightCount > pageCapacity) {
            return false;
        }

        int leftOffset = pageOffset(left);
        int rightOffset = pageOffset(right);
        System.arraycopy(values, rightOffset, values, leftOffset + leftCount, rightCount);
        pageCounts[left] = leftCount + rightCount;
        pageMaxima[left] = values[leftOffset + leftCount + rightCount - 1];
        removePage(right);
        return true;
    }

    private void ensurePageCapacity(int requiredPages) {
        if (requiredPages <= pageCounts.length) {
            return;
        }
        int newPages = PackedSupport.growArrayCapacity(pageCounts.length, requiredPages);
        values = Arrays.copyOf(values, Math.multiplyExact(newPages, pageCapacity));
        pageCounts = Arrays.copyOf(pageCounts, newPages);
        pageMaxima = Arrays.copyOf(pageMaxima, newPages);
    }

    private int pageOffset(int page) {
        return Math.multiplyExact(page, pageCapacity);
    }

    private static int divideCeiling(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
