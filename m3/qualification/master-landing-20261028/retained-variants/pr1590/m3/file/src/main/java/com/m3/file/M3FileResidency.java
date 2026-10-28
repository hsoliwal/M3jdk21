// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Bounded, direct-mapped table of live {@link M3File} facades, owned by one {@link M3FileTree}.
 *
 * <p>Pattern adapted from {@code org.m3.runtime.lazy.LazyObjectParent}: facades are transient and
 * parent-owned. Identity is {@code (tree, id)}, so eviction is dropping the facade and recreation is
 * free. Slot = {@code id & mask}; a request whose id maps to an occupied slot overwrites the occupant.
 * {@link M3FileFlag#RESIDENT} mirrors table membership exactly.</p>
 */
final class M3FileResidency {
    private final M3File[] slots;
    private final int mask;

    M3FileResidency(int capacity) {
        if (capacity < 1 || Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two: " + capacity);
        }
        this.slots = new M3File[capacity];
        this.mask = capacity - 1;
    }

    int capacity() {
        return slots.length;
    }

    /** Returns the resident facade for {@code id}, creating it (and evicting the slot's occupant) on a miss. */
    synchronized M3File file(M3FileTree tree, int id) {
        int slot = id & mask;
        M3File current = slots[slot];
        if (current != null && current.id() == id) return current;
        if (current != null) tree.setFlag(M3FileFlag.RESIDENT, current.id(), false);
        M3File created = new M3File(tree, id);
        slots[slot] = created;
        tree.setFlag(M3FileFlag.RESIDENT, id, true);
        return created;
    }

    synchronized int residentCount() {
        int count = 0;
        for (M3File facade : slots) {
            if (facade != null) count++;
        }
        return count;
    }
}
