/*
 * Copyright 2026 Hitesh Soliwal and contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Source: M3SlotAllocator28, M3Address28 and the existing segmented-lane pattern.
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Compact collection-owned object-state flags keyed by generation-checked 28-bit slots.
 *
 * <p>Each physical slot has up to eight application-defined state bits in a lazily
 * allocated byte lane. No Java object or Entry is retained per slot; actual payloads
 * belong to another array owner. {@link M3SlotAllocator28} owns occupancy and stable
 * handle generations; a released handle cannot read or mutate a reused slot's flags.
 *
 * <p>State bits describe M3 application policy, not HotSpot object-header bits.
 * In particular, PIN_REQUEST does not pin an object in the JVM, disable GC,
 * manage native references, or alter Java reference reachability. Visibility is
 * likewise metadata unless the containing collection enforces it.
 *
 * <p>Single-threaded, no snapshot isolation. Calls must not overlap allocator
 * mutation. All bounds, mask and input-handle checks precede compaction writes;
 * an out-of-memory failure during allocation is not a multi-array transaction.
 */
public final class M3ObjectStateSlots28 {
    /** Retains the original implicit public no-argument constructor contract. */
    public M3ObjectStateSlots28() { }

    public static final int VISIBLE = 0x01;
    public static final int DIRTY = 0x02;
    public static final int DEFERRED = 0x04;
    public static final int INDEXED = 0x08;
    public static final int PIN_REQUEST = 0x10;
    public static final int USER_0 = 0x20;
    public static final int USER_1 = 0x40;
    public static final int USER_2 = 0x80;

    private final M3SlotAllocator28 owner = new M3SlotAllocator28();
    private final byte[][][] states = new byte[M3Address28.REGION_COUNT][][];
    private int stateRegions;
    private int stateBlocks;

    /** Allocate a fresh or recycled logical slot with zero state, without state-page allocation. */
    public long allocate() { return owner.allocate(); }

