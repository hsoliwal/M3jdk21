// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.m3.arrays.packed.Int40Array;
import com.m3.arrays.packed.Int48Array;
import com.m3.arrays.packed.Int56Array;
import com.m3.arrays.packed.PackedIntArray;
import com.m3.arrays.packed.VInt;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Target-authoritative checked-JNI parity and independent ingress/failure contracts. */
final class M3PackedNativeTest {
    @Test
    void publicValidationPrecedesNativeLoading() {
        assertThrows(NullPointerException.class, () -> M3ArrayNative.encodePackedRequired(null, 1));
        assertThrows(NullPointerException.class, () -> M3ArrayNative.decodePackedRequired(null, 1));
        assertThrows(NullPointerException.class, () -> M3ArrayNative.encodeUnsigned64Required(null));
        assertThrows(NullPointerException.class, () -> M3ArrayNative.decodeUnsigned64Required(null));
        assertThrows(NullPointerException.class, () -> M3ArrayNative.encodeZigZag64Required(null));
        assertThrows(NullPointerException.class, () -> M3ArrayNative.decodeZigZag64Required(null));
        for (int bits : new int[] {0, 8, 32, 39, 41, 64, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodePackedRequired(new long[0], bits));
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodePackedRequired(new byte[0], bits));
        }
        for (int bits : new int[] {40, 48, 56}) {
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodePackedRequired(new byte[bits / 8 - 1], bits));
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodePackedRequired(new long[4096 / (bits / 8) + 1], bits));
        }
        assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodeUnsigned64Required(new long[4097]));
        assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodeUnsigned64Required(new byte[4097]));
        assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodeZigZag64Required(new long[4097]));
        assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodeZigZag64Required(new byte[4097]));
    }

    @Test
    void everyPackedWidthMatchesIndependentPortableVectors() {
        requireNative();
        Random random = new Random(0x4d335041434bL);
        for (int bits : new int[] {40, 48, 56}) {
            int limit = 4096 / (bits / 8);
            for (int length : new int[] {0, 1, 2, 7, 63, 64, 127, 128, limit}) {
                long[] values = new long[length];
                for (int i = 0; i < length; i++) values[i] = random.nextLong() >> (64 - bits);
                if (length > 0) values[0] = -(1L << (bits - 1));
                if (length > 1) values[length - 1] = (1L << (bits - 1)) - 1;
                long[] before = values.clone();
                PackedIntArray packed = array(bits, length);
                packed.setAll(0, values, 0, length);
                byte[] bytes = M3ArrayNative.encodePackedRequired(values, bits);
                assertArrayEquals(packed.toByteArray(), bytes);
                assertArrayEquals(values, M3ArrayNative.decodePackedRequired(bytes, bits));
                assertArrayEquals(before, values);
                assertNotSame(values, M3ArrayNative.decodePackedRequired(bytes, bits));
            }
            long minimum = -(1L << (bits - 1));
            long maximum = (1L << (bits - 1)) - 1;
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodePackedRequired(new long[] {0, minimum - 1}, bits));
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodePackedRequired(new long[] {0, maximum + 1}, bits));
        }
    }

    @Test
    void fullWidthVintMatchesPortableForBoundariesAndSeededInputs() {
        requireNative();
        long[] boundary = {0, -1, 1, -2, 127, 128, 16383, 16384, Long.MIN_VALUE, Long.MAX_VALUE};
        assertVints(boundary);
        Random random = new Random(0x56494e543634L);
        for (int trial = 0; trial < 100; trial++) {
            long[] values = new long[trial * 4];
            for (int i = 0; i < values.length; i++) values[i] = random.nextLong();
            assertVints(values);
        }
        assertVints(new long[0]);
        assertVints(new long[4096]);
        long[] negative = new long[410];
        Arrays.fill(negative, -1);
        assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodeUnsigned64Required(negative));
        Arrays.fill(negative, Long.MIN_VALUE);
        assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.encodeZigZag64Required(negative));
    }

    @Test
    void malformedVintsRejectWithoutChangingInputs() {
        requireNative();
        byte[][] invalid = {
            {(byte) 0x80}, {(byte) 0x80, 0}, {0, (byte) 0x80},
            {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
             (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 2},
            {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
             (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0}
        };
        for (byte[] bytes : invalid) {
            byte[] before = bytes.clone();
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodeUnsigned64Required(bytes));
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodeZigZag64Required(bytes));
            assertArrayEquals(before, bytes);
        }
        for (int last = 0; last < 256; last++) {
            if (last == 1) continue;
            byte[] bytes = new byte[10];
            Arrays.fill(bytes, (byte) 0x80);
            bytes[9] = (byte) last;
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodeUnsigned64Required(bytes));
            assertThrows(IllegalArgumentException.class, () -> M3ArrayNative.decodeZigZag64Required(bytes));
        }
    }

    @Test
    void directJniIngressDoesNotRelyOnJavaValidation() throws ReflectiveOperationException {
        requireNative();
        for (String name : new String[] {"nativeEncodeUnsigned64", "nativeEncodeZigZag64"}) {
            Method method = M3ArrayNative.class.getDeclaredMethod(name, long[].class);
            method.setAccessible(true);
            failure(method, NullPointerException.class, (Object) null);
            failure(method, IllegalArgumentException.class, new long[4097]);
        }
        for (String name : new String[] {"nativeDecodeUnsigned64", "nativeDecodeZigZag64"}) {
            Method method = M3ArrayNative.class.getDeclaredMethod(name, byte[].class);
            method.setAccessible(true);
            failure(method, NullPointerException.class, (Object) null);
            failure(method, IllegalArgumentException.class, new byte[4097]);
            failure(method, IllegalArgumentException.class, new byte[] {(byte) 0x80, 0});
        }
        Method encode = M3ArrayNative.class.getDeclaredMethod("nativeEncodePacked", long[].class, int.class);
        Method decode = M3ArrayNative.class.getDeclaredMethod("nativeDecodePacked", byte[].class, int.class);
        encode.setAccessible(true);
        decode.setAccessible(true);
        failure(encode, NullPointerException.class, null, 40);
        failure(decode, NullPointerException.class, null, 40);
        failure(encode, IllegalArgumentException.class, new long[0], 64);
        failure(decode, IllegalArgumentException.class, new byte[0], 64);
        failure(encode, IllegalArgumentException.class, new long[820], 40);
        failure(decode, IllegalArgumentException.class, new byte[6], 40);
    }

    @Test
    void realJvmGuardsEverySilentNullResult() throws Exception {
        requireNative();
        Path fixture = Path.of(System.getProperty("m3.arrays.native.path")).toAbsolutePath()
                .getParent().resolve(System.mapLibraryName("m3_packed_null_fixture"));
        assertTrue(Files.isRegularFile(fixture), "CMake testing must build the fault provider");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xcheck:jni", "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                "com.m3.arrays.acceptance.PackedNullResultProbe", fixture.toString())
                .redirectErrorStream(true).start();
        try {
            assertTrue(child.waitFor(30, TimeUnit.SECONDS), "bounded fresh-JVM fault probe");
            String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, child.exitValue(), output);
            assertEquals("M3_PACKED_NULL_RESULT_PASS cases=6", output.strip());
        } finally {
            if (child.isAlive()) child.destroyForcibly();
        }
    }

    private static void assertVints(long[] values) {
        long[] before = values.clone();
        byte[] unsigned = M3ArrayNative.encodeUnsigned64Required(values);
        byte[] zigzag = M3ArrayNative.encodeZigZag64Required(values);
        assertArrayEquals(VInt.encodeUnsigned64(values), unsigned);
        assertArrayEquals(VInt.encodeZigZag64(values), zigzag);
        assertArrayEquals(values, M3ArrayNative.decodeUnsigned64Required(unsigned));
        assertArrayEquals(values, M3ArrayNative.decodeZigZag64Required(zigzag));
        assertArrayEquals(before, values);
    }

    private static PackedIntArray array(int bits, int length) {
        return switch (bits) {
            case 40 -> new Int40Array(length);
            case 48 -> new Int48Array(length);
            case 56 -> new Int56Array(length);
            default -> throw new AssertionError(bits);
        };
    }

    private static void failure(Method method, Class<? extends Throwable> expected, Object... arguments) {
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, arguments));
        assertInstanceOf(expected, failure.getCause());
    }

    private static void requireNative() {
        String configured = System.getProperty("m3.arrays.native.path");
        assumeTrue(configured != null && !configured.isBlank(), "native-linux profile is the required JNI lane");
        M3ArrayNative.load(Path.of(configured));
        assertTrue(M3ArrayNative.isLoaded());
    }
}
