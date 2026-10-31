/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Primitive int-to-int linear-probing map with parallel key/value lanes and no entry objects.
 *
 * <p>Adapted from the first-party Synexia donor {@code com.synexia.primitives.IntIntHashMap}:
 * zero marks an empty key slot and a present zero key is tracked separately with its own value,
 * so every {@code int} is a legal key; removal back-shifts the probe run instead of leaving
 * tombstones. The API stays primitive and target-internal, shaped like
 * {@link M3LongLongHashMap}; {@link java.util.HashMap} remains the public JDK contract owner.</p>
 */
public final class M3IntIntHashMap {
    private static final int MIN_CAPACITY = 8;
    private static final float LOAD_FACTOR = 0.65f;

    private int[] keys;
    private int[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZero;
    private int zeroValue;
    private int modCount;

    public M3IntIntHashMap() {
        this(0);
    }

    public M3IntIntHashMap(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("negative expectedSize");
        }
        int capacity = capacityFor(expectedSize);
        keys = new int[capacity];
        values = new int[capacity];
        mask = capacity - 1;
        threshold = thresholdFor(capacity);
    }

    /** Puts a value and returns true only when a new key was inserted. */
    public boolean put(int key, int value) {
        if (key == 0) {
            boolean inserted = !hasZero;
            hasZero = true;
            zeroValue = value;
            if (inserted) {
                size++;
                modCount++;
                if (size > threshold) {
                    rehash(keys.length << 1);
                }
            }
            return inserted;
        }
        int pos = mix(key) & mask;
        int current;
        while ((current = keys[pos]) != 0) {
            if (current == key) {
                values[pos] = value;
                return false;
            }
            pos = (pos + 1) & mask;
        }
        keys[pos] = key;
        values[pos] = value;
        size++;
        modCount++;
        if (size > threshold) {
            rehash(keys.length << 1);
        }
        return true;
    }

    public int getOrDefault(int key, int defaultValue) {
        if (key == 0) {
            return hasZero ? zeroValue : defaultValue;
        }
        int pos = find(key);
        return pos < 0 ? defaultValue : values[pos];
    }

    public boolean containsKey(int key) {
        return key == 0 ? hasZero : find(key) >= 0;
    }

    public boolean containsValue(int value) {
        if (hasZero && zeroValue == value) {
            return true;
        }
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0 && values[i] == value) {
                return true;
            }
        }
        return false;
    }

    /** Replaces only when the key maps to {@code expectedValue}. */
    public boolean replace(int key, int expectedValue, int newValue) {
        if (key == 0) {
            if (!hasZero || zeroValue != expectedValue) {
                return false;
            }
            zeroValue = newValue;
            return true;
        }
        int pos = find(key);
        if (pos < 0 || values[pos] != expectedValue) {
            return false;
        }
        values[pos] = newValue;
        return true;
    }

    /** Adds {@code delta} to the mapping (absent counts as zero) and returns the new value. */
    public int addTo(int key, int delta) {
        int next = getOrDefault(key, 0) + delta;
        put(key, next);
        return next;
    }

    public boolean remove(int key) {
        if (key == 0) {
            if (!hasZero) {
                return false;
            }
            hasZero = false;
            zeroValue = 0;
            size--;
            modCount++;
            return true;
        }
        int pos = find(key);
        if (pos < 0) {
            return false;
        }
        shiftKeys(pos);
        size--;
        modCount++;
        return true;
    }

    /** Removes only when the key maps to {@code expectedValue}. */
    public boolean remove(int key, int expectedValue) {
        if (!containsKey(key) || getOrDefault(key, 0) != expectedValue) {
            return false;
        }
        return remove(key);
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

    /** Retained primitive payload: key and value lanes. */
    public long payloadBytes() {
        return (long) keys.length * Integer.BYTES + (long) values.length * Integer.BYTES;
    }

    public void clear() {
        if (size == 0) {
            return;
        }
        Arrays.fill(keys, 0);
        Arrays.fill(values, 0);
        size = 0;
        hasZero = false;
        zeroValue = 0;
        modCount++;
    }

    /** Visits mappings: the zero key first (if present), then slot order. */
    public void forEach(M3IntIntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (hasZero) {
            consumer.accept(0, zeroValue);
        }
        int remaining = size - (hasZero ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            int key = keys[i];
            if (key != 0) {
                consumer.accept(key, values[i]);
                remaining--;
            }
        }
    }

    public M3IntIterator keyIterator() {
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

    public int[] keysToArray() {
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

    private int find(int key) {
        int pos = mix(key) & mask;
        int current;
        while ((current = keys[pos]) != 0) {
            if (current == key) {
                return pos;
            }
            pos = (pos + 1) & mask;
        }
        return -1;
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
                values[last] = 0;
                return;
            }
            keys[last] = current;
            values[last] = values[pos];
        }
    }

    private void rehash(int newCapacity) {
        if (keys.length >= (1 << 30)) {
            throw new IllegalStateException("maximum hash-table capacity reached");
        }
        int[] oldKeys = keys;
        int[] oldValues = values;
        keys = new int[newCapacity];
        values = new int[newCapacity];
        mask = newCapacity - 1;
        threshold = thresholdFor(newCapacity);
        for (int i = 0; i < oldKeys.length; i++) {
            int key = oldKeys[i];
            if (key != 0) {
                int pos = mix(key) & mask;
                while (keys[pos] != 0) {
                    pos = (pos + 1) & mask;
                }
                keys[pos] = key;
                values[pos] = oldValues[i];
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
