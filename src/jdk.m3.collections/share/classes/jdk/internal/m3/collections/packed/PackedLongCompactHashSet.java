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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Memory-minimal primitive long hash set.
 *
 * <p>Uses sentinel-empty linear probing with backward-shift deletion, a pattern used
 * by mature primitive FOSS collections. Key {@code 0L} is stored separately so the
 * slot array itself needs no occupancy/tombstone byte.
 *
 * <p>Retained per hash slot: exactly one {@code long} key (8 bytes payload).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongCompactHashSet.java}
 * blob {@code eb860e34b8f119d40e009ef92a14a4beb59dc878} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongCompactHashSet implements PackedMutableLongSet {
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private long[] keys;
    private int size;
    private boolean hasZeroKey;
    private final float loadFactor;
    private int threshold;

    public PackedLongCompactHashSet() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongCompactHashSet(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongCompactHashSet(int expectedSize, float loadFactor) {
        this.loadFactor = validateLoadFactor(loadFactor);
        int capacity = PackedSupport.tableSizeForExpected(expectedSize, loadFactor);
        keys = new long[capacity];
        threshold = threshold(capacity);
    }

    @Override
    public boolean add(long value) {
        if (value == 0L) {
            if (hasZeroKey) {
                return false;
            }
            hasZeroKey = true;
            size++;
            return true;
        }

        if (nonZeroSize() + 1 > threshold) {
            rehash(PackedSupport.nextTableCapacity(keys.length));
        }

        int mask = keys.length - 1;
        int index = PackedSupport.mix(value) & mask;
        while (true) {
            long existing = keys[index];
            if (existing == 0L) {
                keys[index] = value;
                size++;
                return true;
            }
            if (existing == value) {
                return false;
            }
            index = (index + 1) & mask;
        }
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && add(value);
    }

    public int addAll(long[] values) {
        Objects.requireNonNull(values, "values");
        int before = size;
        ensureExpectedSize(size + values.length);
        for (long value : values) {
            add(value);
        }
        return size - before;
    }

    @Override
    public boolean contains(long value) {
        if (value == 0L) {
            return hasZeroKey;
        }
        return findIndex(value) >= 0;
    }

    @Override
    public boolean remove(long value) {
        if (value == 0L) {
            if (!hasZeroKey) {
                return false;
            }
            hasZeroKey = false;
            size--;
            return true;
        }

        int index = findIndex(value);
        if (index < 0) {
            return false;
        }
        shiftKeys(index);
        size--;
        return true;
    }

    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;

        if (hasZeroKey && !predicate.test(0L)) {
            hasZeroKey = false;
            size--;
        }

        for (int slot = 0; slot < keys.length; ) {
            long value = keys[slot];
            if (value != 0L && !predicate.test(value)) {
                shiftKeys(slot);
                size--;
            } else {
                slot++;
            }
        }
        return before - size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return keys.length;
    }

    public float loadFactor() {
        return loadFactor;
    }

    @Override
    public void clear() {
        Arrays.fill(keys, 0L);
        hasZeroKey = false;
        size = 0;
    }

    @Override
    public void trimToSize() {
        int target = PackedSupport.tableSizeForExpected(nonZeroSize(), loadFactor);
        if (target < keys.length) {
            rehash(target);
        }
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        int write = 0;
        if (hasZeroKey) {
            result[write++] = 0L;
        }
        for (long key : keys) {
            if (key != 0L) {
                result[write++] = key;
            }
        }
        return result;
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private boolean zeroPending = hasZeroKey;
            private int index;
            private int emitted;

            @Override
            public boolean hasNext() {
                return emitted < size;
            }

            @Override
            public long nextLong() {
                if (zeroPending) {
                    zeroPending = false;
                    emitted++;
                    return 0L;
                }
                while (index < keys.length) {
                    long value = keys[index++];
                    if (value != 0L) {
                        emitted++;
                        return value;
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    private int findIndex(long value) {
        int mask = keys.length - 1;
        int index = PackedSupport.mix(value) & mask;
        while (true) {
            long existing = keys[index];
            if (existing == 0L) {
                return -1;
            }
            if (existing == value) {
                return index;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Close a deletion gap while preserving every key's linear-probe lookup path.
     */
    private void shiftKeys(int gap) {
        int mask = keys.length - 1;
        int last;
        int slot;
        long current;

        while (true) {
            gap = ((last = gap) + 1) & mask;
            while (true) {
                current = keys[gap];
                if (current == 0L) {
                    keys[last] = 0L;
                    return;
                }
                slot = PackedSupport.mix(current) & mask;
                if (last <= gap
                        ? last >= slot || slot > gap
                        : last >= slot && slot > gap) {
                    break;
                }
                gap = (gap + 1) & mask;
            }
            keys[last] = current;
        }
    }

    private void ensureExpectedSize(int expectedTotalSize) {
        int expectedNonZero = Math.max(0, expectedTotalSize - (hasZeroKey ? 1 : 0));
        int needed = PackedSupport.tableSizeForExpected(expectedNonZero, loadFactor);
        if (needed > keys.length) {
            rehash(needed);
        }
    }

    private void rehash(int capacity) {
        long[] oldKeys = keys;
        keys = new long[capacity];
        threshold = threshold(capacity);

        int mask = capacity - 1;
        for (long key : oldKeys) {
            if (key == 0L) {
                continue;
            }
            int index = PackedSupport.mix(key) & mask;
            while (keys[index] != 0L) {
                index = (index + 1) & mask;
            }
            keys[index] = key;
        }
    }

    private int nonZeroSize() {
        return size - (hasZeroKey ? 1 : 0);
    }

    private int threshold(int capacity) {
        return Math.min(capacity - 1, Math.max(1, (int) Math.ceil(capacity * loadFactor)));
    }

    private static float validateLoadFactor(float loadFactor) {
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor");
        }
        return loadFactor;
    }
}
