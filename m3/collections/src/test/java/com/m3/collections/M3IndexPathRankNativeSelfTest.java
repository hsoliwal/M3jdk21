/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/**
 * Strict parity of the new rank/index projection against the existing JNI
 * M3IndexPathMasks selector. Run with -Xcheck:jni and the existing C library.
 * The test refuses if the native backend is unavailable; it never substitutes
 * Java while claiming to have tested JNI.
 */
public final class M3IndexPathRankNativeSelfTest {
    private M3IndexPathRankNativeSelfTest() { }

    public static void main(String[] args) {
        if (!M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("JNI path-mask peer unavailable; parity test cannot pass");
        }
        Random rng = new Random(0x4d334a4e492026L);
        for (int trial = 0; trial < 180; trial++) {
            int length = rng.nextInt(70);
            int capacity = 1 + rng.nextInt(45);
            int[] rows = new int[length];
            int[] positions = new int[length];
            int[][] stages = new int[1 + rng.nextInt(3)][capacity];
            int[] required = new int[stages.length];
            int[] forbidden = new int[stages.length];
            for (int i = 0; i < length; i++) {
                rows[i] = rng.nextInt(17) - 8;
                positions[i] = rng.nextInt(capacity);
            }
            for (int s = 0; s < stages.length; s++) {
                for (int p = 0; p < capacity; p++) stages[s][p] = rng.nextInt();
                required[s] = rng.nextBoolean() ? 0 : 1 << rng.nextInt(32);
                forbidden[s] = rng.nextBoolean() ? 0 : 1 << rng.nextInt(32);
            }
            int count = length == 0 ? 0 : rng.nextInt(length + 1);
            int stride = rng.nextBoolean() ? 1 : -1;
            int first = count == 0 ? length
                    : stride > 0 ? rng.nextInt(length - count + 1)
                    : count - 1 + rng.nextInt(length - count + 1);
            M3IndexPathRank index = M3IndexPathRank.snapshot(rows, positions, stages,
                    required, forbidden, first, count, stride);
            int[] reference = M3IndexPathMasks.select(rows, positions, stages,
                    required, forbidden, first, count, stride, M3IndexPathMasks.Backend.JAVA);
            int[] nativeRows = M3IndexPathMasks.select(rows, positions, stages,
                    required, forbidden, first, count, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            int[] bulkRows = new int[index.size()];
            index.window(0, index.sourceSize()).copySelectedRowIds(
                    0, bulkRows, 0, bulkRows.length);
            if (!Arrays.equals(reference, index.toArray())
                    || !Arrays.equals(reference, bulkRows)
                    || !Arrays.equals(reference, nativeRows)) {
                throw new AssertionError("M3 native rank/path mask disagreement in trial " + trial);
            }
        }
        System.out.println("PASS M3IndexPathRank JNI parity: 180 deterministic Java/rank/native paths");
    }
}
