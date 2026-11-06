// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JniPairDensityCrossoverNativeTest.java (blob 6e098fbeca0d) by M3_SEARCH_RELOCATE v2.
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

/** Source-sealed native pair search: sparse-first-to-dense continuation witness. */
final class JniPairDensityCrossoverNativeTest {
    private static M3JniSearchEngine engine() {
        String configured = System.getProperty("m3.native.library.path");
        assertNotNull(configured, "real JNI native library is mandatory");
        Path library = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(library), "native library missing");
        System.setProperty("m3.native.enabled", "true");
        System.setProperty("m3.native.library.path", library.toString());
        M3JniSearchEngine engine = new M3JniSearchEngine();
        assertTrue(engine.available(), "JNI library must be loadable, no Java fallback");
        return engine;
    }

    private static int first(byte[] haystack, byte a, byte b, int start) {
        int from = Math.max(0, start);
        if (haystack.length < 2 || from > haystack.length - 2) return -1;
        for (int offset = from; offset <= haystack.length - 2; offset++) {
            if (haystack[offset] == a && haystack[offset + 1] == b) return offset;
        }
        return -1;
    }

    @Test
    void sparsePrefixDenseSuffixAndBoundaryHitsRetainFirstOffset() {
        M3JniSearchEngine engine = engine();
        byte[][] pairs = {{'a', 'b'}, {(byte) 0x80, (byte) 0xff}, {0, 1}};
        int[] lengths = {4095, 4096, 4097, 8192, 65536, 131072};
        for (byte[] pair : pairs) {
            try (M3CompiledSearch search = engine.compile(pair)) {
                for (int length : lengths) {
                    for (int shape = 0; shape < 4; shape++) {
                        byte[] text = new byte[length];
                        Arrays.fill(text, pair[0]);
                        Arrays.fill(text, 0, Math.min(64, length), (byte) (pair[0] + 1));
                        if (shape == 1) {
                            text[length - 2] = pair[0];
                            text[length - 1] = pair[1];
                        } else if (shape == 2) {
                            text[64] = pair[0];
                            text[65] = pair[1];
                        } else if (shape == 3) {
                            text[512] = pair[0];
                            text[513] = pair[1];
                        }
                        ByteBuffer direct = ByteBuffer.allocateDirect(length + 8);
                        for (int i = 0; i < length; i++) direct.put(3 + i, text[i]);
                        direct.position(3);
                        direct.limit(3 + length);
                        ByteBuffer readOnly = direct.asReadOnlyBuffer();
                        int[] starts = {Integer.MIN_VALUE, -1, 0, 1, 63, 64, 65, 512,
                                length - 2, length - 1, length, Integer.MAX_VALUE};
                        for (int from : starts) {
                            int expected = first(text, pair[0], pair[1], from);
                            assertEquals(expected, search.indexOf(text, from), "array first offset");
                            assertEquals(expected, search.indexOf(direct, from), "direct first offset");
                            assertEquals(expected, search.indexOf(readOnly, from), "read-only first offset");
                            assertEquals(3, direct.position(), "buffer position");
                            assertEquals(length + 3, direct.limit(), "buffer limit");
                        }
                    }
                }
            }
        }
    }

    @Test
    void randomSignedBytesAndOwnershipRetainPriorFirstMatchContract() {
        M3JniSearchEngine engine = engine();
        Random random = new Random(202611051846L);
        for (int trial = 0; trial < 512; trial++) {
            byte[] pair = {(byte) random.nextInt(256), (byte) random.nextInt(256)};
            byte[] original = pair.clone();
            try (M3CompiledSearch search = engine.compile(pair)) {
                Arrays.fill(pair, (byte) 42); // caller mutation must not affect native pattern
                byte[] text = new byte[4096 + random.nextInt(8192)];
                random.nextBytes(text);
                int from = random.nextInt(text.length + 512) - 256;
                assertEquals(first(text, original[0], original[1], from),
                        search.indexOf(text, from), "signed-byte oracle trial=" + trial);
            }
        }
    }
}
