/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.primitives;

import java.util.Arrays;

/** Open-addressing int->int map. Parallel primitive arrays; no entry/node objects. */
public final class IntIntHashMap {
    @FunctionalInterface public interface Consumer { void accept(int key, int value); }
    private int[] keys;
    private int[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZero;
    private int zeroValue;
    private final float loadFactor;

    public IntIntHashMap() { this(16, DenseHash.DEFAULT_LOAD_FACTOR); }
    public IntIntHashMap(int expected) { this(expected, DenseHash.DEFAULT_LOAD_FACTOR); }
    public IntIntHashMap(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int cap = DenseHash.capacityFor(expected, loadFactor);
        keys = new int[cap];
        values = new int[cap];
        mask = cap - 1;
        threshold = DenseHash.threshold(cap, loadFactor);
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return keys.length; }
    public long payloadBytes() {
        return (long) keys.length * 4 + (long) values.length * 4;
    }
    public boolean containsKey(int key) {
        if (key == 0) return hasZero;
        return find(key) >= 0;
    }
    public int get(int key) {
        if (key == 0) return hasZero ? zeroValue : 0;
        int p = find(key);
        return p < 0 ? 0 : values[p];
    }
    public int getOrDefault(int key, int defaultValue) {
        if (key == 0) return hasZero ? zeroValue : defaultValue;
        int p = find(key);
        return p < 0 ? defaultValue : values[p];
    }
    public int put(int key, int value) {
        if (key == 0) {
            int old = zeroValue;
            if (!hasZero) { hasZero = true; size++; }
            zeroValue = value;
            if (size > threshold) rehash(keys.length << 1);
            return old;
        }
        int pos = DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[pos]) != 0) {
            if (cur == key) {
                int old = values[pos];
                values[pos] = value;
                return old;
            }
            pos = (pos + 1) & mask;
        }
        insertAbsent(pos, key, value);
        return 0;
    }
    public int remove(int key) {
        if (key == 0) {
            if (!hasZero) return 0;
            hasZero = false;
            size--;
            int old = zeroValue;
            zeroValue = 0;
            return old;
        }
        int pos = DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[pos]) != 0) {
            if (cur == key) {
                int old = values[pos];
                shiftKeys(pos);
                size--;
                return old;
            }
            pos = (pos + 1) & mask;
        }
        return 0;
    }
    public int addTo(int key, int delta) {
        if (key == 0) {
            int next = (hasZero ? zeroValue : 0) + delta;
            put(0, next);
            return next;
        }
        int pos = DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[pos]) != 0) {
            if (cur == key) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        insertAbsent(pos, key, delta);
        return delta;
    }
    public void clear() {
        Arrays.fill(keys, 0);
        Arrays.fill(values, 0);
        size = 0;
        hasZero = false;
        zeroValue = 0;
    }
    public void forEach(Consumer action) {
        if (hasZero) action.accept(0, zeroValue);
        int remaining = size - (hasZero ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            int key = keys[i];
            if (key != 0) {
                action.accept(key, values[i]);
                remaining--;
            }
        }
    }
    /** Reuses the caller's discovered empty slot; preserve insertion and growth order. */
    private void insertAbsent(int pos, int key, int value) {
        keys[pos] = key;
        values[pos] = value;
        if (++size > threshold) rehash(keys.length << 1);
    }

    private int find(int key) {
        int pos = DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[pos]) != 0) {
            if (cur == key) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }
    private void shiftKeys(int pos) {
        for (;;) {
            int last = pos;
            pos = (pos + 1) & mask;
            int cur;
            while ((cur = keys[pos]) != 0) {
                int slot = DenseHash.mix(cur) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            if (cur == 0) {
                keys[last] = 0;
                values[last] = 0;
                return;
            }
            keys[last] = cur;
            values[last] = values[pos];
        }
    }
    private void rehash(int newCap) {
        int[] oldK = keys;
        int[] oldV = values;
        keys = new int[newCap];
        values = new int[newCap];
        mask = newCap - 1;
        threshold = DenseHash.threshold(newCap, loadFactor);
        for (int i = 0; i < oldK.length; i++) {
            int key = oldK[i];
            if (key != 0) {
                int p = DenseHash.mix(key) & mask;
                while (keys[p] != 0) p = (p + 1) & mask;
                keys[p] = key;
                values[p] = oldV[i];
            }
        }
    }
}
