/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Deterministic Java/JNI parity on bounded row/state windows and chunk seams. */
public final class M3SparseMaskReadContract {
    private M3SparseMaskReadContract() { }
    public static void main(String[] args) {
        if (!M3IndexMaskCompactor.nativeAvailable()) throw new AssertionError("real JNI required");
        Random random = new Random(0x3a57c0deL);
        int checks = 0;
        for (int length : new int[] { 0, 1, 2, 255, 256, 257, 511, 512, 513, 1025 }) {
            for (int mask = 0; mask < 256; mask++) {
                int[] rows = new int[length + 17]; long[] longs = new long[length + 17];
                byte[] states = new byte[length + 23];
                for (int i = 0; i < rows.length; i++) { rows[i] = random.nextInt(); longs[i] = random.nextLong(); }
                random.nextBytes(states);
                for (int shape = 0; shape < 4; shape++) {
                    if (shape != 0) Arrays.fill(states, (byte) (shape == 1 ? 0 : mask));
                    if (shape == 3) {
                        Arrays.fill(states, (byte) 0);
                        if (length > 0) states[11 + (length >>> 1)] = (byte) mask;
                    }
                    int[] rowCopy = rows.clone(); long[] longCopy = longs.clone(); byte[] stateCopy = states.clone();
                    int[] java = M3IndexMaskCompactor.selectWindow(rows, 7, states, 11, length, mask, M3IndexMaskCompactor.Backend.JAVA);
                    int[] nativeRows = M3IndexMaskCompactor.selectWindow(rows, 7, states, 11, length, mask, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    long[] javaLongs = M3IndexMaskCompactor.selectLongWindow(longs, 7, states, 11, length, mask, M3IndexMaskCompactor.Backend.JAVA);
                    long[] nativeLongs = M3IndexMaskCompactor.selectLongWindow(longs, 7, states, 11, length, mask, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    if (!Arrays.equals(java, nativeRows) || !Arrays.equals(javaLongs, nativeLongs)
                            || !Arrays.equals(rows, rowCopy) || !Arrays.equals(longs, longCopy)
                            || !Arrays.equals(states, stateCopy)) throw new AssertionError("window parity " + length + "/" + mask + "/" + shape);
                    int[] direct = Arrays.copyOfRange(rows, 7, 7 + length);
                    byte[] aligned = Arrays.copyOfRange(states, 11, 11 + length);
                    if (!Arrays.equals(java, M3IndexMaskCompactor.select(direct, aligned, mask, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED))) {
                        throw new AssertionError("legacy selector parity");
                    }
                    checks += 6;
                }
            }
        }
        System.out.println("M3_SPARSE_MASK_JNI_PASS checks=" + checks);
    }
}
