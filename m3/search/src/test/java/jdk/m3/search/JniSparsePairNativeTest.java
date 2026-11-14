// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JniSparsePairNativeTest.java (blob a796ffd987ca) by M3_SEARCH_RELOCATE v2.
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

/** Executed original JNI owner oracle for the source-sealed sparse two-byte specialization. */
final class JniSparsePairNativeTest {
    private static JniSearchEngine requiredNative() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured, "compiled original JNI library must be supplied");
        Path library = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(library), "JNI shared library does not exist");
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", library.toString());
        JniSearchEngine engine = new JniSearchEngine();
        assertTrue(engine.available(), "native execution is mandatory: Java fallback is insufficient");
        return engine;
    }

    private static int oracle(byte[] text, byte[] pair, int fromIndex) {
        int first = Math.max(0, fromIndex);
        if (text.length < pair.length || first > text.length - pair.length) return -1;
        for (int index = first; index <= text.length - pair.length; index++) {
            if (text[index] == pair[0] && text[index + 1] == pair[1]) return index;
        }
        return -1;
    }

    @Test
    void signedBytePairsAcrossSparseDenseAndExtremeOffsetsMatchTheIndependentOracle() {
        JniSearchEngine engine = requiredNative();
        Random random = new Random(20261105L);
        int[] lengths = {0, 1, 2, 3, 63, 1024, 4095, 4096, 4097, 8192, 16384};
        byte[][] patterns = {
                {(byte) 0x00, (byte) 0xff},
                {(byte) 0x80, (byte) 0x7f},
                {(byte) 'a', (byte) 'b'},
                {(byte) 'a', (byte) 'a'},
                {(byte) 0xff, (byte) 0x00},
                {(byte) 0xff, (byte) 0xff}
        };
        for (byte[] pair : patterns) {
            try (CompiledSearch compiled = engine.compile(pair)) {
                for (int length : lengths) {
                    for (int shape = 0; shape < 5; shape++) {
                        byte[] text = new byte[length];
                        random.nextBytes(text);
                        if (shape == 1) Arrays.fill(text, pair[0]);
                        if (shape == 2) Arrays.fill(text, (byte) (pair[0] + 1));
                        if (shape == 3 && length >= 2) {
                            text[length - 2] = pair[0];
                            text[length - 1] = pair[1];
                        }
                        if (shape == 4 && length >= 2) {
                            text[0] = pair[0];
                            text[1] = pair[1];
                        }
                        ByteBuffer direct = ByteBuffer.allocateDirect(length + 8);
                        for (int i = 0; i < length; i++) direct.put(3 + i, text[i]);
                        direct.position(3);
                        direct.limit(3 + length);
                        ByteBuffer readOnly = direct.asReadOnlyBuffer();
                        int[] offsets = {
                                Integer.MIN_VALUE, -9, -1, 0, 1, length / 2,
                                Math.max(0, length - 2), length - 1, length,
                                length + 1, Integer.MAX_VALUE
                        };
                        for (int from : offsets) {
                            int expected = oracle(text, pair, from);
                            assertEquals(expected, compiled.indexOf(text, from),
                                    "JNI array pair=" + Arrays.toString(pair) + " length=" + length
                                            + " shape=" + shape + " from=" + from);
                            assertEquals(expected, compiled.indexOf(direct, from), "JNI direct mismatch");
                            assertEquals(expected, compiled.indexOf(readOnly, from),
                                    "JNI read-only direct mismatch");
                            assertEquals(3, direct.position(), "caller position changed");
                            assertEquals(length + 3, direct.limit(), "caller limit changed");
                            assertEquals(3, readOnly.position(), "read-only position changed");
                            assertEquals(length + 3, readOnly.limit(), "read-only limit changed");
                        }
                    }
                }
            }
        }
    }

    @Test
    void millionByteSparseAndDenseCasesKeepLongRangeAndNeedleOwnership() {
        JniSearchEngine engine = requiredNative();
        byte[][] pairs = {{1, 2}, {(byte) 0xff, 0}, {'a', 'b'}, {'a', 'a'}};
        for (byte[] pair : pairs) {
            byte[] original = pair.clone();
            try (CompiledSearch compiled = engine.compile(pair)) {
                Arrays.fill(pair, (byte) 0x7f);
                byte[] text = new byte[1 << 20];
                Arrays.fill(text, original[0]);
                text[text.length - 2] = original[0];
                text[text.length - 1] = original[1];
                int first = oracle(text, original, 0);
                assertEquals(first, compiled.indexOf(text, 0));
                assertEquals(text.length - 2, compiled.indexOf(text, text.length - 2));
                assertEquals(-1, compiled.indexOf(text, text.length - 1));
            }
        }
    }
}
