/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Dependency-free Java 21 differential oracle and JNI parity test. */
public final class M3SpanIndexSelfTest {
    private static long checks;

    private M3SpanIndexSelfTest() { }

    private static void check(boolean yes, String what) {
        checks++;
        if (!yes) throw new AssertionError(what);
    }

    private static void failExpected(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("expected " + expected.getName() + " got " + error, error);
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static int oracle(int target, ArrayList<int[]> intervals,
            int required, int forbidden) {
        if (target <= 0) return Math.max(0, target);
        int best = target;
        for (int[] span : intervals) {
            long end = ((long) span[1] << 32) | (span[2] & 0xffffffffL);
            int flags = span[3];
            if (span[0] < target && end >= target && end > span[0]
                    && (flags & required) == required && (flags & forbidden) == 0) {
                best = Math.min(best, span[0]);
            }
        }
        return best;
    }

    private static int[] span(int start, long inclusiveEnd, int mask) {
        return new int[] {start, (int) (inclusiveEnd >>> 32), (int) inclusiveEnd, mask};
    }

    private static void verify(M3SpanIndex owner, ArrayList<int[]> input,
            int required, int forbidden, Random rnd) {
        M3SpanIndex.Snapshot snap = owner.snapshot(required, forbidden);
        int selected = 0;
        for (int i = 0; i < input.size(); i++) {
            int[] span = input.get(i);
            long end = ((long) span[1] << 32) | (span[2] & 0xffffffffL);
            if (end > span[0] && (span[3] & required) == required
                    && (span[3] & forbidden) == 0) selected++;
        }
        check(snap.size() == selected, "snapshot length");
        int previousStart = -1;
        int previousOrdinal = -1;
        long previousMax = Long.MIN_VALUE;
        for (int i = 0; i < selected; i++) {
            int at = snap.sourceOrdinalAt(i);
            int[] current = input.get(at);
            int start = snap.startAt(i);
            long end = ((long) current[1] << 32) | (current[2] & 0xffffffffL);
            check(start == current[0], "span source alignment");
            check(start >= previousStart, "sorted starts");
            if (start == previousStart) check(at > previousOrdinal, "stable equal starts");
            previousStart = start;
            previousOrdinal = at;
            previousMax = Math.max(previousMax, end);
            check(snap.prefixMaximumEndAt(i) == previousMax, "prefix maximum");
        }
        int[] targets = new int[44];
        targets[0] = Integer.MIN_VALUE;
        targets[1] = -1;
        targets[2] = 0;
        targets[3] = 1;
        targets[4] = Integer.MAX_VALUE - 1;
        targets[5] = Integer.MAX_VALUE;
        for (int i = 6; i < targets.length; i++) targets[i] = rnd.nextInt(1600);
        int[] java = snap.earliestCoveringBatch(targets, M3SpanIndex.Backend.JAVA);
        for (int i = 0; i < targets.length; i++) {
            int want = oracle(targets[i], input, required, forbidden);
            check(snap.earliestCovering(targets[i]) == want, "Java span oracle row=" + targets[i]);
            check(java[i] == want, "Java batch equivalence");
        }
        if (M3SpanIndex.nativeAvailable()) {
            int[] nativeResult = snap.earliestCoveringBatch(targets, M3SpanIndex.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(java, nativeResult), "JNI exact primitive parity");
        } else {
            failExpected(UnsatisfiedLinkError.class,
                    () -> snap.earliestCoveringBatch(targets, M3SpanIndex.Backend.NATIVE_REQUIRED));
        }
    }

    public static long runAll() {
        checks = 0;
        M3SpanIndex empty = new M3SpanIndex();
        check(empty.size() == 0 && empty.snapshot(0, 0).earliestCovering(18) == 18, "empty");
        failExpected(IllegalArgumentException.class, () -> empty.add(-1, 1, 0));
        failExpected(IllegalArgumentException.class, () -> empty.add(9, 8, 0));
        failExpected(IndexOutOfBoundsException.class, () -> empty.removeAt(0));
        failExpected(NullPointerException.class,
                () -> empty.snapshot(0, 0).earliestCoveringBatch(null, M3SpanIndex.Backend.JAVA));
        failExpected(NullPointerException.class,
                () -> empty.snapshot(0, 0).earliestCoveringBatch(new int[0], null));

        M3SpanIndex owner = new M3SpanIndex();
        ArrayList<int[]> oracle = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            int start = (i * 7) % 31;
            long end = (long) start + (i % 9);
            int bits = (i % 2 == 0 ? 1 : 2) | (i % 5 == 0 ? 0x80000000 : 0);
            int ordinal = owner.add(start, end, bits);
            check(ordinal == i, "append ordinal");
            oracle.add(span(start, end, bits));
        }
        verify(owner, oracle, 0, 0, new Random(1));
        verify(owner, oracle, 1, 2, new Random(2));
        verify(owner, oracle, 0x80000000, 0, new Random(3));
        verify(owner, oracle, 2, 2, new Random(4));
        M3SpanIndex.Snapshot old = owner.snapshot(1, 0);
        int originalFirst = old.earliestCovering(20);
        owner.removeAt(1);
        oracle.remove(1);
        owner.set(2, 0, Long.MAX_VALUE, -1);
        oracle.set(2, span(0, Long.MAX_VALUE, -1));
        check(old.earliestCovering(20) == originalFirst, "snapshot unchanged by mutation");
        verify(owner, oracle, 0, 0, new Random(5));
        verify(owner, oracle, 1, 0, new Random(6));
        check(owner.startAt(2) == 0 && owner.endAt(2) == Long.MAX_VALUE,
                "owner extreme interval");

        Random random = new Random(0x6D335350414E2AL);
        for (int iteration = 0; iteration < 850; iteration++) {
            M3SpanIndex dynamic = new M3SpanIndex();
            ArrayList<int[]> values = new ArrayList<>();
            int count = random.nextInt(130);
            for (int i = 0; i < count; i++) {
                int start = random.nextInt(800);
                long end = (long) start + random.nextInt(120);
                int state = random.nextInt();
                dynamic.add(start, end, state);
                values.add(span(start, end, state));
            }
            int required = iteration % 4 == 0 ? 0 : 1 << random.nextInt(32);
            int forbidden = iteration % 7 == 0 ? required : 1 << random.nextInt(32);
            verify(dynamic, values, required, forbidden, random);
        }
        // Large sparse frontier validates asymptotic sort without benchmarking the widget.
        M3SpanIndex large = new M3SpanIndex();
        for (int row = 65_535; row >= 0; row--) {
            large.add(row, (long) row + 50L, 1);
        }
        M3SpanIndex.Snapshot projected = large.snapshot(1, 0);
        check(projected.size() == 65_536, "large sorted span projection");
        check(projected.earliestCovering(200) == 150, "large earliest span");
        check(projected.startAt(0) == 0, "large sorted ascending");
        check(projected.sourceOrdinalAt(0) == 65_535, "large exact source ordinal");
        failExpected(IndexOutOfBoundsException.class, () -> projected.sourceOrdinalAt(-1));
        long total = checks;
        System.out.println("M3SpanIndex Java21/JNI oracle: PASS " + total
                + " assertions, JNI=" + M3SpanIndex.nativeAvailable());
        return total;
    }

    public static void main(String[] args) { runAll(); }
}