    /** Allocate and initialize a slot; zero initialization needs no state page. */
    public long allocate(int initialFlags) {
        requireMask(initialFlags);
        long handle = owner.allocate();
        if (initialFlags != 0) {
            try {
                write(M3SlotAllocator28.slot(handle), initialFlags);
            } catch (RuntimeException | Error failure) {
                try { owner.release(handle); }
                catch (RuntimeException | Error rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
        return handle;
    }

    /** Release a live handle before clearing its metadata; stale/double releases refuse. */
    public void release(long handle) {
        int slot = owner.requireLiveSlot(handle);
        owner.release(handle);
        write(slot, 0);
    }

    public boolean isLive(long handle) { return owner.isLive(handle); }
    public int size() { return owner.size(); }
    public int highWaterMark() { return owner.highWaterMark(); }
    public int freeSlotCount() { return owner.freeSlotCount(); }

    /** O(1) slot/region/page lookup; an untouched page has state zero. */
    public int state(long handle) { return read(owner.requireLiveSlot(handle)); }

    /** Count set bits in this slot's state byte. */
    public int stateBitCount(long handle) { return Integer.bitCount(state(handle)); }

    /** Replace all eight state bits; return prior unsigned state. */
    public int setState(long handle, int state) {
        requireMask(state);
        int slot = owner.requireLiveSlot(handle);
        int old = read(slot);
        if (old != state) write(slot, state);
        return old;
    }

    /**
     * Atomically in method order (not concurrently) set then clear requested bits.
     * Masks must be disjoint; ambiguous overlap refuses before any write.
     */
    public int update(long handle, int setMask, int clearMask) {
        requireMasks(setMask, clearMask);
        if ((setMask & clearMask) != 0) {
            throw new IllegalArgumentException("overlapping set and clear masks");
        }
        int slot = owner.requireLiveSlot(handle);
        int old = read(slot);
        int next = (old | setMask) & ~clearMask & 0xff;
        if (next != old) write(slot, next);
        return old;
    }

    /** First matching *live physical slot*, at or after {@code from}; -1 if absent. */
    public int nextMatchingSlot(int from, int requiredMask, int excludedMask) {
        requireMasks(requiredMask, excludedMask);
        M3Address28.checkRange(from, M3Address28.MAX_SLOTS);
        if ((requiredMask & excludedMask) != 0) return -1;
        for (int slot = owner.nextLiveSlot(from); slot >= 0;
                slot = owner.nextLiveSlot(slot + 1)) {
            if (matches(read(slot), requiredMask, excludedMask)) return slot;
        }
        return -1;
    }

    /** Count matching live physical slots in a checked half-open interval. */
    public int countMatching(int from, int to, int requiredMask, int excludedMask) {
        M3Address28.checkRange(from, to);
        requireMasks(requiredMask, excludedMask);
        if ((requiredMask & excludedMask) != 0) return 0;
        int count = 0;
        for (int slot = owner.nextLiveSlot(from); slot >= 0 && slot < to;
                slot = owner.nextLiveSlot(slot + 1)) {
            if (matches(read(slot), requiredMask, excludedMask)) count++;
        }
        return count;
    }

    /**
     * Stable two-pointer compaction of an ordered handle subrange in-place.
     * Slot generations are checked in a complete preflight pass, before moving
     * any handle; duplicates and original relative order are preserved. The
     * removed suffix in the selected range is zeroed. No scratch array or Entry.
     *
     * @return number of handles retained within [from,to)
     */
    public int compactHandles(long[] orderedHandles, int from, int to,
            int requiredMask, int excludedMask) {
        Objects.requireNonNull(orderedHandles, "orderedHandles");
        Objects.checkFromToIndex(from, to, orderedHandles.length);
        requireMasks(requiredMask, excludedMask);
        validateHandles(orderedHandles, from, to);
        int write = from;
        for (int read = from; read < to; read++) {
            int slot = M3SlotAllocator28.slot(orderedHandles[read]);
            if (matches(read(slot), requiredMask, excludedMask)) {
                orderedHandles[write++] = orderedHandles[read];
            }
        }
        Arrays.fill(orderedHandles, write, to, 0L);
        return write - from;
    }

    /**
     * Detached exact-sized selected handle array. No payload copying or boxes;
     * preserves duplicate handles and encounter order supplied by the caller.
     */
    public long[] snapshotHandles(long[] orderedHandles, int from, int to,
            int requiredMask, int excludedMask) {
        Objects.requireNonNull(orderedHandles, "orderedHandles");
        Objects.checkFromToIndex(from, to, orderedHandles.length);
        requireMasks(requiredMask, excludedMask);
        validateHandles(orderedHandles, from, to);
        int count = 0;
        for (int i = from; i < to; i++) {
            if (matches(read(M3SlotAllocator28.slot(orderedHandles[i])),
                    requiredMask, excludedMask)) count++;
        }
        long[] answer = new long[count];
        int written = 0;
        for (int i = from; i < to; i++) {
            if (matches(read(M3SlotAllocator28.slot(orderedHandles[i])),
                    requiredMask, excludedMask)) answer[written++] = orderedHandles[i];
        }
        return answer;
    }

    /**
     * Export slot-state bytes into caller-owned *position-aligned* mask storage.
     * This is the explicit boundary for existing index/mask Java/JNI compaction.
     * Input handles are validated before any output byte is modified. The
     * exported mask is not live and must be rebuilt after state mutations.
     */
    public void exportStates(long[] orderedHandles, int from, int length,
            byte[] output, int outputFrom) {
        Objects.requireNonNull(orderedHandles, "orderedHandles");
        Objects.requireNonNull(output, "output");
        Objects.checkFromIndexSize(from, length, orderedHandles.length);
        Objects.checkFromIndexSize(outputFrom, length, output.length);
        validateHandles(orderedHandles, from, from + length);
        for (int i = 0; i < length; i++) {
            output[outputFrom + i] = (byte) read(M3SlotAllocator28.slot(orderedHandles[from + i]));
        }
    }

    /** Primitive state bytes only; excludes array headers/directories and slot allocator. */
    public long statePayloadBytes() {
        return (long) stateBlocks * M3Address28.VALUES_PER_BLOCK;
    }

    public int allocatedStateBlocks() { return stateBlocks; }
    public int allocatedStateRegions() { return stateRegions; }
    public long allocatorPayloadBytes() { return owner.payloadBytes(); }

    private void validateHandles(long[] handles, int from, int to) {
        for (int i = from; i < to; i++) owner.requireLiveSlot(handles[i]);
    }

    private static boolean matches(int flags, int required, int excluded) {
        return (flags & required) == required && (flags & excluded) == 0;
    }

    private static void requireMasks(int required, int excluded) {
        requireMask(required);
        requireMask(excluded);
    }

    private static void requireMask(int value) {
        if ((value & ~0xff) != 0) {
            throw new IllegalArgumentException("object state mask must fit an unsigned byte");
        }
    }

    private int read(int slot) {
        byte[][] region = states[slot >>> 20];
        if (region == null) return 0;
        byte[] page = region[(slot >>> 12) & 0xff];
        return page == null ? 0 : page[slot & 4095] & 0xff;
    }

    private void write(int slot, int state) {
        int regionIndex = slot >>> 20;
        byte[][] region = states[regionIndex];
        if (region == null) {
            if (state == 0) return;
            region = new byte[M3Address28.BLOCKS_PER_REGION][];
            states[regionIndex] = region;
            stateRegions++;
        }
        int blockIndex = (slot >>> 12) & 0xff;
        byte[] page = region[blockIndex];
        if (page == null) {
            if (state == 0) return;
            page = new byte[M3Address28.VALUES_PER_BLOCK];
            region[blockIndex] = page;
            stateBlocks++;
        }
        page[slot & 4095] = (byte) state;
    }
}
