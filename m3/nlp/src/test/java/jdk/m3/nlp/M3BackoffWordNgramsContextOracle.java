// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import jdk.m3.nlp.M3BackoffWordNgrams;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Exact context/backoff probes and a managed-allocation witness; not SDK execution. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/WordContextOracle.java blob c99a5b2827e5; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class M3BackoffWordNgramsContextOracle {
    private static long checks;
    private static volatile Object sink;
    private static final String[] WORDS = {"Aa", "BB", "a", "z", "猫", "😀", "\uD800", "\uDC00", "\0", "a b"};
    private M3BackoffWordNgramsContextOracle() { }
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        if (Boolean.getBoolean("m3.proof.requireNamedModule")
                && !"org.m3jdk.nlp".equals(M3BackoffWordNgrams.class.getModule().getName()))
            throw new AssertionError("linked image must execute the named product module");
        if (args.length == 1 && args[0].equals("--allocation")) { allocation(); return; }
        if (args.length == 1 && args[0].equals("--layout")) { layout(); return; }
        if (args.length != 0) throw new IllegalArgumentException("[--allocation|--layout]");
        Random random = new Random(1467920261032L);
        for (int order = 1; order <= 8; order++) {
            for (int trial = 0; trial < 100; trial++) {
                List<List<String>> docs = new ArrayList<>();
                Map<List<String>, Map<String, Long>> oracle = new HashMap<>();
                for (int doc = 0; doc < 4; doc++) {
                    List<String> words = new ArrayList<>();
                    for (int i = 0, size = random.nextInt(24); i < size; i++)
                        words.add(WORDS[random.nextInt(WORDS.length)]);
                    docs.add(words);
                    for (int end = 0; end < words.size(); end++) {
                        for (int length = 0; length <= Math.min(end, order - 1); length++) {
                            var context = List.copyOf(words.subList(end - length, end));
                            oracle.computeIfAbsent(context, ignored -> new HashMap<>())
                                    .merge(words.get(end), 1L, Long::sum);
                        }
                    }
                }
                var limits = new M3BackoffWordNgrams.Limits(20, 1000, 10000, 10000, 10000, 8, 4096);
                var model = M3BackoffWordNgrams.train(docs, order, limits);
                check(model.contexts() == oracle.size(), "context cardinality");
                List<List<String>> histories = new ArrayList<>(oracle.keySet());
                histories.add(List.of("missing", "Aa", "BB"));
                histories.add(List.of("old", "old", "old", "old", "old", "old", "old", "old", "Aa"));
                for (List<String> history : histories) {
                    for (int k : new int[] {0, 1, 3, Integer.MAX_VALUE}) {
                        var actual = model.rank(history, k);
                        int depth = Math.min(order - 1, history.size());
                        Map<String, Long> row = null;
                        for (; depth >= 0; depth--) {
                            row = oracle.get(history.subList(history.size() - depth, history.size()));
                            if (row != null) break;
                        }
                        List<M3BackoffWordNgrams.Prediction> expected = new ArrayList<>();
                        if (row != null && k > 0) {
                            long total = row.values().stream().mapToLong(Long::longValue).sum();
                            var entries = new ArrayList<>(row.entrySet());
                            entries.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                                    .thenComparing(Map.Entry::getKey));
                            for (var entry : entries.subList(0, Math.min(k, entries.size())))
                                expected.add(new M3BackoffWordNgrams.Prediction(entry.getKey(), entry.getValue(),
                                        total, depth, entry.getValue() / (double) total));
                        }
                        check(actual.equals(expected), "reverse suffix / depth / ordering parity");
                    }
                    for (String word : WORDS) {
                        var row = oracle.get(history);
                        long expected = row == null ? 0 : row.getOrDefault(word, 0L);
                        check(model.exactCount(history, word) == expected, "exact context must not back off");
                    }
                }
            }
        }
        // Null detection from List.copyOf must precede empty-token validation.
        var model = M3BackoffWordNgrams.train(List.of(List.of("a", "b")), 2);
        for (List<String> invalid : List.of(Arrays.asList("", null), Arrays.asList("ignored", null, "a"))) {
            try { model.rank(invalid, 0); throw new AssertionError("null not rejected"); }
            catch (NullPointerException expected) { checks++; }
            try { model.exactCount(invalid, "a"); throw new AssertionError("null context not rejected"); }
            catch (NullPointerException expected) { checks++; }
        }
        try { model.rank(List.of("", "ignored", "a"), 1); throw new AssertionError("ignored invalid history"); }
        catch (IllegalArgumentException expected) { checks++; }
        var empty = M3BackoffWordNgrams.train(List.of(), 1);
        check(empty.contexts() == 0 && empty.rank(List.of(), 10).isEmpty(), "empty root");
        check(empty.exactCount(List.of(), "a") == 0, "empty exact lookup");
        System.out.println("WORD_CONTEXT_PASS corpora=800 checks=" + checks);
    }
    private static void allocation() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        var limits = new M3BackoffWordNgrams.Limits(20, 1000, 10000, 10000, 10000, 8, 4096);
        var model = M3BackoffWordNgrams.train(List.of(List.of("a", "b", "c", "d", "e", "f", "g", "h")), 8, limits);
        List<String> history = List.of("absent1", "absent2", "absent3", "absent4", "absent5", "absent6", "absent7");
        check(model.rank(history, 1).get(0).suffixLength() == 0, "allocation fixture fallback");
        for (int i = 0; i < 20000; i++) sink = model.rank(history, 1);
        long thread = Thread.currentThread().threadId();
        long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 50000; i++) sink = model.rank(history, 1);
        long bytes = (bean.getThreadAllocatedBytes(thread) - start) / 50000;
        System.out.println("CONTEXT_ALLOCATION bytesPerQuery=" + bytes + " history=7 order=8 k=1");
        check(bytes < 256, "SUFFIX_PROBE_ALLOCATION: " + bytes);
    }
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true);
        return field.get(object);
    }
    private static void layout() throws Exception {
        List<List<String>> docs = new ArrayList<>(List.of(
                List.of("x", "Aa", "same", "end"), List.of("y", "BB", "same", "end"),
                List.of("猫", "\uD800", "same", "end"), List.of("same", "end")));
        var model = M3BackoffWordNgrams.train(docs, 4);
        Object table = field(model, "table");
        check(!(table instanceof Map<?, ?>), "RETAINED_CONTEXT_MAP");
        Collections.reverse(docs);
        Object reordered = field(M3BackoffWordNgrams.train(docs, 4), "table");
        int[] offsets = (int[])field(table, "offsets");
        int[] children = (int[])field(table, "children");
        String[] labels = (String[])field(table, "labels");
        Object[] rows = (Object[])field(table, "rows");
        check(offsets.length == rows.length + 1 && children.length == rows.length - 1, "one node per context");
        check(labels.length == children.length && offsets[rows.length] == children.length, "complete edge arrays");
        check(Arrays.equals(offsets, (int[])field(reordered, "offsets")), "deterministic offsets");
        check(Arrays.equals(children, (int[])field(reordered, "children")), "deterministic child IDs");
        check(Arrays.equals(labels, (String[])field(reordered, "labels")), "deterministic labels");
        boolean[] seen = new boolean[rows.length]; seen[0] = true;
        for (int node = 0; node < rows.length; node++) {
            for (int edge = offsets[node]; edge < offsets[node + 1]; edge++) {
                check(children[edge] > node && !seen[children[edge]], "unique forward child");
                seen[children[edge]] = true;
                if (edge > offsets[node]) check(labels[edge - 1].compareTo(labels[edge]) < 0, "sorted unique sibling labels");
            }
        }
        for (boolean yes : seen) check(yes, "all context nodes reachable");
        System.out.println("CONTEXT_LAYOUT_PASS checks=" + checks + " nodes=" + rows.length);
    }
}
