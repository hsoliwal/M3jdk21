// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** JDK21 stable-sort oracle for opposite-direction index/offset/mask projections. */
public final class M3LongIndexOrderFlipProof {
    private static long checks;
    private M3LongIndexOrderFlipProof() { }

    private static void same(int[] expected, int[] actual, String step) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(step + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static int[] stable(long[] values, int[] input, boolean descending) {
        Integer[] ids = new Integer[input.length];
        for (int i = 0; i < input.length; i++) ids[i] = input[i];
        Comparator<Integer> order = Comparator.comparingLong(i -> values[i]);
        Arrays.sort(ids, descending ? order.reversed() : order);
        int[] output = new int[ids.length];
        for (int i = 0; i < output.length; i++) output[i] = ids[i];
        return output;
    }

    private static void one(long[] original, int begin, int length, byte[] mask,
                            boolean ascendingFirst) {
        long[] inputs = original.clone();
        M3LongIndexProjection root = M3LongIndexProjection.snapshot(inputs);
        Arrays.fill(inputs, 99L); // the payload must remain a detached snapshot
        M3LongIndexProjection view = root.slice(begin, length).filtered(
                mask, 0x80, M3IndexMaskCompactor.Backend.JAVA);
        int[] physical = view.physicalIndices();
        boolean firstDescending = !ascendingFirst;
        M3LongIndexProjection first = firstDescending
                ? view.sortedDescending() : view.sortedAscending();
        int[] firstOracle = stable(original, physical, firstDescending);
        same(firstOracle, first.physicalIndices(), "first stable sort");
        int[] secondOracle = stable(original, firstOracle, !firstDescending);
        M3LongIndexProjection second = firstDescending
                ? first.sortedAscending() : first.sortedDescending();
        same(secondOracle, second.physicalIndices(), "opposite stable sort");
        same(firstOracle, first.physicalIndices(), "immutable old sorted view");
        same(physical, view.physicalIndices(), "immutable old filter view");
        same(firstOracle, (firstDescending ? second.sortedDescending()
                : second.sortedAscending()).physicalIndices(), "stable round trip");
        // Selection after a direction flip operates on the flipped logical mask.
        byte[] after = new byte[second.size()];
        for (int i = 0; i < after.length; i++) after[i] = (byte) ((i & 1) == 0 ? 0x80 : 0);
        M3LongIndexProjection selected = second.filtered(
                after, 0x80, M3IndexMaskCompactor.Backend.JAVA);
        int[] expected = new int[(secondOracle.length + 1) / 2];
        for (int i = 0, j = 0; i < secondOracle.length; i += 2) expected[j++] = secondOracle[i];
        same(expected, selected.physicalIndices(), "postflip mask order");
        same(stable(original, expected, firstDescending),
                (firstDescending ? selected.sortedDescending()
                        : selected.sortedAscending()).physicalIndices(),
                "sliced filter flip");
        for (int i = 0; i < selected.size(); i++) {
            checks++;
            if (selected.get(i) != original[selected.physicalIndex(i)])
                throw new AssertionError("payload mismatch");
        }
    }

    public static void main(String[] args) {
        long[] extremes = {Long.MIN_VALUE, 7L, 7L, 2L, 7L, Long.MAX_VALUE,
                Long.MIN_VALUE, 0L, Long.MAX_VALUE};
        one(extremes, 0, extremes.length, new byte[] {
                -1, -1, -1, -1, -1, -1, -1, -1, -1}, true);
        one(extremes, 1, 5, new byte[] {-1, 0, -1, -1, -1}, false);
        Random random = new Random(0x4d33464c49504c);
        for (int trial = 0; trial < 3_072; trial++) {
            int count = random.nextInt(160);
            long[] values = new long[count];
            for (int i = 0; i < count; i++) {
                values[i] = switch (i % 17) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(15) - 7L;
                };
            }
            int from = random.nextInt(count + 1);
            int length = random.nextInt(count - from + 1);
            byte[] states = new byte[length];
            random.nextBytes(states);
            if (trial % 7 == 0) Arrays.fill(states, (byte) 0xff);
            if (trial % 7 == 1) Arrays.fill(states, (byte) 0);
            one(values, from, length, states, (trial & 1) == 0);
        }
        System.out.println("PASS M3LongIndexOrderFlipProof checks=" + checks);
    }
}
