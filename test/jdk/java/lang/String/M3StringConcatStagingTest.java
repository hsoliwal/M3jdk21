/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 concat removes O(N) reference staging while preserving JLS order and pairwise DAG identity
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringConcatStagingTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringConcatStagingTest
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class M3StringConcatStagingTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field M3_VALUE;
    private static final Class<?> M3_TUPLE;
    private static final Field TUPLE_LEFT;
    private static final Field TUPLE_RIGHT;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            M3_VALUE = m3.getDeclaredField("value");
            M3_VALUE.setAccessible(true);
            M3_TUPLE = Class.forName("java.lang.M3StringTuple");
            TUPLE_LEFT = M3_TUPLE.getDeclaredField("left");
            TUPLE_LEFT.setAccessible(true);
            TUPLE_RIGHT = M3_TUPLE.getDeclaredField("right");
            TUPLE_RIGHT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        indyOrderAndNullSemantics();
        publicJoinSemantics();
        pairwiseDagIdentityAndHeight();
        System.out.println("M3_STRING_CONCAT_STAGING_PASS|checks=" + checks);
    }

    private static void indyOrderAndNullSemantics() throws Exception {
        ArrayList<Integer> order = new ArrayList<>();
        Trace first = new Trace(1, "A", order);
        Trace second = new Trace(2, "B", order);
        Trace nullText = new Trace(3, null, order);
        String nullable = null;

        String actual = "P:" + first + ':' + second + ':' + nullText + ':' + nullable + ":S";
        check(actual.equals("P:A:B:null:null:S"), "indy concat content");
        check(order.equals(List.of(1, 2, 3)), "Object.toString evaluation order and count");
        check(m3(actual) != null, "indy concat result is M3-backed");
    }

    private static void publicJoinSemantics() throws Exception {
        String joined = String.join("|", "alpha", null, "\u03b2eta", "\ud83d\ude42");
        check(joined.equals("alpha|null|\u03b2eta|\ud83d\ude42"), "String.join null/Unicode semantics");
        check(m3(joined) != null, "String.join result is M3-backed");

        List<CharSequence> iterable =
                Arrays.asList(new StringBuilder("left"), null, new StringBuilder("\u0100right"));
        String iterableJoined = String.join("::", iterable);
        check(iterableJoined.equals("left::null::\u0100right"), "iterable join semantics");
        check(m3(iterableJoined) != null, "iterable join result is M3-backed");
    }

    private static void pairwiseDagIdentityAndHeight() throws Exception {
        String[] pieces = new String[1024];
        Arrays.fill(pieces, "x");

        String joined = String.join("", pieces);
        check(joined.equals("x".repeat(pieces.length)), "large join content");

        ArrayList<String> level = new ArrayList<>(Arrays.asList(pieces));
        while (level.size() > 1) {
            ArrayList<String> next = new ArrayList<>((level.size() + 1) >>> 1);
            for (int index = 0; index < level.size(); index += 2) {
                next.add(index + 1 == level.size()
                        ? level.get(index)
                        : level.get(index).concat(level.get(index + 1)));
            }
            level = next;
        }
        String historicalPairwise = level.getFirst();

        Object joinedM3 = m3(joined);
        Object referenceM3 = m3(historicalPairwise);
        check(joinedM3 != null && referenceM3 != null, "large joins are M3-backed");
        check(M3_OWNER.get(joinedM3) == M3_OWNER.get(referenceM3),
                "streaming join preserves pairwise canonical owner");
        check(M3_VALUE.getLong(joinedM3) == M3_VALUE.getLong(referenceM3),
                "streaming join preserves pairwise canonical coordinate");

        int height = height(joinedM3);
        check(height <= 11, "1024-piece join remains logarithmic height=" + height);
    }

    private static Object m3(String value) throws IllegalAccessException {
        value.length();
        return STRING_M3.get(value);
    }

    private static int height(Object m3) throws IllegalAccessException {
        Object owner = M3_OWNER.get(m3);
        if (!M3_TUPLE.isInstance(owner)) return 0;
        Object left = TUPLE_LEFT.get(owner);
        Object right = TUPLE_RIGHT.get(owner);
        return 1 + Math.max(height(left), height(right));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Trace {
        private final int id;
        private final String text;
        private final ArrayList<Integer> order;

        private Trace(int id, String text, ArrayList<Integer> order) {
            this.id = id;
            this.text = text;
            this.order = order;
        }

        @Override
        public String toString() {
            order.add(id);
            return text;
        }
    }
}
