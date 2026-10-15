/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Composite M3 String leaf coordinate canonicalization is independent of DAG parenthesization
 * @run main/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLeafCursorCanonicalTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

/** A target jtreg test; stock-JDK execution with m3.oracle.only exercises content checks only. */
public class M3StringLeafCursorCanonicalTest {
    private static final boolean ORACLE_ONLY = Boolean.getBoolean("m3.oracle.only");
    private static final Field STRING_M3;
    private static final Field OWNER;
    private static long checks;

    static {
        Field storage = null;
        Field owner = null;
        if (!ORACLE_ONLY) {
            try {
                storage = String.class.getDeclaredField("m3");
                storage.setAccessible(true);
                Class<?> type = Class.forName("java.lang.M3String");
                owner = type.getDeclaredField("owner");
                owner.setAccessible(true);
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        STRING_M3 = storage;
        OWNER = owner;
    }

    public static void main(String[] args) throws Exception {
        String[] seed = {
            fresh("banana"), fresh("Xy"), fresh("\u0000a"), fresh("\ud83dA"),
            fresh("\ude03b"), fresh("\uffff\u07ff"), fresh("Aa"), fresh("BB")
        };
        String high = fresh("\ud83d");
        String low = fresh("\ude03");
        String a = fresh("a");
        String b = fresh("b");
        String left = a.concat(high).concat(low).concat(b);
        String right = a.concat(high.concat(low.concat(b)));
        verify(left, "a\ud83d\ude03b");
        verify(right, "a\ud83d\ude03b");
        identity(left, right, "tuple associativity");
        check(Arrays.equals(new int[] {'a', 0x1f603, 'b'}, left.codePoints().toArray()),
                "surrogate pair assembled across canonical atoms");

        String source = fresh("xx\ud83d\ude03yy");
        verify(source.substring(2, 3).concat(source.substring(3, 4)), "\ud83d\ude03");
        String first = a.concat(source.substring(2, 3)).concat(source.substring(3, 4));
        String second = a.concat(source.substring(2, 4));
        verify(first, "a\ud83d\ude03");
        verify(second, "a\ud83d\ude03");
        identity(first, second, "adjacent atom slices");

        Random random = new Random(20261015);
        for (int trial = 0; trial < 1500; trial++) {
            int count = 1 + random.nextInt(8);
            String[] pieces = new String[count];
            StringBuilder expected = new StringBuilder();
            for (int i = 0; i < count; i++) {
                String word = seed[random.nextInt(seed.length)];
                int start = random.nextInt(word.length() + 1);
                int end = start + random.nextInt(word.length() - start + 1);
                pieces[i] = word.substring(start, end);
                expected.append(pieces[i]);
            }
            String lhs = pieces[0];
            for (int i = 1; i < count; i++) lhs = lhs.concat(pieces[i]);
            String rhs = pieces[count - 1];
            for (int i = count - 2; i >= 0; i--) rhs = pieces[i].concat(rhs);
            verify(lhs, expected.toString());
            verify(rhs, expected.toString());
            if (!lhs.isEmpty()) identity(lhs, rhs, "random tuple reparenthesization");
            if (lhs.length() > 2) {
                int start = random.nextInt(lhs.length());
                int end = start + random.nextInt(lhs.length() - start + 1);
                verify(lhs.substring(start, end), expected.substring(start, end));
            }
        }
        String collidingA = fresh("A").concat(fresh("a"));
        String collidingB = fresh("B").concat(fresh("B"));
        check(collidingA.hashCode() == collidingB.hashCode(), "hash fixture");
        check(!collidingA.equals(collidingB), "hash collision must not imply equality");
        if (!ORACLE_ONLY) check(owner(collidingA) != owner(collidingB),
                "candidate hash cannot prove canonical tuple identity");
        System.out.println("M3_LEAF_CURSOR_JTREG checks=" + checks
                + " oracleOnly=" + ORACLE_ONLY + " PASS");
    }

    private static void verify(String actual, String expected) {
        check(actual.equals(expected), "equals");
        check(expected.equals(actual), "reverse equals");
        check(actual.compareTo(expected) == 0, "compareTo");
        check(actual.hashCode() == expected.hashCode(), "hashCode");
        check(actual.length() == expected.length(), "length");
        check(Arrays.equals(actual.chars().toArray(), expected.chars().toArray()), "UTF-16");
        check(Arrays.equals(actual.codePoints().toArray(), expected.codePoints().toArray()),
                "Unicode seam");
    }

    private static Object owner(String value) throws Exception {
        Object storage = STRING_M3.get(value);
        check(storage != null, "nonempty String has native-backed M3 coordinates");
        return OWNER.get(storage);
    }

    private static void identity(String a, String b, String reason) throws Exception {
        if (!ORACLE_ONLY) check(owner(a) == owner(b), reason);
    }

    private static String fresh(String text) {
        return new String(text.toCharArray());
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
}
