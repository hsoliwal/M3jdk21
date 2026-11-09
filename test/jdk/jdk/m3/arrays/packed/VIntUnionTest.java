/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.arrays.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import java.util.concurrent.CancellationException;
import jdk.m3.arrays.WorkMonitor;
import org.junit.jupiter.api.Test;

/** PACKED_UNION U10: the Synexia common-collections VInt API on the M3 owner, consistent with its bulk codec. */
class VIntUnionTest {

    private static final class Probe implements WorkMonitor {
        long worked;
        boolean cancelAfterFirst;
        boolean canceled;

        @Override public void worked(long work) { worked += work; if (cancelAfterFirst) canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
    }

    private static long[] sample() {
        Random random = new Random(0x56494e54L);
        long[] values = new long[3000];
        for (int i = 0; i < values.length; i++) {
            values[i] = switch (i % 5) {
                case 0 -> random.nextLong();
                case 1 -> random.nextInt(128);
                case 2 -> -random.nextInt(1 << 20);
                case 3 -> i % 2 == 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
                default -> -1L;
            };
        }
        return values;
    }

    @Test
    void rangedEncodingMatchesTheBulkCodec() {
        long[] values = sample();
        assertArrayEquals(VInt.encodeUnsigned64(values), VInt.encode(values, 0, values.length, false, null));
        assertArrayEquals(VInt.encodeZigZag64(values), VInt.encode(values, 0, values.length, true, null));
        assertArrayEquals(values, VInt.decode(VInt.encodeUnsigned64(values), false, null));
        assertArrayEquals(values, VInt.decode(VInt.encodeZigZag64(values), true, null));
        assertArrayEquals(VInt.decodeZigZag64(VInt.encodeZigZag64(values)),
                VInt.decode(VInt.encode(values, 0, values.length, true, null), true, null));
    }

    @Test
    void scalarSizesWritesAndReads() {
        assertEquals(10, VInt.MAX_BYTES);
        assertEquals(1, VInt.unsignedSize(0));
        assertEquals(1, VInt.unsignedSize(127));
        assertEquals(2, VInt.unsignedSize(128));
        assertEquals(10, VInt.unsignedSize(-1));
        assertEquals(1, VInt.signedSize(-1));
        byte[] buffer = new byte[12];
        int end = VInt.writeSigned(-300, buffer, 1, 12);
        assertEquals(1 + VInt.signedSize(-300), end);
        long[] out = new long[1];
        assertEquals(end, VInt.readSigned(buffer, 1, 12, out, 0));
        assertEquals(-300, out[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> VInt.writeUnsigned(-1, new byte[9], 0, 9));
        assertThrows(IllegalArgumentException.class, () -> VInt.readUnsigned(new byte[] {(byte) 0x80}, 0, 1, out, 0));
        assertThrows(IllegalArgumentException.class, () -> VInt.readUnsigned(new byte[] {(byte) 0x80, 0}, 0, 2, out, 0));
    }

    @Test
    void monitoredCodecReportsAndCancels() {
        long[] values = sample();
        Probe probe = new Probe();
        byte[] encoded = VInt.encode(values, 0, values.length, true, probe);
        assertEquals(values.length, probe.worked);
        Probe decoding = new Probe();
        assertArrayEquals(values, VInt.decode(encoded, 0, encoded.length, true, decoding));
        assertEquals(values.length, decoding.worked);
        Probe canceling = new Probe();
        canceling.cancelAfterFirst = true;
        assertThrows(CancellationException.class, () -> VInt.encode(values, 0, values.length, false, canceling));
    }
}
