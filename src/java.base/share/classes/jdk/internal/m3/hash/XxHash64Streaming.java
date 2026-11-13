/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

package jdk.internal.m3.hash;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Streaming xxHash64 for large data. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/m3/qualification/master-landing-20261028/retained-variants/pr14848/synexia-fileindex/src/main/java/com/synexia/fileindex/content/XXHash64.java blob e7a5791a4333; recipe make/m3/qualification/hash-xxhash64-streaming-20261106/recipe/relocate.py v4
public final class XxHash64Streaming {
    /** Seed of the donor's hash(byte[]) and hashFile(Path): the one-shot is M3LongHashFunction.xx(DEFAULT_SEED). */
    public static final long DEFAULT_SEED = 0x9E3779B97F4A7C15L;
    /** Bytes of one file read: the direct buffer hashFile streams through. */
    private static final int CHUNK_SIZE = 256 * 1024;
    /** Little-endian reads of the scratch stripe: the owner's byte[] access. */
    private static final Access<byte[]> STRIPE = ByteArrayAccess.INSTANCE;

    private long v1, v2, v3, v4;
    private long totalLen;
    private final byte[] buffer = new byte[XxHash.STRIPE_BYTES];
    private int bufferSize;
    private final long seed;

    public XxHash64Streaming(long seed) {
        this.seed = seed;
        reset();
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the lane seeds are the owner's (M3XxHash.lane1..lane4), no second kernel
    public void reset() {
        v1 = XxHash.lane1(seed);
        v2 = XxHash.lane2(seed);
        v3 = XxHash.lane3(seed);
        v4 = XxHash.lane4(seed);
        totalLen = 0;
        bufferSize = 0;
    }

    public void update(ByteBuffer input) {
        int len = input.remaining();
        totalLen += len;

        if (bufferSize + len < XxHash.STRIPE_BYTES) {
            // Not enough data, just buffer it
            input.get(buffer, bufferSize, len);
            bufferSize += len;
            return;
        }

        // Process buffered data first
        if (bufferSize > 0) {
            int needed = XxHash.STRIPE_BYTES - bufferSize;
            input.get(buffer, bufferSize, needed);
            processBlock(buffer, 0);
            bufferSize = 0;
        }

        // Process full blocks
        while (input.remaining() >= XxHash.STRIPE_BYTES) {
            // Reuse the instance's scratch block. A new byte[32] here used to
            // allocate once per 32 input bytes (8,192 arrays per 256 KiB
            // file chunk), causing avoidable pressure on the young heap.
            input.get(buffer, 0, XxHash.STRIPE_BYTES);
            processBlock(buffer, 0);
        }

        // Buffer remaining
        if (input.hasRemaining()) {
            bufferSize = input.remaining();
            input.get(buffer, 0, bufferSize);
        }
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: one scratch stripe through the owner's round atom (M3XxHash.round), no second kernel
    private void processBlock(byte[] data, int offset) {
        v1 = XxHash.round(v1, STRIPE.i64(data, offset));
        v2 = XxHash.round(v2, STRIPE.i64(data, offset + XxHash.LANE_2_OFFSET));
        v3 = XxHash.round(v3, STRIPE.i64(data, offset + XxHash.LANE_3_OFFSET));
        v4 = XxHash.round(v4, STRIPE.i64(data, offset + XxHash.LANE_4_OFFSET));
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the merged lanes (or the short start), the length and the buffered tail through the owner's atoms; equals M3XxHash.xxHash64 over the same bytes
    public long getValue() {
        long h64 = totalLen >= XxHash.STRIPE_BYTES
            ? XxHash.mergeLanes(v1, v2, v3, v4)
            : XxHash.shortStart(seed);
        h64 += totalLen;
        return XxHash.tail(h64, buffer, STRIPE, 0L, bufferSize);
    }

    /**
     * Compute xxHash64 of file using streaming.
     *
     * @param path File to hash
     * @return 64-bit hash
     */
    public static long hashFile(Path path) throws IOException {
        return hashFile(path, DEFAULT_SEED);
    }

    /**
     * Compute xxHash64 of file using streaming with seed.
     *
     * @param path File to hash
     * @param seed Hash seed
     * @return 64-bit hash
     */
    public static long hashFile(Path path, long seed) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = channel.size();
            ByteBuffer buffer = ByteBuffer.allocateDirect(CHUNK_SIZE);

            XxHash64Streaming streaming = new XxHash64Streaming(seed);

            while (channel.read(buffer) > 0) {
                buffer.flip();
                streaming.update(buffer);
                buffer.clear();
            }

            long result = streaming.getValue();
            return result;
        }
    }
}
