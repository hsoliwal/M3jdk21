// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent reference counts/ranking; not a JUnit or SDK replacement. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/recipes/nlp-wordrank-20261031/WordRankOracle.java blob 553a92f2bdf2; recipe m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3
public final class BackoffWordNgramsRankOracle {
    private static final String[] WORDS = {"a", "b", "café", "猫", "😀", "\uD800", "\uDC00", "\uE000", "a b", "\0"};
    private static long checks;
    private static volatile Object sink;
    private BackoffWordNgramsRankOracle() { }
    private static void require(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static void rejects(Class<? extends Throwable> type, Runnable task) {
        try { task.run(); }
        catch (Throwable failure) { require(type.isInstance(failure), "exception: " + failure); return; }
        throw new AssertionError("expected " + type.getName());
    }
    private static Map<List<String>, Map<String,Long>> reference(List<List<String>> docs, int order) {
        Map<List<String>, Map<String,Long>> table = new HashMap<>();
        for (var doc : docs) for (int end = 0; end < doc.size(); end++) {
            for (int start = Math.max(0, end - order + 1); start <= end; start++) {
                var history = List.copyOf(doc.subList(start, end));
                var row = table.computeIfAbsent(history, ignored -> new HashMap<>());
                row.put(doc.get(end), row.getOrDefault(doc.get(end), 0L) + 1);
            }
        }
        return table;
    }
    private static List<BackoffWordNgrams.Prediction> rank(Map<List<String>, Map<String,Long>> table,
                                                           List<String> history, int order, int k) {
        for (int n = Math.min(order-1, history.size()); n >= 0; n--) {
            var row = table.get(history.subList(history.size()-n, history.size()));
            if (row == null) continue;
            long total = row.values().stream().mapToLong(Long::longValue).sum();
            var entries = new ArrayList<>(row.entrySet());
            entries.sort(Map.Entry.<String,Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey));
            List<BackoffWordNgrams.Prediction> expected = new ArrayList<>();
            for (var e : entries.subList(0, Math.min(k, entries.size())))
                expected.add(new BackoffWordNgrams.Prediction(e.getKey(), e.getValue(), total, n, e.getValue()/(double)total));
            return expected;
        }
        return List.of();
    }
    private static void digest(MessageDigest digest, List<BackoffWordNgrams.Prediction> values) {
        digest.update(ByteBuffer.allocate(4).putInt(values.size()).array());
        for (var p : values) {
            digest.update(ByteBuffer.allocate(4).putInt(p.token().length()).array());
            for (char c : p.token().toCharArray()) { digest.update((byte)(c >>> 8)); digest.update((byte)c); }
            digest.update(ByteBuffer.allocate(28).putLong(p.count()).putLong(p.observed())
                    .putInt(p.suffixLength()).putLong(Double.doubleToLongBits(p.probability())).array());
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--allocation")) { allocation(); return; }
        if (args.length == 1 && args[0].equals("--long-counts")) { longCounts(); return; }
        if (args.length != 0) throw new IllegalArgumentException("[--allocation|--long-counts]");
        var digest = MessageDigest.getInstance("SHA-256");
        Random random = new Random(2026103114680L);
        for (int trial = 0; trial < 2000; trial++) {
            int order = 1 + random.nextInt(8);
            List<List<String>> docs = new ArrayList<>();
            for (int d = 0, num = random.nextInt(7); d < num; d++) {
                List<String> doc = new ArrayList<>();
                for (int i = 0, numWords = random.nextInt(16); i < numWords; i++) doc.add(WORDS[random.nextInt(WORDS.length)]);
                docs.add(doc);
            }
            var limits = new BackoffWordNgrams.Limits(100, 10000, 10000, 10000, 80000, 8, 4096);
            var model = BackoffWordNgrams.train(docs, order, limits);
            var expected = reference(docs, order);
            long observations = 0;
            for (var row : expected.values()) for (long count : row.values()) observations += count;
            require(model.order() == order && model.observations() == observations, "model metadata");
            require(model.contexts() == expected.size(), "context count");
            for (var row : expected.entrySet()) {
                for (String next : WORDS) require(model.exactCount(row.getKey(), next)
                        == row.getValue().getOrDefault(next, 0L), "exact count");
            }
            var reversed = new ArrayList<>(docs); Collections.reverse(reversed);
            var reordered = BackoffWordNgrams.train(reversed, order, limits);
            for (int query = 0; query < 12; query++) {
                List<String> history = new ArrayList<>();
                if (query < docs.size()) history.addAll(docs.get(query));
                else for (int i = 0, n = random.nextInt(12); i < n; i++)
                    history.add(random.nextInt(4) == 0 ? "missing" : WORDS[random.nextInt(WORDS.length)]);
                for (int k : new int[]{0, 1, 3, 15, Integer.MAX_VALUE}) {
                    var result = model.rank(history, k);
                    require(result.equals(rank(expected, history, order, k)), "backoff/rank parity trial=" + trial);
                    require(result.equals(reordered.rank(history, k)), "training-order invariance");
                    digest(digest, result);
                }
                require(model.exactCount(history, "missing") == 0, "missing exact count");
            }
            var stable = model.rank(List.of(), Integer.MAX_VALUE);
            docs.forEach(List::clear); docs.clear();
            require(model.rank(List.of(), Integer.MAX_VALUE).equals(stable), "input isolation");
        }
        boundaries();
        var m = BackoffWordNgrams.train(List.of(List.of("a","b","a","c")), 3);
        var target = m.rank(List.of("a"), 2);
        rejects(UnsupportedOperationException.class, () -> target.add(target.get(0)));
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 1000; i++) results.add(pool.submit(() -> m.rank(List.of("a"),2).equals(target)));
            for (var result : results) require(result.get(), "concurrent ranking");
        }
        System.out.println("WORD_RANK_PASS corpora=2000 checks=" + checks + " digest=" + HexFormat.of().formatHex(digest.digest()));
    }
    private static void boundaries() {
        var m = BackoffWordNgrams.train(List.of(List.of("a", "b")), 2);
        require(m.rank(List.of("missing", "a"), 10).get(0).token().equals("b"), "suffix match");
        require(m.rank(List.of("missing"),10).get(0).suffixLength() == 0, "unigram fallback");
        require(m.exactCount(List.of("a", "b"), "a") == 0, "no implicit count backoff");
        var empty = BackoffWordNgrams.train(List.of(List.of()), 1);
        require(empty.rank(List.of("x"), Integer.MAX_VALUE).isEmpty(), "empty table");
        for (var model : List.of(m, empty)) {
            rejects(NullPointerException.class, () -> model.rank(null, -1));
            rejects(IllegalArgumentException.class, () -> model.rank(Arrays.asList((String)null), -1));
            rejects(NullPointerException.class, () -> model.rank(Arrays.asList((String)null), 0));
            rejects(IllegalArgumentException.class, () -> model.rank(List.of("", "a"), 0));
            rejects(IllegalArgumentException.class, () -> model.rank(List.of(""), 1));
            rejects(NullPointerException.class, () -> model.exactCount(null, ""));
            rejects(NullPointerException.class, () -> model.exactCount(List.of(""), null));
            rejects(IllegalArgumentException.class, () -> model.exactCount(Arrays.asList((String)null), ""));
        }
        rejects(NullPointerException.class, () -> BackoffWordNgrams.train(null, 1));
        rejects(NullPointerException.class, () -> BackoffWordNgrams.train(Arrays.asList((List<String>)null), 1));
        rejects(NullPointerException.class, () -> BackoffWordNgrams.train(List.of(Arrays.asList((String)null)), 1));
        rejects(IllegalArgumentException.class, () -> BackoffWordNgrams.train(List.of(List.of("")), 1));
        rejects(IllegalArgumentException.class, () -> BackoffWordNgrams.train(List.of(), 0));
        var docs = List.of(List.of("a", "b"), List.of("c"));
        for (var l : List.of(
                new BackoffWordNgrams.Limits(1,10,10,10,30,4,10),
                new BackoffWordNgrams.Limits(5,1,10,10,30,4,10),
                new BackoffWordNgrams.Limits(5,10,1,10,30,4,10),
                new BackoffWordNgrams.Limits(5,10,10,1,30,4,10),
                new BackoffWordNgrams.Limits(5,10,10,10,1,4,10)))
            rejects(IllegalArgumentException.class, () -> BackoffWordNgrams.train(docs, 2, l));
        List<String> words = new ArrayList<>();
        for (int i = 129; i >= 0; i--) words.add("word" + i);
        var wide = BackoffWordNgrams.train(List.of(words), 1);
        var sorted = new ArrayList<>(words); sorted.sort(String::compareTo);
        for (int k : new int[]{0,1,15,16,17,31,32,33,63,64,65,128,129,130,Integer.MAX_VALUE}) {
            var result = wide.rank(List.of(), k);
            require(result.size() == Math.min(k,words.size()), "boundary size");
            for (int i=0;i<result.size();i++) require(result.get(i).token().equals(sorted.get(i)), "lexical boundary");
        }
    }
    private static void longCounts() throws Exception {
        // Synthetic private-row fixture: no claim of training a multi-exabyte corpus.
        Class<?> row = Class.forName(BackoffWordNgrams.class.getName() + "$Counts");
        Class<?> context = Class.forName(BackoffWordNgrams.class.getName() + "$Context");
        Constructor<?> rowCtor = row.getDeclaredConstructor(Map.class, long.class); rowCtor.setAccessible(true);
        Constructor<?> contextCtor = context.getDeclaredConstructor(List.class); contextCtor.setAccessible(true);
        Constructor<BackoffWordNgrams.Model> modelCtor = BackoffWordNgrams.Model.class
                .getDeclaredConstructor(int.class,long.class,Map.class); modelCtor.setAccessible(true);
        for (long high : new long[]{(1L << 53)+1, Long.MAX_VALUE/2}) {
            Map<String,Long> values = new HashMap<>();
            values.put("a",high-1); values.put("z",high); values.put("tie-b",1L); values.put("tie-a",1L);
            long total = Math.addExact(Math.addExact(high, high-1), 2L);
            var model = modelCtor.newInstance(1,total,Map.of(contextCtor.newInstance(List.of()),rowCtor.newInstance(values,total)));
            var result = model.rank(List.of(),Integer.MAX_VALUE);
            require(result.stream().map(BackoffWordNgrams.Prediction::token).toList()
                    .equals(List.of("z","a","tie-a","tie-b")), "exact long ranking");
            require(model.exactCount(List.of(),"z") == high, "exact long count");
            require(result.get(0).probability() == high/(double)total, "probability division unchanged");
            values.clear(); require(model.exactCount(List.of(),"z") == high, "frozen row ownership");
        }
        System.out.println("LONG_COUNTS_PASS checks=" + checks);
    }
    private static void allocation() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation instrumentation unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        List<List<String>> docs = new ArrayList<>();
        for(int i=0;i<6000;i++) docs.add(List.of("center","word"+i));
        var model = BackoffWordNgrams.train(docs,2);
        require(model.rank(List.of("center"),Integer.MAX_VALUE).size()==6000,"allocation fixture");
        for(int i=0;i<300;i++) sink=model.rank(List.of("center"),10);
        long id=Thread.currentThread().threadId();
        long start=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<100;i++) sink=model.rank(List.of("center"),10);
        long bytes=(bean.getThreadAllocatedBytes(id)-start)/100;
        System.out.println("WORD_RANK_ALLOCATION bytesPerQuery="+bytes+" candidates=6000 k=10");
        require(bytes<8192,"QUERY_SORT_ALLOCATION: "+bytes);
    }
}
