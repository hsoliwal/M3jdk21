// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Directed symmetric-window word co-occurrence with positive PMI.
 *
 * <p>This class consumes tokens, not text: the caller chooses JDK, Lucene or
 * OpenNLP tokenization and records the provider/version independently. Neither
 * normalization nor sentence-boundary inference is performed. Each input list
 * is one document; pairs never cross documents. Duplicate token positions
 * contribute separately, including equal-token pairs at distinct positions.</p>
 *
 * <p>All budgets are checked before a completed Snapshot can escape. The
 * primitive pair counter stores packed nonnegative integer token IDs without
 * allocating one Java object per event. Vocabulary indices follow String's
 * natural (UTF-16) order, independent of map iteration.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/nlp-ppmi-topk-20261031/CooccurrencePpmi.java.template blob 6a5cc3ec96a3; recipe m3/nlp/recipe/cooccurrence-ppmi-20261106/relocate.py v3
public final class M3CooccurrencePpmi {
    private M3CooccurrencePpmi() { }

    /** Resource budgets. Every bound is enforced, never an advisory hint. */
    public record Limits(int maxDocuments, long maxTokens, int maxVocabulary,
                         int maxDistinctPairs, long maxEvents,
                         int maxWindow, int maxTokenChars) {
        public Limits {
            if (maxDocuments < 1 || maxTokens < 1 || maxVocabulary < 1
                    || maxDistinctPairs < 1 || maxEvents < 1
                    || maxWindow < 1 || maxWindow > 32
                    || maxTokenChars < 1 || maxTokenChars > 1_000_000) {
                throw new IllegalArgumentException("invalid co-occurrence budgets");
            }
        }
        public static Limits defaults() {
            return new Limits(10_000, 1_000_000, 100_000, 1_000_000,
                    20_000_000, 32, 4_096);
        }
    }

    public record PairCount(String center, String context, long count) {
        public PairCount {
            Objects.requireNonNull(center);
            Objects.requireNonNull(context);
            if (count <= 0) throw new IllegalArgumentException("count must be positive");
        }
    }
    public record ContextScore(String context, long count, double ppmi) { }

    /** Immutable public view; all indexes are private and never returned. */
    public static final class Snapshot {
        private final List<String> vocabulary;
        private final Map<String,Integer> ids;
        private final long[] centers;
        private final long[] contexts;
        private final FrozenPairs pairs;
        private final long events;
        private final int window;

