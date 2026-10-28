/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary JNI-callable UTF-16 ranges compose into one interned M3 String view
 * @run main/native/othervm -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCharRangeJniIngressTest
 * @run main/native/othervm -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCharRangeJniIngressTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class M3StringCharRangeJniIngressTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field M3_COORDINATE;
    private static final Method RANGE_JOIN;
    private static final Method CHAR_SHADOW;
    private static final Field TUPLE_HEIGHT;
    private static int checks;

    static {
        System.loadLibrary("M3StringCharRangeJniIngress");
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            M3_COORDINATE = m3.getDeclaredField("value");
            M3_COORDINATE.setAccessible(true);
            RANGE_JOIN = String.class.getDeclaredMethod(
                    "m3JoinCharRanges", char[][].class, int[].class, int[].class);
            RANGE_JOIN.setAccessible(true);
            CHAR_SHADOW = m3.getDeclaredMethod("charShadow");
            CHAR_SHADOW.setAccessible(true);
            Class<?> tuple = Class.forName("java.lang.M3StringTuple");
            TUPLE_HEIGHT = tuple.getDeclaredField("height");
            TUPLE_HEIGHT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static native String invokeJoinRanges(
            char[][] sources, int[] starts, int[] lengths);

    public static void main(String[] args) throws Exception {
        check(Modifier.isStatic(RANGE_JOIN.getModifiers())
                        && RANGE_JOIN.getReturnType() == String.class,
                "JNI bridge has a static String-returning signature");

        char[] first = {'!', 'A', '\uD83D', '?'};
        char[] second = {'\uDE00', 'B', '!'};
        char[] empty = {'x'};
        char[] third = {'?', 'C'};
        char[][] sources = {first, second, empty, third};
        int[] starts = {1, 0, 0, 1};
        int[] lengths = {2, 2, 0, 1};

        String joined = joinRanges(sources, starts, lengths);
        String repeated = joinRanges(sources, starts, lengths);
        check(joined != null && joined.equals("A\uD83D\uDE00BC"),
                "selected ranges preserve UTF-16 order across a split surrogate pair");
        check(repeated != null && repeated.equals(joined), "repeat has equal text");

        Object storage = requireM3(joined);
        Object repeatedStorage = requireM3(repeated);
        Object owner = M3_OWNER.get(storage);
        long coordinate = M3_COORDINATE.getLong(storage);
        check(owner == M3_OWNER.get(repeatedStorage)
                        && coordinate == M3_COORDINATE.getLong(repeatedStorage),
                "same interned pieces reuse the canonical tuple coordinate");

        int fanout = 257; // Deliberately not a power of two: exercise carry and final fold.
        char[][] manySources = new char[fanout][];
        int[] manyStarts = new int[fanout];
        int[] manyLengths = new int[fanout];
        char[] manyExpected = new char[fanout];
        for (int index = 0; index < fanout; index++) {
            char unit = (char) ('a' + index % 26);
            manySources[index] = new char[] {unit};
            manyLengths[index] = 1;
            manyExpected[index] = unit;
        }
        String many = joinRanges(manySources, manyStarts, manyLengths);
        check(many != null && many.equals(new String(manyExpected)),
                "257 selected ranges preserve order through balanced composition");
        Object manyStorage = requireM3(many);
        Object manyOwner = M3_OWNER.get(manyStorage);
        int height = TUPLE_HEIGHT.getInt(manyOwner);
        int ceilLog2 = Integer.SIZE - Integer.numberOfLeadingZeros(fanout - 1);
        check(height <= 2 * ceilLog2 + 1,
                "non-power-of-two tuple height remains logarithmically bounded");
        String manyAgain = joinRanges(manySources, manyStarts, manyLengths);
        Object manyAgainStorage = requireM3(manyAgain);
        check(manyOwner == M3_OWNER.get(manyAgainStorage)
                        && M3_COORDINATE.getLong(manyStorage)
                                == M3_COORDINATE.getLong(manyAgainStorage),
                "large repeated fanout reuses its canonical tuple coordinate");

        Arrays.fill(first, 'x');
        Arrays.fill(second, 'y');
        Arrays.fill(third, 'z');
        check(joined.equals("A\uD83D\uDE00BC"),
                "source mutation cannot alter the interned view");

        char[] shadow = (char[]) CHAR_SHADOW.invoke(storage);
        check(Arrays.equals(shadow, "A\uD83D\uDE00BC".toCharArray()),
                "JNI compatibility shadow materializes the logical tuple");
        if (shadow.length != 0) shadow[0] = '!';
        check(Arrays.equals((char[]) CHAR_SHADOW.invoke(storage),
                        "A\uD83D\uDE00BC".toCharArray()),
                "mutating a JNI shadow cannot alter canonical tuple storage");
        check(owner == M3_OWNER.get(requireM3(joined))
                        && coordinate == M3_COORDINATE.getLong(requireM3(joined)),
                "JNI materialization retains the same canonical owner and coordinate");

        String emptyView = joinRanges(new char[][] {{'x'}}, new int[] {0}, new int[] {0});
        String noRanges = joinRanges(new char[0][], new int[0], new int[0]);
        check(emptyView != null && emptyView.isEmpty(), "empty ranges compose to empty String");
        check(noRanges != null && noRanges.isEmpty(), "empty descriptor lanes compose to empty String");

        expect(IndexOutOfBoundsException.class,
                () -> joinRanges(new char[][] {{'x'}}, new int[] {1}, new int[] {1}),
                "range bounds rejected before admission");
        expect(IllegalArgumentException.class,
                () -> joinRanges(new char[][] {{'x'}}, new int[0], new int[] {1}),
                "mismatched descriptor lanes rejected");

        System.out.println("M3_STRING_CHAR_RANGE_JNI_INGRESS_PASS|checks=" + checks);
    }

    private static String joinRanges(char[][] sources, int[] starts, int[] lengths) {
        return invokeJoinRanges(sources, starts, lengths);
    }

    private static Object requireM3(String value) throws IllegalAccessException {
        value.length();
        Object m3 = STRING_M3.get(value);
        check(m3 != null, "String must retain M3 storage");
        return m3;
    }

    private static void expect(
            Class<? extends Throwable> expected, CheckedAction action, String label)
            throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            check(expected.isInstance(failure), label + ": " + failure);
            return;
        }
        throw new AssertionError(label + ": no exception");
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
