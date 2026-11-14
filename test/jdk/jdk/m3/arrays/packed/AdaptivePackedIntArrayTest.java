// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AdaptivePackedIntArrayTest {
    @Test
    void losslessWidthsAndSignedBoundaries() {
        check(new int[0]);
        check(new int[] {42});
        check(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE});
        check(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE + 1});
        check(new int[] {-2, -2, -2, -2, -2});
        check(new int[] {5, 6, 7, 8, 9, 10, 11, 12});
        Random r = new Random(0xadd17eL);
        for (int width = 0; width <= 32; width++) {
            for (int n = 1; n <= 73; n++) {
                int[] values = new int[n];
                long mask = width == 32 ? 0xffff_ffffL : ((1L << width) - 1L);
                for (int i = 0; i < n; i++)
                    values[i] = (int) (Integer.MIN_VALUE + (r.nextLong() & mask));
                check(values);
            }
        }
    }

    private static void check(int[] values) {
        AdaptivePackedIntArray encoded = AdaptivePackedIntArray.of(values);
        assertArrayEquals(values, encoded.toIntArray());
        byte[] bytes = encoded.toByteArray();
        assertEquals((((long) values.length * encoded.bitsPerValue()) + 7L) >>> 3,
                encoded.payloadBytes());
        AdaptivePackedIntArray imported = AdaptivePackedIntArray.fromEncoded(
                values.length, encoded.base(), encoded.bitsPerValue(), bytes);
        assertArrayEquals(values, imported.toIntArray());
        if (values.length > 0)
            assertEquals(encoded.base(), java.util.Arrays.stream(values).min().orElseThrow());
        java.util.Arrays.fill(bytes, (byte) 0xff);
        assertArrayEquals(values, encoded.toIntArray());
        assertThrows(IndexOutOfBoundsException.class, () -> encoded.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> encoded.get(values.length));
    }

    @Test
    void corruptBitPlanesAreRejected() {
        assertThrows(NullPointerException.class, () -> AdaptivePackedIntArray.of(null));
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntArray.fromEncoded(1, 0, 33, new byte[5]));
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntArray.fromEncoded(-1, 0, 0, new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntArray.fromEncoded(2, 0, 2, new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntArray.fromEncoded(1, 0, 1, new byte[] {(byte) 0x80}));
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntArray.fromEncoded(1, Integer.MAX_VALUE, 1, new byte[] {1}));
    }

    @Test
    void nativeCrossCheckWhenConfigured() {
        if (System.getProperty("m3.arrays.native.path") == null) return;
        Random r = new Random(116617);
        for (int width = 0; width <= 32; width++) {
            long mask = width == 32 ? 0xffff_ffffL : (1L << width) - 1L;
            for (int n = 0; n <= 71; n++) {
                int[] values = new int[n];
                for (int i = 0; i < n; i++)
                    values[i] = (int) (Integer.MIN_VALUE + (r.nextLong() & mask));
                byte[] nativeBytes = AdaptivePackedIntNative.encodeRequired(values, Integer.MIN_VALUE, width);
                AdaptivePackedIntArray javaBytes =
                    AdaptivePackedIntArray.fromEncoded(n, Integer.MIN_VALUE, width, nativeBytes);
                assertArrayEquals(values, javaBytes.toIntArray());
                AdaptivePackedIntArray canonical = AdaptivePackedIntArray.of(values);
                if (canonical.base() == Integer.MIN_VALUE && canonical.bitsPerValue() == width)
                    assertArrayEquals(canonical.toByteArray(), nativeBytes);
                assertArrayEquals(values, AdaptivePackedIntNative.decodeRequired(nativeBytes, n,
                        Integer.MIN_VALUE, width));
            }
        }
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntNative.encodeRequired(new int[] {3}, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> AdaptivePackedIntNative.decodeRequired(new byte[] {(byte)0x80}, 1, 0, 1));
    }
}
