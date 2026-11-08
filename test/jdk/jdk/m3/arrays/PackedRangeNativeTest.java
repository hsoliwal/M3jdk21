// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Real JNI parity, including payloads larger than the older materialization bound. */
final class PackedRangeNativeTest {
    @BeforeAll static void load() {
        String library = System.getProperty("m3.arrays.native.path");
        assumeTrue(library != null && !library.isBlank(), "native library required for the JNI lane");
        ArrayNative.load(Path.of(library));
    }

    @Test void literalSignedLittleEndianVectorsAreIndependentOfEncoders() {
        byte[] bytes = HexFormat.of().parseHex("ffffffffff0000000080ffffffff7f0000000000");
        assertEquals(2, ArrayNative.countPackedInRangeRequired(bytes, 40, 0, 4, Long.MIN_VALUE, -1));
        assertEquals(0, ArrayNative.firstPackedInRangeRequired(bytes, 40, 0, 4, Long.MIN_VALUE, -1));
        assertEquals(1, ArrayNative.firstPackedInRangeRequired(bytes, 40, 1, 4, Long.MIN_VALUE, -1));
        assertEquals(2, ArrayNative.firstPackedInRangeRequired(bytes, 40, 0, 4, 1, Long.MAX_VALUE));
        assertEquals(-1, ArrayNative.firstPackedInRangeRequired(bytes, 40, 2, 4, Long.MIN_VALUE, -1));
    }

    @Test void alignedChunksAndNonzeroWindowsMatchSourceValueOracle() {
        Random random = new Random(0x4a4e4952414e4745L);
        for (int bits : new int[] {40, 48, 56}) {
            int chunk = 4096 / (bits / 8);
            int length = chunk * 3 + 17;
            long[] values = new long[length];
            for (int i = 0; i < length; i++) values[i] = random.nextLong() >> (64 - bits);
            values[chunk - 1] = -17; values[chunk] = 17; values[chunk + 1] = -17;
            values[length - 1] = -(1L << (bits - 1));
            byte[] bytes = encode(values, bits);
            byte[] before = bytes.clone();
            for (int trial = 0; trial < 200; trial++) {
                int from = trial == 0 ? 0 : random.nextInt(length + 1);
                int to = trial == 0 ? length : from + random.nextInt(length - from + 1);
                long a = random.nextLong() >> (64 - bits), b = random.nextLong() >> (64 - bits);
                compare(bytes, bits, values, from, to, Math.min(a, b), Math.max(a, b));
            }
            for (int from : new int[] {0, 1, chunk - 1, chunk, chunk + 1, length}) {
                compare(bytes, bits, values, from, length, -17, -17);
                compare(bytes, bits, values, from, length, Long.MIN_VALUE, Long.MAX_VALUE);
                compare(bytes, bits, values, from, length, Long.MAX_VALUE, Long.MAX_VALUE);
            }
            assertArrayEquals(before, bytes);
            assertEquals(0, ArrayNative.countPackedInRangeRequired(new byte[0], bits, 0, 0, 0, 0));
            assertEquals(-1, ArrayNative.firstPackedInRangeRequired(new byte[0], bits, 0, 0, 0, 0));
        }
    }

    @Test void publicAndDirectNativeBoundariesValidateWholeGeometryAndWindow() throws Exception {
        assertThrows(NullPointerException.class, () -> ArrayNative.countPackedInRangeRequired(null, 0, -1, -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> ArrayNative.countPackedInRangeRequired(new byte[0], 32, -1, -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> ArrayNative.firstPackedInRangeRequired(new byte[6], 40, 0, 0, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayNative.countPackedInRangeRequired(new byte[5], 40, -1, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> ArrayNative.firstPackedInRangeRequired(new byte[5], 40, 1, 1, 1, 0));
        Method method = ArrayNative.class.getDeclaredMethod("nativeQueryPacked", byte[].class,
                int.class, int.class, int.class, long.class, long.class, boolean.class);
        method.setAccessible(true);
        failure(method, NullPointerException.class, null, 40, 0, 0, 0L, 0L, false);
        failure(method, IllegalArgumentException.class, new byte[0], 64, 0, 0, 0L, 0L, false);
        failure(method, IllegalArgumentException.class, new byte[6], 40, 0, 0, 0L, 0L, true);
        failure(method, IndexOutOfBoundsException.class, new byte[5], 40, 0, 2, 0L, 0L, false);
        failure(method, IndexOutOfBoundsException.class, new byte[5], 40, 1, 0, 0L, 0L, false);
        failure(method, IndexOutOfBoundsException.class, new byte[5], 40, Integer.MIN_VALUE, Integer.MAX_VALUE, 0L, 0L, true);
        failure(method, IllegalArgumentException.class, new byte[0], 40, 0, 0, 1L, 0L, true);
    }

    private static void compare(byte[] bytes, int bits, long[] values, int from, int to, long low, long high) {
        int count = (int) Arrays.stream(values, from, to).filter(v -> v >= low && v <= high).count();
        int first = -1;
        for (int i = from; i < to; i++) if (values[i] >= low && values[i] <= high) { first = i; break; }
        assertEquals(count, ArrayNative.countPackedInRangeRequired(bytes, bits, from, to, low, high));
        assertEquals(first, ArrayNative.firstPackedInRangeRequired(bytes, bits, from, to, low, high));
    }

    private static byte[] encode(long[] values, int bits) {
        int width = bits / 8;
        byte[] result = new byte[values.length * width];
        for (int i = 0; i < values.length; i++) for (int lane = 0; lane < width; lane++)
            result[i * width + lane] = (byte) (values[i] >>> (lane * 8));
        return result;
    }

    private static void failure(Method method, Class<? extends Throwable> type, Object... arguments) {
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, arguments));
        assertInstanceOf(type, error.getCause());
    }
}
