// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/M3ZSimilarityPeriodTwoNativeTest.java (blob 4a3d9943de10) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Exact JNI period-two sum: independent Java Z-array oracle and real library required. */
final class ZSimilarityPeriodTwoNativeTest {
    private static void requireNative() {
        String path = System.getProperty("m3.native.library.path");
        assertNotNull(path, "a real combined JNI library path is required");
        assertTrue(Files.isRegularFile(Path.of(path)), "CMake-built JNI file required");
        System.setProperty("m3.native.enabled", "true");
        assertTrue(new JniSearchEngine().available(), "native ABI-1 is required; no fallback");
    }

    @Test
    void alternatingDistinctUtf16PairsHaveExactLongSum() {
        requireNative();
        char[][] pairs = {{'a', 'b'}, {'\u0000', '\uffff'},
                {'\ud800', '\udc00'}, {'\u20ac', 'a'}};
        for (int length : new int[] {2, 3, 4, 5, 7, 63, 64, 65, 4096, 65_535, 65_536, 100_000}) {
            for (char[] pair : pairs) {
                char[] units = new char[length];
                for (int i = 0; i < units.length; i++) units[i] = pair[i & 1];
                String input = new String(units);
                long m = (length - 1L) / 2L;
                long expected = length + m * length - m * (m + 1);
                assertEquals(expected, ZSimilarity.sumJava(input), "Java period2 baseline");
                assertEquals(expected, ZSimilarity.sumNativeRequired(input),
                        "JNI period2 length=" + length + " unit=" + (int) pair[0]);
            }
        }
    }

    @Test
    void nearPeriodicUnitAtSampledOrUnprobedOffsetFallsBackExactly() {
        requireNative();
        for (int length : new int[] {2, 3, 63, 64, 65, 4096, 65_536, 100_000}) {
            char[] units = new char[length];
            for (int i = 0; i < length; i++) units[i] = (i & 1) == 0 ? 'a' : 'b';
            int[] offsets = {0, 1, 2, length / 4, length / 2,
                    length - length / 4, length - 2, length - 1, Math.min(101, length - 1)};
            for (int offset : offsets) {
                if (offset >= length) continue; char original = units[offset]; // DONOR_TEST_DEFECT: offsets {0, 1, 2, ...} name index 2 for length 2
                units[offset] = 'c';
                String input = new String(units);
                assertEquals(ZSimilarity.sumJava(input), ZSimilarity.sumNativeRequired(input),
                        "near-period2 length=" + length + " mismatch=" + offset);
                units[offset] = original;
            }
        }
    }
}
