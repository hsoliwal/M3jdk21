// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia synexia-fastsearch/com/synexia/fastsearch/problem/PairSumSearchTest.java (blob 0f9ffc69c85d) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** An independent quadratic oracle protects both sorted and unsorted contracts. */
final class PairSumSearchTest {
    @Test
    void sortedDuplicatesHaveCanonicalZeroBasedIndicesAndRemainUnchanged() {
        int[] values = {-4, -1, 0, 0, 2, 2, 5};
        int[] copy = values.clone();
        assertPair(2, 4, M3PairSumSearch.firstSorted(values, 2));
        assertPair(2, 3, M3PairSumSearch.firstSorted(values, 0));
        assertPair(-1, -1, M3PairSumSearch.firstSorted(values, 123));
        assertArrayEquals(copy, values);
    }

    @Test
    void unsortedSuffixSearchReturnsEarliestLeftThenRight() {
        int[] values = {5, 9, 2, 7, 4, 2, 9};
        int[] copy = values.clone();
        assertPair(0, 3, M3PairSumSearch.firstUnsorted(values, 12));
        assertPair(1, 2, M3PairSumSearch.firstUnsorted(values, 11));
        assertPair(2, 5, M3PairSumSearch.firstUnsorted(values, 4));
        assertArrayEquals(copy, values);
        assertPair(-1, -1, M3PairSumSearch.firstUnsorted(values, -999));
    }

    @Test
    void overflowAndInvalidSortedPreconditionsAreRejectedOrHandled() {
        assertPair(0, 1, M3PairSumSearch.firstSorted(
                new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}, 4_294_967_294L));
        assertPair(0, 1, M3PairSumSearch.firstSorted(
                new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE}, -4_294_967_296L));
        assertPair(-1, -1, M3PairSumSearch.firstSorted(
                new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}, -2));
        assertPair(-1, -1, M3PairSumSearch.firstUnsorted(
                new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}, Long.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> M3PairSumSearch.firstSorted(null, 1));
        assertThrows(NullPointerException.class, () -> M3PairSumSearch.firstUnsorted(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> M3PairSumSearch.firstSorted(new int[] {2, 1}, Long.MAX_VALUE));
    }

    @Test
    void deterministicRandomizedDifferentialOracleCoversTwentyThousandInputs() {
        Random random = new Random(0x167_CAFEL);
        for (int attempt = 0; attempt < 20_000; attempt++) {
            int[] values = new int[random.nextInt(30)];
            for (int i = 0; i < values.length; i++) {
                int draw = random.nextInt(20);
                values[i] = draw == 0 ? Integer.MIN_VALUE
                        : draw == 1 ? Integer.MAX_VALUE : random.nextInt(17) - 8;
            }
            long target = switch (attempt % 5) {
                case 0 -> Long.MAX_VALUE;
                case 1 -> Long.MIN_VALUE;
                case 2 -> random.nextInt(25) - 12;
                case 3 -> values.length < 2 ? 0L
                        : (long) values[0] + values[values.length - 1];
                default -> random.nextLong();
            };
            int[] original = values.clone();
            assertEquals(oracle(values, target), M3PairSumSearch.firstUnsorted(values, target),
                    "unsorted differential case " + attempt);
            assertArrayEquals(original, values);
            Arrays.sort(values);
            long expected = oracle(values, target);
            assertEquals(expected, M3PairSumSearch.firstSorted(values, target),
                    "sorted differential case " + attempt);
            assertEquals(expected, M3PairSumSearch.firstUnsorted(values, target),
                    "sorted/unsorted parity case " + attempt);
        }
    }

    private static long oracle(int[] values, long target) {
        for (int left = 0; left < values.length; left++) {
            for (int right = left + 1; right < values.length; right++) {
                if ((long) values[left] + values[right] == target) {
                    return ((long) left << 32) | (right & 0xffff_ffffL);
                }
            }
        }
        return M3PairSumSearch.NOT_FOUND;
    }

    private static void assertPair(int left, int right, long packed) {
        assertEquals(left, M3PairSumSearch.leftIndex(packed));
        assertEquals(right, M3PairSumSearch.rightIndex(packed));
    }
}
