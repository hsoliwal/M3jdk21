// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** JUnit candidate: Java-only contract tests; JNI is qualified by verify.sh. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b42ec59741ed18117c18c72f84cc165ea4900b6e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-bitparallel-edit-20261105/src/test/java/com/synexia/m3/proof/Utf16BitParallelEditOracleTest.java blob 32c361471457; recipe m3/ports/precompute/recipe/utf16-edit-oracle-20261107/relocate.py v2
final class EditDistancePlanBitParallelTest {
    @Test void challengeShapeAndEmptyCases() {
        assertEquals(3, EditDistancePlanOracle.javaDistance("horse".toCharArray(), "ros".toCharArray()));
        assertEquals(5, EditDistancePlanOracle.javaDistance("intention".toCharArray(), "execution".toCharArray()));
        assertEquals(1, EditDistancePlanOracle.javaDistance("geek".toCharArray(), "gesek".toCharArray()));
        assertEquals(0, EditDistancePlanOracle.javaDistance(new char[0], new char[0]));
        assertEquals(4, EditDistancePlanOracle.javaDistance(new char[0], "abcd".toCharArray()));
    }

    @Test void utf16UnitsAndImmutableInput() {
        char[] a = new char[]{0, '\ud800', '\udc00', '\uffff'};
        char[] b = new char[]{0, '\udc00', '\ud800', '\uffff'};
        char[] aBefore = a.clone();
        char[] bBefore = b.clone();
        assertEquals(EditDistancePlanOracle.referenceDistance(a, b),
                EditDistancePlanOracle.javaDistance(a, b));
        assertArrayEquals(aBefore, a);
        assertArrayEquals(bBefore, b);
    }

    @Test void bitMaskBoundaryAndSideSelection() {
        assertEquals(1, EditDistancePlanOracle.javaDistance(new char[63], new char[64]));
        assertEquals(1, EditDistancePlanOracle.javaDistance(new char[64], new char[63]));
        assertEquals(4096, EditDistancePlanOracle.javaDistance(new char[0], new char[4096]));
        assertEquals(4095, EditDistancePlanOracle.javaDistance(new char[1], new char[4096]));
    }

    @Test void rejectsUnsupportedContracts() {
        assertThrows(NullPointerException.class,
                () -> EditDistancePlanOracle.javaDistance(null, new char[0]));
        assertThrows(NullPointerException.class,
                () -> EditDistancePlanOracle.javaDistance(new char[0], null));
        assertThrows(IllegalArgumentException.class,
                () -> EditDistancePlanOracle.javaDistance(new char[64], new char[64]));
        assertThrows(IllegalArgumentException.class,
                () -> EditDistancePlanOracle.javaDistance(new char[4097], new char[0]));
    }
}
