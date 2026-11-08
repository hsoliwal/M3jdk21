// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

/** Encodes and decodes one validated, byte-aligned signed integer. */
final class PackedCodec {
    private PackedCodec() {
    }

    static long readSigned(byte[] data, int offset, int bytes) {
        long raw = 0;
        for (int i = 0; i < bytes; i++) {
            raw |= (data[offset + i] & 0xffL) << (i * Byte.SIZE);
        }
        int shift = Long.SIZE - bytes * Byte.SIZE;
        return (raw << shift) >> shift;
    }

    static void writeSigned(byte[] data, int offset, int bytes, long value) {
        for (int i = 0; i < bytes; i++) {
            data[offset + i] = (byte) (value >>> (i * Byte.SIZE));
        }
    }
}
