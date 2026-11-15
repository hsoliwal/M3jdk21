/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Exact trigram preparation reads UTF-16 once and preserves immutable query ownership
 * @modules java.base/jdk.internal.mindex:+open
 * @run main/othervm M3TQRollingPreparationTest
 * @run main/othervm -XX:-CompactStrings M3TQRollingPreparationTest
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.LongPredicate;
import jdk.internal.mindex.M3TQ;

public final class M3TQRollingPreparationTest {
    private static final Method UNIQUE;
    private static MessageDigest transcript;
    private static boolean incumbent;
    private static long checks;
    private static long preparations;
    private static long unitReads;

    static {
        try {
            UNIQUE = M3TQ.class.getDeclaredMethod("uniqueTrigrams", CharSequence.class);
            UNIQUE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private M3TQRollingPreparationTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " at check " + checks);
    }

    private static void number(long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            transcript.update((byte) (value >>> shift));
        }
    }

    private static void equal(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
        number(actual.length);
        for (long key : actual) number(key);
    }

    private static void refusal(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(failure.getClass() == type, "exact exception " + type.getName());
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static long[] prepare(CharSequence value) {
        try {
            return (long[]) UNIQUE.invoke(null, value);
        } catch (InvocationTargetException wrapped) {
            Throwable cause = wrapped.getCause();
            if (cause instanceof RuntimeException failure) throw failure;
            if (cause instanceof Error failure) throw failure;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static long[] oracle(char[] units, int from, int to) {
        TreeSet<Long> keys = new TreeSet<>();
        for (int index = from; index + 2 < to; index++) {
            // Arithmetic packing deliberately avoids the rolling implementation.
            keys.add(units[index] * 4294967296L
                    + units[index + 1] * 65536L + units[index + 2]);
        }
        return keys.stream().mapToLong(Long::longValue).toArray();
    }

    /** Stable diagnostic view of the internal preparation atom, never a production API. */
    private static final class Counted implements CharSequence {
        private final char[] units;
        private int reads;

        Counted(char[] units) { this.units = units; }

        @Override public int length() { return units.length; }

        @Override public char charAt(int index) {
            int expected = incumbent ? reads / 3 + reads % 3 : reads;
            check(index == expected, "preparation read order");
            reads++;
            return units[index];
        }

        @Override public CharSequence subSequence(int from, int to) {
            throw new AssertionError("preparation must not slice its input");
        }

        @Override public String toString() {
            throw new AssertionError("preparation must not materialize its input");
        }
    }

    private static void preparation(char[] units) {
        Counted input = new Counted(units);
        long[] expected = oracle(units, 0, units.length);
        long[] actual = prepare(input);
        equal(expected, actual, "independent exact trigram set");
        int wantedReads = units.length < 3 ? 0
                : incumbent ? 3 * (units.length - 2) : units.length;
        check(input.reads == wantedReads, "exact diagnostic read count");
        unitReads += input.reads;
        preparations++;
        for (int index = 0; index < actual.length; index++) {
            check(actual[index] >= 0 && actual[index] < (1L << 48), "48-bit key");
            if (index != 0) check(actual[index - 1] < actual[index], "sorted unique key plane");
        }
        String spelling = new String(units);
        M3TQ query = M3TQ.fromExact(List.of(spelling));
        check(query.test(key -> Arrays.binarySearch(expected, key) >= 0), "own facts satisfy query");
        check(query.test(key -> false) == (units.length < 3), "empty membership condition");
        if (units.length >= 3) equal(expected, query.trigrams(), "public exact-query keys");
        else check(query == M3TQ.all(), "short exact branch admits all");
        number(query.op().ordinal());
        number(query.termCount());
    }

    private static void enumerate(char[] alphabet, char[] units, int position) {
        if (position == units.length) {
            preparation(units);
            return;
        }
        for (char unit : alphabet) {
            units[position] = unit;
            enumerate(alphabet, units, position + 1);
        }
    }

    private static void corpus() {
        char[] alphabet = {'a', 'b', 0, '\u00ff', '\u0100', '\ud800', '\udc00', '\uffff'};
        for (int length = 0; length <= 4; length++) {
            enumerate(alphabet, new char[length], 0);
        }
        for (String text : List.of("abcabcabc", "aaaaaaaaaa", "ababababab",
                "a\ud83d\ude00b\ud800c\udc00d", "\uffff\0\uffff\0\uffff")) {
            preparation(text.toCharArray());
        }
        Random random = new Random(0x4d335451524f4c4cL);
        for (int trial = 0; trial < 1200; trial++) {
            char[] units = new char[random.nextInt(257)];
            for (int index = 0; index < units.length; index++) {
                units[index] = (char) (trial % 3 == 0 ? random.nextInt(4) : random.nextInt(65536));
            }
            preparation(units);
        }
        for (int length : new int[] {3, 4, 31, 32, 33, 8191, 8192, 8193, 65536}) {
            char[] units = new char[length];
            for (int index = 0; index < length; index++) units[index] = (char) index;
            preparation(units);
        }
    }

    private static String observation(M3TQ query) {
        return query.op() + ":" + Arrays.toString(query.trigrams()) + ":"
                + query.termCount() + ":" + query.hasScanPlan() + ":" + query;
    }

    private static void ownershipAndComposition() {
        var alternatives = new ArrayList<>(List.of("abcde", "abcab", "\ud800x\udc00"));
        M3TQ original = M3TQ.fromExact(alternatives);
        String frozen = observation(original);
        alternatives.clear();
        alternatives.add("zzz");
        check(observation(original).equals(frozen), "caller list is not retained");
        refusal(UnsupportedOperationException.class,
                () -> original.subqueries().add(M3TQ.all()));
        var leaves = new ArrayList<M3TQ>();
        for (String text : List.of("aaa", "abcd", "bcda", "cdef", "\0\0\0", "\uffffab")) {
            leaves.add(M3TQ.fromExact(List.of(text)));
        }
        String[] before = leaves.stream().map(M3TQRollingPreparationTest::observation)
                .toArray(String[]::new);
        for (int repeat = 0; repeat < 40; repeat++) {
            var input = new ArrayList<>(leaves);
            input.add(leaves.get(repeat % leaves.size()));
            M3TQ conjunction = M3TQ.and(input);
            M3TQ disjunction = M3TQ.or(input);
            String andBefore = observation(conjunction);
            String orBefore = observation(disjunction);
            input.clear();
            Arrays.fill(conjunction.trigrams(), -1L);
            Arrays.fill(disjunction.trigrams(), -1L);
            M3TQ.and(List.of(conjunction, original, conjunction));
            M3TQ.or(List.of(disjunction, original, disjunction));
            check(observation(conjunction).equals(andBefore), "AND exported array and reuse isolation");
            check(observation(disjunction).equals(orBefore), "OR exported array and reuse isolation");
            for (int index = 0; index < leaves.size(); index++) {
                Arrays.fill(leaves.get(index).trigrams(), -1L);
                check(observation(leaves.get(index)).equals(before[index]), "operand key ownership");
            }
            check(observation(original).equals(frozen), "compound operand immutability");
        }
    }

    private static void queryParity() {
        M3TQ a = M3TQ.fromExact(List.of("abcde", "bcdef"));
        M3TQ b = M3TQ.fromExact(List.of("xyzxy", "\0\ud800\udc00"));
        M3TQ c = M3TQ.fromExact(List.of("aaa"));
        List<M3TQ> queries = List.of(M3TQ.all(), M3TQ.none(), a, b, c,
                M3TQ.and(List.of(a, b)), M3TQ.or(List.of(a, b)),
                M3TQ.and(List.of(M3TQ.or(List.of(a, c)), b)));
        List<String> subjects = List.of("", "ab", "abcde", "abcdefxyzxy", "abc-bcd-cde",
                "aaa\0\ud800\udc00", "xxabcdeyyxyzxyzz", "\uffff\udc00\ud800\0abcde");
        for (String subject : subjects) {
            char[] units = subject.toCharArray();
            for (int from = 0; from <= units.length; from++) {
                for (int to = from; to <= units.length; to++) {
                    long[] keys = oracle(units, from, to);
                    LongPredicate membership = key -> Arrays.binarySearch(keys, key) >= 0;
                    M3TQ.Facts facts = M3TQ.precompute(subject, from, to, to - from);
                    equal(keys, facts.keys(), "unchanged facts path");
                    for (M3TQ query : queries) {
                        boolean expected = query.test(membership);
                        check(query.testPrecomputed(facts) == expected, "prepared query parity");
                        if (query.hasScanPlan()) {
                            check(query.testScanning(subject, from, to, to - from) == expected,
                                    "String bounded scan parity");
                            check(query.testScanning(new StringBuilder(subject), from, to, to - from) == expected,
                                    "generic bounded scan parity");
                        }
                        number(expected ? 1 : 0);
                    }
                }
            }
        }
        // A positive trigram condition deliberately cannot establish order or multiplicity.
        check(M3TQ.fromExact(List.of("abcd")).testPrecomputed(M3TQ.precompute("abc-bcd", 7)),
                "necessary-condition false positive is preserved");
        check(!"abc-bcd".contains("abcd"), "exact verifier still required");
    }

    private static void boundaries() {
        refusal(NullPointerException.class, () -> prepare(null));
        refusal(NullPointerException.class, () -> M3TQ.fromExact(null));
        refusal(NullPointerException.class, () -> M3TQ.fromExact(Arrays.asList("abc", null)));
        check(M3TQ.fromExact(Arrays.asList("a", null)) == M3TQ.all(), "short-circuit before null tail");
        check(M3TQ.fromExact(List.of()) == M3TQ.none(), "empty alternatives");
        check(M3TQ.and(List.of()) == M3TQ.all(), "empty AND");
        check(M3TQ.or(List.of()) == M3TQ.none(), "empty OR");
        M3TQ exact = M3TQ.fromExact(List.of("abcd"));
        refusal(NullPointerException.class, () -> exact.testScanning(null, -1, 0, -1));
        refusal(IndexOutOfBoundsException.class, () -> exact.testScanning("abcd", -1, 4, -1));
        refusal(IllegalArgumentException.class, () -> exact.testScanning("abcd", 0, 4, 3));
        refusal(IllegalArgumentException.class, () -> exact.testScanning("abcd", 0, 0, -1));
        for (int keyCount : new int[] {1, 2, 31, 32, 33}) {
            var terms = new ArrayList<M3TQ>();
            for (int index = 0; index < keyCount; index++) {
                terms.add(M3TQ.fromExact(List.of(new String(new char[] {(char) index, 'x', 'y'}))));
            }
            M3TQ query = M3TQ.and(terms);
            check(query.termCount() == keyCount, "key boundary");
            check(query.hasScanPlan() == (keyCount <= 32), "unchanged streaming bound");
            number(query.termCount());
            number(query.hasScanPlan() ? 1 : 0);
        }
    }

    public static void main(String[] args) throws Exception {
        // The private recipe proof also runs this exact test against the incumbent owner.
        incumbent = args.length == 1 && args[0].equals("incumbent");
        transcript = MessageDigest.getInstance("SHA-256");
        corpus();
        ownershipAndComposition();
        queryParity();
        boundaries();
        System.out.println("TQ_ROLLING_PREPARATION_PASS checks=" + checks
                + " preparations=" + preparations + " unitReads=" + unitReads
                + " mode=" + (incumbent ? "incumbent" : "rolling")
                + " transcript=" + HexFormat.of().formatHex(transcript.digest()));
    }
}
