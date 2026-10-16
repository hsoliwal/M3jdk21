/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * Derived from Synexia's m3-four-library-bags count-lane oracle (2026-10-05).
 */
package com.m3.collections;

import java.util.Objects;

/**
 * Checked algebra over already key-aligned nonnegative occurrence-count lanes.
 * Inputs must stay unchanged during either call. Results are independently owned
 * snapshots, not live views. No input is modified, including on failure.
 *
 * <p>The Java path is always available. The native path is deliberately explicit:
 * the caller first loads its trusted compiled library using {@link System#load(String)}.
 * An unavailable native library fails with {@link UnsatisfiedLinkError}; no silent
 * fallback or automatic performance-based dispatch is claimed. Native execution uses
 * one result array and three bounded 1024-element scratch lanes, not pinned heap pointers.
 * Both implementations reject individual or aggregate overflow above Long.MAX_VALUE.
 */
public final class M3CountLanes {
    public static final int SUM = 0;
    public static final int UNION = 1;
    public static final int INTERSECTION = 2;
    public static final int DIFFERENCE = 3;

    private M3CountLanes() { }

    /**
     * Combines corresponding counts: checked sum, maximum, minimum, or floored
     * subtraction. Validation order is null inputs, bounds, operation, then counts.
     * Time is O(length); output storage is O(length). Zero length is permitted.
     * @param left first stable input
     * @param leftOffset start in first input
     * @param right second stable input (may alias first input)
     * @param rightOffset start in second input
     * @param length number of aligned keys
     * @param operation one of SUM, UNION, INTERSECTION or DIFFERENCE
     * @return independent count lane
     */
    public static long[] combine(long[] left, int leftOffset, long[] right,
                                 int rightOffset, int length, int operation) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromIndexSize(leftOffset, length, left.length);
        Objects.checkFromIndexSize(rightOffset, length, right.length);
        if (operation < SUM || operation > DIFFERENCE) {
            throw new IllegalArgumentException("operation");
        }
        long[] result = new long[length];
        long total = 0;
        for (int i = 0; i < length; i++) {
            long a = left[leftOffset + i], b = right[rightOffset + i];
            if (a < 0 || b < 0) throw new IllegalArgumentException("negative count");
            long value = switch (operation) {
                case SUM -> Math.addExact(a, b);
                case UNION -> Math.max(a, b);
                case INTERSECTION -> Math.min(a, b);
                default -> Math.max(0L, a - b);
            };
            total = Math.addExact(total, value);
            result[i] = value;
        }
        return result;
    }

    /**
     * Native counterpart of {@link #combine(long[], int, long[], int, int, int)}.
     * The same stable-input, validation and overflow contracts apply. Exception
     * messages and allocation-failure timing are not promised to be identical.
     */
    public static native long[] combineNative(long[] left, int leftOffset,
            long[] right, int rightOffset, int length, int operation);
}
