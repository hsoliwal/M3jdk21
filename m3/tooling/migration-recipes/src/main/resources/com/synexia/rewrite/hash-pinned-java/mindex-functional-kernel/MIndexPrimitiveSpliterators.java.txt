/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.mindex.function;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongUnaryOperator;

/** Primitive Spliterator factories for bounded MIndex ranges, arrays and finite iterate plans. */
public final class MIndexPrimitiveSpliterators {
    private static final int ORDERED_SIZED =
            Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;

    private MIndexPrimitiveSpliterators() {}

    public static Spliterator.OfInt range(int startInclusive, int endExclusive, int step) {
        if (step == 0) {
            throw new IllegalArgumentException("zero step");
        }
        long size = rangeSize(startInclusive, endExclusive, step);
        return new IntRange(startInclusive, step, size);
    }

    public static Spliterator.OfInt iterate(
            int seed, IntUnaryOperator next, long count) {
        return new IntIterate(seed, Objects.requireNonNull(next), checkedCount(count));
    }

    public static Spliterator.OfLong iterate(
            long seed, LongUnaryOperator next, long count) {
        return new LongIterate(seed, Objects.requireNonNull(next), checkedCount(count));
    }

    public static Spliterator.OfDouble iterate(
            double seed, DoubleUnaryOperator next, long count) {
        return new DoubleIterate(seed, Objects.requireNonNull(next), checkedCount(count));
    }

    private static long checkedCount(long count) {
        if (count < 0L) {
            throw new IllegalArgumentException("negative count");
        }
        return count;
    }

    private static long rangeSize(int start, int end, int step) {
        if (step > 0) {
            if (start >= end) return 0L;
            long distance = (long) end - start;
            return (distance + step - 1L) / step;
        }
        if (start <= end) return 0L;
        long magnitude = -(long) step;
        long distance = (long) start - end;
        return (distance + magnitude - 1L) / magnitude;
    }

    private static final class IntRange implements Spliterator.OfInt {
        private int current;
        private final int step;
        private long remaining;

        IntRange(int current, int step, long remaining) {
            this.current = current;
            this.step = step;
            this.remaining = remaining;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action);
            if (remaining == 0L) return false;
            int value = current;
            remaining--;
            if (remaining != 0L) current = Math.addExact(current, step);
            action.accept(value);
            return true;
        }

        @Override
        public OfInt trySplit() {
            long half = remaining >>> 1;
            if (half == 0L) return null;
            int splitStart = current;
            long delta = Math.multiplyExact(half, (long) step);
            current = Math.toIntExact(Math.addExact((long) current, delta));
            remaining -= half;
            return new IntRange(splitStart, step, half);
        }

        @Override public long estimateSize() { return remaining; }
        @Override public int characteristics() { return ORDERED_SIZED; }
    }

    private static final class IntIterate implements Spliterator.OfInt {
        private int current;
        private final IntUnaryOperator next;
        private long remaining;

        IntIterate(int current, IntUnaryOperator next, long remaining) {
            this.current = current;
            this.next = next;
            this.remaining = remaining;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action);
            if (remaining == 0L) return false;
            int value = current;
            remaining--;
            if (remaining != 0L) current = next.applyAsInt(current);
            action.accept(value);
            return true;
        }

        @Override public OfInt trySplit() { return null; }
        @Override public long estimateSize() { return remaining; }
        @Override public int characteristics() { return Spliterator.ORDERED | Spliterator.SIZED; }
    }

    private static final class LongIterate implements Spliterator.OfLong {
        private long current;
        private final LongUnaryOperator next;
        private long remaining;

        LongIterate(long current, LongUnaryOperator next, long remaining) {
            this.current = current;
            this.next = next;
            this.remaining = remaining;
        }

        @Override
        public boolean tryAdvance(LongConsumer action) {
            Objects.requireNonNull(action);
            if (remaining == 0L) return false;
            long value = current;
            remaining--;
            if (remaining != 0L) current = next.applyAsLong(current);
            action.accept(value);
            return true;
        }

        @Override public OfLong trySplit() { return null; }
        @Override public long estimateSize() { return remaining; }
        @Override public int characteristics() { return Spliterator.ORDERED | Spliterator.SIZED; }
    }

    private static final class DoubleIterate implements Spliterator.OfDouble {
        private double current;
        private final DoubleUnaryOperator next;
        private long remaining;

        DoubleIterate(double current, DoubleUnaryOperator next, long remaining) {
            this.current = current;
            this.next = next;
            this.remaining = remaining;
        }

        @Override
        public boolean tryAdvance(DoubleConsumer action) {
            Objects.requireNonNull(action);
            if (remaining == 0L) return false;
            double value = current;
            remaining--;
            if (remaining != 0L) current = next.applyAsDouble(current);
            action.accept(value);
            return true;
        }

        @Override public OfDouble trySplit() { return null; }
        @Override public long estimateSize() { return remaining; }
        @Override public int characteristics() { return Spliterator.ORDERED | Spliterator.SIZED; }
    }
}
