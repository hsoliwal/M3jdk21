// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import jdk.m3.tools.cpu.M3Bm25Rows;
import jdk.m3.tools.cpu.M3RangePatchMerge;
import jdk.m3.tools.cpu.M3FrequencySketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Independent boxed BM25, patch-concatenation and counter-array reference oracles. */
public final class M3IndexDiffAdmissionContract {
    private M3IndexDiffAdmissionContract() { }
    private static long checks;
    private static void require(boolean valid, String category) {
        if (!valid) throw new AssertionError(category + " check=" + checks);
        checks++;
    }
    private static void rejects(Runnable action) {
        try { action.run(); throw new AssertionError("unexpected acceptance"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void ranking() {
        Random random = new Random(0xB25A4L);
        for (int trial = 0; trial < 1650; trial++) {
            int count = random.nextInt(35), vocabulary = 1 + random.nextInt(35);
            int[] offsets = new int[count + 1];
            int[][] documents = new int[count][];
            int tokensCount = 0;
            for (int d = 0; d < count; d++) {
                int length = random.nextInt(25);
                documents[d] = new int[length];
                for (int t = 0; t < length; t++) documents[d][t] = random.nextInt(vocabulary);
                offsets[d + 1] = tokensCount += length;
            }
            int[] tokens = new int[tokensCount];
            for (int d = 0; d < count; d++) System.arraycopy(
                    documents[d], 0, tokens, offsets[d], documents[d].length);
            Set<Integer> querySet = new HashSet<>();
            for (int k = random.nextInt(9); k > 0; k--) querySet.add(random.nextInt(vocabulary));
            int[] query = querySet.stream().mapToInt(Integer::intValue).sorted().toArray();
            double k1 = 0.5 + random.nextDouble() * 3, b = random.nextDouble();
            var actual = M3Bm25Rows.rank(offsets, tokens, vocabulary, query, k1, b);
            int[] df = new int[vocabulary];
            List<Map<Integer, Integer>> counts = new ArrayList<>();
            for (int doc = 0; doc < count; doc++) {
                Map<Integer, Integer> terms = new HashMap<>();
                for (int token : documents[doc]) terms.merge(token, 1, Integer::sum);
                counts.add(terms);
                for (int term : terms.keySet()) df[term]++;
            }
            double average = count == 0 ? 0 : tokensCount / (double) count;
            double[] scores = new double[count];
            for (int doc = 0; doc < count; doc++) {
                for (int term : query) {
                    int occurrences = counts.get(doc).getOrDefault(term, 0);
                    if (occurrences == 0) continue;
                    double idf = Math.log(1.0 + (count - df[term] + 0.5) / (df[term] + 0.5));
                    double normal = 1.0 - b + b * (average == 0 ? 0 : documents[doc].length / average);
                    scores[doc] += idf * occurrences * (k1 + 1)
                            / (occurrences + k1 * normal);
                }
                require(Math.abs(actual.score()[doc] - scores[doc]) < 1e-9, "BM25 independent score");
            }
            Integer[] ranked = new Integer[count];
            for (int i = 0; i < count; i++) ranked[i] = i;
            Arrays.sort(ranked, (a, c) -> {
                int byScore = Double.compare(actual.score()[c], actual.score()[a]);
                return byScore == 0 ? Integer.compare(a, c) : byScore;
            });
            for (int i = 0; i < count; i++) require(actual.documents()[i] == ranked[i], "BM25 sorted rows");
            if (count > 0) {
                double[] original = actual.score(); original[0] = -100;
                require(actual.score()[0] >= 0, "BM25 scores detached");
            }
        }
        rejects(() -> M3Bm25Rows.rank(new int[]{0, 2, 1}, new int[]{1}, 3,
                new int[]{1}, 1.2, 0.75));
        rejects(() -> M3Bm25Rows.rank(new int[]{0, 1}, new int[]{0}, 2,
                new int[]{0, 0}, 1.2, 0.75));
        rejects(() -> M3Bm25Rows.rank(new int[]{0}, new int[0], 2,
                new int[0], 0, 0.75));
    }

    private static void patches() {
        var conflict = M3RangePatchMerge.merge("abcdef",
                new int[]{1}, new int[]{4}, new String[]{"X"},
                new int[]{3}, new int[]{5}, new String[]{"Y"});
        require(!conflict.clean() && conflict.merged() == null && conflict.leftConflict() == 0
                && conflict.rightConflict() == 0, "overlap refusal");
        var insertionConflict = M3RangePatchMerge.merge("abcdef",
                new int[]{1}, new int[]{1}, new String[]{"X"},
                new int[]{1}, new int[]{3}, new String[]{"Y"});
        require(!insertionConflict.clean(), "same-coordinate insertion refusal");
        var touching = M3RangePatchMerge.merge("abcdef",
                new int[]{1}, new int[]{3}, new String[]{"Q"},
                new int[]{3}, new int[]{5}, new String[]{"R"});
        require(touching.clean() && touching.merged().equals("aQRf"), "adjacent edits allowed");
        var utf16 = M3RangePatchMerge.merge("a😀é\r\nb",
                new int[]{1}, new int[]{3}, new String[]{"Z"},
                new int[]{3}, new int[]{4}, new String[]{"Y"});
        require(utf16.clean() && utf16.merged().equals("aZY\r\nb"), "UTF-16 source offsets");
        Random random = new Random(0x4D335041544348L);
        for (int trial = 0; trial < 12_000; trial++) {
            int length = 2 + random.nextInt(18);
            StringBuilder base = new StringBuilder();
            for (int i = 0; i < length; i++) base.append((char)('a' + random.nextInt(26)));
            int left = random.nextInt(length), right = random.nextInt(length - 1);
            if (right >= left) right++;
            String l = "L" + trial, r = "R" + trial;
            var result = M3RangePatchMerge.merge(base,
                    new int[]{left}, new int[]{left + 1}, new String[]{l},
                    new int[]{right}, new int[]{right + 1}, new String[]{r});
            StringBuilder expected = new StringBuilder();
            for (int at = 0; at < length; at++) {
                if (at == left) expected.append(l);
                else if (at == right) expected.append(r);
                else expected.append(base.charAt(at));
            }
            require(result.clean() && result.merged().equals(expected.toString()), "independent patch replay");
        }
        rejects(() -> M3RangePatchMerge.merge("abc", new int[]{0, 1}, new int[]{2, 3},
                new String[]{"x", "y"}, new int[0], new int[0], new String[0]));
        rejects(() -> M3RangePatchMerge.merge("abc", new int[]{4}, new int[]{5},
                new String[]{"x"}, new int[0], new int[0], new String[0]));
    }

    private static long mix(long input) {
        input ^= input >>> 30; input *= 0xbf58476d1ce4e5b9L;
        input ^= input >>> 27; input *= 0x94d049bb133111ebL;
        return input ^ (input >>> 31);
    }
    private static void sketch() {
        final long[] seeds = {0x9e3779b97f4a7c15L, 0xbf58476d1ce4e5b9L,
                0x94d049bb133111ebL, 0xd6e8feb86659fd93L};
        final int width = 64, window = 223;
        int[][] oracle = new int[4][width];
        var fast = new M3FrequencySketch(6, window);
        Random random = new Random(0xFEEDEF42L);
        for (int trial = 0; trial < 35_000; trial++) {
            long key = random.nextInt(400);
            fast.observe(key);
            for (int row = 0; row < 4; row++) {
                int at = (int)mix(key ^ seeds[row]) & (width - 1);
                oracle[row][at] = Math.min(15, oracle[row][at] + 1);
            }
            if ((trial + 1) % window == 0) {
                for (int[] values : oracle) for (int i = 0; i < values.length; i++) values[i] >>>= 1;
            }
            for (int probe = 0; probe < 8; probe++) {
                long candidate = random.nextInt(400);
                int min = 15;
                for (int row = 0; row < 4; row++) min = Math.min(min,
                        oracle[row][(int)mix(candidate ^ seeds[row]) & (width - 1)]);
                require(min == fast.estimate(candidate), "packed counter vs reference sketch");
            }
        }
        long[] snapshot = fast.packedSnapshot();
        snapshot[0] = -1;
        require(fast.packedSnapshot()[0] != -1, "sketch snapshot detached");
        require(fast.samplesInCurrentWindow() == 35_000 % window, "decay window cadence");
        rejects(() -> new M3FrequencySketch(3, 5));
        rejects(() -> new M3FrequencySketch(17, 5));
        rejects(() -> new M3FrequencySketch(6, 0));
    }

    public static void main(String[] ignored) {
        ranking(); patches(); sketch();
        System.out.println("M3_WAVE4_INDEX_DIFF_ADMISSION_PASS checks=" + checks);
    }
}
