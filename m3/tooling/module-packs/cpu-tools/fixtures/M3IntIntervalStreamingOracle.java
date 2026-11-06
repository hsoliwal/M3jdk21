// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3IntIntervalSet;
import com.m3.tools.cpu.M3IntIntervalSetNative;
import java.util.Arrays;
import java.util.Random;

/** JNI transition coverage for sorted prefixes, late/early inversions, and detached results. */
public final class M3IntIntervalStreamingOracle {
    private static long checks;
    private static void require(boolean valid, String name) {
        checks++;
        if (!valid) throw new AssertionError(name + " @ " + checks);
    }
    private static void compare(M3IntIntervalSet set, int[] queries) {
        int[] before = queries.clone();
        int[] endpoints = set.toPairs();
        byte[] flags = M3IntIntervalSetNative.contains(set, queries);
        require(flags.length == queries.length, "length");
        require(Arrays.equals(before, queries), "caller query mutation");
        require(Arrays.equals(endpoints, set.toPairs()), "canonical endpoints");
        for (int i = 0; i < queries.length; i++) {
            require(flags[i] == (set.contains(queries[i]) ? 1 : 0), "JNI parity " + i);
        }
        byte[] detached = M3IntIntervalSetNative.contains(set, queries);
        require(detached != flags && Arrays.equals(detached, flags), "fresh output");
        if (flags.length > 0) {
            flags[flags.length - 1] ^= 1;
            require(Arrays.equals(detached, M3IntIntervalSetNative.contains(set,queries)),
                    "native result not aliased");
        }
    }
    public static void main(String[] args) {
        Random rng = new Random(0x22110226L);
        for (int trial = 0; trial < 256; trial++) {
            int[] input = new int[(trial % 17) * 2];
            for (int j = 0; j < input.length; j += 2) {
                int begin = rng.nextInt(900) - 450;
                input[j] = begin;
                input[j + 1] = begin + rng.nextInt(19);
            }
            M3IntIntervalSet set = M3IntIntervalSet.copyOf(input);
            int n = switch (trial % 6) {
                case 0 -> 0;
                case 1 -> 63;
                case 2 -> 64;
                case 3 -> 65;
                case 4 -> 129;
                default -> 513;
            };
            int[] queries = new int[n];
            for (int j = 0; j < n; j++) queries[j] = (j / 8) - 30;
            switch (trial % 5) {
                case 0 -> { } // fully monotone
                case 1 -> { if (n > 1) queries[1] = Integer.MIN_VALUE; }
                case 2 -> { if (n > 1) queries[n - 1] = Integer.MIN_VALUE; }
                case 3 -> { for (int j = 0; j < n; j++) queries[j] = rng.nextInt(950)-475; }
                default -> { for (int j = 0; j < n; j++) {
                    queries[j] = (j % 2 == 0) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                } }
            }
            compare(set, queries);
        }
        M3IntIntervalSet empty = M3IntIntervalSet.copyOf(new int[0]);
        int[] large = new int[12_000];
        for (int i = 0; i < large.length; i++) large[i] = i - 6_000;
        compare(empty, large);
        M3IntIntervalSet extremes = M3IntIntervalSet.copyOf(new int[] {
                Integer.MIN_VALUE, Integer.MIN_VALUE+1, -3, 0,
                Integer.MAX_VALUE-1, Integer.MAX_VALUE
        });
        compare(extremes,new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE+1, -3,0,
                Integer.MAX_VALUE-1,Integer.MAX_VALUE});
        M3IntIntervalSet separated = M3IntIntervalSet.copyOf(new int[]{-100,-90,0,10,100,101});
        large[large.length - 1] = Integer.MIN_VALUE;
        compare(separated, large);
        large[1] = Integer.MIN_VALUE;
        compare(separated, large);
        large[1] = -5_999;
        large[large.length - 1] = Integer.MAX_VALUE;
        compare(separated, large);
        System.out.println("M3_INTERVAL_STREAM_JNI_PASS checks=" + checks);
    }
}
