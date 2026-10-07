/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Directly verify bounded M3 String UTF-16 position precompute against raw char[] oracles
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPositionPrecomputeTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public class M3StringPositionPrecomputeTest {
    private static final Field STRING_M3;
    private static final Method INDEX_OF;
    private static final Method INDEX_OF_EITHER;
    private static final Method LAST_INDEX_OF;
    private static final Method MAXIMUM_RETAINED_PRIMITIVE_BYTES;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);

            Class<?> m3 = Class.forName("java.lang.M3String");
            Class<?> precompute = Class.forName("java.lang.M3StringPositionPrecompute");
            INDEX_OF =
                    precompute.getDeclaredMethod(
                            "indexOf", m3, char.class, int.class, int.class);
            INDEX_OF_EITHER =
                    precompute.getDeclaredMethod(
                            "indexOfEither",
                            m3,
                            char.class,
                            char.class,
                            int.class,
                            int.class);
            LAST_INDEX_OF =
                    precompute.getDeclaredMethod(
                            "lastIndexOf", m3, char.class, int.class);
            MAXIMUM_RETAINED_PRIMITIVE_BYTES =
                    precompute.getDeclaredMethod("maximumRetainedPrimitiveBytes");
            INDEX_OF.setAccessible(true);
            INDEX_OF_EITHER.setAccessible(true);
            LAST_INDEX_OF.setAccessible(true);
            MAXIMUM_RETAINED_PRIMITIVE_BYTES.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministic();
        randomized();
        budgetBoundaries();
        System.out.println("M3_STRING_POSITION_PRECOMPUTE_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        char[] value = new char[768];
        Arrays.fill(value, 'm');
        for (int position : new int[] {
                0, 1, 62, 63, 64, 65, 126, 127, 128, 129,
                255, 256, 257, 510, 511, 512, 513, 766, 767
        }) {
            value[position] = 'X';
        }
        for (int position : new int[] {31, 95, 159, 223, 287, 351, 415, 479, 543, 607, 671, 735}) {
            value[position] = '\n';
        }
        for (int position : new int[] {32, 96, 160, 224, 288, 352, 416, 480, 544, 608, 672, 736}) {
            value[position] = '\r';
        }

        String source = String.join(
                "",
                new String(value, 0, 255),
                new String(value, 255, 257),
                new String(value, 512, value.length - 512));
        Object m3 = body(source);

        int[] froms = {-7, 0, 1, 62, 63, 64, 65, 127, 128, 255, 256, 511, 512, 767, 768, 900};
        int[] ends = {0, 1, 64, 65, 128, 256, 512, 767, 768, 900};
        for (char unit : new char[] {'X', 'm', '\n', '\r', '\u0101', '\uffff'}) {
            for (int from : froms) {
                for (int end : ends) {
                    int expected = naiveIndexOf(value, unit, from, end);
                    int actual = directIndexOf(m3, unit, from, end);
                    check(actual == expected,
                            "indexOf unit=" + (int) unit + " from=" + from + " end=" + end);
                }
                int expectedLast = naiveLastIndexOf(value, unit, from);
                int actualLast = directLastIndexOf(m3, unit, from);
                check(actualLast == expectedLast,
                        "lastIndexOf unit=" + (int) unit + " from=" + from);
            }
        }

        for (char[] pair : new char[][] {
                {'\r', '\n'},
                {'X', 'm'},
                {'\u0101', '\uffff'},
                {'X', 'X'}
        }) {
            for (int from : froms) {
                for (int end : ends) {
                    int expected = naiveIndexOfEither(value, pair[0], pair[1], from, end);
                    int actual = directIndexOfEither(m3, pair[0], pair[1], from, end);
                    check(actual == expected,
                            "indexOfEither first=" + (int) pair[0]
                                    + " second=" + (int) pair[1]
                                    + " from=" + from
                                    + " end=" + end);
                }
            }
        }
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33504f53464143L);
        char[] alphabet = {
                0, 1, 'a', 'b', 'X', '\n', '\r',
                '\u00ff', '\u0100', '\u0101', '\ud800', '\ude42', '\uffff'
        };

        for (int trial = 0; trial < 5_000; trial++) {
            int length = 256 + random.nextInt(1_792);
            char[] value = new char[length];
            for (int index = 0; index < length; index++) {
                value[index] = alphabet[random.nextInt(alphabet.length)];
            }

            int firstCut = 1 + random.nextInt(length - 1);
            int secondCut =
                    firstCut + random.nextInt(length - firstCut + 1);
            String source = String.join(
                    "",
                    new String(value, 0, firstCut),
                    new String(value, firstCut, secondCut - firstCut),
                    new String(value, secondCut, length - secondCut));
            Object m3 = body(source);

            char first = alphabet[random.nextInt(alphabet.length)];
            char second = alphabet[random.nextInt(alphabet.length)];
            int from = random.nextInt(length + 65) - 32;
            int end = random.nextInt(length + 65);

            check(directIndexOf(m3, first, from, end)
                            == naiveIndexOf(value, first, from, end),
                    "random index trial=" + trial);
            check(directLastIndexOf(m3, first, from)
                            == naiveLastIndexOf(value, first, from),
                    "random last trial=" + trial);
            check(directIndexOfEither(m3, first, second, from, end)
                            == naiveIndexOfEither(value, first, second, from, end),
                    "random either trial=" + trial);
        }
    }

    private static void budgetBoundaries() throws Exception {
        String shortSource = new String(repeat('x', 255));
        Object shortM3 = body(shortSource);
        check(directIndexOf(shortM3, 'x', 0, shortSource.length()) == 0,
                "short source linear fallback");
        check(directLastIndexOf(shortM3, 'x', shortSource.length()) == 254,
                "short source reverse fallback");

        String minimum = new String(repeat('y', 256));
        check(directIndexOf(body(minimum), 'y', 0, minimum.length()) == 0,
                "minimum cache source admitted");

        char[] max = repeat('z', 32_768);
        max[32_767] = 'Q';
        String maximum = String.join(
                "",
                new String(max, 0, 16_384),
                new String(max, 16_384, 16_384));
        check(directIndexOf(body(maximum), 'Q', 0, maximum.length()) == 32_767,
                "maximum cache source admitted");

        char[] over = repeat('z', 32_769);
        over[32_768] = 'Q';
        String oversized = new String(over);
        check(directIndexOf(body(oversized), 'Q', 0, oversized.length()) == 32_768,
                "oversized source linear fallback");

        long blocksPerEntry = (32_768L + 63L) >>> 6;
        long signalBytes = 64L * blocksPerEntry * Long.BYTES;
        long exactBytes = 64L * 32_768L * (Character.BYTES + Long.BYTES);
        long expected = Math.addExact(signalBytes, exactBytes);
        long actual = (long) MAXIMUM_RETAINED_PRIMITIVE_BYTES.invoke(null);
        check(actual == expected,
                "primitive retention ceiling actual=" + actual + " expected=" + expected);
    }

    private static Object body(String value) throws Exception {
        value.length();
        Object m3 = STRING_M3.get(value);
        if (m3 == null) throw new AssertionError("String was not admitted to M3");
        return m3;
    }

    private static int directIndexOf(Object m3, char unit, int from, int end) throws Exception {
        return (int) INDEX_OF.invoke(null, m3, unit, from, end);
    }

    private static int directIndexOfEither(
            Object m3, char first, char second, int from, int end) throws Exception {
        return (int) INDEX_OF_EITHER.invoke(null, m3, first, second, from, end);
    }

    private static int directLastIndexOf(Object m3, char unit, int from) throws Exception {
        return (int) LAST_INDEX_OF.invoke(null, m3, unit, from);
    }

    private static int naiveIndexOf(char[] value, char unit, int from, int end) {
        int start = Math.max(0, from);
        int limit = Math.min(value.length, end);
        for (int index = start; index < limit; index++) {
            if (value[index] == unit) return index;
        }
        return -1;
    }

    private static int naiveIndexOfEither(
            char[] value, char first, char second, int from, int end) {
        int start = Math.max(0, from);
        int limit = Math.min(value.length, end);
        for (int index = start; index < limit; index++) {
            char unit = value[index];
            if (unit == first || unit == second) return index;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] value, char unit, int from) {
        for (int index = Math.min(from, value.length - 1); index >= 0; index--) {
            if (value[index] == unit) return index;
        }
        return -1;
    }

    private static char[] repeat(char unit, int length) {
        char[] result = new char[length];
        Arrays.fill(result, unit);
        return result;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
