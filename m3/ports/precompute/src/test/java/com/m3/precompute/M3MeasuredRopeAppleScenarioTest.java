/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Scenario donor (pattern only; no Swift code or sample text copied): apple/swift-collections
 * Tests/RopeModuleTests/TestRope.swift (blob 95bb4b99), TestBigString.swift (blob e2135fae) and
 * SampleStrings.swift (blob 563fb8f8) at hsoliwal/swift-collections@b905c889 (== apple/swift-collections main),
 * Copyright (c) 2023 - 2026 Apple Inc. and the Swift project authors, Apache License 2.0 with Runtime Library
 * Exception. Scenario-to-method map: APPLE_TEST_MAP.tsv (run 20261017-synexia-to-public, leaf R2-MEASURED-ROPE).
 * Character (grapheme) and UTF-8 index scenarios have no java.lang.String contract and are mapped to code
 * point and UTF-16 counterparts or recorded NOT_APPLICABLE there.
 */
package com.m3.precompute;

import static com.m3.precompute.M3MeasuredRopeTest.assertMatchesOracle;
import static com.m3.precompute.M3MeasuredRopeTest.textArena;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

final class M3MeasuredRopeAppleScenarioTest {
    private static final String CORPUS = corpus();
    private static final int[] REMOVE_SIMPLE = {
        0, 0, 30, 30, 0, 100, 90, 100, 0, 10, 30, 40, 70, 80, 0, 1, 30, 35, 60, 66, 90, 98, 9, 10, 35, 40, 64, 70,
        98, 100, 0, 20, 80, 100, 10, 30, 50, 70, 0, 30, 70, 90, 0, 60, 60, 100, 40, 70, 10, 90, 0, 69, 42, 73,
        21, 89, 1, 99, 1, 59, 61, 99
    };
    private static final int[] REMOVE_LARGER = {
        0, 0, 0, 1000, 0, 100, 900, 1000, 120, 330, 734, 894, 183, 892, 181, 479, 191, 469, 2, 722, 358, 718,
        12, 732, 348, 728, 63, 783, 297, 655
    };

    /** S01: first-party corpus with the Unicode classes of SampleStrings.swift plus Java-only cases. */
    static String corpus() {
        StringBuilder s = new StringBuilder(" \t ");
        for (int i = 0; i < 3; i++) {
            s.append("The quick brown fox jumps over the lazy dog.  Pack my box\twith five dozen jugs.\r\n");
        }
        for (int i = 0; i < 80; i++) s.appendCodePoint(0x4E00 + i * 37);
        s.append('\n');
        for (int i = 0; i < 50; i++) s.appendCodePoint(0x3041 + i);
        s.append("\r");
        for (int i = 0; i < 40; i++) s.appendCodePoint(0xAC00 + i * 13).append(i % 7 == 0 ? " " : "");
        s.append("\n\n");
        for (int i = 0; i < 30; i++) s.appendCodePoint(0x391 + i % 25).appendCodePoint(0x410 + i).appendCodePoint(0xC0 + i);
        for (int i = 0; i < 24; i++) s.appendCodePoint(0x1F600 + i * 3);
        int[] family = {0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467, 0x200D, 0x1F466};
        for (int i = 0; i < 6; i++) for (int cp : family) s.appendCodePoint(cp);
        for (int i = 0; i < 20; i++) s.appendCodePoint(0x1F1FA).appendCodePoint(0x1F1F8).appendCodePoint(0x1F1E8).appendCodePoint(0x1F1E6);
        s.append("\r\r\n\n");
        for (int i = 0; i < 40; i++) s.append((char) ('a' + i % 26)).append((char) 0x301).append((char) 0x302).append((char) 0x327);
        s.append((char) 0xD800).append('x').append((char) 0xDC00).append((char) 0xDE00).append((char) 0xD83D).append(' ');
        s.append((char) 0xA0).append("nbsp").append((char) 0xA0);
        for (int i = 0; i < 2; i++) {
            s.append(" Swift is a powerful and intuitive programming language; M3 keeps every String composite.\n");
        }
        s.append(' ').append((char) 0x2003).append("\t \n");
        return s.toString();
    }

    private static String chunk(int value, int length) {
        return String.valueOf((char) (0x4E00 + value % 20000)).repeat(length);
    }

