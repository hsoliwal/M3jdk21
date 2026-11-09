// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jdk.m3.precompute.EditDistancePlan.Frontier;
import jdk.m3.precompute.EditDistancePlan.FrontierLimits;
import org.junit.jupiter.api.Test;

/**
 * Real-owner checks of the exact incremental frontier, ported from the donor MIndexDistanceFrontierChecks (com.synexia
 * develop 78cf2ad4c7e5, java mode) for census3 P3-010. The direct-buffer and JNI sections of the donor are held
 * (appendDirect/FrontierBackend: HOLD_DEP). Every member used here is absent on master.
 */
//FLLDCIM: MRL - census3 P3-010 donor test port (recipe/census3-gaps-20261107)
class DistanceFrontierTest {

    @Test
    void exhaustiveShortPairsEveryCut() {
        List<String> words = new ArrayList<>();
        enumerate(words, "", 3);
        int pairs = 0;
        for (String pattern : words) {
            for (String value : words) {
                checkCase(pattern, value, true);
                pairs++;
            }
        }
        assertEquals(85 * 85, pairs);
    }

    @Test
    void seededPairsAndChunkBoundaries() {
        Random random = new Random(0x4d334450L);
        for (int trial = 0; trial < 2000; trial++) {
            String p = randomText(random, random.nextInt(97));
            String v = randomText(random, random.nextInt(129));
            checkCase(p, v, false);
        }
        checkCase("x".repeat(4097), "x".repeat(15) + "𐀀", false);
        checkCase("", "x".repeat(4097), false);
        checkCase("a", "a".repeat(2049), false);
        checkCase("a".repeat(65), "a".repeat(1023) + "𐀀", false);
        checkCase("a".repeat(1025), "a".repeat(255) + "𐀀", false);
    }

