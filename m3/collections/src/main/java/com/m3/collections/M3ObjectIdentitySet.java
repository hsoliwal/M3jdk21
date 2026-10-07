/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Flat reference-identity set: {@code ==} membership and {@link System#identityHashCode} hashing
 * over one reference lane, no entry objects.
 *
 * <p>Object-space owner for the {@code Collections.newSetFromMap(new IdentityHashMap<>())} shape.
 * {@code null} is the empty-slot marker and a present {@code null} member is tracked in one
 * collection-level flag. Removal back-shifts the probe run. {@link #forEach} is fail-fast after
 * the traversal. Adapted from the first-party Synexia donor
 * {@code com.synexia.primitives.DenseIdentityHashSet}; proving ground only.</p>
 */
public final class M3ObjectIdentitySet<E> {
    private static final int MIN_CAPACITY = 8;
    private static final float LOAD_FACTOR = 0.65f;

    private Object[] keys;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNull;
    private int modCount;

    public M3ObjectIdentitySet() {
        this(0);
    }

    public M3ObjectIdentitySet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new Object[capacity];
        mask = capacity - 1;
        threshold = thresholdFor(capacity);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return keys.length;
    }

    /** Retained reference slots: the key lane. */
    public int referenceSlots() {
        return keys.length;
    }

    public boolean contains(Object key) {
        return key == null ? hasNull : find(key) >= 0;
    }

    public boolean add(E key) {
        if (key == null) {
            if (hasNull) {
                return false;
            }
            hasNull = true;
            size++;
            modCount++;
            if (size > threshold) {
                rehash(keys.length << 1);
            }
            return true;
        }
        int index = hash(key) & mask;
        Object current;
        while ((current = keys[index]) != null) {
            if (current == key) {
                return false;
            }
            index = (index + 1) & mask;
        }
        keys[index] = key;
        size++;
        modCount++;
        if (size > threshold) {
            rehash(keys.length << 1);
        }
        return true;
    }

    public boolean remove(Object key) {
        if (key == null) {
            if (!hasNull) {
                return false;
            }
            hasNull = false;
            size--;
            modCount++;
            return true;
        }
        int index = find(key);
        if (index < 0) {
            return false;
        }
        shift(index);
        size--;
        modCount++;
        return true;
    }

    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(keys, null);
        hasNull = false;
        size = 0;
        modCount++;
    }

    /**
     * Visits members ({@code null} first, then slot order). A structural modification during the
     * traversal is reported with {@link ConcurrentModificationException} afterwards.
     */
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int expectedModCount = modCount;
        if (hasNull) {
            consumer.accept(null);
        }
        int remaining = size - (hasNull ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                consumer.accept((E) key);
                remaining--;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /** Snapshot of the members in traversal order ({@code null} first when present). */
    public Object[] toArray() {
        Object[] result = new Object[size];
        int cursor = 0;
        if (hasNull) {
            result[cursor++] = null;
        }
        for (Object key : keys) {
            if (key != null) {
                result[cursor++] = key;
            }
        }
        return result;
    }

    private int find(Object key) {
        int index = hash(key) & mask;
        Object current;
        while ((current = keys[index]) != null) {
            if (current == key) {
                return index;
            }
            index = (index + 1) & mask;
        }
        return -1;
    }

    private void shift(int index) {
        for (;;) {
            int last = index;
            index = (index + 1) & mask;
            Object current;
            while ((current = keys[index]) != null) {
                int slot = hash(current) & mask;
                if (last <= index ? last >= slot || slot > index : last >= slot && slot > index) {
                    break;
                }
                index = (index + 1) & mask;
            }
            if (current == null) {
                keys[last] = null;
                return;
            }
            keys[last] = current;
        }
    }

    private void rehash(int newCapacity) {
        if (keys.length >= (1 << 30)) {
            throw new IllegalStateException("maximum hash-table capacity reached");
        }
        Object[] old = keys;
        keys = new Object[newCapacity];
        mask = newCapacity - 1;
        threshold = thresholdFor(newCapacity);
        for (Object key : old) {
            if (key != null) {
                int index = hash(key) & mask;
                while (keys[index] != null) {
                    index = (index + 1) & mask;
                }
                keys[index] = key;
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

    private static int hash(Object key) {
        int x = System.identityHashCode(key);
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }
}
