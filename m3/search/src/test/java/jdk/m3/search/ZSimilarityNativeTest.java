// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3ZSimilarityNativeTest.java (blob 273f0dd82a35) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Full JNI invocation is mandatory for this proof; no silent fallback or test skip. */
final class ZSimilarityNativeTest {
    @Test
    void executesTheNewNativeSymbolAgainstJavaAcrossUtf16AndLongSums() {
        String path = System.getProperty("m3.native.library.path");
        assertNotNull(path, "set -Dm3.native.library.path to the CMake-built library");
        assertTrue(Files.isRegularFile(Path.of(path)), "native test requires a real shared library");
        System.setProperty("m3.native.enabled", "true");
        assertTrue(new JniSearchEngine().available(), "native ABI-1 provider must load");
        char[] alphabet = {'a', 'b', '\u0000', '$', '\ud83d', '\ude80', '\uffff'};
        Random random = new Random(0x51A3BEEFL);
        for (int i = 0; i < 5_000; i++) {
            char[] chars = new char[random.nextInt(256)];
            for (int j = 0; j < chars.length; j++) {
                chars[j] = alphabet[random.nextInt(alphabet.length)];
            }
            String input = new String(chars);
            long expected = ZSimilarity.sumJava(input);
            assertEquals(expected, ZSimilarity.sumNativeRequired(input), "native sample=" + i);
            assertEquals(expected, ZSimilarity.sum(input), "dispatch sample=" + i);
        }
        int length = 100_000;
        String repeated = "a".repeat(length);
        assertEquals((long) length * (length + 1L) / 2L,
                ZSimilarity.sumNativeRequired(repeated));
        assertThrows(NullPointerException.class, () -> ZSimilarity.sumNativeRequired(null));
    }
}
