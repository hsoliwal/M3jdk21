// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.packed;

import java.nio.ByteOrder;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Every width, byte order and position reads the value an independent byte-loop oracle computes. */
final class PackedByteArrayTest {
    private static final ByteOrder[] ORDERS = {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN};

    private static long oracle(byte[] payload, int index, int bits, boolean signed, ByteOrder order) {
        int bytes = bits >>> 3;
        long raw = 0L;
        for (int i = 0; i < bytes; i++) {
            int source = order == ByteOrder.LITTLE_ENDIAN ? i : bytes - 1 - i;
            raw |= (payload[index * bytes + source] & 0xffL) << (i * Byte.SIZE);
        }
        int shift = Long.SIZE - bits;
        return signed ? (raw << shift) >> shift : raw;
    }

    @Test
    void everyWidthOrderAndPositionMatchesTheByteLoop() {
        SplittableRandom random = new SplittableRandom(20261116);
        for (int bits = 8; bits <= 64; bits += 8) {
            for (boolean signed : new boolean[] {false, true}) {
                for (ByteOrder order : ORDERS) {
                    for (int size = 0; size <= 20; size++) {
                        byte[] payload = new byte[size * (bits >>> 3)];
                        random.nextBytes(payload);
                        PackedByteArray packed = PackedByteArray.fromBytes(payload, bits, signed, order);
                        for (int i = 0; i < size; i++) {
                            assertEquals(oracle(payload, i, bits, signed, order), packed.get(i),
                                    bits + " bits " + order + " signed=" + signed + " index " + i + " of " + size);
                        }
                        assertThrows(IndexOutOfBoundsException.class, () -> packed.get(-1));
                        int end = size;
                        assertThrows(IndexOutOfBoundsException.class, () -> packed.get(end));
                        assertArrayEquals(payload, packed.toByteArray());
                    }
                }
            }
        }
    }

    @Test
    void setThenGetRoundTripsAndLeavesNeighboursAlone() {
        SplittableRandom random = new SplittableRandom(5);
        for (int bits = 8; bits <= 64; bits += 8) {
            for (ByteOrder order : ORDERS) {
                PackedByteArray packed = new PackedByteArray(bits, 17, true, order);
                long[] expected = new long[17];
                int shift = Long.SIZE - bits;
                for (int round = 0; round < 200; round++) {
                    int index = random.nextInt(expected.length);
                    expected[index] = (random.nextLong() << shift) >> shift;
                    packed.set(index, expected[index]);
                    for (int i = 0; i < expected.length; i++) assertEquals(expected[i], packed.get(i), bits + " bits " + order);
                }
            }
        }
    }

    @Test
    void malformedInputsKeepTheirExceptions() {
        assertThrows(IllegalArgumentException.class, () -> new PackedByteArray(12, 1, false, ByteOrder.LITTLE_ENDIAN));
        assertThrows(IllegalArgumentException.class, () -> new PackedByteArray(16, -1, false, ByteOrder.LITTLE_ENDIAN));
        assertThrows(NullPointerException.class, () -> new PackedByteArray(16, 1, false, null));
        assertThrows(IllegalArgumentException.class, () -> PackedByteArray.fromBytes(new byte[3], 16, false, ByteOrder.BIG_ENDIAN));
        assertThrows(NullPointerException.class, () -> PackedByteArray.fromBytes(null, 16, false, ByteOrder.BIG_ENDIAN));
    }
}
