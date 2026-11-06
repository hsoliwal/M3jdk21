// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** JUnit per-atom gates: sorted multiplicity algebra and actual required JNI. */
final class M3SortedIntMultisetsTest {
    @Test
    void unionUsesMaximumMultiplicityWithoutBoxedEntries() {
        assertArrayEquals(new int[] {1, 1, 1, 2, 3, 3, 3},
                M3SortedIntMultisets.union(new int[] {1, 1, 1, 3},
                        new int[] {1, 2, 3, 3, 3}));
        assertArrayEquals(new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
                M3SortedIntMultisets.union(new int[] {Integer.MIN_VALUE, 0},
                        new int[] {Integer.MAX_VALUE}));
    }

    @Test
    void intersectionRetainsLeetCode350MinimumMultiplicity() {
        assertArrayEquals(new int[] {2, 2},
                M3SortedIntMultisets.intersection(new int[] {1, 1, 2, 2, 2},
                        new int[] {2, 2, 3, 3}));
        assertArrayEquals(new int[0],
                M3SortedIntMultisets.intersection(new int[] {1, 1}, new int[] {2, 2}));
    }

    @Test
    void subtractRetainsOnlyPositiveMultiplicityRemainder() {
        assertArrayEquals(new int[] {1, 2, 3, 3},
                M3SortedIntMultisets.subtract(new int[] {1, 1, 2, 3, 3, 3},
                        new int[] {1, 3}));
        assertArrayEquals(new int[0],
                M3SortedIntMultisets.subtract(new int[] {2}, new int[] {1, 2, 2}));
    }

    @Test
    void symmetricDifferenceCancelsPairedOccurrencesOnly() {
        assertArrayEquals(new int[] {-3, 1, 1, 2, 3},
                M3SortedIntMultisets.symmetricDifference(
                        new int[] {-3, 1, 1, 1, 2, 2},
                        new int[] {1, 2, 3}));
        assertArrayEquals(new int[0],
                M3SortedIntMultisets.symmetricDifference(new int[] {8, 8}, new int[] {8, 8}));
    }

    @Test
    void emptyAndAliasedCallerLanesAlwaysReturnDetachedResults() {
        int[] input = {Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE};
        int[] original = input.clone();
        for (var op : M3SortedIntMultisets.Operation.values()) {
            int[] output = M3SortedIntMultisets.apply(input, input, op);
            assertNotSame(input, output);
            assertArrayEquals(original, input);
            if (output.length > 0) output[0]++;
            assertArrayEquals(original, input);
            assertArrayEquals(new int[0],
                    M3SortedIntMultisets.apply(new int[0], new int[0], op));
        }
    }

    @Test
    void unorderedNullAndRequiredNativeUnavailableAreExplicitRefusals() {
        assertThrows(IllegalArgumentException.class,
                () -> M3SortedIntMultisets.union(new int[] {2, 1}, new int[0]));
        assertThrows(IllegalArgumentException.class,
                () -> M3SortedIntMultisets.intersection(new int[0], new int[] {2, 1}));
        assertThrows(NullPointerException.class,
                () -> M3SortedIntMultisets.apply(null, new int[0],
                        M3SortedIntMultisets.Operation.UNION_MAX));
        assertThrows(NullPointerException.class,
                () -> M3SortedIntMultisets.apply(new int[0], null,
                        M3SortedIntMultisets.Operation.SUBTRACT));
        assertThrows(NullPointerException.class,
                () -> M3SortedIntMultisets.apply(new int[0], new int[0], null));
        String path = System.getProperty("m3.arrays.native.path");
        if (!M3ArrayNative.isLoaded() && (path == null || path.isBlank())) {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> M3SortedIntMultisets.applyRequiredNative(new int[0], new int[0],
                            M3SortedIntMultisets.Operation.UNION_MAX));
        }
    }

    @Test
    void requiredNativeMatchesAllJavaOperationsAcrossRegionBoundaries() {
        String path = System.getProperty("m3.arrays.native.path");
        if (!M3ArrayNative.isLoaded() && (path == null || path.isBlank())) return;
        int[] left = new int[4097];
        int[] right = new int[8193];
        for (int i = 0; i < left.length; i++) left[i] = i / 2 - 3000;
        for (int i = 0; i < right.length; i++) right[i] = i / 3 - 3000;
        for (var op : M3SortedIntMultisets.Operation.values()) {
            assertArrayEquals(M3SortedIntMultisets.apply(left, right, op),
                    M3SortedIntMultisets.applyRequiredNative(left, right, op));
        }
        assertArrayEquals(new int[0], M3SortedIntMultisets.applyRequiredNative(
                new int[0], new int[0], M3SortedIntMultisets.Operation.INTERSECTION_MIN));
    }

    @Test
    void seededFrequencyOracleChecksPrimitiveOutputNotJustMembership() {
        M3SortedIntMultisetsSelfTest.run();
    }
}
