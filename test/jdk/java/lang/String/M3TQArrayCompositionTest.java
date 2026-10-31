/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Primitive trigram query composition and shared composite fact metadata
 * @modules java.base/jdk.internal.mindex
 * @run main/native M3TQArrayCompositionTest native
 */
import java.security.MessageDigest;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.LongPredicate;
import jdk.internal.mindex.M3TQ;

/** Exact public-observation transcript plus independent set/Boolean and JNI oracles. */
public final class M3TQArrayCompositionTest {
    private static long checks;
    private static MessageDigest transcript;
    private static boolean nativeEnabled;
    private M3TQArrayCompositionTest() { }
    private static native long[] nativeKeys(char[] text, int from, int to, int budget);

    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("check " + checks);
    }
    private static void number(long n) {
        for (int shift = 56; shift >= 0; shift -= 8) transcript.update((byte) (n >>> shift));
    }
    private static void text(String s) {
        number(s.length());
        for (int i = 0; i < s.length(); i++) number(s.charAt(i));
    }
    private static void equal(long[] expected, long[] actual) {
        check(Arrays.equals(expected, actual));
        number(actual.length);
        for (long key : actual) number(key);
    }
    private static void refusal(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable thrown) {
            check(thrown.getClass() == type);
            text(type.getName());
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }
    private static long[] oracle(char[] units, int from, int to) {
        TreeSet<Long> keys = new TreeSet<>();
        for (int i = from; i + 2 < to; i++) {
            // Arithmetic packing, deliberately independent of the runtime's shift helper.
            keys.add(units[i] * 4294967296L + units[i + 1] * 65536L + units[i + 2]);
        }
        return keys.stream().mapToLong(Long::longValue).toArray();
    }
    private static final class Pieces implements CharSequence {
        final char[][] pieces;
        final int[] ends;
        final int length;
        int reads;
        int expectedNext;
        boolean closed;
        Pieces(char[] value, int firstSplit, int secondSplit) {
            pieces = new char[][] { Arrays.copyOfRange(value, 0, firstSplit),
                    Arrays.copyOfRange(value, firstSplit, secondSplit),
                    Arrays.copyOfRange(value, secondSplit, value.length) };
            ends = new int[] {firstSplit, secondSplit, value.length};
            length = value.length;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) {
            if (closed || index != expectedNext++) throw new AssertionError("nonsequential/payload reread");
            reads++;
            int part = 0;
            while (index >= ends[part]) part++;
            return pieces[part][index - (part == 0 ? 0 : ends[part - 1])];
        }
        @Override public CharSequence subSequence(int from, int to) {
            throw new AssertionError("unexpected source slicing");
        }
        @Override public String toString() { throw new AssertionError("unexpected flattening"); }
    }
    private static void facts(char[] value) {
        long[] wanted = oracle(value, 0, value.length);
        if (nativeEnabled) equal(wanted, nativeKeys(value, 0, value.length, value.length));
        for (int cut = 0; cut <= value.length; cut++) {
            Pieces pieces = new Pieces(value, cut, cut);
            M3TQ.Facts whole = M3TQ.precompute(pieces, value.length);
            check(pieces.reads == value.length);
            pieces.closed = true;
            equal(wanted, whole.keys());
            M3TQ.Facts left = M3TQ.precompute(new String(value), 0, cut, value.length);
            M3TQ.Facts right = M3TQ.precompute(new String(value), cut, value.length, value.length);
            M3TQ.Facts joined = left.concat(right, value.length);
            check(joined.utf16Length() == value.length);
            equal(wanted, joined.keys());
            check(whole.containsAll(joined) && joined.containsAll(whole));
            for (long key : wanted) check(joined.test(key));
            long[] poison = joined.keys();
            Arrays.fill(poison, -1L);
            equal(wanted, joined.keys());
            // Future concatenations check both boundary summaries, not just immediate key sets.
            M3TQ.Facts doubled = joined.concat(joined, value.length * 2);
            char[] twice = new char[value.length * 2];
            System.arraycopy(value, 0, twice, 0, value.length);
            System.arraycopy(value, 0, twice, value.length, value.length);
            equal(oracle(twice, 0, twice.length), doubled.keys());
        }
        for (int from = 0; from <= value.length; from++) {
            int to = from + (value.length - from) / 2;
            Pieces range = new Pieces(value, from, to);
            range.expectedNext = from;
            var result = M3TQ.precompute(range, from, to, to - from);
            check(range.reads == to - from);
            range.closed = true;
            equal(oracle(value, from, to), result.keys());
            if (nativeEnabled) equal(result.keys(), nativeKeys(value, from, to, to - from));
        }
    }
    private static void compositeCorpus() {
        for (String s : List.of("", "a", "aa", "aaa", "ab", "aba", "abcabc", "aaaaabaaaaa",
                "\0\0\0", "\uffff\uffff\uffff", "\ud800a\udc00", "a\ud83d\ude00b",
                "\ud83d\ude00\ud83d\ude00", "\u0100\u0000\u0100\u0000", "customerId = 42;")) {
            facts(s.toCharArray());
        }
        Random random = new Random(0x4d33545120261021L);
        for (int i = 0; i < 350; i++) {
            char[] value = new char[random.nextInt(65)];
            for (int j = 0; j < value.length; j++) {
                value[j] = (char) (i % 3 == 0 ? random.nextInt(3) : random.nextInt(65536));
            }
            facts(value);
        }
        var empty = M3TQ.precompute("", 0);
        var repeated = M3TQ.precompute("a", 1);
        check(empty.concat(repeated, 1) == repeated);
        check(repeated.concat(empty, 1) == repeated);
        for (int n = 0; n < 30; n++) {
            var previous = repeated;
            repeated = repeated.concat(repeated, Integer.MAX_VALUE);
            check(repeated != previous);
            check(repeated.utf16Length() == 1 << (n + 1));
            equal(n == 0 ? new long[0] : new long[]{M3TQ.trigram('a','a','a')}, repeated.keys());
        }
        var limit = repeated;
        refusal(ArithmeticException.class, () -> limit.concat(limit, Integer.MAX_VALUE));
        refusal(IllegalArgumentException.class, () -> limit.concat(empty, 0));
        refusal(NullPointerException.class, () -> limit.concat(null, -1));
        refusal(IllegalArgumentException.class, () -> empty.concat(empty, -1));
        Pieces sealed = new Pieces(new char[]{'x'}, 0, 0);
        refusal(IllegalArgumentException.class, () -> M3TQ.precompute(sealed, 0));
        refusal(IndexOutOfBoundsException.class, () -> M3TQ.precompute(sealed, -1, 1, -1));
        check(sealed.reads == 0);
        if (nativeEnabled) {
            refusal(NullPointerException.class, () -> nativeKeys(null, -1, 0, -1));
            refusal(IndexOutOfBoundsException.class, () -> nativeKeys(new char[1], -1, 0, -1));
            refusal(IllegalArgumentException.class, () -> nativeKeys(new char[1], 0, 1, 0));
        }
    }
    private record Expression(M3TQ query, java.util.function.Predicate<LongPredicate> truth) { }
    private static Expression literal(String spelling) {
        long[] grams = oracle(spelling.toCharArray(), 0, spelling.length());
        return new Expression(M3TQ.fromExact(List.of(spelling)), p -> {
            for (long key : grams) if (!p.test(key)) return false;
            return true;
        });
    }
    private static void queryCorpus() {
        Random r = new Random(0x51434f4d504f5345L);
        List<Expression> pool = new ArrayList<>();
        pool.add(new Expression(M3TQ.all(), p -> true));
        pool.add(new Expression(M3TQ.none(), p -> false));
        for (String s : List.of("aaa", "abc", "bca", "cab", "aaaa", "abcd", "\0\0\0", "\uffffab")) pool.add(literal(s));
        for (int round = 0; round < 2200; round++) {
            boolean conjunction = r.nextBoolean();
            int count = r.nextInt(13);
            List<Expression> terms = new ArrayList<>();
            for (int i = 0; i < count; i++) terms.add(pool.get(r.nextInt(pool.size())));
            List<M3TQ> queries = terms.stream().map(Expression::query).toList();
            M3TQ query = conjunction ? M3TQ.and(queries) : M3TQ.or(queries);
            Expression value = new Expression(query, p -> {
                for (var t : terms) {
                    boolean ok = t.truth.test(p);
                    if (conjunction ? !ok : ok) return !conjunction;
                }
                return conjunction;
            });
            text(query.toString()); number(query.op().ordinal()); number(query.termCount());
            equal(query.trigrams(), query.trigrams());
            for (int sample = 0; sample < 6; sample++) {
                final long salt = r.nextLong();
                LongPredicate membership = key -> ((key ^ salt) & 3) != 0;
                List<Long> callbacks = new ArrayList<>();
                boolean actual = query.test(key -> { callbacks.add(key); return membership.test(key); });
                check(actual == value.truth.test(membership));
                number(actual ? 1 : 0); number(callbacks.size());
                for (long key : callbacks) number(key);
            }
            long[] exported = query.trigrams();
            Arrays.fill(exported, -1);
            text(query.toString());
            if (query.termCount() < 70 && round % 3 == 0) {
                if (pool.size() == 35) pool.remove(10);
                pool.add(value);
            }
        }
        // Large primitive staging, duplicates and zero-valued keys.
        List<M3TQ> many = new ArrayList<>();
        for (int i = 0; i < 4096; i++) many.add(literal(new String(new char[]{(char)i, 0, 'x'})).query);
        check(M3TQ.or(many).termCount() == 4096);
        check(M3TQ.and(many).termCount() == 4096);
        check(M3TQ.or(List.of(literal("\0\0\0").query, literal("\0\0\0").query)).termCount() == 1);
        refusal(NullPointerException.class, () -> M3TQ.and(null));
        refusal(NullPointerException.class, () -> M3TQ.or(Arrays.asList(literal("abc").query, null)));
        check(M3TQ.or(Arrays.asList(M3TQ.all(), null)) == M3TQ.all());
        check(M3TQ.and(Arrays.asList(M3TQ.none(), null)) == M3TQ.none());
        // No speculative second traversal, size(), or tail access on the supplied list.
        List<M3TQ> oneShot = new AbstractList<>() {
            int iterators;
            @Override public int size() { throw new AssertionError("queried input size"); }
            @Override public M3TQ get(int index) { throw new AssertionError("random list access"); }
            @Override public Iterator<M3TQ> iterator() {
                check(++iterators == 1);
                return new Iterator<>() {
                    int next;
                    @Override public boolean hasNext() { check(next == 0); return true; }
                    @Override public M3TQ next() {
                        if (next++ != 0) throw new NoSuchElementException();
                        return M3TQ.all();
                    }
                };
            }
        };
        check(M3TQ.or(oneShot) == M3TQ.all());
    }
    public static void main(String[] args) throws Exception {
        transcript = MessageDigest.getInstance("SHA-256");
        nativeEnabled = args.length != 0 && args[0].equals("native");
        if (nativeEnabled) System.loadLibrary("M3TQTest");
        compositeCorpus(); queryCorpus();
        System.out.println("TQ_ARRAY_COMPOSITION_PASS checks=" + checks
                + " native=" + nativeEnabled + " transcript=" + HexFormat.of().formatHex(transcript.digest()));
    }
}
