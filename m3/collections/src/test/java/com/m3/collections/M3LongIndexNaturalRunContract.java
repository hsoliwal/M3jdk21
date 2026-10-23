// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Differential oracle for stable physical-index ordering and monotonic encounter admission.
 * Tests actual M3LongIndexProjection public behavior against JDK stable object sorting;
 * no caller code depends on the private optimization or layout.
 */
public final class M3LongIndexNaturalRunContract {
    private static long checks;

    private M3LongIndexNaturalRunContract() { }

    public static void main(String[] args) {
        for (int n : new int[] {0, 1, 2, 3, 31, 63, 64, 65, 127, 512}) {
            long[] ascending = new long[n];
            long[] descending = new long[n];
            long[] equal = new long[n];
            for (int i = 0; i < n; i++) {
                ascending[i] = i / 3;
                descending[i] = (n - i) / 3;
                equal[i] = Long.MIN_VALUE;
            }
            exercise(ascending, 0, n, false);
            exercise(descending, 0, n, false);
            exercise(equal, 0, n, false);
        }
        Random random = new Random(0x4d334a444b3231L);
        for (int trial = 0; trial < 900; trial++) {
            int n = random.nextInt(130);
            long[] original = new long[n];
            for (int i = 0; i < n; i++) {
                int bucket = random.nextInt(17);
                original[i] = switch (bucket) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> bucket - 8L;
                };
            }
            int from = n == 0 ? 0 : random.nextInt(n + 1);
            int length = random.nextInt(n - from + 1);
            exercise(original, from, length, true);
        }
        System.out.println("PASS M3LongIndexNaturalRunContract checks=" + checks);
    }

    private static void exercise(long[] original, int from, int length, boolean filtered) {
        long[] unchanged = original.clone();
        M3LongIndexProjection view = M3LongIndexProjection.snapshot(original).slice(from, length);
        int[] positions = range(from, length);
        if (filtered) {
            byte[] mask = new byte[length];
            for (int i = 0; i < mask.length; i++) mask[i] = (byte) (i % 4 == 0 ? 0 : 1);
            view = view.filtered(mask, 1, M3IndexMaskCompactor.Backend.JAVA);
            positions = selected(positions, mask);
        }
        check(view.sortedAscending(), original, positions, false);
        check(view.sortedDescending(), original, positions, true);
        M3LongIndexProjection ascending = view.sortedAscending();
        M3LongIndexProjection descending = view.sortedDescending();
        check(ascending.sortedDescending(), original, positions, true);
        check(descending.sortedAscending(), original, positions, false);
        M3LongIndexProjection again = ascending.sortedAscending();
        check(again, original, positions, false);
        must(Arrays.equals(original, unchanged), "input mutated");
        checks++;
    }

    private static int[] range(int first, int size) {
        int[] indices = new int[size];
        for (int i = 0; i < size; i++) indices[i] = first + i;
        return indices;
    }

    private static int[] selected(int[] indices, byte[] mask) {
        int count = 0;
        for (byte bits : mask) if ((bits & 1) == 1) count++;
        int[] selected = new int[count];
        int position = 0;
        for (int i = 0; i < mask.length; i++) if ((mask[i] & 1) == 1) selected[position++] = indices[i];
        return selected;
    }

    private static void check(M3LongIndexProjection actual, long[] original,
                              int[] physical, boolean descending) {
        Integer[] stable = new Integer[physical.length];
        for (int i = 0; i < stable.length; i++) stable[i] = physical[i];
        Comparator<Integer> signed = (left, right) ->
                Long.compare(original[left], original[right]);
        Arrays.sort(stable, descending ? signed.reversed() : signed);
        must(actual.size() == stable.length, "size");
        must(actual.order() == (descending ? M3LongIndexProjection.Order.DESCENDING
                : M3LongIndexProjection.Order.ASCENDING), "direction");
        long[] values = actual.toArray();
        int[] observed = actual.physicalIndices();
        for (int i = 0; i < stable.length; i++) {
            must(observed[i] == stable[i], "stable physical index / tie order");
            must(actual.physicalIndex(i) == stable[i], "indexed physical access");
            must(actual.get(i) == original[stable[i]], "scalar payload");
            must(values[i] == original[stable[i]], "materialized payload");
            checks += 4;
        }
        checks += 2;
    }

    private static void must(boolean assertion, String reason) {
        if (!assertion) throw new AssertionError(reason);
    }
}
