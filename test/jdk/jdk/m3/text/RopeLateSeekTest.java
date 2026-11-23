// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary KMP late-start literal searches seek through existing M3 rope metadata
 * @modules java.base/jdk.m3.text
 * @run main/othervm RopeLateSeekTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.M3RopeTextIndex;
import jdk.m3.text.M3StringPiece;

public class RopeLateSeekTest {
    private static long checks;
    private static final char[] ABC = {0, 'a', 'b', (char) 0xd800, (char) 0xdc00,
            (char) 0xffff, '\n'};

    public static void main(String[] args) {
        emptyAndSurrogateBoundaries();
        manyTreeLevels();
        randomizedMultiLeafOracles();
        System.out.println("PASS RopeLateSeekTest checks=" + checks);
    }

    private static void emptyAndSurrogateBoundaries() {
        LocalM3Arena arena = new LocalM3Arena();
        M3RopeTextIndex rope = M3RopeTextIndex.of(
                arena.copyUtf16(new char[0]),
                arena.copyUtf16(new char[] {'x', 0, (char) 0xd800}),
                arena.copyUtf16(new char[] {(char) 0xdc00, 'b'}),
                arena.copyUtf16(new char[0]),
                arena.copyUtf16(new char[] {'x'}));
        String source = "x\0\ud800\udc00bx";
        check(rope.segmentCount() == 3, "empty leaves are not retained");
        for (String needle : new String[] {"", "x", "x\0", "\ud800\udc00b",
                "\ud800", "\udc00b", "bx", "\uffff"}) {
            check(rope, source, needle);
        }
        M3RopeTextIndex.LiteralPlan none = M3RopeTextIndex.compileLiteral("");
        same(rope.utf16Length(), rope.findPreparedLiteral(none, rope.utf16Length()),
                "empty pattern at end");
        same(-1, rope.findPreparedLiteral(M3RopeTextIndex.compileLiteral("x"),
                rope.utf16Length()), "nonempty at end");
        expect(IndexOutOfBoundsException.class, () -> rope.findLiteral("x", -1));
        expect(IndexOutOfBoundsException.class,
                () -> rope.findPreparedLiteral(M3RopeTextIndex.compileLiteral("x"),
                        rope.utf16Length() + 1));
        M3RopeTextIndex empty = M3RopeTextIndex.of();
        same(0, empty.findLiteral("", 0), "empty source plus empty pattern");
        same(-1, empty.findPreparedLiteral(M3RopeTextIndex.compileLiteral("x"), 0),
                "empty source plus nonempty pattern");
        expect(IndexOutOfBoundsException.class, () -> empty.charAt(0));
    }

    private static void manyTreeLevels() {
        LocalM3Arena arena = new LocalM3Arena();
        int[] amounts = {1, 2, 16, 17, 255, 256, 257, 4095, 4096, 4097, 8193};
        for (int leaves = 0; leaves < amounts.length; leaves++) {
            int count = amounts[leaves];
            M3StringPiece[] parts = new M3StringPiece[count];
            StringBuilder reference = new StringBuilder(count * 3);
            for (int i = 0; i < count; i++) {
                char[] units = new char[] {(char) ('a' + (i % 4)),
                        (char) (i % 3 == 0 ? 0xd800 : 0),
                        (i % 7 == 0 ? 0xdc00 : 'b')};
                parts[i] = arena.copyUtf16(units);
                reference.append(units);
            }
            M3RopeTextIndex rope = M3RopeTextIndex.of(parts);
            String haystack = reference.toString();
            same(haystack.length(), rope.utf16Length(), "multi-level rope length");
            String[] needles = {"", "b", "\0", "\ud800\udc00", "a\ud800",
                    "ab", "\uffff", haystack.isEmpty() ? "z"
                            : haystack.substring(Math.max(0, haystack.length() - 4))};
            for (String needle : needles) {
                check(rope, haystack, needle);
            }
        }
    }

    private static void randomizedMultiLeafOracles() {
        Random random = new Random(0x4d335345454b3231L);
        LocalM3Arena arena = new LocalM3Arena();
        for (int caseId = 0; caseId < 450; caseId++) {
            int total = random.nextInt(460);
            char[] units = new char[total];
            for (int i = 0; i < units.length; i++) {
                units[i] = ABC[caseId % 5 == 0 ? i % 2 : random.nextInt(ABC.length)];
            }
            List<M3StringPiece> leafList = new ArrayList<>();
            for (int offset = 0; offset < units.length;) {
                int next = Math.min(units.length, offset + 1 + random.nextInt(8));
                leafList.add(arena.copyUtf16(Arrays.copyOfRange(units, offset, next)));
                if (random.nextBoolean()) leafList.add(arena.copyUtf16(new char[0]));
                offset = next;
            }
            M3RopeTextIndex rope = M3RopeTextIndex.of(
                    leafList.toArray(M3StringPiece[]::new));
            String reference = new String(units);
            for (int queryId = 0; queryId < 22; queryId++) {
                String needle;
                if (queryId % 3 == 0 && total > 0) {
                    int first = random.nextInt(total);
                    int end = first + random.nextInt(Math.min(18, total - first) + 1);
                    needle = reference.substring(first, end);
                } else {
                    char[] pattern = new char[random.nextInt(18)];
                    for (int i = 0; i < pattern.length; i++) {
                        pattern[i] = ABC[random.nextInt(ABC.length)];
                    }
                    needle = new String(pattern);
                }
                check(rope, reference, needle);
            }
        }
    }

    private static void check(M3RopeTextIndex rope, String corpus, String query) {
        M3RopeTextIndex.LiteralPlan prepared =
                M3RopeTextIndex.compileLiteral(noFlatten(query.toCharArray()));
        long[] positions = {0L, corpus.length() / 4L,
                corpus.length() / 2L, Math.max(0L, corpus.length() - 4L),
                corpus.length()};
        for (long from : positions) {
            long expected = corpus.indexOf(query, (int) from);
            same(expected, rope.findLiteral(query, from),
                    "legacy KMP from " + from + " query " + query.length());
            same(expected, rope.findPreparedLiteral(prepared, from),
                    "prepared KMP from " + from + " query " + query.length());
        }
    }

    private static CharSequence noFlatten(char[] input) {
        char[] copy = input.clone();
        return new CharSequence() {
            @Override public int length() { return copy.length; }
            @Override public char charAt(int index) { return copy[index]; }
            @Override public CharSequence subSequence(int start, int end) {
                throw new AssertionError("must not request a pattern slice");
            }
            @Override public String toString() {
                throw new AssertionError("must not flatten query");
            }
        };
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("unexpected exception " + thrown, thrown);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void same(long want, long got, String reason) {
        checks++;
        if (want != got) throw new AssertionError(reason + " wanted=" + want
                + " got=" + got + " check=" + checks);
    }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason + " check=" + checks);
    }
}
