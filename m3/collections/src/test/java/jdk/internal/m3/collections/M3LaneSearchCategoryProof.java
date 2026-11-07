// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Category-first LC34/LC35/LC704: linear oracle for existing primitive bisection kernels. */
public final class M3LaneSearchCategoryProof {
    private static final int[] LENGTHS = {0, 1, 2, 3, 4, 7, 8, 15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 255, 256, 257, 1024};
    private static final int TRIALS = 320;
    private M3LaneSearchCategoryProof() { }

    private static int linearLower(int[] lane, int from, int to, int key) {
        for (int i = from; i < to; i++) if (lane[i] >= key) return i;
        return to;
    }
    private static int linearUpper(int[] lane, int from, int to, int key) {
        for (int i = from; i < to; i++) if (lane[i] > key) return i;
        return to;
    }
    private static int linearLower(long[] lane, int from, int to, long key) {
        for (int i = from; i < to; i++) if (lane[i] >= key) return i;
        return to;
    }
    private static int linearUpper(long[] lane, int from, int to, long key) {
        for (int i = from; i < to; i++) if (lane[i] > key) return i;
        return to;
    }
    private static void compare(int[] ints, long[] longs, int from, int to, int key, long longKey) {
        int il = linearLower(ints, from, to, key);
        int iu = linearUpper(ints, from, to, key);
        int ll = linearLower(longs, from, to, longKey);
        int lu = linearUpper(longs, from, to, longKey);
        if (M3LaneSearch.lowerBound(ints, from, to, key) != il
                || M3LaneSearch.upperBound(ints, from, to, key) != iu
                || M3LaneSearch.lowerBound(longs, from, to, longKey) != ll
                || M3LaneSearch.upperBound(longs, from, to, longKey) != lu) {
            throw new AssertionError("mismatch from=" + from + " to=" + to + " key=" + key);
        }
    }
    public static long run() {
        Random random = new Random(0x4d335f4c434f4e47L);
        long comparisons = 0;
        for (int len : LENGTHS) {
            for (int trial = 0; trial < TRIALS; trial++) {
                int[] ints = new int[len];
                long[] longs = new long[len];
                for (int i = 0; i < len; i++) {
                    ints[i] = switch (i % 13) {
                        case 0 -> Integer.MIN_VALUE;
                        case 1 -> Integer.MAX_VALUE;
                        default -> random.nextInt(45) - 22;
                    };
                    longs[i] = switch (i % 13) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        default -> random.nextInt(45) - 22;
                    };
                }
                Arrays.sort(ints);
                Arrays.sort(longs);
                int[] beforeInts = ints.clone();
                long[] beforeLongs = longs.clone();
                int from = random.nextInt(len + 1);
                int to = from + random.nextInt(len + 1 - from);
                int key = switch (trial % 7) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> len == 0 ? 0 : ints[random.nextInt(len)];
                    default -> random.nextInt(67) - 33;
                };
                long longKey = switch (trial % 7) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> len == 0 ? 0 : longs[random.nextInt(len)];
                    default -> random.nextInt(67) - 33;
                };
                compare(ints, longs, from, to, key, longKey);
                comparisons += 4;
                compare(ints, longs, 0, len, key, longKey);
                comparisons += 4;
                compare(ints, longs, len, len, key, longKey);
                comparisons += 4;
                if (!Arrays.equals(ints, beforeInts) || !Arrays.equals(longs, beforeLongs)) {
                    throw new AssertionError("search mutated a primitive lane");
                }
            }
        }
        return comparisons;
    }
    public static void main(String[] args) {
        System.out.println("M3_LANE_CATEGORY_DIFFERENTIAL_PASS comparisons=" + run());
    }
}
