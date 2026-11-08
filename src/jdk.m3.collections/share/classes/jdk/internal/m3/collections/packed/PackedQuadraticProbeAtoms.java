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

/**
 * Shared zero-object triangular/quadratic probe geometry for primitive hash owners.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedQuadraticProbeAtoms.java}
 * blob {@code 318b158ebf622e0b42cb3ac148b25c0f3b30db0e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedQuadraticProbeAtoms {
    static final byte EMPTY = PackedHashAtoms.EMPTY;
    static final byte OCCUPIED = PackedHashAtoms.OCCUPIED;
    static final byte DELETED = PackedHashAtoms.DELETED;

    private PackedQuadraticProbeAtoms() { }

    static float validateLoadFactor(float loadFactor) {
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor");
        }
        return loadFactor;
    }

    static int threshold(int capacity, float loadFactor) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * loadFactor)));
    }

    static int findIndex(long[] keys, byte[] states, long key) {
        int mask = keys.length - 1;
        int index = PackedSupport.mix(key) & mask;
        int step = 0;
        while (true) {
            byte state = states[index];
            if (state == EMPTY) {
                return -1;
            }
            if (state == OCCUPIED && keys[index] == key) {
                return index;
            }
            index = (index + ++step) & mask;
        }
    }

    /**
     * Probe once for an existing key or insertion slot.
     *
     * @return existing slot when non-negative; otherwise insertion slot is {@code ~result}
     */
    static int findOrInsertionIndex(long[] keys, byte[] states, long key) {
        int mask = keys.length - 1;
        int index = PackedSupport.mix(key) & mask;
        int step = 0;
        int firstDeleted = -1;
        while (true) {
            byte state = states[index];
            if (state == EMPTY) {
                return ~(firstDeleted >= 0 ? firstDeleted : index);
            }
            if (state == OCCUPIED && keys[index] == key) {
                return index;
            }
            if (state == DELETED && firstDeleted < 0) {
                firstDeleted = index;
            }
            index = (index + ++step) & mask;
        }
    }

    /** Finds an empty slot in a fresh table where the key is already known unique. */
    static int rehashInsertionIndex(long[] keys, byte[] states, long key) {
        int mask = keys.length - 1;
        int index = PackedSupport.mix(key) & mask;
        int step = 0;
        while (states[index] == OCCUPIED) {
            index = (index + ++step) & mask;
        }
        return index;
    }
}
