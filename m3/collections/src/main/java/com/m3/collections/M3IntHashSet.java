/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Primitive-int linear-probing hash set with backward-shift deletion and no entry objects.
 *
 * <p>Adapted from the first-party Synexia donor {@code com.synexia.primitives.IntHashSet}: the
 * key lane uses zero as the empty marker and tracks a present zero key separately, so every
 * {@code int} is a legal member and no occupancy lane is needed. Removal back-shifts the probe
 * run instead of leaving tombstones. JDK-shaped set semantics with fail-fast primitive iteration;
 * iteration order is zero first (if present), then slot order. This is an M3 proving-ground
 * backend, not a public {@code HashSet} replacement.</p>
 */
public final class M3IntHashSet implements M3IntCollection {
    private static final int MIN_CAPACITY = 8;
    private static final float LOAD_FACTOR = 0.65f;

    private int[] keys;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZero;
    private int modCount;

    public M3IntHashSet() {
        this(0);
    }

    public M3IntHashSet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new int[capacity];
        mask = capacity - 1;
        threshold = thresholdFor(capacity);
    }

    public boolean add(int key) {
        if (key == 0) {
            if (hasZero) {
                return false;
            }
            hasZero = true;
            size++;
            modCount++;
            if (size > threshold) {
                rehash(keys.length << 1);
            }
            return true;
        }
        int pos = mix(key) & mask;
        int current;
        while ((current = keys[pos]) != 0) {
            if (current == key) {
                return false;
            }
            pos = (pos + 1) & mask;
        }
        keys[pos] = key;
        size++;
        modCount++;
        if (size > threshold) {
            rehash(keys.length << 1);
        }
        return true;
    }

    public boolean remove(int key) {
        if (key == 0) {
            if (!hasZero) {
                return false;
            }
            hasZero = false;
            size--;
            modCount++;
            return true;
        }
        int pos = mix(key) & mask;
        int current;
        while ((current = keys[pos]) != 0) {
            if (current == key) {
                shiftKeys(pos);
                size--;
                modCount++;
                return true;
            }
            pos = (pos + 1) & mask;
        }
        return false;
    }

    @Override
    public boolean contains(int key) {
        if (key == 0) {
            return hasZero;
        }
        int pos = mix(key) & mask;
        int current;
        while ((current = keys[pos]) != 0) {
            if (current == key) {
                return true;
            }
            pos = (pos + 1) & mask;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return keys.length;
    }

    /** Retained primitive payload: the key lane only. */
    public long payloadBytes() {
        return (long) keys.length * Integer.BYTES;
    }

    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(keys, 0);
        size = 0;
        hasZero = false;
        modCount++;
    }

    @Override
    public int[] toArray() {
        int[] result = new int[size];
        int cursor = 0;
        if (hasZero) {
            result[cursor++] = 0;
        }
        for (int key : keys) {
            if (key != 0) {
                result[cursor++] = key;
            }
        }
        return result;
    }

    @Override
    public M3IntIterator iterator() {
        int expectedModCount = modCount;
        return new M3IntIterator() {
            private int cursor = hasZero ? -1 : 0;
            private int emitted;

            @Override
            public boolean hasNext() {
                checkForComodification(expectedModCount);
                return emitted < size;
            }

            @Override
            public int nextInt() {
                checkForComodification(expectedModCount);
                if (cursor < 0) {
                    cursor = 0;
                    emitted++;
                    return 0;
                }
                while (cursor < keys.length && keys[cursor] == 0) {
                    cursor++;
                }
                if (cursor >= keys.length) {
                    throw new NoSuchElementException();
                }
                emitted++;
                return keys[cursor++];
            }
        };
    }

    private void shiftKeys(int pos) {
        for (;;) {
            int last = pos;
            pos = (pos + 1) & mask;
            int current;
            while ((current = keys[pos]) != 0) {
                int slot = mix(current) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) {
                    break;
                }
                pos = (pos + 1) & mask;
            }
            if (current == 0) {
                keys[last] = 0;
                return;
            }
            keys[last] = current;
        }
    }

    private void rehash(int newCapacity) {
        if (keys.length >= (1 << 30)) {
            throw new IllegalStateException("maximum hash-table capacity reached");
        }
        int[] old = keys;
        keys = new int[newCapacity];
        mask = newCapacity - 1;
        threshold = thresholdFor(newCapacity);
        for (int key : old) {
            if (key != 0) {
                int pos = mix(key) & mask;
                while (keys[pos] != 0) {
                    pos = (pos + 1) & mask;
                }
                keys[pos] = key;
            }
        }
    }

    private static int capacityFor(int expectedSize) {
        long needed = Math.max(MIN_CAPACITY, (long) Math.ceil(expectedSize / (double) LOAD_FACTOR));
        if (needed > (1L << 30)) {
            throw new IllegalArgumentException("expectedSize too large");
        }
        int capacity = 1;
        while (capacity < needed) {
            capacity <<= 1;
        }
        return capacity;
    }

    private static int thresholdFor(int capacity) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * LOAD_FACTOR)));
    }

    private static int mix(int value) {
        int x = value;
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    private void checkForComodification(int expectedModCount) {
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
