/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * Immutable, detached stable permutation of source-row coordinates ordered by domain signals.
 * Only primitive row ordinals are retained. This is NOT a live Java Collection view, value
 * identity, comparator substitute, or a promise that a hashed class fingerprint is ordered.
 * Signal projection and mask selection occur once; all radix passes inspect primitives only.
 */
public final class M3SignalOrder {
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private static final int RADIX = 256;
    private static final int[] EMPTY = new int[0];
    private final int[] rows;
    private final int from;
    private final int to;

    private M3SignalOrder(int[] rows, int from, int to) {
        this.rows = rows;
        this.from = from;
        this.to = to;
    }

    public static M3SignalOrder ascending(double[] signals) {
        return order(signals, null, 0, 0, true, Backend.JAVA);
    }

    public static M3SignalOrder descending(double[] signals) {
        return order(signals, null, 0, 0, false, Backend.JAVA);
    }

    /** Source mask is aligned to input rows, NOT to the resulting sorted positions. */
    public static M3SignalOrder filtered(double[] signals, byte[] masks,
            int requiredMask, int excludedMask, boolean ascending, Backend backend) {
        Objects.requireNonNull(masks, "masks");
        return order(signals, masks, requiredMask, excludedMask, ascending, backend);
    }

    /**
     * Full-width physical-slot state-mask selection before stable primitive ordering.
     * Bits 0..31 are all significant, including the sign bit. The state lane is
     * caller-owned and never truncated to bytes or modified. JNI, when requested,
     * receives only the selected primitive key/row lanes, not Java objects.
     */
    public static M3SignalOrder filtered32(double[] signals, int[] states,
            int requiredMask, int excludedMask, boolean ascending, Backend backend) {
        Objects.requireNonNull(states, "states");
        return orderCore(signals, null, states, requiredMask, excludedMask, ascending, backend);
    }

    /** A custom domain projection is evaluated once per object before the primitive sort. */
    public static <T> M3SignalOrder ascending(T[] values,
            ToDoubleFunction<? super T> projection) {
        return project(values, projection, true);
    }

    public static <T> M3SignalOrder descending(T[] values,
            ToDoubleFunction<? super T> projection) {
        return project(values, projection, false);
    }

