/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import jdk.m3.parser.M3File;

/** Bounded exact-delta geometry, adversarial refusal and differential reconstruction. */
public final class M3FileHunksContract {
    private static final URI URI = java.net.URI.create("memory:/multi-hunk-source.java");
    private static int checks;
    private static int trials;

    private M3FileHunksContract() { }

    public static void main(String[] args) throws Exception {
        separateEditsAndIndependentSourceCoordinates();
        identicalAndRawDigestTruth();
        fallbackAndBudgetRefusal();
        unicodeCrLfAndInvalidSurrogateBoundaries();
        reorderedAndRepeatedLines();
        seededDifferentialReconstruction();
        System.out.println("M3_FILE_HUNKS_PASS checks=" + checks + " trials=" + trials);
    }

    private static M3File source(String value) { return M3File.of(URI, value); }

    private static M3File utf8(String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return M3File.read(URI, new ByteArrayInputStream(bytes), StandardCharsets.UTF_8, bytes.length);
    }

    private static void separateEditsAndIndependentSourceCoordinates() throws Exception {
        String first = "head\nretained-α-000000000\nA old\nretained-β-111111111\nB old\nretained-γ-222222222\ntail\n";
        String second = "head\nretained-α-000000000\nA new and expanded\nretained-β-111111111\nB new\nretained-γ-222222222\ntail\n";
        M3File old = utf8(first), now = utf8(second);
        M3File.SourceHunks sourceHunks = now.changeHunksSince(old, 10000, 100, 10);
        check(sourceHunks.hunkCount() == 2 && !sourceHunks.coarse(), "two independent changes separated by exact anchors");
        assertReconstruct(old.text(), now.text(), sourceHunks);
        M3File.Utf8Hunks byteHunks = now.utf8ChangeHunksSince(old, 10000, 100, 10);
        check(byteHunks.hunkCount() == 2 && !byteHunks.coarse(), "original UTF-8 geometry preserves two hunks");
        check(byteHunks.rawDigestComparison() == M3File.RawDigestComparison.SHA256_DIFFERENT,
                "raw byte identity remains separate from source text");
        check(sourceHunks.primitiveBytes() == 32 && byteHunks.primitiveBytes() == 32,
                "two parent-owned four-int lanes, without retained per-hunk objects");
        for (int i = 0; i < byteHunks.hunkCount(); i++) {
            M3File.Utf8Hunk bytes = byteHunks.hunk(i);
            M3File.ChangeHunk units = sourceHunks.hunk(i);
            check(bytes.previousStartByte() == old.utf8Offsets().orElseThrow().toByte(units.previousStart()),
                    "old byte coordinate comes from old original UTF-8 generation");
            check(bytes.currentStartByte() == now.utf8Offsets().orElseThrow().toByte(units.currentStart()),
                    "new byte coordinate comes from new original UTF-8 generation");
            check(bytes.previousEndByte() == old.utf8Offsets().orElseThrow().toByte(units.previousEnd())
                    && bytes.currentEndByte() == now.utf8Offsets().orElseThrow().toByte(units.currentEnd()),
                    "both generation ends retain exact UTF-8 scalar boundaries");
        }
        check(byteHunks.hunk(1).previousStartByte() != byteHunks.hunk(1).currentStartByte(),
                "later hunk has shifted generation-absolute offsets; never pass directly as TSInputEdit");
    }

    private static void identicalAndRawDigestTruth() throws Exception {
        M3File old = utf8("same\n😀\n");
        M3File current = utf8("same\n😀\n");
        M3File.SourceHunks unchanged = current.changeHunksSince(old, 100, 20, 5);
        check(unchanged.hunkCount() == 0 && !unchanged.coarse(), "unchanged UTF-16 has no hunks");
        check(unchanged.rawDigestComparison() == M3File.RawDigestComparison.SHA256_SAME,
                "equal independently read original bytes are reported as SHA256_SAME");
        check(current.utf8ChangeHunksSince(old, 100, 20, 5).hunkCount() == 0,
                "byte-domain corresponding hunks are also empty");
        check(source("same\n😀\n").changeHunksSince(old, 100, 20, 5).rawDigestComparison()
                == M3File.RawDigestComparison.UNAVAILABLE,
                "character-only input never fabricates a raw byte digest");
    }

