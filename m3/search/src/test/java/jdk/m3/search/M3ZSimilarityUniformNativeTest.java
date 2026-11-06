// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3ZSimilarityUniformNativeTest.java (blob 0bfeb575adec) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Original JNI ABI-1 library required; the uniform shortcut must be bit-exact. */
final class M3ZSimilarityUniformNativeTest {
    private static void requireNativeLibrary() {
        String path = System.getProperty("m3.native.library.path");
        assertNotNull(path, "set -Dm3.native.library.path to the CMake-built library");
        assertTrue(Files.isRegularFile(Path.of(path)), "a real CMake-built library is required");
        System.setProperty("m3.native.enabled", "true");
        assertTrue(new M3JniSearchEngine().available(), "JNI ABI-1 provider must load");
    }

    @Test
    void uniformUtf16RunsMatchExactTriangularSum() {
        requireNativeLibrary();
        for (char unit : new char[] {'a', '\u0000', '\ud800', '\udc00', '\uffff'}) {
            for (int length : new int[] {2, 3, 4, 255, 4096, 65_536, 100_000}) {
                String value = String.valueOf(unit).repeat(length);
                long expected = (long) length * (length + 1L) / 2L;
                assertEquals(expected, M3ZSimilarity.sumJava(value), "Java reference length=" + length);
                assertEquals(expected, M3ZSimilarity.sumNativeRequired(value),
                        "JNI uniform length=" + length + " unit=" + (int) unit);
            }
        }
    }

    @Test
    void nearUniformInputsNeverTakeTheTriangularShortcut() {
        requireNativeLibrary();
        for (int length : new int[] {2, 3, 4, 255, 4096, 65_536, 100_000}) {
            String prefixMismatch = "b" + "a".repeat(length - 1);
            String suffixMismatch = "a".repeat(length - 1) + "b";
            String interiorMismatch = "a".repeat(length / 2) + "b"
                    + "a".repeat(length - length / 2 - 1);
            for (String value : new String[] {prefixMismatch, suffixMismatch, interiorMismatch}) {
                assertEquals(M3ZSimilarity.sumJava(value), M3ZSimilarity.sumNativeRequired(value),
                        "native Z-fallback length=" + length);
            }
        }
        assertEquals(0L, M3ZSimilarity.sumNativeRequired(""));
        assertEquals(1L, M3ZSimilarity.sumNativeRequired("\ud800"));
    }
}
