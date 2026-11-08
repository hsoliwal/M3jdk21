// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob a582f393d450 at commit 4a2e6452a7ad (repository, path and digests: m3/algorithms/recipe/radix-lanes-fold/PROVENANCE.tsv); recipe m3/algorithms/recipe/radix-lanes-fold (tests.sed, FOLD_MAP.tsv REFS)
final class RadixSortLanesTest {

    @Test
    void intRadixMatchesJdkSignedOrderAcrossExtremesAndRandomInputs() {
        int[] values = {
            Integer.MAX_VALUE,
            0,
            -1,
            Integer.MIN_VALUE,
            1,
            17,
            -17,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE
        };
        int[] expected = values.clone();
        Arrays.sort(expected);

        SortingAlgorithms.radixSortSigned(values);
        assertArrayEquals(expected, values);

        Random random = new Random(0x51A7C0DEL);
        for (int round = 0; round < 200; round++) {
            int length = random.nextInt(256);
            int[] actual = new int[length];
            for (int index = 0; index < length; index++) actual[index] = random.nextInt();
            int[] oracle = actual.clone();
            Arrays.sort(oracle);

            SortingAlgorithms.radixSortSigned(
                    actual,
                    new int[length],
                    new int[SortingAlgorithms.RADIX],
                    ProgressMonitor.none());
            assertArrayEquals(oracle, actual);
        }
    }

    @Test
    void intRadixUnsignedOrderMatchesUnsignedComparator() {
        int[] actual = {
            0,
            -1,
            1,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            -2,
            2
        };
        Integer[] boxed = Arrays.stream(actual).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, Integer::compareUnsigned);
        int[] expected = Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();

        SortingAlgorithms.radixSortUnsigned(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    void intPairRadixIsStableForDuplicateKeys() {
        int[] keys = {2, -1, 2, 0, -1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE};
        int[] payloads = {0, 1, 2, 3, 4, 5, 6, 7};

        SortingAlgorithms.radixSortPairsSigned(
                keys,
                payloads,
                new int[keys.length],
                new int[payloads.length],
                new int[SortingAlgorithms.RADIX],
                ProgressMonitor.none());

        assertArrayEquals(
                new int[] {Integer.MIN_VALUE, -1, -1, 0, 2, 2, 2, Integer.MAX_VALUE},
                keys);
        assertArrayEquals(new int[] {6, 1, 4, 3, 0, 2, 5, 7}, payloads);
    }

    @Test
    void unsignedLongKeyIntPayloadRadixIsStableAcrossFullLongDomain() {
        long[] keys = {
            -1L,
            0L,
            Long.MIN_VALUE,
            1L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE
        };
        int[] payloads = {0, 1, 2, 3, 4, 5, 6};

        SortingAlgorithms.radixSortPairsUnsigned(
                keys,
                payloads,
                new long[keys.length],
                new int[payloads.length],
                new int[SortingAlgorithms.RADIX],
                ProgressMonitor.none());

        assertArrayEquals(
                new long[] {
                    0L,
                    1L,
                    Long.MAX_VALUE,
                    Long.MIN_VALUE,
                    Long.MIN_VALUE,
                    -1L,
                    -1L
                },
                keys);
        assertArrayEquals(new int[] {1, 3, 5, 2, 6, 0, 4}, payloads);
    }

    @Test
    void longKeyIntPayloadRadixIsStableAcrossFullLongDomain() {
        long[] keys = {
            5L,
            Long.MIN_VALUE,
            5L,
            0L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            5L
        };
        int[] payloads = {0, 1, 2, 3, 4, 5, 6, 7};

        SortingAlgorithms.radixSortPairsSigned(
                keys,
                payloads,
                new long[keys.length],
                new int[payloads.length],
                new int[SortingAlgorithms.RADIX],
                ProgressMonitor.none());

        assertArrayEquals(
                new long[] {
                    Long.MIN_VALUE,
                    Long.MIN_VALUE,
                    -1L,
                    0L,
                    5L,
                    5L,
                    5L,
                    Long.MAX_VALUE
                },
                keys);
        assertArrayEquals(new int[] {1, 6, 4, 3, 0, 2, 7, 5}, payloads);
    }

    @Test
    void workspaceValidationAndCancellationFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SortingAlgorithms.radixSortSigned(
                                new int[] {2, 1},
                                new int[1],
                                new int[SortingAlgorithms.RADIX],
                                ProgressMonitor.none()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        SortingAlgorithms.radixSortPairsSigned(
                                new long[] {2L, 1L},
                                new int[] {0},
                                new long[2],
                                new int[2],
                                new int[SortingAlgorithms.RADIX],
                                ProgressMonitor.none()));

        CancelableProgressMonitor canceled = new CancelableProgressMonitor();
        canceled.setCanceled(true);
        assertThrows(
                CancellationException.class,
                () ->
                        SortingAlgorithms.radixSortSigned(
                                new int[] {2, 1},
                                new int[2],
                                new int[SortingAlgorithms.RADIX],
                                canceled));
        assertThrows(
                CancellationException.class,
                () ->
                        SortingAlgorithms.radixSortPairsSigned(
                                new long[] {2L, 1L},
                                new int[] {0, 1},
                                new long[2],
                                new int[2],
                                new int[SortingAlgorithms.RADIX],
                                canceled));
    }
}
