/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 binary concat avoids array staging while retaining canonical owner and UTF-16 semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringBinaryConcatTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringBinaryConcatTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

public class M3StringBinaryConcatTest {
    private static final Field STRING_M3;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static final Method DIRECT;
    private static final Method HISTORICAL;
    private static final Method STRING_CONCAT;
    private static int checks;

    static {
        try {
            Class<?> m3 = Class.forName("java.lang.M3String");
            DIRECT = m3.getDeclaredMethod("join", String.class, String.class);
            HISTORICAL = m3.getDeclaredMethod("join", String[].class);
            STRING_CONCAT = String.class.getDeclaredMethod(
                    "m3Concat", String.class, String.class);
            STRING_M3 = String.class.getDeclaredField("m3");
            OWNER = m3.getDeclaredField("owner");
            COORDINATE = m3.getDeclaredField("value");
            for (Method method : new Method[] {DIRECT, HISTORICAL, STRING_CONCAT}) {
                method.setAccessible(true);
            }
            for (Field field : new Field[] {STRING_M3, OWNER, COORDINATE}) {
                field.setAccessible(true);
            }
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] ignored) throws Exception {
        deterministic();
        randomized();
        nullContract();
        System.out.println("M3_STRING_BINARY_CONCAT_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        String parent = new String(new char[] {'x', 'a', 'b', 'c', '\uD83D',
                '\uDE03', '!', 'y'});
        check(parent.length() == 8, "source exact UTF-16 size");
        compare(parent.substring(1, 4), parent.substring(4, 7));
        compare("", "");
        compare("", "alpha");
        compare("alpha", "");
        compare("\uD83D", "\uDE03");
        compare("\uDE03", "\uD83D");
        compare("Latin1", "\u03B2eta");
        compare("\u0000", "\u0000");
        compare("\uFFFF", "\uD800");
        compare("a".repeat(128), "b".repeat(128));

        String first = publicConcat("alpha", "\u03B2eta");
        String second = publicConcat("alpha", "\u03B2eta");
        check(first != second, "binary wrappers remain fresh");
        check(first.equals(second), "binary results retain content equality");
        check(STRING_M3.get(first) != null, "public M3 concat has canonical owner");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x20261008L);
        char[] alphabet = {'\0', '\n', '\r', 'a', 'b', '\u00E9', '\u0100',
                '\uD800', '\uDC00', '\uD83D', '\uDE03', '\uFFFF'};
        for (int trial = 0; trial < 4096; trial++) {
            String left = randomText(random, alphabet, trial % 47);
            String right = randomText(random, alphabet, (trial * 7) % 53);
            compare(left, right);
        }
    }

    private static String randomText(Random random, char[] alphabet, int length) {
        char[] data = new char[length];
        for (int index = 0; index < length; index++) {
            data[index] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(data);
    }

    private static void compare(String left, String right) throws Exception {
        Object direct = DIRECT.invoke(null, left, right);
        Object historical = HISTORICAL.invoke(null, (Object) new String[] {left, right});
        check(direct != null && historical != null, "both M3 paths return a descriptor");
        check(OWNER.get(direct) == OWNER.get(historical),
                "direct/historical owner identity");
        check(COORDINATE.getLong(direct) == COORDINATE.getLong(historical),
                "direct/historical packed-coordinate identity");

        String actual = publicConcat(left, right);
        String expected = new StringBuilder(left).append(right).toString();
        check(actual.equals(expected), "JDK UTF-16 content parity");
        check(actual.length() == expected.length(), "UTF-16 length parity");
        check(actual.hashCode() == expected.hashCode(), "Java hash parity");
        Object backing = STRING_M3.get(actual);
        check(backing != null && OWNER.get(backing) == OWNER.get(direct),
                "public wrapper retains the same canonical owner");
        check(COORDINATE.getLong(backing) == COORDINATE.getLong(direct),
                "public wrapper retains the same exact coordinate");
    }

    private static String publicConcat(String left, String right) throws Exception {
        Object value = STRING_CONCAT.invoke(null, left, right);
        check(value instanceof String, "M3 concat is active");
        return (String) value;
    }

    private static void nullContract() throws Exception {
        for (boolean nullLeft : new boolean[] {true, false}) {
            String a = nullLeft ? null : "x";
            String b = nullLeft ? "x" : null;
            check(throwsNpe(DIRECT, a, b), "direct null behavior");
            check(throwsNpe(HISTORICAL, (Object) new String[] {a, b}),
                    "historical null behavior");
        }
    }

    private static boolean throwsNpe(Method method, Object... args) throws Exception {
        try {
            method.invoke(null, args);
            return false;
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof NullPointerException) return true;
            throw failure;
        }
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }
}
