// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Real JUnit 5 test source; execution by Maven is required before qualification. */
//FLLDCIM: RELOCATE - donor blob 4ee4d248e7c7 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class IntLcsWitnessTest {
    @Test void integerSequenceReconstruction() {
        int[] a = {1, 2, 3, 4, 1};
        int[] b = {3, 4, 1, 2, 1, 3};
        int[] witness = DynamicProgrammingAlgorithms.javaWitness(a, b);
        assertEquals(3, witness.length);
        assertTrue(DynamicProgrammingAlgorithms.isSubsequence(witness, a));
        assertTrue(DynamicProgrammingAlgorithms.isSubsequence(witness, b));
        assertEquals(DynamicProgrammingAlgorithms.referenceLength(a, b), witness.length);
    }

    @Test void signedAndDuplicateIntegers() {
        int[] a = {Integer.MIN_VALUE, -1, 0, -1, Integer.MAX_VALUE};
        int[] b = {-1, Integer.MIN_VALUE, -1, Integer.MAX_VALUE};
        int[] w = DynamicProgrammingAlgorithms.javaWitness(a, b);
        assertEquals(3, w.length);
        assertTrue(DynamicProgrammingAlgorithms.isSubsequence(w, a));
        assertTrue(DynamicProgrammingAlgorithms.isSubsequence(w, b));
        assertArrayEquals(new int[]{7, 7}, DynamicProgrammingAlgorithms.javaWitness(
                new int[]{7, 7, 7}, new int[]{7, 7}));
    }

    @Test void rejectsNullAndBudgetDrift() {
        assertThrows(NullPointerException.class,
                () -> DynamicProgrammingAlgorithms.javaWitness(null, new int[0]));
        assertThrows(NullPointerException.class,
                () -> DynamicProgrammingAlgorithms.javaWitness(new int[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> DynamicProgrammingAlgorithms.javaWitness(new int[4097], new int[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> DynamicProgrammingAlgorithms.javaWitness(new int[4096], new int[4096]));
        assertArrayEquals(new int[0], DynamicProgrammingAlgorithms.javaWitness(new int[]{1, 2}, new int[0]));
    }

    @Test void seededOracleAndUnchangedInputs() {
        Random rng = new Random(20261105L);
        for (int k = 0; k < 1024; k++) {
            int[] a = new int[rng.nextInt(24)], b = new int[rng.nextInt(24)];
            for (int i = 0; i < a.length; i++) a[i] = rng.nextInt(9) - 4;
            for (int j = 0; j < b.length; j++) b[j] = rng.nextInt(9) - 4;
            int[] beforeA = a.clone(), beforeB = b.clone();
            int[] witness = DynamicProgrammingAlgorithms.javaWitness(a, b);
            assertEquals(DynamicProgrammingAlgorithms.referenceLength(a, b), witness.length);
            assertTrue(DynamicProgrammingAlgorithms.isSubsequence(witness, a));
            assertTrue(DynamicProgrammingAlgorithms.isSubsequence(witness, b));
            assertArrayEquals(beforeA, a);
            assertArrayEquals(beforeB, b);
        }
    }
}
