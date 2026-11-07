// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/**
 * Shared zero-object mechanics for stateful linear open-address collections.
 *
 * <p>This class owns probe/state arithmetic only. Payload lanes, counters, publication and public
 * contracts remain with each collection owner.</p>
 */
//FLLDCIM: RELOCATE - donor blob f5be8e4c208f at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
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

    /**
     * Capacity required when an absent insertion is attempted under used-slot pressure.
     * Same-capacity return means tombstone compaction is sufficient.
     */
    static int capacityAfterPressure(int size, int capacity) {
        return size + 1 <= threshold(capacity)
                ? capacity
                : M3PackedSupport.nextTableCapacity(capacity);
    }

    static int findIndex(long[] keys, byte[] states, long key) {
        // LeetCode 705/706 open addressing: linear probe under the power-of-two mask; the key is compared before the
        // state, so an occupied non-matching slot costs one state load and one key compare; EMPTY ends the chain.
        int mask = states.length - 1;
        if (mask < 0) {
            return -1;
        }
        int index = M3PackedSupport.mix(key) & mask;
        for (byte state; (state = states[index]) != EMPTY; index = (index + 1) & mask) {
            if (keys[index] == key && state == OCCUPIED) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Probe once for either an existing key or the best insertion slot.
     *
     * @return existing slot when non-negative; otherwise insertion slot is {@code ~result}
     */
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

    /** Find an empty slot in a freshly rebuilt table where the key is already known unique. */
    static int rehashInsertionIndex(long[] keys, byte[] states, long key) {
        int mask = states.length - 1;
        int index = M3PackedSupport.mix(key) & mask;
        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }
        return index;
    }

    /** True when deleted slots outnumber live slots and same-capacity compaction is worthwhile. */
    static boolean compactTombstones(int size, int used) {
        return size > 0 && used - size > size;
    }
}
