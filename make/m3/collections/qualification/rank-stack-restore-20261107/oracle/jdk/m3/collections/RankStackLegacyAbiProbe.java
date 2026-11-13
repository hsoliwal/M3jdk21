// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/**
 * Probe of the legacy equalRanges0 export (leaves 34 and 55). Since 2b78514275f the Java owner routes equal ranges through
 * equalRangesWindow0 and keeps equalRanges0 only for older consumers, so no contract reaches it: this probe calls it
 * reflectively (library opted in) and compares every [lower, upper) pair with the Java oracle over seeded snapshots,
 * sorted and unsorted query lanes, sizes across the 256-query page. Fails unless the library is loaded.
 * usage: java -Dm3.collections.rank.native=true -Djava.library.path=LIB RankStackLegacyAbiProbe ROUNDS
 */
public final class RankStackLegacyAbiProbe {
    private RankStackLegacyAbiProbe() { }

    private static int bound(long[] sorted, long key, boolean upper) {
        int low = 0;
        int high = sorted.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (upper ? sorted[mid] <= key : sorted[mid] < key) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    public static void main(String[] args) throws Exception {
        if (!M3LongRankBatchNative.nativeAvailable()) throw new AssertionError("m3rankbatch not loaded");
        Method legacy = M3LongRankBatchNative.class.getDeclaredMethod("equalRanges0", long[].class, long[].class, int[].class);
        legacy.setAccessible(true);
        Random random = new Random(0x4C454741L);
        long checks = 0;
        int rounds = Integer.parseInt(args[0]);
        for (int round = 0; round < rounds; round++) {
            long[] sorted = new long[random.nextInt(4) == 0 ? random.nextInt(900) : random.nextInt(40)];
            for (int i = 0; i < sorted.length; i++) {
                sorted[i] = switch (random.nextInt(6)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(30) - 15;
                };
            }
            Arrays.sort(sorted);
            long[] queries = new long[random.nextInt(4) == 0 ? 200 + random.nextInt(400) : random.nextInt(70)];
            for (int i = 0; i < queries.length; i++) queries[i] = random.nextInt(9) == 0 ? Long.MIN_VALUE : random.nextInt(34) - 17;
            if ((round & 1) == 0) Arrays.sort(queries);
            int[] ranges = new int[queries.length * 2];
            legacy.invoke(null, sorted, queries, ranges);
            for (int i = 0; i < queries.length; i++) {
                int lower = bound(sorted, queries[i], false);
                int upper = bound(sorted, queries[i], true);
                if (ranges[2 * i] != lower || ranges[2 * i + 1] != upper) {
                    throw new AssertionError("equalRanges0 n=" + sorted.length + " q=" + queries[i] + " expected=[" + lower
                            + ", " + upper + ") actual=[" + ranges[2 * i] + ", " + ranges[2 * i + 1] + ")");
                }
                checks += 2;
            }
        }
        System.out.println("PASS M3_RANK_LEGACY_EQUAL_RANGES0 checks=" + checks);
    }
}
