// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BmhCountOnlyContract.java (blob 54bdb86582fc) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Source-independent byte oracle plus historical Horspool visit-path differential. */
public final class BmhCountOnlyContract {
    private static long checks;

    private BmhCountOnlyContract() { }

    private static void require(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static final class Poll implements M3ProgressMonitor {
        final int failAt;
        int calls;
        Poll(int failAt) { this.failAt = failAt; }
        @Override public boolean isCanceled() {
            return failAt > 0 && ++calls >= failAt;
        }
        @Override public void checkCanceled() {
            calls++;
            if (failAt > 0 && calls >= failAt) throw new CancellationException("contract checkpoint");
        }
        @Override public void beginTask(String name, long totalWork) { }
        @Override public void done() { }
        @Override public void worked(long amount) { }
        @Override public void setProgress(long worked, long totalWork) { }
        @Override public void setTaskName(String name) { }
        @Override public void subTask(String name) { }
        @Override public void setCanceled(boolean canceled) { }
        @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
    }

    private static List<Integer> oracle(byte[] haystack, byte[] needle, int from) {
        List<Integer> result = new ArrayList<>();
        for (int start = Math.max(0, from); start <= haystack.length - needle.length; start++) {
            int j = 0;
            while (j < needle.length && haystack[start + j] == needle[j]) j++;
            if (j == needle.length) result.add(start);
        }
        return result;
    }

    private static void testCase(byte[] haystack, byte[] needle, int from, String label,
            boolean cancellationVariants) {
        var candidate = new M3BoyerMooreHorspoolSearch(needle);
        var baseline = new M3BoyerMooreHorspoolSearch(needle);
        List<Integer> expected = oracle(haystack, needle, from);
        Poll countPoll = new Poll(-1), historicalPoll = new Poll(-1);
        int count = M3ByteOccurrences.count(candidate, haystack, from, countPoll);
        historicalPoll.checkCanceled(); // old count entry, then existing visitor
        int prior = M3ByteOccurrences.visit(baseline, haystack, from, Integer.MAX_VALUE,
                ignored -> {}, historicalPoll);
        require(count == expected.size(), "count vs naive " + label);
        require(count == prior, "count vs historical Horspool " + label);
        require(countPoll.calls == historicalPoll.calls,
                "count monitor poll parity " + label + " got=" + countPoll.calls + " prior=" + historicalPoll.calls);
        List<Integer> positions = new ArrayList<>();
        List<Integer> oldPositions = new ArrayList<>();
        var current = new Poll(-1);
        var original = new Poll(-1);
        int n1 = M3ByteOccurrences.visit(candidate, haystack, from, Integer.MAX_VALUE,
                positions::add, current);
        int n2 = M3ByteOccurrences.visit(baseline, haystack, from, Integer.MAX_VALUE,
                oldPositions::add, original);
        require(n1 == n2 && n1 == expected.size(), "visit count " + label);
        require(positions.equals(expected) && positions.equals(oldPositions), "visit offsets " + label);
        require(current.calls == original.calls, "visit polls " + label);
        if (cancellationVariants) {
            int cap = Math.min(14, countPoll.calls + 3);
            for (int cut = 1; cut <= cap; cut++) {
                Poll now = new Poll(cut), before = new Poll(cut);
                boolean currentThrown = false, oldThrown = false;
                try { M3ByteOccurrences.count(candidate, haystack, from, now); }
                catch (CancellationException expectedCancel) { currentThrown = true; }
                try {
                    before.checkCanceled(); // old count entry
                    M3ByteOccurrences.visit(baseline, haystack, from, Integer.MAX_VALUE,
                            ignored -> {}, before);
                } catch (CancellationException expectedCancel) { oldThrown = true; }
                require(currentThrown == oldThrown && now.calls == before.calls,
                        "cancellation cutoff " + cut + " " + label);
            }
        }
    }

    public static long runOracle() {
        checks = 0;
        Random random = new Random(0x4d33424d48434f55L);
        for (int i = 0; i < 20_000; i++) {
            byte[] haystack = new byte[random.nextInt(180)];
            byte[] needle = new byte[1 + random.nextInt(45)];
            for (int j = 0; j < haystack.length; j++) {
                haystack[j] = (byte) (random.nextInt(7) == 0 ? random.nextInt(256) : random.nextInt(7));
            }
            for (int j = 0; j < needle.length; j++) {
                needle[j] = (byte) (random.nextInt(7) == 0 ? random.nextInt(256) : random.nextInt(7));
            }
            int from = switch (i % 8) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> haystack.length;
                default -> random.nextInt(haystack.length + 22) - 10;
            };
            testCase(haystack, needle, from, "random="+i, i % 1201 == 0);
        }
        for (int size : new int[] {0, 1, 3, 255, 4095, 4096, 4097, 8193, 65537}) {
            byte[] haystack = new byte[size];
            for (int j = 0; j < size; j++) haystack[j] = (byte) (j % 3);
            for (int width : new int[] {1, 3, 24, 512}) {
                byte[] needle = new byte[width];
                for (int j = 0; j < width; j++) needle[j] = (byte) (j % 3);
                for (int from : new int[] {Integer.MIN_VALUE, 0, 1, size, Integer.MAX_VALUE}) {
                    testCase(haystack, needle, from, "periodic="+size+"/"+width+"/"+from, true);
                }
            }
        }
        byte[] signed = {0, -1, 0, -1, 0, -1};
        testCase(signed, new byte[] {0, -1}, 0, "signed-bytes", true);
        require(Arrays.equals(signed, new byte[] {0, -1, 0, -1, 0, -1}), "input ownership");
        return checks;
    }

    public static void main(String[] args) {
        System.out.println("M3_BMH_COUNT_ONLY_CONTRACT_PASS checks=" + runOracle());
    }
}
