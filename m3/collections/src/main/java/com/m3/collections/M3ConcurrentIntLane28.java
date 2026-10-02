/*
 * Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
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
 * All int bit patterns are valid. Addition wraps exactly like Java int arithmetic.
 * Streams partition slot ranges without flattening or boxing. Their values are weakly
 * consistent under writes; size counts slots, including zeros, not live collection entries.
 */
public final class M3ConcurrentIntLane28 {
    /** Creates an empty instance with no populated storage. */
    public M3ConcurrentIntLane28() {}

    private static final VarHandle REGIONS = MethodHandles.arrayElementVarHandle(int[][][].class);
    private static final VarHandle PAGES = MethodHandles.arrayElementVarHandle(int[][].class);
    private static final VarHandle VALUES = MethodHandles.arrayElementVarHandle(int[].class);
    private final int[][][] regions = new int[M3Address28.REGION_COUNT][][];

    /** Acquire read; absent storage is zero. */
    public int getAcquire(int slot) {
        M3Address28.checkSlot(slot);
        int[] page = page(slot);
        return page == null ? 0 : (int) VALUES.getAcquire(page, slot & 4095);
    }

    /** Release write; even zero prepares an atomic location for publication semantics. */
    public void setRelease(int slot, int value) {
        M3Address28.checkSlot(slot);
        VALUES.setRelease(ensurePage(slot), slot & 4095, value);
    }

    /** Strong atomic compare-and-set, with volatile read/write memory effects. */
    public boolean compareAndSet(int slot, int expected, int update) {
        M3Address28.checkSlot(slot);
        int[] page = page(slot);
        // Even an absent zero-to-zero CAS must use a real atomic location: callers may
        // use successful CAS as a volatile publication/acquisition boundary.
        if (page == null) page = ensurePage(slot);
        return VALUES.compareAndSet(page, slot & 4095, expected, update);
    }

    /** Atomic exchange with volatile read/write effects; returns the prior value. */
    public int getAndSet(int slot, int value) {
        M3Address28.checkSlot(slot);
        return (int) VALUES.getAndSet(ensurePage(slot), slot & 4095, value);
    }

    /** Atomic wrapping addition with volatile read/write effects; returns the prior value. */
    public int getAndAdd(int slot, int delta) {
        M3Address28.checkSlot(slot);
        return (int) VALUES.getAndAdd(ensurePage(slot), slot & 4095, delta);
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
            int[][] region = (int[][]) REGIONS.getAcquire(regions, r);
            if (region == null) continue;
            for (int b = 0; b < region.length; b++) {
                if (PAGES.getAcquire(region, b) != null) count++;
            }
        }
        return count;
    }

    /** Estimated retained primitive payload bytes, excluding array headers and directories. */
    public long payloadBytes() {
        return (long) allocatedBlockCount() * M3Address28.VALUES_PER_BLOCK * Integer.BYTES;
    }

    /** Disjoint slot partitions; individual reads use acquire semantics. */
    public Spliterator.OfInt spliterator(int from, int to) {
        M3Address28.checkRange(from, to);
        return new Range(this, from, to);
    }

    /** Primitive sequential stream over a fixed slot range; no snapshot is implied. */
    public IntStream stream(int from, int to) {
        return StreamSupport.intStream(spliterator(from, to), false);
    }

    /** Primitive parallel stream with O(1) range splitting and no element buffers. */
    public IntStream parallelStream(int from, int to) {
        return StreamSupport.intStream(spliterator(from, to), true);
    }

    private int[] page(int slot) {
        int[][] region = (int[][]) REGIONS.getAcquire(regions, slot >>> 20);
        return region == null ? null : (int[]) PAGES.getAcquire(region, (slot >>> 12) & 255);
    }

    private int[] ensurePage(int slot) {
        int r = slot >>> 20;
        int[][] region = (int[][]) REGIONS.getAcquire(regions, r);
        if (region == null) {
            int[][] candidate = new int[M3Address28.BLOCKS_PER_REGION][];
            region = (int[][]) REGIONS.compareAndExchange(regions, r, null, candidate);
            if (region == null) region = candidate;
        }
        int b = (slot >>> 12) & 255;
        int[] page = (int[]) PAGES.getAcquire(region, b);
        if (page == null) {
            int[] candidate = new int[M3Address28.VALUES_PER_BLOCK];
            page = (int[]) PAGES.compareAndExchange(region, b, null, candidate);
            if (page == null) page = candidate;
        }
        return page;
    }

    private static final class Range implements Spliterator.OfInt {
        private final M3ConcurrentIntLane28 owner;
        private int cursor;
        private final int fence;

        private Range(M3ConcurrentIntLane28 owner, int cursor, int fence) {
            this.owner = owner; this.cursor = cursor; this.fence = fence;
        }

        @Override public OfInt trySplit() {
            int middle = cursor + ((fence - cursor) >>> 1);
            if (middle == cursor) return null;
            Range prefix = new Range(owner, cursor, middle);
            cursor = middle;
            return prefix;
        }

        @Override public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (cursor == fence) return false;
            int value = owner.getAcquire(cursor++);
            action.accept(value);
            return true;
        }

        @Override public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            while (cursor < fence) action.accept(owner.getAcquire(cursor++));
        }

        @Override public long estimateSize() { return fence - cursor; }

        @Override public int characteristics() {
            return ORDERED | SIZED | SUBSIZED | NONNULL | CONCURRENT;
        }
    }
}
