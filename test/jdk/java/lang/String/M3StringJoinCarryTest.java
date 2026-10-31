/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String.join uses logarithmic carry staging without changing UTF-16 or tuple identity
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringJoinCarryTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringJoinCarryTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

public class M3StringJoinCarryTest {
    private static final Field M3;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static final Field HEIGHT;
    private static final Method OLD_PAIRWISE_JOIN;
    private static int checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
            Class<?> type = Class.forName("java.lang.M3String");
            OWNER = type.getDeclaredField("owner");
            COORDINATE = type.getDeclaredField("value");
            OWNER.setAccessible(true);
            COORDINATE.setAccessible(true);
            Class<?> tuple = Class.forName("java.lang.M3StringTuple");
            HEIGHT = tuple.getDeclaredField("height");
            HEIGHT.setAccessible(true);
            OLD_PAIRWISE_JOIN = type.getDeclaredMethod("join", String[].class);
            OLD_PAIRWISE_JOIN.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        basicValuesAndFreshWrappers();
        iterableConversionOrder();
        pairwiseCanonicalIdentityAndDepth();
        randomizedUtf16();
        System.out.println("M3_STRING_JOIN_CARRY_PASS|checks=" + checks);
    }

    private static void basicValuesAndFreshWrappers() throws Exception {
        check(String.join("|", "a", null, "\u03B2", "\uD83D\uDE03")
                .equals("a|null|\u03B2|\uD83D\uDE03"), "mixed content");
        check(String.join("::").equals(""), "empty varargs");
        check(String.join("--", "", "").equals("--"), "empty participants");
        check(String.join("", "", "", "").equals(""), "all empty");
        String singleton = new String(new char[] {'Z'});
        String joined = String.join(",", singleton);
        check(joined.equals(singleton) && joined != singleton, "singleton wrapper freshness");
        StringJoiner prefixed = new StringJoiner("::", "<", ">");
        prefixed.add("x").add(null).add("\uD83D\uDE03");
        check(prefixed.toString().equals("<x::null::\uD83D\uDE03>"),
                "prefix/suffix and null value");
        check(body(prefixed.toString()) != null, "StringJoiner M3 result");
    }

    private static void iterableConversionOrder() {
        StringBuilder trace = new StringBuilder();
        List<CharSequence> values = List.of(
                new Traced("A", trace),
                new Traced("B", trace),
                new Traced("C", trace));
        String joined = String.join("|", values);
        check(joined.equals("A|B|C"), "iterable content");
        check(trace.toString().equals("ABC"), "CharSequence.toString once in order");
    }

    private static void pairwiseCanonicalIdentityAndDepth() throws Exception {
        String[] elements = new String[1_024];
        for (int index = 0; index < elements.length; index++) {
            elements[index] = "item-" + index;
        }
        String delim = "::";
        String actual = String.join(delim, elements);
        String expected = expected(delim, elements);
        check(actual.equals(expected), "1024-element equality");
        Object actualBody = body(actual);
        check(actualBody != null, "1024-element M3 body");

        String[] historicalPieces = new String[elements.length * 2 - 1];
        int count = 0;
        historicalPieces[count++] = elements[0];
        for (int index = 1; index < elements.length; index++) {
            historicalPieces[count++] = delim;
            historicalPieces[count++] = elements[index];
        }
        Object historical = OLD_PAIRWISE_JOIN.invoke(null, (Object) historicalPieces);
        check(OWNER.get(actualBody) == OWNER.get(historical),
                "canonical owner independent of join staging parenthesization");
        check(COORDINATE.getLong(actualBody) == COORDINATE.getLong(historical),
                "exact canonical coordinate preserved");

        Object owner = OWNER.get(actualBody);
        if (owner.getClass().getName().equals("java.lang.M3StringTuple")) {
            int height = HEIGHT.getInt(owner);
            check(height <= 20, "logarithmic tuple height " + height);
        }
    }

    private static void randomizedUtf16() throws Exception {
        Random random = new Random(0x20261008L);
        String[] pool = {"", "a", "\u0000", "\u00E9", "\u0100", "\uD83D",
                "\uDE03", "\uD83D\uDE03", "\n", "\r", "null", "##"};
        for (int trial = 0; trial < 512; trial++) {
            int size = random.nextInt(65);
            String[] values = new String[size];
            for (int i = 0; i < size; i++) {
                values[i] = pool[random.nextInt(pool.length)];
            }
            String delim = pool[random.nextInt(pool.length)];
            String joined = String.join(delim, values);
            String expected = expected(delim, values);
            check(joined.equals(expected), "random UTF-16 value parity trial=" + trial);
            check(joined.hashCode() == expected.hashCode(), "random Java hash parity");
            if (!joined.isEmpty() && size != 1) {
                check(body(joined) != null, "random joined result is M3-backed");
            }
        }
    }

    private static String expected(String delimiter, String[] elements) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) builder.append(delimiter);
            builder.append(elements[i]);
        }
        return builder.toString();
    }

    private static Object body(String value) throws IllegalAccessException {
        return M3.get(value);
    }

    private record Traced(String value, StringBuilder trace) implements CharSequence {
        @Override public int length() { return value.length(); }
        @Override public char charAt(int index) { return value.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }
        @Override public String toString() {
            trace.append(value);
            return value;
        }
    }

    private static void check(boolean valid, String description) {
        checks++;
        if (!valid) throw new AssertionError(description);
    }
}
