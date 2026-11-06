// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/MinimumCoverWindowRangeOffsetJavaTest.java (blob 32f23fc23f94) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Independent Java 21 per-atom source-bound range-offset checks. */
final class MinimumCoverWindowRangeOffsetJavaTest {
    @Test
    void nonzeroRequiredMinimumAndUnicodeGapsMatchIndependentOracle() {
        String source = "\0a\uffffb\uD800\uDC00\u4e2d\uffffz\0\u4e2d";
        for (String required : new String[]{"\0", "\uffff", "\uffff\uffff",
                "\uD800", "\uDC00", "\u4e2d", "a", "ab", "\0\uffff",
                "a\uffff", "\u4e2d\u4e2d", "b\u4e2d"}) {
            assertEquals(MinimumCoverWindowJavaAtomTest.independent(source, required),
                    M3MinimumCoverWindow.javaSpan(source, required), required);
        }
    }
    @Test
    void shiftedIndexPreservesEarliestStartAndEmptyWindow() {
        assertEquals(M3MinimumCoverWindow.pack(1, 2),
                M3MinimumCoverWindow.javaSpan("\0\uffff\0", "\uffff"));
        assertEquals(M3MinimumCoverWindow.pack(1, 2),
                M3MinimumCoverWindow.javaSpan("x\u4e2d\uffff", "\u4e2d"));
        assertEquals(M3MinimumCoverWindow.pack(0, 1),
                M3MinimumCoverWindow.javaSpan("\0\uffff", "\0"));
        assertEquals(0L, M3MinimumCoverWindow.javaSpan("\uffff", ""));
        assertEquals(-1L, M3MinimumCoverWindow.javaSpan("\0", "\uffff"));
    }
}
