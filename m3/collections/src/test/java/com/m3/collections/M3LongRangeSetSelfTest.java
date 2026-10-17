/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Random;

/** Finite deterministic oracle for disjoint array intervals, offsets, and mask selection. */
public final class M3LongRangeSetSelfTest {
    private static long checks;
    private M3LongRangeSetSelfTest() { }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    private static void compare(M3LongRangeSet actual, boolean[] reference, String label) {
        actual.verifyInvariants();
        long[] starts = actual.startsToArray();
        long[] ends = actual.endsToArray();
        check(starts.length == ends.length && starts.length == actual.rangeCount(), label + " aligned");
        int expectedRanges = 0;
        boolean running = false;
        int active = 0;
        for (int i = 0; i < reference.length; i++) {
            if (reference[i]) active++;
            if (reference[i] != running) {
                if (reference[i]) expectedRanges++;
                running = reference[i];
            }
            check(actual.contains(i - 128L) == reference[i], label + " member " + i);
        }
        check(expectedRanges == actual.rangeCount(), label + " rangeCount");
        check(actual.cardinality().equals(BigInteger.valueOf(active)), label + " cardinality");
        for (int i = 0; i < starts.length; i++) {
            check(starts[i] < ends[i], label + " nonempty");
            if (i > 0) check(ends[i - 1] < starts[i], label + " nonadjacent");
        }
        M3LongRangeSet snapshot = actual.snapshot();
        check(Arrays.equals(snapshot.startsToArray(), starts), label + " clone starts");
        check(Arrays.equals(snapshot.endsToArray(), ends), label + " clone ends");
    }

    private static void seeded() {
        M3LongRangeSet actual = new M3LongRangeSet();
        boolean[] ref = new boolean[256];
        Random random = new Random(0x4D3352414E4745L);
        for (int operation = 0; operation < 20_000; operation++) {
            int i = random.nextInt(257);
            int j = random.nextInt(257);
            int from = Math.min(i, j);
            int to = Math.max(i, j);
            long lower = from - 128L;
            long upper = to - 128L;
            int mode = random.nextInt(9);
            if (mode < 4) {
                actual.add(lower, upper);
                Arrays.fill(ref, from, to, true);
            } else if (mode < 8) {
                actual.remove(lower, upper);
                Arrays.fill(ref, from, to, false);
            } else {
                boolean expected = false;
                for (int k = from; k < to; k++) expected |= ref[k];
                check(actual.intersects(lower, upper) == expected, "intersects " + operation);
            }
            if (operation % 13 == 0) compare(actual, ref, "operation=" + operation);
        }
        compare(actual, ref, "last");
        actual.clear();
        check(actual.isEmpty() && actual.cardinality().signum() == 0, "clear");
    }

    private static void extremesAndStateMasks() {
        M3LongRangeSet set = new M3LongRangeSet();
        set.add(Long.MIN_VALUE, Long.MAX_VALUE);
        check(set.contains(Long.MIN_VALUE), "MIN member");
        check(set.contains(Long.MAX_VALUE - 1), "MAX-1 member");
        check(!set.contains(Long.MAX_VALUE), "exclusive end");
        check(set.cardinality().equals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)),
                "exact beyond signed long cardinality");
        set.remove(-5, 6);
        check(set.rangeCount() == 2, "split extreme interval");
        check(set.contains(-6) && !set.contains(5) && set.contains(6), "split contents");
        set.remove(Long.MIN_VALUE, Long.MAX_VALUE);
        check(set.isEmpty(), "total remove");
        set.add(0, 10);
        set.add(20, 30);
        set.add(40, 50);
        M3LongRangeSet selected = set.selectedByState(new int[] {0x80000001, 0x00000001, 0x80000003},
                0x80000001, 0x00000002, M3IndexPathMasks.Backend.JAVA);
        check(Arrays.equals(new long[] {0}, selected.startsToArray()), "32-bit mask start");
        check(Arrays.equals(new long[] {10}, selected.endsToArray()), "32-bit mask end");
        int[] flags = new int[] {1, 0, 1};
        M3LongRangeSet other = set.selectedByState(flags, 1, 0, M3IndexPathMasks.Backend.JAVA);
        check(Arrays.equals(new long[] {0, 40}, other.startsToArray()), "mask skip midpoint");
        M3LongRangeSet untouched = set.snapshot();
        int[] count = {0};
        untouched.forEachRange((start, end) -> count[0]++);
        check(count[0] == 3, "primitive consumer");
        boolean detected = false;
        try {
            set.forEachRange((start, end) -> set.remove(start, end));
        } catch (ConcurrentModificationException expected) { detected = true; }
        check(detected, "structural mutation rejected after callback");
        check(untouched.rangeCount() == 3, "detached snapshot");
        boolean refused = false;
        try { set.add(20, 1); } catch (IllegalArgumentException expected) { refused = true; }
        check(refused, "reversed range refusal");
        refused = false;
        try { set.selectedByState(new int[0], 1, 0, M3IndexPathMasks.Backend.JAVA); }
        catch (IllegalArgumentException expected) { refused = true; }
        check(refused, "unaligned state mask refusal");
        if (M3IndexPathMasks.nativeAvailable()) {
            M3LongRangeSet nativeSelected = set.selectedByState(
                    new int[set.rangeCount()], 0, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            check(nativeSelected.rangeCount() == set.rangeCount(), "native selected all");
        } else {
            refused = false;
            try { set.selectedByState(new int[set.rangeCount()], 0, 0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED); }
            catch (UnsatisfiedLinkError expected) { refused = true; }
            check(refused, "required native failure not silently demoted");
        }
    }

    public static void run() {
        checks = 0;
        seeded();
        extremesAndStateMasks();
        System.out.println("PASS M3LongRangeSetSelfTest oracleChecks=" + checks);
    }

    public static void main(String[] args) { run(); }
}
