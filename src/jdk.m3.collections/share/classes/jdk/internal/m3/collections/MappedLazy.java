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
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.function.Function;
/**
 * MappedLazy - Memory-mapped file lazy loading.
 *
 * <p>
 * Provides lazy access to file data via memory-mapped buffers.
 * </p>
 *
 * @param <T>
 *            value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/MappedLazy.java}
 * blob {@code cc212ce4d8cde17a3b3fa82ee700e4e7e2cd75a5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MappedLazy<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(MappedLazy.class);


    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();
    private static final String ERR_MAP_FAILED = INTERNER.intern("Failed to map file: ");

    private final Path path;
    private final Function<MappedByteBuffer, T> decoder;
    private volatile MappedByteBuffer buffer;
    private volatile T value;
    private volatile boolean loaded;

    private MappedLazy(Path path, Function<MappedByteBuffer, T> decoder) {
        this.path = path;
        this.decoder = decoder;
    }

    /**
     * Region specification for partial mapping.
     *
     * @param offset
     *            byte offset in file
     * @param size
     *            number of bytes to map (-1 for full file)
     */
    public record Region(long offset, long size) {

        /**
         * Creates a region representing the full file.
         *
         * @return full file Region
         */
        public static Region full() {
            return new Region(0, -1);
        }

        /**
         * Creates a region with specified offset and size.
         *
         * @param offset
         *            byte offset
         * @param size
         *            number of bytes
         * @return Region instance
         */
        public static Region of(long offset, long size) {
            return new Region(offset, size);
        }
    }

    /**
     * Creates a MappedLazy with custom decoder.
     *
     * @param <T>
     *            value type
     * @param path
     *            file path
     * @param decoder
     *            function to decode buffer to value
     * @return MappedLazy instance
     */
    public static <T> MappedLazy<T> of(Path path, Function<MappedByteBuffer, T> decoder) {
        return new MappedLazy<>(path, decoder);
    }

    /**
     * Creates a MappedLazy that returns the raw buffer.
     *
     * @param path
     *            file path
     * @return MappedLazy instance
     */
    public static MappedLazy<MappedByteBuffer> ofBuffer(Path path) {
        return new MappedLazy<>(path, Function.identity());
    }

    /**
     * Gets the decoded value, loading lazily if needed.
     *
     * @return decoded value
     */
    public T get() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    load();
                }
            }
        }
        return value;
    }

    /**
     * Gets the raw mapped buffer, mapping lazily if needed.
     *
     * @return MappedByteBuffer
     */
    public MappedByteBuffer getBuffer() {
        if (buffer == null) {
            synchronized (this) {
                if (buffer == null) {
                    mapFile();
                }
            }
        }
        return buffer;
    }

    private void load() {
        mapFile();
        value = decoder.apply(buffer);
        loaded = true;
    }

    private void mapFile() {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
                FileChannel channel = raf.getChannel()) {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (IOException e) {
            throw new UncheckedIOException(ERR_MAP_FAILED + path, e);
        }
    }

    /**
     * Checks if the value has been loaded.
     *
     * @return true if loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Gets the file path.
     *
     * @return file path
     */
    public Path getPath() {
        return path;
    }
}
