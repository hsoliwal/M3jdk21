// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Source-owned differential test for optional JNI and legacy ABI fallback. */
public final class M3LongRankUpperBoundsContract {
    private M3LongRankUpperBoundsContract() { }

    public static void main(String[] args) {
        boolean expectNative = args.length != 0 && "--native".equals(args[0]);
        boolean expectLegacy = args.length != 0 && "--legacy".equals(args[0]);
        if (expectNative && !M3LongRankBatchNative.nativeUpperBoundsAvailable())
            throw new AssertionError("required native upper-bound ABI unavailable");
        if (expectLegacy && (!M3LongRankBatchNative.nativeAvailable()
                || M3LongRankBatchNative.nativeUpperBoundsAvailable()))
            throw new AssertionError("legacy lower-bound ABI not correctly detected");
        Random random = new Random(0x4d334c43);
        int cases = 0;
        for (int round = 0; round < 300; round++) {
            int length = round % 7 == 0 ? 0 : random.nextInt(160);
            long[] sorted = new long[length];
            for (int i = 0; i < length; i++) {
                sorted[i] = switch (random.nextInt(11)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(19) - 9L;
                };
            }
            Arrays.sort(sorted);
            int count = round % 2 == 0 ? 1 + random.nextInt(50) : 64 + random.nextInt(130);
            long[] keys = new long[count];
            for (int i = 0; i < count; i++) {
                keys[i] = switch (random.nextInt(12)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(25) - 12L;
                };
            }
            int[] lows = M3LongRankBatchNative.lowerBounds(sorted, keys);
            int[] highs = M3LongRankBatchNative.upperBounds(sorted, keys);
            for (int i = 0; i < keys.length; i++) {
                int low = 0, high = 0;
                for (long value : sorted) {
                    if (value < keys[i]) low++;
                    if (value <= keys[i]) high++;
                }
                if (low != lows[i] || high != highs[i] || low > high)
                    throw new AssertionError("rank mismatch round=" + round + " q=" + i);
                cases++;
            }
        }
        try {
            M3LongRankBatchNative.upperBounds(null, new long[0]);
            throw new AssertionError("expected null rejection");
        } catch (NullPointerException expected) {
            // Null validation is retained even for empty work.
        }
        try {
            M3LongRankBatchNative.upperBounds(new long[0], null);
            throw new AssertionError("expected null rejection");
        } catch (NullPointerException expected) {
            // Null validation is retained even for empty work.
        }
        System.out.println("UPPER_BOUNDS_PASS cases=" + cases + " native="
                + M3LongRankBatchNative.nativeUpperBoundsAvailable());
    }
}
