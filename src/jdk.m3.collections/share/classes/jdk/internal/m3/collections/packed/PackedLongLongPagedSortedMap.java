/*******************************************************************************
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

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;
import jdk.m3.collections.LongLongSortedMap;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Mutable sorted primitive {@code long -> long} map using flat fixed-capacity pages.
 *
 * <p>Keys and values are parallel primitive arrays. Page metadata is primitive and
 * proportional to page count rather than entry count. No tree node or Map.Entry is
 * retained per mapping.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongPagedSortedMap.java}
 * blob {@code 218ff6dd0e56cd3eb94797a30a3d927a07e33718} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongPagedSortedMap implements LongLongSortedMap {
    public static final int DEFAULT_PAGE_CAPACITY = 64;

    private final int pageCapacity;
    private long[] keys;
    private long[] values;
    private int[] pageCounts;
    private long[] pageMaxima;
    private int pageCount;
    private int size;

    public PackedLongLongPagedSortedMap() {
        this(0, DEFAULT_PAGE_CAPACITY);
    }

    public PackedLongLongPagedSortedMap(int expectedSize) {
        this(expectedSize, DEFAULT_PAGE_CAPACITY);
    }

    public PackedLongLongPagedSortedMap(int expectedSize, int pageCapacity) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (pageCapacity < 4) {
            throw new IllegalArgumentException("pageCapacity must be >= 4");
        }
        this.pageCapacity = pageCapacity;
        int pages = Math.max(1, divideCeiling(Math.max(1, expectedSize), pageCapacity));
        int slots = Math.multiplyExact(pages, pageCapacity);
        keys = new long[slots];
        values = new long[slots];
        pageCounts = new int[pages];
        pageMaxima = new long[pages];
    }

    /**
     * @return true when the key is new; false when replacing an existing value
     */
    public boolean put(long key, long value) {
        if (pageCount == 0) {
            pageCount = 1;
            keys[0] = key;
            values[0] = value;
            pageCounts[0] = 1;
            pageMaxima[0] = key;
            size = 1;
            return true;
        }

        int page = locatePage(key);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(keys, offset, offset + count, key);
        if (index >= 0) {
            values[index] = value;
            return false;
        }

        if (count == pageCapacity) {
            int insertionInPage = -index - 1 - offset;
            if (!rebalanceFullPageForInsert(page, insertionInPage)) {
                splitPage(page);
            }
            page = locatePage(key);
            offset = pageOffset(page);
            count = pageCounts[page];
            index = Arrays.binarySearch(keys, offset, offset + count, key);
        }

        int insertion = -index - 1;
        int moved = offset + count - insertion;
        if (moved > 0) {
            System.arraycopy(keys, insertion, keys, insertion + 1, moved);
            System.arraycopy(values, insertion, values, insertion + 1, moved);
        }
        keys[insertion] = key;
        values[insertion] = value;
        pageCounts[page] = count + 1;
        pageMaxima[page] = keys[offset + count];
        size++;
        return true;
    }

    public int putAll(long[] sourceKeys, long[] sourceValues) {
        Objects.requireNonNull(sourceKeys, "sourceKeys");
        Objects.requireNonNull(sourceValues, "sourceValues");
        if (sourceKeys.length != sourceValues.length) {
            throw new IllegalArgumentException("key/value length mismatch");
        }
        int before = size;
        for (int i = 0; i < sourceKeys.length; i++) {
            put(sourceKeys[i], sourceValues[i]);
        }
        return size - before;
    }

    /**
     * Merges a nondecreasing primitive key batch in one forward pass.
     * Duplicate incoming keys collapse with the last incoming value winning.
     *
     * @return number of newly-added keys
     */
    public int putAllSorted(long[] sourceKeys, long[] sourceValues) {
        Objects.requireNonNull(sourceKeys, "sourceKeys");
        Objects.requireNonNull(sourceValues, "sourceValues");
        if (sourceKeys.length != sourceValues.length) {
            throw new IllegalArgumentException("key/value length mismatch");
        }
        if (sourceKeys.length == 0) {
            return 0;
        }

        long[] batchKeys = new long[sourceKeys.length];
        long[] batchValues = new long[sourceValues.length];
        int unique = 0;
        for (int i = 0; i < sourceKeys.length; i++) {
            long key = sourceKeys[i];
            if (i > 0 && key < sourceKeys[i - 1]) {
                throw new IllegalArgumentException("source keys must be sorted");
            }
            if (unique > 0 && batchKeys[unique - 1] == key) {
                batchValues[unique - 1] = sourceValues[i];
            } else {
                batchKeys[unique] = key;
                batchValues[unique] = sourceValues[i];
                unique++;
            }
        }

        long[] existingKeys = keysToArray();
        long[] existingValues = valuesToArray();
        long[] mergedKeys = new long[existingKeys.length + unique];
        long[] mergedValues = new long[mergedKeys.length];

        int left = 0;
        int right = 0;
        int write = 0;
        while (left < existingKeys.length && right < unique) {
            long existingKey = existingKeys[left];
            long incomingKey = batchKeys[right];
            if (existingKey < incomingKey) {
                mergedKeys[write] = existingKey;
                mergedValues[write++] = existingValues[left++];
            } else if (incomingKey < existingKey) {
                mergedKeys[write] = incomingKey;
                mergedValues[write++] = batchValues[right++];
            } else {
                mergedKeys[write] = incomingKey;
                mergedValues[write++] = batchValues[right++];
                left++;
            }
        }
        while (left < existingKeys.length) {
            mergedKeys[write] = existingKeys[left];
            mergedValues[write++] = existingValues[left++];
        }
        while (right < unique) {
            mergedKeys[write] = batchKeys[right];
            mergedValues[write++] = batchValues[right++];
        }

        int before = size;
        rebuildFromSorted(
                Arrays.copyOf(mergedKeys, write),
                Arrays.copyOf(mergedValues, write));
        return size - before;
    }

    public boolean containsKey(long key) {
        return findIndex(key) >= 0;
    }

    public long getOrDefault(long key, long defaultValue) {
        int index = findIndex(key);
        return index < 0 ? defaultValue : values[index];
    }

    public long getOrThrow(long key) {
        int index = findIndex(key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    public boolean remove(long key) {
        if (pageCount == 0) {
            return false;
        }
        int page = locatePage(key);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(keys, offset, offset + count, key);
        if (index < 0) {
            return false;
        }

        int moved = offset + count - index - 1;
        if (moved > 0) {
            System.arraycopy(keys, index + 1, keys, index, moved);
            System.arraycopy(values, index + 1, values, index, moved);
        }
        pageCounts[page] = --count;
        size--;

        if (count == 0) {
            removePage(page);
        } else {
            pageMaxima[page] = keys[offset + count - 1];
            rebalanceAdjacentPagesAfterRemove(page);
        }
        return true;
    }

    public long firstKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return keys[0];
    }

    public long lastKey() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        int page = pageCount - 1;
        return keys[pageOffset(page) + pageCounts[page] - 1];
    }

    public long floorKey(long key, long defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int page = locatePage(key);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(keys, offset, offset + count, key);
        if (index >= 0) {
            return keys[index];
        }
        int insertion = -index - 1;
        if (insertion > offset) {
            return keys[insertion - 1];
        }
        if (page == 0) {
            return defaultValue;
        }
        int previous = page - 1;
        return keys[pageOffset(previous) + pageCounts[previous] - 1];
    }

    public long ceilingKey(long key, long defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int page = locatePage(key);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(keys, offset, offset + count, key);
        if (index >= 0) {
            return keys[index];
        }
        int insertion = -index - 1;
        if (insertion < offset + count) {
            return keys[insertion];
        }
        return page + 1 < pageCount ? keys[pageOffset(page + 1)] : defaultValue;
    }

    public long lowerKey(long key, long defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int page = locatePage(key);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(keys, offset, offset + count, key);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        if (candidate >= offset) {
            return keys[candidate];
        }
        if (page == 0) {
            return defaultValue;
        }
        int previous = page - 1;
        return keys[pageOffset(previous) + pageCounts[previous] - 1];
    }

    public long higherKey(long key, long defaultValue) {
        if (size == 0) {
            return defaultValue;
        }
        int page = locatePage(key);
        int offset = pageOffset(page);
        int count = pageCounts[page];
        int index = Arrays.binarySearch(keys, offset, offset + count, key);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        if (candidate < offset + count) {
            return keys[candidate];
        }
        return page + 1 < pageCount ? keys[pageOffset(page + 1)] : defaultValue;
    }

    /** Returns key/value pairs in the half-open key range [fromInclusive, toExclusive). */
    public PackedLongPairs range(long fromInclusive, long toExclusive) {
        PackedLongPairs result = new PackedLongPairs();
        if (fromInclusive >= toExclusive || size == 0) {
            return result;
        }
        forEach((key, value) -> {
            if (key >= fromInclusive && key < toExclusive) {
                result.add(key, value);
            }
        });
        return result;
    }

    public int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long[] keyCopy = keysToArray();
        long[] valueCopy = valuesToArray();
        clear();
        int removed = 0;
        for (int i = 0; i < keyCopy.length; i++) {
            if (predicate.test(keyCopy[i], valueCopy[i])) {
                put(keyCopy[i], valueCopy[i]);
            } else {
                removed++;
            }
        }
        return removed;
    }

    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int page = 0; page < pageCount; page++) {
            int offset = pageOffset(page);
            int count = pageCounts[page];
            for (int i = 0; i < count; i++) {
                consumer.accept(keys[offset + i], values[offset + i]);
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int pageCount() {
        return pageCount;
    }

    public int pageCapacity() {
        return pageCapacity;
    }

    public void clear() {
        Arrays.fill(pageCounts, 0);
        pageCount = 0;
        size = 0;
    }

    public long[] keysToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int page = 0; page < pageCount; page++) {
            int count = pageCounts[page];
            System.arraycopy(keys, pageOffset(page), result, write, count);
            write += count;
        }
        return result;
    }

    public long[] valuesToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int page = 0; page < pageCount; page++) {
            int count = pageCounts[page];
            System.arraycopy(values, pageOffset(page), result, write, count);
            write += count;
        }
        return result;
    }

    private void rebuildFromSorted(long[] sortedKeys, long[] sortedValues) {
        int requiredPages = Math.max(1, divideCeiling(Math.max(1, sortedKeys.length), pageCapacity));
        ensurePageCapacity(requiredPages);
        Arrays.fill(pageCounts, 0);
        pageCount = sortedKeys.length == 0 ? 0 : requiredPages;
        size = sortedKeys.length;

        int source = 0;
        for (int page = 0; page < pageCount; page++) {
            int count = Math.min(pageCapacity, sortedKeys.length - source);
            int offset = pageOffset(page);
            System.arraycopy(sortedKeys, source, keys, offset, count);
            System.arraycopy(sortedValues, source, values, offset, count);
            pageCounts[page] = count;
            pageMaxima[page] = keys[offset + count - 1];
            source += count;
        }
    }

    private int findIndex(long key) {
        if (pageCount == 0) {
            return -1;
        }
        int page = locatePage(key);
        int offset = pageOffset(page);
        return Arrays.binarySearch(keys, offset, offset + pageCounts[page], key);
    }

    private int locatePage(long key) {
        int low = 0;
        int high = pageCount - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (key <= pageMaxima[mid]) {
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
     * <p>Only primitive lanes move. The public map contract, key order and page identity model stay
     * unchanged. Search-strategy adaptation remains separately benchmark-gated.</p>
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
                keys, leftOffset, leftCount, offset, count, move);
        PackedSortedPageRebalanceAtoms.movePrefixToLeft(
                values, leftOffset, leftCount, offset, count, move);

        pageCounts[left] = leftCount + move;
        pageCounts[page] = count - move;
        pageMaxima[left] = keys[leftOffset + leftCount + move - 1];
        pageMaxima[page] = keys[offset + count - move - 1];
    }

    private void rebalanceToRight(int page, int move) {
        int right = page + 1;
        int offset = pageOffset(page);
        int rightOffset = pageOffset(right);
        int count = pageCounts[page];
        int rightCount = pageCounts[right];

        PackedSortedPageRebalanceAtoms.moveSuffixToRight(
                keys, offset, count, rightOffset, rightCount, move);
        PackedSortedPageRebalanceAtoms.moveSuffixToRight(
                values, offset, count, rightOffset, rightCount, move);

        pageCounts[page] = count - move;
        pageCounts[right] = rightCount + move;
        pageMaxima[page] = keys[offset + count - move - 1];
        pageMaxima[right] = keys[rightOffset + rightCount + move - 1];
    }

    private void splitPage(int page) {
        ensurePageCapacity(pageCount + 1);
        int pagesAfter = pageCount - page - 1;
        if (pagesAfter > 0) {
            int sourceOffset = pageOffset(page + 1);
            int length = Math.multiplyExact(pagesAfter, pageCapacity);
            System.arraycopy(keys, sourceOffset, keys, sourceOffset + pageCapacity, length);
            System.arraycopy(values, sourceOffset, values, sourceOffset + pageCapacity, length);
            System.arraycopy(pageCounts, page + 1, pageCounts, page + 2, pagesAfter);
            System.arraycopy(pageMaxima, page + 1, pageMaxima, page + 2, pagesAfter);
        }

        int count = pageCounts[page];
        int leftCount = count >>> 1;
        int rightCount = count - leftCount;
        int leftOffset = pageOffset(page);
        int rightOffset = pageOffset(page + 1);
        System.arraycopy(keys, leftOffset + leftCount, keys, rightOffset, rightCount);
        System.arraycopy(values, leftOffset + leftCount, values, rightOffset, rightCount);

        pageCounts[page] = leftCount;
        pageCounts[page + 1] = rightCount;
        pageMaxima[page] = keys[leftOffset + leftCount - 1];
        pageMaxima[page + 1] = keys[rightOffset + rightCount - 1];
        pageCount++;
    }

    private void removePage(int page) {
        if (pageCount == 1) {
            pageCount = 0;
            return;
        }
        int pagesAfter = pageCount - page - 1;
        if (pagesAfter > 0) {
            int destination = pageOffset(page);
            int source = pageOffset(page + 1);
            int length = Math.multiplyExact(pagesAfter, pageCapacity);
            System.arraycopy(keys, source, keys, destination, length);
            System.arraycopy(values, source, values, destination, length);
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
                        keys, offset, count, rightOffset, rightCount, move);
                PackedSortedPageRebalanceAtoms.movePrefixToLeft(
                        values, offset, count, rightOffset, rightCount, move);
                pageCounts[page] = count + move;
                pageCounts[right] = rightCount - move;
                pageMaxima[page] = keys[offset + count + move - 1];
                pageMaxima[right] = keys[rightOffset + rightCount - move - 1];
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
                        keys, leftOffset, leftCount, offset, count, move);
                PackedSortedPageRebalanceAtoms.moveSuffixToRight(
                        values, leftOffset, leftCount, offset, count, move);
                pageCounts[left] = leftCount - move;
                pageCounts[page] = count + move;
                pageMaxima[left] = keys[leftOffset + leftCount - move - 1];
                pageMaxima[page] = keys[offset + count + move - 1];
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
        System.arraycopy(keys, rightOffset, keys, leftOffset + leftCount, rightCount);
        System.arraycopy(values, rightOffset, values, leftOffset + leftCount, rightCount);
        pageCounts[left] = leftCount + rightCount;
        pageMaxima[left] = keys[leftOffset + leftCount + rightCount - 1];
        removePage(right);
        return true;
    }

    private void ensurePageCapacity(int requiredPages) {
        if (requiredPages <= pageCounts.length) {
            return;
        }
        int newPages = PackedSupport.growArrayCapacity(pageCounts.length, requiredPages);
        int slots = Math.multiplyExact(newPages, pageCapacity);
        keys = Arrays.copyOf(keys, slots);
        values = Arrays.copyOf(values, slots);
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
