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

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongSortedSet;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Sorted primitive long set backed by one sorted {@code long[]}.
 *
 * <p>Single inserts use binary search plus arraycopy. Batch inserts sort/deduplicate
 * once and merge in one forward pass, avoiding one shift per incoming element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSortedSet.java}
 * blob {@code be9baccc01146c512ee5f62cddbce3dd08227e0b} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongSortedSet implements LongSortedSet {
    private long[] values;
    private int size;

    public PackedLongSortedSet() {
        this(8);
    }

    public PackedLongSortedSet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        values = new long[Math.max(1, expectedSize)];
    }

    public boolean add(long value) {
        int index = Arrays.binarySearch(values, 0, size, value);
        if (index >= 0) {
            return false;
        }
        int insertionPoint = -index - 1;
        ensureCapacity(PackedArrayCapacity.required(size, 1));
        System.arraycopy(values, insertionPoint, values, insertionPoint + 1, size - insertionPoint);
        values[insertionPoint] = value;
        size++;
        return true;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && add(value);
    }

    /**
     * Sorts/deduplicates the batch, then performs a single merge.
     *
     * @return number of newly-added values
     */
    public int addAll(long[] source) {
        Objects.requireNonNull(source, "source");
        if (source.length == 0) {
            return 0;
        }
        long[] batch = Arrays.copyOf(source, source.length);
        int unique = PackedLongArrayOps.sortUniqueUnchecked(batch, batch.length);
        int required = PackedArrayCapacity.required(size, unique);
        if (size == 0 || values[size - 1] < batch[0]) {
            if (required > values.length) { values = Arrays.copyOf(values, required); }
            System.arraycopy(batch, 0, values, size, unique);
            size = required;
            return unique;
        }
        if (batch[unique - 1] < values[0]) {
            if (required > values.length) { values = Arrays.copyOf(values, required); }
            System.arraycopy(values, 0, values, unique, size);
            System.arraycopy(batch, 0, values, 0, unique);
            size = required;
            return unique;
        }

        int previousSize = size;
        long[] merged = new long[Math.max(values.length, required)];
        int write = PackedLongArrayOps.unionUniqueUnchecked(values, size, batch, unique, merged);
        values = merged;
        size = write;
        return size - previousSize;
    }

    public boolean remove(long value) {
        int index = Arrays.binarySearch(values, 0, size, value);
        if (index < 0) {
            return false;
        }
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        return true;
    }

    @Override
    public boolean contains(long value) {
        return Arrays.binarySearch(values, 0, size, value) >= 0;
    }

    public int indexOf(long value) {
        return Arrays.binarySearch(values, 0, size, value);
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
        return values[size - 1];
    }

    public long floor(long value, long defaultValue) {
        int index = Arrays.binarySearch(values, 0, size, value);
        if (index >= 0) {
            return values[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == 0 ? defaultValue : values[insertionPoint - 1];
    }

    public long ceiling(long value, long defaultValue) {
        int index = Arrays.binarySearch(values, 0, size, value);
        if (index >= 0) {
            return values[index];
        }
        int insertionPoint = -index - 1;
        return insertionPoint == size ? defaultValue : values[insertionPoint];
    }

    public long lower(long value, long defaultValue) {
        int index = Arrays.binarySearch(values, 0, size, value);
        int candidate = index >= 0 ? index - 1 : -index - 2;
        return candidate < 0 ? defaultValue : values[candidate];
    }

    public long higher(long value, long defaultValue) {
        int index = Arrays.binarySearch(values, 0, size, value);
        int candidate = index >= 0 ? index + 1 : -index - 1;
        return candidate >= size ? defaultValue : values[candidate];
    }

    /**
     * Keeps matching values in-place. On predicate failure, prior rejections are
     * committed, but the throwing value and untested suffix remain intact.
     * The predicate must not mutate this collection.
     */
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int originalSize = size;
        int read = 0;
        int write = 0;
        try {
            for (; read < originalSize; read++) {
                long value = values[read];
                if (predicate.test(value)) {
                    values[write++] = value;
                }
            }
        } finally {
            // If the callback throws, preserve its element and the unread suffix.
            System.arraycopy(values, read, values, write, originalSize - read);
            size = write + originalSize - read;
        }
        return originalSize - size;
    }

    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= values.length) {
            return;
        }
        values = PackedFlatArrays.grow(values, minimumCapacity);
    }

    public void trimToSize() {
        values = Arrays.copyOf(values, Math.max(1, size));
    }

    public int capacity() {
        return values.length;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        size = 0;
    }

    @Override
    public long[] toArray() {
        return Arrays.copyOf(values, size);
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return values[index++];
            }
        };
    }
}
