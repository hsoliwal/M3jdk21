// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/** Independent finite boolean-span oracle for detached half-open range algebra. */
public final class M3LongRangeAlgebraSelfTest {
    private static long checks;
    private static long pairs;

    private M3LongRangeAlgebraSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void same(M3LongRangeSet expected, M3LongRangeSet actual, String label) {
        actual.verifyInvariants();
        check(Arrays.equals(expected.startsToArray(), actual.startsToArray()), label + " starts");
        check(Arrays.equals(expected.endsToArray(), actual.endsToArray()), label + " ends");
        check(expected.cardinality().equals(actual.cardinality()), label + " count");
    }

    private static M3LongRangeSet ranges(long... endpoints) {
        M3LongRangeSet result = new M3LongRangeSet();
        for (int i = 0; i < endpoints.length; i += 2) result.add(endpoints[i], endpoints[i + 1]);
        return result;
    }

    private static boolean[] bits(int value, int length) {
        boolean[] result = new boolean[length];
        for (int i = 0; i < length; i++) result[i] = (value & (1 << i)) != 0;
        return result;
    }

    private static M3LongRangeSet fromGrid(long[] coordinates, boolean[] cells, int capacity) {
        M3LongRangeSet result = new M3LongRangeSet(capacity);
        for (int i = 0; i < cells.length; i++) {
            if (cells[i]) result.add(coordinates[i], coordinates[i + 1]);
        }
        return result;
    }

    private static boolean[] combine(boolean[] left, boolean[] right, int operation) {
        boolean[] result = new boolean[left.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = switch (operation) {
                case 0 -> left[i] || right[i];
                case 1 -> left[i] && right[i];
                case 2 -> left[i] && !right[i];
                default -> throw new AssertionError("unknown boolean operation");
            };
        }
        return result;
    }

    private static void compare(M3LongRangeSet actual, long[] coordinates,
            boolean[] expected, String label) {
        actual.verifyInvariants();
        long[] expectedStarts = new long[expected.length];
        long[] expectedEnds = new long[expected.length];
        BigInteger count = BigInteger.ZERO;
        int rangeCount = 0;
        for (int i = 0; i < expected.length; i++) {
            check(actual.contains(coordinates[i]) == expected[i], label + " cell " + i);
            if (!expected[i]) continue;
            count = count.add(BigInteger.valueOf(coordinates[i + 1])
                    .subtract(BigInteger.valueOf(coordinates[i])));
            if (i == 0 || !expected[i - 1]) expectedStarts[rangeCount] = coordinates[i];
            if (i + 1 == expected.length || !expected[i + 1]) {
                expectedEnds[rangeCount++] = coordinates[i + 1];
            }
        }
        check(!actual.contains(coordinates[coordinates.length - 1]), label + " exclusive terminal");
        check(Arrays.equals(Arrays.copyOf(expectedStarts, rangeCount), actual.startsToArray()),
                label + " exact start lane");
        check(Arrays.equals(Arrays.copyOf(expectedEnds, rangeCount), actual.endsToArray()),
                label + " exact end lane");
        check(actual.rangeCount() == rangeCount, label + " normalized range count");
        check(actual.cardinality().equals(count), label + " BigInteger cardinality");
    }

