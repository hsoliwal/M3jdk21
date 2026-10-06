/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify bounded M3String code-point precompute preserves exact String semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCodePointPrecomputeTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class M3StringCodePointPrecomputeTest {
    private static long checks;

    public static void main(String[] args) throws Exception {
        List<String> values = new ArrayList<>();

        StringBuilder mixed = new StringBuilder();
        for (int i = 0; i < 512; i++) {
            mixed.append((char) ('a' + (i % 26)));
            if ((i & 3) == 0) mixed.appendCodePoint(0x1F600 + (i & 7));
            if ((i & 31) == 7) mixed.append('\uD800');
            if ((i & 31) == 15) mixed.append('\uDC00');
        }
        values.add(fresh(mixed.toString()));

        String high = fresh("\uD83D");
        String low = fresh("\uDE00");
        String seamPair = fresh("left-").repeat(40).concat(high).concat(low)
                .concat(fresh("-right").repeat(40));
        values.add(seamPair);
        values.add(seamPair.substring(3, seamPair.length() - 3));
        values.add(fresh("latin-only-").repeat(64));

        for (String value : values) {
            verifyCounts(value);
            verifyOffsets(value);
        }

        Class<?> precompute = Class.forName("java.lang.M3StringCodePointPrecompute");
        Method maximum = precompute.getDeclaredMethod("maximumRetainedPrimitiveBytes");
        maximum.setAccessible(true);
        long maximumBytes = (long) maximum.invoke(null);
        check(maximumBytes > 0 && maximumBytes <= 256L * 1024L,
                "bounded retained primitive ceiling bytes=" + maximumBytes);

        System.out.println("M3_STRING_CODEPOINT_PRECOMPUTE_PASS checks=" + checks
                + " maximumPrimitiveBytes=" + maximumBytes);
    }

    private static void verifyCounts(String value) {
        char[] chars = value.toCharArray();
        Random random = new Random(0x4d334350L ^ value.length());

        check(value.codePointCount(0, value.length())
                        == Character.codePointCount(chars, 0, chars.length),
                "whole codePointCount");

        int[] edges = {
                0,
                Math.min(1, chars.length),
                Math.max(0, chars.length / 2 - 1),
                chars.length / 2,
                Math.min(chars.length, chars.length / 2 + 1),
                Math.max(0, chars.length - 1),
                chars.length
        };
        for (int begin : edges) {
            for (int end : edges) {
                if (begin > end) continue;
                int expected = Character.codePointCount(chars, begin, end - begin);
                int actual = value.codePointCount(begin, end);
                check(expected == actual,
                        "edge codePointCount begin=" + begin + " end=" + end
                                + " expected=" + expected + " actual=" + actual);
            }
        }

        for (int sample = 0; sample < 10_000; sample++) {
            int a = random.nextInt(chars.length + 1);
            int b = random.nextInt(chars.length + 1);
            int begin = Math.min(a, b);
            int end = Math.max(a, b);
            int expected = Character.codePointCount(chars, begin, end - begin);
            int actual = value.codePointCount(begin, end);
            check(expected == actual,
                    "random codePointCount begin=" + begin + " end=" + end);
        }
    }

    private static void verifyOffsets(String value) {
        char[] chars = value.toCharArray();
        Random random = new Random(0x4d334f46L ^ value.length());

        for (int sample = 0; sample < 4_000; sample++) {
            int index = random.nextInt(chars.length + 1);
            int backward = Character.codePointCount(chars, 0, index);
            int forward = Character.codePointCount(chars, index, chars.length - index);

            int[] offsets = {
                    0,
                    forward == 0 ? 0 : 1,
                    forward,
                    forward / 2,
                    backward == 0 ? 0 : -1,
                    -backward,
                    -(backward / 2)
            };
            for (int offset : offsets) {
                int expected =
                        Character.offsetByCodePoints(chars, 0, chars.length, index, offset);
                int actual = value.offsetByCodePoints(index, offset);
                check(expected == actual,
                        "offsetByCodePoints index=" + index + " offset=" + offset
                                + " expected=" + expected + " actual=" + actual);

                // Repeat the exact query so hot cache reuse is exercised without changing semantics.
                int repeated = value.offsetByCodePoints(index, offset);
                check(repeated == expected,
                        "repeated offsetByCodePoints index=" + index + " offset=" + offset);
            }

            expectBounds(
                    () -> value.offsetByCodePoints(index, forward + 1),
                    "forward overflow index=" + index);
            expectBounds(
                    () -> value.offsetByCodePoints(index, -(backward + 1)),
                    "reverse overflow index=" + index);
        }

        expectBounds(() -> value.offsetByCodePoints(-1, 0), "negative index");
        expectBounds(() -> value.offsetByCodePoints(chars.length + 1, 0), "index beyond end");
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static void expectBounds(Throwing action, String label) {
        checks++;
        try {
            action.run();
            throw new AssertionError("expected IndexOutOfBoundsException: " + label);
        } catch (IndexOutOfBoundsException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    @FunctionalInterface
    private interface Throwing {
        void run();
    }
}
