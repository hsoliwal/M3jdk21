/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary Dependency-free parity proof of M3XxHash64Streaming (recipe m3/qualification/hash-xxhash64-streaming-20261106):
 *          17 lengths x 4 seeds, 7-byte and 4,097-byte updates, files, the known empty-input vector, every value equal
 *          to the one-shot of the owner, M3LongHashFunction.xx(seed).hashBytes(byte[]).
 * @modules java.base/jdk.internal.m3.hash:open
 * @run main M3XxHash64StreamingProof
 */

import jdk.internal.m3.hash.M3LongHashFunction;
import jdk.internal.m3.hash.M3XxHash64Streaming;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Dependency-free parity probe for Java 21, independent from JUnit / Maven. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-fileindex/src/test/java/com/synexia/fileindex/content/XXHash64StandaloneProof.java blob a5303cc84a1b; recipe m3/qualification/hash-xxhash64-streaming-20261106/recipe/relocate.py v4
public final class M3XxHash64StreamingProof {
    private M3XxHash64StreamingProof() {}

    public static void main(String[] args) throws Exception {
        int[] lengths = {0, 1, 7, 31, 32, 33, 63, 64, 65, 255, 256,
            4_095, 4_096, 262_143, 262_144, 262_145, 1_048_577};
        long[] seeds = {0L, 1L, -1L, 0x9E3779B97F4A7C15L};
        if (oneShot(new byte[0], 0L) != 0xEF46DB3751D8E999L) {
            throw new AssertionError("xxHash64 known empty input vector changed");
        }
        Random random = new Random(42L);
        Path file = Files.createTempFile("m3-xxhash64-proof-", ".bin");
        try {
            for (int length : lengths) {
                byte[] data = new byte[length];
                random.nextBytes(data);
                Files.write(file, data);
                for (long seed : seeds) {
                    long expected = oneShot(data, seed);
                    M3XxHash64Streaming streamed = new M3XxHash64Streaming(seed);
                    int size = length > 8_192 ? 4_097 : 7;
                    for (int i = 0; i < length; i += size) {
                        streamed.update(ByteBuffer.wrap(data, i, Math.min(size, length - i)));
                    }
                    if (streamed.getValue() != expected || M3XxHash64Streaming.hashFile(file, seed) != expected) {
                        throw new AssertionError("xxHash64 parity error at length=" + length
                            + ", seed=" + seed);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("PASS: 17 lengths x 4 seeds, streamed and file parity, zero-seed vector");
    }

    //FLLDCIM: RELOCATE - the donor's one-shot hash(byte[], seed) is the owner's existing API, M3LongHashFunction.xx(seed).hashBytes (no second kernel, no second facade)
    private static long oneShot(byte[] data, long seed) {
        return M3LongHashFunction.xx(seed).hashBytes(data);
    }

    private static long oneShot(byte[] data) {
        return oneShot(data, M3XxHash64Streaming.DEFAULT_SEED);
    }
}
