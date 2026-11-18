/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Primitive long-to-long map with passive time-to-live expiry.
 *
 * <p>Each mapping owns one ordinal in parallel dynamic long key, value and
 * deadline lanes. The existing {@link PackedLongRowIndex} references those
 * ordinals without retaining boxed keys, buckets or Entry objects. Three
 * state bits per slot are packed in {@code long[]} mask words, 21 slots per
 * word. Deletion swaps the last row and repairs the single canonical index.
 *
 * <p>Negative TTL (including the original no-argument constructor) means
 * never expire, matching the Apache Commons PassiveExpiringMap default.
 * Zero TTL expires on the next access, and positive TTL is measured in
 * milliseconds from the last put. Expiration is passive: key reads purge only
 * their key; {@link #size()} sweeps the full table. No background threads.
 * Not thread-safe. Hash lookup is expected O(1), worst-case O(n) on collision.
 */
public final class ExpiringLongLongMapView {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;
    /** State-mask bit {@code EXPIRED} (per-slot state word, bit 2). */
    static final long STATE_EXPIRED = 1L << 2;

    private static final int SLOTS_PER_MASK_WORD = 21;
    private static final long STATE_BITS = 7L;

    private long[] keyLane = new long[0];
    private long[] valueLane = new long[0];
    private long[] deadlineLane = new long[0];
    private long[] stateMaskWords = new long[0];
    private int size;

    private final PackedLongRowIndex index = new PackedLongRowIndex(0);
    private final long ttlMillis;
    private final LongSupplier clock;

    /** No expiration, as in the Apache Commons default constructor. */
    public ExpiringLongLongMapView() {
        this(-1L, System::currentTimeMillis);
    }

    /** Constant time-to-live in milliseconds; negative means no expiration. */
    public ExpiringLongLongMapView(long ttlMillis) {
        this(ttlMillis, System::currentTimeMillis);
    }

    /**
     * Clock-injected TTL constructor. The supplier returns milliseconds, is
     * called once per expiry-sensitive operation and must be monotonic for
     * deterministic expiration semantics. No per-entry supplier is retained.
     */
    public ExpiringLongLongMapView(long ttlMillis, LongSupplier clock) {
        this.ttlMillis = ttlMillis;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Preserves the historical state and lane descriptor literally. */
    public static String scaffoldDescriptor() {
        return "M3ExpiringLongLongMapView|EXPIRING_LONG_MAP|LONG|OCCUPIED,DELETED,EXPIRED|long[] keyLane;long[] valueLane;long[] deadlineLane";
    }

    /** Returns the unexpired mapping, or the fallback for a missing/expired key. */
    public long getOrDefault(long key, long defaultValue) {
        int at = liveIndex(key, now());
        return at < 0 ? defaultValue : valueLane[at];
    }

    /** Replaces a mapping and its deadline; returns old unexpired value or 0. */
    public long put(long key, long value) {
        long now = now();
        int at = liveIndex(key, now);
        long deadline = deadline(now);
        if (at >= 0) {
            long previous = valueLane[at];
            valueLane[at] = value;
            deadlineLane[at] = deadline;
            return previous;
        }
        reserve(Math.addExact(size, 1));
        keyLane[size] = key;
        valueLane[size] = value;
        deadlineLane[size] = deadline;
        markState(size, (int) STATE_OCCUPIED);
        index.insert(size, keyLane);
        size++;
        return 0;
    }

    /** Removes the key only if it has not already expired; otherwise returns 0. */
    public long remove(long key) {
        int at = liveIndex(key, now());
        if (at < 0) return 0;
        long old = valueLane[at];
        removeAt(at, false);
        return old;
    }

    /** True only for an unexpired mapping, including values equal to zero. */
    public boolean containsKey(long key) {
        return liveIndex(key, now()) >= 0;
    }

    /** Sweeps all expired rows before returning the live cardinality. */
    public int size() {
        if (ttlMillis >= 0) {
            long now = now();
            for (int row = size - 1; row >= 0; row--) {
                if (expired(row, now)) removeAt(row, true);
            }
        }
        return size;
    }

    /** Clears indexed mappings while retaining reusable array capacity. */
    public void clear() {
        index.clear();
        Arrays.fill(stateMaskWords, 0);
        size = 0;
    }

    private long now() {
        return ttlMillis < 0 ? 0 : clock.getAsLong();
    }

    private long deadline(long now) {
        if (ttlMillis < 0) return Long.MAX_VALUE;
        if (ttlMillis > 0 && now > Long.MAX_VALUE - ttlMillis)
            return Long.MAX_VALUE;
        return now + ttlMillis;
    }

    private boolean expired(int row, long now) {
        return ttlMillis >= 0 && now >= deadlineLane[row];
    }

    /** A targeted read never performs an unrelated full-map expiry sweep. */
    private int liveIndex(long key, long now) {
        int at = index.find(key, keyLane);
        if (at >= 0 && expired(at, now)) {
            removeAt(at, true);
            return -1;
        }
        return at;
    }

    private void reserve(int required) {
        index.ensure(required, keyLane);
        if (required <= keyLane.length) return;
        int grown = PackedLongRowIndex.grownLength(keyLane.length, required);
        long[] keys = Arrays.copyOf(keyLane, grown);
        long[] values = Arrays.copyOf(valueLane, grown);
        long[] deadlines = Arrays.copyOf(deadlineLane, grown);
        long[] masks = Arrays.copyOf(stateMaskWords,
                (int) ((grown + (long) SLOTS_PER_MASK_WORD - 1) / SLOTS_PER_MASK_WORD));
        keyLane = keys;
        valueLane = values;
        deadlineLane = deadlines;
        stateMaskWords = masks;
    }

    private int state(int slot) {
        int bit = (slot % SLOTS_PER_MASK_WORD) * 3;
        return (int) ((stateMaskWords[slot / SLOTS_PER_MASK_WORD] >>> bit) & STATE_BITS);
    }

    private void markState(int slot, int flags) {
        int word = slot / SLOTS_PER_MASK_WORD;
        int bit = (slot % SLOTS_PER_MASK_WORD) * 3;
        long mask = STATE_BITS << bit;
        stateMaskWords[word] = (stateMaskWords[word] & ~mask)
                | (((long) flags & STATE_BITS) << bit);
    }

    /** Tombstone-free last-row relocation, including deadline and state lanes. */
    private void removeAt(int slot, boolean byExpiry) {
        markState(slot, (int) (STATE_OCCUPIED |
                (byExpiry ? STATE_EXPIRED : STATE_DELETED)));
        index.remove(keyLane[slot], keyLane);
        int last = --size;
        if (slot != last) {
            index.relocate(last, slot, keyLane);
            keyLane[slot] = keyLane[last];
            valueLane[slot] = valueLane[last];
            deadlineLane[slot] = deadlineLane[last];
            markState(slot, state(last));
        }
        keyLane[last] = 0;
        valueLane[last] = 0;
        deadlineLane[last] = 0;
        markState(last, 0);
    }
}
