// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Verify TQ range facts, canonical backing reuse and required JNI parity
 * @modules java.base/jdk.internal.mindex
 * @build M3MappedStringBackingTest
 * @run main/native M3TQTest
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TQ;
import jdk.internal.mindex.M3MappedStringBacking;
import jdk.internal.mindex.M3StringBacking;

public class M3TQTest {
    private static long checks;
    private static long ranges;
    private static long compositions;
    private static long nativeCalls;
    private static long coldReads;
    private static long mappedRanges;
    private static final List<String> LITERALS = List.of("", "a", "aba", "bab", "abc",
            "\u0000\u0100\uD800", "\uD800\uDC00a", "\\Qx\\E");
    private static final List<M3TQ> QUERIES = LITERALS.stream()
            .map(value -> M3TQ.fromExact(List.of(value))).toList();
    private static final List<Pattern> PATTERNS = LITERALS.stream()
            .map(value -> Pattern.compile(Pattern.quote(value))).toList();
    private static native long[] nativeKeys(char[] text);
    private static native long[] nativeRangeKeys(char[] text, int from, int to, int budget);

    public static void main(String[] args) throws Exception {
        System.loadLibrary("M3TQTest");
        check(M3TQ.class.getModule().getName().equals("java.base"), "actual java.base owner");
        check(M3TQ.class.getClassLoader() == null, "bootstrap-loaded kernel");
        List<String> corpus = new ArrayList<>();
        enumerate(corpus, "", 3);
        corpus.addAll(List.of("ababa", "abcabc", "\uFFFF\uFFFF\uFFFF", "A\uD800x\uD83D\uDE00\uDC00Z",
                "class C{}", "a.*b", "\\u0061", "\"null\"", "a\nb\rc\u0000", "abababab"));
        for (String text : corpus) {
            char[] units = text.toCharArray(), original = units.clone();
            equal(oracle(text, 0, text.length()), nativeKeys(units), "native whole");
            nativeCalls++;
            for (int from = 0; from <= text.length(); from++) {
                for (int to = from; to <= text.length(); to++) {
                    range(text, units, from, to);
                }
            }
            check(Arrays.equals(original, units), "native preserves input");
        }
        queryAlgebra();
        refusals();
        mapped();
        System.out.printf("TQ checks=%d strings=%d ranges=%d compositions=%d nativeCalls=%d coldReads=%d mappedRanges=%d warmPayloadReads=0%n",
                checks, corpus.size(), ranges, compositions, nativeCalls, coldReads, mappedRanges);
    }

    private static void enumerate(List<String> result, String prefix, int remaining) {
        result.add(prefix);
        if (remaining == 0) return;
        for (char unit : new char[]{'a', 'b', '\u0000', '\u00FF', '\u0100', '\uD800', '\uDC00'}) {
            enumerate(result, prefix + unit, remaining - 1);
        }
    }

    private static void range(String text, char[] units, int from, int to) {
        int length = to - from;
        long[] expected = oracle(text, from, to);
        Fence reader = new Fence(text, from, to);
        M3TQ.Facts facts = M3TQ.precompute(reader, from, to, length);
        check(reader.reads == length, "one cold read per admitted unit");
        coldReads += reader.reads;
        reader.sealed = true;
        equal(expected, facts.keys(), "range facts");
        equal(expected, nativeRangeKeys(units, from, to, length), "native range");
        nativeCalls++;
        check(facts.utf16Length() == length && facts.keyCount() == expected.length, "fact dimensions");
        long[] exposed = facts.keys();
        if (exposed.length != 0) exposed[0] = -1;
        equal(expected, facts.keys(), "defensive export");
        check(!facts.test(-1) && !facts.test(1L << 48), "outside packed-key domain");
        for (long key : expected) check(facts.test(key), "exact membership");
        for (int split = from; split <= to; split++) {
            Fence leftReader = new Fence(text, from, split), rightReader = new Fence(text, split, to);
            var left = M3TQ.precompute(leftReader, from, split, length);
            var right = M3TQ.precompute(rightReader, split, to, length);
            check(leftReader.reads + rightReader.reads == length, "partition cold reads");
            leftReader.sealed = rightReader.sealed = true;
            equal(expected, left.concat(right, length).keys(), "seam composition");
            check(left.concat(right, length).utf16Length() == length, "composed length");
            compositions++;
        }
        for (int p = 0; p < QUERIES.size(); p++) {
            boolean guard = QUERIES.get(p).testPrecomputed(facts);
            boolean match = PATTERNS.get(p).matcher(text).region(from, to).find();
            check(!match || guard, "literal guard cannot reject a true region match");
        }
        ranges++;
    }