        private Snapshot(List<String> vocabulary, Map<String,Integer> ids,
                         long[] centers, long[] contexts, PackedPairs pairs,
                         long events, int window) {
            this.vocabulary = List.copyOf(vocabulary);
            this.ids = Map.copyOf(ids);
            this.centers = centers;
            this.contexts = contexts;
            this.pairs = pairs.freeze();
            this.events = events;
            this.window = window;
        }
        public List<String> vocabulary() { return vocabulary; }
        public long events() { return events; }
        public int window() { return window; }
        public int distinctPairs() { return pairs.keys.length; }
        public long centerCount(String token) {
            Integer id = ids.get(Objects.requireNonNull(token));
            return id == null ? 0 : centers[id];
        }
        public long contextCount(String token) {
            Integer id = ids.get(Objects.requireNonNull(token));
            return id == null ? 0 : contexts[id];
        }
        public long pairCount(String center, String context) {
            Integer a = ids.get(Objects.requireNonNull(center));
            Integer b = ids.get(Objects.requireNonNull(context));
            return a == null || b == null ? 0 : pairs.get(pack(a, b));
        }
        /**
         * Returns 0 for missing pairs or non-positive PMI. Never returns NaN.
         * The logarithm form avoids integer products and loss of long capacity.
         */
        public double ppmi(String center, String context) {
            Integer a = ids.get(Objects.requireNonNull(center));
            Integer b = ids.get(Objects.requireNonNull(context));
            if (a == null || b == null || events == 0) return 0;
            long n = pairs.get(pack(a, b));
            if (n == 0) return 0;
            double value = Math.log(n) + Math.log(events)
                    - Math.log(centers[a]) - Math.log(contexts[b]);
            return Math.max(0.0, value);
        }
        /** Contexts with positive PPMI, sorted by score descending then token ascending. */
        public List<ContextScore> topPositiveContexts(String center, int limit) {
            Objects.requireNonNull(center);
            if (limit < 0) throw new IllegalArgumentException("negative limit");
            Integer id = ids.get(center);
            if (id == null || limit == 0) return List.of();
            int from = pairs.lowerBound(pack(id, 0));
            // IDs are nonnegative list indices, so id+1 still fits signed int.
            int to = pairs.lowerBound(pack(id + 1, 0));
            boolean nativeEnabled = NativeSelection.ENABLED;
            if (from == to) return List.of();
            int k = Math.min(limit, to - from);
            double logEvents = Math.log(events), logCenter = Math.log(centers[id]);
            // Java owns scores; native indices refer to this lexical-order row.
            if (nativeEnabled) {
                double[] values = new double[to - from];
                for (int slot = from; slot < to; slot++) {
                    int j = (int)pairs.keys[slot];
                    values[slot - from] = Math.max(0.0, Math.log(pairs.counts[slot]) + logEvents
                            - logCenter - Math.log(contexts[j]));
                }
                int[] selected = selectPositiveNative(values, k);
                List<ContextScore> result = new ArrayList<>(selected.length);
                for (int offset : selected) {
                    int slot = from + offset;
                    result.add(new ContextScore(vocabulary.get((int)pairs.keys[slot]),
                            pairs.counts[slot], values[offset]));
                }
                return List.copyOf(result);
            }
            PositiveTopK selected = new PositiveTopK();
            for (int slot = from; slot < to; slot++) {
                int j = (int)pairs.keys[slot];
                double score = Math.max(0.0, Math.log(pairs.counts[slot]) + logEvents
                        - logCenter - Math.log(contexts[j]));
                // Row slots and token IDs have the same lexical order.
                if (score > 0) selected.offer(slot, score, k);
            }
            int count = selected.size;
            selected.sort();
            List<ContextScore> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int slot = selected.ids[i];
                result.add(new ContextScore(vocabulary.get((int)pairs.keys[slot]),
                        pairs.counts[slot], selected.scores[i]));
            }
            return List.copyOf(result);
        }
        /** Sorted by center/context UTF-16 lexical identity; never hash-table order. */
        public List<PairCount> pairCounts() {
            List<PairCount> output = new ArrayList<>(pairs.keys.length);
            for (int slot = 0; slot < pairs.keys.length; slot++) {
                long key = pairs.keys[slot];
                int a = (int)(key >>> 32), b = (int)key;
                output.add(new PairCount(vocabulary.get(a), vocabulary.get(b), pairs.counts[slot]));
            }
            return List.copyOf(output);
        }
    }

    public static Snapshot build(List<? extends List<String>> documents, int window) {
        return build(documents, window, Limits.defaults());
    }

    public static Snapshot build(List<? extends List<String>> documents,
                                 int window, Limits limits) {
        Objects.requireNonNull(documents);
        Objects.requireNonNull(limits);
        if (window < 1 || window > limits.maxWindow())
            throw new IllegalArgumentException("window exceeds admitted range");
        if (documents.size() > limits.maxDocuments())
            throw new IllegalArgumentException("document count exceeds budget");
        List<List<String>> frozen = new ArrayList<>(documents.size());
        TreeSet<String> sorted = new TreeSet<>();
        long tokens = 0;
        for (List<String> doc : documents) {
            Objects.requireNonNull(doc, "null document");
            if (doc.size() > limits.maxTokens() - tokens)
                throw new IllegalArgumentException("token count exceeds budget");
            List<String> copy = List.copyOf(doc);
            tokens += copy.size();
            for (String token : copy) {
                if (token.isEmpty() || token.length() > limits.maxTokenChars())
                    throw new IllegalArgumentException("empty or oversized token");
                sorted.add(token);
                if (sorted.size() > limits.maxVocabulary())
                    throw new IllegalArgumentException("vocabulary exceeds budget");
            }
            frozen.add(copy);
        }
        List<String> vocabulary = List.copyOf(sorted);
        Map<String,Integer> ids = new HashMap<>();
        for (int i=0; i<vocabulary.size(); i++) ids.put(vocabulary.get(i), i);
        long[] centers = new long[vocabulary.size()];
        long[] contexts = new long[vocabulary.size()];
        PackedPairs pairs = new PackedPairs(limits.maxDistinctPairs());
        long events = 0;
        for (List<String> doc : frozen) {
            int[] sequence = new int[doc.size()];
            for (int i=0; i<doc.size(); i++) sequence[i] = ids.get(doc.get(i));
            for (int i=0; i<sequence.length; i++) {
                int lo = Math.max(0, i - window);
                int hi = (int)Math.min(sequence.length - 1L, (long)i + window);
                for (int j=lo; j<=hi; j++) {
                    if (i == j) continue;
                    if (events >= limits.maxEvents())
                        throw new IllegalArgumentException("co-occurrence event budget exhausted");
                    int a = sequence[i], b = sequence[j];
                    pairs.increment(pack(a,b));
                    centers[a] = Math.addExact(centers[a],1L);
                    contexts[b] = Math.addExact(contexts[b],1L);
                    events++;
                }
            }
        }
        return new Snapshot(vocabulary,ids,centers,contexts,pairs,events,window);
    }

    /** The root is the worst retained result: low score, then high lexical ID. */
    private static final class PositiveTopK {
        private int[] ids;
        private double[] scores;
        private int size;
        private void grow(int capacity) {
            int length = ids == null ? 0 : ids.length;
            int next = length == 0 ? Math.min(capacity, 16)
                    : length + Math.min(length, capacity - length);
            // Publish the pair only after both allocations succeed.
            int[] nextIds = ids == null ? new int[next] : Arrays.copyOf(ids, next);
            double[] nextScores = scores == null ? new double[next] : Arrays.copyOf(scores, next);
            ids = nextIds;
            scores = nextScores;
        }
        private static boolean worse(int a, double x, int b, double y) {
            return x < y || (x == y && a > b);
        }
        void offer(int id, double score, int capacity) {
            if (size < capacity) {
                if (ids == null || size == ids.length) grow(capacity);
                int p = size++;
                while (p != 0) {
                    int parent = (p - 1) >>> 1;
                    if (!worse(id, score, ids[parent], scores[parent])) break;
                    ids[p] = ids[parent]; scores[p] = scores[parent]; p = parent;
                }
                ids[p] = id; scores[p] = score;
            } else if (worse(ids[0], scores[0], id, score)) {
                down(id, score, size);
            }
        }
        private void down(int id, double score, int length) {
            int p = 0;
            while (p < length / 2) {
                int child = p * 2 + 1;
                if (child + 1 < length && worse(ids[child + 1], scores[child + 1],
                        ids[child], scores[child])) child++;
                if (!worse(ids[child], scores[child], id, score)) break;
                ids[p] = ids[child]; scores[p] = scores[child]; p = child;
            }
            ids[p] = id; scores[p] = score;
        }
        void sort() {
            for (int last = size - 1; last > 0; last--) {
                int id = ids[0]; double score = scores[0];
                down(ids[last], scores[last], last);
                ids[last] = id; scores[last] = score;
            }
        }
    }

    /** Optional provider, fixed on first nonempty ranking request; never auto-enabled. */
    private static final class NativeSelection {
        private static final boolean ENABLED = load();
        private static boolean load() {
            final boolean requested;
            try { requested = Boolean.getBoolean("m3.ppmi.nativeTopK"); }
            catch (SecurityException denied) { return false; }
            if (requested) System.loadLibrary("m3_ppmi_topk");
            return requested;
        }
    }

    // Bulk copied scores only: no Java heap pointer or reference escapes this call.
    private static native int[] selectPositiveNative(double[] scores, int limit);

    private static long pack(int a, int b) {
        return ((long)a << 32) | Integer.toUnsignedLong(b);
    }

    /** Immutable sorted pair owner: no hash slack, duplicate index or per-row objects. */
    private static final class FrozenPairs {
        private final long[] keys;
        private final long[] counts;
        FrozenPairs(long[] keys, long[] counts) {
            this.keys = keys;
            this.counts = counts;
        }
        long get(long key) {
            int slot = Arrays.binarySearch(keys, key);
            return slot < 0 ? 0L : counts[slot];
        }
        int lowerBound(long key) {
            // Packed keys are unique, so a found key is already the lower bound.
            int slot = Arrays.binarySearch(keys, key);
            return slot < 0 ? ~slot : slot;
        }
    }

    /** Training-only open-addressed pair counter; discarded after immutable freeze. */
    private static final class PackedPairs {
        private static final long EMPTY = -1L;
        private long[] keys = new long[16];
        private long[] counts = new long[16];
        private final int limit;
        private int size;
        PackedPairs(int limit) {
            this.limit = limit;
            Arrays.fill(keys,EMPTY);
        }
        private static int hash(long key) {
            long z = key;
            z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
            z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
            return (int)(z ^ (z >>> 33));
        }
        private int locate(long key) {
            int slot = hash(key) & (keys.length - 1);
            while (keys[slot] != EMPTY && keys[slot] != key)
                slot = (slot + 1) & (keys.length - 1);
            return slot;
        }
        long get(long key) {
            int slot = locate(key);
            return keys[slot] == EMPTY ? 0 : counts[slot];
        }
        void increment(long key) {
            int slot = locate(key);
            if (keys[slot] == key) {
                counts[slot] = Math.addExact(counts[slot], 1L);
                return;
            }
            if (size == limit)
                throw new IllegalArgumentException("distinct pair budget exhausted");
            if ((long)(size + 1) * 10L >= (long)keys.length * 7L) {
                grow();
                slot = locate(key);
            }
            keys[slot] = key;
            counts[slot] = 1;
            size++;
        }
        private void grow() {
            if (keys.length >= (1 << 26))
                throw new IllegalArgumentException("pair counter storage limit");
            long[] oldKeys = keys, oldCounts = counts;
            keys = new long[oldKeys.length * 2];
            counts = new long[keys.length];
            Arrays.fill(keys,EMPTY);
            for (int i=0; i<oldKeys.length; i++) {
                if (oldKeys[i] != EMPTY) {
                    int slot = locate(oldKeys[i]);
                    keys[slot] = oldKeys[i]; counts[slot] = oldCounts[i];
                }
            }
        }
        FrozenPairs freeze() {
            long[] ordered = sortedKeys();
            long[] values = new long[ordered.length];
            for (int i = 0; i < ordered.length; i++) values[i] = get(ordered[i]);
            return new FrozenPairs(ordered, values);
        }
        long[] sortedKeys() {
            long[] result = new long[size];
            int used = 0;
            for (long key : keys) if (key != EMPTY) result[used++] = key;
            if (used != size) throw new IllegalStateException("pair count corruption");
            Arrays.sort(result);
            return result;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("--self-test"))
            throw new IllegalArgumentException("--self-test required");
        Snapshot snapshot = build(List.of(List.of("a","b","a")),1);
        if (snapshot.events()!=4 || snapshot.pairCount("a","b")!=2
                || snapshot.pairCount("b","a")!=2
                || Math.abs(snapshot.ppmi("a","b")-Math.log(2))>1.0e-12)
            throw new AssertionError("PPMI sample contract");
        System.out.println("M3_JMOD_TEST_PASS text.ppmi");
    }
}
