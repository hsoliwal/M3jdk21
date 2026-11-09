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

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;
import jdk.m3.collections.LongLongSortedMap;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sorted long-to-long map in sparse fixed-width primitive blocks.
 *
 * <p>Keys/values move together. Count/max lanes index blocks. No mapping object is retained.
 * Normal insertion shifts at most one block; split/growth may copy O(n) primitive data.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongSparseSortedMap.java}
 * blob {@code deb3e25f972ceffaa6d6e17411614bf5d8078c1c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongSparseSortedMap implements LongLongSortedMap {
    private long[] keys;
    private long[] values;
    private int[] counts;
    private long[] maxima;
    private final int blockWidth;
    private final int targetFill;
    private int blocks;
    private int size;
    private int callbacks;

    public PackedLongLongSparseSortedMap() { this(0, 64, 0.75); }
    public PackedLongLongSparseSortedMap(int expectedSize) { this(expectedSize, 64, 0.75); }

    public PackedLongLongSparseSortedMap(int expectedSize, int blockWidth, double targetLoad) {
        if (expectedSize < 0) throw new IllegalArgumentException("expectedSize");
        if (blockWidth < 4 || blockWidth > 4096 || (blockWidth & (blockWidth - 1)) != 0) {
            throw new IllegalArgumentException("blockWidth must be a power of two in 4..4096");
        }
        if (!(targetLoad >= 0.5 && targetLoad < 1.0)) {
            throw new IllegalArgumentException("targetLoad must be in [0.5, 1)");
        }
        this.blockWidth = blockWidth;
        targetFill = Math.min(blockWidth - 1, (int) (blockWidth * targetLoad));
        int capacity = blocksFor(expectedSize);
        keys = new long[slotsFor(capacity)];
        values = new long[keys.length];
        counts = new int[capacity];
        maxima = new long[capacity];
    }

    public int blockWidth() { return blockWidth; }
    public int blockCount() { return blocks; }
    public int capacity() { return keys.length; }
    public long backingArrayPayloadBytes() {
        return 16L * keys.length + 4L * counts.length + 8L * maxima.length;
    }
    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }

    @Override
    public boolean put(long key, long value) {
        writable();
        if (blocks == 0) {
            ensureBlocks(1);
            keys[0] = key; values[0] = value; counts[0] = 1; maxima[0] = key;
            blocks = 1; size = 1; return true;
        }
        int block = Math.min(findBlock(key), blocks - 1);
        int found = search(block, key);
        if (found >= 0) { values[found] = value; return false; }
        PackedArrayCapacity.required(size, 1);
        if (counts[block] == blockWidth) {
            split(block);
            if (key > maxima[block]) block++;
            found = search(block, key);
        }
        int insertion = -found - 1;
        int end = offset(block) + counts[block];
        System.arraycopy(keys, insertion, keys, insertion + 1, end - insertion);
        System.arraycopy(values, insertion, values, insertion + 1, end - insertion);
        keys[insertion] = key; values[insertion] = value;
        counts[block]++; maxima[block] = keys[end]; size++;
        return true;
    }

    /** Admission runs before retention; callback mutation is rejected. */
    public boolean putIf(long key, long value, LongLongPredicate admission) {
        writable(); Objects.requireNonNull(admission, "admission");
        boolean accepted; callbacks++;
        try { accepted = admission.test(key, value); } finally { callbacks--; }
        return accepted && put(key, value);
    }

    @Override public boolean containsKey(long key) { return findIndex(key) >= 0; }
    @Override public long getOrDefault(long key, long fallback) {
        int index = findIndex(key); return index < 0 ? fallback : values[index];
    }
    @Override public long getOrThrow(long key) {
        int index = findIndex(key);
        if (index < 0) throw new IllegalArgumentException("missing key: " + key);
        return values[index];
    }

    @Override
    public boolean remove(long key) {
        writable();
        int block = findBlock(key);
        if (block == blocks) return false;
        int found = search(block, key);
        if (found < 0) return false;
        int end = offset(block) + counts[block];
        int moved = end - found - 1;
        if (moved > 0) {
            System.arraycopy(keys, found + 1, keys, found, moved);
            System.arraycopy(values, found + 1, values, found, moved);
        }
        counts[block]--; size--;
        if (counts[block] == 0) {
            dropBlock(block);
        } else {
            maxima[block] = keys[end - 2];
            if (block + 1 < blocks && counts[block] + counts[block + 1] <= targetFill) {
                merge(block);
            } else if (block > 0 && counts[block - 1] + counts[block] <= targetFill) {
                merge(block - 1);
            }
        }
        return true;
    }

    @Override public long firstKey() {
        if (size == 0) throw new NoSuchElementException();
        return keys[0];
    }
    @Override public long lastKey() {
        if (size == 0) throw new NoSuchElementException();
        return maxima[blocks - 1];
    }
    @Override public long floorKey(long key, long fallback) {
        return predecessor(key, fallback, true);
    }
    @Override public long lowerKey(long key, long fallback) {
        return predecessor(key, fallback, false);
    }
    @Override public long ceilingKey(long key, long fallback) {
        return successor(key, fallback, true);
    }
    @Override public long higherKey(long key, long fallback) {
        return successor(key, fallback, false);
    }

    /** All decisions complete against private primitive snapshots before retained lanes change. */
    @Override
    public int filterInPlace(LongLongPredicate predicate) {
        writable(); Objects.requireNonNull(predicate, "predicate");
        long[] keptKeys = keysToArray(), keptValues = valuesToArray();
        int write = 0; callbacks++;
        try {
            for (int read = 0; read < keptKeys.length; read++) {
                if (predicate.test(keptKeys[read], keptValues[read])) {
                    keptKeys[write] = keptKeys[read];
                    keptValues[write++] = keptValues[read];
                }
            }
        } finally { callbacks--; }
        int removed = size - write;
        if (removed != 0) rebuild(keptKeys, keptValues, write);
        return removed;
    }

    @Override
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer"); callbacks++;
        try {
            for (int block = 0; block < blocks; block++) {
                int start = offset(block), end = start + counts[block];
                for (int index = start; index < end; index++) {
                    consumer.accept(keys[index], values[index]);
                }
            }
        } finally { callbacks--; }
    }

    @Override public void clear() { writable(); size = 0; blocks = 0; }
    public void trimToSize() { writable(); rebuild(keysToArray(), valuesToArray(), size); }

    @Override
    public long[] keysToArray() {
        long[] result = new long[size]; int write = 0;
        for (int block = 0; block < blocks; block++) {
            int count = counts[block];
            System.arraycopy(keys, offset(block), result, write, count); write += count;
        }
        return result;
    }

    @Override
    public long[] valuesToArray() {
        long[] result = new long[size]; int write = 0;
        for (int block = 0; block < blocks; block++) {
            int count = counts[block];
            System.arraycopy(values, offset(block), result, write, count); write += count;
        }
        return result;
    }

    private void writable() {
        if (callbacks != 0) throw new IllegalStateException("mutation during callback");
    }
    private int findIndex(long key) {
        int block = findBlock(key); return block == blocks ? -1 : search(block, key);
    }
    private int findBlock(long key) {
        int low = 0, high = blocks;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (maxima[middle] < key) low = middle + 1; else high = middle;
        }
        return low;
    }
    private int search(int block, long key) {
        int start = offset(block);
        return Arrays.binarySearch(keys, start, start + counts[block], key);
    }
    private long predecessor(long key, long fallback, boolean inclusive) {
        int block = findBlock(key);
        if (block == blocks) return blocks == 0 ? fallback : maxima[blocks - 1];
        int found = search(block, key), start = offset(block);
        int candidate = found >= 0 ? found - (inclusive ? 0 : 1) : -found - 2;
        return candidate < start ? (block == 0 ? fallback : maxima[block - 1]) : keys[candidate];
    }
    private long successor(long key, long fallback, boolean inclusive) {
        int block = findBlock(key);
        if (block == blocks) return fallback;
        int found = search(block, key), end = offset(block) + counts[block];
        int candidate = found >= 0 ? found + (inclusive ? 0 : 1) : -found - 1;
        return candidate == end
                ? (block + 1 == blocks ? fallback : keys[offset(block + 1)])
                : keys[candidate];
    }

    private void split(int block) {
        ensureBlocks(blocks + 1);
        int following = blocks - block - 1;
        if (following > 0) {
            int source = offset(block + 1), target = offset(block + 2);
            int length = following * blockWidth;
            System.arraycopy(keys, source, keys, target, length);
            System.arraycopy(values, source, values, target, length);
            System.arraycopy(counts, block + 1, counts, block + 2, following);
            System.arraycopy(maxima, block + 1, maxima, block + 2, following);
        }
        int half = blockWidth >>> 1, left = offset(block), right = offset(block + 1);
        System.arraycopy(keys, left + half, keys, right, half);
        System.arraycopy(values, left + half, values, right, half);
        counts[block] = half; counts[block + 1] = half;
        maxima[block] = keys[left + half - 1]; maxima[block + 1] = keys[right + half - 1];
        blocks++;
    }

    private void dropBlock(int block) {
        int following = blocks - block - 1;
        if (following > 0) {
            int source = offset(block + 1), target = offset(block);
            int length = following * blockWidth;
            System.arraycopy(keys, source, keys, target, length);
            System.arraycopy(values, source, values, target, length);
            System.arraycopy(counts, block + 1, counts, block, following);
            System.arraycopy(maxima, block + 1, maxima, block, following);
        }
        blocks--; counts[blocks] = 0; maxima[blocks] = 0;
    }

    private void merge(int leftBlock) {
        int rightBlock = leftBlock + 1, left = offset(leftBlock), right = offset(rightBlock);
        int leftCount = counts[leftBlock], rightCount = counts[rightBlock];
        System.arraycopy(keys, right, keys, left + leftCount, rightCount);
        System.arraycopy(values, right, values, left + leftCount, rightCount);
        counts[leftBlock] = leftCount + rightCount; maxima[leftBlock] = maxima[rightBlock];
        dropBlock(rightBlock);
    }

    private void ensureBlocks(int needed) {
        if (needed <= counts.length) return;
        int max = PackedSupport.MAX_ARRAY_SIZE / blockWidth;
        if (needed > max) throw new OutOfMemoryError("sparse map capacity exceeded");
        long grown = counts.length == 0 ? 1L : 2L * counts.length;
        int capacity = (int) Math.min(max, Math.max((long) needed, grown));
        long[] newKeys = Arrays.copyOf(keys, slotsFor(capacity));
        long[] newValues = Arrays.copyOf(values, newKeys.length);
        int[] newCounts = Arrays.copyOf(counts, capacity);
        long[] newMaxima = Arrays.copyOf(maxima, capacity);
        keys = newKeys; values = newValues; counts = newCounts; maxima = newMaxima;
    }

    private int blocksFor(int count) {
        if (count == 0) return 0;
        long capacity = ((long) count + targetFill - 1) / targetFill;
        if (capacity > PackedSupport.MAX_ARRAY_SIZE / blockWidth) {
            throw new OutOfMemoryError("sparse map capacity exceeded");
        }
        return (int) capacity;
    }
    private int slotsFor(int blockCapacity) {
        long slots = (long) blockCapacity * blockWidth;
        if (slots > PackedSupport.MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("sparse map capacity exceeded");
        }
        return (int) slots;
    }
    private int offset(int block) { return block * blockWidth; }

    private void rebuild(long[] sortedKeys, long[] sortedValues, int length) {
        int capacity = blocksFor(length);
        long[] newKeys = new long[slotsFor(capacity)], newValues = new long[newKeys.length];
        int[] newCounts = new int[capacity]; long[] newMaxima = new long[capacity];
        int read = 0, block = 0;
        while (read < length) {
            int count = Math.min(targetFill, length - read), target = block * blockWidth;
            System.arraycopy(sortedKeys, read, newKeys, target, count);
            System.arraycopy(sortedValues, read, newValues, target, count);
            newCounts[block] = count; newMaxima[block] = sortedKeys[read + count - 1];
            read += count; block++;
        }
        keys = newKeys; values = newValues; counts = newCounts; maxima = newMaxima;
        size = length; blocks = block;
    }

    void checkInvariants() {
        if (keys.length != values.length || keys.length != counts.length * blockWidth
                || counts.length != maxima.length || blocks < 0 || blocks > counts.length) {
            throw new IllegalStateException("invalid sparse-map lanes");
        }
        int total = 0; long previous = 0; boolean first = true;
        for (int block = 0; block < blocks; block++) {
            int count = counts[block], start = offset(block);
            if (count <= 0 || count > blockWidth) throw new IllegalStateException("block size");
            for (int i = 0; i < count; i++) {
                long key = keys[start + i];
                if (!first && key <= previous) throw new IllegalStateException("key order");
                previous = key; first = false;
            }
            if (maxima[block] != previous) throw new IllegalStateException("block maximum");
            total += count;
        }
        if (total != size || (size == 0) != (blocks == 0)) {
            throw new IllegalStateException("size mismatch");
        }
    }
}
