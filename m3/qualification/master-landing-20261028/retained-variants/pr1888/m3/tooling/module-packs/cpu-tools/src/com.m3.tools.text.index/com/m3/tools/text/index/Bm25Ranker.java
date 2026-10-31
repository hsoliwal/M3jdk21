// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.index;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Bounded Okapi BM25 over the incumbent primitive-lane {@link InvertedIndex}.
 * Documents and queries contain exact caller-selected String tokens: the
 * JDK/M3String and WordTokenizer own decoding and word-boundary policy.
 * Query repetitions are intentionally set-like; terms are scored in sorted
 * UTF-16 order so query permutation cannot alter floating-point summation.
 *
 * <p>The formula follows the published Apache Lucene BM25 contract, but this
 * class is an independently implemented, non-Lucene scoring atom. Scores are
 * ranking signals, never equality or semantic-relevance proofs.
 */
public final class Bm25Ranker {
    public static final int MAX_DOCUMENTS = 10_000;
    public static final int MAX_TOKENS = 1_000_000;
    public static final int MAX_TOKEN_UNITS = 16 * 1024 * 1024;
    public static final int MAX_QUERY_TERMS = 1_024;
    public static final int MAX_RESULTS = 10_000;

    private Bm25Ranker() { }

    public record Parameters(double k1, double b) {
        public Parameters {
            if (!Double.isFinite(k1) || k1 < 0.0d || !Double.isFinite(b) || b < 0.0d || b > 1.0d) {
                throw new IllegalArgumentException("invalid BM25 k1/b");
            }
        }
        public static Parameters defaults() { return new Parameters(1.2d, 0.75d); }
    }

    public record Hit(int documentId, double score) { }

    /** Immutable primitive result lanes and an exact full-corpus hit count. */
    public static final class Result {
        private final int[] documentIds;
        private final double[] scores;
        private final int totalMatches;

        private Result(int[] documentIds, double[] scores, int totalMatches) {
            this.documentIds = documentIds.clone();
            this.scores = scores.clone();
            this.totalMatches = totalMatches;
        }
        public int size() { return documentIds.length; }
        public int totalMatches() { return totalMatches; }
        public boolean complete() { return size() == totalMatches; }
        public Hit hit(int ordinal) { return new Hit(documentIds[ordinal], scores[ordinal]); }
        public List<Hit> hits() {
            List<Hit> result = new ArrayList<>(size());
            for (int i = 0; i < size(); i++) result.add(hit(i));
            return List.copyOf(result);
        }
    }

    /** Immutable parent-owned postings, term-frequency lanes and corpus identity. */
    public static final class Prepared {
        private final InvertedIndex index;
        private final Parameters parameters;
        private final double averageLength;
        private final String sha256;

        private Prepared(InvertedIndex index, Parameters parameters, String sha256) {
            this.index = index;
            this.parameters = parameters;
            this.averageLength = index.documentCount() == 0 ? 0.0d
                    : index.totalTokenCount() / (double) index.documentCount();
            this.sha256 = sha256;
        }

        public int documentCount() { return index.documentCount(); }
        public double averageDocumentLength() { return averageLength; }
        public Parameters parameters() { return parameters; }
        public String sha256() { return sha256; }

        public Result rank(List<String> query, int maxResults) {
            return rank(query, maxResults, () -> { });
        }

        /** Top-K uses an integer min-heap whose root is the worst retained hit. */
        public Result rank(List<String> query, int maxResults, Runnable checkpoint) {
            Objects.requireNonNull(query, "query");
            Objects.requireNonNull(checkpoint, "checkpoint");
            if (query.size() > MAX_QUERY_TERMS) throw new IllegalArgumentException("BM25 query token budget");
            if (maxResults < 0 || maxResults > MAX_RESULTS) {
                throw new IllegalArgumentException("BM25 result budget");
            }
            checkpoint.run();
            TreeSet<String> terms = new TreeSet<>();
            int units = 0;
            for (String term : query) {
                String word = Objects.requireNonNull(term, "query term");
                units = Math.addExact(units, word.length());
                if (units > MAX_TOKEN_UNITS) throw new IllegalArgumentException("BM25 query UTF-16 budget");
                terms.add(word);
            }
            double[] scores = new double[index.documentCount()];
            long[] visited = {0};
            for (String term : terms) {
                checkpoint.run();
                int documentFrequency = index.documentFrequency(term);
                if (documentFrequency == 0) continue;
                double idf = StrictMath.log1p((index.documentCount() - documentFrequency + 0.5d)
                        / (documentFrequency + 0.5d));
                index.forEachPosting(term, (documentId, frequency) -> {
                    if ((visited[0]++ & 4095L) == 0L) checkpoint.run();
                    double normalized = parameters.k1() * (1.0d - parameters.b()
                            + parameters.b() * index.documentLength(documentId) / averageLength);
                    scores[documentId] += idf * (frequency * (parameters.k1() + 1.0d))
                            / (frequency + normalized);
                });
            }
            int[] heap = new int[Math.min(maxResults, index.documentCount())];
            int size = 0, total = 0;
            for (int id = 0; id < scores.length; id++) {
                if ((id & 4095) == 0) checkpoint.run();
                if (!(scores[id] > 0.0d)) continue;
                total++;
                if (size < heap.length) {
                    heap[size] = id;
                    siftUp(heap, size++, scores);
                } else if (size > 0 && worse(heap[0], id, scores)) {
                    heap[0] = id;
                    siftDown(heap, size, 0, scores);
                }
            }
            int[] documentIds = new int[size];
            double[] rankedScores = new double[size];
            for (int ordinal = size - 1; ordinal >= 0; ordinal--) {
                checkpoint.run();
                int worst = heap[0];
                documentIds[ordinal] = worst;
                rankedScores[ordinal] = scores[worst];
                heap[0] = heap[--size];
                if (size > 0) siftDown(heap, size, 0, scores);
            }
            checkpoint.run();
            return new Result(documentIds, rankedScores, total);
        }
    }

