// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.util.Arrays;
import java.util.Objects;

/** A bounded primitive byte sink shared by raw-format encoders; no per-token objects. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3CompressionByteSink.java blob 0d1b2ff2cc0e; recipe m3/compression/recipe/compression-20261106/relocate.py v3
final class CompressionByteSink {
    private byte[] buffer;
    private static final int MAX_RETAINED_BYTES = 64 * 1024;
    private int maximum;
    private int length;

    CompressionByteSink(int maximum, int hint) {
        buffer = new byte[0];
        reset(maximum, hint);
    }

    /** Start a fresh block without retaining excess scratch from a large prior block. */
    void reset(int maximum, int hint) {
        if (maximum < 0 || hint < 0) {
            throw new IllegalArgumentException("negative compression output bound");
        }
        int initial = Math.min(maximum, Math.max(16, Math.min(hint, 8192)));
        if (buffer.length > MAX_RETAINED_BYTES || buffer.length > maximum) {
            buffer = new byte[initial];
        } else if (buffer.length < initial) {
            buffer = Arrays.copyOf(buffer, initial);
        }
        this.maximum = maximum;
        length = 0;
    }

    /** Package-private retention check for memory-pressure qualification. */
    int retainedCapacity() {
        return buffer.length;
    }

    void append(int value) {
        reserve(1);
        buffer[length++] = (byte) value;
    }

    void appendLE16(int value) {
        if (value < 0 || value > 0xffff) {
            throw new IllegalArgumentException("invalid unsigned offset");
        }
        reserve(2);
        buffer[length++] = (byte) value;
        buffer[length++] = (byte) (value >>> 8);
    }

    void append(byte[] source, int offset, int count) {
        Objects.requireNonNull(source, "source");
        if (offset < 0 || count < 0 || offset > source.length - count) {
            throw new IllegalArgumentException("invalid literal range");
        }
        reserve(count);
        System.arraycopy(source, offset, buffer, length, count);
        length += count;
    }

    byte[] finish() {
        return Arrays.copyOf(buffer, length);
    }

    private void reserve(int count) {
        if (count < 0 || count > maximum - length) {
            throw new IllegalArgumentException("compressed output exceeds bound");
        }
        int needed = length + count;
        if (needed <= buffer.length) {
            return;
        }
        long doubled = (long) buffer.length * 2;
        long proposal = Math.max(needed, Math.max(16L, doubled));
        buffer = Arrays.copyOf(buffer, (int) Math.min(maximum, proposal));
    }
}
