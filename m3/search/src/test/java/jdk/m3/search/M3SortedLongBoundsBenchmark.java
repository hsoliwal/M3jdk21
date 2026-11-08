/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.search;

import java.util.Arrays;
import java.util.Locale;

/** Bounded host-only comparison; no universal performance or automatic JNI-dispatch claim. */
public final class M3SortedLongBoundsBenchmark {
    private static volatile long sink;

    private M3SortedLongBoundsBenchmark() {
    }

    /**
     * Benchmarks ordered and shuffled fixed-workload query batches on the current host JVM.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        long[] source = new long[1024];
        long[] ordered = new long[4096];
        for (int i = 0; i < source.length; i++) {
            source[i] = i / 4L - 128;
        }
        for (int i = 0; i < ordered.length; i++) {
            ordered[i] = i / 8L - 128;
        }
        long[] unordered = ordered.clone();
        for (int i = 0; i < unordered.length; i++) {
            int j = (i * 127 + 3) % unordered.length;
            long t = unordered[i];
            unordered[i] = unordered[j];
            unordered[j] = t;
        }
        long[] clustered = new long[4096];
        long[] representatives = {Long.MAX_VALUE, Long.MIN_VALUE, -7L, 0L, 31L, -128L};
        for (int i = 0; i < clustered.length; i++) {
            clustered[i] = representatives[(i / 257) % representatives.length];
        }
        compare("ordered", source, ordered);
        long[] fineOrdered = new long[4096];
        for (int i = 0; i < fineOrdered.length; i++) {
            fineOrdered[i] = i / 2L - 160L;
        }
        compare("ordered-fine", source, fineOrdered);
        long[] lateDrop = fineOrdered.clone();
        for (int i = 256; i < lateDrop.length; i++) {
            lateDrop[i] = i % 2 == 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        compare("ordered-prefix-late-drop", source, lateDrop);
        compare("unordered", source, unordered);
        compare("clustered-unordered", source, clustered);
        // Alternating near-key neighborhoods exercise directional insertion fences, not whole-batch sort.
        long[] localityZigzag = new long[4096];
        for (int i = 0; i < localityZigzag.length; i++) {
            long center = (i / 31L) % 19L - 9L;
            localityZigzag[i] = center + switch (i % 6) {
                case 0 -> -3L;
                case 1 -> 4L;
                case 2 -> -1L;
                case 3 -> 2L;
                case 4 -> -2L;
                default -> 1L;
            };
        }
        compare("locality-zigzag", source, localityZigzag);
        long[] shortRuns = new long[4096];
        for (int i = 0; i < shortRuns.length; i++) {
            shortRuns[i] = ((i * 31L) % 256L) - 128L;
        }
        compare("short-duplicate-runs", source, shortRuns);
        // Prefetch threshold: 127 keys uses demand pages; 128 uses the whole-source copy.
        compare("short-batch-fallback", source, Arrays.copyOf(unordered, 127));
        compare("prefetch-minimum", source, Arrays.copyOf(unordered, 128));
        // One element beyond the 1,024-source cap must retain four demand-filled windows.
        long[] wider = new long[1025];
        for (int i = 0; i < wider.length; i++) {
            wider[i] = i / 4L - 128L;
        }
        compare("source-over-capacity", wider, unordered);

        // Category: two-pointer ordered-search merge over sources exceeding
        // the whole-slice prefetch capacity. Other shapes retain binary paging.
        for (int sourceSize : new int[]{4_096, 8_192}) {
            long[] large = new long[sourceSize];
            for (int i = 0; i < large.length; i++) {
                large[i] = i / 4L - sourceSize / 5L;
            }
            long[] orderedLarge = new long[4_096];
            for (int i = 0; i < orderedLarge.length; i++) {
                orderedLarge[i] = i / 4L - sourceSize / 5L;
            }
            compare("windowed-ordered", large, orderedLarge);
            long[] descendingAfterPrefix = orderedLarge.clone();
            for (int i = 257; i < descendingAfterPrefix.length; i++) {
                descendingAfterPrefix[i] = i % 2 == 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
            compare("windowed-late-drop", large, descendingAfterPrefix);
            long[] shuffled = orderedLarge.clone();
            for (int i = 0; i < shuffled.length; i++) {
                int j = (i * 127 + 3) % shuffled.length;
                long tmp = shuffled[i];
                shuffled[i] = shuffled[j];
                shuffled[j] = tmp;
            }
            compare("windowed-unordered", large, shuffled);
            // Nonadjacent key recurrence is not handled by the previous-key memo.
            long[] recurring = new long[4_096];
            for (int i = 0; i < recurring.length; i++) {
                recurring[i] = (i % 7) * 37L - 581L;
            }
            compare("windowed-nonadjacent-repeat", large, recurring);
            // The first chunk has no repetition; the new native memo must stay off.
            long[] distinct = new long[4_096];
            for (int i = 0; i < distinct.length; i++) {
                distinct[i] = (i * 127L % 4_096L) - 1_200L;
            }
            compare("windowed-distinct-control", large, distinct);
            // The first chunk repeats 7 keys; 129 later keys force hash collisions.
            long[] collisions = new long[4_096];
            for (int i = 0; i < collisions.length; i++) {
                collisions[i] = i < 64 ? (i % 7) * 41L - 123L
                        : ((i * 17L) % 129L) * 41L - 123L;
            }
            compare("windowed-repeat-collisions", large, collisions);
            // Four exact signed-long keys collide in the prior 64-slot memo.
            long[] sameSlot = {0L, 34L, 89L, 178L, 233L, 267L, 322L, 411L, 466L};
            for (int width : new int[]{4, 5, 8, 9}) {
                long[] collidingRing = new long[4_096];
                for (int i = 0; i < collidingRing.length; i++) {
                    collidingRing[i] = sameSlot[i % width];
                }
                compare("windowed-associative-collision-" + width, large, collidingRing);
            }
        }
    }

    private static void compare(String name, long[] source, long[] keys) {
        int iterations = 25;
        long[] measuredJava = new long[iterations];
        long[] measuredNative = new long[iterations];
        long[] measuredBinary = new long[iterations];
        for (int i = 0; i < iterations + 10; i++) {
            int[] result;
            long start = System.nanoTime();
            result = M3SortedLongBounds.bounds(source, 0, source.length, keys);
            long elapsedJava = System.nanoTime() - start;
            sink += result[0];
            start = System.nanoTime();
            result = M3NativeSortedLongBounds.bounds(source, 0, source.length, keys);
            long elapsedNative = System.nanoTime() - start;
            sink += result[1];
            start = System.nanoTime();
            result = binaryOnly(source, keys);
            long elapsedBinary = System.nanoTime() - start;
            sink += result[2];
            if (i >= 10) {
                measuredJava[i - 10] = elapsedJava;
                measuredNative[i - 10] = elapsedNative;
                measuredBinary[i - 10] = elapsedBinary;
            }
        }
        System.out.printf(Locale.ROOT,
                "M3_SORTED_LONG_HOST_BENCH\tmode=%s\tN=%d\tQ=%d\titerations=%d"
                + "\tjava_median_ns=%d\tnative_median_ns=%d\tbinary_median_ns=%d\tsink=%d"
                + "\tproduct=HOLD%n",
                name, source.length, keys.length, iterations,
                median(measuredJava), median(measuredNative), median(measuredBinary), sink);
    }

    private static int[] binaryOnly(long[] source, long[] keys) {
        int[] result = new int[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            result[2 * i] = M3SortedLongBounds.lowerBound(source, 0, source.length, keys[i]);
            result[2 * i + 1] = M3SortedLongBounds.upperBound(source, 0, source.length, keys[i]);
        }
        return result;
    }

    private static long median(long[] values) {
        long[] ordered = values.clone();
        Arrays.sort(ordered);
        return ordered[ordered.length / 2];
    }
}
