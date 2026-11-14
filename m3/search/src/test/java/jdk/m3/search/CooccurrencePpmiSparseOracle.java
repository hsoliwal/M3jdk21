// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/** Independent counts and sparse-query allocation witness, not an SDK replacement. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-ppmi-topk-20261031/PpmiSparseOracle.java blob 26f96873d193; recipe m3/nlp/recipe/cooccurrence-ppmi-20261106/relocate.py v3
public final class CooccurrencePpmiSparseOracle {
    private static final String[] WORDS = {"\0", "a", "b", "a b", "café", "猫", "\uD800", "\uDC00", "😀", "\uFFFF"};
    private static long checks;
    private static volatile Object sink;
    private CooccurrencePpmiSparseOracle() { }
    private static void require(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--allocation")) { allocation(); return; }
        if (args.length != 0) throw new IllegalArgumentException("[--allocation]");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Random random = new Random(2026103114679L);
        for (int trial = 0; trial < 1500; trial++) {
            int window = 1 + random.nextInt(4);
            List<List<String>> docs = new ArrayList<>();
            long[][] counts = new long[WORDS.length][WORDS.length];
            long[] centers = new long[WORDS.length], contexts = new long[WORDS.length];
            boolean[] present = new boolean[WORDS.length];
            long events = 0;
            for (int d = 0, documents = random.nextInt(8); d < documents; d++) {
                List<String> doc = new ArrayList<>();
                int[] ids = new int[random.nextInt(20)];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = random.nextInt(WORDS.length);
                    doc.add(WORDS[ids[i]]); present[ids[i]] = true;
                }
                docs.add(doc);
                for (int i = 0; i < ids.length; i++) {
                    for (int j = 0; j < ids.length; j++) {
                        if (i != j && Math.abs(i - j) <= window) {
                            counts[ids[i]][ids[j]]++;
                            centers[ids[i]]++; contexts[ids[j]]++; events++;
                        }
                    }
                }
            }
            var snapshot = CooccurrencePpmi.build(docs, window);
            require(snapshot.events() == events, "event total");
            List<String> vocabulary = new ArrayList<>();
            List<CooccurrencePpmi.PairCount> pairs = new ArrayList<>();
            for (int a = 0; a < WORDS.length; a++) {
                if (present[a]) vocabulary.add(WORDS[a]);
                require(snapshot.centerCount(WORDS[a]) == centers[a], "center total");
                require(snapshot.contextCount(WORDS[a]) == contexts[a], "context total");
                List<CooccurrencePpmi.ContextScore> expected = new ArrayList<>();
                for (int b = 0; b < WORDS.length; b++) {
                    long count = counts[a][b];
                    require(snapshot.pairCount(WORDS[a], WORDS[b]) == count, "dense count parity");
                    double ppmi = count == 0 ? 0.0 : Math.max(0.0, Math.log(count) + Math.log(events)
                            - Math.log(centers[a]) - Math.log(contexts[b]));
                    require(Double.doubleToLongBits(snapshot.ppmi(WORDS[a], WORDS[b]))
                            == Double.doubleToLongBits(ppmi), "bitwise independent PPMI");
                    if (count > 0) pairs.add(new CooccurrencePpmi.PairCount(WORDS[a], WORDS[b], count));
                    if (ppmi > 0) expected.add(new CooccurrencePpmi.ContextScore(WORDS[b], count, ppmi));
                }
                expected.sort(Comparator.comparingDouble(CooccurrencePpmi.ContextScore::ppmi)
                        .reversed().thenComparing(CooccurrencePpmi.ContextScore::context));
                for (int k : new int[] {0, 1, 3, Integer.MAX_VALUE}) {
                    var actual = snapshot.topPositiveContexts(WORDS[a], k);
                    require(actual.equals(expected.subList(0, Math.min(k, expected.size()))), "rank from dense counts");
                }
            }
            vocabulary.sort(String::compareTo);
            pairs.sort(Comparator.comparing(CooccurrencePpmi.PairCount::center)
                    .thenComparing(CooccurrencePpmi.PairCount::context));
            require(snapshot.vocabulary().equals(vocabulary), "vocabulary ordering");
            require(snapshot.pairCounts().equals(pairs), "full pair iteration ordering");
            require(snapshot.distinctPairs() == pairs.size(), "distinct pairs");
            for (var pair : pairs) {
                for (String token : List.of(pair.center(), pair.context())) {
                    for (char c : token.toCharArray()) { digest.update((byte)(c >>> 8)); digest.update((byte)c); }
                    digest.update((byte)255);
                }
                for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte)(pair.count() >>> shift));
            }
            docs.forEach(List::clear); docs.clear();
            require(snapshot.pairCounts().equals(pairs), "input mutation isolation");
            require(snapshot.pairCount("missing", "a") == 0, "missing lower-bound lookup");
        }
        var isolated = CooccurrencePpmi.build(List.of(List.of("first"), List.of("last")), 1);
        require(isolated.pairCounts().isEmpty(), "empty pair store");
        require(isolated.topPositiveContexts("first", Integer.MAX_VALUE).isEmpty(), "known empty row");
        var zeroKey = CooccurrencePpmi.build(List.of(List.of("a", "a")), 1);
        require(zeroKey.pairCount("a", "a") == 2, "zero packed key");
        require(zeroKey.topPositiveContexts("a", Integer.MAX_VALUE).isEmpty(), "nonpositive row");
        System.out.println("SPARSE_ORACLE_PASS corpora=1500 checks=" + checks
                + " digest=" + HexFormat.of().formatHex(digest.digest()));
    }
    private static void allocation() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counters unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        List<List<String>> docs = new ArrayList<>();
        for (int i = 0; i < 7500; i++) docs.add(List.of("isolate-" + i));
        docs.add(List.of("sparse-center", "sparse-context"));
        var snapshot = CooccurrencePpmi.build(docs, 1);
        var expected = List.of(new CooccurrencePpmi.ContextScore("sparse-context", 1, Math.log(2)));
        require(snapshot.topPositiveContexts("sparse-center", Integer.MAX_VALUE).equals(expected), "sparse witness exact result");
        for (int i = 0; i < 500; i++) sink = snapshot.topPositiveContexts("sparse-center", Integer.MAX_VALUE);
        long thread = Thread.currentThread().threadId();
        long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100; i++) sink = snapshot.topPositiveContexts("sparse-center", Integer.MAX_VALUE);
        long bytes = (bean.getThreadAllocatedBytes(thread) - start) / 100;
        System.out.println("SPARSE_ALLOCATION bytesPerCall=" + bytes + " vocabulary=7502 degree=1 k=MAX_VALUE");
        require(bytes < 4096, "SPARSE_ALLOCATION_EXCEEDED: " + bytes);
    }
}
