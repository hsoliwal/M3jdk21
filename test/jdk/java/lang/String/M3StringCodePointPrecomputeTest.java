/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Directly verify bounded M3 String code-point precompute against raw UTF-16 oracles
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCodePointPrecomputeTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

public class M3StringCodePointPrecomputeTest {
    private static final int UNAVAILABLE = Integer.MIN_VALUE;
    private static final Field STRING_M3;
    private static final Method CODE_POINT_COUNT;
    private static final Method OFFSET_BY_CODE_POINTS;
    private static final Method MAXIMUM_RETAINED_PRIMITIVE_BYTES;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);

            Class<?> m3 = Class.forName("java.lang.M3String");
            Class<?> precompute = Class.forName("java.lang.M3StringCodePointPrecompute");
            CODE_POINT_COUNT =
                    precompute.getDeclaredMethod("codePointCount", m3, int.class, int.class);
            OFFSET_BY_CODE_POINTS =
                    precompute.getDeclaredMethod(
                            "offsetByCodePoints", m3, int.class, int.class);
            MAXIMUM_RETAINED_PRIMITIVE_BYTES =
                    precompute.getDeclaredMethod("maximumRetainedPrimitiveBytes");
            CODE_POINT_COUNT.setAccessible(true);
            OFFSET_BY_CODE_POINTS.setAccessible(true);
            MAXIMUM_RETAINED_PRIMITIVE_BYTES.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministic();
        randomized();
        budgetBoundaries();
        System.out.println("M3_STRING_CODE_POINT_PRECOMPUTE_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        String source = String.join(
                "",
                "a".repeat(63),
                "\ud83d",
                "\ude42",
                "b".repeat(63),
                "\ud83d",
                "x",
                "\ude42",
                "\u0000",
                "\ud800",
                "\udc00",
                "z".repeat(70));
        char[] oracle = chars(source);
        Object m3 = body(source);

        int[] boundaries = {
                0, 1, 62, 63, 64, 65, 66, 126, 127, 128,
                129, 130, oracle.length - 2, oracle.length - 1, oracle.length
        };
        for (int begin : boundaries) {
            if (begin < 0 || begin > oracle.length) continue;
            for (int end : boundaries) {
                if (end < begin || end > oracle.length) continue;
                int actual = directCount(m3, begin, end);
                int expected = naiveCodePointCount(oracle, begin, end);
                check(actual == expected,
                        "count " + begin + ":" + end + " actual=" + actual + " expected=" + expected);
                check(source.codePointCount(begin, end) == expected,
                        "public count " + begin + ":" + end);
            }
        }

        for (int index : boundaries) {
            if (index < 0 || index > oracle.length) continue;
            for (int offset = -8; offset <= 8; offset++) {
                try {
                    int expected = naiveOffsetByCodePoints(oracle, index, offset);
                    int actual = directOffset(m3, index, offset);
                    check(actual == expected,
                            "offset index=" + index + " cp=" + offset
                                    + " actual=" + actual + " expected=" + expected);
                    check(source.offsetByCodePoints(index, offset) == expected,
                            "public offset index=" + index + " cp=" + offset);
                } catch (IndexOutOfBoundsException expectedFailure) {
                    int actual = directOffset(m3, index, offset);
                    check(actual == UNAVAILABLE,
                            "direct insufficient offset must fail open index=" + index + " cp=" + offset);
                    int failingIndex = index;
                    int failingOffset = offset;
                    expectIndexOutOfBounds(
                            () -> source.offsetByCodePoints(failingIndex, failingOffset),
                            "public insufficient offset index=" + index + " cp=" + offset);
                }
            }
        }
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33435046414354L);
        char[] alphabet = {
                0, 'a', 'b', 'Z', '\u00ff', '\u0100',
                '\ud800', '\ud83d', '\udc00', '\ude42', '\uffff'
        };

        for (int trial = 0; trial < 5_000; trial++) {
            int length = 128 + random.nextInt(896);
            char[] value = new char[length];
            for (int index = 0; index < value.length; index++) {
                value[index] = alphabet[random.nextInt(alphabet.length)];
            }

            int seam = 1 + random.nextInt(length - 1);
            String source = String.join(
                    "",
                    new String(value, 0, seam),
                    new String(value, seam, length - seam));
            Object m3 = body(source);

            int begin = random.nextInt(length + 1);
            int end = begin + random.nextInt(length - begin + 1);
            int expectedCount = naiveCodePointCount(value, begin, end);
            check(directCount(m3, begin, end) == expectedCount,
                    "random count trial=" + trial);
            check(source.codePointCount(begin, end) == expectedCount,
                    "random public count trial=" + trial);

            int index = random.nextInt(length + 1);
            int offset = random.nextInt(33) - 16;
            try {
                int expected = naiveOffsetByCodePoints(value, index, offset);
                check(directOffset(m3, index, offset) == expected,
                        "random offset trial=" + trial);
                check(source.offsetByCodePoints(index, offset) == expected,
                        "random public offset trial=" + trial);
            } catch (IndexOutOfBoundsException expectedFailure) {
                check(directOffset(m3, index, offset) == UNAVAILABLE,
                        "random insufficient direct offset trial=" + trial);
                expectIndexOutOfBounds(
                        () -> source.offsetByCodePoints(index, offset),
                        "random insufficient public offset trial=" + trial);
            }
        }
    }

    private static void budgetBoundaries() throws Exception {
        String shortSource = new String(new char[127]);
        check(directCount(body(shortSource), 0, shortSource.length()) == UNAVAILABLE,
                "short source bypass");

        String minimum = new String(new char[128]);
        check(directCount(body(minimum), 0, minimum.length()) == 128,
                "minimum source admitted");

        String maximum = String.join("", "a".repeat(16_384), "\ud83d\ude42".repeat(8_192));
        check(maximum.length() == 32_768, "maximum source length");
        check(directCount(body(maximum), 0, maximum.length()) == 24_576,
                "maximum source admitted");

        String oversized = maximum + "x";
        check(oversized.length() == 32_769, "oversized source length");
        check(directCount(body(oversized), 0, oversized.length()) == UNAVAILABLE,
                "oversized source bypass");
        check(directOffset(body(oversized), 0, 1) == UNAVAILABLE,
                "oversized offset bypass");

        long expectedBytes =
                64L * ((32_768L + 63L) >>> 6) * Long.BYTES
                        + 64L * ((((32_768L + 63L) >>> 6) + 1L) * Integer.BYTES);
        long actualBytes = (long) MAXIMUM_RETAINED_PRIMITIVE_BYTES.invoke(null);
        check(actualBytes == expectedBytes,
                "primitive retention ceiling actual=" + actualBytes + " expected=" + expectedBytes);
    }

    private static Object body(String value) throws Exception {
        value.length();
        Object m3 = STRING_M3.get(value);
        if (m3 == null) throw new AssertionError("String was not admitted to M3");
        return m3;
    }

    private static int directCount(Object m3, int begin, int end) throws Exception {
        return (int) CODE_POINT_COUNT.invoke(null, m3, begin, end);
    }

    private static int directOffset(Object m3, int index, int offset) throws Exception {
        return (int) OFFSET_BY_CODE_POINTS.invoke(null, m3, index, offset);
    }

    private static int naiveCodePointCount(char[] value, int begin, int end) {
        int count = 0;
        int index = begin;
        while (index < end) {
            char unit = value[index++];
            if (Character.isHighSurrogate(unit)
                    && index < end
                    && Character.isLowSurrogate(value[index])) {
                index++;
            }
            count++;
        }
        return count;
    }

    private static int naiveOffsetByCodePoints(char[] value, int index, int offset) {
        if (index < 0 || index > value.length) throw new IndexOutOfBoundsException();
        int cursor = index;
        if (offset >= 0) {
            for (int remaining = offset; remaining > 0; remaining--) {
                if (cursor >= value.length) throw new IndexOutOfBoundsException();
                char unit = value[cursor++];
                if (Character.isHighSurrogate(unit)
                        && cursor < value.length
                        && Character.isLowSurrogate(value[cursor])) {
                    cursor++;
                }
            }
            return cursor;
        }

        for (long remaining = -(long) offset; remaining > 0; remaining--) {
            if (cursor <= 0) throw new IndexOutOfBoundsException();
            char unit = value[--cursor];
            if (Character.isLowSurrogate(unit)
                    && cursor > 0
                    && Character.isHighSurrogate(value[cursor - 1])) {
                cursor--;
            }
        }
        return cursor;
    }

    private static char[] chars(String value) {
        char[] result = new char[value.length()];
        value.getChars(0, value.length(), result, 0);
        return result;
    }

    private static void expectIndexOutOfBounds(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (IndexOutOfBoundsException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
