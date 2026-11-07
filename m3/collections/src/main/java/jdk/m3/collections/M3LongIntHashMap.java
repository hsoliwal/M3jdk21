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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Open-addressing long->int map. Parallel primitive arrays; no entry/node objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongIntHashMap.java}
 * blob {@code 0f8d83b92f1bc1f2f7295de4a385533b9b050ab4} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongIntHashMap {
    @FunctionalInterface public interface Consumer { void accept(long key, int value); }
    private long[] keys;
    private int[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZero;
    private int zeroValue;
    private final float loadFactor;

    public M3LongIntHashMap() { this(16, M3DenseHash.DEFAULT_LOAD_FACTOR); }
    public M3LongIntHashMap(int expected) { this(expected, M3DenseHash.DEFAULT_LOAD_FACTOR); }
    public M3LongIntHashMap(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int cap = M3DenseHash.capacityFor(expected, loadFactor);
        keys = new long[cap];
        values = new int[cap];
        mask = cap - 1;
        threshold = M3DenseHash.threshold(cap, loadFactor);
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return keys.length; }
    public long payloadBytes() {
        return (long) keys.length * 8 + (long) values.length * 4;
    }
    public boolean containsKey(long key) {
        if (key == 0L) return hasZero;
        return find(key) >= 0;
    }
    public int get(long key) {
        if (key == 0L) return hasZero ? zeroValue : 0;
        int p = find(key);
        return p < 0 ? 0 : values[p];
    }
    public int getOrDefault(long key, int defaultValue) {
        if (key == 0L) return hasZero ? zeroValue : defaultValue;
        int p = find(key);
        return p < 0 ? defaultValue : values[p];
    }
    public int put(long key, int value) {
        if (key == 0L) {
            int old = zeroValue;
            if (!hasZero) { hasZero = true; size++; }
            zeroValue = value;
            if (size > threshold) rehash(keys.length << 1);
            return old;
        }
        int pos = M3DenseHash.mix(key) & mask;
        long cur;
        while ((cur = keys[pos]) != 0L) {
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
    public int remove(long key) {
        if (key == 0L) {
            if (!hasZero) return 0;
            hasZero = false;
            size--;
            int old = zeroValue;
            zeroValue = 0;
            return old;
        }
        int pos = M3DenseHash.mix(key) & mask;
        long cur;
        while ((cur = keys[pos]) != 0L) {
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
    /** One probe for nonzero keys; preserve zero-key insertion via put. */
    public int addTo(long key, int delta) {
        if (key == 0L) {
            int next = (hasZero ? zeroValue : 0) + delta;
            put(0L, next);
            return next;
        }
        int pos = M3DenseHash.mix(key) & mask;
        long current;
        while ((current = keys[pos]) != 0L) {
            if (current == key) return values[pos] += delta;
            pos = (pos + 1) & mask;
        }
        insertAbsent(pos, key, delta);
        return delta;
    }
    public void clear() {
        Arrays.fill(keys, 0L);
        Arrays.fill(values, 0);
        size = 0;
        hasZero = false;
        zeroValue = 0;
    }
    public void forEach(Consumer action) {
        if (hasZero) action.accept(0L, zeroValue);
        int remaining = size - (hasZero ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            long key = keys[i];
            if (key != 0L) {
                action.accept(key, values[i]);
                remaining--;
            }
        }
    }
    /** The caller has already identified an empty slot; preserve put's growth sequence. */
    private void insertAbsent(int pos, long key, int value) {
        keys[pos] = key;
        values[pos] = value;
        if (++size > threshold) rehash(keys.length << 1);
    }

    private int find(long key) {
        int pos = M3DenseHash.mix(key) & mask;
        long cur;
        while ((cur = keys[pos]) != 0L) {
            if (cur == key) return pos;
            pos = (pos + 1) & mask;
        }
        return -1;
    }
    private void shiftKeys(int pos) {
        for (;;) {
            int last = pos;
            pos = (pos + 1) & mask;
            long cur;
            while ((cur = keys[pos]) != 0L) {
                int slot = M3DenseHash.mix(cur) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            if (cur == 0L) {
                keys[last] = 0L;
                values[last] = 0;
                return;
            }
            keys[last] = cur;
            values[last] = values[pos];
        }
    }
    private void rehash(int newCap) {
        long[] oldK = keys;
        int[] oldV = values;
        keys = new long[newCap];
        values = new int[newCap];
        mask = newCap - 1;
        threshold = M3DenseHash.threshold(newCap, loadFactor);
        for (int i = 0; i < oldK.length; i++) {
            long key = oldK[i];
            if (key != 0L) {
                int p = M3DenseHash.mix(key) & mask;
                while (keys[p] != 0L) p = (p + 1) & mask;
                keys[p] = key;
                values[p] = oldV[i];
            }
        }
    }
}
