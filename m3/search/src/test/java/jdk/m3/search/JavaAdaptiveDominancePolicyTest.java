// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JavaAdaptiveDominancePolicyTest.java (blob 87652873534a) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Canonical first-party KMP/Horspool strategy semantics, including 32-bit overflow edges. */
final class JavaAdaptiveDominancePolicyTest {
    @Test
    void matchesIndependentOriginalStrategyForAllBoundedByteDistributions() {
        Random random = new Random(0x4d33444f4d494e41L);
        JavaAdaptiveSearchEngine engine = new JavaAdaptiveSearchEngine();
        long compared = 0;
        for (int sample = 0; sample < 24_000; sample++) {
            int length = 1 + random.nextInt(513);
            int alphabet = switch (sample % 7) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 3;
                case 3 -> 4;
                case 4 -> 16;
                case 5 -> 127;
                default -> 256;
            };
            byte[] needle = new byte[length];
            for (int i = 0; i < needle.length; i++) {
                needle[i] = (byte) random.nextInt(alphabet);
            }
            String expected = referenceStrategy(needle);
            try (CompiledSearch compiled = engine.compile(needle)) {
                assertEquals(expected, compiled.strategy(), "sample=" + sample);
            }
            compared++;
        }
        assertEquals(24_000L, compared);
    }

    @Test
    void meetsExactMajorityBoundariesAndPreservesShortNeedlePolicy() {
        JavaAdaptiveSearchEngine engine = new JavaAdaptiveSearchEngine();
        for (int length = 1; length <= 3; length++) {
            byte[] shortNeedle = new byte[length];
            for (int i = 0; i < length; i++) shortNeedle[i] = (byte) i;
            try (CompiledSearch compiled = engine.compile(shortNeedle)) {
                assertEquals("KMP", compiled.strategy());
            }
        }
        for (int length = 4; length <= 513; length++) {
            int threshold = (length >>> 1) + (length & 1);
            byte[] concentrated = new byte[length];
            Arrays.fill(concentrated, 0, threshold, (byte) -128);
            for (int i = threshold; i < concentrated.length; i++) {
                concentrated[i] = (byte) (i % 127);
            }
            try (CompiledSearch compiled = engine.compile(concentrated)) {
                assertEquals("KMP", compiled.strategy(), "length=" + length);
            }
        }
        assertEquals("BOYER_MOORE_HORSPOOL", strategy(new byte[] {0, 1, 2, 3, 4, 5, 6, 7}));
        assertEquals("KMP", strategy(new byte[] {0, 0, 0, 0, 1, 2, 3, 4}));
        assertEquals("BOYER_MOORE_HORSPOOL", strategy(new byte[] {0, 0, 0, 1, 1, 2, 2, 3}));
    }

    @Test
    void majorityThresholdIsOverflowSafeAcrossFullIntDomain() throws Exception {
        Method threshold = JavaAdaptiveSearchEngine.class.getDeclaredMethod("majorityThreshold", int.class);
        threshold.setAccessible(true);
        int[] lengths = {0, 1, 2, 3, 4, 5, 1_073_741_822,
                1_073_741_823, 1_073_741_824, 1_073_741_825,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int length : lengths) {
            int expected = (int) (((long) length + 1L) / 2L);
            assertEquals(expected, ((Integer) threshold.invoke(null, length)).intValue(),
                    "length=" + length);
        }
        assertEquals(1_073_741_824, ((Integer) threshold.invoke(null, Integer.MAX_VALUE)).intValue());
    }

    @Test
    void snapshotsCallerBytesAndRetainsNullAndEmptyValidation() {
        JavaAdaptiveSearchEngine engine = new JavaAdaptiveSearchEngine();
        assertThrows(NullPointerException.class, () -> engine.compile(null));
        assertThrows(IllegalArgumentException.class, () -> engine.compile(new byte[0]));
        byte[] needle = {100, 100, 100, 100, 2, 3, 4, 5};
        try (CompiledSearch compiled = engine.compile(needle)) {
            Arrays.fill(needle, (byte) 1);
            assertEquals("KMP", compiled.strategy());
            assertEquals(2, compiled.indexOf(new byte[] {1, 2, 100, 100, 100, 100, 2, 3, 4, 5}, 0));
        }
    }

    private static String strategy(byte[] needle) {
        try (CompiledSearch compiled = new JavaAdaptiveSearchEngine().compile(needle)) {
            return compiled.strategy();
        }
    }

    /** Independent finite input oracle: original full-frequency count and multiply rule. */
    private static String referenceStrategy(byte[] needle) {
        if (needle.length < 4) return "KMP";
        int[] counts = new int[256];
        int max = 0;
        for (byte value : needle) {
            max = Math.max(max, ++counts[Byte.toUnsignedInt(value)]);
        }
        return max * 2 >= needle.length ? "KMP" : "BOYER_MOORE_HORSPOOL";
    }
}
