// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia synexia-fastsearch/recipe-crates/problem-pair-sum-jni-20261103/com/synexia/fastsearch/problem/PairSumParityDriver.java (blob c148f06b684a) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Temporary toolchain-independent driver; official Java21 Maven/JUnit is a separate gate. */
public final class PairSumParityDriver {
    private PairSumParityDriver() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("library path required");
        M3PairSumJni.Provider nativeProvider = M3PairSumJni.load(Path.of(args[0]));
        Random random = new Random(0x1670_F00DL);
        for (int attempt = 0; attempt < 15_000; attempt++) {
            int[] values = lane(random, random.nextInt(38));
            int[] before = values.clone();
            long target = query(random, values, attempt);
            long expected = oracle(values, target);
            check(M3PairSumJni.rawSorted(values, target) == expected, "raw single " + attempt);
            check(nativeProvider.firstSorted(values, target) == expected, "checked single " + attempt);
            check(Arrays.equals(before, values), "native mutated input " + attempt);
        }

        random = new Random(0xBADC_167L);
        for (int pass = 0; pass < 3; pass++) {
            int[] values = lane(random, 1 + random.nextInt(42));
            int[] before = values.clone();
            long[] targets = new long[8_192];
            for (int i = 0; i < targets.length; i++) targets[i] = query(random, values, i);
            long[] targetBefore = targets.clone();
            long[] raw = M3PairSumJni.rawSortedBatch(values, targets);
            long[] checked = nativeProvider.firstSortedBatch(values, targets);
            check(raw.length == targets.length && checked.length == targets.length,
                    "batch length");
            for (int i = 0; i < targets.length; i++) {
                long expected = oracle(values, targets[i]);
                check(raw[i] == expected, "raw batch " + pass + ":" + i);
                check(checked[i] == expected, "checked batch " + pass + ":" + i);
            }
            check(Arrays.equals(before, values), "batch input mutated");
            check(Arrays.equals(targetBefore, targets), "target input mutated");
        }

        random = new Random(0xA11C_E521L);
        for (int attempt = 0; attempt < 8_000; attempt++) {
            int[] values = unsortedLane(random, random.nextInt(49));
            int[] before = values.clone();
            long target = query(random, values, attempt);
            long expected = oracle(values, target);
            check(M3PairSumJni.rawUnsorted(values, target) == expected,
                    "raw unsorted single " + attempt);
            check(nativeProvider.firstUnsorted(values, target) == expected,
                    "checked unsorted single " + attempt);
            check(Arrays.equals(before, values), "unsorted single mutation " + attempt);
        }
        random = new Random(0x8123_231EL);
        for (int pass = 0; pass < 2; pass++) {
            int[] values = unsortedLane(random, 35 + random.nextInt(16));
            int[] before = values.clone();
            long[] targets = new long[4_096];
            for (int i = 0; i < targets.length; i++) targets[i] = query(random, values, i);
            long[] targetBefore = targets.clone();
            long[] raw = M3PairSumJni.rawUnsortedBatch(values, targets);
            long[] checked = nativeProvider.firstUnsortedBatch(values, targets);
            for (int i = 0; i < targets.length; i++) {
                long expected = oracle(values, targets[i]);
                check(raw[i] == expected, "raw unsorted batch " + pass + ":" + i);
                check(checked[i] == expected, "checked unsorted batch " + pass + ":" + i);
            }
            check(Arrays.equals(before, values), "unsorted batch mutated array");
            check(Arrays.equals(targetBefore, targets), "unsorted batch mutated targets");
        }
        int[] oversized = new int[65_537];
        Arrays.fill(oversized, 1);
        check(nativeProvider.firstUnsorted(oversized, 2L) == 1L,
                "oversized native candidate did not use Java fallback");
        check(Arrays.equals(nativeProvider.firstUnsortedBatch(oversized,
                new long[] {2L, 3L}), new long[] {1L, -1L}),
                "oversized native batch did not use Java fallback");
        check(M3PairSumJni.validatedUnsortedBatch(new int[] {4, -1, -1},
                new long[] {3L}, new long[] {0L})[0] == 1L,
                "unsorted native candidate corruption not repaired");
        check(M3PairSumJni.validated(-1L, 0L) == -1L, "invalid candidate not repaired");
        boolean refused = false;
        try {
            nativeProvider.firstSortedBatch(new int[] {1, 2}, new long[8_193]);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        check(refused, "oversized batch accepted");
        refused = false;
        try {
            nativeProvider.firstSorted(new int[] {9, 3}, 12L);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        check(refused, "unsorted input accepted");
        System.out.println("JNI C17/Java parity PASS: 15000 sorted singles, 24576 sorted "
                + "batch queries, 8000 unsorted singles, 8192 unsorted batch queries "
                + "(55768 total), with validation and ownership checks "
                + "(temporary JVM17 smoke gate)");
    }

    private static int[] lane(Random random, int length) {
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            int draw = random.nextInt(25);
            values[i] = draw == 0 ? Integer.MIN_VALUE : draw == 1
                    ? Integer.MAX_VALUE : random.nextInt(31) - 15;
        }
        Arrays.sort(values);
        return values;
    }

    private static int[] unsortedLane(Random random, int length) {
        int[] values = lane(random, length);
        for (int i = values.length - 1; i > 0; i--) {
            int selected = random.nextInt(i + 1);
            int swap = values[selected];
            values[selected] = values[i];
            values[i] = swap;
        }
        return values;
    }

    private static long query(Random random, int[] values, int index) {
        return switch (index % 6) {
            case 0 -> Long.MAX_VALUE;
            case 1 -> Long.MIN_VALUE;
            case 2 -> random.nextInt(61) - 30L;
            case 3 -> values.length < 2 ? 0L : (long) values[0] + values[values.length - 1];
            case 4 -> values.length < 2 ? 0L : (long) values[random.nextInt(values.length)]
                    + values[random.nextInt(values.length)];
            default -> random.nextLong();
        };
    }

    private static long oracle(int[] values, long target) {
        for (int left = 0; left < values.length; left++) {
            for (int right = left + 1; right < values.length; right++) {
                if ((long) values[left] + values[right] == target) {
                    return ((long) left << 32) | (right & 0xffff_ffffL);
                }
            }
        }
        return -1L;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
