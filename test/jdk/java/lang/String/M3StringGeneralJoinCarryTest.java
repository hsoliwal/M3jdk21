/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary General M3 join folds use O(log N) lanes and preserve exact historical pairwise owners
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringGeneralJoinCarryTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringGeneralJoinCarryTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class M3StringGeneralJoinCarryTest {
    private static final Class<?> M3;
    private static final Field STRING_M3;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static final Method ARRAY_JOIN;
    private static final Method VALUE_JOIN;
    private static final Method CANONICALIZE;
    private static final Method EMPTY;
    private static final Method POOL_CONCAT;
    private static final Method LENGTH;
    private static final Method CHAR_AT;
    private static int checks;

    static {
        try {
            M3 = Class.forName("java.lang.M3String");
            Class<?> pool = Class.forName("java.lang.M3StringPool");
            STRING_M3 = String.class.getDeclaredField("m3");
            OWNER = M3.getDeclaredField("owner");
            COORDINATE = M3.getDeclaredField("value");
            ARRAY_JOIN = M3.getDeclaredMethod("join", String[].class);
            VALUE_JOIN = M3.getDeclaredMethod("joinValues", ArrayList.class);
            CANONICALIZE = M3.getDeclaredMethod("canonicalize", String.class);
            EMPTY = M3.getDeclaredMethod("empty");
            POOL_CONCAT = pool.getDeclaredMethod("concat", M3, M3);
            LENGTH = M3.getDeclaredMethod("length");
            CHAR_AT = M3.getDeclaredMethod("charAt", int.class);
            for (var field : new Field[] {STRING_M3, OWNER, COORDINATE}) {
                field.setAccessible(true);
            }
            for (var method : new Method[] {
                    ARRAY_JOIN, VALUE_JOIN, CANONICALIZE, EMPTY, POOL_CONCAT, LENGTH, CHAR_AT}) {
                method.setAccessible(true);
            }
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        compare(new String[0]);
        compare(new String[] {""});
        compare(new String[] {"", "A", "", "\uD83D", "\uDE03", "", "\u0100", ""});
        compare(new String[] {"left", "", "middle", "", "right"});
        compare(new String[] {"\uD83D", "\uDE03", "\uD83D\uDE03", "\uD800", "\uDC00"});

        Random random = new Random(0x20261010L);
        String[] alphabet = {
                "", "", "", "A", "b", "\n", "\0", "\u00E9", "\u0100",
                "\uD83D", "\uDE03", "\uD83D\uDE03", "\uDC00", "\uD800"
        };
        for (int trial = 0; trial < 160; trial++) {
            String[] pieces = new String[random.nextInt(65)];
            for (int i = 0; i < pieces.length; i++) {
                // This constructor exercises fresh canonical admissions and preserves UTF-16 units.
                pieces[i] = new String(alphabet[random.nextInt(alphabet.length)].toCharArray());
            }
            compare(pieces);
        }

        String[] longArray = new String[256];
        for (int i = 0; i < longArray.length; i++) {
            longArray[i] = (i % 5 == 0) ? "" : "x";
        }
        compare(longArray);
        expectNullPieceRejected();
        expectSingletonNullPreserved();
        System.out.println("M3_GENERAL_JOIN_CARRY_PASS|checks=" + checks);
    }

    private static void compare(String[] parts) throws Exception {
        Object result = ARRAY_JOIN.invoke(null, (Object) parts);
        check(result != null, "M3 array admission unexpectedly refused");
        ArrayList<Object> active = new ArrayList<>();
        ArrayList<Object> exactPositions = new ArrayList<>();
        StringBuilder expectedText = new StringBuilder();
        for (String part : parts) {
            Object m3 = m3(part);
            exactPositions.add(m3);
            if (!part.isEmpty()) active.add(m3);
            expectedText.append(part);
        }
        Object arrayReference = oldPairwise(active);
        sameCoordinate(result, arrayReference, "general array fold");
        check(expectedText.toString().equals(spelling(result)), "UTF-16 content");
        check(expectedText.toString().hashCode() == spelling(result).hashCode(), "Java hash");

        Object privateResult = VALUE_JOIN.invoke(null, exactPositions);
        Object privateReference = oldPairwise(exactPositions);
        sameCoordinate(privateResult, privateReference, "internal value fold");
        check(expectedText.toString().equals(spelling(privateResult)), "internal UTF-16 content");
    }

    private static Object m3(String source) throws Exception {
        if (source.isEmpty()) return EMPTY.invoke(null);
        Object storage = STRING_M3.get(source);
        if (storage == null) storage = CANONICALIZE.invoke(null, source);
        check(storage != null && (int) LENGTH.invoke(storage) == source.length(),
                "canonical String source absent");
        return storage;
    }

    /** M3String is not a public String; read its exact UTF-16 units without assuming toString(). */
    private static String spelling(Object value) throws Exception {
        int count = (int) LENGTH.invoke(value);
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(((Character) CHAR_AT.invoke(value, index)).charValue());
        }
        return builder.toString();
    }

    private static Object oldPairwise(List<Object> values) throws Exception {
        if (values.isEmpty()) return EMPTY.invoke(null);
        ArrayList<Object> level = new ArrayList<>(values);
        while (level.size() > 1) {
            ArrayList<Object> next = new ArrayList<>((level.size() + 1) >>> 1);
            for (int index = 0; index < level.size(); index += 2) {
                next.add(index + 1 == level.size()
                        ? level.get(index)
                        : POOL_CONCAT.invoke(null, level.get(index), level.get(index + 1)));
            }
            level = next;
        }
        return level.getFirst();
    }

    private static void sameCoordinate(Object actual, Object expected, String operation)
            throws Exception {
        check(OWNER.get(actual) == OWNER.get(expected),
                operation + " changed canonical owner");
        check(COORDINATE.getLong(actual) == COORDINATE.getLong(expected),
                operation + " changed canonical UTF-16 coordinate");
    }

    private static void expectSingletonNullPreserved() throws Exception {
        ArrayList<Object> values = new ArrayList<>();
        values.add(null);
        check(VALUE_JOIN.invoke(null, values) == null,
                "single-value private fold must preserve the original null result");
    }

    private static void expectNullPieceRejected() throws Exception {
        try {
            ARRAY_JOIN.invoke(null, (Object) new String[] {"x", null, "y"});
            throw new AssertionError("null piece was accepted");
        } catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof NullPointerException,
                    "null piece exception contract");
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
