// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, add-alpha statistical scorer over a single exact MIndex n-gram snapshot.
 *
 * <p>All counts remain owned by {@link ScopedNgrams.Snapshot}; the only derived
 * resident state is a sorted primitive unigram vocabulary. Unseen IDs share one
 * out-of-vocabulary category. This scorer never learns or modifies the snapshot.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexNgramPerplexity.java blob a283c73104c3; recipe m3/nlp/recipe/scoped-ngram-perplexity-20261106/relocate.py v2
public final class NgramPerplexity {
    private final ScopedNgrams.Snapshot counts;
    private final long[] vocabulary;
    private final long observations;
    private final double alpha;
    private final double bigramWeight;
    private final double smoothingMass;
    private final double unigramDenominator;

    private NgramPerplexity(
            ScopedNgrams.Snapshot counts,
            long[] vocabulary,
            long observations,
            double alpha,
            double bigramWeight) {
        this.counts = counts;
        this.vocabulary = vocabulary;
        this.observations = observations;
        this.alpha = alpha;
        this.bigramWeight = bigramWeight;
        smoothingMass = alpha * (vocabulary.length + 1.0d);
        unigramDenominator = observations + smoothingMass;
        if (!Double.isFinite(smoothingMass) || !Double.isFinite(unigramDenominator)
                || smoothingMass <= 0.0d || unigramDenominator <= 0.0d) {
            throw new IllegalArgumentException("non-finite smoothing denominator");
        }
    }

    /**
     * Bind exactly one frozen corpus to a normalized unigram/bigram model.
     *
     * @param alpha strictly positive, finite pseudocount per predictive category
     * @param bigramWeight interpolation coefficient in the closed interval [0,1]
     */
    public static NgramPerplexity of(
            ScopedNgrams.Snapshot counts, double alpha, double bigramWeight) {
        Objects.requireNonNull(counts, "counts");
        if (!(alpha > 0.0d) || !Double.isFinite(alpha)) {
            throw new IllegalArgumentException("alpha must be positive and finite");
        }
        if (!(bigramWeight >= 0.0d && bigramWeight <= 1.0d)
                || !Double.isFinite(bigramWeight)) {
            throw new IllegalArgumentException("bigramWeight must be finite and in [0,1]");
        }

        List<Map.Entry<ScopedNgrams.Tuple, Long>> entries = counts.entries();
        int distinctUnigrams = 0;
        long total = 0L;
        for (var entry : entries) {
            long[] gram = entry.getKey().ids();
            long frequency = entry.getValue();
            if (frequency <= 0L) {
                throw new IllegalArgumentException("all observed counts must be positive");
            }
            if (gram.length == 1) {
                distinctUnigrams++;
                total = Math.addExact(total, frequency);
            }
        }
        if (distinctUnigrams == 0 || distinctUnigrams == Integer.MAX_VALUE || total == 0L) {
            throw new IllegalArgumentException("snapshot has no usable unigram observations");
        }
        long[] vocabulary = new long[distinctUnigrams];
        int at = 0;
        for (var entry : entries) {
            long[] gram = entry.getKey().ids();
            if (gram.length == 1) {
                vocabulary[at++] = gram[0];
            }
        }
        Arrays.sort(vocabulary);
        if (total != counts.contextTotal()) {
            throw new IllegalArgumentException("unigram total does not match the corpus scope");
        }

        long[] outgoing = new long[vocabulary.length];
        for (var entry : entries) {
            long[] gram = entry.getKey().ids();
            if (gram.length == 2) {
                int from = Arrays.binarySearch(vocabulary, gram[0]);
                int to = Arrays.binarySearch(vocabulary, gram[1]);
                if (from < 0 || to < 0) {
                    throw new IllegalArgumentException("bigram references a missing unigram");
                }
                outgoing[from] = Math.addExact(outgoing[from], entry.getValue());
            }
        }
        for (int i = 0; i < vocabulary.length; i++) {
            if (outgoing[i] != counts.contextTotal(vocabulary[i])
                    || outgoing[i] > counts.count(vocabulary[i])) {
                throw new IllegalArgumentException("inconsistent outgoing bigram marginal");
            }
        }
        return new NgramPerplexity(counts, vocabulary, total, alpha, bigramWeight);
    }

    /** Immutable source, revision, language, generation and token-kind identity. */
    public ScopedNgrams.Scope scope() {
        return counts.scope();
    }

    /** Observed vocabulary size, excluding the one reserved OOV bucket. */
    public int vocabularySize() {
        return vocabulary.length;
    }

    /** Exact number of observed unigram tokens. */
    public long observationCount() {
        return observations;
    }

    /** Normalized unigram probability, including one shared OOV category. */
    public double unigramProbability(long word) {
        return Math.exp(logUnigram(word));
    }

    /** Normalized bigram/unigram interpolation; unseen contexts have uniform bigram mass. */
    public double conditionalProbability(long previous, long current) {
        return Math.exp(logConditional(previous, current));
    }

    /**
     * Natural-log likelihood of one sentence. The first token uses the unigram
     * prior; adjacent pairs never extend beyond the supplied array.
     */
    public double logLikelihood(long[] sentence) {
        Objects.requireNonNull(sentence, "sentence");
        if (sentence.length == 0) {
            return 0.0d;
        }
        double sum = logUnigram(sentence[0]);
        double compensation = 0.0d;
        for (int i = 1; i < sentence.length; i++) {
            double term = logConditional(sentence[i - 1], sentence[i]);
            double adjusted = term - compensation;
            double next = sum + adjusted;
            compensation = (next - sum) - adjusted;
            sum = next;
        }
        return sum;
    }

    /**
     * Natural-number perplexity across separate sentences. Empty sentences are
     * neutral, but an entirely empty evaluation corpus is invalid.
     */
    public double perplexity(Iterable<long[]> sentences) {
        Objects.requireNonNull(sentences, "sentences");
        long tokens = 0L;
        double sum = 0.0d;
        double compensation = 0.0d;
        for (long[] sentence : sentences) {
            Objects.requireNonNull(sentence, "sentence");
            tokens = Math.addExact(tokens, sentence.length);
            double adjusted = logLikelihood(sentence) - compensation;
            double next = sum + adjusted;
            compensation = (next - sum) - adjusted;
            sum = next;
        }
        if (tokens == 0L) {
            throw new IllegalArgumentException("perplexity requires at least one token");
        }
        double result = Math.exp(-sum / tokens);
        if (!Double.isFinite(result)) {
            throw new ArithmeticException("perplexity overflow");
        }
        return result;
    }

    private double logUnigram(long word) {
        double n = counts.count(word) + alpha;
        return Math.log(n) - Math.log(unigramDenominator);
    }

    private double logBigram(long previous, long current) {
        long observed = counts.count(previous, current);
        double denominator = counts.contextTotal(previous) + smoothingMass;
        return Math.log(observed + alpha) - Math.log(denominator);
    }

    private double logConditional(long previous, long current) {
        if (bigramWeight == 0.0d) {
            return logUnigram(current);
        }
        double bigram = logBigram(previous, current);
        if (bigramWeight == 1.0d) {
            return bigram;
        }
        double first = Math.log(bigramWeight) + bigram;
        double second = Math.log1p(-bigramWeight) + logUnigram(current);
        double larger = Math.max(first, second);
        return larger + Math.log(Math.exp(first - larger) + Math.exp(second - larger));
    }
}