    @Test
    void budgetsAndPurity() {
        EditDistancePlan plan = EditDistancePlan.compile("abcde");
        Frontier bounded = plan.frontier(new FrontierLimits(8, 40, 48), null);
        assertEquals(8, bounded.append("abcdefgh").inputUnits(), "exact budget admitted");
        Frontier prefix = bounded.append("abcd");
        assertThrows(IllegalArgumentException.class, () -> prefix.append("efghi"));
        Frontier cells = plan.frontier(new FrontierLimits(8, 39, 48), null).append("abcd");
        assertThrows(IllegalArgumentException.class, () -> cells.append("efgh"));
        Frontier tight = plan.frontier(new FrontierLimits(8, 40, 24), null);
        assertThrows(IllegalArgumentException.class, () -> tight.append("a"));
        assertSame(tight, tight.append(""), "empty append needs no workspace");
        assertThrows(IllegalArgumentException.class, () -> plan.frontier(new FrontierLimits(8, 40, 23), null));
        assertThrows(IllegalArgumentException.class, () -> new FrontierLimits(Integer.MAX_VALUE, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new FrontierLimits(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> bounded.within(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bounded.append("a", 1, 0, null));
        assertThrows(NullPointerException.class, () -> bounded.append(null));
        assertThrows(NullPointerException.class, () -> plan.frontier(null, null));
        assertSame(plan, bounded.plan());
        assertEquals(new FrontierLimits(8, 40, 48), bounded.limits());
        assertEquals(FrontierLimits.DEFAULT, plan.frontier().limits());
        SpyChars spy = new SpyChars("!abc?", 1, 4);
        assertArrayEquals(oracleRow("abcde", "abc"), plan.frontier().append(spy, 1, 4, null).rowCopy());
        assertEquals(3, spy.reads, "one charAt per selected unit");
        SpyChars noReads = new SpyChars("abcdefghi", 0, 0);
        assertThrows(IllegalArgumentException.class, () -> bounded.append(noReads));
        assertEquals(0, noReads.reads, "budget before payload read");
        int[] initial = bounded.rowCopy();
        SpyChars exploding = new SpyChars("abc", 0, 1);
        assertThrows(IllegalStateException.class, () -> bounded.append(exploding));
        assertArrayEquals(initial, bounded.rowCopy(), "callback failure rollback");
        CountMonitor progress = new CountMonitor(Long.MAX_VALUE, Integer.MAX_VALUE);
        bounded.append("abc", progress);
        assertEquals(3, progress.work, "exact reported input units");
        assertEquals(0, progress.lifecycle, "caller owns lifecycle");
        // A scalar prefix score does not determine the next row.
        Frontier left = EditDistancePlan.compile("ab").frontier();
        assertEquals(left.append("a").distance(), left.append("b").distance());
        assertNotEquals(left.append("a").append("b").distance(), left.append("b").append("b").distance(),
                "frontier, not scalar, must be retained");
    }

    @Test
    void minimumPossibleDistanceIsAnExactSuffixBound() {
        EditDistancePlan plan = EditDistancePlan.compile("kitten");
        Frontier prefix = plan.frontier().append("sit");
        assertThrows(IllegalArgumentException.class, () -> prefix.minimumPossibleDistance(-1));
        for (int remaining = 0; remaining <= 9; remaining++) {
            long bound = prefix.minimumPossibleDistance(remaining);
            int best = Integer.MAX_VALUE;
            for (String suffix : suffixes("kitengx", remaining)) {
                best = Math.min(best, prefix.append(suffix).distance());
            }
            assertEquals(best, bound, "remaining=" + remaining);
        }
        assertEquals(3 + 10 - 6, prefix.minimumPossibleDistance(10));
    }

    @Test
    void cancellation() {
        EditDistancePlan plan = EditDistancePlan.compile("x".repeat(4097));
        Frontier origin = plan.frontier();
        int[] saved = origin.rowCopy();
        CountMonitor already = new CountMonitor(0, 0);
        assertThrows(CancellationException.class, () -> origin.append(new PoisonChars(), already));
        assertThrows(CancellationException.class, () -> plan.frontier(FrontierLimits.DEFAULT, already));
        CountMonitor middle = new CountMonitor(Long.MAX_VALUE, 15);
        assertThrows(CancellationException.class, () -> origin.append("x".repeat(40), middle));
        assertArrayEquals(saved, origin.rowCopy(), "mid-row cancellation leaves origin");
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> origin.append(""));
        } finally {
            Thread.interrupted();
        }
        assertEquals(0, origin.append("").inputUnits());
    }

    @Test
    void concurrentForks() throws Exception {
        Frontier prefix = EditDistancePlan.compile("base-𐀀-tail").frontier().append("base-");
        List<Future<int[]>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            for (int i = 0; i < 120; i++) {
                String suffix = "𐀀-" + i;
                futures.add(executor.submit(() -> prefix.append(suffix).rowCopy()));
            }
            for (int i = 0; i < futures.size(); i++) {
                assertArrayEquals(oracleRow("base-𐀀-tail", "base-𐀀-" + i), futures.get(i).get());
            }
        } finally {
            executor.shutdown();
        }
    }

    private static void checkCase(String pattern, String value, boolean everyCut) {
        EditDistancePlan plan = EditDistancePlan.compile(pattern);
        int[] expected = oracleRow(pattern, value);
        Frontier origin = plan.frontier();
        int[] initial = origin.rowCopy();
        Frontier whole = origin.append(value);
        assertArrayEquals(expected, whole.rowCopy(), "whole frontier");
        assertEquals(plan.distance(value), whole.distance(), "legacy exact distance");
        assertEquals(value.length(), whole.inputUnits(), "unit count");
        assertEquals((long) value.length() * Math.max(1, pattern.length()), whole.cellUpdates(), "cell count");
        assertSame(plan, whole.plan(), "exact query owner");
        assertEquals(4L * (pattern.length() + 1), whole.primitivePayloadBytes(), "retained row bytes");
        assertTrue(whole.within(whole.distance()), "within inclusive");
        if (whole.distance() > 0) assertFalse(whole.within(whole.distance() - 1), "within exclusive");
        int step = everyCut ? 1 : Math.max(1, value.length() / 3);
        for (int cut = 0; cut <= value.length(); cut += step) {
            Frontier prefix = origin.append(value, 0, cut, null);
            int[] saved = prefix.rowCopy();
            Frontier resumed = prefix.append(value, cut, value.length(), null);
            assertArrayEquals(expected, resumed.rowCopy(), "partition invariant");
            assertArrayEquals(saved, prefix.rowCopy(), "prefix immutable");
            assertArrayEquals(oracleRow(pattern, value.substring(0, cut) + "!"), prefix.append("!").rowCopy(),
                    "branch independent");
            assertTrue(prefix.minimumPossibleDistance(value.length() - cut) <= whole.distance(), "suffix bound");
        }
        assertArrayEquals(initial, origin.rowCopy(), "origin immutable");
        int[] escaped = whole.rowCopy();
        escaped[0] = -123;
        assertArrayEquals(expected, whole.rowCopy(), "defensive snapshot");
        assertArrayEquals(expected, EditDistancePlan.compile(pattern, false).frontier().append(value).rowCopy(),
                "legacy false flag remains UTF-16");
    }

