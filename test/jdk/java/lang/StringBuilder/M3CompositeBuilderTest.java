// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/*
 * @test
 * @summary R5-BUILDER composite StringBuilder/StringBuffer lane: appended M3 Strings stay pieces,
 *          every reader/editor sees exactly the stock builder (content, length, capacity,
 *          exceptions, streams, compareTo, serialization), and flag-on toString returns a String
 *          whose owner is the composed M3 tuple of the appended pieces (no flatten)
 * @modules java.base/java.lang:+open
 * @run main M3CompositeBuilderTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CompositeBuilderTest flagOn
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Differential test of the composite builder lane against an independent stock oracle.
 *
 * <p>The subject receives pieces through the real lift path when M3 storage is on, and through the
 * package-private {@code AbstractStringBuilder.m3AppendPiece} entry (reflection) in both modes, so
 * the lane logic is exercised flag-off too. The oracle is a {@code StringBuilder} fed only through
 * paths that never lift ({@code char[]} appends, inserts), so it stays a stock flat builder.</p>
 *
 * <p>Deterministic scenario set replayed from apple/swift-collections (Apache-2.0, scenario donor
 * only, no code copied; fork hsoliwal/swift-collections at b905c889ff75; map
 * R2-MEASURED-ROPE/APPLE_TEST_MAP.tsv):
 * R02 TestRope.swift:134 test_build, R09 :286 test_append_item, R10 :305 test_prepend_item,
 * R11 :325 test_insert_item, R12 :351 test_remove_at_index, R13 :376 test_remove_at_inout_index,
 * R14 :404 test_remove_at_position, R15 :429 test_join, R16 :456 test_join_copy_on_write,
 * R17 :541 test_removeSubrange_simple, R18 :597 test_removeSubrange_larger,
 * R19 :622 test_removeSubrange_random, R20 :643 test_replaceSubrange_simple
 * (TestRope.swift blob 95bb4b992ad1); B21 TestBigString.swift:499 test_append_string,
 * B22 :525 test_append_big, B23 :567 test_append_copy_on_write, B24 :588 test_insert_string,
 * B25 :632 test_insert_big, B26 :680 test_replaceSubrange_string,
 * B37 :1010 testSubstringMutationIndexRounding, B44 :1131 unaligned UTF-16 indices
 * (TestBigString.swift blob e2135faed7c3); S01 SampleStrings.swift:32 multilingual sample
 * (blob 563fb8f87adc); F01 CRLF split across pieces, F02 surrogate pair split across pieces,
 * F03 large random edit scripts vs the stock oracle (R2 additions).</p>
 */
public class M3CompositeBuilderTest {
    private static final Method PIECE;
    private static final Field LANE;
    private static final Method STRING_M3;
    private static long checks;
    private static boolean flagOn;

