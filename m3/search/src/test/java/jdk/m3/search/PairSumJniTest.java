// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/problem/PairSumJniTest.java (blob 41ca8fa29406) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** JNI is mandatory for this proof: raw native results face a separate quadratic oracle. */
final class PairSumJniTest {
    private static PairSumJni.Provider nativeProvider;

    @BeforeAll
    static void loadBuiltNativeLibrary() {
        String library = System.getProperty("m3.pair.jni.library");
        assertNotNull(library, "compile C17 native proof and supply m3.pair.jni.library");
        nativeProvider = PairSumJni.load(Path.of(library));
    }

    @Test
    void nativeSingleMatchesOracleAndLeavesCallerArraysUnchanged() {
        Random random = new Random(0x1670_F00DL);
        for (int attempt = 0; attempt < 15_000; attempt++) {
            int[] values = lane(random, random.nextInt(38));
            long target = target(random, values, attempt);
            int[] copy = values.clone();
            long expected = quadraticOracle(values, target);
            assertEquals(expected, PairSumJni.rawSorted(values, target),
                    "raw JNI single case " + attempt);
            assertEquals(expected, nativeProvider.firstSorted(values, target),
                    "checked JNI single case " + attempt);
            assertArrayEquals(copy, values);
        }
    }

    @Test
    void nativeBatchOneTransitionHasExactPackedOutputsAndNoRetainedInputs() {
        Random random = new Random(0xBADC_167L);
        for (int pass = 0; pass < 3; pass++) {
            int[] values = lane(random, 1 + random.nextInt(42));
            long[] targets = new long[8_192];
            for (int i = 0; i < targets.length; i++) {
                targets[i] = target(random, values, i);
            }
            int[] copy = values.clone();
            long[] targetsCopy = targets.clone();
            long[] raw = PairSumJni.rawSortedBatch(values, targets);
            long[] checked = nativeProvider.firstSortedBatch(values, targets);
            assertEquals(targets.length, raw.length);
            assertEquals(targets.length, checked.length);
            for (int i = 0; i < targets.length; i++) {
                long oracle = quadraticOracle(values, targets[i]);
                assertEquals(oracle, raw[i], "raw JNI batch pass " + pass + " query " + i);
                assertEquals(oracle, checked[i], "validated JNI batch query " + i);
            }
            assertArrayEquals(copy, values);
            assertArrayEquals(targetsCopy, targets);
        }
    }

    @Test
    void unsortedNativeSuffixHashMatchesIndependentQuadraticOracle() {
        Random random = new Random(0xA11C_E521L);
        for (int attempt = 0; attempt < 8_000; attempt++) {
            int[] values = unsortedLane(random, random.nextInt(49));
            long target = target(random, values, attempt);
            int[] copy = values.clone();
            long expected = quadraticOracle(values, target);
            assertEquals(expected, PairSumJni.rawUnsorted(values, target),
                    "raw unsorted single " + attempt);
            assertEquals(expected, nativeProvider.firstUnsorted(values, target),
                    "validated unsorted single " + attempt);
            assertArrayEquals(copy, values);
        }
    }

    @Test
    void unsortedBatchHasNoAliasOrLeakAndRepairsCorruptCandidates() {
        Random random = new Random(0x8123_231EL);
        for (int pass = 0; pass < 2; pass++) {
            int[] values = unsortedLane(random, 35 + random.nextInt(16));
            long[] targets = new long[4_096];
            for (int i = 0; i < targets.length; i++) targets[i] = target(random, values, i);
            int[] copy = values.clone();
            long[] queriesCopy = targets.clone();
            long[] raw = PairSumJni.rawUnsortedBatch(values, targets);
            long[] checked = nativeProvider.firstUnsortedBatch(values, targets);
            for (int i = 0; i < targets.length; i++) {
                long oracle = quadraticOracle(values, targets[i]);
                assertEquals(oracle, raw[i], "raw unsorted batch " + pass + ":" + i);
                assertEquals(oracle, checked[i], "validated unsorted batch " + pass + ":" + i);
            }
            assertArrayEquals(copy, values);
            assertArrayEquals(queriesCopy, targets);
        }
        int[] oversized = new int[65_537];
        Arrays.fill(oversized, 1);
        assertEquals(1L, nativeProvider.firstUnsorted(oversized, 2L));
        assertArrayEquals(new long[] {1L, -1L},
                nativeProvider.firstUnsortedBatch(oversized, new long[] {2L, 3L}));
        assertThrows(IllegalArgumentException.class,
                () -> PairSumJni.rawUnsorted(oversized, 2L));
        assertThrows(IllegalArgumentException.class,
                () -> nativeProvider.firstUnsortedBatch(new int[] {1}, new long[8_193]));

        int[] values = {4, -1, -1};
        long[] candidates = {0L, 0L};
        assertArrayEquals(new long[] {1L, -1L}, PairSumJni.validatedUnsortedBatch(
                values, new long[] {3L, 9L}, candidates));
        assertArrayEquals(new long[] {0L, 0L}, candidates);
        assertThrows(IllegalArgumentException.class,
                () -> PairSumJni.validatedUnsortedBatch(values, new long[] {3L}, candidates));
    }

