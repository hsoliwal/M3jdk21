// SPDX-License-Identifier: Apache-2.0
// JVM-local descriptive timing; not JMH and not a production throughput claim.
package jdk.m3.regexcursor;

import java.util.Arrays;
import java.util.Random;

public final class TokenCursorSeekLocalityBench {
    private TokenCursorSeekLocalityBench() { }
    private static volatile long sink;

    private static int[][] rows(int count) {
        int[][] r = new int[count][];
        for (int i = 0; i < count; i++) r[i] = new int[] {i + 1, i + 3, i + 7};
        return r;
    }

    private static int[] workload(int lanes, String kind, int count) {
        int[] p = new int[count];
        Random rnd = new Random(0x10ca1144L);
        for (int i = 0; i < count; i++) {
            p[i] = switch (kind) {
                case "same" -> 3 * (lanes / 2) + (i % 3);
                case "neighbor" -> 3 * (lanes / 2 + ((i & 1) == 0 ? 0 : 1)) + i % 3;
                case "random" -> rnd.nextInt(lanes * 3 + 1);
                default -> throw new IllegalArgumentException(kind);
            };
        }
        return p;
    }

    private static void measure(int lanes, String kind) {
        int[][] arrays = rows(lanes);
        TokenCursor cursor = TokenCursor.lanes(arrays);
        int[] workload = workload(lanes, kind, 200_000);
        long checksum = 0;
        for (int warm = 0; warm < 4; warm++) {
            for (int position : workload) {
                cursor.seek(position);
                checksum += cursor.offset();
            }
        }
        long[] samples = new long[7];
        for (int trial = 0; trial < samples.length; trial++) {
            long start = System.nanoTime();
            for (int position : workload) {
                cursor.seek(position);
                checksum += cursor.offset();
            }
            samples[trial] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        sink = checksum;
        System.out.printf("M3_SEEK_LOCALITY_BENCH lanes=%d kind=%s ns_per_seek=%.2f checksum=%d%n",
                lanes, kind, samples[3] / (double) workload.length, sink);
    }

    public static void main(String[] args) {
        for (int lanes : new int[] {8, 128, 1024}) {
            measure(lanes, "same");
            measure(lanes, "neighbor");
            measure(lanes, "random");
        }
    }
}
