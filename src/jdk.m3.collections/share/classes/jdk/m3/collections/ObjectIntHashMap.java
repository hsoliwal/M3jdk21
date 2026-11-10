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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Open-addressing Object->int map with parallel Object[]/int[] and no entry/node objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ObjectIntHashMap.java}
 * blob {@code 0bc883ba30af54be177cdf2572b141234179ffce} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class ObjectIntHashMap<K> {
    @FunctionalInterface public interface Consumer<K> { void accept(K key, int value); }
    private Object[] keys;
    private int[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasNull;
    private int nullValue;
    private final float loadFactor;

    public ObjectIntHashMap() { this(16, DenseHash.DEFAULT_LOAD_FACTOR); }
    public ObjectIntHashMap(int expected) { this(expected, DenseHash.DEFAULT_LOAD_FACTOR); }
    public ObjectIntHashMap(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int cap = DenseHash.capacityFor(expected, loadFactor);
        keys = new Object[cap];
        values = new int[cap];
        mask = cap - 1;
        threshold = DenseHash.threshold(cap, loadFactor);
    }
    public int size() { return size; }
    public long primitivePayloadBytes() { return (long) values.length * Integer.BYTES; }
    public int referenceSlots() { return keys.length; }
    public boolean containsKey(K key) {
        return key == null ? hasNull : find(key) >= 0;
    }
    public int get(K key) {
        if (key == null) return hasNull ? nullValue : 0;
        int p = find(key);
        return p < 0 ? 0 : values[p];
    }
    public int getOrDefault(K key, int defaultValue) {
        if (key == null) return hasNull ? nullValue : defaultValue;
        int p = find(key);
        return p < 0 ? defaultValue : values[p];
    }
    public int put(K key, int value) {
        if (key == null) {
            int old = nullValue;
            if (!hasNull) {
                // A failed growth must not publish null membership or its value.
                if (size + 1 > threshold) rehash(growthCapacity());
                hasNull = true;
                size++;
            }
            nullValue = value;
            return old;
        }
        int p = DenseHash.mix(key.hashCode()) & mask;
        Object cur;
        while ((cur = keys[p]) != null) {
            if (cur.equals(key)) {
                int old = values[p];
                values[p] = value;
                return old;
            }
            p = (p + 1) & mask;
        }
        if (size + 1 > threshold) {
            // Stage the pending entry too. A second user hashCode() failure
            // cannot leave the visible table resized or partially migrated.
            rehash(growthCapacity(), key, value);
            return 0;
        }
        keys[p] = key;
        values[p] = value;
        size++;
        return 0;
    }
    public int remove(K key) {
        if (key == null) {
            if (!hasNull) return 0;
            hasNull = false;
            size--;
            int old = nullValue;
            nullValue = 0;
            return old;
        }
        int p = DenseHash.mix(key.hashCode()) & mask;
        Object cur;
        while ((cur = keys[p]) != null) {
            if (cur.equals(key)) {
                int old = values[p];
                shiftKeys(p);
                size--;
                return old;
            }
            p = (p + 1) & mask;
        }
        return 0;
    }
    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, 0);
        size = 0;
        hasNull = false;
        nullValue = 0;
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super K> action) {
        if (hasNull) action.accept(null, nullValue);
        int remaining = size - (hasNull ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                action.accept((K) key, values[i]);
                remaining--;
            }
        }
    }
    private int find(K key) {
        int p = DenseHash.mix(key.hashCode()) & mask;
        Object cur;
        while ((cur = keys[p]) != null) {
            if (cur.equals(key)) return p;
            p = (p + 1) & mask;
        }
        return -1;
    }
    private void shiftKeys(int pos) {
        for (;;) {
            int last = pos;
            pos = (pos + 1) & mask;
            Object cur;
            while ((cur = keys[pos]) != null) {
                int slot = DenseHash.mix(cur.hashCode()) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            if (cur == null) {
                keys[last] = null;
                values[last] = 0;
                return;
            }
            keys[last] = cur;
            values[last] = values[pos];
        }
    }
    private void rehash(int cap) {
        rehash(cap, null, 0);
    }

    /** Build both array lanes privately; publish only after every Java callback succeeds. */
    private void rehash(int cap, Object pendingKey, int pendingValue) {
        Object[] nextKeys = new Object[cap];
        int[] nextValues = new int[cap];
        int nextMask = cap - 1;
        int nextThreshold = DenseHash.threshold(cap, loadFactor);
        for (int i = 0; i < keys.length; i++) {
            Object key = keys[i];
            if (key != null) {
                int p = DenseHash.mix(key.hashCode()) & nextMask;
                while (nextKeys[p] != null) p = (p + 1) & nextMask;
                nextKeys[p] = key;
                nextValues[p] = values[i];
            }
        }
        if (pendingKey != null) {
            int p = DenseHash.mix(pendingKey.hashCode()) & nextMask;
            while (nextKeys[p] != null) p = (p + 1) & nextMask;
            nextKeys[p] = pendingKey;
            nextValues[p] = pendingValue;
        }
        keys = nextKeys;
        values = nextValues;
        mask = nextMask;
        threshold = nextThreshold;
        if (pendingKey != null) size++;
    }

    private int growthCapacity() {
        if (keys.length >= (1 << 30)) throw new IllegalStateException("collection too large");
        return keys.length << 1;
    }
}
