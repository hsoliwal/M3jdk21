/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class M3ArrayStorageNativeContractTest {
    private static final Class<?>[] TYPES = {byte.class, short.class, char.class, int.class,
            long.class, float.class, double.class, boolean.class};
    @BeforeAll static void loadLibrary() {
        String path = System.getProperty("m3.native.path");
        org.junit.jupiter.api.Assumptions.assumeTrue(path != null && !path.isBlank(),
                "host must explicitly provide the optional JNI library");
        System.load(path);
    }

    @Test void allPrimitiveBoundsMatchIndependentLinearOracleAcrossBatchSeams() throws Exception {
        Random random = new Random(70123);
        for (Class<?> type : TYPES) {
            Object[] values = new Object[301];
            for (int i = 0; i < values.length; i++) values[i] = value(type, i);
            Arrays.sort(values, (a, b) -> compare(type, a, b));
            Object sorted = Array.newInstance(type, values.length + 4);
            for (int i = 0; i < values.length; i++) Array.set(sorted, i + 2, values[i]);
            Method method = bounds(type);
            for (int pattern = 0; pattern < 5; pattern++) {
                Object queries = Array.newInstance(type, 2049);
                for (int i = 0; i < Array.getLength(queries); i++) {
                    int at = switch (pattern) {
                        case 0 -> i % values.length;
                        case 1 -> values.length - 1 - i % values.length;
                        case 2 -> 100;
                        case 3 -> random.nextInt(values.length);
                        default -> i < 263 ? i % values.length : (i % 2 == 0 ? 0 : 300);
                    };
                    Array.set(queries, i, values[at]);
                }
                for (boolean upper : new boolean[] {false, true}) {
                    for (int count : new int[] {0, 1, 7, 8, 255, 256, 257, 513, 1027}) {
                        int[] output = new int[4099]; Arrays.fill(output, -73);
                        invoke(method, sorted, 2, values.length + 2, queries, 3, count + 3, output, 11, upper);
                        for (int i = 0; i < output.length; i++) {
                            int expected = -73;
                            if (i >= 11 && i < count + 11) {
                                Object query = Array.get(queries, i - 11 + 3); int at = 0;
                                while (at < values.length) {
                                    int c = compare(type, values[at], query);
                                    if (c > 0 || (!upper && c == 0)) break;
                                    at++;
                                }
                                expected = at + 2;
                            }
                            assertEquals(expected, output[i], type + " pattern=" + pattern + " at=" + i);
                        }
                    }
                }
            }
            // Input/query identity is valid; no second acquisition is required.
            int[] output = new int[values.length];
            invoke(method, sorted, 2, values.length + 2, sorted, 2, values.length + 2, output, 0, false);
            for (int i = 0; i < output.length; i++) {
                int at = i; while (at > 0 && compare(type, values[at - 1], values[i]) == 0) at--;
                assertEquals(at + 2, output[i]);
            }
        }
    }

    @Test void nativeExactProjectionMatchesJavaOwnerAcrossPrimitiveFamilies() throws Exception {
        for (Class<?> type : TYPES) {
            Object[] values = new Object[301];
            for (int i = 0; i < values.length; i++) values[i] = value(type, i);
            Arrays.sort(values, (a, b) -> compare(type, a, b));
            Object sorted = Array.newInstance(type, values.length);
            for (int i = 0; i < values.length; i++) Array.set(sorted, i, values[i]);
            Object queries = Array.newInstance(type, 263);
            for (int i = 0; i < Array.getLength(queries); i++) {
                Array.set(queries, i, value(type, (i * 17) % 367));
            }

            Method javaScalar = exactIndex(M3PackedPrimitiveArrayOps.class, type);
            Method nativeScalar = exactIndex(M3PackedArrayNative.class, type);
            for (int i = 0; i < Array.getLength(queries); i++) {
                Object query = Array.get(queries, i);
                assertEquals(
                        call(javaScalar, sorted, 0, values.length, query),
                        call(nativeScalar, sorted, 0, values.length, query),
                        type + " scalar " + i);
            }

            Method javaBatch = exactIndexes(M3PackedPrimitiveArrayOps.class, type);
            Method nativeBatch = exactIndexes(M3PackedArrayNative.class, type);
            int[] expected = new int[277]; Arrays.fill(expected, -73);
            int[] actual = new int[277]; Arrays.fill(actual, -73);
            call(javaBatch, sorted, 0, values.length, queries, 3, 260, expected, 7);
            call(nativeBatch, sorted, 0, values.length, queries, 3, 260, actual, 7);
            assertArrayEquals(expected, actual, type + " exact batch");
        }
    }

    @Test void invalidInputsAreRejectedBeforeWritingIncludingEmptyQueries() throws Exception {
        for (Class<?> type : TYPES) {
            Method method = bounds(type);
            Object[] pair = {value(type, 0), value(type, 241)};
            Arrays.sort(pair, (a, b) -> compare(type, a, b));
            Object bad = Array.newInstance(type, 2);
            Array.set(bad, 0, pair[1]); Array.set(bad, 1, pair[0]);
            int[] output = {91, 92}; Object empty = Array.newInstance(type, 0);
            assertThrows(IllegalArgumentException.class, () -> invoke(method, bad, 0, 2, empty, 0, 0, output, 0, false));
            assertArrayEquals(new int[] {91, 92}, output);
            assertThrows(NullPointerException.class, () -> invoke(method, null, 0, 0, empty, 0, 0, output, 0, false));
            assertThrows(IndexOutOfBoundsException.class, () -> invoke(method, bad, -1, 2, empty, 0, 0, output, 0, false));
            assertThrows(IndexOutOfBoundsException.class, () -> invoke(method, bad, 0, 0, bad, 0, 2, output, 1, false));
        }
        int[] a = {1, 2};
        assertThrows(IllegalArgumentException.class, () -> M3PackedArrayNative.bounds(a, 0, 2, a, 0, 2, a, 0, false));
        assertArrayEquals(new int[] {1, 2}, a);
    }

    @Test void nativeBitmapAgreesWithWarmCountsAfterMutations() {
        var bits = new M3SegmentedBitLane28(); Random random = new Random(75);
        for (int pass = 0; pass < 1500; pass++) {
            bits.flip(random.nextInt((2 << 20) + 77)); bits.rank(1234567);
            if (pass % 17 == 0) {
                int from = random.nextInt(1 << 20); int to = from + random.nextInt(1 << 20);
                assertEquals(bits.cardinality(from, to), bits.cardinalityNative(from, to));
            }
        }
        bits.set(M3SegmentedAddress28.MAX_SLOT);
        assertEquals(bits.cardinality(), bits.cardinalityNative(0, M3SegmentedAddress28.MAX_SLOTS));
        assertEquals(1, bits.cardinalityNative(M3SegmentedAddress28.MAX_SLOT, M3SegmentedAddress28.MAX_SLOTS));
    }

    private static Method exactIndex(Class<?> owner, Class<?> type)
            throws NoSuchMethodException {
        Class<?> array = Array.newInstance(type, 0).getClass();
        return owner.getMethod("exactIndex", array, int.class, int.class, type);
    }

    private static Method exactIndexes(Class<?> owner, Class<?> type)
            throws NoSuchMethodException {
        Class<?> array = Array.newInstance(type, 0).getClass();
        return owner.getMethod("exactIndexes", array, int.class, int.class,
                array, int.class, int.class, int[].class, int.class);
    }

    private static Method bounds(Class<?> type) throws NoSuchMethodException {
        Class<?> array = Array.newInstance(type, 0).getClass();
        return M3PackedArrayNative.class.getMethod("bounds", array, int.class, int.class,
                array, int.class, int.class, int[].class, int.class, boolean.class);
    }
    private static Object call(Method method, Object... arguments) throws Exception {
        try { return method.invoke(null, arguments); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof RuntimeException runtime) throw runtime;
            if (error.getCause() instanceof Error fatal) throw fatal;
            throw error;
        }
    }

    private static void invoke(Method method, Object... arguments) throws Exception {
        call(method, arguments);
    }
    private static int compare(Class<?> type, Object a, Object b) {
        if (type == boolean.class) return Boolean.compare((Boolean) a, (Boolean) b);
        if (type == char.class) return Character.compare((Character) a, (Character) b);
        if (type == float.class) return Float.compare((Float) a, (Float) b);
        if (type == double.class) return Double.compare((Double) a, (Double) b);
        return Long.compare(((Number) a).longValue(), ((Number) b).longValue());
    }
    private static Object value(Class<?> type, int index) {
        if (type == boolean.class) return index >= 150;
        if (type == char.class) return (char) (index * 211);
        if (type == byte.class) return (byte) (index - 128);
        if (type == short.class) return (short) (index * 201 - 31000);
        if (type == int.class) return index % 5 == 0 ? Integer.MAX_VALUE : (index - 150) / 3;
        if (type == long.class) return index % 5 == 0 ? Long.MIN_VALUE : (long) (index - 150) / 3;
        if (type == float.class) return switch (index % 11) {
            case 0 -> Float.intBitsToFloat(0x7fc00001); case 1 -> Float.NEGATIVE_INFINITY;
            case 2 -> -0.0f; case 3 -> 0.0f; case 4 -> Float.POSITIVE_INFINITY;
            default -> (float) (index - 150);
        };
        return switch (index % 11) {
            case 0 -> Double.longBitsToDouble(0x7ff8000000000001L); case 1 -> Double.NEGATIVE_INFINITY;
            case 2 -> -0.0; case 3 -> 0.0; case 4 -> Double.POSITIVE_INFINITY;
            default -> (double) (index - 150);
        };
    }
}
