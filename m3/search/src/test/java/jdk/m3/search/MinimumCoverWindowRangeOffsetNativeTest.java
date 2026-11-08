// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/MinimumCoverWindowRangeOffsetNativeTest.java (blob 05727f41786f) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Separate native C17/JNI per-atom range-offset proof, opt-in only. */
@EnabledIfSystemProperty(named = "m3.minimum.cover.native.junit", matches = "true")
final class MinimumCoverWindowRangeOffsetNativeTest {
    @BeforeAll
    static void loadNative() {
        String path = System.getProperty("m3.minimum.cover.library");
        if (path == null || path.isBlank()) throw new IllegalStateException("native library required");
        System.load(path);
    }
    @Test
    void nativeNonzeroOffsetMatchesIndependentOracleWithoutArrayWrites() {
        String source = "\0a\uffffb\uD800\uDC00\u4e2d\uffffz\0\u4e2d";
        for (String required : new String[]{"\0", "\uffff", "\uffff\uffff",
                "\uD800", "\uDC00", "\u4e2d", "a", "ab", "\0\uffff",
                "a\uffff", "\u4e2d\u4e2d", "b\u4e2d"}) {
            char[] haystack = source.toCharArray();
            char[] pattern = required.toCharArray();
            char[] copySource = haystack.clone();
            char[] copyPattern = pattern.clone();
            assertEquals(MinimumCoverWindowJavaAtomTest.independent(source, required),
                    MinimumCoverWindow.nativeSpan(haystack, pattern), required);
            assertArrayEquals(copySource, haystack);
            assertArrayEquals(copyPattern, pattern);
        }
    }
    @Test
    void aliasedCharArraysAndHighSparseCodeUnitsAreStable() {
        char[] shared = "\uffff\0\u4e2d".toCharArray();
        char[] copy = shared.clone();
        assertEquals(MinimumCoverWindow.pack(0, shared.length),
                MinimumCoverWindow.nativeSpan(shared, shared));
        assertArrayEquals(copy, shared);
        assertEquals(MinimumCoverWindow.pack(1, 2),
                MinimumCoverWindow.nativeSpan("\0\uffff\0".toCharArray(), new char[]{'\uffff'}));
    }
}
