/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Measure laws, seam repair, a 40,000-operation edit differential against a flat {@link StringBuilder}
 * oracle (fold-vs-combine checked through {@link M3MeasuredRope#verifyInvariants()}), the DFA transfer
 * element against {@code java.util.regex}, and the retained-field shape (no stored node objects).
 */
final class M3MeasuredRopeTest {
    private static final char[] ABX = {'a', 'b', 'x', 'y', 'z', (char) 0xE9};
    private static final Pattern TERMINATOR = Pattern.compile("\\r\\n|\\r|\\n");
    private static final int[] ALPHABET = {
        'a', 'b', 'x', ' ', '\t', '\r', '\n', 0xE9, 0x436, 0x4E2D, 0x1F600, 0x1F1E8, 0xD83D, 0xDE00, 0x301, 0x2003, 0xA0
    };

    static M3RopeArena textArena(int maxLeaf) {
        return new M3RopeArena(maxLeaf, M3RopeMeasures.text());
    }

    /** Asserts text, length and every built-in measure against values computed from the flat String only. */
    static void assertMatchesOracle(M3MeasuredRope rope, String expected) {
        int n = expected.length();
        assertEquals(n, rope.length());
        assertEquals(expected, rope.toString());
        long first = n == 0 ? -1 : expected.charAt(0);
        long last = n == 0 ? -1 : expected.charAt(n - 1);
        int pow = 1;
        for (int i = 0; i < n; i++) pow *= 31;
        assertArrayEquals(new long[] {expected.hashCode(), pow}, rope.summary(M3RopeMeasures.HASH), "hash");
        assertArrayEquals(new long[] {expected.codePointCount(0, n), first, last},
                rope.summary(M3RopeMeasures.CODE_POINTS), "code points");
        assertArrayEquals(new long[] {expected.getBytes(StandardCharsets.UTF_8).length, first, last},
                rope.summary(M3RopeMeasures.UTF8_LENGTH), "utf8");
        assertArrayEquals(new long[] {terminators(expected), first, last},
                rope.summary(M3RopeMeasures.LINES), "lines");
        long[] ws = rope.summary(M3RopeMeasures.WHITESPACE);
        assertEquals(n - expected.stripLeading().length(), ws[0], "leading whitespace");
        assertEquals(n - expected.stripTrailing().length(), ws[1], "trailing whitespace");
        assertEquals(n, ws[2]);
        assertEquals(expected.isBlank(), ws[0] == n, "isBlank");
    }

    static long terminators(String s) {
        Matcher m = TERMINATOR.matcher(s);
        long count = 0;
        while (m.find()) count++;
        return count;
    }

    static String randomText(SplittableRandom random, int maxUnits) {
        StringBuilder out = new StringBuilder();
        int target = random.nextInt(maxUnits + 1);
        while (out.length() < target) out.appendCodePoint(ALPHABET[random.nextInt(ALPHABET.length)]);
        return out.toString();
    }

    /** Offset of the start of line {@code k} (k >= 1) found by the LINES metric. */
    static int lineStart(M3MeasuredRope rope, int k) {
        int p = rope.seek(M3RopeMeasures.LINES, k - 1);
        if (p > 0 && p < rope.length() && rope.charAt(p - 1) == '\r' && rope.charAt(p) == '\n') p++;
        return p;
    }

    static M3RopeMeasures.Dfa containsAb() {
        byte[] classes = new byte[256];
        Arrays.fill(classes, (byte) 2);
        classes['a'] = 0;
        classes['b'] = 1;
        return M3RopeMeasures.dfa(3, 0, 1 << 2, 3, classes, 2, new byte[] {1, 0, 0, 1, 2, 0, 2, 2, 2});
    }

    static M3RopeMeasures.Dfa evenX() {
        byte[] classes = new byte[256];
        classes['x'] = 1;
        return M3RopeMeasures.dfa(2, 0, 1, 2, classes, 0, new byte[] {0, 1, 1, 0});
    }

    @Test
    void measureLawsAtEverySplit() {
        SplittableRandom random = new SplittableRandom(11);
        List<M3RopeMeasure> all = new ArrayList<>(List.of(M3RopeMeasures.text()));
        all.add(containsAb());
        all.add(evenX());
        for (int round = 0; round < 60; round++) {
            String s = randomText(random, 48);
            int n = s.length();
            for (M3RopeMeasure m : all) {
                int w = m.width();
                long[] whole = leaf(m, s, 0, n);
                long[] id = new long[w];
                m.identity(id, 0);
                assertArrayEquals(whole, combine(m, id, whole), "left identity");
                assertArrayEquals(whole, combine(m, whole, id), "right identity");
                for (int b = 0; b <= n; b++) {
                    assertArrayEquals(whole, combine(m, leaf(m, s, 0, b), leaf(m, s, b, n)), "split " + b);
                }
                int[] cut = {random.nextInt(n + 1), random.nextInt(n + 1), random.nextInt(n + 1)};
                Arrays.sort(cut);
                long[] x = leaf(m, s, 0, cut[0]);
                long[] y = leaf(m, s, cut[0], cut[1]);
                long[] z = leaf(m, s, cut[1], cut[2]);
                assertArrayEquals(combine(m, combine(m, x, y), z), combine(m, x, combine(m, y, z)), "assoc");
            }
        }
    }

    private static long[] leaf(M3RopeMeasure m, String s, int from, int to) {
        long[] out = new long[m.width()];
        m.leaf(s, from, to, out, 0);
        return out;
    }

    private static long[] combine(M3RopeMeasure m, long[] l, long[] r) {
        long[] out = new long[m.width()];
        m.combine(l, 0, r, 0, out, 0);
        return out;
    }

    /** The lane measures are the node form of the module's value-level {@link M3TextMetrics} owner. */
    @Test
    void agreesWithM3TextMetrics() {
        SplittableRandom random = new SplittableRandom(29);
        for (int round = 0; round < 200; round++) {
            M3RopeArena arena = textArena(1 + random.nextInt(5));
            M3MeasuredRope rope = arena.empty();
            for (int k = random.nextInt(6); k >= 0; k--) rope = rope.concat(randomText(random, 30));
            String s = rope.toString();
            M3TextMetrics metrics = M3TextMetrics.of(s);
            assertEquals(metrics.javaHashCode(), (int) rope.summary(M3RopeMeasures.HASH)[0]);
            assertEquals(metrics.hash31Power(), (int) rope.summary(M3RopeMeasures.HASH)[1]);
            assertEquals(metrics.codePointCount(), rope.summary(M3RopeMeasures.CODE_POINTS)[0]);
            assertEquals(metrics.utf8Length(), rope.summary(M3RopeMeasures.UTF8_LENGTH)[0]);
            assertEquals(metrics.firstUtf16Unit(), rope.summary(M3RopeMeasures.CODE_POINTS)[1]);
            assertEquals(metrics.lastUtf16Unit(), rope.summary(M3RopeMeasures.CODE_POINTS)[2]);
        }
    }

    @Test
    void crlfSplitAtEverySeam() {
        String t = "ab\r\ncd\r\r\n\n\re\r\n\r\nf\r";
        for (int maxLeaf = 1; maxLeaf <= 3; maxLeaf++) {
            M3RopeArena arena = textArena(maxLeaf);
            for (int s = 0; s <= t.length(); s++) {
                M3MeasuredRope rope = arena.of(t.substring(0, s)).concat(t.substring(s));
                rope.verifyInvariants();
                assertMatchesOracle(rope, t);
                List<Integer> starts = new ArrayList<>();
                Matcher m = TERMINATOR.matcher(t);
                while (m.find()) starts.add(m.end());
                for (int k = 1; k <= starts.size(); k++) assertEquals(starts.get(k - 1), lineStart(rope, k), "line " + k);
                long tail = t.isEmpty() || t.endsWith("\r") || t.endsWith("\n") ? 0 : 1;
                assertEquals(t.lines().count(), starts.size() + tail);
            }
        }
    }

    @Test
    void randomEditDifferential() {
        SplittableRandom random = new SplittableRandom(20261018);
        int[] leafSizes = {1, 2, 3, 4, 7, 16, 64};
        int operations = 0;
        for (int round = 0; round < 40; round++) {
            M3RopeArena arena = textArena(leafSizes[round % leafSizes.length]);
            M3MeasuredRope rope = arena.empty();
            StringBuilder oracle = new StringBuilder();
            List<M3MeasuredRope> snapshots = new ArrayList<>();
            List<String> snapshotText = new ArrayList<>();
            for (int step = 0; step < 1000; step++, operations++) {
                rope = edit(random, arena, rope, oracle);
                String expected = oracle.toString();
                assertMatchesOracle(rope, expected);
                if (step % 64 == 63) rope.verifyInvariants();
                if (step % 100 == 0) {
                    snapshots.add(rope);
                    snapshotText.add(expected);
                }
            }
            rope.verifyInvariants();
            for (int i = 0; i < snapshots.size(); i++) assertMatchesOracle(snapshots.get(i), snapshotText.get(i));
        }
        assertTrue(operations >= 40_000);
    }

    private static M3MeasuredRope edit(SplittableRandom random, M3RopeArena arena, M3MeasuredRope rope,
            StringBuilder oracle) {
        int n = rope.length();
        int a = random.nextInt(n + 1);
        int b = a + random.nextInt(n - a + 1);
        if (n > 1500) {
            oracle.delete(a, b);
            return rope.remove(a, b);
        }
        switch (random.nextInt(6)) {
            case 0 -> {
                String s = randomText(random, 40);
                oracle.insert(a, s);
                return rope.insert(a, s);
            }
            case 1 -> {
                oracle.delete(a, b);
                return rope.remove(a, b);
            }
            case 2 -> {
                String s = randomText(random, 40);
                oracle.replace(a, b, s);
                return rope.replace(a, b, s);
            }
            case 3 -> {
                String s = randomText(random, 200);
                if (random.nextBoolean()) {
                    oracle.append(s);
                    return rope.concat(arena.of(s));
                }
                oracle.insert(0, s);
                return arena.of(s).concat(rope);
            }
            case 4 -> {
                String keep = oracle.substring(a, b);
                oracle.setLength(0);
                oracle.append(keep);
                return rope.slice(a, b);
            }
            default -> {
                String shared = oracle.substring(a, b);
                int at = random.nextInt(n + 1);
                oracle.insert(at, shared);
                return rope.insert(at, rope.slice(a, b));
            }
        }
    }

    @Test
    void dfaTransferMatchesRegex() {
        M3RopeMeasures.Dfa ab = containsAb();
        M3RopeMeasures.Dfa even = evenX();
        SplittableRandom random = new SplittableRandom(5);
        Pattern needle = Pattern.compile("ab");
        for (int round = 0; round < 300; round++) {
            M3RopeArena arena = new M3RopeArena(1 + random.nextInt(6), ab, even, M3RopeMeasures.CODE_POINTS);
            StringBuilder oracle = new StringBuilder();
            M3MeasuredRope rope = arena.empty();
            for (int step = 0; step < 12; step++) {
                String piece = randomAbx(random);
                int at = random.nextInt(rope.length() + 1);
                oracle.insert(at, piece);
                rope = rope.insert(at, piece);
                String s = oracle.toString();
                assertEquals(needle.matcher(s).find(), ab.accepts(rope.summary(ab), 0), s);
                assertEquals(s.chars().filter(c -> c == 'x').count() % 2 == 0, even.accepts(rope.summary(even), 0));
                int from = random.nextInt(s.length() + 1);
                int to = from + random.nextInt(s.length() - from + 1);
                assertEquals(needle.matcher(s.substring(from, to)).find(),
                        ab.accepts(rope.rangeSummary(ab, from, to), 0), "range");
            }
            rope.verifyInvariants();
        }
    }

    private static String randomAbx(SplittableRandom random) {
        StringBuilder out = new StringBuilder();
        int n = random.nextInt(9);
        for (int i = 0; i < n; i++) out.append(ABX[random.nextInt(ABX.length)]);
        return out.toString();
    }

    @Test
    void whitespaceMatchesStrip() {
        SplittableRandom random = new SplittableRandom(3);
        int[] spaces = {' ', '\t', '\n', 0x2003, 0xA0, 'q', 0x1F600};
        for (int round = 0; round < 400; round++) {
            StringBuilder text = new StringBuilder();
            int len = random.nextInt(30);
            for (int i = 0; i < len; i++) text.appendCodePoint(spaces[random.nextInt(round % 2 == 0 ? 3 : spaces.length)]);
            String s = text.toString();
            List<String> chunks = new ArrayList<>();
            for (int i = 0; i < s.length(); ) {
                int j = Math.min(s.length(), i + 1 + random.nextInt(4));
                chunks.add(s.substring(i, j));
                i = j;
            }
            M3MeasuredRope rope = textArena(2).ofChunks(chunks.toArray(new String[0]));
            rope.verifyInvariants();
            assertMatchesOracle(rope, s);
        }
    }

    @Test
    void contractEdgesAndRefusals() {
        M3RopeArena arena = textArena(4);
        M3MeasuredRope rope = arena.of("hello world");
        assertSame(arena.empty(), arena.of(""));
        assertSame(rope, rope.slice(0, rope.length()));
        assertEquals(0, arena.empty().height());
        assertTrue(rope.contentEquals("hello world"));
        assertFalse(rope.contentEquals("hello worle"));
        assertEquals("lo wo", rope.subSequence(3, 8).toString());
        assertThrows(IndexOutOfBoundsException.class, () -> rope.charAt(11));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.slice(4, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.insert(12, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> rope.getChars(0, 3, new char[2], 0));
        assertThrows(IllegalArgumentException.class, () -> rope.concat(textArena(4).of("x")));
        assertThrows(IllegalArgumentException.class, () -> rope.seek(M3RopeMeasures.CODE_POINTS, -1));
        assertThrows(IllegalArgumentException.class, () -> rope.summary(containsAb()));
        assertThrows(IllegalArgumentException.class, () -> new M3RopeArena(0));
        assertThrows(IllegalArgumentException.class,
                () -> new M3RopeArena(4, M3RopeMeasures.HASH, M3RopeMeasures.HASH));
        assertThrows(IllegalArgumentException.class,
                () -> M3RopeMeasures.dfa(17, 0, 1, 1, new byte[256], 0, new byte[17]));
        assertEquals(rope.length(), rope.seek(M3RopeMeasures.CODE_POINTS, 11));
        assertEquals(0, arena.empty().seek(M3RopeMeasures.LINES, 0));
        char[] out = new char[5];
        rope.getChars(6, 11, out, 0);
        assertEquals("world", new String(out));
    }

    /** DOOP evidence: per-node and per-element allocation 0; the arena holds primitive lanes only. */
    @Test
    void retainedFieldShapeHasNoNodeObjects() {
        Set<Class<?>> lanes = Set.of(int.class, int[].class, byte[].class, long[].class, long[][].class,
                String[].class, M3RopeMeasure[].class, M3MeasuredRope.class);
        for (Field f : M3RopeArena.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) assertTrue(lanes.contains(f.getType()), f.toString());
        }
        List<Class<?>> facade = new ArrayList<>();
        for (Field f : M3MeasuredRope.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) facade.add(f.getType());
        }
        assertEquals(List.of(M3RopeArena.class, int.class), facade);
        M3RopeArena arena = textArena(8);
        M3MeasuredRope rope = arena.of("x".repeat(64));
        int before = arena.nodeCount();
        rope.insert(30, "y");
        assertTrue(arena.nodeCount() - before <= 4 * (rope.height() + 2), "path copy is O(log n)");
    }
}
