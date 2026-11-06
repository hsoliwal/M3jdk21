// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import jdk.m3.nlp.M3BackoffWordNgrams;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent training-context proof. It is not a JUnit/OpenRewrite substitute. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/TrainingContextIndexOracle.java blob b3229b7aeebe; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class M3BackoffWordNgramsTrainingContextIndexOracle {
    private static long checks;
    private static volatile Object sink;
    private M3BackoffWordNgramsTrainingContextIndexOracle() { }
    private static void require(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--allocation")) { allocation(); return; }
        if (args.length == 1 && args[0].equals("--layout")) { layout(); return; }
        if (args.length != 0) throw new IllegalArgumentException("[--allocation|--layout]");
        behavior();
        System.out.println("TRAINING_CONTEXT_BEHAVIOR_PASS checks=" + checks);
    }
    private static void behavior() {
        Random random = new Random(202611051724L);
        for (int trial = 0; trial < 600; trial++) {
            int order = 1 + random.nextInt(8);
            List<List<String>> docs = new ArrayList<>();
            for (int d = 0, dc = random.nextInt(7); d < dc; d++) {
                List<String> doc = new ArrayList<>();
                for (int i = 0, n = random.nextInt(28); i < n; i++) {
                    doc.add(switch (random.nextInt(9)) {
                        case 0 -> "Aa"; case 1 -> "BB"; case 2 -> "猫"; case 3 -> "😀";
                        case 4 -> "\uD800"; case 5 -> "\uDC00"; default -> "w" + random.nextInt(5);
                    });
                }
                docs.add(doc);
            }
            var trialLimits = new M3BackoffWordNgrams.Limits(100, 100_000, 100_000, 200_000, 500_000, 8, 4_096);
            M3BackoffWordNgrams.Model model = M3BackoffWordNgrams.train(docs, order, trialLimits);
            long expectedObservations = 0;
            for (List<String> doc : docs) {
                for (int i = 0; i < doc.size(); i++) expectedObservations += Math.min(i, order - 1) + 1L;
            }
            require(model.observations() == expectedObservations, "observation total");
            // Compare exact counts against direct token-position counting.
            for (List<String> doc : docs) {
                for (int i = 0; i < doc.size(); i++) {
                    String next = doc.get(i);
                    for (int len = 0; len <= Math.min(i, order - 1); len++) {
                        List<String> context = List.copyOf(doc.subList(i - len, i));
                        long count = 0;
                        for (List<String> scan : docs) {
                            for (int p = len; p < scan.size(); p++) {
                                if (!scan.get(p).equals(next)) continue;
                                boolean same = true;
                                for (int q = 0; q < len; q++) {
                                    if (!scan.get(p - len + q).equals(context.get(q))) { same = false; break; }
                                }
                                if (same) count++;
                            }
                        }
                        require(model.exactCount(context, next) == count, "exact context count");
                    }
                }
            }
            // Input mutation after training must not affect the model.
            long before = model.observations();
            docs.forEach(List::clear); docs.clear();
            require(model.observations() == before, "input mutation isolation");
        }
        // Budget/validation precedence retained around context creation.
        var contextsOne = new M3BackoffWordNgrams.Limits(1, 10, 1, 10, 10, 8, 100);
        try { M3BackoffWordNgrams.train(List.of(List.of("a", "b")), 2, contextsOne); throw new AssertionError("context budget"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("context"), "context budget precedence"); }
        var observationsOne = new M3BackoffWordNgrams.Limits(1, 10, 10, 10, 1, 8, 100);
        try { M3BackoffWordNgrams.train(List.of(List.of("a", "b")), 2, observationsOne); throw new AssertionError("observation budget"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("observation"), "observation budget precedence"); }
    }
    private static void allocation() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counters unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        List<String> words = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) words.add("w" + (i & 15));
        List<List<String>> docs = List.of(List.copyOf(words));
        var limits = new M3BackoffWordNgrams.Limits(10, 100_000, 100_000, 100_000, 1_000_000, 8, 4_096);
        for (int i = 0; i < 5; i++) sink = M3BackoffWordNgrams.train(docs, 8, limits);
        long thread = Thread.currentThread().threadId();
        long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 10; i++) sink = M3BackoffWordNgrams.train(docs, 8, limits);
        long bytes = (bean.getThreadAllocatedBytes(thread) - start) / 10;
        System.out.println("TRAINING_CONTEXT_ALLOCATION bytesPerTraining=" + bytes + " tokens=20000 order=8");
        require(bytes < 12_000_000L, "TRAINING_CONTEXT_ALLOCATION_EXCEEDED: " + bytes);
    }
    private static void layout() throws Exception {
        Class<?> mutable = Class.forName(M3BackoffWordNgrams.class.getName() + "$MutableContextIndex");
        int arrays = 0;
        for (Field field : mutable.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            require(!java.util.Map.class.isAssignableFrom(field.getType()), "no retained training context map");
            if (field.getType().isArray()) arrays++;
        }
        require(arrays >= 6, "parallel-array context builder");
        // Candidate must freeze into the already-existing array ContextIndex.
        var model = M3BackoffWordNgrams.train(List.of(List.of("a", "b", "a", "c")), 4);
        Field table = M3BackoffWordNgrams.Model.class.getDeclaredField("table"); table.setAccessible(true);
        Object index = table.get(model);
        int arrayFields = 0;
        for (Field field : index.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            require(!java.util.Map.class.isAssignableFrom(field.getType()), "immutable context index has no map");
            if (field.getType().isArray()) {
                field.setAccessible(true); arrayFields++;
                require(Array.getLength(field.get(index)) >= 0, "readable frozen array");
            }
        }
        require(arrayFields == 4, "immutable index retains exactly four arrays");
        System.out.println("TRAINING_CONTEXT_LAYOUT_PASS checks=" + checks + " mutableArrays=" + arrays);
    }
}
