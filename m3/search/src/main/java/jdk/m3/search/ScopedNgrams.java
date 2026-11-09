// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact, bounded n-gram precompute. Instances never silently combine corpora or ID generations. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexScopedNgrams.java blob 66a18d7d622a; recipe m3/nlp/recipe/relocate.py v1
public final class ScopedNgrams {
    public record Scope(String source, String revision, String language, long generation, String tokenKind) {
        public Scope { text(source); text(revision); text(language); text(tokenKind); }
    }

    /** Defensive primitive key; supports any admitted long ID, including signed packed IDs. */
    public static final class Tuple implements Comparable<Tuple> {
        private final long[] ids;
        private final int hash;
        public Tuple(long... ids) { this.ids = ids.clone(); hash = Arrays.hashCode(this.ids); }
        public long[] ids() { return ids.clone(); }
        public int size() { return ids.length; }
        private Tuple prefix() { return new Tuple(Arrays.copyOf(ids, ids.length - 1)); }
        @Override public int compareTo(Tuple other) { return Arrays.compare(ids, other.ids); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) { return other instanceof Tuple t && Arrays.equals(ids, t.ids); }
    }

    public record Candidate(long nextId, long count, double conditionalProbability) { }
    private final Scope scope;
    private final int maxOrder;
    private final int maxDistinct;
    private final int maxBatchTokens;
    private final Map<Tuple, Long> counts = new HashMap<>();
    private final Map<Tuple, Long> totals = new HashMap<>();

    public ScopedNgrams(Scope scope, int maxOrder, int maxDistinct, int maxBatchTokens) {
        this.scope = Objects.requireNonNull(scope, "scope");
        if (maxOrder < 1 || maxOrder > 64 || maxDistinct < 1 || maxBatchTokens < 1) {
            throw new IllegalArgumentException("ngram budgets");
        }
        this.maxOrder = maxOrder; this.maxDistinct = maxDistinct; this.maxBatchTokens = maxBatchTokens;
    }

    /** One sentence/document boundary. No n-grams cross between calls. Atomic on rejected batches. */
    public synchronized void observe(Scope expected, long[] tokens, long multiplicity) {
        requireScope(expected); Objects.requireNonNull(tokens, "tokens");
        if (multiplicity < 1 || tokens.length > maxBatchTokens) throw new IllegalArgumentException("batch budget/count");
        Map<Tuple, Long> delta = new HashMap<>();
        for (int start = 0; start < tokens.length; start++) {
            for (int n = 1; n <= Math.min(maxOrder, tokens.length - start); n++) {
                Tuple key = new Tuple(Arrays.copyOfRange(tokens, start, start + n));
                delta.merge(key, multiplicity, Math::addExact);
                if (delta.size() > maxDistinct) throw new IllegalArgumentException("distinct tuple budget");
            }
        }
        commit(delta);
    }

    /** Import a counted tuple. Caller owns source-record deduplication and tokenization consistency. */
    public synchronized void add(Scope expected, long[] gram, long count) {
        requireScope(expected); Objects.requireNonNull(gram, "gram");
        if (count < 1 || gram.length < 1 || gram.length > maxOrder) throw new IllegalArgumentException("tuple/count");
        commit(Map.of(new Tuple(gram), count));
    }

    private void commit(Map<Tuple, Long> delta) {
        Map<Tuple, Long> prefixDelta = new HashMap<>();
        int distinct = counts.size();
        for (var entry : delta.entrySet()) {
            if (!counts.containsKey(entry.getKey()) && ++distinct > maxDistinct) {
                throw new IllegalArgumentException("distinct tuple budget");
            }
            Math.addExact(counts.getOrDefault(entry.getKey(), 0L), entry.getValue());
            prefixDelta.merge(entry.getKey().prefix(), entry.getValue(), Math::addExact);
        }
        prefixDelta.forEach((key, value) -> Math.addExact(totals.getOrDefault(key, 0L), value));
        delta.forEach((key, value) -> counts.merge(key, value, Math::addExact));
        prefixDelta.forEach((key, value) -> totals.merge(key, value, Math::addExact));
    }

    public synchronized Snapshot freeze() { return new Snapshot(scope, counts, totals); }
    private void requireScope(Scope expected) {
        if (!scope.equals(expected)) throw new IllegalArgumentException("source/language/generation mismatch");
    }

