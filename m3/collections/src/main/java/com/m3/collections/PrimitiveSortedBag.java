/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/** Sorted primitive multiset using the sparse-gap sorted map for value-to-count storage. */
public final class PrimitiveSortedBag {
    @FunctionalInterface
    public interface CountConsumer {
        void accept(long valueBits, int count);
    }

    private final PrimitiveKind kind;
    private final PrimitiveSortedGapMap counts;
    private long size;

    public PrimitiveSortedBag(PrimitiveKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
        counts = new PrimitiveSortedGapMap(kind, PrimitiveKind.INT);
    }

    public PrimitiveKind kind() { return kind; }
    public long size() { return size; }
    public int distinctSize() { return counts.size(); }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return counts.payloadBytes(); }

    public int countBits(long valueBits) {
        return (int) counts.getOrDefaultBits(valueBits, 0L);
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

    public int addBits(long valueBits) {
        return addBits(valueBits, 1);
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

    public int removeBits(long valueBits) {
        return removeBits(valueBits, 1);
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
