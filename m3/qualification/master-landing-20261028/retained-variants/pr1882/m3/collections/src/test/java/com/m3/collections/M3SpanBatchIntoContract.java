// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Exact original M3SpanIndex Java21/native contract; no LeetCode source was copied. */
public final class M3SpanBatchIntoContract {
    private static long checks;
    private M3SpanBatchIntoContract() { }

    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why);
    }

    private static void expected(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("expected " + kind.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + kind.getName());
    }


    /**
     * Native LC #88 two-cursor admission and LC #34 fallback, including
     * monotone 256-element pages, tail fragments and unsorted query vectors.
     * The unchanged scalar Java method remains the independent oracle.
     */
    private static void orderedQueryPageDifferential() {
        M3SpanIndex indexed = new M3SpanIndex();
        for (int i = 0; i < 124; i++) {
            int start = (i * 17) % 241;
            indexed.add(start, (long) start + (i % 13) + 1L, i % 3 == 0 ? 1 : 3);
        }
        M3SpanIndex.Snapshot span = indexed.snapshot(1, 0);
        final int count = 1057;
        int[] ascending = new int[count + 5];
        for (int i = 0; i < count; i++) {
            ascending[i + 2] = i == 0 ? Integer.MIN_VALUE
                    : i == 1 ? -1
                    : i == 2 ? 0
                    : i == count - 1 ? Integer.MAX_VALUE
                    : (i - 3) / 3;
        }
        int[] original = ascending.clone();
        for (int pass = 0; pass < 2; pass++) {
            int[] queries = ascending.clone();
            if (pass == 1) {
                // Reverse the actual window only; this forces bisection pages.
                for (int left = 2, right = count + 1; left < right; left++, right--) {
                    int value = queries[left];
                    queries[left] = queries[right];
                    queries[right] = value;
                }
            }
            int[] javaOut = new int[count + 9];
            int[] nativeOut = new int[count + 9];
            Arrays.fill(javaOut, -741);
            Arrays.fill(nativeOut, -741);
            span.earliestCoveringInto(queries, 2, count, javaOut, 4, M3SpanIndex.Backend.JAVA);
            for (int i = 0; i < count; i++) {
                check(javaOut[i + 4] == span.earliestCovering(queries[2 + i]),
                        "ordered/fallback page scalar Java oracle pass=" + pass);
            }
            check(javaOut[3] == -741 && javaOut[count + 4] == -741,
                    "caller-owned padding survives");
            if (M3SpanIndex.nativeAvailable()) {
                span.earliestCoveringInto(queries, 2, count, nativeOut, 4,
                        M3SpanIndex.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(javaOut, nativeOut),
                        "one JNI transition, 256-slot pages, exact Java parity");
            } else {
                expected(UnsatisfiedLinkError.class, () -> span.earliestCoveringInto(
                        queries, 2, count, nativeOut, 4, M3SpanIndex.Backend.NATIVE_REQUIRED));
                check(nativeOut[4] == -741, "missing JNI must never write output");
            }
        }
        check(Arrays.equals(ascending, original), "original source query array unchanged");
    }

    public static long runAll() {
        checks = 0;
        M3SpanIndex owner = new M3SpanIndex();
        M3SpanIndex.Snapshot empty = owner.snapshot(0, 0);
        int[] emptyOut = {37, 38, 39};
        empty.earliestCoveringInto(new int[0], 0, 0, emptyOut, 1, M3SpanIndex.Backend.JAVA);
        check(Arrays.equals(emptyOut, new int[]{37, 38, 39}), "empty must not write");
        expected(NullPointerException.class, () -> empty.earliestCoveringInto(
                null, 0, 0, emptyOut, 1, M3SpanIndex.Backend.JAVA));
        expected(NullPointerException.class, () -> empty.earliestCoveringInto(
                new int[0], 0, 0, null, 0, M3SpanIndex.Backend.JAVA));
        expected(NullPointerException.class, () -> empty.earliestCoveringInto(
                new int[0], 0, 0, emptyOut, 1, null));
        expected(IndexOutOfBoundsException.class, () -> empty.earliestCoveringInto(
                new int[]{1}, -1, 1, emptyOut, 0, M3SpanIndex.Backend.JAVA));
        expected(IndexOutOfBoundsException.class, () -> empty.earliestCoveringInto(
                new int[]{1}, 0, 2, emptyOut, 0, M3SpanIndex.Backend.JAVA));
        expected(IndexOutOfBoundsException.class, () -> empty.earliestCoveringInto(
                new int[]{1}, 0, 1, emptyOut, Integer.MAX_VALUE, M3SpanIndex.Backend.JAVA));
        expected(IndexOutOfBoundsException.class, () -> empty.earliestCoveringInto(
                new int[]{1}, 0, 1, emptyOut, -1, M3SpanIndex.Backend.JAVA));
        int[] alias = {0, 1, 2};
        expected(IllegalArgumentException.class, () -> empty.earliestCoveringInto(
                alias, 0, 0, alias, 0, M3SpanIndex.Backend.JAVA));
        check(Arrays.equals(alias, new int[]{0, 1, 2}), "alias must remain unchanged");
        if (!M3SpanIndex.nativeAvailable()) {
            expected(UnsatisfiedLinkError.class, () -> empty.earliestCoveringInto(
                    new int[0], 0, 0, emptyOut, 0, M3SpanIndex.Backend.NATIVE_REQUIRED));
        } else {
            empty.earliestCoveringInto(new int[0], 0, 0, emptyOut, 0,
                    M3SpanIndex.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(emptyOut, new int[]{37, 38, 39}), "native empty must not write");
        }

        Random random = new Random(0x57A1851L);
        int[] sizes = {0, 1, 2, 3, 31, 127, 255, 256, 257, 513, 1025};
        for (int round = 0; round < 360; round++) {
            M3SpanIndex indexed = new M3SpanIndex();
            int count = random.nextInt(90);
            for (int i = 0; i < count; i++) {
                int start = random.nextInt(140);
                long end = i % 41 == 0 ? Long.MAX_VALUE : (long) start + random.nextInt(140);
                indexed.add(start, end, random.nextInt(8));
            }
            int required = round % 5 == 0 ? 0 : 1 << (round % 3);
            int forbidden = round % 7 == 0 ? required : 0;
            M3SpanIndex.Snapshot span = indexed.snapshot(required, forbidden);
            int n = sizes[round % sizes.length];
            int[] source = new int[n + 5];
            for (int i = 0; i < n; i++) {
                source[2 + i] = switch (i % 13) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    case 2 -> 0;
                    default -> random.nextInt(340) - 20;
                };
            }
            int[] original = source.clone();
            int[] javaOut = new int[n + 9];
            int[] nativeOut = new int[n + 9];
            Arrays.fill(javaOut, -177);
            Arrays.fill(nativeOut, -177);
            span.earliestCoveringInto(source, 2, n, javaOut, 3, M3SpanIndex.Backend.JAVA);
            for (int i = 0; i < n; i++) {
                check(javaOut[i + 3] == span.earliestCovering(source[i + 2]),
                        "Java offset and prefix-binary-search equivalence");
            }
            check(Arrays.equals(source, original), "input query source unchanged");
            check(javaOut[0] == -177 && javaOut[2] == -177
                    && javaOut[javaOut.length - 1] == -177, "untouched Java padding");
            if (M3SpanIndex.nativeAvailable()) {
                span.earliestCoveringInto(source, 2, n, nativeOut, 3,
                        M3SpanIndex.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(javaOut, nativeOut), "exact JNI/Java output with padding");
                check(Arrays.equals(source, original), "JNI must not mutate query input");
            } else {
                expected(UnsatisfiedLinkError.class, () -> span.earliestCoveringInto(
                        source, 2, n, nativeOut, 3, M3SpanIndex.Backend.NATIVE_REQUIRED));
                check(nativeOut[3] == -177, "missing native must not write output");
            }
            int[] base = Arrays.copyOfRange(source, 2, 2 + n);
            int[] oracleJava = span.earliestCoveringBatch(base, M3SpanIndex.Backend.JAVA);
            for (int i = 0; i < n; i++) check(oracleJava[i] == javaOut[i + 3], "old API parity");
            if (M3SpanIndex.nativeAvailable()) {
                check(Arrays.equals(oracleJava,
                        span.earliestCoveringBatch(base, M3SpanIndex.Backend.NATIVE_REQUIRED)),
                        "old JNI export parity");
            }
            if (round % 11 == 0) {
                int[] before = nativeOut.clone();
                expected(IndexOutOfBoundsException.class, () -> span.earliestCoveringInto(
                        source, 2, n, nativeOut, nativeOut.length + 1, M3SpanIndex.Backend.JAVA));
                check(Arrays.equals(before, nativeOut), "all windows prevalidated before writes");
            }
        }
        orderedQueryPageDifferential();
        System.out.println("PASS M3_SPAN_BATCH_INTO checks=" + checks
                + " native=" + M3SpanIndex.nativeAvailable());
        return checks;
    }

    public static void main(String[] ignored) { runAll(); }
}
