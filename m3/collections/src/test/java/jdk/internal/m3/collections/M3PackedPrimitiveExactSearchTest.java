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
import org.junit.jupiter.api.Test;

final class M3PackedPrimitiveExactSearchTest {
    private static final Class<?>[] TYPES = {byte.class, short.class, char.class, int.class,
            long.class, float.class, double.class, boolean.class};

    @Test void scalarAndBatchExactIndexesMatchLinearOracleForEveryPrimitiveFamily()
            throws Exception {
        for (Class<?> type : TYPES) {
            Object[] values = new Object[301];
            for (int i = 0; i < values.length; i++) values[i] = value(type, i);
            Arrays.sort(values, (a, b) -> compare(type, a, b));
            Object sorted = Array.newInstance(type, values.length + 4);
            for (int i = 0; i < values.length; i++) Array.set(sorted, i + 2, values[i]);

            Object queries = Array.newInstance(type, 263);
            for (int i = 0; i < Array.getLength(queries); i++) {
                Array.set(queries, i, value(type, (i * 17) % 367));
            }

            Method scalar = exactIndex(type);
            for (int i = 0; i < Array.getLength(queries); i++) {
                Object query = Array.get(queries, i);
                assertEquals(oracle(type, sorted, 2, values.length + 2, query),
                        call(scalar, sorted, 2, values.length + 2, query),
                        type + " scalar " + i);
            }

            Method batch = exactIndexes(type);
            int[] output = new int[277]; Arrays.fill(output, -73);
            call(batch, sorted, 2, values.length + 2, queries, 3, 260, output, 7);
            int[] expected = new int[277]; Arrays.fill(expected, -73);
            for (int i = 3; i < 260; i++) {
                expected[7 + i - 3] = oracle(
                        type, sorted, 2, values.length + 2, Array.get(queries, i));
            }
            assertArrayEquals(expected, output, type + " batch");
        }
    }

    @Test void batchPreservesExistingValidationAndNoWriteOnDescendingInput() {
        int[] sorted = {2, 1};
        int[] queries = {1};
        int[] result = {91};
        assertThrows(IllegalArgumentException.class,
                () -> M3PackedPrimitiveArrayOps.exactIndexes(
                        sorted, 0, sorted.length, queries, 0, 1, result, 0));
        assertArrayEquals(new int[] {91}, result);
    }

    private static int oracle(Class<?> type, Object sorted, int from, int to, Object query) {
        for (int i = from; i < to; i++) {
            int order = compare(type, Array.get(sorted, i), query);
            if (order == 0) return i;
            if (order > 0) break;
        }
        return -1;
    }

    private static Method exactIndex(Class<?> type) throws NoSuchMethodException {
        Class<?> array = Array.newInstance(type, 0).getClass();
        return M3PackedPrimitiveArrayOps.class.getMethod(
                "exactIndex", array, int.class, int.class, type);
    }

    private static Method exactIndexes(Class<?> type) throws NoSuchMethodException {
        Class<?> array = Array.newInstance(type, 0).getClass();
        return M3PackedPrimitiveArrayOps.class.getMethod(
                "exactIndexes", array, int.class, int.class,
                array, int.class, int.class, int[].class, int.class);
    }

    private static Object call(Method method, Object... arguments) throws Exception {
        try { return method.invoke(null, arguments); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof RuntimeException runtime) throw runtime;
            if (error.getCause() instanceof Error fatal) throw fatal;
            throw error;
        }
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
