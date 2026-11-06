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
        assertArrayEquals(new int[] {0, 9}, M3FixedWordConcatenation.find(
                "barfoothefoobarman", new String[] {"foo", "bar"}));
        assertArrayEquals(new int[] {6, 9, 12}, M3FixedWordConcatenation.find(
                "barfoofoobarthefoobarman", new String[] {"bar", "foo", "the"}));
    }

    @Test void duplicatesAndOverlaps() {
        assertArrayEquals(new int[] {0, 1, 2}, M3FixedWordConcatenation.find(
                "aaaaaa", new String[] {"aa", "aa"}));
        assertArrayEquals(new int[0], M3FixedWordConcatenation.find(
                "wordgoodgoodgoodbestword", new String[] {"word", "good", "best", "word"}));
    }

    @Test void utf16AndBinaryNulAreLiteral() {
        assertArrayEquals(new int[] {0, 2, 4}, M3FixedWordConcatenation.find(
                "a\u0000b\uD800a\u0000b\uD800", new String[] {"a\u0000", "b\uD800"}));
        assertArrayEquals(new int[] {0}, M3FixedWordConcatenation.find(
                "\uD83D\uDE00\uD800\uDC00", new String[] {"\uD83D\uDE00", "\uD800\uDC00"}));
    }

    @Test void nullAndWidthsFailClosed() {
        assertThrows(NullPointerException.class, () -> M3FixedWordConcatenation.find(null,
                new String[] {"a"}));
        assertThrows(NullPointerException.class, () -> M3FixedWordConcatenation.find("a", null));
        assertThrows(IllegalArgumentException.class, () -> M3FixedWordConcatenation.find("a",
                new String[0]));
        assertThrows(IllegalArgumentException.class, () -> M3FixedWordConcatenation.find("ab",
                new String[] {"a", "bb"}));
    }
}
