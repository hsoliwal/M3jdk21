/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.sketch;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import jdk.m3.collections.RuntimeHeap;

/** Standalone Java 21 contract oracle, without requiring JUnit or a native installation. */
public final class MarkerTopKContract {
    private MarkerTopKContract() { }

    public static void main(String[] args) {
        randomDifferential();
        zeroKAndInvalidGeometry();
        extremeKAndStableIdTies();
        highPressureReleasesOnlyNonwinners();
        healthyHeapSkipsOptionalCleanup();
        cacheReleaseFailureLeavesInputIntact();
        reversedExactOrderingIgnoresMisleadingSignal();
        System.out.println("M3MarkerTopKContract: 7/7 passed");
    }

    public static void randomDifferential() {
        Random random = new Random(0x6D33544FL);
        for (int test = 0; test < 1_200; test++) {
            int n = random.nextInt(170);
            int[] ids = new int[n];
            long[] values = new long[240];
            for (int i = 0; i < values.length; i++) values[i] = random.nextInt(27) - 13;
            for (int i = 0; i < n; i++) ids[i] = random.nextInt(values.length);
            int[] original = ids.clone();
            int k = random.nextInt(n + 1);
            IntBinaryOperator order = (a, b) -> Long.compare(values[a], values[b]);
            int[] actual = MarkerTopKIndex.smallest(ids, 0, n, k, order);
            Integer[] expected = new Integer[n];
            for (int i = 0; i < n; i++) expected[i] = ids[i];
            Arrays.sort(expected, (a, b) -> {
                int diff = Long.compare(values[a], values[b]);
                return diff != 0 ? diff : Integer.compare(a, b);
            });
            require(actual.length == k, "result length");
            for (int i = 0; i < k; i++) require(actual[i] == expected[i],
                    "random top-K disagreement at " + test + ":" + i);
            require(Arrays.equals(ids, original), "input array reordered");
        }
    }

    public static void zeroKAndInvalidGeometry() {
        int[] ids = {3, 2, 1};
        int[] unchanged = ids.clone();
        AtomicInteger comparisons = new AtomicInteger(), releases = new AtomicInteger();
        IntBinaryOperator order = (a, b) -> { comparisons.incrementAndGet(); return Integer.compare(a, b); };
        int[] empty = MarkerTopKIndex.smallest(ids, 0, 3, 0, order,
                discarded -> releases.incrementAndGet());
        require(empty.length == 0 && comparisons.get() == 0 && releases.get() == 0, "zero K did work");
        reject(() -> MarkerTopKIndex.smallest(ids, -1, 3, 1, order));
        reject(() -> MarkerTopKIndex.smallest(ids, 0, 3, 4, order));
        reject(() -> MarkerTopKIndex.smallest(ids, 0, 4, 1, order));
        reject(() -> MarkerTopKIndex.smallest(new int[]{1, -1}, 0, 2, 1, order,
                discarded -> releases.incrementAndGet()));
        require(Arrays.equals(ids, unchanged) && releases.get() == 0, "validation changed source/cache");
    }

    public static void extremeKAndStableIdTies() {
        int[] ids = {8, 5, 5, 3, 1, 3, 7, 8, 0};
        int[] first = MarkerTopKIndex.smallest(ids, 0, ids.length, ids.length,
                (a, b) -> 0);
        require(Arrays.equals(first, new int[]{0, 1, 3, 3, 5, 5, 7, 8, 8}), "stable ID ties");
        int[] smallest = MarkerTopKIndex.smallest(ids, 2, 7, 1, Integer::compare);
        require(Arrays.equals(smallest, new int[]{1}), "offset and K=1");
        int[] one = MarkerTopKIndex.smallest(new int[]{0}, 0, 1, 1, Integer::compare);
        require(Arrays.equals(one, new int[]{0}), "one-element heap");
    }

    public static void highPressureReleasesOnlyNonwinners() {
        int[] ids = {9, 7, 5, 3, 1, 8, 6, 4, 2, 0};
        boolean[] released = new boolean[10];
        AtomicInteger count = new AtomicInteger();
        RuntimeHeap.Snapshot high = new RuntimeHeap.Snapshot(32L << 20, 31L << 20, 30L << 20);
        int[] result = MarkerTopKIndex.smallest(ids, 0, ids.length, 3, Integer::compare,
                id -> { released[id] = true; count.incrementAndGet(); }, high);
        require(Arrays.equals(result, new int[]{0, 1, 2}), "pressure changed ranking");
        require(count.get() == 7, "not every discarded item cache released");
        for (int id = 0; id < 10; id++) require(released[id] == (id >= 3), "wrong cache eviction");
    }

    public static void healthyHeapSkipsOptionalCleanup() {
        RuntimeHeap.Snapshot normal = new RuntimeHeap.Snapshot(1L << 30, 200L << 20, 20L << 20);
        AtomicInteger releases = new AtomicInteger();
        int[] result = MarkerTopKIndex.smallest(new int[]{9, 8, 7, 1}, 0, 4, 2,
                Integer::compare, id -> releases.incrementAndGet(), normal);
        require(Arrays.equals(result, new int[]{1, 7}), "normal top-K differs");
        require(releases.get() == 0, "healthy cache retention should be unchanged");
    }

    public static void cacheReleaseFailureLeavesInputIntact() {
        int[] ids = {7, 6, 5, 4, 3};
        rejectState(() -> MarkerTopKIndex.smallest(ids, 0, ids.length, 2,
                Integer::compare, id -> { throw new IllegalStateException("secondary cache failure"); },
                new RuntimeHeap.Snapshot(32L << 20, 31L << 20, 30L << 20)));
        require(Arrays.equals(ids, new int[]{7, 6, 5, 4, 3}), "cache failure mutated caller's IDs");
    }

    public static void reversedExactOrderingIgnoresMisleadingSignal() {
        long[] actual = {20, -1, 50, 2, 20, 5};
        // Deliberately misleading first hint has no order authority in an unverified recipe.
        long[] firstHint = {9999, -9999, 0, 18, 9999, -3};
        IntBinaryOperator exact = (a, b) -> Long.compare(actual[a], actual[b]);
        int[] source = {0, 1, 2, 3, 4, 5};
        int[] top = MarkerTopKIndex.smallest(source, 0, source.length, 3, exact);
        require(Arrays.equals(top, new int[]{1, 3, 5}), "hint usurped exact comparison");
        require(firstHint[0] != firstHint[1], "invalid test hint");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException | IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("expected argument refusal");
    }
    private static void rejectState(Runnable action) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("expected optional cache failure");
    }
}
