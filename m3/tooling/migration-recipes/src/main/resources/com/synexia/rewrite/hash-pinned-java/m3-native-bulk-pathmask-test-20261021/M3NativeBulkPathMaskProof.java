/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/**
 * Exact selected-index differential for both the source-pinned original native
 * JNI implementation and M3JDK21's 256-row contiguous bulk successor.
 */
public final class M3NativeBulkPathMaskProof {
    private static final Random RANDOM = new Random(0x4d334e4154495645L);
    private static final MessageDigest DIGEST = sha256();
    private static int compared;
    private static int contiguous;
    private static int otherStride;

    private M3NativeBulkPathMaskProof() { }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void record(int[] result) {
        int length = result.length;
        DIGEST.update((byte) length);
        DIGEST.update((byte) (length >>> 8));
        DIGEST.update((byte) (length >>> 16));
        DIGEST.update((byte) (length >>> 24));
        for (int value : result) {
            DIGEST.update((byte) value);
            DIGEST.update((byte) (value >>> 8));
            DIGEST.update((byte) (value >>> 16));
            DIGEST.update((byte) (value >>> 24));
        }
    }

    private static void compare(int[] rows, int[] positions, int[][] states,
            int[] required, int[] forbidden, int first, int count, int stride) {
        int[] expected = M3IndexPathMasks.select(rows, positions, states,
                required, forbidden, first, count, stride, M3IndexPathMasks.Backend.JAVA);
        int[] actual = M3IndexPathMasks.select(rows, positions, states,
                required, forbidden, first, count, stride,
                M3IndexPathMasks.Backend.NATIVE_REQUIRED);
        if (!Arrays.equals(expected, actual) || actual == rows) {
            throw new AssertionError("native/Java mismatch: rows=" + rows.length
                    + " stages=" + states.length + " first=" + first
                    + " count=" + count + " stride=" + stride);
        }
        if (rows.length > 0) {
            int saved = rows[0];
            rows[0] = saved ^ 0x55aa55aa;
            if (!Arrays.equals(expected, actual)) {
                throw new AssertionError("detached native result aliased caller row lane");
            }
            rows[0] = saved;
        }
        record(actual);
        compared++;
        if (stride == 1 && count >= 8 && states.length <= 8) contiguous++;
        else otherStride++;
    }

    private static void test(int length, int stages, int pattern) {
        int[] rows = new int[length];
        int[] physical = new int[length];
        int capacity = Math.max(1, length / 3 + 9);
        int[][] state = new int[stages][capacity];
        int[] needs = new int[stages];
        int[] denies = new int[stages];
        for (int i = 0; i < length; i++) {
            rows[i] = i % 9 == 0 ? Integer.MIN_VALUE
                    : i % 7 == 0 ? Integer.MAX_VALUE : i % 5 == 0 ? -1
                    : RANDOM.nextInt(23) - 11;
            physical[i] = RANDOM.nextInt(capacity);
        }
        for (int stage = 0; stage < stages; stage++) {
            for (int i = 0; i < capacity; i++) state[stage][i] = RANDOM.nextInt();
            int bit = 1 << (stage % 32);
            switch (pattern) {
                case 0 -> { needs[stage] = 0; denies[stage] = 0; }
                case 1 -> { needs[stage] = bit; denies[stage] = bit; }
                case 2 -> { needs[stage] = bit; denies[stage] = 0; }
                case 3 -> { needs[stage] = 0; denies[stage] = bit; }
                case 4 -> { needs[stage] = Integer.MIN_VALUE; denies[stage] = 0; }
                case 5 -> { needs[stage] = 0; denies[stage] = Integer.MIN_VALUE; }
                case 6 -> { needs[stage] = RANDOM.nextInt(); denies[stage] = RANDOM.nextInt(); }
                default -> throw new AssertionError("unexpected mask pattern");
            }
        }
        int[] strides = {1, -1, 2, -2, 3, -3};
        for (int stride : strides) {
            int delta = Math.abs(stride);
            int first = stride > 0 ? 0 : Math.max(0, length - 1);
            int count = (length + delta - 1) / delta;
            compare(rows, physical, state, needs, denies, first, count, stride);
            if (length > 3) {
                int start = stride > 0 ? 1 : length - 2;
                int available = stride > 0 ? length - start : start + 1;
                compare(rows, physical, state, needs, denies, start,
                        (available + delta - 1) / delta, stride);
            }
        }
    }

    public static void main(String[] arguments) {
        if (!M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("M3JDK21 native path-mask library is required");
        }
        int[] lengths = {0, 1, 2, 7, 8, 9, 63, 64, 65, 127, 128, 129,
                255, 256, 257, 511, 512, 513, 1023, 1024, 4096};
        int[] stages = {1, 2, 4, 8, 9, 12};
        for (int length : lengths) {
            for (int stage : stages) {
                for (int pattern = 0; pattern < 7; pattern++) {
                    test(length, stage, pattern);
                }
            }
        }
        for (int i = 0; i < 600; i++) {
            test(RANDOM.nextInt(300), 1 + RANDOM.nextInt(12),
                    RANDOM.nextInt(7));
        }
        System.out.println("M3 native bulk mask selection scenarios=" + compared
                + " contiguous=" + contiguous + " fallback=" + otherStride
                + " checksum=" + HexFormat.of().formatHex(DIGEST.digest()));
    }
}