    public static Prepared prepare(List<? extends List<String>> documents) {
        return prepare(documents, Parameters.defaults(), () -> { });
    }

    public static Prepared prepare(List<? extends List<String>> documents, Parameters parameters) {
        return prepare(documents, parameters, () -> { });
    }

    /** Snapshots caller token lists before indexing and hashing exact UTF-16 identities. */
    public static Prepared prepare(List<? extends List<String>> documents, Parameters parameters,
                                   Runnable checkpoint) {
        Objects.requireNonNull(documents, "documents");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (documents.size() > MAX_DOCUMENTS) throw new IllegalArgumentException("BM25 document budget");
        checkpoint.run();
        MessageDigest hash = digest();
        number(hash, documents.size());
        number(hash, Double.doubleToLongBits(parameters.k1()));
        number(hash, Double.doubleToLongBits(parameters.b()));
        List<List<String>> snapshot = new ArrayList<>(documents.size());
        int tokens = 0, units = 0;
        for (int id = 0; id < documents.size(); id++) {
            checkpoint.run();
            List<String> document = List.copyOf(Objects.requireNonNull(documents.get(id), "document"));
            tokens = Math.addExact(tokens, document.size());
            if (tokens > MAX_TOKENS) throw new IllegalArgumentException("BM25 corpus token budget");
            number(hash, document.size());
            int seen = 0;
            for (String term : document) {
                if ((seen++ & 4095) == 0) checkpoint.run();
                units = Math.addExact(units, term.length());
                if (units > MAX_TOKEN_UNITS) throw new IllegalArgumentException("BM25 corpus UTF-16 budget");
                text(hash, term, checkpoint);
            }
            snapshot.add(document);
        }
        InvertedIndex index = InvertedIndex.build(snapshot, checkpoint);
        checkpoint.run();
        return new Prepared(index, parameters, HexFormat.of().formatHex(hash.digest()));
    }

    private static boolean worse(int left, int right, double[] scores) {
        int compared = Double.compare(scores[left], scores[right]);
        return compared < 0 || (compared == 0 && left > right);
    }

    private static void siftUp(int[] heap, int at, double[] scores) {
        while (at > 0) {
            int parent = (at - 1) >>> 1;
            if (!worse(heap[at], heap[parent], scores)) break;
            int swapped = heap[at]; heap[at] = heap[parent]; heap[parent] = swapped;
            at = parent;
        }
    }

    private static void siftDown(int[] heap, int size, int at, double[] scores) {
        while ((long) at * 2 + 1 < size) {
            int child = at * 2 + 1;
            if (child + 1 < size && worse(heap[child + 1], heap[child], scores)) child++;
            if (!worse(heap[child], heap[at], scores)) break;
            int swapped = heap[at]; heap[at] = heap[child]; heap[child] = swapped;
            at = child;
        }
    }

    private static MessageDigest digest() {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update("m3-bm25-exact-utf16-v1".getBytes(StandardCharsets.US_ASCII));
            return hash;
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void text(MessageDigest hash, String word, Runnable checkpoint) {
        number(hash, word.length());
        for (int i = 0; i < word.length(); i++) {
            if ((i & 4095) == 0) checkpoint.run();
            char unit = word.charAt(i);
            hash.update((byte) (unit >>> 8));
            hash.update((byte) unit);
        }
    }

    private static void number(MessageDigest hash, int value) {
        hash.update((byte) (value >>> 24));
        hash.update((byte) (value >>> 16));
        hash.update((byte) (value >>> 8));
        hash.update((byte) value);
    }

    private static void number(MessageDigest hash, long value) {
        for (int shift = 56; shift >= 0; shift -= 8) hash.update((byte) (value >>> shift));
    }
}
