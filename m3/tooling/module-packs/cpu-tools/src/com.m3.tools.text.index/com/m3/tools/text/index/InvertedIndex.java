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
    private final Set<String> terms;

    private InvertedIndex(String[] vocabulary, int[] starts, int[] postings) {
        this.vocabulary = vocabulary; this.starts = starts; this.postings = postings;
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
        Objects.requireNonNull(documents);
        int total = 0;
        for (List<String> document : documents) total = Math.addExact(total, Objects.requireNonNull(document).size());
        String[] tokens = new String[total]; int[] documentIds = new int[total];
        int at = 0;
        for (int id = 0; id < documents.size(); id++) {
            for (String term : documents.get(id)) {
                tokens[at] = Objects.requireNonNull(term); documentIds[at++] = id;
            }
        }
        String[] sorted = tokens.clone(); Arrays.sort(sorted);
        int unique = 0;
        for (String term : sorted) {
            if (unique == 0 || !term.equals(sorted[unique - 1])) sorted[unique++] = term;
        }
        String[] vocabulary = Arrays.copyOf(sorted, unique);
        int[] starts = new int[Math.addExact(unique, 1)], lastDocument = new int[unique];
        Arrays.fill(lastDocument, -1);
        for (int i = 0; i < total; i++) {
            int term = Arrays.binarySearch(vocabulary, tokens[i]);
            if (lastDocument[term] != documentIds[i]) { starts[term + 1]++; lastDocument[term] = documentIds[i]; }
        }
        for (int i = 1; i <= unique; i++) starts[i] = Math.addExact(starts[i], starts[i - 1]);
        int[] cursor = starts.clone(), postings = new int[starts[unique]];
        Arrays.fill(lastDocument, -1);
        for (int i = 0; i < total; i++) {
            int term = Arrays.binarySearch(vocabulary, tokens[i]);
            if (lastDocument[term] != documentIds[i]) {
                postings[cursor[term]++] = documentIds[i]; lastDocument[term] = documentIds[i];
            }
        }
        return new InvertedIndex(vocabulary, starts, postings);
    }

    public int[] documents(String term) {
        int at = Arrays.binarySearch(vocabulary, Objects.requireNonNull(term));
        return at < 0 ? new int[0] : Arrays.copyOfRange(postings, starts[at], starts[at + 1]);
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
