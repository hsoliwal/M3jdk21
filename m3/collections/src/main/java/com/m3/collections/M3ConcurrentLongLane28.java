/*
 * Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * CAS-published, stable 28-bit primitive storage for collection values or metadata.
 *
 * <p>Regions and pages are installed once and never replaced or reclaimed. An absent page
 * denotes zeros. No entry, box, node or temporary payload buffer is used per operation.
 * First writes may allocate a region and a page; a publication race can allocate losing
 * candidates which become garbage. Prepare known ranges before a latency-sensitive phase.
 * Prepared-page scalar operations contain no locks, blocking waits or retry loops here;
 * atomic progress still depends on the JVM's VarHandle implementation and scheduling.
 * Allocation, GC, streams and arbitrary user callbacks are not claimed to be lock-free.
 *
 * <p>Each scalar operation is atomic. CAS is over the value, not a generation-tagged
 * handle: ABA and multi-lane transactions remain the owning collection's responsibility.
 * All long bit patterns are valid. Addition wraps exactly like Java long arithmetic.
 * Streams partition slot ranges without flattening or boxing. Their values are weakly
 * consistent under writes; size counts slots, including zeros, not live collection entries.
 */
public final class M3ConcurrentLongLane28 {
    /** Creates an empty instance with no populated storage. */
    public M3ConcurrentLongLane28() {}

    private static final VarHandle REGIONS = MethodHandles.arrayElementVarHandle(long[][][].class);
    private static final VarHandle PAGES = MethodHandles.arrayElementVarHandle(long[][].class);
    private static final VarHandle VALUES = MethodHandles.arrayElementVarHandle(long[].class);
    private final long[][][] regions = new long[M3Address28.REGION_COUNT][][];

    /** Acquire read; absent storage is zero. */
    public long getAcquire(int slot) {
        M3Address28.checkSlot(slot);
        long[] page = page(slot);
        return page == null ? 0 : (long) VALUES.getAcquire(page, slot & 4095);
    }

    /** Release write; even zero prepares an atomic location for publication semantics. */
    public void setRelease(int slot, long value) {
        M3Address28.checkSlot(slot);
        VALUES.setRelease(ensurePage(slot), slot & 4095, value);
    }

    /** Strong atomic compare-and-set, with volatile read/write memory effects. */
    public boolean compareAndSet(int slot, long expected, long update) {
        M3Address28.checkSlot(slot);
        long[] page = page(slot);
        // Even an absent zero-to-zero CAS must use a real atomic location: callers may
        // use successful CAS as a volatile publication/acquisition boundary.
        if (page == null) page = ensurePage(slot);
        return VALUES.compareAndSet(page, slot & 4095, expected, update);
    }

    /** Atomic exchange with volatile read/write effects; returns the prior value. */
    public long getAndSet(int slot, long value) {
        M3Address28.checkSlot(slot);
        return (long) VALUES.getAndSet(ensurePage(slot), slot & 4095, value);
    }

    /** Atomic wrapping addition with volatile read/write effects; returns the prior value. */
    public long getAndAdd(int slot, long delta) {
        M3Address28.checkSlot(slot);
        return (long) VALUES.getAndAdd(ensurePage(slot), slot & 4095, delta);
    }

    /** Prepares all pages intersecting [from,to); no values are changed. */
    public void prepare(int from, int to) {
        M3Address28.checkRange(from, to);
        while (from < to) {
            ensurePage(from);
            from = Math.min(to, (from | 4095) + 1);
        }
    }

    /** Weakly consistent page count; directories/pages are never removed. */
    public int allocatedBlockCount() {
        int count = 0;
        for (int r = 0; r < regions.length; r++) {
            long[][] region = (long[][]) REGIONS.getAcquire(regions, r);
            if (region == null) continue;
            for (int b = 0; b < region.length; b++) {
                if (PAGES.getAcquire(region, b) != null) count++;
            }
        }
        return count;
    }

    /** Estimated retained primitive payload bytes, excluding array headers and directories. */
    public long payloadBytes() {
        return (long) allocatedBlockCount() * M3Address28.VALUES_PER_BLOCK * Long.BYTES;
    }

    /** Disjoint slot partitions; individual reads use acquire semantics. */
    public Spliterator.OfLong spliterator(int from, int to) {
        M3Address28.checkRange(from, to);
        return new Range(this, from, to);
    }

    /** Primitive sequential stream over a fixed slot range; no snapshot is implied. */
    public LongStream stream(int from, int to) {
        return StreamSupport.longStream(spliterator(from, to), false);
    }

    /** Primitive parallel stream with O(1) range splitting and no element buffers. */
    public LongStream parallelStream(int from, int to) {
        return StreamSupport.longStream(spliterator(from, to), true);
    }

    private long[] page(int slot) {
        long[][] region = (long[][]) REGIONS.getAcquire(regions, slot >>> 20);
        return region == null ? null : (long[]) PAGES.getAcquire(region, (slot >>> 12) & 255);
    }

    private long[] ensurePage(int slot) {
        int r = slot >>> 20;
        long[][] region = (long[][]) REGIONS.getAcquire(regions, r);
        if (region == null) {
            long[][] candidate = new long[M3Address28.BLOCKS_PER_REGION][];
            region = (long[][]) REGIONS.compareAndExchange(regions, r, null, candidate);
            if (region == null) region = candidate;
        }
        int b = (slot >>> 12) & 255;
        long[] page = (long[]) PAGES.getAcquire(region, b);
        if (page == null) {
            long[] candidate = new long[M3Address28.VALUES_PER_BLOCK];
            page = (long[]) PAGES.compareAndExchange(region, b, null, candidate);
            if (page == null) page = candidate;
        }
        return page;
    }

    private static final class Range implements Spliterator.OfLong {
        private final M3ConcurrentLongLane28 owner;
        private int cursor;
        private final int fence;

        private Range(M3ConcurrentLongLane28 owner, int cursor, int fence) {
            this.owner = owner; this.cursor = cursor; this.fence = fence;
        }

        @Override public OfLong trySplit() {
            int middle = cursor + ((fence - cursor) >>> 1);
            if (middle == cursor) return null;
            Range prefix = new Range(owner, cursor, middle);
            cursor = middle;
            return prefix;
        }

        @Override public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            if (cursor == fence) return false;
            long value = owner.getAcquire(cursor++);
            action.accept(value);
            return true;
        }

        @Override public void forEachRemaining(LongConsumer action) {
            Objects.requireNonNull(action, "action");
            while (cursor < fence) action.accept(owner.getAcquire(cursor++));
        }

        @Override public long estimateSize() { return fence - cursor; }

        @Override public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL | CONCURRENT;
        }
    }
}
