// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/MinimumSubsequenceWindowTest.java (blob 01b1caaff229) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Product-level JUnit contract. Actual Maven execution is separately required. */
final class MinimumSubsequenceWindowTest {
    @Test void orderedCoverageIsNotMultisetCoverage() {
        assertEquals(M3MinimumSubsequenceWindow.pack(1, 5),
                M3MinimumSubsequenceWindow.find("abcdebdde", "bde"));
        assertEquals(M3MinimumSubsequenceWindow.pack(0, 4),
                M3MinimumSubsequenceWindow.find("abcd", "ad"));
        assertEquals(-1L, M3MinimumSubsequenceWindow.find("cba", "abc"));
    }

    @Test void emptyNoMatchAndEarliestTie() {
        assertEquals(0L, M3MinimumSubsequenceWindow.find("abc", ""));
        assertEquals(-1L, M3MinimumSubsequenceWindow.find("", "a"));
        assertEquals(M3MinimumSubsequenceWindow.pack(0, 2),
                M3MinimumSubsequenceWindow.find("abab", "ab"));
    }

    @Test void utf16UnitSemanticsAndDuplicateRequirements() {
        assertEquals(M3MinimumSubsequenceWindow.pack(0, 3),
                M3MinimumSubsequenceWindow.find("\uD800\u0000\uDC00", "\uD800\uDC00"));
        assertEquals(M3MinimumSubsequenceWindow.pack(0, 4),
                M3MinimumSubsequenceWindow.find("aaaaa", "aaaa"));
    }

    @Test void explicitInputBudgetAndNullRefusal() {
        assertThrows(NullPointerException.class,
                () -> M3MinimumSubsequenceWindow.find(null, "a"));
        assertThrows(NullPointerException.class,
                () -> M3MinimumSubsequenceWindow.find("a", null));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumSubsequenceWindow.find("a".repeat(1_000_001), ""));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumSubsequenceWindow.find("abc", "x".repeat(4_097)));
        assertThrows(IllegalArgumentException.class,
                () -> M3MinimumSubsequenceWindow.find("a".repeat(8_000),
                        "a".repeat(4_096)));
    }
}