    private static List<String> chunks(int count) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(chunk(i, i % 4 + 1));
        return out;
    }

    private static List<String> piecesByCodePoints(String s, int stride) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < s.length(); ) {
            int j = s.offsetByCodePoints(i, Math.min(stride, s.codePointCount(i, s.length())));
            out.add(s.substring(i, j));
            i = j;
        }
        return out;
    }

    private static int[] shuffled(int n, long seed) {
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        SplittableRandom random = new SplittableRandom(seed);
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        return order;
    }

    @Test
    void emptyRope() {
        M3RopeArena arena = textArena(8);
        M3MeasuredRope empty = arena.empty();
        empty.verifyInvariants();
        assertMatchesOracle(empty, "");
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.leafCount());
        assertEquals(0, empty.seek(M3RopeMeasures.CODE_POINTS, 0));
        assertEquals("foobar", arena.of("foobar").toString());
        for (M3RopeMeasure m : M3RopeMeasures.text()) {
            long[] id = new long[m.width()];
            m.identity(id, 0);
            assertArrayEquals(id, empty.summary(m));
        }
    }

    @Test
    void buildFromChunksMatchesFold() {
        List<String> ref = chunks(1000);
        M3RopeArena arena = textArena(64);
        M3MeasuredRope built = arena.ofChunks(ref.toArray(new String[0]));
        M3MeasuredRope incremental = arena.empty();
        for (String c : ref) {
            incremental = incremental.concat(c);
            incremental.verifyInvariants();
        }
        String expected = String.join("", ref);
        for (M3MeasuredRope rope : List.of(built, incremental)) {
            rope.verifyInvariants();
            assertMatchesOracle(rope, expected);
            for (M3RopeMeasure m : M3RopeMeasures.text()) {
                long[] fold = new long[m.width()];
                m.identity(fold, 0);
                for (String c : ref) {
                    long[] piece = new long[m.width()];
                    long[] next = new long[m.width()];
                    m.leaf(c, 0, c.length(), piece, 0);
                    m.combine(fold, 0, piece, 0, next, 0);
                    fold = next;
                }
                assertArrayEquals(fold, rope.summary(m));
            }
        }
    }

    @Test
    void leafIterationForwardAndBackward() {
        for (int c : new int[] {0, 1, 2, 10, 100, 500, 1000, 10000}) {
            List<String> ref = chunks(c);
            M3MeasuredRope rope = textArena(64).ofChunks(ref.toArray(new String[0]));
            List<String> forward = new ArrayList<>();
            rope.forEachLeaf((t, from, to) -> forward.add(t.substring(from, to)));
            assertEquals(ref, forward);
            List<String> backward = new ArrayList<>();
            rope.forEachLeafReverse((t, from, to) -> backward.add(0, t.substring(from, to)));
            assertEquals(ref, backward);
            assertEquals(c, rope.leafCount());
        }
    }

    @Test
    void charAtEveryIndex() {
        for (int maxLeaf : new int[] {1, 7, 64}) {
            M3MeasuredRope rope = textArena(maxLeaf).of(CORPUS);
            for (int i = 0; i < CORPUS.length(); i++) assertEquals(CORPUS.charAt(i), rope.charAt(i));
            for (int i = CORPUS.length() - 1; i >= 0; i--) assertEquals(CORPUS.charAt(i), rope.charAt(i));
            assertThrows(IndexOutOfBoundsException.class, () -> rope.charAt(-1));
        }
    }

    @Test
    void rangeSummaryAllBoundaryPairs() {
        List<String> parts = new ArrayList<>();
        int[] bounds = new int[301];
        for (int i = 0, at = 0; i < 300; i++) {
            int len = i % 4 + 1;
            parts.add(CORPUS.substring(at, at + len));
            at += len;
            bounds[i + 1] = at;
        }
        String s = String.join("", parts);
        M3MeasuredRope rope = textArena(64).ofChunks(parts.toArray(new String[0]));
        int total = s.codePointCount(0, s.length());
        for (int i = 0; i < bounds.length; i++) {
            for (int j = i; j < bounds.length; j++) {
                String sub = s.substring(bounds[i], bounds[j]);
                assertEquals(s.codePointCount(bounds[i], bounds[j]),
                        rope.rangeSummary(M3RopeMeasures.CODE_POINTS, bounds[i], bounds[j])[0]);
                assertEquals(sub.getBytes(StandardCharsets.UTF_8).length,
                        rope.rangeSummary(M3RopeMeasures.UTF8_LENGTH, bounds[i], bounds[j])[0]);
                assertEquals(sub.hashCode(), (int) rope.rangeSummary(M3RopeMeasures.HASH, bounds[i], bounds[j])[0]);
                if (onCodePoint(s, bounds[i]) && onCodePoint(s, bounds[j])) {
                    int k = s.codePointCount(0, bounds[i]) + s.codePointCount(bounds[i], bounds[j]);
                    assertEquals(bounds[j], k == total ? s.length() : rope.seek(M3RopeMeasures.CODE_POINTS, k) - 1);
                }
            }
        }
    }

    private static boolean onCodePoint(String s, int p) {
        return p == 0 || p == s.length()
                || !(Character.isHighSurrogate(s.charAt(p - 1)) && Character.isLowSurrogate(s.charAt(p)));
    }

    @Test
    void seekEveryCodePoint() {
        for (int maxLeaf : new int[] {1, 3, 64}) {
            M3MeasuredRope rope = textArena(maxLeaf).of(CORPUS);
            int total = CORPUS.codePointCount(0, CORPUS.length());
            int[] expected = CORPUS.codePoints().toArray();
            for (int k = 0; k <= total; k++) {
                int offset = k == total ? rope.length() : rope.seek(M3RopeMeasures.CODE_POINTS, k) - 1;
                assertEquals(CORPUS.offsetByCodePoints(0, k), offset, "code point " + k);
                if (k < total) assertEquals(expected[k], Character.codePointAt(rope, offset));
            }
            for (int p = 0; p <= CORPUS.length(); p++) {
                assertEquals(CORPUS.codePointCount(0, p), rope.rangeSummary(M3RopeMeasures.CODE_POINTS, 0, p)[0]);
            }
            for (int k = total - 1, at = rope.length(); k >= 0; k--) {
                at = Character.offsetByCodePoints(rope, at, -1);
                assertEquals(expected[k], Character.codePointAt(rope, at));
            }
        }
    }

    @Test
    void appendPrependInsertChunks() {
        List<String> ref = chunks(1000);
        String expected = String.join("", ref);
        M3RopeArena arena = textArena(64);
        M3MeasuredRope appended = arena.empty();
        for (String c : ref) {
            appended = appended.concat(c);
            appended.verifyInvariants();
        }
        assertMatchesOracle(appended, expected);
        M3MeasuredRope prepended = arena.empty();
        for (int i = ref.size() - 1; i >= 0; i--) {
            prepended = prepended.insert(0, ref.get(i));
            prepended.verifyInvariants();
        }
        assertMatchesOracle(prepended, expected);
        int[] order = shuffled(ref.size(), 0);
        M3MeasuredRope inserted = arena.empty();
        StringBuilder oracle = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            int position = 0;
            for (int j = 0; j < i; j++) if (order[j] < order[i]) position += ref.get(order[j]).length();
            inserted = inserted.insert(position, ref.get(order[i]));
            oracle.insert(position, ref.get(order[i]));
            inserted.verifyInvariants();
            assertEquals(oracle.length(), inserted.length());
        }
        assertMatchesOracle(inserted, expected);
    }

    @Test
    void removeShuffledChunks() {
        List<String> ref = chunks(1000);
        M3MeasuredRope rope = textArena(64).ofChunks(ref.toArray(new String[0]));
        StringBuilder oracle = new StringBuilder(String.join("", ref));
        boolean[] removed = new boolean[ref.size()];
        int[] order = shuffled(ref.size(), 0);
        SplittableRandom random = new SplittableRandom(1);
        for (int i = 0; i < order.length; i++) {
            int value = order[i];
            int offset = 0;
            for (int j = 0; j < value; j++) if (!removed[j]) offset += ref.get(j).length();
            int len = ref.get(value).length();
            if (i % 2 == 1) {
                int position = offset + random.nextInt(len);
                int covering = 0;
                int seen = 0;
                for (int j = 0; j < ref.size(); j++) {
                    if (removed[j]) continue;
                    if (position < seen + ref.get(j).length()) {
                        covering = j;
                        break;
                    }
                    seen += ref.get(j).length();
                }
                assertEquals(value, covering, "position form selects the covering chunk");
            }
            assertEquals(ref.get(value), rope.slice(offset, offset + len).toString());
            rope = rope.remove(offset, offset + len);
            oracle.delete(offset, offset + len);
            removed[value] = true;
            assertEquals(oracle.toString(), rope.toString());
            if (i % 50 == 0) rope.verifyInvariants();
        }
        assertMatchesOracle(rope, "");
    }

    @Test
    void randomAdjacentJoins() {
        int c = 20_000;
        List<String> ref = chunks(c);
        String full = String.join("", ref);
        int[] prefix = new int[c + 1];
        for (int i = 0; i < c; i++) prefix[i + 1] = prefix[i] + ref.get(i).length();
        M3RopeArena arena = textArena(16);
        List<M3MeasuredRope> trees = new ArrayList<>();
        List<int[]> ranges = new ArrayList<>();
        for (int i = 0; i < c; i++) {
            trees.add(arena.of(ref.get(i)));
            ranges.add(new int[] {i, i + 1});
        }
        SplittableRandom random = new SplittableRandom(0);
        for (int step = 0; trees.size() >= 2; step++) {
            int i = random.nextInt(trees.size() - 1);
            M3MeasuredRope a = trees.get(i);
            M3MeasuredRope b = trees.remove(i + 1);
            int[] range = {ranges.get(i)[0], ranges.remove(i + 1)[1]};
            M3MeasuredRope joined = a.concat(b);
            if (step % 97 == 0 || trees.size() < 4) {
                joined.verifyInvariants();
                a.verifyInvariants();
                b.verifyInvariants();
                int mid = ranges.get(i)[1];
                assertEquals(full.substring(prefix[range[0]], prefix[mid]), a.toString());
                assertEquals(full.substring(prefix[mid], prefix[range[1]]), b.toString());
                assertEquals(full.substring(prefix[range[0]], prefix[range[1]]), joined.toString());
            }
            trees.set(i, joined);
            ranges.set(i, range);
        }
        assertMatchesOracle(trees.get(0), full);
    }

    @Test
    void removeSubrangeSimpleAndLarger() {
        checkRanges(10, REMOVE_SIMPLE, new int[] {0});
        checkRanges(100, REMOVE_LARGER, new int[] {0});
    }

    @Test
    void replaceSubrangeSimple() {
        checkRanges(10, REMOVE_SIMPLE, new int[] {0, 1, 10, 100});
        M3RopeArena arena = textArena(3);
        for (int len : new int[] {0, 1, 10, 100}) {
            M3MeasuredRope rope = arena.empty().replace(0, 0, "~".repeat(len));
            rope.verifyInvariants();
            assertMatchesOracle(rope, "~".repeat(len));
        }
    }

    private static void checkRanges(int chunkCount, int[] ranges, int[] replacementSizes) {
        for (int maxLeaf : new int[] {10, 3}) {
            M3RopeArena arena = textArena(maxLeaf);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < chunkCount; i++) parts.add(chunk(i, 10));
            M3MeasuredRope rope = arena.ofChunks(parts.toArray(new String[0]));
            String flat = String.join("", parts);
            for (int size : replacementSizes) {
                String replacement = "~".repeat(size);
                for (int r = 0; r < ranges.length; r += 2) {
                    M3MeasuredRope edited = rope.replace(ranges[r], ranges[r + 1], replacement);
                    edited.verifyInvariants();
                    assertMatchesOracle(edited, new StringBuilder(flat).replace(ranges[r], ranges[r + 1], replacement).toString());
                    assertMatchesOracle(rope, flat);
                }
            }
        }
    }

    @Test
    void removeSubrangeRandom() {
        for (int seed = 0; seed < 20; seed++) {
            SplittableRandom random = new SplittableRandom(seed);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < 1000; i++) parts.add(chunk(i, 2));
            M3MeasuredRope rope = textArena(seed % 2 == 0 ? 2 : 5).ofChunks(parts.toArray(new String[0]));
            StringBuilder oracle = new StringBuilder(String.join("", parts));
            while (oracle.length() > 0) {
                int i = random.nextInt(oracle.length());
                int j = i + 1 + random.nextInt(oracle.length() - i);
                rope = rope.remove(i, j);
                oracle.delete(i, j);
                assertMatchesOracle(rope, oracle.toString());
            }
            rope.verifyInvariants();
        }
    }

    @Test
    void equalityIsCodeUnitEquality() {
        M3RopeArena arena = textArena(2);
        String decomposed = "Cafe" + (char) 0x301;
        String precomposed = "Caf" + (char) 0xE9;
        M3MeasuredRope a = arena.of(decomposed);
        M3MeasuredRope b = arena.of(precomposed);
        assertFalse(a.contentEquals(b));
        assertFalse(decomposed.equals(precomposed));
        assertNotEquals(a.summary(M3RopeMeasures.HASH)[0], b.summary(M3RopeMeasures.HASH)[0]);
        assertEquals(5, a.summary(M3RopeMeasures.CODE_POINTS)[0]);
        assertEquals(4, b.summary(M3RopeMeasures.CODE_POINTS)[0]);
        assertMatchesOracle(a, decomposed);
        assertMatchesOracle(b, precomposed);
    }

    @Test
    void sampleConversion() {
        for (int maxLeaf : new int[] {1, 2, 5, 64, 100_000}) {
            M3MeasuredRope rope = textArena(maxLeaf).of(CORPUS);
            rope.verifyInvariants();
            assertMatchesOracle(rope, CORPUS);
        }
    }

    @Test
    void utf8PrefixAtEveryBoundary() {
        M3MeasuredRope rope = textArena(5).of(CORPUS);
        for (int p = 0; p <= CORPUS.length(); p++) {
            assertEquals(CORPUS.substring(0, p).getBytes(StandardCharsets.UTF_8).length,
                    rope.rangeSummary(M3RopeMeasures.UTF8_LENGTH, 0, p)[0], "prefix " + p);
        }
    }

    @Test
    void hashUnderEverySplit() {
        String[] classes = {
            "Cafe" + (char) 0x301, "Caf" + (char) 0xE9, "Foo" + (char) 0x301 + (char) 0x327,
            "Foo" + (char) 0x327 + (char) 0x301, "Foo;bar", "Foo" + (char) 0x37E + "bar"
        };
        M3RopeArena arena = textArena(2);
        for (String s : classes) {
            for (int split = 0; split <= s.length(); split++) {
                M3MeasuredRope rope = arena.of(s.substring(0, split)).concat(s.substring(split));
                assertEquals(s.hashCode(), (int) rope.summary(M3RopeMeasures.HASH)[0]);
            }
        }
        String a400 = "a".repeat(400);
        M3MeasuredRope big = textArena(7).of(a400);
        assertArrayEquals(big.summary(M3RopeMeasures.HASH), big.slice(0, 400).summary(M3RopeMeasures.HASH));
        assertArrayEquals(big.summary(M3RopeMeasures.HASH), big.rangeSummary(M3RopeMeasures.HASH, 0, 400));
        assertEquals(a400.hashCode(), (int) big.summary(M3RopeMeasures.HASH)[0]);
        assertArrayEquals(big.slice(13, 377).summary(M3RopeMeasures.HASH), big.rangeSummary(M3RopeMeasures.HASH, 13, 377));
    }

    @Test
    void appendByCodePointStrides() {
        for (int stride : new int[] {1, 2, 4, 8, 16, 32, 64, 128, 250, 1000, 10000, 20000}) {
            M3RopeArena arena = textArena(64);
            M3MeasuredRope strings = arena.empty();
            M3MeasuredRope ropes = arena.empty();
            for (String piece : piecesByCodePoints(CORPUS, stride)) {
                strings = strings.concat(piece);
                M3MeasuredRope p = arena.of(piece);
                if (stride >= 16) p.verifyInvariants();
                ropes = ropes.concat(p);
            }
            for (M3MeasuredRope rope : List.of(strings, ropes)) {
                rope.verifyInvariants();
                assertMatchesOracle(rope, CORPUS);
            }
        }
    }

    @Test
    void appendKeepsSnapshots() {
        String flat = CORPUS.repeat(3);
        for (int stride : new int[] {32, 64, 128, 250, 1000}) {
            M3MeasuredRope rope = textArena(64).empty();
            int i = 0;
            for (String piece : piecesByCodePoints(flat, stride)) {
                M3MeasuredRope copy = rope;
                String expected = copy.toString();
                rope = rope.concat(piece);
                assertEquals(expected, copy.toString());
                if (i++ % 10 == 0) copy.verifyInvariants();
            }
            assertMatchesOracle(rope, flat);
        }
    }

    @Test
    void insertShuffledPieces() {
        insertShuffled(new int[] {1, 2, 4, 8, 16, 32, 64, 128, 250, 1000, 10000, 20000}, false);
        insertShuffled(new int[] {64, 128, 250, 256, 257, 500, 512, 513, 1000, 2000, 10000, 20000}, true);
    }

    private static void insertShuffled(int[] strides, boolean ropePieces) {
        for (int stride : strides) {
            List<String> pieces = piecesByCodePoints(CORPUS, stride);
            int[] order = shuffled(pieces.size(), 0);
            M3RopeArena arena = textArena(32);
            M3MeasuredRope rope = arena.empty();
            StringBuilder oracle = new StringBuilder();
            for (int i = 0; i < order.length; i++) {
                int offset = 0;
                for (int j = 0; j < i; j++) if (order[j] < order[i]) offset += pieces.get(order[j]).length();
                String piece = pieces.get(order[i]);
                rope = ropePieces ? rope.insert(offset, arena.of(piece)) : rope.insert(offset, piece);
                oracle.insert(offset, piece);
                if (i % 20 == 0) {
                    rope.verifyInvariants();
                    assertMatchesOracle(rope, oracle.toString());
                }
            }
            rope.verifyInvariants();
            assertMatchesOracle(rope, CORPUS);
        }
    }

    @Test
    void replacePlaceholders() {
        for (int stride : new int[] {1, 2, 4, 8, 16, 32, 64, 128, 250, 1000, 10000, 20000}) {
            List<String> pieces = piecesByCodePoints(CORPUS, stride);
            int[] order = shuffled(pieces.size(), 0);
            int interval = Math.max(pieces.size() / 25, 25);
            M3MeasuredRope rope = textArena(16).of(" ".repeat(pieces.size()));
            StringBuilder oracle = new StringBuilder(" ".repeat(pieces.size()));
            for (int i = 0; i < order.length; i++) {
                int offset = order[i];
                for (int j = 0; j < i; j++) if (order[j] < order[i]) offset += pieces.get(order[j]).length() - 1;
                rope = rope.replace(offset, offset + 1, pieces.get(order[i]));
                oracle.replace(offset, offset + 1, pieces.get(order[i]));
                if (i % interval == 0) {
                    rope.verifyInvariants();
                    assertMatchesOracle(rope, oracle.toString());
                }
            }
            rope.verifyInvariants();
            assertMatchesOracle(rope, CORPUS);
        }
    }

    @Test
    void sliceAtCodePointBoundaries() {
        M3MeasuredRope rope = textArena(9).of(CORPUS);
        List<Integer> bounds = new ArrayList<>();
        for (int p = 0; p <= CORPUS.length(); p++) if (onCodePoint(CORPUS, p) && p % 37 == 0) bounds.add(p);
        bounds.add(CORPUS.length());
        for (int i = 0; i < bounds.size(); i++) {
            for (int j = i; j < bounds.size(); j++) {
                int from = bounds.get(i);
                int to = bounds.get(j);
                M3MeasuredRope slice = rope.slice(from, to);
                assertMatchesOracle(slice, CORPUS.substring(from, to));
                for (M3RopeMeasure m : M3RopeMeasures.text()) {
                    assertArrayEquals(slice.summary(m), rope.rangeSummary(m, from, to));
                }
            }
        }
    }

    @Test
    void codePointRounding() {
        M3MeasuredRope rope = textArena(3).of(CORPUS);
        int n = CORPUS.length();
        int total = CORPUS.codePointCount(0, n);
        for (int p = 0; p <= n; p++) {
            boolean mid = !onCodePoint(CORPUS, p);
            int k = (int) rope.rangeSummary(M3RopeMeasures.CODE_POINTS, 0, p)[0];
            int up = k == total ? n : rope.seek(M3RopeMeasures.CODE_POINTS, k) - 1;
            int down = up == p ? p : rope.seek(M3RopeMeasures.CODE_POINTS, k - 1) - 1;
            assertEquals(mid ? p + 1 : p, up, "round up " + p);
            assertEquals(mid ? p - 1 : p, down, "round down " + p);
        }
    }

    @Test
    void surrogateRepairOnEdit() {
        M3RopeArena arena = textArena(2);
        String high = String.valueOf((char) 0xD83D);
        String low = String.valueOf((char) 0xDE00);
        M3MeasuredRope lone = arena.of("Foo" + high);
        assertMatchesOracle(lone, "Foo" + high);
        M3MeasuredRope paired = lone.insert(4, low).concat("bar");
        paired.verifyInvariants();
        assertMatchesOracle(paired, "Foo" + high + low + "bar");
        assertEquals(7, paired.summary(M3RopeMeasures.CODE_POINTS)[0]);
        StringBuilder flags = new StringBuilder();
        flags.appendCodePoint(0x1F1FA).appendCodePoint(0x1F1F8).appendCodePoint(0x1F1E8).appendCodePoint(0x1F1E6);
        String t = flags.toString();
        for (int cut = 0; cut <= t.length(); cut++) {
            M3MeasuredRope left = arena.of(t.substring(0, cut));
            assertMatchesOracle(left, t.substring(0, cut));
            M3MeasuredRope joined = left.concat(t.substring(cut));
            assertMatchesOracle(joined, t);
            M3MeasuredRope reinserted = joined.remove(cut, Math.min(t.length(), cut + 1));
            reinserted = reinserted.insert(cut, t.substring(cut, Math.min(t.length(), cut + 1)));
            reinserted.verifyInvariants();
            assertMatchesOracle(reinserted, t);
        }
    }

    @Test
    void combiningRunsAcrossLeaves() {
        M3RopeArena arena = textArena(5);
        for (int x = 0; x < 300; x++) {
            String s = "a" + String.valueOf((char) 0x301).repeat(x);
            M3MeasuredRope rope = arena.of(s);
            assertEquals(1 + x, rope.summary(M3RopeMeasures.CODE_POINTS)[0]);
            assertEquals(1 + 2L * x, rope.summary(M3RopeMeasures.UTF8_LENGTH)[0]);
            if (x % 50 == 0) rope.verifyInvariants();
        }
        String mark = "a" + (char) 0x301 + (char) 0x302;
        for (int maxLeaf : new int[] {4, 5}) {
            for (int x = 0; x < 5; x++) {
                String s = mark.repeat(64) + "x".repeat(x);
                M3MeasuredRope rope = textArena(maxLeaf).of(s);
                rope.verifyInvariants();
                assertMatchesOracle(rope, s);
                for (int a = 0; a <= s.length(); a++) {
                    for (int b = a; b <= s.length(); b++) {
                        assertEquals(s.codePointCount(a, b), rope.rangeSummary(M3RopeMeasures.CODE_POINTS, a, b)[0]);
                    }
                }
            }
        }
    }

    @Test
    void flagsSplitAtEveryUnit() {
        StringBuilder flags = new StringBuilder();
        flags.appendCodePoint(0x1F1E8).appendCodePoint(0x1F1E6).appendCodePoint(0x1F1FA).appendCodePoint(0x1F1F8);
        M3RopeArena arena = textArena(3);
        for (int x = 0; x < 5; x++) {
            String s = flags.toString().repeat(16) + "x".repeat(x);
            int total = s.codePointCount(0, s.length());
            for (int split = 0; split <= s.length(); split++) {
                M3MeasuredRope rope = arena.of(s.substring(0, split)).concat(s.substring(split));
                assertMatchesOracle(rope, s);
                if (split != 31) continue;
                rope.verifyInvariants();
                for (int a = 0; a <= s.length(); a++) {
                    for (int b = a; b <= s.length(); b++) {
                        assertEquals(s.codePointCount(a, b), rope.rangeSummary(M3RopeMeasures.CODE_POINTS, a, b)[0]);
                    }
                }
                for (int k = 0; k <= total; k++) {
                    int offset = k == total ? s.length() : rope.seek(M3RopeMeasures.CODE_POINTS, k) - 1;
                    assertEquals(s.offsetByCodePoints(0, k), offset);
                }
            }
        }
    }
}
