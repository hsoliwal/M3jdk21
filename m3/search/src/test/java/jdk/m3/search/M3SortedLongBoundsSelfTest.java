/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent signed-long linear-reference oracle for Java and real C17/JNI implementations. */
public final class M3SortedLongBoundsSelfTest {
    private static long checks;

    private M3SortedLongBoundsSelfTest() {
    }

    /**
     * Executes deterministic adversarial Java/native parity with fixed thread participation.
     *
     * @param args ignored
     * @throws Exception if any parallel read-only worker fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "invalid-without-library".equals(args[0])) {
            invalidWithoutLibrary();
            System.out.println("M3_SORTED_LONG_NATIVE_PRECONDITIONS\tchecks=" + checks + "\tstatus=PASS");
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("unrecognized self-test mode");
        }
        invalidArguments();
        boundaryCases();
        fullRangeAndSourceSlices();
        orderedAndUnorderedBatches();
        windowCrossing();
        adjacentDuplicateRuns();
        directionalFingerCases();
        boundedDuplicateRuns();
        multipleNativeSourceWindows();
        wholeSlicePrefetchBoundaries();
        orderedNativeMergeAndLateDescent();
        largeSourceWindowedOrderedMerge();
        nonAdjacentRepeatedKeys();
        setAssociativeMemoCollisions();
        concurrentReaders();
        System.out.println("M3_SORTED_LONG_BOUNDS_TEST\tchecks=" + checks
                + "\tjava=PASS\tnative_jni=PASS\tproduct_m3jdk=HOLD");
    }

    /** Proves Java validation precedes library initialization in a fresh JVM without -D native path. */
    private static void invalidWithoutLibrary() {
        if (System.getProperty("m3.native.sorted.long.search.library") != null) {
            throw new AssertionError("native library property must be absent for this probe");
        }
        assertThrows(NullPointerException.class,
                () -> M3NativeSortedLongBounds.bounds(null, 0, 0, new long[0]));
        assertThrows(NullPointerException.class,
                () -> M3NativeSortedLongBounds.bounds(new long[0], 0, 0, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3NativeSortedLongBounds.bounds(new long[0], 0, 1, new long[0]));
        assertThrows(IllegalStateException.class,
                () -> M3NativeSortedLongBounds.bounds(new long[0], 0, 0, new long[0]));
    }

    private static void invalidArguments() {
        assertThrows(NullPointerException.class, () -> M3SortedLongBounds.bounds(null, 0, 0, new long[0]));
        assertThrows(NullPointerException.class, () -> M3NativeSortedLongBounds.bounds(null, 0, 0, new long[0]));
        assertThrows(NullPointerException.class, () -> M3SortedLongBounds.bounds(new long[0], 0, 0, null));
        assertThrows(NullPointerException.class, () -> M3NativeSortedLongBounds.bounds(new long[0], 0, 0, null));
        for (int[] range : new int[][]{{-1, 0}, {0, -1}, {1, 0}, {0, 3}, {3, 3}}) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3SortedLongBounds.bounds(new long[2], range[0], range[1], new long[0]));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> M3NativeSortedLongBounds.bounds(new long[2], range[0], range[1], new long[0]));
        }
        assertThrows(NullPointerException.class, () -> M3SortedLongBounds.bounds(null, new long[0]));
        assertThrows(NullPointerException.class, () -> M3SortedLongBounds.bounds(new long[0], null));
    }

    private static void boundaryCases() {
        compare(new long[0], 0, 0,
                new long[]{Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE});
        compare(new long[]{7L, 7L, 7L, 7L}, 0, 4,
                new long[]{Long.MIN_VALUE, 6, 7, 8, Long.MAX_VALUE});
        compare(new long[]{Long.MIN_VALUE, Long.MIN_VALUE, -3, -3, 0, 0,
                Long.MAX_VALUE, Long.MAX_VALUE}, 0, 8,
                new long[]{Long.MIN_VALUE, -4, -3, 0, 1, Long.MAX_VALUE});
        compare(new long[]{-3, -2, -1, 0, 1, 2, 3}, 3, 3, new long[]{-3, 0, 2, 3});
        compare(new long[]{-3, -2, -1, 0, 1, 2, 3}, 7, 7, new long[]{-3, 0, 2, 3});
        compare(new long[]{-3, -2, -1, 0, 1, 2, 3}, 2, 5, new long[]{-5, -1, 0, 1, 3});
        compare(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}, 0, 3, new long[0]);
        int[] result = M3SortedLongBounds.bounds(new long[]{0L, 1L, 1L}, new long[]{1L});
        check(Arrays.equals(result, new int[]{1, 3}), "full range overload");
        check(M3SortedLongBounds.lowerBound(new long[]{Long.MAX_VALUE}, 0, 1, Long.MIN_VALUE) == 0,
                "lower signed extreme");
        check(M3SortedLongBounds.upperBound(new long[]{Long.MIN_VALUE}, 0, 1, Long.MAX_VALUE) == 1,
                "upper signed extreme");
    }

    private static void fullRangeAndSourceSlices() {
        for (int n = 0; n <= 384; n++) {
            long[] sorted = new long[n];
            for (int i = 0; i < n; i++) {
                sorted[i] = i / 3 - n / 5;
            }
            if (n >= 1) {
                sorted[0] = Long.MIN_VALUE;
            }
            if (n >= 2) {
                sorted[n - 1] = Long.MAX_VALUE;
            }
            long[] queries = new long[44];
            for (int i = 0; i < queries.length; i++) {
                queries[i] = i == 0 ? Long.MIN_VALUE : i == 1 ? Long.MAX_VALUE
                        : (i * 31L % 139) - 69;
            }
            compare(sorted, 0, n, queries);
            int from = n / 5;
            int to = n - n / 7;
            compare(sorted, from, to, queries);
            compare(sorted, from, from, queries);
        }
    }

    private static void orderedAndUnorderedBatches() {
        for (int n = 0; n <= 640; n += 13) {
            long[] sorted = new long[n];
            for (int i = 0; i < n; i++) {
                sorted[i] = ((long)i << 40) - (350L << 40);
            }
            for (int q : new int[]{1, 2, 127, 128, 129, 257, 513}) {
                long[] queries = new long[q];
                for (int i = 0; i < q; i++) {
                    queries[i] = ((long)i / 3L << 40) - (50L << 40);
                }
                compare(sorted, 0, sorted.length, queries);
                long[] reversed = queries.clone();
                reverse(reversed);
                compare(sorted, 0, sorted.length, reversed);
                int from = n / 4;
                compare(sorted, from, n, queries);
            }
        }
    }

    private static void windowCrossing() {
        long[] sorted = new long[11_219];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i / 7L - 1_200L;
        }
        long[] queries = new long[5_137];
        for (int i = 0; i < queries.length; i++) {
            queries[i] = (i * 7L % 3_301L) - 1_500L;
        }
        compare(sorted, 0, sorted.length, queries);
        compare(sorted, 117, 10_091, queries);
        Arrays.sort(queries);
        compare(sorted, 0, sorted.length, queries);
        compare(sorted, 117, 10_091, queries);
    }

    /** Explicitly covers equal-key carries across the JNI 255/256/257 batch boundary. */
    private static void adjacentDuplicateRuns() {
        long[] sorted = new long[1_033];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = i / 7L - 73L;
        }
        sorted[0] = Long.MIN_VALUE;
        sorted[sorted.length - 1] = Long.MAX_VALUE;
        long[] representatives = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, -73L, 73L, -1L};
        for (int n : new int[]{1, 2, 127, 128, 129, 255, 256, 257, 258, 511, 512, 513, 1_025}) {
            long[] keys = new long[n];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = representatives[(i / 17) % representatives.length];
            }
            compare(sorted, 0, sorted.length, keys);
            compare(sorted, 119, 899, keys);
            compare(sorted, 256, 256, keys);
            Arrays.fill(keys, Long.MIN_VALUE);
            compare(sorted, 0, sorted.length, keys);
            Arrays.fill(keys, Long.MAX_VALUE);
            compare(sorted, 119, 899, keys);
        }
    }

    /** Oscillating and locally progressing queries prove both directional fences across JNI windows. */
    private static void directionalFingerCases() {
        long[] sorted = new long[1_049];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = (i / 5L) - 111L;
        }
        sorted[0] = Long.MIN_VALUE;
        sorted[sorted.length - 1] = Long.MAX_VALUE;
        for (int count : new int[]{0, 1, 2, 127, 128, 129, 255, 256, 257,
                511, 512, 513, 1_025, 2_057}) {
            long[] keys = new long[count];
            for (int i = 0; i < count; i++) {
                int group = i / 23;
                keys[i] = switch (i % 9) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> -2_047L;
                    case 3 -> 2_047L;
                    case 4 -> group - 35L;
                    case 5 -> group - 34L;
                    case 6 -> group - 36L;
                    case 7 -> 0L;
                    default -> -1L;
                };
            }
            compare(sorted, 0, sorted.length, keys);
            compare(sorted, 19, 1_003, keys);
            compare(sorted, 256, 256, keys);
            reverse(keys);
            compare(sorted, 0, sorted.length, keys);
            compare(sorted, 19, 1_003, keys);
        }
    }

    /** Duplicate-run lengths around the eight-element probe cutoff, with absolute slicing. */
    private static void boundedDuplicateRuns() {
        int[] runLengths = {1, 2, 3, 7, 8, 9, 17, 255, 256, 257};
        int capacity = 2;
        for (int len : runLengths) {
            capacity += len;
        }
        long[] sorted = new long[capacity];
        sorted[0] = Long.MIN_VALUE;
        int cursor = 1;
        long[] members = new long[runLengths.length];
        for (int run = 0; run < runLengths.length; run++) {
            members[run] = run * 31L - 155L;
            Arrays.fill(sorted, cursor, cursor + runLengths[run], members[run]);
            cursor += runLengths[run];
        }
        sorted[cursor] = Long.MAX_VALUE;
        for (int count : new int[]{0, 1, 2, 7, 8, 9, 17, 127, 128, 129, 255, 256, 257, 513}) {
            long[] keys = new long[count];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = switch (i % 5) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> members[(i / 5) % members.length];
                    case 3 -> members[(i / 5) % members.length] + 1;
                    default -> members[(i / 5) % members.length] - 1;
                };
            }
            compare(sorted, 0, sorted.length, keys);
            compare(sorted, 1, sorted.length - 1, keys);
            compare(sorted, 11, cursor - 1, keys);
            compare(sorted, 255, 255, keys);
            reverse(keys);
            compare(sorted, 0, sorted.length, keys);
        }
    }

    /** Exercises 256-entry pages, source offsets and direct-cache-slot collisions. */
    private static void multipleNativeSourceWindows() {
        int[] sizes = {1, 255, 256, 257, 511, 512, 513, 1023, 1024, 1025, 2049, 4097, 8193};
        int[] starts = {0, 1, 127, 255, 256, 511};
        for (int size : sizes) {
            long[] sorted = new long[size];
            for (int i = 0; i < size; i++) {
                sorted[i] = i / 3L - size / 4L;
            }
            sorted[0] = Long.MIN_VALUE;
            if (size > 1) {
                sorted[size - 1] = Long.MAX_VALUE;
            }
            for (int from : starts) {
                if (from > size) {
                    continue;
                }
                int to = size - (size > from + 2 ? 1 : 0);
                long[] queries = new long[257];
                int span = to - from;
                for (int j = 0; j < queries.length; j++) {
                    if (j % 11 == 0) {
                        queries[j] = Long.MIN_VALUE;
                    } else if (j % 11 == 1) {
                        queries[j] = Long.MAX_VALUE;
                    } else if (span == 0) {
                        queries[j] = 0;
                    } else {
                        int position = from + (int)((j * 257L + (j % 5) * 127L) % span);
                        queries[j] = sorted[position];
                    }
                }
                compare(sorted, from, to, queries);
            }
        }
    }

    /**
     * Covers the source copy threshold, short-query fallback, and offset-indexed whole-slice copy.
     * The independent linear oracle decides correctness, not the candidate's binary-search code.
     */
    private static void wholeSlicePrefetchBoundaries() {
        int[] sizes = {0, 1, 255, 256, 257, 1023, 1024, 1025, 2049};
        int[] queryCounts = {0, 1, 127, 128, 129, 255, 256, 257, 513};
        for (int size : sizes) {
            long[] source = new long[size];
            for (int i = 0; i < size; i++) {
                source[i] = i / 3L - size / 4L;
            }
            if (size > 0) {
                source[0] = Long.MIN_VALUE;
                source[size - 1] = Long.MAX_VALUE;
            }
            int[] offsets = {0, Math.min(size, 1), size / 3, size / 2};
            for (int from : offsets) {
                int to = size > from + 1 ? size - 1 : size;
                for (int count : queryCounts) {
                    long[] keys = new long[count];
                    for (int i = 0; i < count; i++) {
                        keys[i] = switch (i % 7) {
                            case 0 -> Long.MIN_VALUE;
                            case 1 -> Long.MAX_VALUE;
                            case 2 -> -1L;
                            case 3 -> 0L;
                            case 4 -> 1L;
                            default -> to > from ? source[from + (int)((i * 127L + 11L) % (to - from))] : 0L;
                        };
                    }
                    compare(source, from, to, keys);
                    reverse(keys);
                    compare(source, from, to, keys);
                }
            }
        }
    }

    /** Proves ordered-prefix admission and correct irreversible fallback after a late descent. */
    private static void orderedNativeMergeAndLateDescent() {
        int[] sizes = {0, 1, 127, 255, 256, 257, 1023, 1024, 1025, 2049};
        int[] queryCounts = {127, 128, 129, 255, 256, 257, 513, 1025};
        for (int size : sizes) {
            long[] sorted = new long[size];
            for (int i = 0; i < size; i++) {
                sorted[i] = i / 4L - size / 5L;
            }
            if (size > 0) {
                sorted[0] = Long.MIN_VALUE;
                sorted[size - 1] = Long.MAX_VALUE;
            }
            for (int from : new int[]{0, Math.min(11, size)}) {
                int to = size > from + 3 ? size - 3 : size;
                for (int queryCount : queryCounts) {
                    long[] ordered = new long[queryCount];
                    for (int i = 0; i < queryCount; i++) {
                        ordered[i] = i / 3L - 160;
                    }
                    compare(sorted, from, to, ordered);
                    long[] lateDrop = ordered.clone();
                    for (int i = 256; i < lateDrop.length; i++) {
                        lateDrop[i] = (i % 3 == 0) ? Long.MAX_VALUE :
                                ((i % 3 == 1) ? Long.MIN_VALUE : 0L);
                    }
                    compare(sorted, from, to, lateDrop);
                    reverse(lateDrop);
                    compare(sorted, from, to, lateDrop);
                }
            }
        }
    }

    /**
     * Crosses the 1,024-value prefetch cap and proves streaming merge, its
     * density refusal, late-descent fallback, and 8,192-value source ceiling.
     */
    private static void largeSourceWindowedOrderedMerge() {
        int[][] domains = {
                {1_025, 257}, {2_048, 513}, {4_096, 1_024}, {8_192, 2_048},
                {8_193, 2_048}, {4_096, 127}, {4_096, 255}, {4_096, 4_096}
        };
        for (int[] domain : domains) {
            int size = domain[0];
            int queryCount = domain[1];
            long[] sorted = new long[size];
            for (int i = 0; i < size; i++) {
                sorted[i] = i / 3L - size / 5L;
            }
            sorted[0] = Long.MIN_VALUE;
            sorted[size - 1] = Long.MAX_VALUE;
            long[] keys = new long[queryCount];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = ((long) i * (size / 3L + 17L)) / queryCount - size / 5L;
            }
            compare(sorted, 0, size, keys);
            if (size <= 4_096) {
                compare(sorted, 113, size - 17, keys);
            }
            if (queryCount >= 513) {
                long[] late = keys.clone();
                for (int i = 257; i < late.length; i++) {
                    late[i] = i % 2 == 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
                }
                compare(sorted, 0, size, late);
            }
            if (size <= 4_096) {
                long[] reversed = keys.clone();
                reverse(reversed);
                compare(sorted, 0, size, reversed);
                Arrays.fill(reversed, 0L);
                compare(sorted, 113, size - 17, reversed);
            }
        }
    }

    /**
     * Independent linear oracle for native exact-key memo hits, direct-map
     * collisions, 256-query windows, signed extremes and nonzero source offsets.
     */
    private static void nonAdjacentRepeatedKeys() {
        for (int size : new int[]{1_024, 1_025, 4_096, 8_192, 8_193}) {
            long[] sorted = new long[size];
            for (int i = 0; i < sorted.length; i++) {
                sorted[i] = i / 4L - size / 7L;
            }
            sorted[0] = Long.MIN_VALUE;
            sorted[size - 1] = Long.MAX_VALUE;
            for (int ring : new int[]{3, 7, 31, 63, 65}) {
                for (int count : new int[]{127, 255, 256, 257, 513}) {
                    long[] queries = new long[count];
                    for (int i = 0; i < count; i++) {
                        queries[i] = switch (i % 13) {
                            case 0 -> Long.MIN_VALUE;
                            case 1 -> Long.MAX_VALUE;
                            case 2 -> -9_000_000_000_000L;
                            case 3 -> 9_000_000_000_000L;
                            default -> ((long)(i % ring) * 37L) - 581L;
                        };
                    }
                    compare(sorted, 0, size, queries);
                    compare(sorted, 117, size - 61, queries);
                }
            }
            // A 7-key repeated first region admits the memo, followed by more
            // than 64 distinct keys: direct-cache slot collisions are unavoidable.
            long[] evicting = new long[513];
            for (int i = 0; i < evicting.length; i++) {
                evicting[i] = i < 64 ? ((i % 7) * 41L - 123L)
                        : (((i * 17L) % 129L) * 41L - 123L);
            }
            compare(sorted, 0, size, evicting);
            reverse(evicting);
            compare(sorted, 13, size - 7, evicting);
        }
    }

    /** Four signed-long keys deliberately sharing the previous 64-slot index. */
    private static void setAssociativeMemoCollisions() {
        long[] sameFormerSlot = {0L, 34L, 89L, 178L, 233L, 267L, 322L, 411L, 466L};
        for (int size : new int[]{1_025, 4_096, 8_192, 8_193}) {
            long[] source = new long[size];
            for (int i = 0; i < size; i++) {
                source[i] = i / 4L - size / 7L;
            }
            source[0] = Long.MIN_VALUE;
            source[size - 1] = Long.MAX_VALUE;
            for (int width : new int[]{2, 3, 4, 5, 8, 9}) {
                for (int count : new int[]{127, 128, 255, 256, 257, 513, 1_025}) {
                    long[] keys = new long[count];
                    for (int i = 0; i < keys.length; i++) {
                        keys[i] = sameFormerSlot[i % width];
                    }
                    compare(source, 0, source.length, keys);
                    compare(source, 111, source.length - 17, keys);
                    if (keys.length > 3) {
                        keys[1] = Long.MIN_VALUE;
                        keys[3] = Long.MAX_VALUE;
                    }
                    compare(source, 0, source.length, keys);
                    compare(source, 117, 117, keys);
                }
            }
        }
    }

    private static void concurrentReaders() throws Exception {
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Callable<Long>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                final int w = worker;
                tasks.add(() -> {
                    long validated = 0;
                    for (int n = 1; n <= 384; n += 17) {
                        long[] source = new long[n];
                        for (int i = 0; i < source.length; i++) {
                            source[i] = i / 3L - 41L;
                        }
                        long[] keys = {Long.MIN_VALUE, Long.MAX_VALUE, w - 4L, -500, 0, 500};
                        int[] expected = linearOracle(source, 0, n, keys);
                        if (!Arrays.equals(expected, M3SortedLongBounds.bounds(source, 0, n, keys))
                                || !Arrays.equals(expected, M3NativeSortedLongBounds.bounds(source, 0, n, keys))) {
                            throw new AssertionError("parallel read-only long array parity divergence");
                        }
                        validated += 12;
                    }
                    return validated;
                });
            }
            for (Future<Long> result : pool.invokeAll(tasks)) {
                checks += result.get();
            }
        }
    }

    private static void reverse(long[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            long swap = values[i];
            values[i] = values[j];
            values[j] = swap;
        }
    }

    private static void compare(long[] source, int from, int to, long[] keys) {
        long[] unchangedSource = source.clone();
        long[] unchangedKeys = keys.clone();
        int[] expected = linearOracle(source, from, to, keys);
        int[] java = M3SortedLongBounds.bounds(source, from, to, keys);
        int[] nativePairs = M3NativeSortedLongBounds.bounds(source, from, to, keys);
        check(Arrays.equals(java, expected), "Java lower/upper mismatch");
        check(Arrays.equals(nativePairs, expected), "C17/JNI lower/upper mismatch");
        check(Arrays.equals(source, unchangedSource), "sorted source modified");
        check(Arrays.equals(keys, unchangedKeys), "query batch modified");
        check(java != nativePairs && java != expected && nativePairs != expected,
                "result ownership aliased");
        for (int i = 0; i < keys.length; i++) {
            int lower = java[2 * i];
            int upper = java[2 * i + 1];
            check(from <= lower && lower <= upper && upper <= to, "absolute bounds outside slice");
            if (lower < to) {
                check(source[lower] >= keys[i], "lower bound inequality");
            }
            if (upper < to) {
                check(source[upper] > keys[i], "upper bound inequality");
            }
        }
    }

    private static int[] linearOracle(long[] source, int from, int to, long[] keys) {
        int[] expected = new int[keys.length * 2];
        for (int i = 0; i < keys.length; i++) {
            int lower = from;
            int upper = from;
            while (lower < to && source[lower] < keys[i]) {
                lower++;
            }
            while (upper < to && source[upper] <= keys[i]) {
                upper++;
            }
            expected[2 * i] = lower;
            expected[2 * i + 1] = upper;
        }
        return expected;
    }

    private static void assertThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "wrong exception: " + failure);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
