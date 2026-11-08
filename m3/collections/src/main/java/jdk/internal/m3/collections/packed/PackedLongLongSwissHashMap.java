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

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.LongLongPredicate;

/**
 * Primitive {@code long -> long} Swiss-table-style hash map.
 *
 * <p>Eight-slot groups are filtered through one byte of metadata per slot. Full
 * controls store seven fingerprint bits; negative controls encode empty/deleted.
 * Retained data is exactly primitive key/value lanes plus the control-byte lane.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLongSwissHashMap.java}
 * blob {@code 4896a9133715c6d53ccc99450e1320658ef775b4} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLongSwissHashMap implements PackedLongLongMap {
    private static final int GROUP_SIZE = PackedSwissProbeAtoms.GROUP_SIZE;
    private static final byte EMPTY = PackedSwissProbeAtoms.EMPTY;
    private static final byte DELETED = PackedSwissProbeAtoms.DELETED;
    public static final float DEFAULT_LOAD_FACTOR = 0.875f;

    private long[] keys;
    private long[] values;
    private byte[] controls;
    private int size;
    private int used;
    private final float loadFactor;
    private int threshold;

    public PackedLongLongSwissHashMap() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongLongSwissHashMap(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongLongSwissHashMap(int expectedSize, float loadFactor) {
        this.loadFactor = PackedSwissProbeAtoms.validateLoadFactor(loadFactor, DEFAULT_LOAD_FACTOR);
        int capacity = Math.max(GROUP_SIZE, PackedSupport.tableSizeForExpected(expectedSize, loadFactor));
        keys = new long[capacity];
        values = new long[capacity];
        controls = new byte[capacity];
        Arrays.fill(controls, EMPTY);
        threshold = PackedSwissProbeAtoms.threshold(capacity, loadFactor);
    }

    @Override
    public boolean put(long key, long value) {
        if (used + 1 > threshold) {
            int existing = findIndex(key);
            if (existing >= 0) {
                values[existing] = value;
                return false;
            }
            int capacity = size + 1 <= threshold
                    ? keys.length
                    : PackedSupport.nextTableCapacity(keys.length);
            rehash(capacity);
        }

        int slot = PackedSwissProbeAtoms.findOrInsertionIndex(keys, controls, key);
        if (slot >= 0) {
            values[slot] = value;
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = controls[target] == DELETED;
        keys[target] = key;
        values[target] = value;
        controls[target] = PackedSwissProbeAtoms.fingerprint(PackedSupport.mix(key));
        size++;
        if (!reusedDeleted) {
            used++;
        }
        return true;
    }

    @Override
    public boolean containsKey(long key) {
        return findIndex(key) >= 0;
    }

    @Override
    public long getOrDefault(long key, long defaultValue) {
        int index = findIndex(key);
        return index < 0 ? defaultValue : values[index];
    }

    @Override
    public long getOrThrow(long key) {
        int index = findIndex(key);
        if (index < 0) {
            throw new IllegalArgumentException("missing key: " + key);
        }
        return values[index];
    }

    public long addTo(long key, long delta, long initialValue) {
        int index = findIndex(key);
        if (index >= 0) {
            values[index] += delta;
            return values[index];
        }
        long value = initialValue + delta;
        put(key, value);
        return value;
    }

    @Override
    public boolean remove(long key) {
        int index = findIndex(key);
        if (index < 0) {
            return false;
        }
        controls[index] = DELETED;
        values[index] = 0L;
        size--;
        compactAfterRemoval();
        return true;
    }

    @Override
    public int filterInPlace(LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < controls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(controls[i]) && !predicate.test(keys[i], values[i])) {
                controls[i] = DELETED;
                values[i] = 0L;
                size--;
            }
        }
        if (size == 0) {
            Arrays.fill(controls, EMPTY);
            used = 0;
        } else if (size != before) {
            rehash(PackedSupport.tableSizeForExpected(size, loadFactor));
        }
        return before - size;
    }

    @Override
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < controls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(controls[i])) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
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
        Arrays.fill(controls, EMPTY);
        size = 0;
        used = 0;
    }

    @Override
    public void trimToSize() {
        int target = Math.max(GROUP_SIZE, PackedSupport.tableSizeForExpected(size, loadFactor));
        if (target != keys.length || used != size) {
            rehash(target);
        }
    }

    @Override
    public long[] keysToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < controls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(controls[i])) {
                result[write++] = keys[i];
            }
        }
        return result;
    }

    @Override
    public long[] valuesToArray() {
        long[] result = new long[size];
        int write = 0;
        for (int i = 0; i < controls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(controls[i])) {
                result[write++] = values[i];
            }
        }
        return result;
    }

    private int findIndex(long key) {
        return PackedSwissProbeAtoms.findIndex(keys, controls, key);
    }

    private void compactAfterRemoval() {
        if (size == 0) {
            Arrays.fill(controls, EMPTY);
            used = 0;
        } else if (used - size > Math.max(GROUP_SIZE, size >>> 1)) {
            rehash(keys.length);
        }
    }

    private void rehash(int requestedCapacity) {
        int capacity = Math.max(GROUP_SIZE, requestedCapacity);
        long[] oldKeys = keys;
        long[] oldValues = values;
        byte[] oldControls = controls;

        keys = new long[capacity];
        values = new long[capacity];
        controls = new byte[capacity];
        Arrays.fill(controls, EMPTY);
        threshold = PackedSwissProbeAtoms.threshold(capacity, loadFactor);
        int oldSize = size;
        size = 0;
        used = 0;

        for (int i = 0; i < oldControls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(oldControls[i])) {
                putRehashed(oldKeys[i], oldValues[i]);
            }
        }
        if (size != oldSize) {
            throw new IllegalStateException("rehash size mismatch");
        }
    }

    private void putRehashed(long key, long value) {
        int slot = PackedSwissProbeAtoms.rehashInsertionIndex(keys, controls, key);
        keys[slot] = key;
        values[slot] = value;
        controls[slot] = PackedSwissProbeAtoms.fingerprint(PackedSupport.mix(key));
        size++;
        used++;
    }

}
