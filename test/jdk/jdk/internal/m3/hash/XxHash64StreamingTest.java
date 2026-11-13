/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary Differential of the relocated xxHash64 streaming and file API (jdk.internal.m3.hash.XxHash64Streaming,
 *          recipe make/m3/qualification/hash-xxhash64-streaming-20261106): the donor contract (the known empty-input
 *          vector, 25 lengths x 4 seeds x 8 chunk widths of read-only big-endian heap slices, direct sliced buffers,
 *          reset and reuse, repeatable and intermediate getValue, files across the 256 KiB read boundary) against the
 *          one-shot of the owner, M3LongHashFunction.xx(seed).hashBytes(byte[]).
 * @modules java.base/jdk.internal.m3.hash:open
 * @run junit M3XxHash64StreamingTest
 */

import jdk.internal.m3.hash.LongHashFunction;
import jdk.internal.m3.hash.XxHash64Streaming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Contract checks for the existing array, ByteBuffer and file hashing interfaces. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-fileindex/src/test/java/com/synexia/fileindex/content/XXHash64StreamingTest.java blob e813c1ae9577; recipe make/m3/qualification/hash-xxhash64-streaming-20261106/recipe/relocate.py v4
final class XxHash64StreamingTest {
    private static final int[] LENGTHS = {
        0, 1, 3, 7, 8, 15, 16, 31, 32, 33, 63, 64, 65, 127, 128,
        255, 256, 257, 4_095, 4_096, 8_192, 262_143, 262_144, 262_145, 1_048_577
    };
    private static final long[] SEEDS = {0L, 1L, -1L, 0x9E3779B97F4A7C15L};

    @Test
    void knownXxhash64VectorAndEveryStreamingBoundaryAgree() {
        assertEquals(0xEF46DB3751D8E999L, oneShot(new byte[0], 0L));
        Random random = new Random(0x6d330c7L);
        for (int length : LENGTHS) {
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            for (long seed : SEEDS) {
                long expected = oneShot(bytes, seed);
                for (int chunk : new int[] {1, 7, 31, 32, 33, 65, 1_021, 262_144}) {
                    if (length > 8_192 && chunk < 1_021) {
                        continue;
                    }
                    XxHash64Streaming hash = new XxHash64Streaming(seed);
                    for (int start = 0; start < length; start += chunk) {
                        int end = Math.min(length, start + chunk);
                        ByteBuffer input = ByteBuffer.wrap(bytes, start, end - start)
                            .order(ByteOrder.BIG_ENDIAN).asReadOnlyBuffer();
                        int limit = input.limit();
                        hash.update(input);
                        assertEquals(limit, input.position(), "consume only the supplied range");
                        assertEquals(limit, input.limit(), "preserve caller's buffer limit");
                    }
                    assertEquals(expected, hash.getValue(), "length=" + length + " seed=" + seed
                        + " chunk=" + chunk);
                    assertEquals(expected, hash.getValue(), "getValue must be repeatable");
                    hash.reset();
                    hash.update(ByteBuffer.wrap(bytes));
                    assertEquals(expected, hash.getValue(), "reset and reuse");
                }

                ByteBuffer direct = ByteBuffer.allocateDirect(length + 17);
                direct.position(9);
                direct.put(bytes);
                direct.flip();
                direct.position(9);
                ByteBuffer slice = direct.slice().order(ByteOrder.BIG_ENDIAN);
                XxHash64Streaming fromDirect = new XxHash64Streaming(seed);
                fromDirect.update(slice);
                assertEquals(expected, fromDirect.getValue(), "direct sliced buffer");
                assertEquals(length, slice.position(), "consume direct slice exactly");
            }
        }
    }

    @Test
    void fileHashAgreesWithArrayAcrossBufferBoundary(@TempDir Path temp) throws IOException {
        Path path = temp.resolve("xxhash64.bin");
        Random random = new Random(0x5eedL);
        for (int length : LENGTHS) {
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            Files.write(path, bytes);
            assertEquals(oneShot(bytes), XxHash64Streaming.hashFile(path), "default seed length=" + length);
            for (long seed : SEEDS) {
                assertEquals(oneShot(bytes, seed), XxHash64Streaming.hashFile(path, seed),
                    "explicit seed length=" + length);
            }
        }
    }

    @Test
    void intermediateValueDoesNotModifyIncrementalState() {
        byte[] bytes = new byte[513];
        new Random(1729L).nextBytes(bytes);
        XxHash64Streaming hash = new XxHash64Streaming(0L);
        hash.update(ByteBuffer.wrap(bytes, 0, 47));
        assertEquals(oneShot(java.util.Arrays.copyOf(bytes, 47), 0L), hash.getValue());
        hash.update(ByteBuffer.wrap(bytes, 47, bytes.length - 47));
        assertEquals(oneShot(bytes, 0L), hash.getValue());
    }

    //FLLDCIM: RELOCATE - the donor's one-shot hash(byte[], seed) is the owner's existing API, M3LongHashFunction.xx(seed).hashBytes (no second kernel, no second facade)
    private static long oneShot(byte[] data, long seed) {
        return LongHashFunction.xx(seed).hashBytes(data);
    }

    private static long oneShot(byte[] data) {
        return oneShot(data, XxHash64Streaming.DEFAULT_SEED);
    }
}
