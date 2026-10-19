/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.concurrent.ProgressMonitor;
import java.util.Random;
import java.util.SortAlgorithm;
import java.util.Sorts;

/**
 * Real-owner qualification of the existing signal JNI radix kernel and java.util.Sorts.
 * This source belongs to the sorting verification scope: it requires the actual java.base
 * sorting patch, and is not a replacement collection owner or a fixture implementation.
 */
public final class M3SortsNativeParityContract {
    private static final Random RANDOM = new Random(0x4d334a4e494c5344L);
    private static final long[] SPECIAL_BITS = {
        0L, Long.MIN_VALUE, 0x3ff0000000000000L, 0xbff0000000000000L,
        1L, 0x8000000000000001L, 0x000fffffffffffffL, 0x800fffffffffffffL,
        0x7fefffffffffffffL, 0xffefffffffffffffL,
        0x7ff0000000000000L, 0xfff0000000000000L,
        0x7ff0000000000001L, 0x7ff8000000000001L,
        0xfff0000000000001L, 0xfff8000000000042L
    };
    private static long checks;
    private static long cases;

    private M3SortsNativeParityContract() { }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("missing")) {
            missingBackend();
            System.out.println("PASS M3SortsNativeParityContract mode=missing checks=" + checks);
            return;
        }
        if (args.length != 1 || !args[0].equals("native")) {
            throw new IllegalArgumentException("expected native or missing");
        }
        check(M3SignalOrderNative.available(), "actual m3signalorder JNI is required");
        signalBoundaries();
        for (int round = 0; round < 512; round++) {
            signalCase(RANDOM.nextInt(522), round % 5);
        }
        unsignedKeyBoundaries();
        for (int round = 0; round < 512; round++) {
            unsignedKeyCase(RANDOM.nextInt(522), round % 5);
        }
        nativeArgumentFailures();
        explicitNoMonitorInterrupt();
        System.out.println("PASS M3SortsNativeParityContract mode=native cases=" + cases
                + " checks=" + checks + " realJNI=true sorts=LSD_RADIX");
    }

    private static void signalBoundaries() {
        int[] sizes = {0, 1, 2, 3, 31, 32, 33, 127, 128, 129, 255, 256, 257,
                1023, 1024, 1025, 4095, 4096, 4097, 8193};
        for (int size : sizes) {
            for (int pattern = 0; pattern < 5; pattern++) signalCase(size, pattern);
        }
    }

    private static void signalCase(int size, int pattern) {
        // Native signal order has a full-source mask API. Guard rows outside this
        // nonzero sorting range are rejected by the required high bit.
        int from = 3;
        int to = from + size;
        double[] signals = new double[size + 7];
        byte[] masks = new byte[signals.length];
        for (int i = 0; i < signals.length; i++) {
            long bits = switch (pattern) {
                case 0 -> SPECIAL_BITS[i % SPECIAL_BITS.length];
                case 1 -> Double.doubleToRawLongBits((double) (i % 7 - 3));
                case 2 -> 0xfff8000000000000L | i;
                case 3 -> Double.doubleToRawLongBits((double) (signals.length - i));
                default -> RANDOM.nextLong();
            };
            signals[i] = Double.longBitsToDouble(bits);
            if (i >= from && i < to) {
                masks[i] = (byte) (0x80 | (pattern == 3 ? 0 : RANDOM.nextInt(128)));
            }
        }
        long[] beforeBits = rawBits(signals);
        byte[] beforeMasks = masks.clone();
        int[][] maskPairs = {{0x80, 0}, {0x81, 0x02}, {0x85, 0x40}, {0x80, 0x80}};
        for (int[] pair : maskPairs) {
            int required = pair[0];
            int excluded = pair[1];
            for (boolean ascending : new boolean[] {true, false}) {
                int[] oracle = signalOracle(signals, masks, from, to,
                        required, excluded, ascending);
                M3SignalOrder nativeOrder = M3SignalOrder.filtered(signals, masks,
                        required, excluded, ascending, M3SignalOrder.Backend.NATIVE_REQUIRED);
                M3SignalOrder javaOrder = M3SignalOrder.filtered(signals, masks,
                        required, excluded, ascending, M3SignalOrder.Backend.JAVA);
                equal(oracle, nativeOrder.rows(), "native signal order");
                equal(oracle, javaOrder.rows(), "scalar signal order");
                int[] keyCalls = new int[signals.length];
                int[] keyed = Sorts.orderByLong(from, to, index -> {
                    keyCalls[index]++;
                    long signed = M3SignalOrder.sortableKey(signals[index]) ^ Long.MIN_VALUE;
                    return ascending ? signed : ~signed;
                }, SortAlgorithm.LSD_RADIX, ProgressMonitor.NONE);
                equal(oracle, select(keyed, masks, required, excluded),
                        "unsigned JNI keys versus signed Sorts keys");
                for (int i = 0; i < keyCalls.length; i++) {
                    check(keyCalls[i] == (i >= from && i < to ? 1 : 0),
                            "one key callback per admitted coordinate");
                }
                if (ascending) {
                    int[] primitive = Sorts.order(signals, from, to,
                            SortAlgorithm.LSD_RADIX, ProgressMonitor.NONE);
                    equal(oracle, select(primitive, masks, required, excluded),
                            "primitive double Sorts total order");
                }
                immutableSlice(nativeOrder, oracle);
                cases++;
            }
        }
        equal(beforeBits, rawBits(signals), "signal and NaN payload input preservation");
        check(Arrays.equals(beforeMasks, masks), "input masks remain unchanged");
        // A direct payload sort must preserve the exact NaN payload occurrence order.
        double[] copied = signals.clone();
        int[] fullOracle = signalOracle(signals, masks, from, to, 0x80, 0, true);
        Sorts.sort(copied, from, to, SortAlgorithm.LSD_RADIX, ProgressMonitor.NONE);
        for (int i = 0; i < copied.length; i++) {
            long expected = i >= from && i < to ? beforeBits[fullOracle[i - from]] : beforeBits[i];
            check(Double.doubleToRawLongBits(copied[i]) == expected,
                    "stable raw-payload commit and range guards");
        }
    }

    private static void immutableSlice(M3SignalOrder order, int[] expected) {
        int first = expected.length / 3;
        int last = expected.length - expected.length / 4;
        M3SignalOrder slice = order.slice(first, last);
        equal(Arrays.copyOfRange(expected, first, last), slice.rows(), "nonzero immutable slice");
        check(slice.retainedPrimitiveBytes() == order.retainedPrimitiveBytes(),
                "slice retains the same immutable index directory");
        int[] escaped = order.rows();
        if (escaped.length > 0) {
            escaped[0] ^= -1;
            check(order.rowAt(0) == expected[0], "row export is detached");
        }
    }

    private static int[] signalOracle(double[] signals, byte[] masks, int from, int to,
            int required, int excluded, boolean ascending) {
        Integer[] rows = new Integer[to - from];
        int count = 0;
        for (int i = from; i < to; i++) {
            int state = Byte.toUnsignedInt(masks[i]);
            if ((state & required) == required && (state & excluded) == 0) rows[count++] = i;
        }
        rows = Arrays.copyOf(rows, count);
        Arrays.sort(rows, (left, right) -> {
            int comparison = ascending ? Double.compare(signals[left], signals[right])
                    : Double.compare(signals[right], signals[left]);
            return comparison != 0 ? comparison : Integer.compare(left, right);
        });
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = rows[i];
        return result;
    }

    private static int[] select(int[] sorted, byte[] masks, int required, int excluded) {
        int[] result = new int[sorted.length];
        int count = 0;
        for (int index : sorted) {
            int state = Byte.toUnsignedInt(masks[index]);
            if ((state & required) == required && (state & excluded) == 0) result[count++] = index;
        }
        return Arrays.copyOf(result, count);
    }

    private static void unsignedKeyBoundaries() {
        int[] sizes = {0, 1, 2, 3, 127, 128, 129, 255, 256, 257, 4095, 4096, 4097};
        for (int size : sizes) {
            for (int pattern = 0; pattern < 5; pattern++) unsignedKeyCase(size, pattern);
        }
    }

    private static void unsignedKeyCase(int size, int pattern) {
        long[] keys = new long[size];
        int[] rows = new int[size];
        for (int i = 0; i < size; i++) {
            keys[i] = switch (pattern) {
                case 0 -> SPECIAL_BITS[i % SPECIAL_BITS.length];
                case 1 -> i % 5;
                case 2 -> -1L;
                case 3 -> Long.MIN_VALUE + size - i;
                default -> RANDOM.nextLong();
            };
            // The existing native kernel carries opaque signed labels, including duplicates.
            rows[i] = pattern == 1 ? i % 3 - 1 : Integer.MIN_VALUE + i;
        }
        long[] originalKeys = keys.clone();
        int[] originalRows = rows.clone();
        Integer[] boxed = new Integer[size];
        for (int i = 0; i < size; i++) boxed[i] = i;
        Arrays.sort(boxed, (left, right) -> {
            int comparison = Long.compareUnsigned(originalKeys[left], originalKeys[right]);
            return comparison != 0 ? comparison : Integer.compare(left, right);
        });
        int[] sorts = Sorts.orderByLong(0, size, index -> originalKeys[index] ^ Long.MIN_VALUE,
                SortAlgorithm.LSD_RADIX, ProgressMonitor.NONE);
        M3SignalOrderNative.sort(keys, rows);
        for (int i = 0; i < size; i++) {
            check(sorts[i] == boxed[i], "signed key adaptation matches unsigned oracle");
            check(keys[i] == originalKeys[boxed[i]], "native unsigned key order");
            check(rows[i] == originalRows[boxed[i]], "stable opaque native row labels");
        }
        cases++;
    }

    private static void nativeArgumentFailures() {
        long[] keys = {Long.MIN_VALUE, 0, -1};
        int[] rows = {7, 7, -9};
        long[] keysBefore = keys.clone();
        int[] rowsBefore = rows.clone();
        reject(NullPointerException.class, () -> M3SignalOrderNative.sort(null, rows));
        reject(NullPointerException.class, () -> M3SignalOrderNative.sort(keys, null));
        reject(IllegalArgumentException.class, () -> M3SignalOrderNative.sort(keys, new int[1]));
        reject(IllegalArgumentException.class, () -> M3SignalOrderNative.sort(new long[0], rows));
        equal(keysBefore, keys, "native admission failure preserves keys");
        equal(rowsBefore, rows, "native admission failure preserves row labels");
    }

    private static void explicitNoMonitorInterrupt() {
        double[] signals = {Double.NaN, -0.0, 0.0, -1, 1};
        byte[] masks = new byte[signals.length];
        Thread.currentThread().interrupt();
        try {
            int[] nativeRows = M3SignalOrder.filtered(signals, masks, 0, 0, true,
                    M3SignalOrder.Backend.NATIVE_REQUIRED).rows();
            int[] javaRows = Sorts.order(signals, 0, signals.length,
                    SortAlgorithm.LSD_RADIX, ProgressMonitor.NONE);
            equal(nativeRows, javaRows, "existing non-monitor JNI and explicit NONE order parity");
            check(Thread.currentThread().isInterrupted(), "neither owner clears interruption");
        } finally {
            Thread.interrupted();
        }
    }

    private static void missingBackend() {
        check(!M3SignalOrderNative.available(), "separate JVM must have no signal native library");
        for (int size : new int[] {0, 1, 3}) {
            double[] values = new double[size];
            byte[] masks = new byte[size];
            reject(UnsatisfiedLinkError.class, () -> M3SignalOrder.filtered(values, masks,
                    0, 0, true, M3SignalOrder.Backend.NATIVE_REQUIRED));
            reject(UnsatisfiedLinkError.class, () -> M3SignalOrder.filtered(values, masks,
                    1, 1, true, M3SignalOrder.Backend.NATIVE_REQUIRED));
            check(M3SignalOrder.filtered(values, masks, 0, 0, true,
                    M3SignalOrder.Backend.JAVA).size() == size, "explicit scalar remains usable");
        }
    }

    private static long[] rawBits(double[] values) {
        long[] result = new long[values.length];
        for (int i = 0; i < values.length; i++) result[i] = Double.doubleToRawLongBits(values[i]);
        return result;
    }

    private static void equal(int[] expected, int[] actual, String reason) {
        check(Arrays.equals(expected, actual), reason);
    }

    private static void equal(long[] expected, long[] actual, String reason) {
        check(Arrays.equals(expected, actual), reason);
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void reject(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            check(type.isInstance(error), "expected " + type.getName() + ", got " + error);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }
}
