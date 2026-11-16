// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.SplittableRandom;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The word-at-a-time reads agree with an independent byte-loop oracle, for values and for exceptions. */
final class PackedCodecTest {
    private static long oracle(byte[] data, int offset, int bytes) {
        long raw = 0;
        for (int i = 0; i < bytes; i++) raw |= (data[offset + i] & 0xffL) << (i * Byte.SIZE);
        return raw;
    }

    private static Object outcome(LongSupplier read) {
        try {
            return read.getAsLong();
        } catch (RuntimeException failure) {
            return failure.getClass();
        }
    }

    private static void same(byte[] data, int offset, int bytes) {
        int shift = Long.SIZE - bytes * Byte.SIZE;
        String at = "offset " + offset + " bytes " + bytes;
        assertEquals(outcome(() -> oracle(data, offset, bytes)), outcome(() -> PackedCodec.readUnsigned(data, offset, bytes)), at);
        assertEquals(outcome(() -> (oracle(data, offset, bytes) << shift) >> shift),
                outcome(() -> PackedCodec.readSigned(data, offset, bytes)), at);
    }

    @Test
    void everyWidthOffsetAndTailMatchesTheByteLoop() {
        SplittableRandom random = new SplittableRandom(20261116);
        for (int length = 0; length <= 20; length++) {
            byte[] data = new byte[length];
            random.nextBytes(data);
            for (int bytes = -2; bytes <= 10; bytes++) {
                for (int offset = -3; offset <= length + 3; offset++) same(data, offset, bytes);
            }
        }
    }

    @Test
    void randomReadsMatchTheByteLoop() {
        SplittableRandom random = new SplittableRandom(7);
        byte[] data = new byte[4096];
        random.nextBytes(data);
        for (int i = 0; i < 200_000; i++) same(data, random.nextInt(-8, data.length + 8), random.nextInt(1, 9));
    }

    @Test
    void nullPayloadKeepsItsException() {
        for (int bytes = -2; bytes <= 10; bytes++) same(null, 0, bytes);
        assertThrows(NullPointerException.class, () -> PackedCodec.readSigned(null, 0, 4));
        assertEquals(0L, PackedCodec.readUnsigned(null, 0, 0));
    }

    @Test
    void writeThenReadRoundTripsEveryWidth() {
        SplittableRandom random = new SplittableRandom(3);
        byte[] data = new byte[64];
        for (int bytes = 1; bytes <= Long.BYTES; bytes++) {
            int shift = Long.SIZE - bytes * Byte.SIZE;
            for (int offset = 0; offset + bytes <= data.length; offset++) {
                long value = (random.nextLong() << shift) >> shift;
                PackedCodec.writeSigned(data, offset, bytes, value);
                assertEquals(value, PackedCodec.readSigned(data, offset, bytes));
                assertEquals(value & (-1L >>> shift), PackedCodec.readUnsigned(data, offset, bytes));
            }
        }
    }
}
