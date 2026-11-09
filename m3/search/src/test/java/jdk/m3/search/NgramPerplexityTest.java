// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Independent normalization, sentence-boundary and failure-oracle fixtures. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/ca1aafc4d94f9dd1ee288226dcca070cc60991c9/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ngram-perplexity-20261031/01-MIndexNgramPerplexityTest.java.txt blob 9526d238f3e7; recipe m3/nlp/recipe/scoped-ngram-perplexity-20261106/relocate.py v2
class NgramPerplexityTest {
    private static final ScopedNgrams.Scope SCOPE =
            new ScopedNgrams.Scope("fixture", "v1", "en", 1, "word");

    private static ScopedNgrams.Snapshot corpus() {
        var model = new ScopedNgrams(SCOPE, 3, 100, 20);
        model.observe(SCOPE, new long[] {1, 2, 1}, 1);
        model.observe(SCOPE, new long[] {1, 3}, 1);
        return model.freeze();
    }

    @Test
    void smoothingNormalizesObservedAndUnknownUnigrams() {
        var scorer = NgramPerplexity.of(corpus(), 1, 0.75);
        assertEquals(SCOPE, scorer.scope());
        assertEquals(3, scorer.vocabularySize());
        assertEquals(5, scorer.observationCount());
        assertEquals(4.0 / 9.0, scorer.unigramProbability(1), 1e-14);
        assertEquals(2.0 / 9.0, scorer.unigramProbability(2), 1e-14);
        assertEquals(1.0 / 9.0, scorer.unigramProbability(999), 1e-14);
        assertEquals(scorer.unigramProbability(999), scorer.unigramProbability(-9), 0);
        double total = scorer.unigramProbability(1) + scorer.unigramProbability(2)
                + scorer.unigramProbability(3) + scorer.unigramProbability(999);
        assertEquals(1, total, 1e-14);
    }

    @Test
    void conditionalInterpolationUsesOutgoingMarginalAndReservedOov() {
        var scorer = NgramPerplexity.of(corpus(), 1, 0.75);
        // C(1,2) = 1, C(1,*) = 2, K = 4; P2(2|1) = 2/6.
        assertEquals(0.75 * (2.0 / 6.0) + 0.25 * (2.0 / 9.0),
                scorer.conditionalProbability(1, 2), 1e-14);
        for (long prior : new long[] {1, 2, 999}) {
            double mass = 0;
            for (long next : new long[] {1, 2, 3, 999}) {
                mass += scorer.conditionalProbability(prior, next);
            }
            assertEquals(1, mass, 1e-14, "context " + prior);
        }
        assertEquals(scorer.conditionalProbability(1, 999),
                scorer.conditionalProbability(1, -9), 0);
    }

    @Test
    void wholeSentenceScoresOnlyLocalAdjacentPairs() {
        var scorer = NgramPerplexity.of(corpus(), 1, 0.75);
        double first = Math.log(scorer.unigramProbability(1));
        double pair = Math.log(scorer.conditionalProbability(1, 2));
        assertEquals(first + pair, scorer.logLikelihood(new long[] {1, 2}), 1e-14);
        assertEquals(0, scorer.logLikelihood(new long[0]));
        double expected = Math.exp(-(first + pair + first) / 3);
        assertEquals(expected, scorer.perplexity(List.of(new long[] {1, 2}, new long[] {1})), 1e-14);
        assertTrue(scorer.perplexity(List.of(new long[] {1, 2})) > 1.0);
    }

    @Test
    void isolatedSentencesDoNotCreateCrossBoundaryBigrams() {
        var scorer = NgramPerplexity.of(corpus(), 1, 1.0);
        double independent = scorer.perplexity(List.of(new long[] {1}, new long[] {2}));
        double chained = scorer.perplexity(List.of(new long[] {1, 2}));
        assertTrue(Math.abs(independent - chained) > 1e-6);
    }

    @Test
    void invalidCandidatesRefuseBeforeAnyEvaluation() {
        assertThrows(NullPointerException.class, () -> NgramPerplexity.of(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> NgramPerplexity.of(corpus(), 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> NgramPerplexity.of(corpus(), Double.NaN, 1));
        assertThrows(IllegalArgumentException.class,
                () -> NgramPerplexity.of(corpus(), 1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> NgramPerplexity.of(corpus(), 1, -0.01));
        assertThrows(IllegalArgumentException.class, () -> NgramPerplexity.of(corpus(), 1, 1.01));
        assertThrows(IllegalArgumentException.class,
                () -> NgramPerplexity.of(corpus(), Double.MAX_VALUE, 1));
        var scorer = NgramPerplexity.of(corpus(), 1, 0.5);
        assertThrows(NullPointerException.class, () -> scorer.logLikelihood(null));
        assertThrows(NullPointerException.class, () -> scorer.perplexity(null));
        assertThrows(NullPointerException.class, () -> scorer.perplexity(Arrays.asList(new long[] {1}, null)));
        assertThrows(IllegalArgumentException.class, () -> scorer.perplexity(List.of(new long[0])));
    }

    @Test
    void refusesInconsistentOrMissingUnigramCounts() {
        var empty = new ScopedNgrams(SCOPE, 2, 100, 10).freeze();
        assertThrows(IllegalArgumentException.class, () -> NgramPerplexity.of(empty, 1, 1));
        var malformed = new ScopedNgrams(SCOPE, 2, 100, 10);
        malformed.add(SCOPE, new long[] {1}, 1);
        malformed.add(SCOPE, new long[] {1, 2}, 3);
        assertThrows(IllegalArgumentException.class,
                () -> NgramPerplexity.of(malformed.freeze(), 1, 1));
    }

    @Test
    void emptyAndExtremeIdsRetainDefinedOovMass() {
        var scorer = NgramPerplexity.of(corpus(), 0.25, 0.0);
        assertEquals(scorer.unigramProbability(Long.MIN_VALUE),
                scorer.unigramProbability(Long.MAX_VALUE), 0);
        assertEquals(scorer.unigramProbability(1), scorer.conditionalProbability(999, 1), 1e-14);
        assertTrue(Double.isFinite(scorer.logLikelihood(new long[] {Long.MIN_VALUE, 1, Long.MAX_VALUE})));
    }
}
