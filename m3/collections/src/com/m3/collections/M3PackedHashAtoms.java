/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedHashAtoms
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

/** Probe/state arithmetic for stateful linear open-address M3 collections. */
final class M3PackedHashAtoms {
    static final byte EMPTY = 0;
    static final byte OCCUPIED = 1;
    static final byte DELETED = 2;

    private M3PackedHashAtoms() { }

    static int threshold(int capacity) {
        return Math.max(1, (int) (capacity * M3PackedSupport.DEFAULT_LOAD_FACTOR));
    }

    static boolean pressure(int used, int capacity) {
        return used + 1 > threshold(capacity);
    }

    static int capacityAfterPressure(int size, int capacity) {
        return size + 1 <= threshold(capacity)
                ? capacity
                : M3PackedSupport.nextTableCapacity(capacity);
    }

    static int findIndex(long[] keys, byte[] states, long key) {
        if (states.length == 0) {
            return -1;
        }
        int mask = states.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
        while (true) {
            byte state = states[index];
            if (state == EMPTY) {
                return -1;
            }
            if (state == OCCUPIED && keys[index] == key) {
                return index;
            }
            index = (index + 1) & mask;
        }
    }

    static int findOrInsertionIndex(long[] keys, byte[] states, long key) {
        int mask = states.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
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
            index = (index + 1) & mask;
        }
    }

    static int rehashInsertionIndex(long[] keys, byte[] states, long key) {
        int mask = states.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }
        return index;
    }

    static boolean compactTombstones(int size, int used) {
        return size > 0 && used - size > size;
    }
}
