/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;

/** Standalone Java21, source-owned differential oracle with no third-party test dependency. */
public final class M3SortableTopKIdsContract {
    private M3SortableTopKIdsContract() { }

    public static void main(String[] args) {
        randomDifferential();
        duplicatesStableId();
        reversedAndMinValueComparator();
        unverifiedFirstSignalCannotOverrideExact();
        callerOwnedWindowAndSourceIdentity();
        validatesBeforeWriting();
        zeroWorkNeverCallsComparator();
        largeSparseIdsAndLargeWorkset();
        System.out.println("M3SortableTopKIds: 8/8 focused contract checks passed");
    }

    static void randomDifferential() {
        Random random = new Random(0xA31B002DL);
        for (int trial = 0; trial < 1_000; trial++) {
            int n = random.nextInt(350);
            int[] keys = new int[n];
            int[] ids = new int[n];
            for (int i = 0; i < n; i++) { keys[i] = random.nextInt(51) - 25; ids[i] = i; }
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), tmp = ids[j]; ids[j] = ids[i]; ids[i] = tmp;
            }
            int from = n == 0 ? 0 : random.nextInt(n + 1);
            int to = from + random.nextInt(n - from + 1);
            int k = random.nextInt(to - from + 8);
            int[] before = ids.clone();
            IntBinaryOperator exact = (a, b) -> Integer.compare(keys[a], keys[b]);
            Integer[] oracle = new Integer[to - from];
            for (int i = 0; i < oracle.length; i++) oracle[i] = ids[from + i];
            Arrays.sort(oracle, (a, b) -> {
                int cmp = exact.applyAsInt(a, b);
                return cmp != 0 ? cmp : Integer.compare(a, b);
            });
            int[] actual = M3SortableTopKIds.smallest(ids, from, to, k, exact);
            check(actual.length == Math.min(k, oracle.length), "top-K count");
            for (int i = 0; i < actual.length; i++) check(actual[i] == oracle[i], "oracle mismatch " + trial);
            check(Arrays.equals(ids, before), "source mutation");
        }
    }

    static void duplicatesStableId() {
        int[] ids = {8, 3, 13, 1, 9, 11};
        int[] result = M3SortableTopKIds.smallest(ids, 0, ids.length, 4, (a, b) -> 0);
        check(Arrays.equals(result, new int[]{1, 3, 8, 9}), "tie order by stable ID");
    }

    static void reversedAndMinValueComparator() {
        int[] ids = {0, 1, 2, 3, 4};
        int[] top = M3SortableTopKIds.smallest(ids, 0, ids.length, 3,
                (a, b) -> a == b ? 0 : a > b ? Integer.MIN_VALUE : Integer.MAX_VALUE);
        check(Arrays.equals(top, new int[]{4, 3, 2}), "avoid sign inversion/overflow");
    }

    static void unverifiedFirstSignalCannotOverrideExact() {
        long[] values = {50, 10, 30, 20, 40};
        long[] wrongFirstSignal = {0, 100, 0, 100, 0};
        byte[] cached = new byte[values.length];
        AtomicInteger signalComputations = new AtomicInteger();
        AtomicInteger exactComparisons = new AtomicInteger();
        IntBinaryOperator signalThenExact = (a, b) -> {
            if (cached[a] == 0) { cached[a] = 1; signalComputations.incrementAndGet(); }
            if (cached[b] == 0) { cached[b] = 1; signalComputations.incrementAndGet(); }
            // The provisional signal may disagree with the required final order.
            long signalDelta = wrongFirstSignal[a] - wrongFirstSignal[b];
            if (signalDelta == Long.MIN_VALUE) throw new AssertionError();
            exactComparisons.incrementAndGet();
            return Long.compare(values[a], values[b]);
        };
        int[] ids = {0, 1, 2, 3, 4};
        int[] result = M3SortableTopKIds.smallest(ids, 0, 5, 2, signalThenExact);
        check(Arrays.equals(result, new int[]{1, 3}), "unverified provisional signal changed exact order");
        check(signalComputations.get() <= values.length, "signals recomputed despite cached evidence");
        check(exactComparisons.get() > 0, "exact comparator must run");
    }

    static void callerOwnedWindowAndSourceIdentity() {
        int[] ids = {4, 3, 0, 2, 1};
        int[] out = {91, 91, 91, 91, 91, 91};
        int n = M3SortableTopKIds.fillSmallest(ids, 0, ids.length, 3, out, 2, Integer::compare);
        check(n == 3 && Arrays.equals(out, new int[]{91, 91, 0, 1, 2, 91}), "window");
        check(Arrays.equals(ids, new int[]{4, 3, 0, 2, 1}), "caller source stays in original order");
    }

    static void validatesBeforeWriting() {
        int[] out = {71, 72, 73};
        reject(() -> M3SortableTopKIds.fillSmallest(new int[]{7, -1, 2}, 0, 3, 2,
                out, 0, Integer::compare));
        check(Arrays.equals(out, new int[]{71, 72, 73}), "invalid IDs changed output");
        reject(() -> M3SortableTopKIds.fillSmallest(new int[]{1, 2, 3}, 0, 3, 3,
                out, 1, Integer::compare));
        reject(() -> M3SortableTopKIds.fillSmallest(out, 0, out.length, 2,
                out, 0, Integer::compare));
        reject(() -> M3SortableTopKIds.smallest(new int[]{1}, 0, 1, -1, Integer::compare));
        check(Arrays.equals(out, new int[]{71, 72, 73}), "bad geometry changed output");
    }

    static void zeroWorkNeverCallsComparator() {
        AtomicInteger comparisons = new AtomicInteger();
        int[] out = {8};
        int written = M3SortableTopKIds.fillSmallest(new int[]{1, 2}, 0, 2, 0,
                out, 0, (a, b) -> { comparisons.incrementAndGet(); return 0; });
        check(written == 0 && comparisons.get() == 0 && out[0] == 8, "zero K must do zero comparisons");
        check(M3SortableTopKIds.fillSmallest(new int[]{-1, -2}, 0, 2, 0,
                out, 0, (a, b) -> { throw new AssertionError("zero K comparator"); }) == 0,
                "zero K must not inspect source items");
    }

    static void largeSparseIdsAndLargeWorkset() {
        int n = 50_000;
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = 2 * (n - i);
        int[] result = M3SortableTopKIds.smallest(ids, 0, n, 97, Integer::compare);
        for (int i = 0; i < result.length; i++) check(result[i] == 2 * (i + 1), "large range order");
        check(ids[0] == 2 * n && ids[n - 1] == 2, "input unchanged");
    }

    private static void reject(Runnable call) {
        try { call.run(); } catch (IllegalArgumentException | IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("expected rejection");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
