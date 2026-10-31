/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Differential Java21/C17 JNI oracle for independently offset primitive mask windows. */
public final class M3IndexMaskWindowRecoverySelfTest {
    private static long checks;
    private M3IndexMaskWindowRecoverySelfTest() { }

    public static void main(String[] args) {
        boolean nativeRun = args.length == 1 && args[0].equals("--native");
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("JNI path was required but did not load");
        }
        boundaries(nativeRun);
        randomized(nativeRun);
        System.out.println("PASS M3IndexMaskWindowRecoverySelfTest checks=" + checks + " JNI=" + nativeRun);
    }

    private static void boundaries(boolean nativeRun) {
        int[] rows = {Integer.MIN_VALUE, 7, 7, -5, Integer.MAX_VALUE};
        long[] longs = {Long.MIN_VALUE, 0, 0, 11, Long.MAX_VALUE};
        byte[] states = {0, 1, (byte) 0x80, (byte) 0xff, 7, 0};
        var backend = M3IndexMaskCompactor.Backend.JAVA;
        equal(new int[] {7, 7}, M3IndexMaskCompactor.selectWindow(rows, 1, states, 2, 3, 128, backend));
        equal(new long[] {0, 0}, M3IndexMaskCompactor.selectLongWindow(longs, 1, states, 2, 3, 128, backend));
        int[] all = M3IndexMaskCompactor.selectWindow(rows, 0, states, 1, 5, 0, backend);
        equal(rows, all);
        ok(rows != all);
        all[0] = 100;
        ok(rows[0] == Integer.MIN_VALUE);
        long[] allLong = M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 1, 5, 0, backend);
        equal(longs, allLong);
        ok(longs != allLong);
        equal(new int[0], M3IndexMaskCompactor.selectWindow(rows, 5, states, 6, 0, 255, backend));
        equal(new long[0], M3IndexMaskCompactor.selectLongWindow(longs, 5, states, 6, 0, 255, backend));
        // Existing whole-array all-pass fast paths must remain unchanged.
        byte[] zero = new byte[rows.length];
        int[] full = M3IndexMaskCompactor.select(rows, zero, 0, backend);
        equal(rows, full);
        ok(full != rows);
        long[] fullLong = M3IndexMaskCompactor.selectLong(longs, zero, 0);
        equal(longs, fullLong);
        ok(fullLong != longs);
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(null, 0, states, 0, 0, 0, backend));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(rows, 0, null, 0, 0, 0, backend));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 0, 0, 0, null));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectLongWindow(null, 0, states, 0, 0, 0, backend));
        fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(rows, -1, states, 0, 0, 0, backend));
        fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(rows, 2, states, 0, 4, 0, backend));
        fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 4, 3, 0, backend));
        fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(longs, Integer.MAX_VALUE, states, 0, 1, 0, backend));
        fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 0, -1, 0, backend));
        fails(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 0, 0, -1, backend));
        fails(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 0, 0, 256, backend));
        if (nativeRun) {
            var n = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
            equal(new int[] {7, 7}, M3IndexMaskCompactor.selectWindow(rows, 1, states, 2, 3, 128, n));
            equal(new long[] {0, 0}, M3IndexMaskCompactor.selectLongWindow(longs, 1, states, 2, 3, 128, n));
            equal(rows, M3IndexMaskCompactor.selectWindow(rows, 0, states, 1, 5, 0, n));
            equal(longs, M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 1, 5, 0, n));
        } else if (!M3IndexMaskCompactor.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 0, 0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            fails(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, states, 0, 0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }

    private static void randomized(boolean nativeRun) {
        Random rnd = new Random(0x7A3B14C9L);
        for (int pass = 0; pass < 1500; pass++) {
            int length = switch (pass % 12) {
                case 0 -> 0; case 1 -> 1; case 2 -> 2; case 3 -> 7;
                case 4 -> 255; case 5 -> 256; case 6 -> 257;
                case 7 -> 511; case 8 -> 512; case 9 -> 513;
                default -> rnd.nextInt(200);
            };
            int ro = rnd.nextInt(8), so = rnd.nextInt(8);
            int[] rows = new int[ro + length + rnd.nextInt(8)];
            long[] longs = new long[rows.length];
            byte[] states = new byte[so + length + rnd.nextInt(8)];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = i % 7 == 0 ? Integer.MIN_VALUE : i % 7 == 1 ? Integer.MAX_VALUE : rnd.nextInt(11) - 5;
                longs[i] = i % 7 == 0 ? Long.MIN_VALUE : i % 7 == 1 ? Long.MAX_VALUE : rnd.nextLong();
            }
            rnd.nextBytes(states);
            int mask = switch (pass % 11) { case 0 -> 0; case 1 -> 255; case 2 -> 128; default -> rnd.nextInt(256); };
            int[] beforeRows = rows.clone();
            long[] beforeLongs = longs.clone();
            byte[] beforeState = states.clone();
            int[] expectedInt = oracleInt(rows, ro, states, so, length, mask);
            long[] expectedLong = oracleLong(longs, ro, states, so, length, mask);
            equal(expectedInt, M3IndexMaskCompactor.selectWindow(rows, ro, states, so, length, mask,
                    M3IndexMaskCompactor.Backend.JAVA));
            equal(expectedLong, M3IndexMaskCompactor.selectLongWindow(longs, ro, states, so, length, mask,
                    M3IndexMaskCompactor.Backend.JAVA));
            if (nativeRun) {
                equal(expectedInt, M3IndexMaskCompactor.selectWindow(rows, ro, states, so, length, mask,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                equal(expectedLong, M3IndexMaskCompactor.selectLongWindow(longs, ro, states, so, length, mask,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
            equal(beforeRows, rows);
            equal(beforeLongs, longs);
            equal(beforeState, states);
        }
    }

    private static int[] oracleInt(int[] rows, int ro, byte[] states, int so, int length, int mask) {
        int[] out = new int[length]; int count = 0;
        for (int i = 0; i < length; i++) if (((states[so + i] & 255) & mask) == mask) out[count++] = rows[ro + i];
        return Arrays.copyOf(out, count);
    }

    private static long[] oracleLong(long[] rows, int ro, byte[] states, int so, int length, int mask) {
        long[] out = new long[length]; int count = 0;
        for (int i = 0; i < length; i++) if (((states[so + i] & 255) & mask) == mask) out[count++] = rows[ro + i];
        return Arrays.copyOf(out, count);
    }

    private static void ok(boolean condition) { checks++; if (!condition) throw new AssertionError("check " + checks); }
    private static void equal(int[] expected, int[] actual) {
        checks++; if (!Arrays.equals(expected, actual)) throw new AssertionError("int " + checks + " " + Arrays.toString(expected) + " != " + Arrays.toString(actual));
    }
    private static void equal(long[] expected, long[] actual) {
        checks++; if (!Arrays.equals(expected, actual)) throw new AssertionError("long " + checks);
    }
    private static void equal(byte[] expected, byte[] actual) {
        checks++; if (!Arrays.equals(expected, actual)) throw new AssertionError("bytes " + checks);
    }
    private static void fails(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong failure " + checks, failure);
        }
        throw new AssertionError("did not refuse " + type + " check " + checks);
    }
}