    private static void fallbackAndBudgetRefusal() throws Exception {
        M3File old = source("x\nunique-111\nfirst\nunique-222\nsecond\nunique-333\nz\n");
        M3File now = source("x\nunique-111\nFIRST\nunique-222\nSECOND\nunique-333\nz\n");
        M3File.SourceHunks bounded = now.changeHunksSince(old, 200, 30, 1);
        M3File.ChangeEnvelope original = now.changeSince(old, 200);
        check(bounded.coarse() && bounded.hunkCount() == 1, "hunk count exhaustion falls back to old envelope");
        M3File.ChangeHunk fallback = bounded.hunk(0);
        check(fallback.previousStart() == original.previousStart()
                && fallback.previousEnd() == original.previousEnd()
                && fallback.currentStart() == original.currentStart()
                && fallback.currentEnd() == original.currentEnd(), "coarse fallback is original bounded algorithm");
        assertReconstruct(old.text(), now.text(), bounded);
        expect(IllegalArgumentException.class, () -> now.changeHunksSince(old, 1, 30, 5));
        expect(IllegalArgumentException.class, () -> now.changeHunksSince(old, 200, 0, 5));
        expect(IllegalArgumentException.class, () -> now.changeHunksSince(old, 200, 30, 0));
        expect(IllegalArgumentException.class, () -> now.changeHunksSince(old, 200, 30, Integer.MAX_VALUE));
        expect(IllegalArgumentException.class, () -> now.changeHunksSince(old, 200, 1, 10));
        expect(IllegalArgumentException.class, () -> now.changeHunksSince(M3File.of(java.net.URI.create("memory:/other"), "x"), 200, 30, 10));
        expect(IllegalArgumentException.class, () -> now.utf8ChangeHunksSince(old, 200, 30, 10));
        expect(IndexOutOfBoundsException.class, () -> bounded.hunk(1));
    }

    private static void unicodeCrLfAndInvalidSurrogateBoundaries() throws Exception {
        for (String[] pair : new String[][] {
                {"before\r\n😀 end\r\nfixed-line\r\nstop", "before\r\n😁 end\r\nfixed-line\r\nstop"},
                {"😀🐱\nzero\nend", "😁🐱\nzero\nend"},
                {"\uD83D\uDE00\nline\n", "\uD83D\uDE01\nline\n"},
                {"\uD83D!\nline\n", "\uD83D?\nline\n"},
                {"\n", "\r\n"},
                {"", "😀"},
                {"😀", ""}
        }) {
            M3File before = source(pair[0]), after = source(pair[1]);
            M3File.SourceHunks changes = after.changeHunksSince(before, 1024, 100, 10);
            assertReconstruct(before.text(), after.text(), changes);
        }
        M3File old = utf8("😀\r\nα\nend"), now = utf8("😁\r\nβ\nend");
        M3File.SourceHunks units = now.changeHunksSince(old, 1024, 100, 10);
        assertReconstruct(old.text(), now.text(), units);
        M3File.Utf8Hunks bytes = now.utf8ChangeHunksSince(old, 1024, 100, 10);
        check(bytes.hunkCount() == units.hunkCount(), "Unicode UTF-8 byte lanes preserve hunk count");
        for (int i = 0; i < bytes.hunkCount(); i++) {
            M3File.Utf8Hunk hunk = bytes.hunk(i);
            check(old.utf8Offsets().orElseThrow().toUtf16(hunk.previousStartByte()) == units.hunk(i).previousStart(),
                    "old UTF-8 range start is valid exact UTF-16 scalar boundary");
            check(now.utf8Offsets().orElseThrow().toUtf16(hunk.currentEndByte()) == units.hunk(i).currentEnd(),
                    "new UTF-8 range end is valid exact UTF-16 scalar boundary");
        }
    }