    private static void queryAlgebra() {
        var a = M3TQ.fromExact(List.of("abc"));
        var b = M3TQ.fromExact(List.of("bcd"));
        var c = M3TQ.fromExact(List.of("def", "xyz"));
        for (int mask = 0; mask < 16; mask++) {
            final int selected = mask;
            java.util.function.LongPredicate membership = key -> {
                long[] keys = {M3TQ.trigram('a','b','c'), M3TQ.trigram('b','c','d'),
                        M3TQ.trigram('d','e','f'), M3TQ.trigram('x','y','z')};
                for (int i = 0; i < keys.length; i++) if (keys[i] == key) return (selected & (1 << i)) != 0;
                return false;
            };
            check(M3TQ.and(List.of(a,b,c)).test(membership) ==
                    ((mask & 3) == 3 && (mask & 12) != 0), "AND/OR truth table");
            check(M3TQ.or(List.of(a,b)).test(membership) == ((mask & 3) != 0), "OR truth table");
            check(M3TQ.and(List.of(a,M3TQ.none())).test(membership) == false, "NONE absorber");
            check(M3TQ.or(List.of(b,M3TQ.all())).test(membership), "ALL absorber");
        }
        check(M3TQ.fromExact(List.of()).op() == M3TQ.Op.NONE, "empty language");
        check(!M3TQ.fromExact(List.of("abc", "x")).hasConstraints(), "short branch admits all");
        check(!M3TQ.and(List.of()).hasConstraints(), "empty AND");
        check(M3TQ.or(List.of()).op() == M3TQ.Op.NONE, "empty OR");
        check(M3TQ.and(List.of(a,a)).termCount() == 1, "unique flattened keys");
        check(M3TQ.or(List.of(M3TQ.none(),a)).termCount() == 1, "identity flattening");
        long[] export = a.trigrams(); export[0] = -1;
        check(a.trigrams()[0] == M3TQ.trigram('a','b','c'), "query defensive export");
        expect(UnsupportedOperationException.class, () -> a.subqueries().add(b));
        check(a.toString().contains("abc") && M3TQ.none().toString().equals("NONE"), "query rendering");
        var falsePositive = M3TQ.precompute("abc-bcd", 7);
        check(M3TQ.fromExact(List.of("abcd")).testPrecomputed(falsePositive), "deliberate guard positive");
        check(!Pattern.compile("abcd").matcher("abc-bcd").find(), "guard positive is not a match");
    }

    private static void refusals() {
        Fence unread = new Fence("abcd", 0, 4); unread.sealed = true;
        expect(NullPointerException.class, () -> M3TQ.precompute((CharSequence) null, 1));
        expect(IllegalArgumentException.class, () -> M3TQ.precompute(unread, -1));
        expect(IllegalArgumentException.class, () -> M3TQ.precompute(unread, 3));
        expect(IndexOutOfBoundsException.class, () -> M3TQ.precompute(unread, -1, 2, 4));
        expect(IndexOutOfBoundsException.class, () -> M3TQ.precompute(unread, 3, 2, 4));
        expect(IndexOutOfBoundsException.class, () -> M3TQ.precompute(unread, 0, 5, 4));
        expect(IllegalArgumentException.class, () -> M3TQ.precompute(unread, 1, 4, 2));
        check(unread.reads == 0, "refusals precede payload reads");
        var a = M3TQ.precompute("abc", 3);
        expect(NullPointerException.class, () -> a.concat(null, 3));
        expect(IllegalArgumentException.class, () -> a.concat(a, 5));
        expect(IllegalArgumentException.class, () -> M3TQ.precompute("", 0).concat(a, -1));
        expect(NullPointerException.class, () -> M3TQ.all().testPrecomputed(null));
        expect(NullPointerException.class, () -> M3TQ.fromExact(null));
        expect(NullPointerException.class, () -> M3TQ.and(Arrays.asList(M3TQ.all(),null)));
        expect(NullPointerException.class, () -> M3TQ.precompute((M3StringBacking) null, 1, 0, 0, 0));
        expect(NullPointerException.class, () -> nativeKeys(null));
        expect(NullPointerException.class, () -> nativeRangeKeys(null, 0, 0, 0));
        expect(IndexOutOfBoundsException.class, () -> nativeRangeKeys(new char[1], -1, 1, 1));
        expect(IndexOutOfBoundsException.class, () -> nativeRangeKeys(new char[1], 1, 0, 1));
        expect(IndexOutOfBoundsException.class, () -> nativeRangeKeys(new char[1], 0, 2, 1));
        expect(IllegalArgumentException.class, () -> nativeRangeKeys(new char[1], 0, 1, 0));
        expect(IllegalArgumentException.class, () -> nativeRangeKeys(new char[0], 0, 0, -1));
        equal(new long[0], nativeRangeKeys(new char[0], 0, 0, 0), "native empty");
        nativeCalls += 8;
    }

