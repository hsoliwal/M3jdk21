/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable target contract for one operator-supplied Synexia language-pack
 * n-gram projection.
 *
 * <p>This type owns neither a bundled corpus nor a second lexical interner.
 * Its maps are supplied by the operator, copied at admission, and scoped by
 * the exact source identity and tokenizer revision that produced them. The
 * probability methods intentionally mirror Synexia's LanguageNgramModel so a
 * receiver can be differentially checked without changing the source
 * semantics.</p>
 */
public final class M3LanguageNgramPrecompute {
    private static final double SMOOTHING = 0.01d;

    private final String sourceId;
    private final String sourceRevision;
    private final String languageTag;
    private final String sourceFingerprint;
    private final String tokenizerVersion;
    private final Map<String, Long> unigrams;
    private final Map<Bigram, Long> bigrams;
    private final long totalUnigrams;

    public M3LanguageNgramPrecompute(
            String sourceId,
            String sourceRevision,
            String languageTag,
            String sourceFingerprint,
            String tokenizerVersion,
            Map<String, Long> unigrams,
            Map<Bigram, Long> bigrams) {
        this.sourceId = text(sourceId, "sourceId");
        this.sourceRevision = text(sourceRevision, "sourceRevision");
        this.languageTag = text(languageTag, "languageTag");
        this.sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
        this.tokenizerVersion = text(tokenizerVersion, "tokenizerVersion");
        this.unigrams = immutableCounts(unigrams, "unigram");
        this.bigrams = immutableCounts(bigrams, "bigram");
        this.totalUnigrams = this.unigrams.values().stream()
                .mapToLong(Long::longValue).sum();
    }

    public String sourceId() { return sourceId; }
    public String sourceRevision() { return sourceRevision; }
    public String languageTag() { return languageTag; }
    public String sourceFingerprint() { return sourceFingerprint; }
    public String tokenizerVersion() { return tokenizerVersion; }
    public int unigramCount() { return unigrams.size(); }
    public int bigramCount() { return bigrams.size(); }
    public long totalUnigrams() { return totalUnigrams; }
    public boolean hasBigrams() { return !bigrams.isEmpty(); }
    public Map<String, Long> unigrams() { return unigrams; }
    public Map<Bigram, Long> bigrams() { return bigrams; }

    public long unigramFrequency(String word) {
        return unigrams.getOrDefault(Objects.requireNonNull(word, "word"), 0L);
    }

    public long bigramFrequency(String previous, String current) {
        return bigrams.getOrDefault(new Bigram(previous, current), 0L);
    }

    public double conditionalProbability(String previous, String current) {
        long bigram = bigramFrequency(previous, current);
        long previousFrequency = unigramFrequency(previous);
        double denominator = previousFrequency + (SMOOTHING * Math.max(1L, totalUnigrams));
        if (denominator <= 0.0d) return SMOOTHING;
        return Math.min(1.0d, Math.max(0.0d, (bigram + SMOOTHING) / denominator));
    }

    public double contextScore(String previous, String current, String next) {
        double total = 0.0d;
        int terms = 0;
        if (previous != null && !previous.isBlank()) {
            total += conditionalProbability(previous, current);
            terms++;
        }
        if (next != null && !next.isBlank()) {
            total += conditionalProbability(current, next);
            terms++;
        }
        return terms == 0 ? 0.0d : total / terms;
    }

    public boolean appliesTo(String source, String revision, String language,
                             String fingerprint, String tokenizer) {
        return sourceId.equals(source) && sourceRevision.equals(revision)
                && languageTag.equals(language) && sourceFingerprint.equals(fingerprint)
                && tokenizerVersion.equals(tokenizer);
    }

    /** Immutable bigram identity with the same ordering as Synexia. */
    public record Bigram(String previous, String current) implements Comparable<Bigram> {
        public Bigram {
            if (previous == null || previous.isBlank()
                    || current == null || current.isBlank()) {
                throw new IllegalArgumentException("bigram terms must not be blank");
            }
        }

        @Override
        public int compareTo(Bigram other) {
            int previousOrder = previous.compareTo(other.previous);
            return previousOrder == 0 ? current.compareTo(other.current) : previousOrder;
        }
    }

    private static String text(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is blank");
        return value;
    }

    private static <K> Map<K, Long> immutableCounts(Map<K, Long> input, String name) {
        Objects.requireNonNull(input, name);
        TreeMap<K, Long> copy = new TreeMap<>((left, right) -> {
            if (left instanceof Bigram a && right instanceof Bigram b) return a.compareTo(b);
            if (left instanceof String a && right instanceof String b) return a.compareTo(b);
            throw new IllegalArgumentException(name + " keys have incompatible types");
        });
        input.forEach((key, value) -> {
            if (key == null || value == null || value < 0L) {
                throw new IllegalArgumentException(name + " counts must be non-negative");
            }
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
