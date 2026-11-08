// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/FixedWordConcatenationTest.java (blob 5544b4d20f93) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Product JUnit; JNI parity stays in the independent executable oracle. */
final class FixedWordConcatenationTest {
    @Test void canonicalWordMultisetAndOrder() {
        assertArrayEquals(new int[] {0, 9}, FixedWordConcatenation.find(
                "barfoothefoobarman", new String[] {"foo", "bar"}));
        assertArrayEquals(new int[] {6, 9, 12}, FixedWordConcatenation.find(
                "barfoofoobarthefoobarman", new String[] {"bar", "foo", "the"}));
    }

    @Test void duplicatesAndOverlaps() {
        assertArrayEquals(new int[] {0, 1, 2}, FixedWordConcatenation.find(
                "aaaaaa", new String[] {"aa", "aa"}));
        assertArrayEquals(new int[0], FixedWordConcatenation.find(
                "wordgoodgoodgoodbestword", new String[] {"word", "good", "best", "word"}));
    }

    @Test void utf16AndBinaryNulAreLiteral() {
        assertArrayEquals(new int[] {0, 2, 4}, FixedWordConcatenation.find(
                "a\u0000b\uD800a\u0000b\uD800", new String[] {"a\u0000", "b\uD800"}));
        assertArrayEquals(new int[] {0}, FixedWordConcatenation.find(
                "\uD83D\uDE00\uD800\uDC00", new String[] {"\uD83D\uDE00", "\uD800\uDC00"}));
    }

    @Test void nullAndWidthsFailClosed() {
        assertThrows(NullPointerException.class, () -> FixedWordConcatenation.find(null,
                new String[] {"a"}));
        assertThrows(NullPointerException.class, () -> FixedWordConcatenation.find("a", null));
        assertThrows(IllegalArgumentException.class, () -> FixedWordConcatenation.find("a",
                new String[0]));
        assertThrows(IllegalArgumentException.class, () -> FixedWordConcatenation.find("ab",
                new String[] {"a", "bb"}));
    }
}