    private static void pair(long[] coordinates, boolean[] leftCells, boolean[] rightCells,
            int spareCapacity, boolean laws) {
        pairs++;
        M3LongRangeSet left = fromGrid(coordinates, leftCells, spareCapacity);
        M3LongRangeSet right = fromGrid(coordinates, rightCells, spareCapacity + 3);
        M3LongRangeSet leftBefore = left.snapshot();
        M3LongRangeSet rightBefore = right.snapshot();
        M3LongRangeSet union = left.union(right);
        M3LongRangeSet intersection = left.intersection(right);
        M3LongRangeSet difference = left.difference(right);
        compare(union, coordinates, combine(leftCells, rightCells, 0), "union");
        compare(intersection, coordinates, combine(leftCells, rightCells, 1), "intersection");
        compare(difference, coordinates, combine(leftCells, rightCells, 2), "difference");
        check(union != left && union != right, "union owner detached");
        check(intersection != left && intersection != right, "intersection owner detached");
        check(difference != left && difference != right, "difference owner detached");
        same(leftBefore, left, "left source unchanged");
        same(rightBefore, right, "right source unchanged");
        if (laws) {
            same(union, right.union(left), "union commutative");
            same(intersection, right.intersection(left), "intersection commutative");
            same(left, left.union(left), "union idempotent");
            same(left, left.intersection(left), "intersection idempotent");
            check(left.difference(left).isEmpty(), "self difference empty");
            same(left, difference.union(intersection), "difference partitions source");
            check(difference.intersection(right).isEmpty(), "difference disjoint from right");
            check(left.cardinality().add(right.cardinality())
                    .equals(union.cardinality().add(intersection.cardinality())),
                    "inclusion exclusion cardinality");
        }
    }

    private static void exhaustive() {
        long[] coordinates = {-3, -2, -1, 0, 1, 2, 3};
        // Every subset includes all single intervals and all normalized multi-interval shapes.
        for (int left = 0; left < 64; left++) {
            for (int right = 0; right < 64; right++) {
                pair(coordinates, bits(left, 6), bits(right, 6), (left ^ right) % 11, true);
            }
        }
    }

    private static void randomized() {
        Random random = new Random(0x4d3352414c474542L);
        long[] coordinates = new long[65];
        for (int i = 0; i < coordinates.length; i++) coordinates[i] = i - 32L;
        for (int trial = 0; trial < 1_200; trial++) {
            boolean[] a = new boolean[64];
            boolean[] b = new boolean[64];
            boolean[] c = new boolean[64];
            for (int i = 0; i < a.length; i++) {
                a[i] = random.nextBoolean();
                b[i] = random.nextBoolean();
                c[i] = random.nextBoolean();
            }
            pair(coordinates, a, b, random.nextInt(25), true);
            if (trial < 256) {
                M3LongRangeSet left = fromGrid(coordinates, a, 0);
                M3LongRangeSet right = fromGrid(coordinates, b, 0);
                M3LongRangeSet third = fromGrid(coordinates, c, 0);
                same(left.union(right).union(third), left.union(right.union(third)),
                        "union associative");
                same(left.intersection(right).intersection(third),
                        left.intersection(right.intersection(third)), "intersection associative");
                same(left.intersection(right.union(third)),
                        left.intersection(right).union(left.intersection(third)), "distributive");
                same(left.difference(right.union(third)),
                        left.difference(right).difference(third), "successive subtraction");
            }
        }
    }

