// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Shared zero-object mechanics for stateful linear open-address collections.
 *
 * <p>This class owns probe/state arithmetic only. Payload lanes, counters, publication and public
 * contracts remain with each collection owner.</p>
 */
//FLLDCIM: RELOCATE - donor blob f5be8e4c208f at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
final class PackedHashAtoms {
    static final byte EMPTY = 0;
    static final byte OCCUPIED = 1;
    static final byte DELETED = 2;

    private PackedHashAtoms() { }

    static int threshold(int capacity) {
        return Math.max(1, (int) (capacity * PackedSupport.DEFAULT_LOAD_FACTOR));
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
                : PackedSupport.nextTableCapacity(capacity);
    }

    static int findIndex(long[] keys, byte[] states, long key) {
        // LeetCode 705/706 open addressing: linear probe under the power-of-two mask; the key is compared before the
        // state, so an occupied non-matching slot costs one state load and one key compare; EMPTY ends the chain.
        int mask = states.length - 1;
        if (mask < 0) {
            return -1;
        }
        int index = PackedSupport.mix(key) & mask;
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
        int index = PackedSupport.mix(key) & mask;
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
        int index = PackedSupport.mix(key) & mask;
        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }
        return index;
    }

    /** True when deleted slots outnumber live slots and same-capacity compaction is worthwhile. */
    static boolean compactTombstones(int size, int used) {
        return size > 0 && used - size > size;
    }

    // Lane-ordinal slot index over a caller-owned long[] lane (the PackedLongLaneHashIndex
    // pattern): a slot holds lane index + 1, 0 is empty, the length is a power of two and the
    // caller keeps load at most 1/2, so every probe chain ends at an empty slot.

    /** Smallest power-of-two slot count that holds {@code count} ordinals at load at most 1/2. */
    static int laneIndexLength(int count) {
        return Integer.highestOneBit((count << 1) - 1) << 1;
    }

    /** Lane index of {@code key}, or -1 when the probe chain reaches an empty slot. */
    static int findLaneIndex(int[] slots, long[] lane, long key) {
        int mask = slots.length - 1;
        for (int slot = PackedSupport.mix(key) & mask; ; slot = (slot + 1) & mask) {
            int entry = slots[slot];
            if (entry == 0 || lane[entry - 1] == key) {
                return entry - 1;
            }
        }
    }

    /** Places lane ordinal {@code index} in the first empty slot of its probe chain. */
    static void placeLaneIndex(int[] slots, long[] lane, int index) {
        int mask = slots.length - 1;
        int slot = PackedSupport.mix(lane[index]) & mask;
        while (slots[slot] != 0) {
            slot = (slot + 1) & mask;
        }
        slots[slot] = index + 1;
    }

    /** Clears or replaces {@code slots}, places lane[0, size) and returns the live table. */
    static int[] rebuildLaneIndex(int[] slots, long[] lane, int size, int length) {
        int[] target = length > slots.length ? new int[length] : slots;
        if (target == slots) {
            Arrays.fill(target, 0);
        }
        for (int index = 0; index < size; index++) {
            placeLaneIndex(target, lane, index);
        }
        return target;
    }

    /** Backward-shift deletion of ordinal {@code index}; lane still holds pre-removal values. */
    static void deleteLaneIndex(int[] slots, long[] lane, int index) {
        int mask = slots.length - 1;
        int gap = PackedSupport.mix(lane[index]) & mask;
        while (slots[gap] != index + 1) {
            gap = (gap + 1) & mask;
        }
        for (int scan = (gap + 1) & mask; slots[scan] != 0; scan = (scan + 1) & mask) {
            int home = PackedSupport.mix(lane[slots[scan] - 1]) & mask;
            if (((scan - home) & mask) >= ((scan - gap) & mask)) {
                slots[gap] = slots[scan];
                gap = scan;
            }
        }
        slots[gap] = 0;
    }

    /** Rebases ordinals after lane index {@code index} was removed and the suffix moved left. */
    static void shiftLaneIndexes(int[] slots, int index) {
        int removed = index + 1;
        for (int slot = 0; slot < slots.length; slot++) {
            slots[slot] -= (removed - slots[slot]) >>> 31;
        }
    }
}
