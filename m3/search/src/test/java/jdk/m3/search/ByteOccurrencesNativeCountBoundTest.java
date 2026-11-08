// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/ByteOccurrencesNativeCountBoundTest.java (blob d7feb2890354) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Existing Java strategy remains an independent oracle for JNI count hardening. */
final class ByteOccurrencesNativeCountBoundTest {
    @Test
    void javaFallbackRetainsOverlappingCountsAndExtremeCursors() {
        JavaAdaptiveSearchEngine provider = new JavaAdaptiveSearchEngine();
        Random random = new Random(0x4d334e4154495645L);
        int[] froms = {Integer.MIN_VALUE, -9, -1, 0, 1, 7,
                63, 128, Integer.MAX_VALUE};
        for (int iteration = 0; iteration < 5_000; ++iteration) {
            byte[] haystack = new byte[random.nextInt(129)];
            byte[] needle = new byte[1 + random.nextInt(33)];
            random.nextBytes(haystack);
            random.nextBytes(needle);
            if (iteration % 4 == 0) {
                Arrays.fill(haystack, (byte) 3);
                Arrays.fill(needle, (byte) 3);
            }
            try (CompiledSearch compiled = provider.compile(needle)) {
                for (int from : froms) {
                    assertEquals(oracle(haystack, needle, from),
                            ByteOccurrences.count(compiled, haystack, from, null),
                            "iteration=" + iteration + " from=" + from);
                }
            }
        }
    }

    @Test
    void argumentExceptionsRetainExistingOrdering() {
        var provider = new JavaAdaptiveSearchEngine();
        try (CompiledSearch compiled = provider.compile(new byte[]{1})) {
            assertThrows(NullPointerException.class,
                    () -> ByteOccurrences.count(null, new byte[]{1}, 0, null));
            assertThrows(NullPointerException.class,
                    () -> ByteOccurrences.count(compiled, null, 0, null));
            assertEquals(0, ByteOccurrences.count(compiled, new byte[0], 0, null));
            assertEquals(0, ByteOccurrences.count(compiled, new byte[]{1}, Integer.MAX_VALUE, null));
        }
    }

    private static int oracle(byte[] text, byte[] needle, int from) {
        int count = 0;
        for (int start = Math.max(0, from); start <= text.length - needle.length; ++start) {
            if (Arrays.equals(text, start, start + needle.length, needle, 0, needle.length)) {
                ++count;
            }
        }
        return count;
    }
}
