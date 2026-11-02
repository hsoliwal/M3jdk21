// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.text.search.AffixIndex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Diagnostic only: reports admission separately from repeated exact queries.
 * Not JMH, not a proof that indexing should replace direct startsWith/endsWith.
 */
public final class M3AffixIndexBenchmark {
    private static volatile long sink;
    private M3AffixIndexBenchmark() { }

    public static void main(String[] args) {
        int words = args.length > 0 ? Integer.parseInt(args[0]) : 30_000;
        int queries = args.length > 1 ? Integer.parseInt(args[1]) : 80;
        if (words < 1 || words > 60_000 || queries < 1 || queries > 1_000) {
            throw new IllegalArgumentException("benchmark cardinality budget");
        }
        Random random = new Random(20261024L);
        List<String> lexicon = new ArrayList<>(words);
        for (int i = 0; i < words; i++) {
            StringBuilder word = new StringBuilder(12);
            for (int j = 0; j < 12; j++) word.append((char) ('a' + random.nextInt(26)));
            lexicon.add(word.toString());
        }
        List<String> prefixes = new ArrayList<>(queries);
        List<String> suffixes = new ArrayList<>(queries);
        for (int i = 0; i < queries; i++) {
            String word = lexicon.get(random.nextInt(words));
            prefixes.add(word.substring(0, 3));
            suffixes.add(word.substring(word.length() - 3));
        }
        for (int i = 0; i < 3; i++) run(lexicon, prefixes, suffixes, false);
        long[][] samples = new long[3][7];
        for (int i = 0; i < samples[0].length; i++) {
            long[] measured = run(lexicon, prefixes, suffixes, true);
            for (int j = 0; j < samples.length; j++) samples[j][i] = measured[j];
        }
        for (long[] values : samples) Arrays.sort(values);
        System.out.printf("M3_AFFIX_BENCH words=%d query_pairs=%d baseline_ms=%.3f admission_ms=%.3f indexed_queries_ms=%.3f indexed_total_ms=%.3f sink=%d%n",
                words, queries, samples[0][3] / 1e6, samples[1][3] / 1e6,
                samples[2][3] / 1e6, (samples[1][3] + samples[2][3]) / 1e6, sink);
    }

    private static long[] run(List<String> words, List<String> prefixes,
                              List<String> suffixes, boolean validate) {
        long before = System.nanoTime();
        long baseline = 0;
        for (int q = 0; q < prefixes.size(); q++) {
            String prefix = prefixes.get(q), suffix = suffixes.get(q);
            for (String word : words) {
                if (word.startsWith(prefix)) baseline++;
                if (word.endsWith(suffix)) baseline++;
            }
        }
        long afterBaseline = System.nanoTime();
        AffixIndex index = AffixIndex.prepare(words);
        long afterBuild = System.nanoTime();
        long indexed = 0;
        for (int q = 0; q < prefixes.size(); q++) {
            indexed += index.countPrefix(prefixes.get(q));
            indexed += index.countSuffix(suffixes.get(q));
        }
        long afterIndex = System.nanoTime();
        if (validate && baseline != indexed) throw new AssertionError("affix benchmark differential mismatch");
        sink = indexed;
        return new long[]{afterBaseline - before, afterBuild - afterBaseline, afterIndex - afterBuild};
    }
}
