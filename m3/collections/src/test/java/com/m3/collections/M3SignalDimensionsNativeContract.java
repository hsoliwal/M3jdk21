/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Actual JNI differential parity using the existing M3 stable unsigned radix symbol. */
public final class M3SignalDimensionsNativeContract {
    private M3SignalDimensionsNativeContract() { }
    public static void main(String[] args) {
        if (!M3SignalOrderNative.available()) throw new AssertionError("expected JNI library");
        Random random = new Random(0x17f35ac0L);
        int checks = 0;
        for (int round = 0; round < 200; round++) {
            int n = (round * 23) % 513;
            int dims = 1 + round % 4;
            double[][] columns = new double[dims][n];
            boolean[] descending = new boolean[dims];
            for (int d = 0; d < dims; d++) {
                descending[d] = random.nextBoolean();
                for (int i = 0; i < n; i++) {
                    columns[d][i] = Double.longBitsToDouble(random.nextLong());
                    if (i % 11 == 0) columns[d][i] = -0.0;
                    if (i % 19 == 0) columns[d][i] = Double.NaN;
                }
            }
            byte[] states = new byte[n];
            random.nextBytes(states);
            int[] javaRows = M3SignalDimensions.filtered(columns, descending, states, 0x4, 0x80,
                    M3SignalOrder.Backend.JAVA);
            int[] nativeRows = M3SignalDimensions.filtered(columns, descending, states, 0x4, 0x80,
                    M3SignalOrder.Backend.NATIVE_REQUIRED);
            if (!Arrays.equals(javaRows, nativeRows)) throw new AssertionError("JNI mismatch round " + round);
            checks++;
            int[] j2 = M3SignalDimensions.order(columns, descending, M3SignalOrder.Backend.JAVA);
            int[] n2 = M3SignalDimensions.order(columns, descending,
                    M3SignalOrder.Backend.NATIVE_REQUIRED);
            if (!Arrays.equals(j2, n2)) throw new AssertionError("JNI full mismatch round " + round);
            checks++;
        }
        System.out.println("PASS M3SignalDimensionsNativeContract JNI checks=" + checks);
    }
}
