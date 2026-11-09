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
 * Primitive long Swiss-table-style hash set.
 *
 * <p>Eight slots form a probe group. Each slot has one control byte: EMPTY, DELETED,
 * or a 7-bit hash fingerprint. Group probing is triangular; a fingerprint match is
 * checked against the full primitive key. This retains exactly {@code long[] + byte[]}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongSwissHashSet.java}
 * blob {@code 3ae58669ad6cb75cc763287d41b4aa37021163cf} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongSwissHashSet implements PackedMutableLongSet {
    private static final int GROUP_SIZE = PackedSwissProbeAtoms.GROUP_SIZE;
    private static final byte EMPTY = PackedSwissProbeAtoms.EMPTY;
    private static final byte DELETED = PackedSwissProbeAtoms.DELETED;
    public static final float DEFAULT_LOAD_FACTOR = 0.875f;

    private long[] keys;
    private byte[] controls;
    private int size;
    private int used;
    private final float loadFactor;
    private int threshold;

    public PackedLongSwissHashSet() {
        this(8, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongSwissHashSet(int expectedSize) {
        this(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    public PackedLongSwissHashSet(int expectedSize, float loadFactor) {
        this.loadFactor = PackedSwissProbeAtoms.validateLoadFactor(loadFactor, DEFAULT_LOAD_FACTOR);
        int capacity = Math.max(GROUP_SIZE, PackedSupport.tableSizeForExpected(expectedSize, loadFactor));
        keys = new long[capacity];
        controls = new byte[capacity];
        Arrays.fill(controls, EMPTY);
        threshold = PackedSwissProbeAtoms.threshold(capacity, loadFactor);
    }

    @Override
    public boolean add(long value) {
        if (used + 1 > threshold) {
            if (contains(value)) {
                return false;
            }
            int capacity = size + 1 <= threshold
                    ? keys.length
                    : PackedSupport.nextTableCapacity(keys.length);
            rehash(capacity);
        }

        int slot = PackedSwissProbeAtoms.findOrInsertionIndex(keys, controls, value);
        if (slot >= 0) {
            return false;
        }
        int target = ~slot;
        boolean reusedDeleted = controls[target] == DELETED;
        keys[target] = value;
        controls[target] = PackedSwissProbeAtoms.fingerprint(PackedSupport.mix(value));
        size++;
        if (!reusedDeleted) {
            used++;
        }
        return true;
    }

    public boolean addIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && add(value);
    }

    @Override
    public boolean contains(long value) {
        return findIndex(value) >= 0;
    }

    @Override
    public boolean remove(long value) {
        int index = findIndex(value);
        if (index < 0) {
            return false;
        }
        controls[index] = DELETED;
        size--;
        compactAfterRemoval();
        return true;
    }

    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int before = size;
        for (int i = 0; i < controls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(controls[i]) && !predicate.test(keys[i])) {
                controls[i] = DELETED;
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
    public long[] toArray() {
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
    public LongIterator iterator() {
        return new LongIterator() {
            private int index;
            private int emitted;

            @Override
            public boolean hasNext() {
                return emitted < size;
            }

            @Override
            public long nextLong() {
                while (index < controls.length) {
                    int current = index++;
                    if (PackedSwissProbeAtoms.isFull(controls[current])) {
                        emitted++;
                        return keys[current];
                    }
                }
                throw new NoSuchElementException();
            }
        };
    }

    private int findIndex(long value) {
        return PackedSwissProbeAtoms.findIndex(keys, controls, value);
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
        byte[] oldControls = controls;

        keys = new long[capacity];
        controls = new byte[capacity];
        Arrays.fill(controls, EMPTY);
        threshold = PackedSwissProbeAtoms.threshold(capacity, loadFactor);
        int oldSize = size;
        size = 0;
        used = 0;

        for (int i = 0; i < oldControls.length; i++) {
            if (PackedSwissProbeAtoms.isFull(oldControls[i])) {
                addRehashed(oldKeys[i]);
            }
        }
        if (size != oldSize) {
            throw new IllegalStateException("rehash size mismatch");
        }
    }

    private void addRehashed(long value) {
        int slot = PackedSwissProbeAtoms.rehashInsertionIndex(keys, controls, value);
        keys[slot] = value;
        controls[slot] = PackedSwissProbeAtoms.fingerprint(PackedSupport.mix(value));
        size++;
        used++;
    }

}