    private static List<String> suffixes(String alphabet, int length) {
        List<String> out = new ArrayList<>(List.of(""));
        for (int i = 0; i < length; i++) {
            List<String> next = new ArrayList<>();
            for (String prefix : out) for (char unit : alphabet.toCharArray()) next.add(prefix + unit);
            out = next;
        }
        return out;
    }

    private static int[] oracleRow(String pattern, String value) {
        int[][] table = new int[value.length() + 1][pattern.length() + 1];
        for (int i = 0; i <= value.length(); i++) table[i][0] = i;
        for (int j = 0; j <= pattern.length(); j++) table[0][j] = j;
        for (int i = 1; i <= value.length(); i++) {
            for (int j = 1; j <= pattern.length(); j++) {
                int diagonal = table[i - 1][j - 1];
                if (value.charAt(i - 1) != pattern.charAt(j - 1)) diagonal++;
                table[i][j] = Math.min(diagonal, 1 + Math.min(table[i - 1][j], table[i][j - 1]));
            }
        }
        return table[value.length()];
    }

    private static void enumerate(List<String> out, String prefix, int depth) {
        out.add(prefix);
        if (depth > 0) {
            for (char unit : new char[] {'a', 'b', '\ud800', '\udc00'}) enumerate(out, prefix + unit, depth - 1);
        }
    }

    private static String randomText(Random random, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = (char) random.nextInt(65536);
        return new String(chars);
    }

    private static final class SpyChars implements CharSequence {
        private final String source;
        private final int from;
        private final int to;
        private int reads;

        SpyChars(String source, int from, int to) {
            this.source = source;
            this.from = from;
            this.to = to;
        }

        @Override public int length() { return source.length(); }

        @Override public char charAt(int index) {
            reads++;
            if (index < from || index >= to) throw new IllegalStateException("read beyond selected interval");
            return source.charAt(index);
        }

        @Override public CharSequence subSequence(int start, int end) { throw new AssertionError("substring forbidden"); }

        @Override public String toString() { throw new AssertionError("flatten forbidden"); }
    }

    private static final class PoisonChars implements CharSequence {
        @Override public int length() { throw new AssertionError("length after cancellation"); }

        @Override public char charAt(int index) { throw new AssertionError("read after cancellation"); }

        @Override public CharSequence subSequence(int start, int end) { throw new AssertionError("substring"); }
    }

    private static final class CountMonitor implements Progress {
        private long work;
        private int lifecycle;
        private int checks;
        private final long cancelAtWork;
        private final int cancelAtCheck;

        CountMonitor(long work, int check) {
            cancelAtWork = work;
            cancelAtCheck = check;
        }

        @Override public void checkCanceled() {
            if (++checks >= cancelAtCheck || work >= cancelAtWork) {
                throw new CancellationException("fixture cancellation");
            }
        }

        @Override public void begin(String task, long totalWork) { lifecycle++; }

        @Override public void done() { lifecycle++; }

        @Override public void worked(long delta) { work += delta; }
    }
}
