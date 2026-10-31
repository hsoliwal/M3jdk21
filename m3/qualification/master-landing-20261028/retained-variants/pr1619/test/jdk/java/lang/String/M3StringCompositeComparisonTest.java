/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Composite String comparisons preserve UTF-16, callback and empty-region contracts
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCompositeComparisonTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCompositeComparisonTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xint M3StringCompositeComparisonTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public final class M3StringCompositeComparisonTest {
    private static long checks;
    private static final Field BACKING;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static final Method FACTS;
    private static final ThreadLocal<Boolean> ADMISSION_GUARD;

    static {
        try {
            Class<?> type = Class.forName("java.lang.M3String");
            BACKING = String.class.getDeclaredField("m3");
            OWNER = type.getDeclaredField("owner");
            COORDINATE = type.getDeclaredField("value");
            FACTS = type.getDeclaredMethod("facts");
            BACKING.setAccessible(true);
            OWNER.setAccessible(true);
            COORDINATE.setAccessible(true);
            FACTS.setAccessible(true);
            ADMISSION_GUARD = admissionGuard(type);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<Boolean> admissionGuard(Class<?> type)
            throws ReflectiveOperationException {
        Field field = type.getDeclaredField("ADMITTING");
        field.setAccessible(true);
        return (ThreadLocal<Boolean>) field.get(null);
    }

    private static String flat(char[] units) {
        Boolean previous = ADMISSION_GUARD.get();
        ADMISSION_GUARD.set(Boolean.TRUE);
        try {
            return new String(units);
        } finally {
            if (previous == null) ADMISSION_GUARD.remove();
            else ADMISSION_GUARD.set(previous);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static int compare(char[] left, char[] right) {
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            if (left[i] != right[i]) return left[i] - right[i];
        }
        return left.length - right.length;
    }

    private static boolean prefix(char[] source, char[] sought, int offset) {
        if (offset < 0 || offset > source.length - sought.length) return false;
        for (int i = 0; i < sought.length; i++) {
            if (source[offset + i] != sought[i]) return false;
        }
        return true;
    }

    private static String composite(char[] source, int pieces) {
        String result = "";
        int from = 0;
        for (int i = 1; i <= pieces; i++) {
            int to = (int) ((long) i * source.length / pieces);
            if (to > from) result = result.concat(new String(source, from, to - from));
            from = to;
        }
        return result;
    }

    private static Object body(String source) throws IllegalAccessException {
        source.length();
        return BACKING.get(source);
    }

    private static void parity(char[] left, char[] right, int pieces) throws Exception {
        String source = composite(left, pieces);
        String target = composite(right, pieces + 1);
        String flatTarget = flat(right);
        check(body(flatTarget) == null, "fixture retains a flat comparison side");
        int expected = compare(left, right);
        check(source.compareTo(target) == expected, "composite compareTo exact difference");
        check(target.compareTo(source) == -expected, "reverse compareTo exact difference");
        check(source.compareTo(flatTarget) == expected, "flat-right compareTo");
        check(flatTarget.compareTo(source) == -expected, "flat-left compareTo");
        check(source.equals(target) == (expected == 0), "composite equals");
        check(source.equals(flatTarget) == (expected == 0), "flat-right equals");
        check(flatTarget.equals(source) == (expected == 0), "flat-left equals");
        check(source.contentEquals(target) == (expected == 0), "String CharSequence");
        check(source.contentEquals(new StringBuilder(flatTarget)) == (expected == 0),
                "StringBuilder contentEquals");
        check(source.contentEquals(new StringBuffer(flatTarget)) == (expected == 0),
                "StringBuffer contentEquals");
        OrderedSequence sequence = new OrderedSequence(right, -1);
        check(source.contentEquals(sequence) == (expected == 0), "generic CharSequence");
        check(sequence.lengthCalls == 1, "generic length is read once");
        int visited = 0;
        if (left.length == right.length) {
            while (visited < left.length) {
                int at = visited++;
                if (left[at] != right[at]) break;
            }
        }
        check(sequence.reads == visited, "generic callbacks stop at the first mismatch");
        Object backing = body(source);
        Object owner = backing == null ? null : OWNER.get(backing);
        long coordinate = backing == null ? 0 : COORDINATE.getLong(backing);
        for (int offset : new int[] {-1, 0, 1, left.length / 2,
                left.length, Integer.MAX_VALUE}) {
            check(source.startsWith(target, offset) == prefix(left, right, offset),
                    "composite prefix bounds and content");
            check(source.startsWith(flatTarget, offset) == prefix(left, right, offset),
                    "flat prefix bounds and content");
        }
        if (backing != null) {
            FACTS.invoke(backing);
            check(source.equals(target) == (expected == 0), "prepared-facts equals");
            check(source.startsWith(target) == prefix(left, right, 0), "prepared-facts prefix");
            check(OWNER.get(body(source)) == owner
                            && COORDINATE.getLong(body(source)) == coordinate,
                    "comparison retains canonical owner and coordinate");
        }
    }

    private static final class Sentinel extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private static final class OrderedSequence implements CharSequence {
        private final char[] units;
        private final int throwAt;
        private final Sentinel sentinel = new Sentinel();
        private int reads;
        private int lengthCalls;

        OrderedSequence(char[] units, int throwAt) {
            this.units = units;
            this.throwAt = throwAt;
        }

        @Override public int length() { lengthCalls++; return units.length; }

        @Override public char charAt(int index) {
            check(index == reads, "callbacks run once in increasing index order");
            reads++;
            if (index == throwAt) throw sentinel;
            return units[index];
        }

        @Override public CharSequence subSequence(int start, int end) {
            throw new AssertionError("comparison must not request a subsequence");
        }

        @Override public String toString() {
            throw new AssertionError("comparison must not flatten a generic sequence");
        }
    }

    private static void callbacks() {
        char[] units = new char[257];
        Arrays.fill(units, 'x');
        String source = composite(units, 37);
        for (int at : new int[] {0, 1, 63, 64, 65, 128, 255, 256}) {
            OrderedSequence sequence = new OrderedSequence(units, at);
            try {
                source.contentEquals(sequence);
                throw new AssertionError("callback failure must propagate");
            } catch (Sentinel expected) {
                check(expected == sequence.sentinel, "original callback exception identity");
                check(sequence.lengthCalls == 1 && sequence.reads == at + 1,
                        "no callback retry or read after failure");
            }
        }
    }

    private static void regions() {
        String left = composite("aBcDeFgH".repeat(16).toCharArray(), 17);
        String right = composite("AbCdEfGh".repeat(16).toCharArray(), 19);
        int[] offsets = {-1, 0, 1, 63, 128, 129, Integer.MAX_VALUE};
        int[] lengths = {Integer.MIN_VALUE, -129, -1, 0};
        for (int from : offsets) for (int other : offsets) for (int length : lengths) {
            boolean expected = other >= 0 && from >= 0
                    && from <= (long) left.length() - length
                    && other <= (long) right.length() - length;
            check(left.regionMatches(from, right, other, length) == expected,
                    "zero/negative exact-region length after ordered bounds");
            check(left.regionMatches(false, from, right, other, length) == expected,
                    "delegating zero/negative exact-region length");
            check(left.regionMatches(true, from, right, other, length) == expected,
                    "zero/negative ignore-case-region length after ordered bounds");
            boolean shortCircuit = other < 0 || from < 0
                    || from > (long) left.length() - length;
            for (boolean ignoreCase : new boolean[] {false, true}) {
                try {
                    boolean actual = left.regionMatches(ignoreCase, from, null, other, length);
                    check(shortCircuit && !actual, "invalid prior bounds precede null access");
                } catch (NullPointerException expectedNull) {
                    check(!shortCircuit, "null is observed after prior valid bounds");
                }
            }
        }
        check(left.regionMatches(true, 0, right, 0, left.length()), "positive folded region");
        check(!left.regionMatches(0, right, 0, left.length()), "positive exact region");
    }

    private static void directed() throws Exception {
        String witness = composite("aBcDeFgH".repeat(128).toCharArray(), 37);
        Object backing = body(witness);
        check(backing != null && OWNER.get(backing).getClass().getName()
                        .equals("java.lang.M3StringTuple"),
                "fixture executes the canonical M3 composite owner");
        for (int length : new int[] {0, 1, 2, 7, 31, 63, 64, 65, 127, 255, 256, 257, 1024}) {
            char[] units = new char[length];
            for (int i = 0; i < length; i++) {
                units[i] = switch (i % 10) {
                    case 0 -> '\0';
                    case 1 -> '\u00ff';
                    case 2 -> '\u0100';
                    case 3 -> '\ud800';
                    case 4 -> '\udc00';
                    case 5 -> '\uffff';
                    default -> (char) ('a' + i % 26);
                };
            }
            for (int pieces : new int[] {1, 2, 7, 67}) {
                parity(units, units.clone(), pieces);
                if (length != 0) {
                    for (int at : new int[] {0, length / 2, length - 1}) {
                        char[] miss = units.clone();
                        miss[at] ^= 1;
                        parity(units, miss, pieces);
                    }
                }
                parity(units, Arrays.copyOf(units, length + 1), pieces);
            }
        }
        // Equal Java hashes do not establish text equality.
        parity("Aa".repeat(256).toCharArray(), "BB".repeat(256).toCharArray(), 61);
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33434f4d504152L);
        for (int trial = 0; trial < 200; trial++) {
            char[] units = new char[64 + random.nextInt(1024)];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            char[] other = units.clone();
            if ((trial & 1) != 0) other[random.nextInt(other.length)] ^= 1;
            parity(units, other, 2 + random.nextInt(64));
        }
    }

    public static void main(String[] arguments) throws Exception {
        regions();
        directed();
        callbacks();
        randomized();
        System.out.println("M3_STRING_COMPOSITE_COMPARISON_PASS checks=" + checks);
    }
}
