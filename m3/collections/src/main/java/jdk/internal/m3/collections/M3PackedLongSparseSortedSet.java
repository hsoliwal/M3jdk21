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
package jdk.internal.m3.collections;

import jdk.m3.collections.LongCollection;
import jdk.m3.collections.LongIterator;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Sorted unique long values in fixed-width blocks of ONE flat primitive array.
 * Each block has a sorted occupied prefix and spare tail slots. Primitive count/max
 * lanes index the blocks; no node or block object exists. Normal insertion moves
 * at most one block's contents. Splits/growth can copy O(n) data, not O(log n).
 * Target load controls rebuilding/merging, not a global occupancy guarantee.
 * All long values are legal. Not thread-safe. See packed-array-evolution/CONTRACT.md.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSparseSortedSet.java}
 * blob {@code 0eb2141f2dc2cc804f9852bf71294f2c82d7680f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongSparseSortedSet implements LongCollection {
    private long[] values;
    private int[] counts;
    private long[] maxima;
    private final int blockWidth;
    private final int targetFill;
    private int blocks;
    private int size;
    private long version;

    public M3PackedLongSparseSortedSet() {
        this(0, 64, 0.75);
    }

    public M3PackedLongSparseSortedSet(int expectedSize) {
        this(expectedSize, 64, 0.75);
    }

    public M3PackedLongSparseSortedSet(int expectedSize, int blockWidth, double targetLoad) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        if (blockWidth < 4 || blockWidth > 4096 || (blockWidth & (blockWidth - 1)) != 0) {
            throw new IllegalArgumentException("blockWidth must be a power of two in 4..4096");
        }
        if (!(targetLoad >= 0.5 && targetLoad < 1.0)) {
            throw new IllegalArgumentException("targetLoad must be in [0.5, 1)");
        }
        this.blockWidth = blockWidth;
        targetFill = Math.min(blockWidth - 1, (int) (blockWidth * targetLoad));
        int capacity = blocksFor(expectedSize);
        values = new long[slotsFor(capacity)];
        counts = new int[capacity];
        maxima = new long[capacity];
    }

    public int blockWidth() {
        return blockWidth;
    }

    public int blockCount() {
        return blocks;
    }

    /** Allocated payload slots, including inactive reserved blocks. */
    public int capacity() {
        return values.length;
    }

    /** Element bytes only: excludes object/array headers, scalar fields and alignment. */
    public long backingArrayPayloadBytes() {
        return 8L * values.length + 4L * counts.length + 8L * maxima.length;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(long value) {
        int block = findBlock(value);
        return block < blocks && search(block, value) >= 0;
    }

    public boolean add(long value) {
        if (blocks == 0) {
            ensureBlocks(1);
            values[0] = value;
            counts[0] = 1;
            maxima[0] = value;
            blocks = 1;
            size = 1;
            version++;
            return true;
        }
        int block = Math.min(findBlock(value), blocks - 1);
        int found = search(block, value);
        if (found >= 0) {
            return false;
        }
        M3PackedArrayCapacity.required(size, 1);
        if (counts[block] == blockWidth) {
            split(block);
            if (value > maxima[block]) {
                block++;
            }
            found = search(block, value);
        }
        int insertion = -found - 1;
        int end = block * blockWidth + counts[block];
        System.arraycopy(values, insertion, values, insertion + 1, end - insertion);
        values[insertion] = value;
        counts[block]++;
        maxima[block] = values[end];
        size++;
        version++;
        return true;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long expected = version;
        boolean accepted = predicate.test(value);
        checkVersion(expected);
        return accepted && add(value);
    }

    /** Examines the current minimum/maximum before insertion; size disambiguates empty. */
    public boolean addIfRange(long value, M3LongRangeAdmission admission) {
        Objects.requireNonNull(admission, "admission");
        long expected = version;
        boolean accepted = admission.test(value, size,
                size == 0 ? 0 : first(), size == 0 ? 0 : last());
        checkVersion(expected);
        return accepted && add(value);
    }

    /** Sorts/deduplicates a private copy, then merges once with existing values. */
    public int addAll(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) {
            return 0;
        }
        return mergeBatch(Arrays.copyOf(source, source.length), source.length);
    }

    /**
     * Evaluates each input in original order before sorting/retaining. A throwing
     * predicate leaves this set unchanged, except for forbidden callback side effects.
     */
    public int addAllIf(long[] source, LongPredicate predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        long expected = version;
        long[] batch = Arrays.copyOf(source, source.length);
        int kept = 0;
        for (int i = 0; i < source.length; i++) {
            long value = batch[i];
            boolean accepted = predicate.test(value);
            checkVersion(expected);
            if (accepted) {
                batch[kept++] = value;
            }
        }
        return mergeBatch(batch, kept);
    }

    public boolean remove(long value) {
        int block = findBlock(value);
        if (block == blocks) {
            return false;
        }
        int found = search(block, value);
        if (found < 0) {
            return false;
        }
        int end = block * blockWidth + counts[block];
        System.arraycopy(values, found + 1, values, found, end - found - 1);
        counts[block]--;
        size--;
        if (counts[block] == 0) {
            dropBlock(block);
        } else {
            maxima[block] = values[end - 2];
            if (block + 1 < blocks && counts[block] + counts[block + 1] <= targetFill) {
                mergeNeighbors(block);
            } else if (block > 0 && counts[block - 1] + counts[block] <= targetFill) {
                mergeNeighbors(block - 1);
            }
        }
        version++;
        return true;
    }

    /** Transactional retain, using one primitive scratch array plus rebuilt lanes. */
    public int retainIf(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long expected = version;
        long[] kept = toArray();
        int write = 0;
        for (int read = 0; read < kept.length; read++) {
            long value = kept[read];
            boolean accepted = predicate.test(value);
            checkVersion(expected);
            if (accepted) {
                kept[write++] = value;
            }
        }
        int removed = size - write;
        if (removed != 0) {
            rebuild(kept, write);
        }
        return removed;
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
        return maxima[blocks - 1];
    }

    public long floor(long value, long defaultValue) {
        return predecessor(value, defaultValue, true);
    }

    public long lower(long value, long defaultValue) {
        return predecessor(value, defaultValue, false);
    }

    public long ceiling(long value, long defaultValue) {
        return successor(value, defaultValue, true);
    }

    public long higher(long value, long defaultValue) {
        return successor(value, defaultValue, false);
    }

    /** Rebuilds blocks at target fill. Unlike a dense trim, intentional gaps remain. */
    public void trimToSize() {
        rebuild(toArray(), size);
    }

    @Override
    public void clear() {
        if (size != 0) {
            size = 0;
            blocks = 0;
            version++;
        }
    }

    @Override
    public long[] toArray() {
        long[] copy = new long[size];
        int write = 0;
        for (int block = 0; block < blocks; block++) {
            System.arraycopy(values, block * blockWidth, copy, write, counts[block]);
            write += counts[block];
        }
        return copy;
    }

    @Override
    public LongIterator iterator() {
        long expected = version;
        return new LongIterator() {
            private int block;
            private int offset;

            @Override
            public boolean hasNext() {
                checkVersion(expected);
                return block < blocks;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                long value = values[block * blockWidth + offset++];
                if (offset == counts[block]) {
                    block++;
                    offset = 0;
                }
                return value;
            }
        };
    }

    private int mergeBatch(long[] batch, int length) {
        if (length == 0) {
            return 0;
        }
        Arrays.sort(batch, 0, length);
        int unique = 1;
        for (int i = 1; i < length; i++) {
            if (batch[i] != batch[unique - 1]) {
                batch[unique++] = batch[i];
            }
        }
        long[] existing = toArray();
        long[] merged = new long[M3PackedArrayCapacity.required(size, unique)];
        int left = 0;
        int right = 0;
        int write = 0;
        while (left < size && right < unique) {
            long a = existing[left];
            long b = batch[right];
            if (a < b) {
                merged[write++] = a;
                left++;
            } else if (b < a) {
                merged[write++] = b;
                right++;
            } else {
                merged[write++] = a;
                left++;
                right++;
            }
        }
        while (left < size) {
            merged[write++] = existing[left++];
        }
        while (right < unique) {
            merged[write++] = batch[right++];
        }
        int added = write - size;
        if (added != 0) {
            rebuild(merged, write);
        }
        return added;
    }

    private long predecessor(long value, long fallback, boolean inclusive) {
        int block = findBlock(value);
        if (block == blocks) {
            return blocks == 0 ? fallback : maxima[blocks - 1];
        }
        int found = search(block, value);
        int candidate = found >= 0 ? found - (inclusive ? 0 : 1) : -found - 2;
        if (candidate < block * blockWidth) {
            return block == 0 ? fallback : maxima[block - 1];
        }
        return values[candidate];
    }

    private long successor(long value, long fallback, boolean inclusive) {
        int block = findBlock(value);
        if (block == blocks) {
            return fallback;
        }
        int found = search(block, value);
        int candidate = found >= 0 ? found + (inclusive ? 0 : 1) : -found - 1;
        if (candidate == block * blockWidth + counts[block]) {
            return block + 1 == blocks ? fallback : values[(block + 1) * blockWidth];
        }
        return values[candidate];
    }

    private int findBlock(long value) {
        int low = 0;
        int high = blocks;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (maxima[mid] < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    private int search(int block, long value) {
        int start = block * blockWidth;
        return Arrays.binarySearch(values, start, start + counts[block], value);
    }

    private void split(int block) {
        ensureBlocks(blocks + 1);
        int following = blocks - block - 1;
        System.arraycopy(values, (block + 1) * blockWidth,
                values, (block + 2) * blockWidth, following * blockWidth);
        System.arraycopy(counts, block + 1, counts, block + 2, following);
        System.arraycopy(maxima, block + 1, maxima, block + 2, following);
        int half = blockWidth >>> 1;
        System.arraycopy(values, block * blockWidth + half,
                values, (block + 1) * blockWidth, half);
        counts[block] = half;
        counts[block + 1] = half;
        maxima[block] = values[block * blockWidth + half - 1];
        maxima[block + 1] = values[(block + 1) * blockWidth + half - 1];
        blocks++;
    }

    private void dropBlock(int block) {
        int following = blocks - block - 1;
        System.arraycopy(values, (block + 1) * blockWidth,
                values, block * blockWidth, following * blockWidth);
        System.arraycopy(counts, block + 1, counts, block, following);
        System.arraycopy(maxima, block + 1, maxima, block, following);
        blocks--;
        counts[blocks] = 0;
        maxima[blocks] = 0;
    }

    private void mergeNeighbors(int left) {
        System.arraycopy(values, (left + 1) * blockWidth,
                values, left * blockWidth + counts[left], counts[left + 1]);
        counts[left] += counts[left + 1];
        maxima[left] = maxima[left + 1];
        dropBlock(left + 1);
    }

    private void ensureBlocks(int needed) {
        if (needed <= counts.length) {
            return;
        }
        int max = M3PackedSupport.MAX_ARRAY_SIZE / blockWidth;
        if (needed > max) {
            throw new OutOfMemoryError("sparse set capacity exceeded");
        }
        int capacity = (int) Math.min(max, Math.max((long) needed, 2L * counts.length));
        // Allocate every lane before publication: allocation failure preserves old state.
        long[] newValues = Arrays.copyOf(values, slotsFor(capacity));
        int[] newCounts = Arrays.copyOf(counts, capacity);
        long[] newMaxima = Arrays.copyOf(maxima, capacity);
        values = newValues;
        counts = newCounts;
        maxima = newMaxima;
    }

    private int blocksFor(int count) {
        long capacity = Math.max(1L, ((long) count + targetFill - 1) / targetFill);
        if (capacity > M3PackedSupport.MAX_ARRAY_SIZE / blockWidth) {
            throw new OutOfMemoryError("sparse set capacity exceeded");
        }
        return (int) capacity;
    }

    private int slotsFor(int capacity) {
        long slots = (long) capacity * blockWidth;
        if (slots > M3PackedSupport.MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("sparse set capacity exceeded");
        }
        return (int) slots;
    }

    private void rebuild(long[] sorted, int length) {
        int capacity = blocksFor(length);
        long[] newValues = new long[slotsFor(capacity)];
        int[] newCounts = new int[capacity];
        long[] newMaxima = new long[capacity];
        int read = 0;
        int block = 0;
        while (read < length) {
            int count = Math.min(targetFill, length - read);
            System.arraycopy(sorted, read, newValues, block * blockWidth, count);
            newCounts[block] = count;
            newMaxima[block] = sorted[read + count - 1];
            read += count;
            block++;
        }
        values = newValues;
        counts = newCounts;
        maxima = newMaxima;
        size = length;
        blocks = block;
        version++;
    }

    private void checkVersion(long expected) {
        if (expected != version) {
            throw new ConcurrentModificationException();
        }
    }

    /** Package-local mechanical representation check; no allocation, used by tests. */
    void checkInvariants() {
        if (values.length != counts.length * blockWidth || counts.length != maxima.length
                || blocks < 0 || blocks > counts.length || size < 0) {
            throw new IllegalStateException("invalid lanes");
        }
        int total = 0;
        long previous = 0;
        boolean first = true;
        for (int block = 0; block < blocks; block++) {
            if (counts[block] <= 0 || counts[block] > blockWidth) {
                throw new IllegalStateException("invalid block size");
            }
            for (int i = 0; i < counts[block]; i++) {
                long value = values[block * blockWidth + i];
                if (!first && value <= previous) {
                    throw new IllegalStateException("not strictly ordered");
                }
                previous = value;
                first = false;
            }
            if (maxima[block] != previous) {
                throw new IllegalStateException("invalid block maximum");
            }
            total += counts[block];
        }
        if (total != size || (size == 0) != (blocks == 0)) {
            throw new IllegalStateException("size mismatch");
        }
    }
}
