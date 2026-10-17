/*
 * Copyright 2026 Hitesh Soliwal and contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Derives the slot/bit-mask primitive mechanisms from existing M3ObjectStateSlots28.
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Batch bit-mask operations on existing generation-safe object-state handles.
 *
 * <p>This class owns no new payloads, entry nodes, indexes, mask buffers or
 * JNI symbols. The caller owns the {@code long[]} encounter order, which may
 * contain duplicate handles and arbitrary half-open subranges. The existing
 * {@link M3ObjectStateSlots28} remains the sole state/generation authority.
 * A slot's state describes application metadata, not a HotSpot object header,
 * pinned Java reference, GC root or native address.</p>
 *
 * <p>Writes are preflight-validated across the entire selected range before
 * the first mutation. Invalid masks, offsets and stale handles therefore
 * cannot leave a partially modified range. Allocation failures and concurrent
 * owner mutation are not transactional; the owner is single-threaded.
 * Duplicate handles are visited in encounter order, and a transition is counted
 * only when the state changes at that occurrence. Masks use all eight unsigned
 * bits. Array values themselves are neither moved nor modified.</p>
 */
public final class M3ObjectStateBatch28 {
    private M3ObjectStateBatch28() { }

    /**
     * Apply set/clear state-bit operations to every occurrence in a checked slice.
     * The result counts actual state transitions, not distinct handles.
     * @return the number of occurrences that changed a state byte
     */
    public static int update(M3ObjectStateSlots28 owner, long[] handles,
            int from, int length, int setMask, int clearMask) {
        requireMasks(setMask, clearMask, true);
        check(Objects.requireNonNull(owner, "owner"), handles, from, length);
        preflight(owner, handles, from, length);
        int nextMask = ~clearMask & 0xff;
        int changed = 0;
        for (int i = from, end = from + length; i < end; i++) {
            long handle = handles[i];
            int old = owner.update(handle, setMask, clearMask);
            if (old != ((old | setMask) & nextMask)) changed++;
        }
        return changed;
    }

    /**
     * Apply set/clear masks only to entries satisfying required/excluded flags.
     * An accepted occurrence is tested against its state *at that occurrence*;
     * duplicates can consequently see the effects of earlier updates.
     * An overlapping required/excluded mask matches nothing.
     * @return number of occurrences whose state actually changed
     */
    public static int updateMatching(M3ObjectStateSlots28 owner, long[] handles,
            int from, int length, int requiredMask, int excludedMask,
            int setMask, int clearMask) {
        requireMasks(requiredMask, excludedMask, false);
        requireMasks(setMask, clearMask, true);
        check(Objects.requireNonNull(owner, "owner"), handles, from, length);
        preflight(owner, handles, from, length);
        if ((requiredMask & excludedMask) != 0) return 0;
        int changed = 0;
        int complement = ~clearMask & 0xff;
        for (int i = from, end = from + length; i < end; i++) {
            long handle = handles[i];
            int state = owner.state(handle);
            if (matches(state, requiredMask, excludedMask)) {
                owner.update(handle, setMask, clearMask);
                if (state != ((state | setMask) & complement)) changed++;
            }
        }
        return changed;
    }

    /** Counts matching *occurrences*, including duplicates, without allocating. */
    public static int countMatching(M3ObjectStateSlots28 owner, long[] handles,
            int from, int length, int requiredMask, int excludedMask) {
        requireMasks(requiredMask, excludedMask, false);
        check(Objects.requireNonNull(owner, "owner"), handles, from, length);
        int count = 0;
        for (int i = from, end = from + length; i < end; i++) {
            int state = owner.state(handles[i]);
            if (matches(state, requiredMask, excludedMask)) count++;
        }
        return count;
    }

    /**
     * Return the first matching <em>array index</em>, not the physical slot.
     * An invalid later handle is not visited after an early match: this read
     * search retains short-circuit semantics, unlike preflighted writes.
     */
    public static int firstMatchingIndex(M3ObjectStateSlots28 owner, long[] handles,
            int from, int length, int requiredMask, int excludedMask) {
        requireMasks(requiredMask, excludedMask, false);
        check(Objects.requireNonNull(owner, "owner"), handles, from, length);
        for (int i = from, end = from + length; i < end; i++) {
            int state = owner.state(handles[i]);
            if (matches(state, requiredMask, excludedMask)) return i;
        }
        return -1;
    }

    private static void check(M3ObjectStateSlots28 owner, long[] handles,
            int from, int length) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(handles, "handles");
        Objects.checkFromIndexSize(from, length, handles.length);
    }

    private static void preflight(M3ObjectStateSlots28 owner, long[] handles,
            int from, int length) {
        for (int i = from, end = from + length; i < end; i++) {
            owner.state(handles[i]);
        }
    }

    private static boolean matches(int flags, int required, int excluded) {
        return (flags & required) == required && (flags & excluded) == 0;
    }

    private static void requireMasks(int set, int clear, boolean disjoint) {
        if (((set | clear) & ~0xff) != 0) {
            throw new IllegalArgumentException("state masks must fit an unsigned byte");
        }
        if (disjoint && (set & clear) != 0) {
            throw new IllegalArgumentException("overlapping set/clear state masks");
        }
    }
}
