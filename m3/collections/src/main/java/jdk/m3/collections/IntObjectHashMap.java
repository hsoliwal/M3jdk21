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
import java.util.Objects;

/**
 * Open-addressing int->Object map. One primitive key array + one Object[]; no entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntObjectHashMap.java}
 * blob {@code dacaf5475012eb2d8878d58d904bed3df8287f02} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntObjectHashMap<V> {
    @FunctionalInterface public interface Consumer<V> { void accept(int key, V value); }
    private int[] keys;
    private Object[] values;
    private int mask;
    private int threshold;
    private int size;
    private boolean hasZero;
    private V zeroValue;
    private final float loadFactor;

    public IntObjectHashMap() { this(16, M3DenseHash.DEFAULT_LOAD_FACTOR); }
    public IntObjectHashMap(int expected) { this(expected, M3DenseHash.DEFAULT_LOAD_FACTOR); }
    public IntObjectHashMap(int expected, float loadFactor) {
        this.loadFactor = loadFactor;
        int cap = M3DenseHash.capacityFor(expected, loadFactor);
        keys = new int[cap];
        values = new Object[cap];
        mask = cap - 1;
        threshold = M3DenseHash.threshold(cap, loadFactor);
    }
    public int size() { return size; }
    public long primitivePayloadBytes() { return (long) keys.length * Integer.BYTES; }
    public int referenceSlots() { return values.length; }
    public boolean containsKey(int key) {
        return key == 0 ? hasZero : find(key) >= 0;
    }
    @SuppressWarnings("unchecked")
    public V get(int key) {
        if (key == 0) return hasZero ? zeroValue : null;
        int p = find(key);
        return p < 0 ? null : (V) values[p];
    }
    @SuppressWarnings("unchecked")
    public V put(int key, V value) {
        Objects.requireNonNull(value, "value");
        if (key == 0) {
            V old = zeroValue;
            if (!hasZero) {
                // Allocate and migrate first. A failed growth must not publish zero membership.
                if (size + 1 > threshold) rehash(growthCapacity());
                hasZero = true;
                size++;
            }
            zeroValue = value;
            return old;
        }
        int p = M3DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[p]) != 0) {
            if (cur == key) {
                V old = (V) values[p];
                values[p] = value;
                return old;
            }
            p = (p + 1) & mask;
        }
        if (size + 1 > threshold) {
            // No user hash callbacks for primitive keys, but arrays must be committed
            // together before inserting the new pair.
            rehash(growthCapacity());
            p = M3DenseHash.mix(key) & mask;
            while (keys[p] != 0) p = (p + 1) & mask;
        }
        keys[p] = key;
        values[p] = value;
        size++;
        return null;
    }
    @SuppressWarnings("unchecked")
    public V remove(int key) {
        if (key == 0) {
            if (!hasZero) return null;
            hasZero = false;
            size--;
            V old = zeroValue;
            zeroValue = null;
            return old;
        }
        int p = M3DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[p]) != 0) {
            if (cur == key) {
                V old = (V) values[p];
                shiftKeys(p);
                size--;
                return old;
            }
            p = (p + 1) & mask;
        }
        return null;
    }
    public void clear() {
        Arrays.fill(keys, 0);
        Arrays.fill(values, null);
        size = 0;
        hasZero = false;
        zeroValue = null;
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> action) {
        if (hasZero) action.accept(0, zeroValue);
        int remaining = size - (hasZero ? 1 : 0);
        for (int i = 0; remaining > 0 && i < keys.length; i++) {
            int key = keys[i];
            if (key != 0) {
                action.accept(key, (V) values[i]);
                remaining--;
            }
        }
    }
    private int find(int key) {
        int p = M3DenseHash.mix(key) & mask;
        int cur;
        while ((cur = keys[p]) != 0) {
            if (cur == key) return p;
            p = (p + 1) & mask;
        }
        return -1;
    }
    private void shiftKeys(int pos) {
        for (;;) {
            int last = pos;
            pos = (pos + 1) & mask;
            int cur;
            while ((cur = keys[pos]) != 0) {
                int slot = M3DenseHash.mix(cur) & mask;
                if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
                pos = (pos + 1) & mask;
            }
            if (cur == 0) {
                keys[last] = 0;
                values[last] = null;
                return;
            }
            keys[last] = cur;
            values[last] = values[pos];
        }
    }
    private void rehash(int cap) {
        // One private staging transaction, no per-key objects or JNI calls.
        // All allocations and migration finish before any live field changes.
        int[] nextKeys = new int[cap];
        Object[] nextValues = new Object[cap];
        int nextMask = cap - 1;
        int nextThreshold = M3DenseHash.threshold(cap, loadFactor);
        for (int i = 0; i < keys.length; i++) {
            int key = keys[i];
            if (key != 0) {
                int p = M3DenseHash.mix(key) & nextMask;
                while (nextKeys[p] != 0) p = (p + 1) & nextMask;
                nextKeys[p] = key;
                nextValues[p] = values[i];
            }
        }
        keys = nextKeys;
        values = nextValues;
        mask = nextMask;
        threshold = nextThreshold;
    }

    private int growthCapacity() {
        if (keys.length >= (1 << 30)) throw new IllegalStateException("collection too large");
        return keys.length << 1;
    }
}
