// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/JniSingleByteCountMemchrNativeTest.java (blob 65819ad839e9) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Actual provider-boundary JNI oracle; its test must not silently use a Java fallback. */
final class JniSingleByteCountMemchrNativeTest {
    @Test
    void countAllUnsignedBytesWithOverlapsAndBoundaries() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured, "compiled JNI library required");
        Path library = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(library), "native library unavailable");
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", library.toString());
        M3JniSearchEngine engine = new M3JniSearchEngine();
        assertTrue(engine.available(), "JNI must not fall back silently");
        Random random = new Random(0x4d334d33434f554eL);
        int[] lengths = {0, 1, 2, 3, 7, 31, 127, 2048};
        for (int wanted = 0; wanted < 256; ++wanted) {
            byte[] needle = {(byte) wanted};
            try (M3CompiledSearch compiled = engine.compile(needle)) {
                for (int length : lengths) {
                    for (int sample = 0; sample < 2; ++sample) {
                        byte[] input = new byte[length];
                        random.nextBytes(input);
                        if (sample == 1) Arrays.fill(input, (byte) wanted);
                        for (int from : new int[]{Integer.MIN_VALUE, -1, 0, 1,
                                length / 2, length - 1, length, length + 1, Integer.MAX_VALUE}) {
                            int expected = oracle(input, needle, from);
                            int actual = M3JniSearchEngine.countIfNative(
                                    compiled, input, from, new M3DefaultProgressMonitor());
                            assertEquals(expected, actual,
                                    "byte=" + wanted + " length=" + length + " from=" + from);
                        }
                    }
                }
            }
        }
    }

    @Test
    void multibyteAndPrefixInteractionStayOnOriginalKmpPath() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured);
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path",
                Path.of(configured).toAbsolutePath().normalize().toString());
        M3JniSearchEngine engine = new M3JniSearchEngine();
        assertTrue(engine.available());
        byte[] one = {(byte) 7};
        byte[] repeat = new byte[131072];
        Arrays.fill(repeat, one[0]);
        try (M3CompiledSearch compiled = engine.compile(one)) {
            assertEquals(repeat.length,
                    M3JniSearchEngine.countIfNative(compiled, repeat, 0, new M3DefaultProgressMonitor()));
            assertEquals(repeat.length - 3,
                    M3JniSearchEngine.countIfNative(compiled, repeat, 3, new M3DefaultProgressMonitor()));
        }
        for (byte[] needle : new byte[][]{{7, 7}, {0, 1, 0}, {8, 8, 8, 8}}) {
            try (M3CompiledSearch compiled = engine.compile(needle)) {
                for (int from : new int[]{-1, 0, 1, 10, repeat.length, Integer.MAX_VALUE}) {
                    assertEquals(oracle(repeat, needle, from), M3JniSearchEngine.countIfNative(
                            compiled, repeat, from, new M3DefaultProgressMonitor()));
                }
            }
        }
    }

    private static int oracle(byte[] input, byte[] needle, int from) {
        int found = 0;
        for (int i = Math.max(0, from); i <= input.length - needle.length; ++i) {
            boolean matches = true;
            for (int j = 0; j < needle.length; ++j) {
                if (input[i + j] != needle[j]) { matches = false; break; }
            }
            if (matches) ++found;
        }
        return found;
    }
}
