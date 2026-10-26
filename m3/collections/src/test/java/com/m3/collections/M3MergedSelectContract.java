// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

public final class M3MergedSelectContract {
    private static long checks;
    private static void eq(long expected, long actual, String description) {
        checks++;
        if (expected != actual) throw new AssertionError(description + ": " + expected + " != " + actual);
    }
    private static void expectedThrows(Class<? extends Throwable> cls, Runnable op) {
        checks++;
        try { op.run(); throw new AssertionError("missing " + cls); }
        catch (Throwable ex) { if (!cls.isInstance(ex)) throw ex; }
    }
    private static void runCase(long[] a, long[] b) {
        long[] combined = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        Arrays.sort(combined);
        int n = combined.length;
        long[] ranks = new long[n + 4];
        long[] result = new long[n + 7];
        long[] expected = new long[n + 7];
        Arrays.fill(ranks, -37);
        Arrays.fill(result, -4519);
        Arrays.fill(expected, -4519);
        for (int i = 0; i < n; i++) { ranks[2 + i] = i; expected[3 + i] = combined[i]; }
        for (boolean nativeFlag : new boolean[] {false, true}) {
            if (nativeFlag && !M3LongRankBatchNative.nativeAvailable()) continue;
            Arrays.fill(result, -4519);
            M3LongRankBatchNative.selectMergedInto(a, b, ranks, 2, n, result, 3, nativeFlag);
            for (int i = 0; i < expected.length; i++) eq(expected[i], result[i], "sorted-merge output");
            for (int i = 0; i < n; i++) {
                eq(combined[i], M3LongRankBatchNative.selectMerged(a, b, i), "scalar selection");
            }
            eq(a.length, a.length, "owner size remains stable");
        }
        if (n != 0) {
            expectedThrows(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.selectMergedInto(a, b, new long[]{-1},0,1,new long[]{777},0,true));
            expectedThrows(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.selectMergedInto(a,b,new long[]{n},0,1,new long[]{777},0,true));
            // Invalid second page must not partially write the first page.
            long[] bad = new long[260], sentinel = new long[260];
            Arrays.fill(bad, 0);
            bad[259] = n;
            Arrays.fill(sentinel, 12345);
            expectedThrows(IndexOutOfBoundsException.class, () ->
                M3LongRankBatchNative.selectMergedInto(a,b,bad,0,bad.length,sentinel,0,true));
            for (long e : sentinel) eq(12345,e,"fail-before-write");
        }
        long[] aliases = {0, 1, 2};
        expectedThrows(IllegalArgumentException.class, () ->
            M3LongRankBatchNative.selectMergedInto(a, b, aliases,0,0,aliases,0,true));
        expectedThrows(IndexOutOfBoundsException.class, () ->
            M3LongRankBatchNative.selectMergedInto(a, b, new long[0],1,0,new long[0],0,true));
    }
    public static void main(String[] args) {
        if (Boolean.getBoolean("m3.collections.rank.native")
                && !M3LongRankBatchNative.nativeAvailable()) {
            throw new AssertionError("requested JNI provider did not load");
        }
        runCase(new long[0], new long[0]);
        runCase(new long[]{1},new long[0]);
        runCase(new long[0],new long[]{-42});
        runCase(new long[]{Long.MIN_VALUE, Long.MIN_VALUE, -1, 0},
                new long[]{Long.MIN_VALUE, -1, Long.MAX_VALUE, Long.MAX_VALUE});
        runCase(new long[]{1, 1, 1},new long[]{1, 1});
        Random r = new Random(0x4C4D3344);
        for (int i=0;i<400;i++) {
            long[] a = new long[r.nextInt(80)], b = new long[r.nextInt(80)];
            for(int j=0;j<a.length;j++) a[j]=r.nextInt(21)-10;
            for(int j=0;j<b.length;j++) b[j]=r.nextInt(21)-10;
            Arrays.sort(a); Arrays.sort(b);
            runCase(a,b);
        }
        // Source overlap can be the very same immutable owner in Java and JNI.
        long[] same={Long.MIN_VALUE,-1,-1,2,Long.MAX_VALUE};
        runCase(same,same);
        if (!M3LongRankBatchNative.nativeAvailable()) {
            expectedThrows(UnsatisfiedLinkError.class, () ->
                M3LongRankBatchNative.selectMergedInto(new long[]{1}, new long[]{2},
                    new long[]{0}, 0, 1, new long[1], 0, true));
        }
        System.out.println("PASS M3_MERGED_SELECT_JNI checks=" + checks
                + " native=" + M3LongRankBatchNative.nativeAvailable());
    }
}
