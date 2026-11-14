// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JniSingleByteMemchrNativeTest.java (blob c8c0bdc0a2e8) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Full owner-JNI result, first-byte identity, and direct-buffer contract oracle. */
final class JniSingleByteMemchrNativeTest {
    @Test
    void everyUnsignedByteAndFirstMatchBoundaryMatchesAnIndependentJavaOracle() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured, "the actual compiled JNI library is mandatory");
        Path nativeLibrary = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(nativeLibrary));
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", nativeLibrary.toString());

        JniSearchEngine engine = new JniSearchEngine();
        assertTrue(engine.available(), "do not replace unavailable JNI with a Java fallback");
        Random random = new Random(0x4d334d454d434852L);
        int[] lengths = {0, 1, 2, 3, 7, 32, 257, 4096};

        for (int wanted = 0; wanted < 256; wanted++) {
            try (CompiledSearch compiled = engine.compile(new byte[] {(byte) wanted})) {
                for (int length : lengths) {
                    for (int trial = 0; trial < 4; trial++) {
                        byte[] values = new byte[length];
                        random.nextBytes(values);
                        if (length > 0 && trial == 1) values[0] = (byte) wanted;
                        if (length > 0 && trial == 2) values[length - 1] = (byte) wanted;
                        if (length > 0 && trial == 3) Arrays.fill(values, (byte) wanted);

                        ByteBuffer direct = ByteBuffer.allocateDirect(length + 8);
                        for (int i = 0; i < length; i++) direct.put(3 + i, values[i]);
                        direct.position(3);
                        direct.limit(3 + length);
                        int[] starts = {Integer.MIN_VALUE, -10, -1, 0, 1,
                                length / 2, length - 1, length, length + 1, Integer.MAX_VALUE};
                        for (int from : starts) {
                            int expected = first(values, (byte) wanted, from);
                            assertEquals(expected, compiled.indexOf(values, from),
                                    "array byte=" + wanted + " length=" + length
                                            + " trial=" + trial + " from=" + from);
                            assertEquals(expected, compiled.indexOf(direct, from),
                                    "direct byte=" + wanted + " length=" + length
                                            + " trial=" + trial + " from=" + from);
                            assertEquals(3, direct.position(), "read-only direct-buffer position");
                            assertEquals(3 + length, direct.limit(), "direct-buffer limit");
                        }
                    }
                }
            }
        }
    }

    @Test
    void longAbsentAndNearEndSingleByteSearchRetainsRelativeOffsets() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured);
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path",
                Path.of(configured).toAbsolutePath().normalize().toString());
        JniSearchEngine engine = new JniSearchEngine();
        assertTrue(engine.available());

        byte[] values = new byte[1 << 20];
        Arrays.fill(values, (byte) 5);
        values[values.length - 1] = (byte) 0xff;
        try (CompiledSearch compiled = engine.compile(new byte[] {(byte) 0xff})) {
            assertEquals(values.length - 1, compiled.indexOf(values, 0));
            assertEquals(values.length - 1, compiled.indexOf(values, values.length - 1));
            assertEquals(-1, compiled.indexOf(values, values.length));
            ByteBuffer direct = ByteBuffer.allocateDirect(values.length + 6);
            for (int i = 0; i < values.length; i++) direct.put(2 + i, values[i]);
            direct.position(2);
            direct.limit(2 + values.length);
            assertEquals(values.length - 1, compiled.indexOf(direct, 0));
            assertEquals(-1, compiled.indexOf(direct, values.length));
        }
    }

    private static int first(byte[] array, byte target, int from) {
        for (int index = Math.max(0, from); index < array.length; index++) {
            if (array[index] == target) return index;
        }
        return -1;
    }
}