    private static void boundaries() {
        M3LongRangeSet empty = new M3LongRangeSet(50);
        M3LongRangeSet left = ranges(1, 5, 10, 20);
        M3LongRangeSet bridge = ranges(5, 10);
        same(ranges(1, 20), left.union(bridge), "touching ranges merge");
        check(left.intersection(bridge).isEmpty(), "touching ranges do not intersect");
        same(left, left.difference(bridge), "touching subtraction unchanged");
        same(left, left.union(empty), "union right empty");
        same(left, empty.union(left), "union left empty");
        same(left, left.difference(empty), "difference right empty");
        check(empty.difference(left).isEmpty(), "difference left empty");
        check(left.intersection(empty).isEmpty(), "intersection right empty");
        check(empty.intersection(left).isEmpty(), "intersection left empty");
        check(empty.union(empty).isEmpty(), "both empty union");
        same(ranges(2, 6, 8, 10, 12, 14),
                ranges(0, 6, 8, 14).intersection(ranges(2, 10, 12, 20)),
                "intersection can outnumber either input");
        same(ranges(0, 1, 4, 5, 9, 10),
                ranges(0, 5, 7, 10).difference(ranges(1, 4, 5, 9)), "split and span cuts");
        check(ranges(0, 2, 4, 6, 8, 10).difference(ranges(-1, 20)).isEmpty(),
                "one cut covers multiple source intervals");
        same(ranges(Long.MIN_VALUE, 0, 1, Long.MAX_VALUE),
                ranges(Long.MIN_VALUE, Long.MAX_VALUE).difference(ranges(0, 1)),
                "wide difference uses exact endpoints");
        same(ranges(Long.MIN_VALUE, Long.MAX_VALUE),
                ranges(Long.MIN_VALUE, 0).union(ranges(0, Long.MAX_VALUE)), "wide adjacent union");
        check(ranges(Long.MIN_VALUE, Long.MAX_VALUE).cardinality()
                .equals(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)), "wide cardinality");
        for (int operation = 0; operation < 3; operation++) {
            boolean refused = false;
            try {
                switch (operation) {
                    case 0 -> left.union(null);
                    case 1 -> left.intersection(null);
                    case 2 -> left.difference(null);
                    default -> throw new AssertionError();
                }
            } catch (NullPointerException expected) { refused = true; }
            check(refused, "null operand refused for " + operation);
            same(ranges(1, 5, 10, 20), left, "null leaves receiver unchanged");
        }
    }

    private static void wideGrids() {
        long[] coordinates = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -10, -1,
                0, 1, 10, Long.MAX_VALUE - 1, Long.MAX_VALUE};
        Random random = new Random(0x5749444552414e47L);
        for (int i = 0; i < 512; i++) {
            pair(coordinates, bits(random.nextInt(256), 8), bits(random.nextInt(256), 8), 0, true);
        }
        pair(coordinates, bits(255, 8), bits(0, 8), 0, true);
        pair(coordinates, bits(255, 8), bits(255, 8), 0, true);
        pair(coordinates, bits(85, 8), bits(170, 8), 0, true);
    }

    private static void fragmentedGrowth() {
        long[] coordinates = new long[8_194];
        for (int i = 0; i < coordinates.length; i++) coordinates[i] = i;
        boolean[] whole = new boolean[8_193];
        boolean[] holes = new boolean[8_193];
        Arrays.fill(whole, true);
        for (int i = 1; i < holes.length; i += 2) holes[i] = true;
        pair(coordinates, whole, holes, 0, true);
        boolean[] even = new boolean[8_193];
        for (int i = 0; i < even.length; i += 4) even[i] = true;
        boolean[] shifted = new boolean[8_193];
        for (int i = 2; i < shifted.length; i += 4) shifted[i] = true;
        pair(coordinates, even, shifted, 0, true);
    }

    private static void detachedMutation() {
        for (int operation = 0; operation < 3; operation++) {
            M3LongRangeSet left = ranges(-20, -5, 0, 10, 20, 40);
            M3LongRangeSet right = ranges(-10, 2, 5, 25);
            M3LongRangeSet leftBefore = left.snapshot();
            M3LongRangeSet rightBefore = right.snapshot();
            M3LongRangeSet result = apply(left, right, operation);
            M3LongRangeSet retained = result.snapshot();
            result.remove(-30, 30);
            result.add(100, 110);
            same(leftBefore, left, "result mutation leaves left");
            same(rightBefore, right, "result mutation leaves right");
            result = apply(left, right, operation);
            left.clear();
            right.remove(-100, 100);
            left.add(200, 210);
            same(retained, result, "source mutation leaves result");
            result.clear();
            result.add(Long.MIN_VALUE, Long.MAX_VALUE);
            check(result.rangeCount() == 1, "result remains mutable");
        }
        M3LongRangeSet source = ranges(0, 10, 20, 30, 40, 50);
        // Reading through algebra must not change the receiver's iteration revision.
        source.forEachRange((start, end) -> {
            same(source, source.union(source), "read-only union in callback");
            same(source, source.intersection(source), "read-only intersection in callback");
            check(source.difference(source).isEmpty(), "read-only difference in callback");
        });
    }

    private static M3LongRangeSet apply(M3LongRangeSet left, M3LongRangeSet right, int operation) {
        return switch (operation) {
            case 0 -> left.union(right);
            case 1 -> left.intersection(right);
            case 2 -> left.difference(right);
            default -> throw new AssertionError("unknown range operation");
        };
    }

    private static void stateComposition(boolean nativeLane) {
        M3LongRangeSet left = ranges(Long.MIN_VALUE, -100, -80, -60, -40, -20,
                0, 20, 40, 60, 80, Long.MAX_VALUE);
        M3LongRangeSet right = ranges(-110, -90, -70, -50, -30, -10, 10, 30, 50, 70, 90, 100);
        int[] words = {0x80000001, 0x80000003, 0x7fffffff, 0, Integer.MIN_VALUE, -1, 0x40000000};
        int[] required = {0, Integer.MIN_VALUE, 1, 0x80000001, -1, 0x40000000};
        int[] forbidden = {0, 2, Integer.MIN_VALUE, 0x40000000, -1};
        for (int operation = 0; operation < 3; operation++) {
            M3LongRangeSet result = apply(left, right, operation);
            M3LongRangeSet before = result.snapshot();
            long[] starts = result.startsToArray();
            long[] ends = result.endsToArray();
            int[] states = new int[result.rangeCount()];
            for (int i = 0; i < states.length; i++) states[i] = words[i % words.length];
            int[] stateCopy = states.clone();
            for (int need : required) {
                for (int deny : forbidden) {
                    M3LongRangeSet expected = new M3LongRangeSet();
                    for (int i = 0; i < states.length; i++) {
                        if ((states[i] & need) == need && (states[i] & deny) == 0) {
                            expected.add(starts[i], ends[i]);
                        }
                    }
                    M3LongRangeSet selected = result.selectedByState(states, need, deny,
                            M3IndexPathMasks.Backend.JAVA);
                    same(expected, selected, "full32 Java selection of algebra result");
                    selected.clear();
                    same(before, result, "selected output detached");
                    if (nativeLane) {
                        M3LongRangeSet nativeSelected = result.selectedByState(states, need, deny,
                                M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                        same(expected, nativeSelected, "full32 JNI selection of algebra result");
                        nativeSelected.clear();
                        same(before, result, "native selected output detached");
                    }
                    check(Arrays.equals(stateCopy, states), "mask state input unchanged");
                }
            }
            if (!nativeLane) {
                boolean refused = false;
                try {
                    result.selectedByState(states, 0, 0, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                } catch (UnsatisfiedLinkError expected) { refused = true; }
                check(refused, "missing required native backend refuses");
            }
        }
        M3LongRangeSet empty = left.difference(left);
        check(empty.selectedByState(new int[0], 0, 0, M3IndexPathMasks.Backend.JAVA).isEmpty(),
                "empty algebra result selects empty Java state");
        if (nativeLane) {
            check(empty.selectedByState(new int[0], 0, 0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED).isEmpty(), "empty JNI composition");
        }
    }

    private static void run(boolean requireNative) {
        checks = 0;
        pairs = 0;
        boolean nativeLane = M3IndexPathMasks.nativeAvailable();
        check(!requireNative || nativeLane, "requested native library is available");
        boundaries();
        exhaustive();
        randomized();
        wideGrids();
        fragmentedGrowth();
        detachedMutation();
        stateComposition(nativeLane);
        System.out.println("PASS M3LongRangeAlgebraSelfTest checks=" + checks
                + " pairs=" + pairs + " JNI=" + nativeLane);
    }

    public static void run() { run(false); }

    public static void main(String[] args) { run(Arrays.asList(args).contains("--native")); }
}
