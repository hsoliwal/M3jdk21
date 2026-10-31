// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * Independent scalar oracle for ordered int/long row windows against unsigned
 * required/excluded state masks, including JNI parity and physical-offset gaps.
 */
public final class M3RequiredExcludedWindowContract {
    //FLLDCIM: RECIPE - one scalar oracle, stable parent-array order, no API replacement.
    private static long assertions;

    private M3RequiredExcludedWindowContract() { }

    private static void check(boolean condition, String description) {
        ++assertions;
        if (!condition) throw new AssertionError(description + " at check " + assertions);
    }

    private static int[] expectedInt(int[] rows, int rowOffset, byte[] flags,
            int stateOffset, int length, int required, int excluded) {
        int[] answer = new int[length];
        int count = 0;
        for (int i = 0; i < length; i++) {
            int state = Byte.toUnsignedInt(flags[stateOffset + i]);
            if ((state & required) == required && (state & excluded) == 0) {
                answer[count++] = rows[rowOffset + i];
            }
        }
        return Arrays.copyOf(answer, count);
    }

    private static long[] expectedLong(long[] rows, int rowOffset, byte[] flags,
            int stateOffset, int length, int required, int excluded) {
        long[] answer = new long[length];
        int count = 0;
        for (int i = 0; i < length; i++) {
            int state = Byte.toUnsignedInt(flags[stateOffset + i]);
            if ((state & required) == required && (state & excluded) == 0) {
                answer[count++] = rows[rowOffset + i];
            }
        }
        return Arrays.copyOf(answer, count);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable operation) {
        ++assertions;
        try {
            operation.run();
        } catch (Throwable error) {
            if (kind.isInstance(error)) return;
            throw new AssertionError("wrong exception", error);
        }
        throw new AssertionError("missing " + kind.getName());
    }

    private static void contracts(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {999, 42, -7, 42, 11, 998};
        long[] longs = {Long.MIN_VALUE, Long.MAX_VALUE, -1, Long.MAX_VALUE, 5, 0};
        byte[] states = {99, (byte) 0x81, 3, (byte) 0x82, 0, 99, 7};
        int[] selected = M3IndexMaskCompactor.selectWindow(rows, 1, states, 1, 4,
                2, 0x80, backend);
        check(Arrays.equals(selected, new int[] {-7}), "independent offsets and excluded bits");
        long[] kept = M3IndexMaskCompactor.selectLongWindow(longs, 1, states, 1, 4,
                2, 0x80, backend);
        check(Arrays.equals(kept, new long[] {-1L}), "signed long extremes and exclusions");
        check(Arrays.equals(M3IndexMaskCompactor.selectWindow(rows, 0, states, 0,
                0, 0xff, 0xff, backend), new int[0]), "empty contradictory window");
        check(Arrays.equals(M3IndexMaskCompactor.selectWindow(rows, 0, states, 0,
                5, 1, 1, backend), new int[0]), "contradictory masks");
        check(Arrays.equals(M3IndexMaskCompactor.selectLongWindow(longs, 1,
                states, 1, 4, 2, 0, backend),
                M3IndexMaskCompactor.selectLongWindow(longs, 1, states, 1,
                        4, 2, backend)), "old ABI unchanged when excluded is zero");
        int[] isolation = M3IndexMaskCompactor.selectWindow(rows, 0, states, 1, 3,
                0, 0, backend);
        isolation[0] = 123456;
        check(rows[0] == 999, "full-window defensive copy");
    }

    private static void invalidGeometry() {
        int[] rows = {7, 9};
        long[] longs = {7, 9};
        byte[] flags = {1, 2};
        var java = M3IndexMaskCompactor.Backend.JAVA;
        expect(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectWindow(null, 0, flags, 0,
                        1, 1, 2, java));
        expect(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longs, 0, null, 0,
                        1, 1, 2, java));
        expect(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, 0, flags, 0,
                        1, 1, 2, null));
        expect(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, flags, 0,
                        1, 1, 2, java));
        expect(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, 0, flags, 2,
                        1, 1, 2, java));
        expect(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longs, 2, flags, 0,
                        1, 1, 2, java));
        expect(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, 0, flags, 0,
                        1, 256, 1, java));
        expect(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, 0, flags, 0,
                        1, 1, -1, java));
    }

    private static void differential(M3IndexMaskCompactor.Backend backend) {
        Random random = new Random(0x5eed_2026_1021L);
        for (int round = 0; round < 4096; round++) {
            int rowLength = random.nextInt(120);
            int stateLength = random.nextInt(120);
            int[] rows = new int[rowLength];
            long[] longs = new long[rowLength];
            byte[] states = new byte[stateLength];
            for (int i = 0; i < rowLength; i++) {
                rows[i] = i % 7 == 0 ? -13 : i % 5 == 0 ? 7 : random.nextInt();
                longs[i] = i % 7 == 0 ? Long.MIN_VALUE
                        : i % 5 == 0 ? Long.MAX_VALUE : random.nextLong();
            }
            random.nextBytes(states);
            int first = random.nextInt(rowLength + 1);
            int offset = random.nextInt(stateLength + 1);
            int length = random.nextInt(Math.min(rowLength - first,
                    stateLength - offset) + 1);
            int required = random.nextInt(256);
            int excluded = random.nextInt(256);
            if (round % 23 == 0) { required = 0; excluded = 0; }
            if (round % 29 == 0) { required = 0x80; excluded = 0x80; }
            int[] beforeRows = rows.clone();
            long[] beforeLongs = longs.clone();
            byte[] beforeStates = states.clone();
            int[] expected = expectedInt(rows, first, states, offset,
                    length, required, excluded);
            long[] expectedLong = expectedLong(longs, first, states, offset,
                    length, required, excluded);
            int[] actual = M3IndexMaskCompactor.selectWindow(rows, first,
                    states, offset, length, required, excluded, backend);
            long[] actualLong = M3IndexMaskCompactor.selectLongWindow(longs, first,
                    states, offset, length, required, excluded, backend);
            check(Arrays.equals(expected, actual), "int stable index selection round " + round);
            check(Arrays.equals(expectedLong, actualLong), "long stable index selection round " + round);
            check(Arrays.equals(rows, beforeRows) && Arrays.equals(longs, beforeLongs)
                    && Arrays.equals(states, beforeStates), "caller-owned storage unchanged " + round);
        }
    }

    public static long run(M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        long initial = assertions;
        contracts(backend);
        invalidGeometry();
        differential(backend);
        return assertions - initial;
    }

    public static void main(String[] args) {
        long javaChecks = run(M3IndexMaskCompactor.Backend.JAVA);
        if (args.length > 0 && "--native".equals(args[0])) {
            check(M3IndexMaskCompactor.nativeAvailable(), "required JNI library loaded");
            long nativeChecks = run(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            System.out.println("M3_TWO_MASK_WINDOW_PASS java=" + javaChecks
                    + " native=" + nativeChecks);
        } else {
            System.out.println("M3_TWO_MASK_WINDOW_PASS java=" + javaChecks);
        }
    }
}
