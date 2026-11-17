// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Actual Maven JUnit gate for the existing immutable sparse DFA and optional JNI. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipe-crates/m3-utf16-stitch-20261022/src/test/java/com/synexia/m3/mastery/M3SparseUtf16StitchJUnitTest.java blob f2108dbd088e; recipe m3/ports/precompute/recipe/utf16-stitch-20261106/relocate.py v3
final class SparseUtf16StitchedTest {
    private static final String ROOT =
            "140f8b291985ea6f35c5cf86d1df3544ae18892fba49455f4b26c6c1c0f9bbdf";

    @Test
    void multipleChunkWidthsAndIndependentJdkStringFirstIndex() throws Exception {
        var serial = SparseUtf16StitchedOracle.verify(1, false);
        assertEquals(serial, SparseUtf16StitchedOracle.verify(2, false));
        assertEquals(serial, SparseUtf16StitchedOracle.verify(4, false));
        assertEquals(256, serial.trials());
        assertEquals(12_200L, serial.checks());
        assertTrue(serial.crossings() > 0);
        assertEquals(ROOT, serial.sha256());
    }

    @Test
    void invalidGeometryAndFirstSixtyFourWindowSeamsRefuseOrPreserve() {
        SparseUtf16StitchedOracle.verifyRefusals(false);
        var plan = SparseUtf16LiteralPlan.compile(List.of("ab", "\uD83D", "\uDE00"), 0);
        assertEquals(1, SparseUtf16Stitched.windowCount(0));
        assertEquals(4096, SparseUtf16Stitched.windowCount(16_523_264));
        assertEquals(-1, SparseUtf16Stitched.firstJava(plan, new char[0], 0, 0)[0]);
    }

    @Test
    void certifiedFrontierRefusesPrematureCompletionAndPreservesLongSelections() {
        assertTrue(SparseUtf16Stitched.settledBefore(new int[] {0, 25, 25}, 25L));
        assertFalse(SparseUtf16Stitched.settledBefore(new int[] {0, -1}, 200L));
        assertFalse(SparseUtf16Stitched.settledBefore(new int[] {0, 201}, 200L));
        assertTrue(SparseUtf16Stitched.settledBefore(new int[] {0, 1}, 2_000_000_000L));
        SparseUtf16StitchedOracle.verifyFrontier(false);
    }

    @Test
    void nativeRequiredIsNeverSilentFallback() throws Exception {
        if (Boolean.getBoolean("m3.stitch.native.required")) {
            var nativeResult = SparseUtf16StitchedOracle.verify(1, true);
            assertEquals(ROOT, nativeResult.sha256());
            assertEquals(nativeResult, SparseUtf16StitchedOracle.verify(2, true));
            assertEquals(nativeResult, SparseUtf16StitchedOracle.verify(4, true));
            SparseUtf16StitchedOracle.verifyRefusals(true);
        } else {
            assertTrue(SparseUtf16Stitched.windowCount(4096) == 2);
        }
    }
}
