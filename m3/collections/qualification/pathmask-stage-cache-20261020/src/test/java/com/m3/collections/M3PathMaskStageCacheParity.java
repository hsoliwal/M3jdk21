/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Standalone Java21/native parity of the M3JDK21 original index-path-mask owner. */
public final class M3PathMaskStageCacheParity {
    private static int cases;
    private static int cached;
    private static int fallback;

    private static void equal(int[] rows, int[] offsets, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        int[] java = M3IndexPathMasks.select(rows, offsets, planes, required, forbidden,
                first, count, stride, M3IndexPathMasks.Backend.JAVA);
        int[] nativeRows = M3IndexPathMasks.select(rows, offsets, planes, required, forbidden,
                first, count, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
        if (!Arrays.equals(java, nativeRows)) {
            throw new AssertionError("JNI path mismatch: stages=" + planes.length
                    + ", first=" + first + ", count=" + count + ", stride=" + stride
                    + ", java=" + Arrays.toString(java) + ", native=" + Arrays.toString(nativeRows));
        }
        cases++;
        if (planes.length <= 8 && count >= 8) cached++; else fallback++;
    }

    private static void failure(Runnable operation, Class<? extends Throwable> kind) {
        try { operation.run(); }
        catch (Throwable ex) {
            if (kind.isInstance(ex)) return;
            throw new AssertionError("wrong exception", ex);
        }
        throw new AssertionError("expected " + kind.getSimpleName());
    }

    public static void main(String[] args) {
        if (!M3IndexPathMasks.nativeAvailable()) throw new AssertionError("JNI binary not loaded");
        Random random = new Random(0x4d334a444b3231L);
        int[] lengths = {0, 1, 2, 63, 64, 65, 127, 128, 256, 513, 1024, 4096};
        int[] stageCounts = {1, 2, 3, 4, 7, 8, 9, 10, 12};
        for (int length : lengths) {
            for (int stages : stageCounts) {
                int[] rows = new int[length];
                int[] offsets = new int[length];
                int capacity = Math.max(1, length / 2 + 7);
                int[][] planes = new int[stages][capacity];
                int[] required = new int[stages];
                int[] forbidden = new int[stages];
                for (int i = 0; i < length; i++) {
                    rows[i] = i % 5 == 0 ? Integer.MIN_VALUE : (i % 7 == 0 ? -7 : random.nextInt(17));
                    offsets[i] = random.nextInt(capacity);
                }
                for (int j = 0; j < stages; j++) {
                    for (int i = 0; i < capacity; i++) planes[j][i] = random.nextInt();
                    int bit = 1 << (j % 32);
                    required[j] = bit;
                    forbidden[j] = (j % 3 == 0) ? 0 : (bit << 1);
                }
                int[][] maskPatterns = {required.clone(), new int[stages]};
                for (int pattern = 0; pattern < 5; pattern++) {
                    if (pattern == 1) Arrays.fill(required, 0);
                    if (pattern == 2) Arrays.fill(required, -1);
                    if (pattern == 3) Arrays.fill(forbidden, -1);
                    if (pattern == 4) {
                        System.arraycopy(maskPatterns[0], 0, required, 0, stages);
                        System.arraycopy(maskPatterns[1], 0, forbidden, 0, stages);
                    }
                    equal(rows, offsets, planes, required, forbidden, 0, length, 1);
                    equal(rows, offsets, planes, required, forbidden,
                            length == 0 ? 0 : length - 1, length, -1);
                    equal(rows, offsets, planes, required, forbidden, 0, (length + 1) / 2, 2);
                }
            }
        }
        for (int seed = 0; seed < 500; seed++) {
            int length = random.nextInt(2000);
            int stages = 1 + random.nextInt(12);
            int[] rows = new int[length], offsets = new int[length];
            int capacity = 1 + random.nextInt(1000);
            int[][] planes = new int[stages][capacity];
            int[] req = new int[stages], forbid = new int[stages];
            for (int i = 0; i < length; i++) {
                rows[i] = random.nextInt();
                offsets[i] = random.nextInt(capacity);
            }
            for (int j = 0; j < stages; j++) {
                for (int i = 0; i < capacity; i++) planes[j][i] = random.nextInt();
                req[j] = random.nextInt();
                forbid[j] = random.nextInt();
            }
            int stride = (seed % 4 == 0 ? 3 : seed % 4 == 1 ? -3 : seed % 4 == 2 ? 1 : -1);
            int first = stride < 0 ? Math.max(0, length - 1) : 0;
            int count = stride < 0 ? (length + Math.abs(stride) - 1) / Math.abs(stride)
                    : (length + stride - 1) / stride;
            equal(rows, offsets, planes, req, forbid, first, count, stride);
        }
        int[] rows = {1}, offsets = {0};
        int[][] planes = {{3}};
        int[] req = {1}, forbid = {2};
        failure(() -> M3IndexPathMasks.select(rows, offsets, planes, req, forbid,
                0, 2, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED), IndexOutOfBoundsException.class);
        failure(() -> M3IndexPathMasks.select(rows, new int[]{1}, planes, req, forbid,
                0, 1, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED), IndexOutOfBoundsException.class);
        failure(() -> M3IndexPathMasks.select(rows, offsets, new int[][]{null}, req, forbid,
                0, 1, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED), NullPointerException.class);
        System.out.println("M3 path-mask Java/JNI parity=" + cases
                + " cached=" + cached + " fallback=" + fallback);
    }
}