    /** Immutable snapshot; sparse observed-continuation maximum-likelihood probabilities, not smoothing. */
    public static final class Snapshot {
        private final Scope scope;
        private final Map<Tuple, Long> counts;
        private final Map<Tuple, Long> totals;
        private Snapshot(Scope scope, Map<Tuple, Long> counts, Map<Tuple, Long> totals) {
            this.scope = scope; this.counts = Map.copyOf(counts); this.totals = Map.copyOf(totals);
        }
        public Scope scope() { return scope; }
        public int distinctTuples() { return counts.size(); }
        public long count(long... gram) { return counts.getOrDefault(new Tuple(gram), 0L); }
        public long contextTotal(long... prefix) { return totals.getOrDefault(new Tuple(prefix), 0L); }
        /** Deterministic export order; returned keys and map entries are immutable. */
        public List<Map.Entry<Tuple, Long>> entries() {
            return counts.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        }
        /** O(distinctTuples * prefixLength + matches * log k), O(k) result working memory. */
        public List<Candidate> top(long[] prefix, int k) {
            Objects.requireNonNull(prefix, "prefix");
            if (prefix.length >= 64 || k < 1 || k > 4096) throw new IllegalArgumentException("query budget");
            long denominator = contextTotal(prefix);
            if (denominator == 0) return List.of();
            // Keep a primitive worst-first heap. Candidate records are projected only for results.
            long[] nextIds = new long[Math.min(k, 16)];
            long[] frequencies = new long[nextIds.length];
            int size = 0;
            for (var entry : counts.entrySet()) {
                long[] ids = entry.getKey().ids;
                if (!matches(ids, prefix)) continue;
                long nextId = ids[ids.length - 1];
                long frequency = entry.getValue();
                if (size < k) {
                    if (size == nextIds.length) {
                        int capacity = Math.min(k, nextIds.length * 2);
                        nextIds = Arrays.copyOf(nextIds, capacity);
                        frequencies = Arrays.copyOf(frequencies, capacity);
                    }
                    int at = size++;
                    while (at > 0) {
                        int parent = (at - 1) >>> 1;
                        if (!worse(frequency, nextId, frequencies[parent], nextIds[parent])) break;
                        frequencies[at] = frequencies[parent];
                        nextIds[at] = nextIds[parent];
                        at = parent;
                    }
                    frequencies[at] = frequency;
                    nextIds[at] = nextId;
                } else if (worse(frequencies[0], nextIds[0], frequency, nextId)) {
                    sink(nextIds, frequencies, size, nextId, frequency);
                }
            }
            // Heap-sort worst to the end, obtaining best-first primitive lanes in-place.
            for (int end = size - 1; end > 0; end--) {
                long incomingId = nextIds[end];
                long incomingCount = frequencies[end];
                nextIds[end] = nextIds[0];
                frequencies[end] = frequencies[0];
                sink(nextIds, frequencies, end, incomingId, incomingCount);
            }
            var results = new java.util.ArrayList<Candidate>(size);
            for (int i = 0; i < size; i++) {
                results.add(new Candidate(nextIds[i], frequencies[i], (double) frequencies[i] / denominator));
            }
            return results.stream().toList();
        }
        /** Lower frequency is worse; a higher signed ID loses exact-count ties. */
        private static boolean worse(long countA, long idA, long countB, long idB) {
            return countA < countB || (countA == countB && idA > idB);
        }
        /** Move one primitive candidate into its place in a worst-first heap. */
        private static void sink(long[] ids, long[] frequencies, int size, long nextId, long count) {
            int at = 0;
            while (at < (size >>> 1)) {
                int child = at * 2 + 1;
                if (child + 1 < size && worse(frequencies[child + 1], ids[child + 1],
                        frequencies[child], ids[child])) child++;
                if (!worse(frequencies[child], ids[child], count, nextId)) break;
                ids[at] = ids[child];
                frequencies[at] = frequencies[child];
                at = child;
            }
            ids[at] = nextId;
            frequencies[at] = count;
        }
        private static boolean matches(long[] ids, long[] prefix) {
            if (ids.length != prefix.length + 1) return false;
            for (int i = 0; i < prefix.length; i++) if (ids[i] != prefix[i]) return false;
            return true;
        }
    }
    private static void text(String value) {
        if (Objects.requireNonNull(value, "scope value").isBlank()) throw new IllegalArgumentException("blank scope");
    }
}
