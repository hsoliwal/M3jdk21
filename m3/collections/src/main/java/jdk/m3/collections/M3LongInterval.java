/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Immutable, inclusive arithmetic progression of primitive long values.
 *
 * <p>Inspired by the Eclipse Collections LongInterval positional contract;
 * no Eclipse source or storage implementation is copied. Only scalar facts
 * are retained; no value array, boxed element or per-position state object.
 * The signed-long domain is checked with unsigned-distance arithmetic, so
 * ranges crossing zero and even steps of Long.MIN_VALUE do not overflow the
 * element count or accidentally wrap. The existing int-sized API refuses
 * progressions longer than Integer.MAX_VALUE instead of truncating them.</p>
 *
 * <p>Reverse returns a new immutable progression. It never changes this
 * instance, including any original iterator/consumer observations.</p>
 */
public final class M3LongInterval {
    static final long STATE_FROZEN = 1L << 0;

    private final long from;
    private final long to;
    private final long step;
    private final int size;

    /** Creates an empty immutable sequence. */
    public M3LongInterval() {
        this(0, 0, 1, 0);
    }

    private M3LongInterval(long from, long to, long step, int size) {
        this.from = from;
        this.to = to;
        this.step = step;
        this.size = size;
    }

    /** Preserves the original public inventory descriptor. */
    public static String scaffoldDescriptor() {
        return "M3LongInterval|LONG_INTERVAL|LONG|FROZEN|long from;long to;long step";
    }

    /** Inclusive ascending or descending unit-step progression. */
    public static M3LongInterval fromTo(long from, long to) {
        return fromToBy(from, to, from <= to ? 1 : -1);
    }

    /**
     * Inclusive progression. Step must be nonzero and point towards the bound.
     * A singleton accepts either sign. Large intervals are refused because
     * size(), get(int) and indexOf(long) use signed 32-bit positions.
     */
    public static M3LongInterval fromToBy(long from, long to, long step) {
        if (step == 0 || from < to && step < 0 || from > to && step > 0) {
            throw new IllegalArgumentException("step must be nonzero and point towards the bound");
        }
        long distance = from <= to ? to - from : from - to; // unsigned distance
        long magnitude = step >= 0 ? step : -step; // MIN_VALUE is unsigned 2^63
        long steps = Long.divideUnsigned(distance, magnitude);
        if (Long.compareUnsigned(steps, (long) Integer.MAX_VALUE - 1L) > 0) {
            throw new IllegalArgumentException("interval exceeds int-indexed capacity");
        }
        return new M3LongInterval(from, to, step, (int) steps + 1);
    }

    /** Inclusive progression from zero, decreasing for negative bounds. */
    public static M3LongInterval zeroTo(long to) {
        return fromTo(0, to);
    }

    /** First value or NoSuchElementException for an empty sequence. */
    public long getFirst() {
        if (size == 0) throw new NoSuchElementException();
        return from;
    }

    /** Last actual element; the supplied upper bound need not be reached. */
    public long getLast() {
        if (size == 0) throw new NoSuchElementException();
        return valueAt(size - 1);
    }

    /** O(1) lookup without materializing an array. */
    public long get(int index) {
        Objects.checkIndex(index, size);
        return valueAt(index);
    }

    private long valueAt(int index) {
        // Java's modulo-2^64 intermediate arithmetic yields the exact final
        // signed long for every in-range progression element.
        return from + (long) index * step;
    }

    public int size() { return size; }

    /** Constant-time arithmetic membership, including both signed extremes. */
    public boolean contains(long value) { return indexOf(value) >= 0; }

    /** First logical position or -1; uses unsigned arithmetic to avoid overflow. */
    public int indexOf(long value) {
        if (size == 0) return -1;
        long last = getLast();
        boolean ascending = from <= last;
        if (ascending ? (value < from || value > last) : (value > from || value < last)) {
            return -1;
        }
        long distance = ascending ? value - from : from - value;
        long magnitude = step >= 0 ? step : -step;
        if (Long.remainderUnsigned(distance, magnitude) != 0) return -1;
        long index = Long.divideUnsigned(distance, magnitude);
        return Long.compareUnsigned(index, size) < 0 ? (int) index : -1;
    }

    /** Reversed immutable view with the same values, also for MIN_VALUE steps. */
    public M3LongInterval reverseThis() {
        if (size < 2) return this;
        return new M3LongInterval(getLast(), from, -step, size);
    }

    /** Explicit detached array materialization; may fail under memory pressure. */
    public long[] toArray() {
        long[] copy = new long[size];
        for (int i = 0; i < size; i++) copy[i] = valueAt(i);
        return copy;
    }

    /** Native primitive callback iteration, without intermediate objects. */
    public void forEach(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(valueAt(i));
    }
}
