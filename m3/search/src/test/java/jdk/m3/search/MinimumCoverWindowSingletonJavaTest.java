// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowSingletonJavaTest.java (blob e9eb356c1e88) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Independent Java 21 per-atom source-bound range-offset checks. */
final class MinimumCoverWindowSingletonJavaTest {
    @Test
    void nonzeroRequiredMinimumAndUnicodeGapsMatchIndependentOracle() {
        String source = "\0a\uffffb\uD800\uDC00\u4e2d\uffffz\0\u4e2d";
        for (String required : new String[]{"\0", "\uffff", "\uffff\uffff",
                "\uD800", "\uDC00", "\u4e2d", "a", "ab", "\0\uffff",
                "a\uffff", "\u4e2d\u4e2d", "b\u4e2d"}) {
            assertEquals(MinimumCoverWindowJavaAtomTest.independent(source, required),
                    MinimumCoverWindow.javaSpan(source, required), required);
        }
    }
    @Test
    void shiftedIndexPreservesEarliestStartAndEmptyWindow() {
        assertEquals(MinimumCoverWindow.pack(1, 2),
                MinimumCoverWindow.javaSpan("\0\uffff\0", "\uffff"));
        assertEquals(MinimumCoverWindow.pack(1, 2),
                MinimumCoverWindow.javaSpan("x\u4e2d\uffff", "\u4e2d"));
        assertEquals(MinimumCoverWindow.pack(0, 1),
                MinimumCoverWindow.javaSpan("\0\uffff", "\0"));
        assertEquals(0L, MinimumCoverWindow.javaSpan("\uffff", ""));
        assertEquals(-1L, MinimumCoverWindow.javaSpan("\0", "\uffff"));
    }

    @Test
    void exhaustiveSingletonCodeUnits() {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String source = new String(new char[] {'\uffff', (char) unit, (char) unit, '\0'});
            String required = String.valueOf((char) unit);
            assertEquals(MinimumCoverWindowJavaAtomTest.independent(source, required),
                    MinimumCoverWindow.javaSpan(source, required), "unit=" + unit);
        }
    }

    @Test
    void singletonAtMillionUnitBoundaryAndRefusals() {
        assertEquals(MinimumCoverWindow.pack(999999, 1000000),
                MinimumCoverWindow.javaSpan("a".repeat(999999) + "\uD800", "\uD800"));
        assertEquals(-1L, MinimumCoverWindow.javaSpan("\uffff\0", "a"));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> MinimumCoverWindow.javaSpan(null, "z"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> MinimumCoverWindow.javaSpan("z".repeat(1000001), "z"));
    }
}
