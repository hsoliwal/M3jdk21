// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Reference-model gate of recipe/lint-storage-20261107 in the test phase: the seeded corpus of
 * {@link M3LintStorageDump} (383 edge values x 4 partners plus seeded values) runs through {@link M3IntWidth} and
 * {@link M3Lint}, and every width fact and lint result must equal the BigInteger model of
 * {@link M3LintStorageReference} (the dump aborts on the first drift).
 */
//FLLDCIM: TFG - JUnit wrapper from the module template M3IntRingBufferReferenceTest; recipe m3/arrays/recipe/lint-storage-20261107
final class M3LintStorageReferenceTest {
    private static final int CASES = 4_000;
    private static final int EDGES = 383 * 4;

    @Test
    void everyWidthFactAndLintResultEqualsBigIntegerModel() throws Exception {
        long[] counts = M3LintStorageDump.run(M3IntWidth.class, M3Lint.class, CASES, M3LintStorageDump.discard());
        assertEquals(CASES + EDGES, counts[0]);
        assertTrue(counts[1] > counts[0], "reference comparisons");
        assertTrue(counts[2] > 0, "ArithmeticException outcomes");
    }
}
