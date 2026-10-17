/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/** Primitive multiset/bag backed by a primitive-key to int-count hash table. */
public final class PrimitiveBag {
    @FunctionalInterface
    public interface CountConsumer {
        void accept(long valueBits, int count);
    }

    private final PrimitiveKind kind;
    private final PrimitivePrimitiveHashMap counts;
    private long size;

    public PrimitiveBag(PrimitiveKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
        counts = new PrimitivePrimitiveHashMap(kind, PrimitiveKind.INT);
    }

    public PrimitiveKind kind() { return kind; }
    public long size() { return size; }
    public int distinctSize() { return counts.size(); }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return counts.payloadBytes(); }

    public int countBits(long valueBits) {
        return (int) counts.getOrDefaultBits(valueBits, 0L);
    }

    public int addBits(long valueBits) {
        return addBits(valueBits, 1);
    }

    public int addBits(long valueBits, int occurrences) {
        if (occurrences < 0) throw new IllegalArgumentException("occurrences < 0");
        if (occurrences == 0) return countBits(valueBits);
        int old = countBits(valueBits);
        long next = (long) old + occurrences;
        if (next > Integer.MAX_VALUE) throw new IllegalStateException("count overflow");
        counts.putBits(valueBits, next, 0L);
        size = Math.addExact(size, occurrences);
        return (int) next;
    }

    public int removeBits(long valueBits) {
        return removeBits(valueBits, 1);
    }

    public int removeBits(long valueBits, int occurrences) {
        if (occurrences < 0) throw new IllegalArgumentException("occurrences < 0");
        int old = countBits(valueBits);
        if (old == 0 || occurrences == 0) return old;
        int removed = Math.min(old, occurrences);
        int next = old - removed;
        if (next == 0) counts.removeOrDefaultBits(valueBits, 0L);
        else counts.putBits(valueBits, next, 0L);
        size -= removed;
        return next;
    }

    /**
     * Assigns an exact occurrence count and returns the previous count.
     * Zero removes the value. Unlike the legacy add/remove methods, this method
     * returns the count before the change. This bag is not thread-safe.
     *
     * @throws IllegalArgumentException if count is negative
     */
    public int setCountBits(long valueBits, int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        int old = countBits(valueBits);
        if (old == count) return old;
        long nextSize = Math.addExact(size, (long) count - old);
        if (count == 0) counts.removeOrDefaultBits(valueBits, 0L);
        else counts.putBits(valueBits, count, 0L);
        size = nextSize;
        return old;
    }

    /**
     * Assigns count only when the current count equals expectedCount.
     * Both counts are validated before comparison. This is a conditional update,
     * not a concurrent compare-and-set operation.
     *
     * @throws IllegalArgumentException if either count is negative
     */
    public boolean setCountBits(long valueBits, int expectedCount, int count) {
        if (expectedCount < 0 || count < 0) throw new IllegalArgumentException("negative count");
        if (countBits(valueBits) != expectedCount) return false;
        setCountBits(valueBits, count);
        return true;
    }

    /** Removes every occurrence of a value and returns the number removed. */
    public int removeAllBits(long valueBits) {
        return setCountBits(valueBits, 0);
    }

    /** Releases unused count-table capacity without removing any occurrence. */
    public void compact() {
        counts.compact();
    }

    public void clear() {
        counts.clear();
        size = 0;
    }

    public void forEachCount(CountConsumer action) {
        Objects.requireNonNull(action, "action");
        counts.forEach((value, count) -> action.accept(value, (int) count));
    }
}
