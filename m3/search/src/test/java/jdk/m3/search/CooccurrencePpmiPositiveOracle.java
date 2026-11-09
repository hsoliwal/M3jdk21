// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Public-output allocation witness: many observed neighbours but only one positive score. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-ppmi-topk-20261031/PpmiPositiveOracle.java blob 3075f5e78d5d; recipe m3/nlp/recipe/cooccurrence-ppmi-20261106/relocate.py v3
public final class CooccurrencePpmiPositiveOracle {
    private static volatile Object sink;
    private CooccurrencePpmiPositiveOracle() { }
    public static void main(String[] args) {
        if (args.length > 1 || args.length == 1 && !args[0].equals("--behavior"))
            throw new IllegalArgumentException("[--behavior]");
        growthCases();
        int neighbours = 3000;
        List<List<String>> docs = new ArrayList<>();
        for (int i = 0; i < neighbours; i++) {
            String token = "word-" + i;
            docs.add(List.of("center", token));
            docs.add(List.of(token, token));
        }
        List<String> repeated = new ArrayList<>();
        for (int i = 0; i <= neighbours; i++) repeated.add("center");
        docs.add(repeated);
        var model = CooccurrencePpmi.build(docs, 1);
        var actual = model.topPositiveContexts("center", Integer.MAX_VALUE);
        if (actual.size() != 1 || !actual.get(0).context().equals("center")
                || actual.get(0).count() != 6000
                || Double.doubleToLongBits(actual.get(0).ppmi())
                    != Double.doubleToLongBits(model.ppmi("center", "center")))
            throw new AssertionError("positive-density fixture contract");
        for (int i = 0; i < neighbours; i++)
            if (model.ppmi("center", "word-" + i) != 0.0)
                throw new AssertionError("nonpositive neighbour");
        if (args.length == 1) {
            System.out.println("POSITIVE_BEHAVIOR_PASS degree=3001 positive=1");
            return;
        }
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counters unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 500; i++) sink = model.topPositiveContexts("center", Integer.MAX_VALUE);
        long thread = Thread.currentThread().threadId();
        long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100; i++) sink = model.topPositiveContexts("center", Integer.MAX_VALUE);
        long bytes = (bean.getThreadAllocatedBytes(thread) - start) / 100;
        System.out.println("POSITIVE_ALLOCATION bytesPerCall=" + bytes + " degree=3001 positive=1 k=MAX_VALUE");
        if (bytes >= 4096) throw new AssertionError("POSITIVE_SCRATCH_EXCEEDED: " + bytes);
    }
    private static void growthCases() {
        List<List<String>> docs = new ArrayList<>();
        for (int i = 0; i < 129; i++)
            for (int j = 0; j <= i % 7; j++) docs.add(List.of("hub", "leaf-" + i));
        var model = CooccurrencePpmi.build(docs, 1);
        List<CooccurrencePpmi.ContextScore> expected = new ArrayList<>();
        for (String token : model.vocabulary()) {
            double score = model.ppmi("hub", token);
            if (score > 0) expected.add(new CooccurrencePpmi.ContextScore(token, model.pairCount("hub", token), score));
        }
        expected.sort(Comparator.comparingDouble(CooccurrencePpmi.ContextScore::ppmi).reversed()
                .thenComparing(CooccurrencePpmi.ContextScore::context));
        if (expected.size() != 129) throw new AssertionError("growth fixture cardinality");
        for (int limit : new int[] {0, 1, 15, 16, 17, 31, 32, 33, 63, 64, 65, 128, 129, Integer.MAX_VALUE}) {
            if (!model.topPositiveContexts("hub", limit).equals(expected.subList(0, Math.min(limit, expected.size()))))
                throw new AssertionError("heap growth boundary: " + limit);
        }
    }

}
