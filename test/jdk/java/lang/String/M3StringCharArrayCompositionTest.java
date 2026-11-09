/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3String must compose independent char arrays and ranges without retaining flat payloads
 * @run main/othervm --add-opens java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCharArrayCompositionTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class M3StringCharArrayCompositionTest {
    private static long checks;
    private static Class<?> type;
    private static Method admitArrays;
    private static Method admitRanges;
    private static Method length;
    private static Method charAt;
    private static Method hash;
    private static Method owner;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label + " check=" + checks);
    }

    private static Method method(String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Method method, Object... arguments) throws Exception {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw failure;
        }
    }

    private static int length(Object value) throws Exception {
        return (int) length.invoke(value);
    }

    private static char charAt(Object value, int index) throws Exception {
        return (char) charAt.invoke(value, index);
    }

    private static int hash(Object value) throws Exception {
        return (int) hash.invoke(value);
    }

    private static String text(Object value) throws Exception {
        char[] chars = new char[length(value)];
        for (int index = 0; index < chars.length; index++) chars[index] = charAt(value, index);
        return new String(chars);
    }

    private static void sameText(Object value, String expected, String label) throws Exception {
        check(value != null, label + " admitted");
        check(length(value) == expected.length(), label + " length");
        for (int index = 0; index < expected.length(); index++) {
            check(charAt(value, index) == expected.charAt(index), label + " unit=" + index);
        }
        check(hash(value) == expected.hashCode(), label + " hash");
        check(text(value).equals(expected), label + " text");
    }

    private static void expect(Class<? extends Throwable> expected, Throwing action, String label)
            throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(label + " expected=" + expected.getName()
                    + " actual=" + failure, failure);
        }
        throw new AssertionError(label + " did not fail");
    }

    public static void main(String[] args) throws Exception {
        type = Class.forName("java.lang.M3String");
        admitArrays = method("admitArrays", char[][].class);
        admitRanges = method("admitArrayRanges", char[][].class, int[].class, int[].class);
        length = method("length");
        charAt = method("charAt", int.class);
        hash = method("hashCodeValue");
        owner = method("owner");

        Field[] state =
                Arrays.stream(type.getDeclaredFields())
                        .filter(field -> !Modifier.isStatic(field.getModifiers()))
                        .toArray(Field[]::new);
        check(state.length == 2, "M3String instance field count");
        check(Arrays.stream(state).anyMatch(field -> field.getName().equals("owner")),
                "owner field");
        check(Arrays.stream(state).anyMatch(field -> field.getName().equals("value")
                        && field.getType() == long.class),
                "packed value field");
        check(Arrays.stream(state).noneMatch(field -> field.getType().isArray()),
                "no retained array fields");

        char[] first = {'a', 'b'};
        char[] high = {'\ud83d'};
        char[] tail = {'\ude42', 'c', '\0', '\ud800'};
        Object joined = invoke(admitArrays, (Object) new char[][] {first, high, tail});
        sameText(joined, "ab\ud83d\ude42c\0\ud800", "three arrays");
        check(owner.invoke(joined).getClass().getSimpleName().equals("M3StringTuple"),
                "multi-array tuple owner");

        first[0] = 'z';
        high[0] = 'x';
        tail[0] = 'y';
        sameText(joined, "ab\ud83d\ude42c\0\ud800", "source mutation isolation");

        char[][] ranges = {
                {'@', 'A', 'B', '#'},
                {'x', '\ud83d', 'q'},
                {'p', '\ude42', 'C', 'D', '!'}
        };
        Object ranged =
                invoke(
                        admitRanges,
                        (Object) ranges,
                        new int[] {1, 1, 1},
                        new int[] {2, 1, 3});
        sameText(ranged, "AB\ud83d\ude42CD", "selected ranges");

        Object two =
                invoke(
                        admitArrays,
                        (Object) new char[][] {
                                "left".toCharArray(), "right".toCharArray()
                        });
        sameText(two, "leftright", "two arrays");

        char[][] many = new char[64][];
        StringBuilder manyExpected = new StringBuilder();
        for (int index = 0; index < many.length; index++) {
            many[index] = new char[] {(char) ('a' + index % 26), (char) ('0' + index % 10)};
            manyExpected.append(many[index]);
        }
        Object manyValue = invoke(admitArrays, (Object) many);
        sameText(manyValue, manyExpected.toString(), "sixty-four arrays");

        Object emptyA = invoke(admitArrays, (Object) new char[0][]);
        Object emptyB =
                invoke(
                        admitRanges,
                        (Object) new char[][] {new char[0], {'x'}},
                        new int[] {0, 1},
                        new int[] {0, 0});
        check(emptyA == emptyB, "canonical empty identity");
        sameText(emptyA, "", "empty");

        expect(
                NullPointerException.class,
                () -> invoke(admitArrays, new Object[] {null}),
                "null outer arrays");
        expect(
                NullPointerException.class,
                () -> invoke(admitArrays, (Object) new char[][] {null}),
                "null inner array");
        expect(
                IllegalArgumentException.class,
                () -> invoke(
                        admitRanges,
                        (Object) new char[][] {{'a'}},
                        new int[] {0, 0},
                        new int[] {1}),
                "range lane cardinality");
        expect(
                IndexOutOfBoundsException.class,
                () -> invoke(
                        admitRanges,
                        (Object) new char[][] {{'a', 'b'}},
                        new int[] {1},
                        new int[] {2}),
                "invalid range");

        System.out.println("M3_STRING_CHAR_ARRAY_COMPOSITION_PASS checks=" + checks);
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
