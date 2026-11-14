/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
/**
 * ScatterGatherIO - Vectored I/O operations.
 *
 * <p>
 * Provides efficient scatter/gather I/O for multiple buffers.
 * </p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/ScatterGatherIO.java}
 * blob {@code ee3a4e32c202654b5854de0d5f06eabfd18ef280} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ScatterGatherIO {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ScatterGatherIO.class);


    /** Default buffer count for scatter read. */
    private static final int DEFAULT_BUFFER_COUNT = 4;

    /** Default buffer size for scatter read. */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** Private constructor to prevent instantiation. */
    private ScatterGatherIO() {
        // Utility class
    }

    /**
     * Scatter-read configuration.
     *
     * @param bufferCount
     *            number of buffers
     * @param bufferSize
     *            size of each buffer
     */
    public record ScatterConfig(int bufferCount, int bufferSize) {

        /**
         * Creates a ScatterConfig with specified parameters.
         *
         * @param count
         *            buffer count
         * @param size
         *            buffer size
         * @return ScatterConfig instance
         */
        public static ScatterConfig of(int count, int size) {
            return new ScatterConfig(count, size);
        }

        /**
         * Creates a ScatterConfig with default values.
         *
         * @return default ScatterConfig
         */
        public static ScatterConfig defaults() {
            return new ScatterConfig(DEFAULT_BUFFER_COUNT, DEFAULT_BUFFER_SIZE);
        }
    }

    /**
     * Scatter-read from a file into multiple buffers.
     *
     * @param path
     *            the file path to read from
     * @param buffers
     *            the buffers to scatter read into
     * @return the total number of bytes read
     * @throws IOException
     *             if an I/O error occurs
     */
    public static long scatterRead(Path path, ByteBuffer[] buffers) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            return channel.read(buffers);
        }
    }

    /**
     * Scatter-read from a channel into multiple buffers.
     *
     * @param channel
     *            the channel to read from
     * @param buffers
     *            the buffers to scatter read into
     * @return the total number of bytes read
     * @throws IOException
     *             if an I/O error occurs
     */
    public static long scatterRead(ScatteringByteChannel channel, ByteBuffer[] buffers) throws IOException {
        return channel.read(buffers);
    }

    /**
     * Gather-write from multiple buffers to a file.
     *
     * @param path
     *            the file path to write to
     * @param buffers
     *            the buffers to gather write from
     * @return the total number of bytes written
     * @throws IOException
     *             if an I/O error occurs
     */
    public static long gatherWrite(Path path, ByteBuffer[] buffers) throws IOException {
        try (FileChannel channel = FileChannel.open(
                path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            return channel.write(buffers);
        }
    }

    /**
     * Gather-write from multiple buffers to a channel.
     *
     * @param channel
     *            the channel to write to
     * @param buffers
     *            the buffers to gather write from
     * @return the total number of bytes written
     * @throws IOException
     *             if an I/O error occurs
     */
    public static long gatherWrite(GatheringByteChannel channel, ByteBuffer[] buffers) throws IOException {
        return channel.write(buffers);
    }

    /**
     * Create scatter buffers.
     *
     * @param config
     *            the scatter configuration
     * @return array of direct byte buffers
     */
    public static ByteBuffer[] createScatterBuffers(ScatterConfig config) {
        return java.util.stream.IntStream.range(0, config.bufferCount())
                .mapToObj(i -> ByteBuffer.allocateDirect(config.bufferSize()))
                .toArray(ByteBuffer[]::new);
    }

    /**
     * Combine scattered buffers into one.
     *
     * @param buffers
     *            the buffers to combine
     * @return combined byte buffer
     */
    public static ByteBuffer combine(ByteBuffer[] buffers) {
        int total =
                java.util.Arrays.stream(buffers).mapToInt(ByteBuffer::remaining).sum();
        ByteBuffer combined = ByteBuffer.allocate(total);
        java.util.Arrays.stream(buffers).forEach(combined::put);
        combined.flip();
        return combined;
    }

    /**
     * Split a buffer into scatter buffers.
     *
     * @param source
     *            the source buffer to split
     * @param chunkSize
     *            the size of each chunk
     * @return array of byte buffers
     */
    public static ByteBuffer[] scatter(ByteBuffer source, int chunkSize) {
        int chunks = (source.remaining() + chunkSize - 1) / chunkSize;
        ByteBuffer[] buffers = new ByteBuffer[chunks];
        for (int i = 0; i < chunks; i++) { // NFL — byte-level ByteBuffer position-dependent state mutation
            int size = Math.min(chunkSize, source.remaining());
            ByteBuffer chunk = ByteBuffer.allocate(size);
            int limit = source.limit();
            source.limit(source.position() + size);
            chunk.put(source);
            source.limit(limit);
            chunk.flip();
            buffers[i] = chunk;
        }
        return buffers;
    }
}
