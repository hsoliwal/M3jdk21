// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JniSingleByteReverseNativeTest.java (blob 1a9ad33ac2c9) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Native JNI-ABI1 reverse single-byte array/direct parity, with no Java fallback. */
final class JniSingleByteReverseNativeTest {
    private static JniSearchEngine nativeEngine() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured, "a compiled native library must be supplied");
        Path library = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(library), "native library is missing");
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", library.toString());
        JniSearchEngine engine = new JniSearchEngine();
        assertTrue(engine.available(), "JNI must execute, not a Java fallback");
        return engine;
    }

    @Test
    void everyOctetReverseBoundMatchesIndependentJavaAndDirectBufferOracle() {
        JniSearchEngine engine = nativeEngine();
        Random random = new Random(0x4d33524556313231L);
        int[] lengths = {0, 1, 2, 3, 7, 32, 257, 4096};
        for (int octet = 0; octet < 256; octet++) {
            try (CompiledSearch compiled = engine.compile(new byte[] {(byte) octet})) {
                assertEquals(1, compiled.needleLength());
                for (int length : lengths) {
                    for (int trial = 0; trial < 4; trial++) {
                        byte[] bytes = new byte[length];
                        random.nextBytes(bytes);
                        if (length > 0 && trial == 1) bytes[0] = (byte) octet;
                        if (length > 0 && trial == 2) bytes[length - 1] = (byte) octet;
                        if (trial == 3) Arrays.fill(bytes, (byte) octet);
                        byte[] original = bytes.clone();
                        ByteBuffer direct = ByteBuffer.allocateDirect(length + 8);
                        for (int i = 0; i < length; i++) direct.put(3 + i, bytes[i]);
                        direct.position(3);
                        direct.limit(3 + length);
                        int[] starts = {Integer.MIN_VALUE, -10, -1, 0, 1,
                                length / 2, length - 1, length, length + 1,
                                Integer.MAX_VALUE};
                        for (int from : starts) {
                            int expected = last(bytes, (byte) octet, from);
                            assertEquals(expected, compiled.lastIndexOf(bytes, from),
                                    "array octet=" + octet + " length=" + length
                                            + " trial=" + trial + " from=" + from);
                            assertEquals(expected, compiled.lastIndexOf(direct, from),
                                    "direct octet=" + octet + " length=" + length
                                            + " trial=" + trial + " from=" + from);
                            assertEquals(3, direct.position(), "direct buffer position");
                            assertEquals(3 + length, direct.limit(), "direct buffer limit");
                        }
                        assertArrayEquals(original, bytes, "haystack unchanged");
                    }
                }
            }
        }
    }

    @Test
    void longAbsentDenseAndNearEndReverseSearchPreservesLogicalOffsets() {
        JniSearchEngine engine = nativeEngine();
        byte[] bytes = new byte[1 << 20];
        Arrays.fill(bytes, (byte) 5);
        bytes[0] = (byte) 0xff;
        bytes[bytes.length - 1] = (byte) 0xff;
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 5);
        for (int i = 0; i < bytes.length; i++) direct.put(2 + i, bytes[i]);
        direct.position(2);
        direct.limit(2 + bytes.length);
        try (CompiledSearch compiled = engine.compile(new byte[] {(byte) 0xff})) {
            assertEquals(bytes.length - 1, compiled.lastIndexOf(bytes, Integer.MAX_VALUE));
            assertEquals(0, compiled.lastIndexOf(bytes, bytes.length - 2));
            assertEquals(-1, compiled.lastIndexOf(bytes, -1));
            assertEquals(bytes.length - 1, compiled.lastIndexOf(direct, Integer.MAX_VALUE));
            assertEquals(0, compiled.lastIndexOf(direct, bytes.length - 2));
            assertEquals(-1, compiled.lastIndexOf(direct, -1));
        }
        try (CompiledSearch absent = engine.compile(new byte[] {(byte) 0x80})) {
            assertEquals(-1, absent.lastIndexOf(bytes, Integer.MAX_VALUE));
            assertEquals(-1, absent.lastIndexOf(direct, Integer.MAX_VALUE));
        }
    }

    private static int last(byte[] bytes, byte wanted, int from) {
        if (from < 0) return -1;
        for (int i = Math.min(from, bytes.length - 1); i >= 0; --i) {
            if (bytes[i] == wanted) return i;
        }
        return -1;
    }
}
