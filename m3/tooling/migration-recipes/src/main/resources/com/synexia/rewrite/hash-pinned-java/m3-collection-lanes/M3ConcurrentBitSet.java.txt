/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Fixed-capacity concurrent bit set backed by primitive long words.
 *
 * <p>Each mutation CASes exactly one word. There is no object, node or Boolean
 * wrapper per bit. Multi-word observations such as {@link #cardinalityApprox()}
 * are weakly consistent under concurrent mutation.
 */
public final class M3ConcurrentBitSet {
    private static final VarHandle LONG_ELEMENT =
            MethodHandles.arrayElementVarHandle(long[].class);

    private final long[] words;
    private final int bitCapacity;

    public M3ConcurrentBitSet(int bitCapacity) {
        if (bitCapacity < 0) throw new IllegalArgumentException("bitCapacity");
        this.bitCapacity = bitCapacity;
        int wordCount = (int) ((((long) bitCapacity) + 63L) >>> 6);
        this.words = new long[wordCount];
    }

    public int bitCapacity() { return bitCapacity; }
    public int wordCount() { return words.length; }

    public boolean get(int bit) {
        check(bit);
        long word = (long) LONG_ELEMENT.getAcquire(words, bit >>> 6);
        return (word & mask(bit)) != 0L;
    }

    /** @return true when this call changed 0 -> 1. */
    public boolean set(int bit) {
        check(bit);
        int wordIndex = bit >>> 6;
        long mask = mask(bit);
        for (;;) {
            long before = (long) LONG_ELEMENT.getAcquire(words, wordIndex);
            if ((before & mask) != 0L) return false;
            if (LONG_ELEMENT.compareAndSet(words, wordIndex, before, before | mask)) return true;
        }
    }

    /** @return true when this call changed 1 -> 0. */
    public boolean clear(int bit) {
        check(bit);
        int wordIndex = bit >>> 6;
        long mask = mask(bit);
        for (;;) {
            long before = (long) LONG_ELEMENT.getAcquire(words, wordIndex);
            if ((before & mask) == 0L) return false;
            if (LONG_ELEMENT.compareAndSet(words, wordIndex, before, before & ~mask)) return true;
        }
    }

    /** Atomically flips one bit and returns its new value. */
    public boolean toggle(int bit) {
        check(bit);
        int wordIndex = bit >>> 6;
        long mask = mask(bit);
        for (;;) {
            long before = (long) LONG_ELEMENT.getAcquire(words, wordIndex);
            long after = before ^ mask;
            if (LONG_ELEMENT.compareAndSet(words, wordIndex, before, after)) {
                return (after & mask) != 0L;
            }
        }
    }

    /**
     * Bit-level compare-and-set.
     *
     * @return true when observed state matched expected; setting the same value
     *         is considered a successful no-op
     */
    public boolean compareAndSet(int bit, boolean expected, boolean update) {
        check(bit);
        int wordIndex = bit >>> 6;
        long mask = mask(bit);
        for (;;) {
            long before = (long) LONG_ELEMENT.getAcquire(words, wordIndex);
            boolean observed = (before & mask) != 0L;
            if (observed != expected) return false;
            if (expected == update) return true;
            long after = update ? before | mask : before & ~mask;
            if (LONG_ELEMENT.compareAndSet(words, wordIndex, before, after)) return true;
        }
    }

    public int cardinalityApprox() {
        int count = 0;
        for (int index = 0; index < words.length; index++) {
            count += Long.bitCount((long) LONG_ELEMENT.getAcquire(words, index));
        }
        return count;
    }

    /** Primitive boundary snapshot; trailing unused bits are always zero. */
    public long[] snapshotWords() {
        long[] snapshot = new long[words.length];
        for (int index = 0; index < words.length; index++) {
            snapshot[index] = (long) LONG_ELEMENT.getAcquire(words, index);
        }
        return snapshot;
    }

    public void clearAll() {
        for (int index = 0; index < words.length; index++) {
            LONG_ELEMENT.getAndSet(words, index, 0L);
        }
    }

    /**
     * Ascending set indices, read one word at a time with acquire semantics.
     * Weakly consistent, not a snapshot. No per-bit object or copied bitmap is created.
     */
    public java.util.stream.IntStream setBitStream() {
        return java.util.stream.StreamSupport.intStream(new SetBits(0, words.length), false);
    }

    /** Parallel set-bit stream; partitions whole words into disjoint ranges. */
    public java.util.stream.IntStream parallelSetBitStream() {
        return java.util.stream.StreamSupport.intStream(new SetBits(0, words.length), true);
    }

    private final class SetBits implements java.util.Spliterator.OfInt {
        private int cursor;
        private final int fence;
        private int currentBase;
        private long pending;
        private SetBits(int cursor, int fence) { this.cursor = cursor; this.fence = fence; }
        @Override public OfInt trySplit() {
            if (pending != 0) return null;
            int middle = cursor + ((fence - cursor) >>> 1);
            if (middle == cursor) return null;
            SetBits prefix = new SetBits(cursor, middle); cursor = middle; return prefix;
        }
        @Override public boolean tryAdvance(java.util.function.IntConsumer action) {
            java.util.Objects.requireNonNull(action, "action");
            while (pending == 0) {
                if (cursor == fence) return false;
                currentBase = cursor << 6;
                pending = (long) LONG_ELEMENT.getAcquire(words, cursor++);
            }
            int bit = currentBase + Long.numberOfTrailingZeros(pending);
            pending &= pending - 1;
            action.accept(bit); return true;
        }
        @Override public long estimateSize() { return ((long) fence - cursor) * 64 + Long.bitCount(pending); }
        @Override public int characteristics() { return ORDERED | DISTINCT | NONNULL | CONCURRENT; }
    }

    private void check(int bit) {
        if (bit < 0 || bit >= bitCapacity) throw new IndexOutOfBoundsException(bit);
    }

    private static long mask(int bit) {
        return 1L << (bit & 63);
    }
}
