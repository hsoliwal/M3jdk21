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
import java.util.Random;

/** Exact Java 21 contract oracle for index/offset/full-width state-mask selection. */
public final class M3IndexPathMaskClassificationProof {
    private static int scenarios;
    private static int nativeComparisons;

    private M3IndexPathMaskClassificationProof() { }

    private static boolean passes(int[][] states, int[] required, int[] forbidden, int slot) {
        for (int stage = 0; stage < states.length; stage++) {
            int state = states[stage][slot];
            if ((state & required[stage]) != required[stage]
                    || (state & forbidden[stage]) != 0) return false;
        }
        return true;
    }

    private static int[] reference(int[] rows, int[] offsets, int[][] states,
            int[] required, int[] forbidden, int first, int count, int stride) {
        int[] output = new int[count];
        int used = 0;
        long at = first;
        for (int index = 0; index < count; index++, at += stride) {
            if (passes(states, required, forbidden, offsets[(int) at])) {
                output[used++] = rows[(int) at];
            }
        }
        return Arrays.copyOf(output, used);
    }

    private static void verify(int length, int stages, int pattern, int stride, long seed) {
        Random random = new Random(seed);
        int capacity = Math.max(1, length / 3 + 5);
        int[] rows = new int[length];
        int[] offsets = new int[length];
        int[][] states = new int[stages][capacity];
        int[] required = new int[stages];
        int[] forbidden = new int[stages];
        for (int index = 0; index < length; index++) {
            rows[index] = index % 7 == 0 ? Integer.MIN_VALUE
                    : index % 5 == 0 ? 0 : random.nextInt(13) - 6;
            offsets[index] = random.nextInt(capacity);
        }
        for (int stage = 0; stage < stages; stage++) {
            for (int slot = 0; slot < capacity; slot++) states[stage][slot] = random.nextInt();
            int bit = 1 << (stage & 31);
            switch (pattern) {
                case 0 -> { required[stage] = 0; forbidden[stage] = 0; }
                case 1 -> { required[stage] = bit; forbidden[stage] = bit; }
                case 2 -> { required[stage] = 0; forbidden[stage] = bit; }
                case 3 -> { required[stage] = bit; forbidden[stage] = bit << 1; }
                case 4 -> { required[stage] = Integer.MIN_VALUE; forbidden[stage] = 0; }
                case 5 -> { required[stage] = 0; forbidden[stage] = Integer.MIN_VALUE; }
                case 6 -> { required[stage] = random.nextInt(); forbidden[stage] = random.nextInt(); }
                case 7 -> { required[stage] = stage == stages - 1 ? bit : 0; forbidden[stage] = 0; }
                case 8 -> { required[stage] = 0; forbidden[stage] = stage == stages - 1 ? bit : 0; }
                default -> throw new AssertionError(pattern);
            }
        }
        int first = stride < 0 ? Math.max(0, length - 1) : 0;
        int count = (length + Math.abs(stride) - 1) / Math.abs(stride);
        int[] expected = reference(rows, offsets, states, required, forbidden, first, count, stride);
        int[] selected = M3IndexPathMasks.select(rows, offsets, states, required,
                forbidden, first, count, stride, M3IndexPathMasks.Backend.JAVA);
        if (!Arrays.equals(expected, selected) || selected == rows) {
            throw new AssertionError("selection mismatch length=" + length + " stages=" + stages
                    + " mode=" + pattern + " stride=" + stride);
        }
        if (M3IndexPathMasks.nativeAvailable()
                && (scenarios % 11 == 0 || (pattern < 2 && stages <= 8 && stride == 1))) {
            int[] nativeRows = M3IndexPathMasks.select(rows, offsets, states, required,
                    forbidden, first, count, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            if (!Arrays.equals(expected, nativeRows)) {
                throw new AssertionError("Java/JNI mismatch: " + length + "/" + stages);
            }
            nativeComparisons++;
        }
        int expectedFirst = -1;
        long at = first;
        for (int index = 0; index < count; index++, at += stride) {
            if (passes(states, required, forbidden, offsets[(int) at])) {
                expectedFirst = (int) at;
                break;
            }
        }
        if (expectedFirst != M3IndexPathMasks.firstMatchingIndex(rows, offsets, states,
                required, forbidden, first, count, stride)) {
            throw new AssertionError("first matching logical index mismatch");
        }
        if (rows.length > 0) {
            int old = rows[0];
            rows[0] ^= 0x5a5a5a5a;
            if (!Arrays.equals(expected, selected)) throw new AssertionError("source alias");
            rows[0] = old;
        }
        scenarios++;
    }

    private static void fails(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("wrong failure kind", actual);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }

    private static void validation() {
        int[] rows = {5, 5}, offsets = {0, 99};
        int[][] planes = {{Integer.MIN_VALUE}};
        int[] zero = {0}, one = {1};
        // A zero or contradictory mask must never bypass validation.
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                offsets, planes, zero, zero, 0, 2, 1, M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                offsets, planes, one, one, 0, 2, 1, M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                offsets, planes, zero, zero, 0, 3, 1, M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows,
                offsets, planes, zero, zero, 0, 1, 0, M3IndexPathMasks.Backend.JAVA));
        fails(NullPointerException.class, () -> M3IndexPathMasks.select(rows,
                offsets, new int[][]{null}, zero, zero, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        fails(IllegalArgumentException.class, () -> M3IndexPathMasks.select(rows,
                offsets, planes, new int[0], zero, 0, 1, 1,
                M3IndexPathMasks.Backend.JAVA));
        int[] goodOffsets = {0, 0};
        if (!M3IndexPathMasks.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, () -> M3IndexPathMasks.select(rows,
                    goodOffsets, planes, zero, zero, 0, 2, 1,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }

    public static void main(String[] args) {
        validation();
        int[] lengths = {0, 1, 2, 7, 8, 31, 63, 64, 65, 127, 256, 1024};
        int[] stageCounts = {1, 2, 4, 8, 9, 12};
        int[] strides = {1, -1, 3, -3};
        for (int length : lengths) {
            for (int stages : stageCounts) {
                for (int pattern = 0; pattern < 9; pattern++) {
                    for (int stride : strides) {
                        verify(length, stages, pattern, stride,
                                0x21D3C0DEL + 17L * length + 31L * stages + 13L * pattern
                                        + 97L * stride);
                    }
                }
            }
        }
        Random random = new Random(0x5eed21L);
        for (int index = 0; index < 500; index++) {
            int stride = strides[random.nextInt(strides.length)];
            verify(random.nextInt(2048), 1 + random.nextInt(12),
                    random.nextInt(9), stride, random.nextLong());
        }
        System.out.println("M3 index path full-width mask comparisons=" + scenarios
                + " JNI parity=" + nativeComparisons);
    }
}
