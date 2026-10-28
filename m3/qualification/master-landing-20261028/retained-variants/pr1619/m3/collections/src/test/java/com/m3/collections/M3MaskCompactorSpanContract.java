/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Actual-owner independent scalar oracle; run with native under -Xcheck:jni. */
public final class M3MaskCompactorSpanContract {
    private M3MaskCompactorSpanContract() { }

    public static void main(String[] arguments) {
        boolean nativeRequired = arguments.length != 0 && arguments[0].equals("native");
        M3IndexMaskCompactor.Backend backend = nativeRequired
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        if (nativeRequired && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("required native owner unavailable");
        }
        Random random = new Random(0x3c0a7L);
        int checks = 0;
        int[] edges = {0, 1, 127, 128, 129, 255, 256, 257, 511, 512, 513, 1025};
        for (int iteration = 0; iteration < 8192; iteration++) {
            int length = iteration < edges.length ? edges[iteration] : random.nextInt(2050);
            int rowOffset = iteration % 9, stateOffset = iteration % 11;
            int[] rows = new int[rowOffset + length + 7];
            long[] longRows = new long[rows.length];
            byte[] states = new byte[stateOffset + length + 13];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = i % 7 == 0 ? Integer.MIN_VALUE : i % 13 - 6;
                longRows[i] = i % 7 == 0 ? Long.MIN_VALUE : (1L << 43) + i % 17;
            }
            random.nextBytes(states);
            int mask = iteration & 255;
            if ((iteration & 7) == 0) {
                Arrays.fill(states, stateOffset, stateOffset + length, (byte)0);
                if (length != 0) states[stateOffset + random.nextInt(length)] = (byte)0xff;
            } else if ((iteration & 7) == 1) {
                Arrays.fill(states, stateOffset, stateOffset + length, (byte)0xff);
            }
            int[] frozenRows = rows.clone();
            long[] frozenLongs = longRows.clone();
            byte[] frozenStates = states.clone();
            int[] expected = new int[length];
            long[] expectedLongs = new long[length];
            int kept = 0;
            for (int i = 0; i < length; i++) {
                if (((states[stateOffset + i] & 255) & mask) == mask) {
                    expected[kept] = rows[rowOffset + i];
                    expectedLongs[kept++] = longRows[rowOffset + i];
                }
            }
            expected = Arrays.copyOf(expected, kept);
            expectedLongs = Arrays.copyOf(expectedLongs, kept);
            int[] actual = M3IndexMaskCompactor.selectWindow(rows, rowOffset, states,
                    stateOffset, length, mask, backend);
            long[] actualLongs = M3IndexMaskCompactor.selectLongWindow(longRows, rowOffset,
                    states, stateOffset, length, mask, backend);
            require(actual != rows && actualLongs != longRows, "detached results", iteration);
            require(Arrays.equals(actual, expected) && Arrays.equals(actualLongs, expectedLongs),
                    "window oracle", iteration);
            byte[] aligned = Arrays.copyOfRange(states, stateOffset, stateOffset + length);
            int[] entire = Arrays.copyOfRange(rows, rowOffset, rowOffset + length);
            require(Arrays.equals(M3IndexMaskCompactor.select(entire, aligned, mask, backend), expected),
                    "whole oracle", iteration);
            if (nativeRequired) {
                require(Arrays.equals(M3IndexMaskCompactor.selectNativeRange(rows, rowOffset,
                        length, aligned, mask), expected), "range oracle", iteration);
            }
            require(Arrays.equals(rows, frozenRows) && Arrays.equals(longRows, frozenLongs)
                    && Arrays.equals(states, frozenStates), "inputs immutable", iteration);
            if (actual.length != 0) actual[0] ^= 1;
            if (actualLongs.length != 0) actualLongs[0] ^= 1;
            require(Arrays.equals(rows, frozenRows) && Arrays.equals(longRows, frozenLongs),
                    "results independent", iteration);
            checks++;
        }
        expect(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(null, 0,
                new byte[0], 0, 0, 0, backend));
        expect(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(new int[2],
                Integer.MAX_VALUE, new byte[2], 0, 1, 0, backend));
        expect(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(new long[2],
                0, new byte[2], 2, Integer.MAX_VALUE, 0, backend));
        expect(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectWindow(new int[0],
                0, new byte[0], 0, 0, 256, backend));
        expect(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectLongWindow(new long[0],
                0, new byte[0], 0, 0, -1, backend));
        System.out.println("COMPACTOR_SCALAR_PASS cases=" + checks + " invalid=5 backend=" + backend);
    }

    private static void require(boolean condition, String label, int iteration) {
        if (!condition) throw new AssertionError(label + " iteration=" + iteration);
    }

    private static void expect(Class<? extends Throwable> expected, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("expected " + expected.getName(), failure);
        }
        throw new AssertionError("missing " + expected.getName());
    }
}