    private static <T> M3SignalOrder project(T[] values,
            ToDoubleFunction<? super T> projection, boolean ascending) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(projection, "projection");
        double[] signals = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            signals[i] = projection.applyAsDouble(Objects.requireNonNull(values[i], "values[" + i + "]"));
        }
        return order(signals, null, 0, 0, ascending, Backend.JAVA);
    }

    private static M3SignalOrder order(double[] signals, byte[] masks,
            int requiredMask, int excludedMask, boolean ascending, Backend backend) {
        return orderCore(signals, masks, null, requiredMask, excludedMask, ascending, backend);
    }

    /** One scan/count and one fill for either metadata-width variant. */
    private static M3SignalOrder orderCore(double[] signals, byte[] masks, int[] states32,
            int requiredMask, int excludedMask, boolean ascending, Backend backend) {
        Objects.requireNonNull(signals, "signals");
        Objects.requireNonNull(backend, "backend");
        if (states32 == null) {
            if (((requiredMask | excludedMask) & ~0xff) != 0
                    || requiredMask < 0 || excludedMask < 0) {
                throw new IllegalArgumentException("mask must be within 0..255");
            }
            if (masks != null && masks.length != signals.length) {
                throw new IllegalArgumentException("mask length must match signal length");
            }
        } else if (states32.length != signals.length) {
            throw new IllegalArgumentException("state length must match signal length");
        }
        if (backend == Backend.NATIVE_REQUIRED && !M3SignalOrderNative.available()) {
            throw new UnsatisfiedLinkError("m3signalorder JNI is unavailable");
        }
        int count = 0;
        for (int i = 0; i < signals.length; i++) {
            if (selected(masks, states32, i, requiredMask, excludedMask)) count++;
        }
        if (count == 0) return new M3SignalOrder(EMPTY, 0, 0);
        int[] rows = new int[count];
        long[] keys = new long[count];
        int offset = 0;
        for (int i = 0; i < signals.length; i++) {
            if (selected(masks, states32, i, requiredMask, excludedMask)) {
                rows[offset] = i;
                long key = sortableKey(signals[i]);
                keys[offset++] = ascending ? key : ~key;
            }
        }
        if (backend == Backend.NATIVE_REQUIRED) M3SignalOrderNative.sort(keys, rows);
        else sortJava(keys, rows);
        return new M3SignalOrder(rows, 0, rows.length);
    }

    private static boolean selected(byte[] masks, int[] states32,
            int index, int required, int excluded) {
        if (states32 != null) {
            int state = states32[index];
            return (state & required) == required && (state & excluded) == 0;
        }
        if (masks == null) return true;
        int state = Byte.toUnsignedInt(masks[index]);
        return (state & required) == required && (state & excluded) == 0;
    }

    /** Total order matches Double.compare, including signed zeros and canonicalized NaNs. */
    static long sortableKey(double signal) {
        long bits = Double.doubleToLongBits(signal);
        return bits ^ ((bits >> 63) | Long.MIN_VALUE);
    }

    /** Stable unsigned-key LSD radix; uses caller-local primitive workspaces only. */
    private static void sortJava(long[] keys, int[] rows) {
        if (keys.length < 2) return;
        long[] scratchKeys = new long[keys.length];
        int[] scratchRows = new int[rows.length];
        int[] counts = new int[RADIX];
        long[] sourceKeys = keys, targetKeys = scratchKeys;
        int[] sourceRows = rows, targetRows = scratchRows;
        for (int pass = 0; pass < Long.BYTES; pass++) {
            Arrays.fill(counts, 0);
            int shift = pass << 3;
            for (long key : sourceKeys) counts[(int) ((key >>> shift) & 255L)]++;
            // A uniform byte is an identity pass; retain the current source planes.
            if (counts[(int) ((sourceKeys[0] >>> shift) & 255L)] == sourceKeys.length) continue;
            int start = 0;
            for (int digit = 0; digit < RADIX; digit++) {
                int occurrences = counts[digit];
                counts[digit] = start;
                start += occurrences;
            }
            for (int row = 0; row < sourceKeys.length; row++) {
                int digit = (int) ((sourceKeys[row] >>> shift) & 255L);
                int target = counts[digit]++;
                targetKeys[target] = sourceKeys[row];
                targetRows[target] = sourceRows[row];
            }
            long[] k = sourceKeys; sourceKeys = targetKeys; targetKeys = k;
            int[] r = sourceRows; sourceRows = targetRows; targetRows = r;
        }
        // Skipped identity passes can leave the final order in the scratch planes.
        if (sourceKeys != keys) {
            System.arraycopy(sourceKeys, 0, keys, 0, keys.length);
            System.arraycopy(sourceRows, 0, rows, 0, rows.length);
        }
    }

    /** The scalar projection defines ordering; separation is NOT automatically a domain metric. */
    public static double numericSeparation(double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) return Double.NaN;
        return Math.abs(a - b);
    }

    public static int compare(double a, double b) { return Double.compare(a, b); }
    public int size() { return to - from; }
    public boolean isEmpty() { return to == from; }
    public int rowAt(int index) { return rows[from + Objects.checkIndex(index, size())]; }
    public int[] rows() { return Arrays.copyOfRange(rows, from, to); }

    /** O(1) bounded view over the same immutable row plane, no per-row entry objects. */
    public M3SignalOrder slice(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, size());
        return new M3SignalOrder(rows, from + fromInclusive, from + toExclusive);
    }

    /** Exactly retained index payload; the short-lived radix workspaces are not retained. */
    public long retainedPrimitiveBytes() { return (long) rows.length * Integer.BYTES; }
}
