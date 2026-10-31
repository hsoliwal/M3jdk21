// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.nio.ByteBuffer;

/**
 * Immutable logical byte-array view. Implementations may be segmented; {@link #copy()} is the
 * explicit mutable-array materialization boundary.
 */
public interface M3ByteArrayView {
    int length();

    byte byteAt(int index);

    M3ByteArrayView slice(int start, int end);

    int segmentCount();

    ByteBuffer[] asReadOnlyBuffers();

    default void copyTo(int sourceStart, byte[] target, int targetStart, int count) {
        java.util.Objects.requireNonNull(target, "target");
        java.util.Objects.checkFromIndexSize(sourceStart, count, length());
        java.util.Objects.checkFromIndexSize(targetStart, count, target.length);
        int remaining = count;
        int skip = sourceStart;
        int destination = targetStart;
        for (ByteBuffer original : asReadOnlyBuffers()) {
            ByteBuffer buffer = java.util.Objects.requireNonNull(original, "buffer").asReadOnlyBuffer();
            int available = buffer.remaining();
            if (skip >= available) {
                skip -= available;
                continue;
            }
            int take = Math.min(remaining, available - skip);
            buffer.get(buffer.position() + skip, target, destination, take);
            destination += take;
            remaining -= take;
            skip = 0;
            if (remaining == 0) return;
        }
        if (remaining != 0) throw new IllegalStateException("byte segments do not cover declared length");
    }

    default byte[] copy() {
        byte[] result = new byte[length()];
        copyTo(0, result, 0, result.length);
        return result;
    }
}
