/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify M3-backed String chars/codePoints traverse canonical coordinates without flattening
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringStreamsTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;

public class M3StringStreamsTest {
    private static final Field STRING_M3;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        verify(fresh("alpha"), "ASCII");
        verify(fresh("A\u20acB"), "BMP");
        verify(fresh("A\ud83d\ude03B"), "surrogate pair");
        verify(fresh("A\ud83dB"), "unpaired high surrogate");
        verify(fresh("A\ude03B"), "unpaired low surrogate");

        String atom = fresh("xxA\ud83d\ude03Byy");
        String range = atom.substring(2, atom.length() - 2);
        verify(range, "range over canonical atom");

        String tuple = fresh("left-").concat(fresh("A\ud83d\ude03B")).concat(fresh("-right"));
        verify(tuple, "tuple composition");

        int[] sequential = tuple.codePoints().toArray();
        int[] parallel = tuple.codePoints().parallel().toArray();
        check(Arrays.equals(sequential, parallel), "parallel codePoints split preserves order/content");

        int[] charsSequential = tuple.chars().toArray();
        int[] charsParallel = tuple.chars().parallel().toArray();
        check(Arrays.equals(charsSequential, charsParallel), "parallel chars split preserves order/content");

        System.out.println("M3_STRING_STREAMS_PASS checks=" + checks);
    }

    private static void verify(String value, String label) throws Exception {
        ensureM3(value, label);

        int[] expectedChars = expectedChars(value);
        int[] actualChars = value.chars().toArray();
        check(Arrays.equals(expectedChars, actualChars), label + " chars");

        int[] expectedCodePoints = expectedCodePoints(value);
        int[] actualCodePoints = value.codePoints().toArray();
        check(Arrays.equals(expectedCodePoints, actualCodePoints), label + " codePoints");
    }

    private static int[] expectedChars(String value) {
        int[] result = new int[value.length()];
        for (int index = 0; index < result.length; index++) {
            result[index] = value.charAt(index);
        }
        return result;
    }

    private static int[] expectedCodePoints(String value) {
        int[] scratch = new int[value.length()];
        int count = 0;
        for (int index = 0; index < value.length();) {
            char first = value.charAt(index++);
            if (Character.isHighSurrogate(first) && index < value.length()) {
                char second = value.charAt(index);
                if (Character.isLowSurrogate(second)) {
                    index++;
                    scratch[count++] = Character.toCodePoint(first, second);
                    continue;
                }
            }
            scratch[count++] = first;
        }
        return Arrays.copyOf(scratch, count);
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static void ensureM3(String value, String label) throws Exception {
        value.length();
        check(STRING_M3.get(value) != null, label + " must be M3-backed");
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
