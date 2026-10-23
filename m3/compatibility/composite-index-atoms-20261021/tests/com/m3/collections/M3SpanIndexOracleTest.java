/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Exhaustive small geometry and seeded differential tests on the real Java/JNI owner. */
public final class M3SpanIndexOracleTest {
    private static long checks;
    private M3SpanIndexOracleTest() { }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static int oracle(int[] starts, long[] ends, int[] states, int row, int required, int forbidden) {
        if (row <= 0) return Math.max(0, row);
        int best = row;
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] < row && row <= ends[i] && (states[i] & required) == required
                    && (states[i] & forbidden) == 0) best = Math.min(best, starts[i]);
        }
        return best;
    }
    private static void verify(int[] starts, long[] ends, int[] flags, int required, int forbidden, int[] rows) {
        M3SpanIndex index = new M3SpanIndex();
        for (int i = 0; i < starts.length; i++) check(index.add(starts[i], ends[i], flags[i]) == i, "ordinal");
        M3SpanIndex.Snapshot snapshot = index.snapshot(required, forbidden);
        int[] expected = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            expected[i] = oracle(starts, ends, flags, rows[i], required, forbidden);
            check(snapshot.earliestCovering(rows[i]) == expected[i], "scalar query=" + rows[i]);
        }
        int[] before = rows.clone();
        check(Arrays.equals(snapshot.earliestCoveringBatch(rows, M3SpanIndex.Backend.JAVA), expected), "Java batch");
        check(Arrays.equals(snapshot.earliestCoveringBatch(rows, M3SpanIndex.Backend.NATIVE_REQUIRED), expected), "native batch");
        check(Arrays.equals(rows, before), "query mutation");
        int previousStart = -1, previousOrdinal = -1;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < snapshot.size(); i++) {
            int at = snapshot.sourceOrdinalAt(i);
            check((flags[at] & required) == required && (flags[at] & forbidden) == 0 && ends[at] > starts[at], "mask");
            check(starts[at] >= previousStart, "sort order");
            if (starts[at] == previousStart) check(at > previousOrdinal, "stable ties");
            max = Math.max(max, ends[at]);
            check(snapshot.prefixMaximumEndAt(i) == max, "prefix max");
            previousStart = starts[at]; previousOrdinal = at;
        }
        index.clear();
        index.add(0, Long.MAX_VALUE, -1);
        check(Arrays.equals(snapshot.earliestCoveringBatch(rows, M3SpanIndex.Backend.JAVA), expected), "detached view");
        check(Arrays.equals(snapshot.earliestCoveringBatch(rows, M3SpanIndex.Backend.NATIVE_REQUIRED), expected), "detached JNI");
        check(snapshot.earliestCoveringBatch(new int[0], M3SpanIndex.Backend.NATIVE_REQUIRED).length == 0, "empty queries");
    }
    public static void main(String[] args) {
        check(M3SpanIndex.nativeAvailable(), "required native backend absent");
        int[] boundaryRows = {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4, 5, 6, Integer.MAX_VALUE};
        // Every pair of small intervals, including duplicate starts and empty spans.
        for (int a = 0; a < 5; a++) for (int ae = a; ae <= 5; ae++) {
            for (int b = 0; b < 5; b++) for (int be = b; be <= 5; be++) {
                verify(new int[] {a, b}, new long[] {ae, be}, new int[] {1, 2}, 0, 0, boundaryRows);
            }
        }
        Random random = new Random(0x4d335350414eL);
        for (int trial = 0; trial < 1500; trial++) {
            int n = random.nextInt(129);
            int[] starts = new int[n], flags = new int[n];
            long[] ends = new long[n];
            for (int i = 0; i < n; i++) {
                starts[i] = random.nextInt(256);
                ends[i] = starts[i] + random.nextInt(512);
                if (i % 29 == 0) ends[i] = Long.MAX_VALUE;
                flags[i] = random.nextInt();
            }
            int[] rows = new int[260];
            for (int i = 0; i < rows.length; i++) rows[i] = random.nextInt(768) - 12;
            rows[0] = Integer.MAX_VALUE; rows[1] = Integer.MIN_VALUE;
            int required = trial % 3 == 0 ? 0 : 1 << (trial % 32);
            int forbidden = trial % 7 == 0 ? required : 1 << ((trial + 1) % 32);
            verify(starts, ends, flags, required, forbidden, rows);
        }
        verify(new int[] {Integer.MAX_VALUE, 1, 1}, new long[] {Long.MAX_VALUE, 2, Long.MAX_VALUE},
                new int[] {-1, -1, -1}, Integer.MIN_VALUE, 0, boundaryRows);
        System.out.println("PASS span assertions=" + checks);
    }
}