    private static void reorderedAndRepeatedLines() {
        for (String[] pair : new String[][] {
                {"A\nB\nC\nD\n", "A\nC\nB\nD\n"},
                {"same\nsame\nsame\n", "same\nchanged\nsame\n"},
                {"x\nx\nx\n", "y\nx\nx\n"},
                {"A\nB\nC", "A\nnew\nB\nC"},
                {"A\nB\nC", "A\nC"},
                {"A\r\nB\nC\rD", "A\nB\r\nC\rD"}
        }) {
            M3File before = source(pair[0]), after = source(pair[1]);
            assertReconstruct(before.text(), after.text(),
                    after.changeHunksSince(before, 1000, 100, 100));
        }
    }

    private static void seededDifferentialReconstruction() {
        Random random = new Random(0x3a57beefL);
        String[] tokens = {"", "A", "a", "x\n", "retained\r\n", "😀", "β", "\t", "\n", "\r", "Z"};
        for (int test = 0; test < 4000; test++) {
            StringBuilder before = new StringBuilder(), after = new StringBuilder();
            for (int i = 0; i < 30; i++) {
                String chosen = tokens[random.nextInt(tokens.length)];
                before.append(chosen);
                if (random.nextInt(7) == 0) {
                    int action = random.nextInt(3);
                    if (action == 0) after.append(tokens[random.nextInt(tokens.length)]);
                    else if (action == 1) after.append(chosen).append(tokens[random.nextInt(tokens.length)]);
                } else {
                    after.append(chosen);
                }
            }
            M3File old = source(before.toString()), now = source(after.toString());
            M3File.SourceHunks changes = now.changeHunksSince(old, 4096, 100, 16);
            assertReconstruct(old.text(), now.text(), changes);
            check(changes.hunkCount() <= 16, "strict retained hunk budget");
            trials++;
        }
    }

    private static void assertReconstruct(String before, String after, M3File.SourceHunks changes) {
        int p = 0, c = 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < changes.hunkCount(); i++) {
            M3File.ChangeHunk h = changes.hunk(i);
            check(h.previousStart() >= p && h.currentStart() >= c,
                    "hunks are ordered and nonoverlapping in both generations");
            check(h.previousEnd() <= before.length() && h.currentEnd() <= after.length(),
                    "hunk boundaries are bounded by actual owners");
            check(!insidePair(before, h.previousStart()) && !insidePair(before, h.previousEnd())
                    && !insidePair(after, h.currentStart()) && !insidePair(after, h.currentEnd()),
                    "UTF-16 hunk boundary never splits a surrogate pair");
            String oldUnchanged = before.substring(p, h.previousStart());
            String currentUnchanged = after.substring(c, h.currentStart());
            check(oldUnchanged.equals(currentUnchanged), "disjoint hunk gap is exactly unchanged");
            result.append(oldUnchanged).append(after, h.currentStart(), h.currentEnd());
            p = h.previousEnd();
            c = h.currentEnd();
        }
        check(before.substring(p).equals(after.substring(c)), "unchanged suffix is exactly equal");
        result.append(before, p, before.length());
        check(result.toString().equals(after), "reconstructed source equals actual immutable current generation");
    }

    private static boolean insidePair(String s, int boundary) {
        return boundary > 0 && boundary < s.length()
                && Character.isHighSurrogate(s.charAt(boundary - 1))
                && Character.isLowSurrogate(s.charAt(boundary));
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static <T extends Throwable> void expect(Class<T> kind, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong refusal type: " + failure, failure);
        }
        throw new AssertionError("missing expected refusal: " + kind.getName());
    }
}
