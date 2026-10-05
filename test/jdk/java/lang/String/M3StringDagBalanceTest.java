/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify canonical M3 tuple identity is association-independent and AVL-bounded
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringDagBalanceTest
 */

import java.lang.reflect.Field;

public class M3StringDagBalanceTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field TUPLE_HEIGHT;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            Class<?> tuple = Class.forName("java.lang.M3StringTuple");
            TUPLE_HEIGHT = tuple.getDeclaredField("height");
            TUPLE_HEIGHT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        associationIndependentIdentity();
        deepAppendPrepend();
        System.out.println("M3_STRING_DAG_BALANCE_PASS|checks=" + checks);
    }

    private static void associationIndependentIdentity() throws Exception {
        String a = fresh("a");
        String b = fresh("b");
        String c = fresh("c");
        String d = fresh("d");

        String leftAssociated = a.concat(b).concat(c).concat(d);
        String rightAssociated = a.concat(b.concat(c.concat(d)));
        String mixed = a.concat(b).concat(c.concat(d));

        sameOwner(leftAssociated, rightAssociated, "left/right association");
        sameOwner(leftAssociated, mixed, "mixed association");
        equal("abcd", leftAssociated, "association content");
        equal(leftAssociated, rightAssociated, "association equality");
        check(leftAssociated.hashCode() == rightAssociated.hashCode(), "association hash");
    }

    private static void deepAppendPrepend() throws Exception {
        final int count = 4_096;
        String unit = fresh("x");
        String append = unit;
        String prepend = unit;
        for (int index = 1; index < count; index++) {
            append = append.concat(unit);
            prepend = unit.concat(prepend);
        }

        check(append.length() == count && prepend.length() == count, "deep length");
        sameOwner(append, prepend, "append/prepend canonical coordinate sequence");

        int appendHeight = height(append);
        int prependHeight = height(prepend);
        int logarithmicBound = Math.addExact(2 * ceilLog2(count), 2);
        check(appendHeight <= logarithmicBound,
                "append height " + appendHeight + " <= " + logarithmicBound);
        check(prependHeight <= logarithmicBound,
                "prepend height " + prependHeight + " <= " + logarithmicBound);

        check(append.charAt(0) == 'x'
                && append.charAt(count / 2) == 'x'
                && append.charAt(count - 1) == 'x', "deep charAt");

        String middle = append.substring(count / 2, count / 2 + 16);
        check(middle.length() == 16, "deep slice length");
        for (int index = 0; index < middle.length(); index++) {
            check(middle.charAt(index) == 'x', "deep slice content");
        }

        int expectedHash = 0;
        for (int index = 0; index < count; index++) expectedHash = 31 * expectedHash + 'x';
        check(append.hashCode() == expectedHash, "deep hash parity");

        char[] shadow = append.toCharArray();
        check(shadow.length == count && shadow[0] == 'x' && shadow[count - 1] == 'x',
                "deep JNI char shadow");
        shadow[0] = '!';
        check(append.charAt(0) == 'x', "shadow mutation isolation");
    }

    private static int height(String value) throws Exception {
        Object owner = owner(value);
        if (!owner.getClass().getName().equals("java.lang.M3StringTuple")) return 0;
        return TUPLE_HEIGHT.getInt(owner);
    }

    private static void sameOwner(String left, String right, String label) throws Exception {
        check(owner(left) == owner(right), label);
    }

    private static Object owner(String value) throws Exception {
        value.length();
        Object m3 = STRING_M3.get(value);
        if (m3 == null) throw new AssertionError("String was not admitted to M3");
        return M3_OWNER.get(m3);
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static int ceilLog2(int value) {
        int result = 0;
        int power = 1;
        while (power < value) {
            power <<= 1;
            result++;
        }
        return result;
    }

    private static void equal(Object expected, Object actual, String label) {
        check(expected.equals(actual), label + " expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