    static {
        try {
            Class<?> asb = Class.forName("java.lang.AbstractStringBuilder");
            PIECE = asb.getDeclaredMethod("m3AppendPiece", String.class, int.class, int.class);
            PIECE.setAccessible(true);
            LANE = asb.getDeclaredField("m3Lane");
            LANE.setAccessible(true);
            STRING_M3 = String.class.getDeclaredMethod("m3");
            STRING_M3.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** One surface for StringBuilder and StringBuffer subjects (no public common supertype). */
    interface Sb {
        Object raw();
        Sb append(String s);
        Sb append(CharSequence s, int start, int end);
        Sb append(char c);
        Sb append(int i);
        Sb append(char[] c);
        Sb appendRaw(Object other);
        Sb insert(int offset, String s);
        Sb insert(int offset, char c);
        Sb delete(int start, int end);
        Sb deleteCharAt(int index);
        Sb replace(int start, int end, String s);
        Sb reverse();
        Sb repeat(CharSequence cs, int count);
        void setCharAt(int index, char c);
        void setLength(int length);
        void ensureCapacity(int capacity);
        void trimToSize();
        int capacity();
        CharSequence cs();
        int compareToRaw(Object other);
    }

    record Builder(StringBuilder b) implements Sb {
        public Object raw() { return b; }
        public Sb append(String s) { b.append(s); return this; }
        public Sb append(CharSequence s, int start, int end) { b.append(s, start, end); return this; }
        public Sb append(char c) { b.append(c); return this; }
        public Sb append(int i) { b.append(i); return this; }
        public Sb append(char[] c) { b.append(c); return this; }
        public Sb appendRaw(Object o) { if (o instanceof StringBuffer f) b.append(f); else b.append((CharSequence) o); return this; }
        public Sb insert(int o, String s) { b.insert(o, s); return this; }
        public Sb insert(int o, char c) { b.insert(o, c); return this; }
        public Sb delete(int s, int e) { b.delete(s, e); return this; }
        public Sb deleteCharAt(int i) { b.deleteCharAt(i); return this; }
        public Sb replace(int s, int e, String r) { b.replace(s, e, r); return this; }
        public Sb reverse() { b.reverse(); return this; }
        public Sb repeat(CharSequence cs, int n) { b.repeat(cs, n); return this; }
        public void setCharAt(int i, char c) { b.setCharAt(i, c); }
        public void setLength(int n) { b.setLength(n); }
        public void ensureCapacity(int n) { b.ensureCapacity(n); }
        public void trimToSize() { b.trimToSize(); }
        public int capacity() { return b.capacity(); }
        public CharSequence cs() { return b; }
        public int compareToRaw(Object o) { return b.compareTo((StringBuilder) o); }
    }

    record Buffer(StringBuffer b) implements Sb {
        public Object raw() { return b; }
        public Sb append(String s) { b.append(s); return this; }
        public Sb append(CharSequence s, int start, int end) { b.append(s, start, end); return this; }
        public Sb append(char c) { b.append(c); return this; }
        public Sb append(int i) { b.append(i); return this; }
        public Sb append(char[] c) { b.append(c); return this; }
        public Sb appendRaw(Object o) { if (o instanceof StringBuffer f) b.append(f); else b.append((CharSequence) o); return this; }
        public Sb insert(int o, String s) { b.insert(o, s); return this; }
        public Sb insert(int o, char c) { b.insert(o, c); return this; }
        public Sb delete(int s, int e) { b.delete(s, e); return this; }
        public Sb deleteCharAt(int i) { b.deleteCharAt(i); return this; }
        public Sb replace(int s, int e, String r) { b.replace(s, e, r); return this; }
        public Sb reverse() { b.reverse(); return this; }
        public Sb repeat(CharSequence cs, int n) { b.repeat(cs, n); return this; }
        public void setCharAt(int i, char c) { b.setCharAt(i, c); }
        public void setLength(int n) { b.setLength(n); }
        public void ensureCapacity(int n) { b.ensureCapacity(n); }
        public void trimToSize() { b.trimToSize(); }
        public int capacity() { return b.capacity(); }
        public CharSequence cs() { return b; }
        public int compareToRaw(Object o) { return b.compareTo((StringBuffer) o); }
    }

    public static void main(String[] args) throws Throwable {
        flagOn = args.length > 0 && args[0].equals("flagOn");
        if (flagOn) {
            ownerChecks();
        } else {
            flagOffNeverLifts();
        }
        appleScenarios();
        seamScenarios();
        int scripts = randomScripts(40_000, 0x4d33534243L);
        System.out.println("M3CompositeBuilderTest mode=" + (flagOn ? "flagOn" : "flagOff")
                + " scripts=" + scripts + " checks=" + checks);
    }

    // ---------------------------------------------------------------- pieces and oracle feeding

    /** Subject gets a piece (real lift path or forced); oracle gets the same chars, never lifted. */
    private static void piece(Sb subject, StringBuilder oracle, String s, int start, int end,
                              boolean forced) throws Throwable {
        if (forced) {
            try {
                PIECE.invoke(subject.raw(), s, start, end);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
            if (subject.raw() instanceof StringBuffer f) {
                f.append(""); // the package entry bypasses StringBuffer's toStringCache reset
            }
        } else if (start == 0 && end == s.length()) {
            subject.append(s);
        } else {
            subject.append(s, start, end);
        }
        oracle.append(s.toCharArray(), start, end - start);
    }

    private static boolean composite(Object builder) throws IllegalAccessException {
        return LANE.get(builder) != null;
    }

    private static void same(Sb subject, StringBuilder oracle, String where) {
        check(subject.cs().length() == oracle.length(), where + ": length " + subject.cs().length()
                + " vs " + oracle.length());
        check(subject.capacity() == oracle.capacity(), where + ": capacity " + subject.capacity()
                + " vs " + oracle.capacity());
    }

    private static void sameContent(Sb subject, StringBuilder oracle, String where) {
        same(subject, oracle, where);
        String expected = oracle.toString();
        String actual = subject.raw().toString();
        check(expected.equals(actual), where + ": content");
        check(actual.hashCode() == expected.hashCode(), where + ": hash");
        check(expected.contentEquals(subject.cs()), where + ": contentEquals");
    }

    // ---------------------------------------------------------------- flag modes

    private static void flagOffNeverLifts() throws Throwable {
        StringBuilder sb = new StringBuilder();
        StringBuffer sf = new StringBuffer();
        String big = text(new Random(1), 500);
        for (int i = 0; i < 8; i++) {
            sb.append(big).append(big, 3, 400);
            sf.append(big).append(big, 3, 400);
        }
        check(!composite(sb) && !composite(sf), "flag off: the public append path never creates a lane");
    }

    private static void ownerChecks() throws Throwable {
        Random random = new Random(7);
        String a = new String(text(random, 300).toCharArray());
        String b = new String(text(random, 200).toCharArray());
        String c = new String(text(random, 90).toCharArray());
        check(STRING_M3.invoke(a) != null, "flag on: runtime Strings must be M3-admitted");
        StringBuilder sb = new StringBuilder().append(a).append('|').append(b).append(42).append(c);
        check(composite(sb), "flag on: M3 appends above LIFT_UNITS create the lane");
        String s1 = sb.toString();
        String expected = a + '|' + b + 42 + c;
        check(expected.equals(s1), "flag on: composed content");
        check(composite(sb), "flag on: toString leaves the builder composite");
        Object m3 = STRING_M3.invoke(s1);
        Object owner = ownerOf(m3);
        check(owner.getClass().getName().equals("java.lang.M3StringTuple"), "flag on: owner is a tuple, got " + owner.getClass());
        Map<Object, Boolean> leaves = new IdentityHashMap<>();
        collectLeaves(m3, leaves);
        for (String piece : List.of(a, b, c)) {
            check(leaves.containsKey(ownerOf(STRING_M3.invoke(piece))), "flag on: piece atom is a leaf of the result (no flatten)");
        }
        check(STRING_M3.invoke(sb.toString()) == m3, "flag on: unchanged builder reuses the composed storage");
        StringBuffer sf = new StringBuffer().append(a).append(b);
        String f1 = sf.toString();
        String f2 = sf.toString();
        check(f1 != f2 && ownerOf(STRING_M3.invoke(f1)) == ownerOf(STRING_M3.invoke(f2)), "flag on: StringBuffer cache shares the owner");
        check((a + b).equals(f2), "flag on: StringBuffer content");
    }

    private static Object ownerOf(Object m3) throws ReflectiveOperationException {
        Method owner = m3.getClass().getDeclaredMethod("owner");
        owner.setAccessible(true);
        return owner.invoke(m3);
    }

    private static void collectLeaves(Object m3, Map<Object, Boolean> leaves) throws ReflectiveOperationException {
        Object owner = ownerOf(m3);
        if (owner.getClass().getName().equals("java.lang.M3StringTuple")) {
            for (String side : new String[] {"left", "right"}) {
                Field f = owner.getClass().getDeclaredField(side);
                f.setAccessible(true);
                collectLeaves(f.get(owner), leaves);
            }
        } else {
            leaves.put(owner, Boolean.TRUE);
        }
    }

    // ---------------------------------------------------------------- Apple scenario replay

    private static void appleScenarios() throws Throwable {
        String sample = sample();
        for (Function<Integer, Sb> kind : kinds()) {
            buildAndAppend(kind, sample);             // R02, R09, B21, B22
            prependAndInsert(kind, sample);           // R10, R11, B24, B25
            removeAtIndex(kind, sample);              // R12, R13, R14
            join(kind, sample);                       // R15, R16, B23
            removeSubranges(kind, 10, 10);            // R17
            removeSubranges(kind, 100, 10);           // R18
            removeRandom(kind);                       // R19
            replaceSubranges(kind, sample);           // R20, B26
            unalignedIndices(kind, sample);           // B37, B44, S01
        }
    }

    private static List<Function<Integer, Sb>> kinds() {
        return List.of(cap -> new Builder(new StringBuilder(cap)), cap -> new Buffer(new StringBuffer(cap)));
    }

    private static Sb pieces(Function<Integer, Sb> kind, StringBuilder oracle, String src, int[] strides) throws Throwable {
        Sb s = kind.apply(16);
        int at = 0;
        for (int i = 0; at < src.length(); i++) {
            int end = Math.min(src.length(), at + strides[i % strides.length]);
            piece(s, oracle, src, at, end, true);
            at = end;
            if ((i & 3) == 3) {
                s.append((char) ('a' + i % 26));
                oracle.append((char) ('a' + i % 26));
            }
            same(s, oracle, "pieces stride");
        }
        return s;
    }

    private static void buildAndAppend(Function<Integer, Sb> kind, String sample) throws Throwable {
        for (int k : new int[] {1, 2, 3, 4, 13, 64, 100, 256, 257, 512, 513}) {
            StringBuilder oracle = new StringBuilder(16);
            Sb s = pieces(kind, oracle, sample, new int[] {k, k % 4 + 1, k + 1});
            sameContent(s, oracle, "R02/R09/B21/B22 stride " + k);
        }
    }

    private static void prependAndInsert(Function<Integer, Sb> kind, String sample) throws Throwable {
        List<String> chunks = chunks(sample, 37);
        StringBuilder oracle = new StringBuilder(16);
        Sb s = kind.apply(16);
        for (int i = chunks.size() - 1; i >= 0; i--) {
            piece(s, oracle, chunks.get(i), 0, chunks.get(i).length(), true);
            s.insert(0, chunks.get(i).substring(0, 3));
            oracle.insert(0, chunks.get(i).substring(0, 3));
            same(s, oracle, "R10 prepend");
        }
        sameContent(s, oracle, "R10");
        Random random = new Random(0);
        for (int i = 0; i < 200; i++) {
            String c = chunks.get(random.nextInt(chunks.size()));
            int at = random.nextInt(oracle.length() + 1);
            piece(s, oracle, c, 0, c.length(), true);
            s.insert(at, c);
            oracle.insert(at, c);
            same(s, oracle, "R11/B24/B25 insert");
        }
        sameContent(s, oracle, "R11/B24/B25");
    }

    private static void removeAtIndex(Function<Integer, Sb> kind, String sample) throws Throwable {
        StringBuilder oracle = new StringBuilder(16);
        Sb s = pieces(kind, oracle, sample, new int[] {5, 9, 70});
        Random random = new Random(3);
        int refills = 0;
        while (oracle.length() > 0) {
            int i = random.nextInt(oracle.length());
            if ((i & 1) == 0) {
                s.deleteCharAt(i);
                oracle.deleteCharAt(i);
            } else {
                int e = Math.min(oracle.length(), i + 1 + random.nextInt(9));
                s.delete(i, e);
                oracle.delete(i, e);
            }
            same(s, oracle, "R12/R13/R14 remove");
            if (refills < 64 && random.nextInt(8) == 0) {
                refills++;
                String c = sample.substring(0, 1 + random.nextInt(80));
                piece(s, oracle, c, 0, c.length(), true);
            }
        }
        sameContent(s, oracle, "R12/R13/R14");
    }

    private static void join(Function<Integer, Sb> kind, String sample) throws Throwable {
        List<Sb> parts = new ArrayList<>();
        List<StringBuilder> expected = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            StringBuilder o = new StringBuilder(16);
            Sb s = kind.apply(16);
            int start = (i * 7) % (sample.length() - 40);
            piece(s, o, sample, start, start + 1 + i % 40, true);
            parts.add(s);
            expected.add(o);
        }
        Random random = new Random(5);
        while (parts.size() > 1) {
            int i = random.nextInt(parts.size() - 1);
            Sb left = parts.get(i);
            Sb right = parts.remove(i + 1);
            String rightBefore = expected.get(i + 1).toString();
            left.appendRaw(right.raw());
            expected.get(i).append(expected.remove(i + 1));
            same(left, expected.get(i), "R15 join");
            check(rightBefore.equals(right.raw().toString()), "R16/B23: the joined source is unchanged");
            right.append('#');
            check(left.raw().toString().equals(expected.get(i).toString()), "R16/B23 copy on write");
        }
        sameContent(parts.get(0), expected.get(0), "R15");
    }

    private static void removeSubranges(Function<Integer, Sb> kind, int count, int width) throws Throwable {
        String src = text(new Random(count), count * width);
        int total = count * width;
        int[][] ranges = {{0, 0}, {0, 1}, {0, width}, {1, width - 1}, {width - 1, width + 1}, {width, 2 * width},
            {width / 2, 3 * width + 1}, {0, total}, {total - 1, total}, {total / 2, total / 2 + width}, {3, total - 3}};
        for (int[] r : ranges) {
            StringBuilder oracle = new StringBuilder(16);
            Sb s = pieces(kind, oracle, src, new int[] {width});
            s.delete(r[0], r[1]);
            oracle.delete(r[0], r[1]);
            sameContent(s, oracle, "R17/R18 removeSubrange " + r[0] + ".." + r[1]);
        }
    }

    private static void removeRandom(Function<Integer, Sb> kind) throws Throwable {
        for (int seed = 0; seed < 20; seed++) {
            Random random = new Random(seed);
            StringBuilder oracle = new StringBuilder(16);
            Sb s = pieces(kind, oracle, text(random, 600), new int[] {10, 33, 64});
            while (oracle.length() > 0) {
                int i = random.nextInt(oracle.length() + 1);
                int j = i + random.nextInt(oracle.length() - i + 1);
                s.delete(i, j);
                oracle.delete(i, j);
                same(s, oracle, "R19 seed " + seed);
            }
            sameContent(s, oracle, "R19 seed " + seed);
        }
    }

    private static void replaceSubranges(Function<Integer, Sb> kind, String sample) throws Throwable {
        String src = text(new Random(11), 100);
        String[] replacements = {"", "x", "0123456789", sample.substring(0, 100)};
        for (int i = 0; i <= 100; i += 7) {
            for (int j = i; j <= 100; j += 31) {
                for (String r : replacements) {
                    StringBuilder oracle = new StringBuilder(16);
                    Sb s = pieces(kind, oracle, src, new int[] {10});
                    s.replace(i, j, r);
                    oracle.replace(i, j, r);
                    sameContent(s, oracle, "R20/B26 replace " + i + ".." + j);
                }
            }
        }
        StringBuilder oracle = new StringBuilder(16);
        Sb empty = kind.apply(16);
        empty.replace(0, 0, sample.substring(0, 10));
        oracle.replace(0, 0, sample.substring(0, 10));
        sameContent(empty, oracle, "R20 into empty");
    }

    private static void unalignedIndices(Function<Integer, Sb> kind, String sample) throws Throwable {
        StringBuilder oracle = new StringBuilder(16);
        Sb s = pieces(kind, oracle, sample, new int[] {1, 2, 3, 5, 7});
        String expected = oracle.toString();
        for (int i = 0; i < expected.length(); i++) {
            Sb copy = kind.apply(16);
            StringBuilder o = new StringBuilder(16);
            if (i > 0) {
                piece(copy, o, expected, 0, i, true);
            }
            piece(copy, o, expected, i, expected.length(), true);
            CharSequence cs = copy.cs();
            check(Character.codePointAt(cs, i) == expected.codePointAt(i), "B44 codePointAt " + i);
            check(cs.codePoints().sum() == expected.codePoints().sum(), "S01 codePoints " + i);
            if (i > 0) {
                check(Character.codePointBefore(cs, i) == expected.codePointBefore(i), "B44 codePointBefore " + i);
            }
            same(copy, o, "B37/B44");
        }
        sameContent(s, oracle, "S01");
    }

    // ---------------------------------------------------------------- seams (F01, F02)

    private static void seamScenarios() throws Throwable {
        char hi = Character.highSurrogate(0x1F600);
        char lo = Character.lowSurrogate(0x1F600);
        String crPiece = "line one\r";
        String hiPiece = "smile " + hi;
        String loPiece = lo + " after";
        for (Function<Integer, Sb> kind : kinds()) {
            StringBuilder oracle = new StringBuilder(16);
            Sb s = kind.apply(16);
            piece(s, oracle, crPiece, 0, crPiece.length(), true);
            s.append('\n');
            oracle.append('\n');
            piece(s, oracle, hiPiece, 0, hiPiece.length(), true);
            piece(s, oracle, loPiece, 0, loPiece.length(), true);
            check(Character.codePointAt(s.cs(), 16) == 0x1F600, "F02 code point across a piece seam");
            check(s.cs().chars().sum() == oracle.chars().sum(), "F01 chars across a CRLF seam");
            check(s.cs().codePoints().count() == oracle.codePoints().count(), "F02 codePoints count");
            sameContent(s, oracle, "F01/F02");
            piece(s, oracle, hiPiece, 0, hiPiece.length(), true);
            piece(s, oracle, loPiece, 0, loPiece.length(), true);
            s.reverse();
            oracle.reverse();
            sameContent(s, oracle, "F02 reverse keeps surrogate pairs");
        }
    }

    // ---------------------------------------------------------------- F03 random scripts

    private static int randomScripts(int scripts, long seed) throws Throwable {
        Random random = new Random(seed);
        for (int n = 0; n < scripts; n++) {
            script(random, n);
        }
        return scripts;
    }

    private static void script(Random random, int n) throws Throwable {
        boolean buffer = (n & 7) == 7;
        String init = random.nextInt(4) == 0 ? text(random, random.nextInt(120)) : null;
        StringBuilder oracle = init == null ? new StringBuilder(16) : new StringBuilder(init.length() + 16);
        Sb s;
        if (init == null) {
            s = buffer ? new Buffer(new StringBuffer()) : new Builder(new StringBuilder());
        } else {
            s = buffer ? new Buffer(new StringBuffer(init)) : new Builder(new StringBuilder(init));
            oracle.append(init.toCharArray());
        }
        Sb peer = buffer ? new Buffer(new StringBuffer()) : new Builder(new StringBuilder());
        StringBuilder peerOracle = new StringBuilder(16);
        String big = text(random, 64 + random.nextInt(300));
        piece(peer, peerOracle, big, 0, big.length(), true);
        int ops = 4 + random.nextInt(14);
        for (int i = 0; i < ops; i++) {
            op(random, s, oracle, peer, peerOracle);
            same(s, oracle, "script " + n + " op " + i);
        }
        sameContent(s, oracle, "script " + n);
    }

    private static void op(Random r, Sb s, StringBuilder o, Sb peer, StringBuilder peerOracle) throws Throwable {
        int len = o.length();
        switch (r.nextInt(24)) {
            case 0 -> { String t = text(r, r.nextInt(9)); s.append(t); o.append(t.toCharArray()); }
            case 1, 2, 3 -> { String t = text(r, 64 + r.nextInt(240)); piece(s, o, t, 0, t.length(), r.nextBoolean()); }
            case 4 -> { String t = text(r, 64 + r.nextInt(240)); int a = r.nextInt(t.length() - 32);
                        int b = a + 1 + r.nextInt(t.length() - a); piece(s, o, t, a, b, r.nextBoolean() || b - a < 64); }
            case 5 -> { char c = alphabet(r); s.append(c); o.append(c); }
            case 6 -> { int v = r.nextInt(); s.append(v); o.append(v); }
            case 7 -> { char[] c = text(r, r.nextInt(20)).toCharArray(); s.append(c); o.append(c); }
            case 8 -> { String t = text(r, r.nextInt(30)); s.append(CharBuffer.wrap(t), 0, t.length()); o.append(CharBuffer.wrap(t), 0, t.length()); }
            case 9 -> { if (len < 4_000) { s.appendRaw(s.raw()); o.append(o); } }
            case 10 -> { String before = peerOracle.toString(); s.appendRaw(peer.raw()); o.append(peerOracle);
                         check(before.equals(peer.raw().toString()), "peer unchanged by append"); }
            case 11 -> { String t = text(r, r.nextInt(70)); int at = r.nextInt(len + 1); s.insert(at, t); o.insert(at, t); }
            case 12 -> { int a = r.nextInt(len + 1); int b = a + r.nextInt(len - a + 1); s.delete(a, b); o.delete(a, b); }
            case 13 -> { if (len > 0) { int a = r.nextInt(len); s.deleteCharAt(a); o.deleteCharAt(a); } }
            case 14 -> { int a = r.nextInt(len + 1); int b = a + r.nextInt(len - a + 1); String t = text(r, r.nextInt(20));
                         s.replace(a, b, t); o.replace(a, b, t); }
            case 15 -> { if (len > 0) { int a = r.nextInt(len); char c = alphabet(r); s.setCharAt(a, c); o.setCharAt(a, c); } }
            case 16 -> { int n = r.nextInt(len + 20); s.setLength(n); o.setLength(n); }
            case 17 -> { s.reverse(); o.reverse(); }
            case 18 -> reads(r, s, o);
            case 19 -> { int c = r.nextInt(len + 300); s.ensureCapacity(c); o.ensureCapacity(c); if (r.nextInt(4) == 0) { s.trimToSize(); o.trimToSize(); } }
            case 20 -> { check(s.cs().chars().sum() == o.chars().sum(), "chars"); check(s.cs().codePoints().count() == o.codePoints().count(), "codePoints"); }
            case 21 -> check(Integer.signum(s.compareToRaw(peer.raw())) == Integer.signum(o.compareTo(peerOracle)), "compareTo");
            case 22 -> { if (len < 2_000) { int k = 2 + r.nextInt(3); s.repeat(peer.raw() instanceof StringBuilder p ? p : peer.cs(), k);
                         o.repeat(peerOracle, k); } }
            default -> serialRoundTrip(s, o);
        }
    }

    private static void reads(Random r, Sb s, StringBuilder o) {
        int len = o.length();
        CharSequence cs = s.cs();
        int i = r.nextInt(len + 2) - 1;
        check(outcome(() -> cs.charAt(i)).equals(outcome(() -> o.charAt(i))), "charAt " + i);
        String text = o.toString();
        if (len > 0) {
            int a = r.nextInt(len);
            int b = Math.min(len, a + 1 + r.nextInt(12));
            String needle = text.substring(a, b);
            int from = r.nextInt(len + 1);
            check(indexOf(cs, needle, from) == o.indexOf(needle, from), "indexOf");
            check(lastIndexOf(cs, needle, from) == o.lastIndexOf(needle, from), "lastIndexOf");
            check(cs.subSequence(a, b).toString().equals(needle), "subSequence");
        }
        int j = r.nextInt(len + 2) - 1;
        check(outcome(() -> cs.subSequence(0, j).length()).equals(outcome(() -> o.subSequence(0, j).length())), "subSequence bounds");
    }

    private static int indexOf(CharSequence cs, String needle, int from) {
        return cs instanceof StringBuilder b ? b.indexOf(needle, from) : ((StringBuffer) cs).indexOf(needle, from);
    }

    private static int lastIndexOf(CharSequence cs, String needle, int from) {
        return cs instanceof StringBuilder b ? b.lastIndexOf(needle, from) : ((StringBuffer) cs).lastIndexOf(needle, from);
    }

    private static void serialRoundTrip(Sb s, StringBuilder o) throws Exception {
        Object copy = roundTrip(s.raw());
        Object oracleCopy = roundTrip(o);
        check(copy.toString().equals(oracleCopy.toString()), "serialization content");
        int capacity = copy instanceof StringBuilder b ? b.capacity() : ((StringBuffer) copy).capacity();
        check(capacity == ((StringBuilder) oracleCopy).capacity(), "serialization capacity");
    }

    private static Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    // ---------------------------------------------------------------- text

    private interface Thrower { Object get(); }

    private static String outcome(Thrower t) {
        try {
            return "value:" + t.get();
        } catch (RuntimeException e) {
            return "throws:" + e.getClass().getName();
        }
    }

    private static char alphabet(Random r) {
        return switch (r.nextInt(12)) {
            case 0 -> '\r';
            case 1 -> '\n';
            case 2 -> (char) 0xE9;
            case 3 -> (char) 0x3B1;
            case 4 -> (char) 0x939;
            case 5 -> Character.highSurrogate(0x1F600);
            case 6 -> Character.lowSurrogate(0x1F600);
            default -> (char) ('a' + r.nextInt(26));
        };
    }

    private static String text(Random r, int length) {
        char[] chars = new char[length];
        boolean latin = r.nextInt(3) != 0;
        for (int i = 0; i < length; i++) {
            char c = alphabet(r);
            chars[i] = latin && c > 0xFF ? (char) ('A' + i % 26) : c;
        }
        return new String(chars);
    }

    private static List<String> chunks(String src, int width) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < src.length(); i += width) {
            out.add(src.substring(i, Math.min(src.length(), i + width)));
        }
        return out;
    }

    /** Multilingual sample in the spirit of S01 (ASCII, CJK, kana, Hangul, ZWJ emoji, flags, marks). */
    private static String sample() {
        int[] cps = {'T', 'h', 'e', ' ', 0x4E2D, 0x6587, ' ', 0x3072, 0x3089, 0x304C, 0x306A, ' ', 0xD55C, 0xAE00,
            ' ', 0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467, ' ', 0x1F1EE, 0x1F1F3, ' ', 'e', 0x301, ' ', '\r', '\n'};
        String unit = new String(cps, 0, cps.length);
        return unit.repeat(40);
    }

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError(what);
        }
    }
}
