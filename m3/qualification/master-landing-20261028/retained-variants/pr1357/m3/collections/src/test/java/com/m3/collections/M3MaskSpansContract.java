/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent scalar oracle for span edges, offset guards and resumable bounded windows. */
public final class M3MaskSpansContract {
    private M3MaskSpansContract() { }
    public static void main(String[] args) {
        var backend = args.length == 0 ? M3IndexMaskCompactor.Backend.JAVA
                : M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        if (args.length != 0 && !M3IndexMaskCompactor.nativeAvailable()) throw new AssertionError("JNI unavailable");
        Random random = new Random(0x3a18L);
        int checks = 0;
        for (int iteration = 0; iteration < 12000; iteration++) {
            int length = random.nextInt(600);
            int[] rows = new int[length]; byte[] states = new byte[length];
            for (int i = 0; i < length; i++) {
                rows[i] = random.nextInt();
                states[i] = (byte) (iteration % 2 == 0 ? (i % 128 == iteration % 128 ? 128 : 0) : random.nextInt(256));
            }
            int first = random.nextInt(length + 1), budget = random.nextInt(700);
            int cap = iteration % 131, offset = iteration % 7;
            int required = iteration % 2 == 0 ? 128 : random.nextInt(256);
            int excluded = iteration % 2 == 0 ? 0 : random.nextInt(256);
            int[] actual = new int[offset + cap + 9]; Arrays.fill(actual, 0x3a18);
            int[] expected = actual.clone(), frozenRows = rows.clone(); byte[] frozenStates = states.clone();
            int next = first, count = 0, end = Math.min(length, first + budget);
            while (next < end && count < cap) {
                int flags = states[next] & 255;
                if ((flags & required) == required && (flags & excluded) == 0) expected[offset + count++] = rows[next];
                next++;
            }
            long result = M3IndexMaskWindow.scanBounded(rows, states, first, budget,
                    required, excluded, actual, offset, cap, backend);
            if (result != (((long) next << 32) | count) || !Arrays.equals(actual, expected)
                    || !Arrays.equals(rows, frozenRows) || !Arrays.equals(states, frozenStates)) {
                throw new AssertionError("span oracle seed=0x3a18 iteration=" + iteration);
            }
            checks++;
        }
        System.out.println("MASK_SPANS_PASS cases=" + checks + " backend=" + backend);
    }
}
