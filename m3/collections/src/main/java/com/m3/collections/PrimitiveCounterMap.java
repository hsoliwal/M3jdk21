/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Primitive-key long counter map.
 *
 * <p>The configured missing value is not retained. An update that reaches the
 * missing value removes the key, matching counter-map semantics without an
 * entry object or boxed counter.
 */
public final class PrimitiveCounterMap {
    @FunctionalInterface
    public interface CounterConsumer {
        void accept(long keyBits, long count);
    }

    private final PrimitiveKind keyKind;
    private final long missingValue;
    private final PrimitivePrimitiveHashMap counts;

    public PrimitiveCounterMap(PrimitiveKind keyKind) {
        this(keyKind, 0L);
    }

    public PrimitiveCounterMap(PrimitiveKind keyKind, long missingValue) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.missingValue = missingValue;
        counts = new PrimitivePrimitiveHashMap(keyKind, PrimitiveKind.LONG);
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public long missingValue() { return missingValue; }
    public int size() { return counts.size(); }
    public boolean isEmpty() { return counts.isEmpty(); }
    public long payloadBytes() { return counts.payloadBytes(); }

    public boolean containsKeyBits(long keyBits) {
        return counts.containsKeyBits(keyBits);
    }

    public long get(long keyBits) {
        return counts.getOrDefaultBits(keyBits, missingValue);
    }

    public long put(long keyBits, long value) {
        if (value == missingValue) {
            throw new IllegalArgumentException("cannot store missingValue");
        }
        return counts.putBits(keyBits, value, missingValue);
    }

    public long getAndAdd(long keyBits, long delta) {
        long previous = get(keyBits);
        if (delta == 0L) return previous;
        long next = Math.addExact(previous, delta);
        if (next == missingValue) {
            counts.removeOrDefaultBits(keyBits, missingValue);
        } else {
            counts.putBits(keyBits, next, missingValue);
        }
        return previous;
    }

    public long addAndGet(long keyBits, long delta) {
        long previous = getAndAdd(keyBits, delta);
        return Math.addExact(previous, delta);
    }

    public long increment(long keyBits) { return addAndGet(keyBits, 1L); }
    public long decrement(long keyBits) { return addAndGet(keyBits, -1L); }
    public long getAndIncrement(long keyBits) { return getAndAdd(keyBits, 1L); }
    public long getAndDecrement(long keyBits) { return getAndAdd(keyBits, -1L); }

    public long minValue() {
        return counts.minValueOrDefaultBits(missingValue);
    }

    public long maxValue() {
        return counts.maxValueOrDefaultBits(missingValue);
    }

    public long remove(long keyBits) {
        return counts.removeOrDefaultBits(keyBits, missingValue);
    }

    public void clear() { counts.clear(); }

    public void forEach(CounterConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        counts.forEach(consumer::accept);
    }
}
