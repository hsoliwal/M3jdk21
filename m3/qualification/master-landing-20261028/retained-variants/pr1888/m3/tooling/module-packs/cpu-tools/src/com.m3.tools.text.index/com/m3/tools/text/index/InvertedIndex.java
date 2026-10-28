// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-18: mapped receiver with packed primitive storage; see DONOR_CHANGES.tsv.
package com.m3.tools.text.index;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/** Immutable exact postings over canonical String references and one contiguous primitive lane. */
public final class InvertedIndex {
    private final String[] vocabulary;
    private final int[] starts;
    private final int[] postings;
    private final int[] frequencies;
    private final int[] documentLengths;
    private final int totalTokens;
    private final Set<String> terms;

    private InvertedIndex(String[] vocabulary, int[] starts, int[] postings, int[] frequencies,
                          int[] documentLengths, int totalTokens) {
        this.vocabulary = vocabulary; this.starts = starts; this.postings = postings;
        this.frequencies = frequencies; this.documentLengths = documentLengths;
        this.totalTokens = totalTokens;
        this.terms = Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() { return vocabulary.length; }
            @Override public boolean contains(Object value) {
                Objects.requireNonNull(value);
                return value instanceof String text && Arrays.binarySearch(vocabulary, text) >= 0;
            }
            @Override public Iterator<String> iterator() {
                return new Iterator<>() {
                    private int next;
                    @Override public boolean hasNext() { return next < vocabulary.length; }
                    @Override public String next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return vocabulary[next++];
                    }
                };
            }
        });
    }

    /** Keeps distinct document IDs per term, in ascending document order; no input container is retained. */
    public static InvertedIndex build(List<? extends List<String>> documents) {
        return build(documents, () -> { });
    }

    /** The same postings with parallel primitive term-frequency and document-length lanes. */
    public static InvertedIndex build(List<? extends List<String>> documents, Runnable checkpoint) {
        Objects.requireNonNull(documents);
        Objects.requireNonNull(checkpoint, "checkpoint");
        checkpoint.run();
        int total = 0;
        int[] documentLengths = new int[documents.size()];
        for (int id = 0; id < documents.size(); id++) {
            if ((id & 1023) == 0) checkpoint.run();
            documentLengths[id] = Objects.requireNonNull(documents.get(id), "document").size();
            total = Math.addExact(total, documentLengths[id]);
        }
        String[] tokens = new String[total]; int[] documentIds = new int[total];
        int at = 0;
        for (int id = 0; id < documents.size(); id++) {
            for (String term : documents.get(id)) {
                if ((at & 4095) == 0) checkpoint.run();
                tokens[at] = Objects.requireNonNull(term); documentIds[at++] = id;
            }
        }
        checkpoint.run();
        String[] sorted = tokens.clone(); Arrays.sort(sorted);
        int unique = 0;
        for (String term : sorted) {
            if ((unique & 4095) == 0) checkpoint.run();
            if (unique == 0 || !term.equals(sorted[unique - 1])) sorted[unique++] = term;
        }
        String[] vocabulary = Arrays.copyOf(sorted, unique);
        int[] starts = new int[Math.addExact(unique, 1)], lastDocument = new int[unique];
        Arrays.fill(lastDocument, -1);
        for (int i = 0; i < total; i++) {
            if ((i & 4095) == 0) checkpoint.run();
            int term = Arrays.binarySearch(vocabulary, tokens[i]);
            if (lastDocument[term] != documentIds[i]) { starts[term + 1]++; lastDocument[term] = documentIds[i]; }
        }
        for (int i = 1; i <= unique; i++) starts[i] = Math.addExact(starts[i], starts[i - 1]);
        int[] cursor = starts.clone(), postings = new int[starts[unique]];
        int[] frequencies = new int[postings.length];
        Arrays.fill(lastDocument, -1);
        for (int i = 0; i < total; i++) {
            if ((i & 4095) == 0) checkpoint.run();
            int term = Arrays.binarySearch(vocabulary, tokens[i]);
            if (lastDocument[term] != documentIds[i]) {
                int slot = cursor[term]++;
                postings[slot] = documentIds[i]; frequencies[slot] = 1;
                lastDocument[term] = documentIds[i];
            } else {
                int slot = cursor[term] - 1;
                frequencies[slot] = Math.addExact(frequencies[slot], 1);
            }
        }
        checkpoint.run();
        return new InvertedIndex(vocabulary, starts, postings, frequencies, documentLengths, total);
    }

    public int[] documents(String term) {
        int at = Arrays.binarySearch(vocabulary, Objects.requireNonNull(term));
        return at < 0 ? new int[0] : Arrays.copyOfRange(postings, starts[at], starts[at + 1]);
    }

    /** Exact token occurrences parallel to {@link #documents(String)}. */
    public int[] termFrequencies(String term) {
        int at = Arrays.binarySearch(vocabulary, Objects.requireNonNull(term));
        return at < 0 ? new int[0] : Arrays.copyOfRange(frequencies, starts[at], starts[at + 1]);
    }

    public int documentCount() { return documentLengths.length; }
    public int totalTokenCount() { return totalTokens; }
    public int documentLength(int documentId) {
        Objects.checkIndex(documentId, documentLengths.length);
        return documentLengths[documentId];
    }
    public int documentFrequency(String term) {
        int at = Arrays.binarySearch(vocabulary, Objects.requireNonNull(term));
        return at < 0 ? 0 : starts[at + 1] - starts[at];
    }

    @FunctionalInterface public interface PostingConsumer {
        void accept(int documentId, int termFrequency);
    }

    /** Allocation-free primitive-lane traversal; does not expose the parent arrays. */
    public void forEachPosting(String term, PostingConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int at = Arrays.binarySearch(vocabulary, Objects.requireNonNull(term));
        if (at < 0) return;
        for (int slot = starts[at]; slot < starts[at + 1]; slot++) {
            consumer.accept(postings[slot], frequencies[slot]);
        }
    }

    /** Immutable lexicographically ordered view; elements remain the original String values. */
    public Set<String> terms() { return terms; }

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].equals("--self-test"))
            throw new IllegalArgumentException("--self-test");
        InvertedIndex index = build(List.of(List.of("m3", "search", "m3"), List.of("search"), List.of()));
        if (!Arrays.equals(index.documents("m3"), new int[]{0})) throw new AssertionError("posting dedup");
        if (!Arrays.equals(index.documents("search"), new int[]{0, 1})) throw new AssertionError("posting order");
        int[] mutable = index.documents("search"); mutable[0] = 99;
        if (index.documents("search")[0] != 0) throw new AssertionError("defensive");
        System.out.println("M3_JMOD_TEST_PASS text.index");
    }
}
