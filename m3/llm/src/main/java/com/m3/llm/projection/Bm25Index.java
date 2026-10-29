// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.projection;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Deterministic inverted-index reference backend, not a replacement for Lucene. */
public final class Bm25Index {
    public static final int MAX_DOCUMENTS = 10_000;
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_]+");
    private record Posting(int document, int frequency) {}
    public record Hit(TextInventoryCell cell, double score) {
        public Hit {
            java.util.Objects.requireNonNull(cell, "cell");
            if (!Double.isFinite(score) || score < 0) throw new IllegalArgumentException("INVALID_SCORE");
        }
    }
    private final List<TextInventoryCell> documents;
    private final Map<String, List<Posting>> postings;
    private final int[] lengths;
    private final double averageLength;
    private final String sourceRoot;

    public Bm25Index(InventorySnapshot snapshot, String namespace, String family, String qualifier, long asOf) {
        sourceRoot = snapshot.root();
        documents = snapshot.scan(namespace, "", family, qualifier, asOf, MAX_DOCUMENTS + 1);
        if (documents.size() > MAX_DOCUMENTS) throw new IllegalArgumentException("INDEX_DOCUMENT_LIMIT");
        lengths = new int[documents.size()];
        Map<String, List<Posting>> index = new TreeMap<>(EntropyValues::compare);
        long totalLength = 0;
        for (int i = 0; i < documents.size(); i++) {
            List<String> tokens = tokenize(documents.get(i).text(), 131_072);
            lengths[i] = tokens.size();
            totalLength += tokens.size();
            Map<String, Integer> counts = new TreeMap<>(EntropyValues::compare);
            for (String token : tokens) counts.merge(token, 1, Integer::sum);
            for (var entry : counts.entrySet()) {
                index.computeIfAbsent(entry.getKey(), unused -> new ArrayList<>()).add(new Posting(i, entry.getValue()));
            }
        }
        index.replaceAll((term, values) -> List.copyOf(values));
        postings = Collections.unmodifiableMap(index);
        averageLength = documents.isEmpty() ? 0 : (double) totalLength / documents.size();
    }

    public String sourceRoot() { return sourceRoot; }
    public int documentCount() { return documents.size(); }

    /** Query terms are a set. Repeating a term cannot inflate its weight. */
    public List<Hit> search(String query, int limit) {
        EntropyValues.text(query, 8192, true);
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("SEARCH_LIMIT");
        TreeSet<String> terms = new TreeSet<>(EntropyValues::compare);
        terms.addAll(tokenize(query, 256));
        if (terms.size() > 64) throw new IllegalArgumentException("QUERY_TERM_LIMIT");
        Map<Integer, Double> scores = new HashMap<>();
        for (String term : terms) {
            List<Posting> matches = postings.getOrDefault(term, List.of());
            double idf = StrictMath.log(1.0 + (documents.size() - matches.size() + 0.5) / (matches.size() + 0.5));
            for (Posting posting : matches) {
                double tf = posting.frequency();
                double norm = 1.2 * (0.25 + 0.75 * lengths[posting.document()] / averageLength);
                scores.merge(posting.document(), idf * tf * 2.2 / (tf + norm), Double::sum);
            }
        }
        return scores.entrySet().stream().map(entry -> new Hit(documents.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingDouble(Hit::score).reversed()
                        .thenComparing(hit -> hit.cell().key().row(), EntropyValues::compare))
                .limit(limit).toList();
    }

    private static List<String> tokenize(String text, int limit) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        var matcher = TOKEN.matcher(normalized);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            if (result.size() == limit) throw new IllegalArgumentException("TOKEN_LIMIT");
            result.add(matcher.group());
        }
        return result;
    }
}
