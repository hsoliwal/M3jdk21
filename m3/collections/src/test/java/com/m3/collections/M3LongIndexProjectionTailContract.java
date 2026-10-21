// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Real projection APIs compared with JDK stable sorting of physical occurrences. */
public final class M3LongIndexProjectionTailContract {
    private static int checks;

    private M3LongIndexProjectionTailContract() { }

    private static M3LongIndexProjection checkSorted(
            M3LongIndexProjection source, long[] payload, boolean descending) {
        int[] before = source.physicalIndices();
        Integer[] expected = Arrays.stream(before).boxed().toArray(Integer[]::new);
        Arrays.sort(expected, (left, right) -> descending
                ? Long.compare(payload[right], payload[left])
                : Long.compare(payload[left], payload[right]));
        M3LongIndexProjection actual = descending
                ? source.sortedDescending() : source.sortedAscending();
        int[] expectedIndices = Arrays.stream(expected).mapToInt(Integer::intValue).toArray();
        array(expectedIndices, actual.physicalIndices(), "stable physical occurrences");
        array(before, source.physicalIndices(), "source index unchanged");
        if (actual.order() != (descending ? M3LongIndexProjection.Order.DESCENDING
                : M3LongIndexProjection.Order.ASCENDING)) {
            throw new AssertionError("wrong result order");
        }
        long[] materialized = actual.toArray();
        for (int i = 0; i < expected.length; i++) {
            long value = payload[expected[i]];
            if (actual.physicalIndex(i) != expected[i] || actual.get(i) != value
                    || materialized[i] != value) {
                throw new AssertionError("slot/value mismatch at " + i);
            }
            checks++;
        }
        return actual;
    }

    private static void directions(M3LongIndexProjection source, long[] payload) {
        M3LongIndexProjection ascending = checkSorted(source, payload, false);
        M3LongIndexProjection descending = checkSorted(source, payload, true);
        checkSorted(ascending, payload, false);
        checkSorted(descending, payload, true);
        checkSorted(ascending, payload, true);
        checkSorted(descending, payload, false);
    }

    private static void exercise(long[] input) {
        long[] payload = input.clone();
        M3LongIndexProjection snapshot = M3LongIndexProjection.snapshot(input);
        Arrays.fill(input, 0x5a5a5a5a5a5a5a5aL);
        directions(snapshot, payload);
        int offset = payload.length / 4;
        int length = payload.length - offset - payload.length / 5;
        M3LongIndexProjection slice = snapshot.slice(offset, length);
        directions(slice, payload);
        byte[] states = new byte[length];
        for (int i = 0; i < states.length; i++) states[i] = (byte) (i % 3 == 1 ? 0 : 0x81);
        M3LongIndexProjection filtered = slice.filtered(
                states, 0x80, M3IndexMaskCompactor.Backend.JAVA);
        int[] selected = filtered.physicalIndices();
        Arrays.fill(states, (byte) 0);
        directions(filtered, payload);
        array(selected, filtered.physicalIndices(), "caller mask is not retained");
        M3LongIndexProjection sorted = checkSorted(snapshot, payload, false);
        M3LongIndexProjection sortedSlice = sorted.slice(offset, length);
        directions(sortedSlice, payload);
        byte[] keep = new byte[sortedSlice.size()];
        Arrays.fill(keep, (byte) 1);
        if (keep.length > 2) keep[keep.length / 2] = 0;
        directions(sortedSlice.filtered(keep, 1, M3IndexMaskCompactor.Backend.JAVA), payload);
    }

    private static void array(int[] expected, int[] actual, String label) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
        checks++;
    }

    public static void main(String[] args) {
        exercise(new long[]{4, 1, 4, 2, 3});
        exercise(new long[]{Long.MIN_VALUE, Long.MAX_VALUE, 0, Long.MIN_VALUE,
                -1, Long.MAX_VALUE, 1, 0});
        Random random = new Random(0x4d3350524f4a2026L);
        for (int length : new int[]{0, 1, 2, 3, 7, 8, 9, 31, 32, 33, 63, 64, 65, 127, 128, 129}) {
            for (int trial = 0; trial < 12; trial++) {
                long[] values = new long[length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = switch (trial % 4) {
                        case 0 -> i;
                        case 1 -> -i;
                        case 2 -> 7;
                        default -> random.nextInt(9) - 4;
                    };
                }
                exercise(values);
            }
        }
        System.out.println("M3_LONG_PROJECTION_TAIL_CONTRACT_PASS checks=" + checks);
    }
}
