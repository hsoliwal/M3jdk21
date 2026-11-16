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
package jdk.internal.m3.collections;

/**
 * Open-addressed index from a key hash to a slot of parallel array lanes: the atoms that hash-indexed owners share.
 *
 * <p>The index lane holds slot + 1, and 0 is empty. Its owner keeps it at most half full, so every probe run ends at an
 * empty position. The key hash of each slot is kept in a hash lane, so no key is hashed twice and most unequal keys
 * are rejected by an int comparison. All methods are pure functions of the lanes they are given.</p>
 */
final class SlotIndex {
    private static final int GOLDEN_RATIO_32 = 0x9E3779B9;
    private static final int FOLD_SHIFT = 16;
    private static final int MIN_LENGTH = 8;
    private static final int ABSENT = -1;

    private SlotIndex() {
    }

    /** Returns the index lane length for {@code slots} slots: a power of two, at least twice the slots. */
    static int length(int slots) {
        return Integer.highestOneBit(Math.max(2 * slots, MIN_LENGTH) - 1) << 1;
    }

    /** Returns the hash of a key as {@code java.util.Map} defines it; 0 for null. */
    static int hash(Object key) {
        return key == null ? 0 : key.hashCode();
    }

    /** Spreads a hash over the index lane: one multiplication, then the high half folded onto the low half. */
    static int mix(int hash) {
        int spread = hash * GOLDEN_RATIO_32;
        return spread ^ spread >>> FOLD_SHIFT;
    }

    /** Returns the slot of {@code key}, or -1; the lookup key is the receiver of {@code equals}. */
    static int find(int[] index, int[] hashLane, Object[] keyLane, Object key, int hash) {
        int mask = index.length - 1;
        for (int at = mix(hash) & mask; index[at] != 0; at = at + 1 & mask) {
            int slot = index[at] - 1;
            if (hashLane[slot] == hash && (key == keyLane[slot] || key != null && key.equals(keyLane[slot]))) {
                return slot;
            }
        }
        return ABSENT;
    }

    /** Stores {@code slot} at the first empty position of the probe run of its hash. */
    static void place(int[] index, int[] hashLane, int slot) {
        int mask = index.length - 1;
        int at = mix(hashLane[slot]) & mask;
        while (index[at] != 0) {
            at = at + 1 & mask;
        }
        index[at] = slot + 1;
    }

    /** Returns an index lane for {@code slots} slots that holds the slots {@code 0 .. used - 1}. */
    static int[] rebuilt(int[] hashLane, int used, int slots) {
        int[] index = new int[length(slots)];
        for (int slot = 0; slot < used; slot++) {
            place(index, hashLane, slot);
        }
        return index;
    }
}
