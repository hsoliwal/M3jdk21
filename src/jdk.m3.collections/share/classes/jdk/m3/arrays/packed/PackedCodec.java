// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * Encodes and decodes one validated, byte-aligned signed integer.
 *
 * <p>Reads are word-at-a-time: when a full word is available at the offset, one little-endian load replaces
 * the byte loop and the shift or mask drops the bytes above the width. The byte loop serves only the last
 * seven bytes of a payload and widths outside 1..8, so every result and every exception is unchanged.
 */
final class PackedCodec {
    private static final VarHandle WORDS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private PackedCodec() {
    }

    static long readSigned(byte[] data, int offset, int bytes) {
        int shift = Long.SIZE - bytes * Byte.SIZE;
        return ((whole(data, offset, bytes) ? (long) WORDS.get(data, offset) : tail(data, offset, bytes)) << shift) >> shift;
    }

    static long readUnsigned(byte[] data, int offset, int bytes) {
        return whole(data, offset, bytes)
                ? (long) WORDS.get(data, offset) & (-1L >>> (Long.SIZE - bytes * Byte.SIZE))
                : tail(data, offset, bytes);
    }

    static void writeSigned(byte[] data, int offset, int bytes, long value) {
        for (int i = 0; i < bytes; i++) {
            data[offset + i] = (byte) (value >>> (i * Byte.SIZE));
        }
    }

    /** True when the width is 1..8 bytes and a full word can be loaded at the offset. */
    private static boolean whole(byte[] data, int offset, int bytes) {
        return ((bytes - 1) & ~(Long.BYTES - 1)) == 0 && offset >= 0 && offset <= data.length - Long.BYTES;
    }

    private static long tail(byte[] data, int offset, int bytes) {
        long raw = 0;
        for (int i = 0; i < bytes; i++) {
            raw |= (data[offset + i] & 0xffL) << (i * Byte.SIZE);
        }
        return raw;
    }
}