    @Test
    void sourcePreconditionsAndBatchBoundsFailBeforeNativeExecution() {
        assertThrows(NullPointerException.class,
                () -> nativeProvider.firstSorted(null, 2L));
        assertThrows(NullPointerException.class,
                () -> nativeProvider.firstSortedBatch(new int[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> nativeProvider.firstSorted(new int[] {4, 2}, 6L));
        assertThrows(IllegalArgumentException.class,
                () -> nativeProvider.firstSortedBatch(new int[] {4, 2}, new long[0]));
        assertThrows(IllegalArgumentException.class,
                () -> nativeProvider.firstSortedBatch(new int[] {1, 2}, new long[8_193]));
        assertArrayEquals(new long[0],
                nativeProvider.firstSortedBatch(new int[0], new long[0]));
        assertThrows(IllegalArgumentException.class,
                () -> PairSumJni.load(Path.of("nonexistent-native-library.so")));
    }

    @Test
    void invalidNativeCandidatesAreRepairedWithoutEditingCandidateArrays() {
        int[] sorted = {-4, 0, 0, 7, 10};
        long[] targets = {3L, 0L, 17L, Long.MAX_VALUE};
        long[] fabricated = {0L, 7L, 8L, 9L};
        long[] original = fabricated.clone();
        long[] corrected = PairSumJni.validatedBatch(sorted, targets, fabricated);
        for (int i = 0; i < targets.length; i++) {
            assertEquals(quadraticOracle(sorted, targets[i]), corrected[i]);
        }
        assertArrayEquals(original, fabricated);
        assertEquals(PairSumSearch.NOT_FOUND, PairSumJni.validated(
                PairSumSearch.NOT_FOUND, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> PairSumJni.validatedBatch(sorted, targets, new long[2]));
    }

    private static int[] lane(Random random, int size) {
        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            int draw = random.nextInt(25);
            data[i] = draw == 0 ? Integer.MIN_VALUE
                    : draw == 1 ? Integer.MAX_VALUE : random.nextInt(31) - 15;
        }
        Arrays.sort(data);
        return data;
    }

    private static int[] unsortedLane(Random random, int size) {
        int[] data = lane(random, size);
        for (int i = data.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int swap = data[index];
            data[index] = data[i];
            data[i] = swap;
        }
        return data;
    }

    private static long target(Random random, int[] values, int index) {
        return switch (index % 6) {
            case 0 -> Long.MAX_VALUE;
            case 1 -> Long.MIN_VALUE;
            case 2 -> random.nextInt(61) - 30L;
            case 3 -> values.length < 2 ? 0L
                    : (long) values[0] + values[values.length - 1];
            case 4 -> values.length < 2 ? 0L
                    : (long) values[random.nextInt(values.length)]
                            + values[random.nextInt(values.length)];
            default -> random.nextLong();
        };
    }

    private static long quadraticOracle(int[] values, long target) {
        for (int left = 0; left < values.length; left++) {
            for (int right = left + 1; right < values.length; right++) {
                if ((long) values[left] + values[right] == target) {
                    return ((long) left << 32) | (right & 0xffff_ffffL);
                }
            }
        }
        return PairSumSearch.NOT_FOUND;
    }
}
