/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;

/**
 * Exact pure-Java oracle for the existing packed-mask kernel and optional JNI peer.
 * JNI execution requires a host-owned explicit System.load of a built Synexia native
 * selection library. No native result is claimed if only the Java lane is run.
 */
public final class FilteredLongSnapshotsContract {
    private FilteredLongSnapshotsContract() { }
    private static int fixtures;

    private static void match(long[] input, LongPredicate filter,
            FilteredLongSnapshots.Backend backend) {
        long[] original = input.clone();
        AtomicInteger calls = new AtomicInteger();
        long[] output = FilteredLongSnapshots.filterSorted(input, value -> {
            calls.incrementAndGet();
            return filter.test(value);
        }, backend);
        long[] expected = Arrays.stream(input).filter(filter).toArray();
        if (!Arrays.equals(expected, output)) throw new AssertionError("compaction parity");
        if (!Arrays.equals(original, input)) throw new AssertionError("mutated input");
        if (calls.get() != input.length) throw new AssertionError("not exactly one predicate evaluation");
        fixtures++;
    }

    public static void run(FilteredLongSnapshots.Backend backend) {
        match(new long[0], x -> true, backend);
        match(new long[] {-7L, -7L, 0L, 0L, 2L, Long.MAX_VALUE}, x -> x <= 0L, backend);
        match(new long[] {Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE},
                x -> x == Long.MIN_VALUE, backend);
        match(new long[] {5, 5, 5}, x -> false, backend);
        Random r = new Random(0x2026_1016_5E7L);
        for (int t = 0; t < 3000; t++) {
            long[] data = new long[r.nextInt(700)];
            for (int i = 0; i < data.length; i++) data[i] = r.nextInt(300) - 150;
            Arrays.sort(data);
            int bound = r.nextInt(400) - 200;
            match(data, x -> x < bound && (x & 1L) == 0L, backend);
        }
        long[] invalid = new long[] {4, 1};
        AtomicInteger evaluated = new AtomicInteger();
        try {
            FilteredLongSnapshots.filterSorted(invalid,
                    x -> { evaluated.incrementAndGet(); return true; }, backend);
            throw new AssertionError("unsorted accepted");
        } catch (IllegalArgumentException expected) {
            if (evaluated.get() != 0) throw new AssertionError("predicate ran before preflight");
        }
        try {
            FilteredLongSnapshots.filterSorted(new long[] {1}, x -> {
                throw new IllegalStateException("predicate failure");
            }, backend);
            throw new AssertionError("predicate failure suppressed");
        } catch (IllegalStateException expected) {
            if (!"predicate failure".equals(expected.getMessage())) throw expected;
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args.length != 2 || !"jni".equals(args[0])) {
                throw new IllegalArgumentException("usage: [jni <absolute-library-path>]");
            }
            System.load(args[1]);
            run(FilteredLongSnapshots.Backend.JNI_REQUIRED);
            System.out.println("PASS M3_FILTERED_SNAPSHOTS backend=JNI fixtures=" + fixtures);
        } else {
            run(FilteredLongSnapshots.Backend.JAVA);
            System.out.println("PASS M3_FILTERED_SNAPSHOTS backend=JAVA fixtures=" + fixtures);
        }
    }
}
