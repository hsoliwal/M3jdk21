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
package jdk.internal.m3.collections;

/**
 * Shared zero-object Robin-Hood lookup and probe-distance geometry.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedRobinHoodProbeAtoms.java}
 * blob {@code 7ec16b1a1df65da84f9956bd325aac9d827aafb7} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3PackedRobinHoodProbeAtoms {
    static final byte EMPTY = M3PackedHashAtoms.EMPTY;
    static final byte OCCUPIED = M3PackedHashAtoms.OCCUPIED;

    private M3PackedRobinHoodProbeAtoms() { }

    static float validateLoadFactor(float loadFactor) {
        if (!(loadFactor > 0.0f && loadFactor < 1.0f)) {
            throw new IllegalArgumentException("loadFactor");
        }
        return loadFactor;
    }

    static int threshold(int capacity, float loadFactor) {
        return Math.min(capacity - 1, Math.max(1, (int) (capacity * loadFactor)));
    }

    static int probeDistance(int index, long key, int mask) {
        return (index - (M3PackedSupport.mix(key) & mask)) & mask;
    }

    static int findIndex(long[] keys, byte[] states, long key) {
        int mask = keys.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
        int distance = 0;
        while (states[index] == OCCUPIED) {
            long resident = keys[index];
            if (resident == key) {
                return index;
            }
            int residentDistance = probeDistance(index, resident, mask);
            if (residentDistance < distance) {
                return -1;
            }
            index = (index + 1) & mask;
            distance++;
        }
        return -1;
    }
}