    private static void mapped() throws Exception {
        var writer = M3MappedStringBackingTest.class.getDeclaredMethod("writeFixture", Path.class, String.class);
        writer.setAccessible(true);
        Path directory = Files.createTempDirectory("m3-tq");
        int file = 0;
        for (String text : List.of("", "a", "ab", "abc", "ababa", "A\uD800x\uD83D\uDE00\uDC00Z", "\u0000\u00FF\u0100")) {
            Path path = directory.resolve("text-" + file++ + ".midx");
            writer.invoke(null, path, text);
            try (var owner = M3MappedStringBacking.open(path)) {
                for (long id : new long[]{1, (7L << 32) | 2}) {
                    for (int from = 0; from <= text.length(); from++) {
                        for (int to = from; to <= text.length(); to++) {
                            var reader = new BackingFence(owner, id, from, to);
                            int length = to - from;
                            var facts = M3TQ.precompute(reader, id, from, to, length);
                            check(reader.reads == length, "mapped single scan");
                            int split = from + length / 2;
                            var left = M3TQ.precompute(reader, id, from, split, length);
                            var right = M3TQ.precompute(reader, id, split, to, length);
                            reader.sealed = true;
                            equal(oracle(text, from, to), facts.keys(), "mapped facts");
                            equal(facts.keys(), left.concat(right, length).keys(), "mapped zero-read reuse");
                            expect(IllegalArgumentException.class, () -> M3TQ.precompute(reader, id, reader.from, reader.to, -1));
                            mappedRanges++;
                        }
                    }
                }
            }
        }
    }

    private static long[] oracle(String value, int from, int to) {
        TreeSet<Long> keys = new TreeSet<>();
        for (int i = from; i + 2 < to; i++) {
            keys.add(value.charAt(i) * 4294967296L + value.charAt(i+1) * 65536L + value.charAt(i+2));
        }
        return keys.stream().mapToLong(Long::longValue).toArray();
    }

    private static final class Fence implements CharSequence {
        final String value; final int from, to; int reads; boolean sealed;
        Fence(String value, int from, int to) { this.value=value; this.from=from; this.to=to; }
        public int length() { return value.length(); }
        public char charAt(int index) {
            check(!sealed && index >= from && index < to, "payload read outside admitted cold scan");
            reads++; return value.charAt(index);
        }
        public CharSequence subSequence(int start, int end) { throw new AssertionError("no slicing"); }
        public String toString() { throw new AssertionError("no materialization"); }
    }

    private static final class BackingFence implements M3StringBacking {
        final M3StringBacking delegate; final long id; final int from,to;
        int reads; boolean sealed;
        BackingFence(M3StringBacking delegate, long id, int from, int to) {
            this.delegate=delegate; this.id=id; this.from=from; this.to=to;
        }
        public int length(long key) { check(key==id,"owner id"); return delegate.length(key); }
        public char charAt(long key, int index) {
            check(key==id && !sealed && index>=from && index<to,"backing payload fence");
            reads++; return delegate.charAt(key,index);
        }
        public void close() { throw new AssertionError("precompute cannot close owner"); }
        public int utf8Length(long key) { throw new AssertionError("no encoding"); }
        public int codePointCount(long key) { throw new AssertionError("exact code units"); }
        public int unpairedSurrogateCount(long key) { throw new AssertionError("no source facts assumed"); }
        public int hashCode(long key) { throw new AssertionError("no hash identity"); }
        public CharBuffer utf16View(long key) { throw new AssertionError("no view export"); }
        public ByteBuffer utf8View(long key) { throw new AssertionError("no encoding"); }
        public String materialize(long key) { throw new AssertionError("no payload copy"); }
    }

    private static void equal(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual), label);
    }
    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try { operation.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) { checks++; return; }
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
