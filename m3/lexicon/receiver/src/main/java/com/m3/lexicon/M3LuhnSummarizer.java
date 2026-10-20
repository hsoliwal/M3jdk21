// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Extractive Luhn summarization over immutable CWID language geometry.
 *
 * <p>Input is already tokenized into M3LEX001 word IDs and sentence-end coordinates. No String,
 * tokenizer, POS object, model object or runtime training is used in the scoring loop. Novelty is
 * only the temporary composition of frozen word coordinates into sentence/context scores.</p>
 */
public final class M3LuhnSummarizer {
    public static final int WEIGHT_ONE = 1 << 10;

    public record Config(
            M3StaticLanguageGeometry.FrequencyBand significantFrequencyBand,
            int maximumGap,
            int luhnWeightQ10,
            int pairWeightQ10,
            int semanticWeightQ10,
            long requiredClassMask,
            long excludedClassMask) {
        public Config {
            Objects.requireNonNull(significantFrequencyBand, "significantFrequencyBand");
            if (maximumGap < 0 || luhnWeightQ10 < 0 || pairWeightQ10 < 0
                    || semanticWeightQ10 < 0) {
                throw new IllegalArgumentException("negative summarizer parameter");
            }
        }

        public static Config defaults(M3StaticLanguageGeometry geometry) {
            return new Config(
                    Objects.requireNonNull(geometry, "geometry").defaultLuhnFrequencyBand(),
                    4,
                    WEIGHT_ONE,
                    WEIGHT_ONE / 4,
                    WEIGHT_ONE / 8,
                    0L,
                    0L);
        }
    }

    public record SentenceScore(
            int sentenceIndex,
            long totalQ20,
            long luhnQ20,
            long pairQ20,
            long semanticQ20,
            int significantWords,
            int strongestClusterSpan) { }

    public record Summary(int[] sentenceIndexes, List<SentenceScore> rankedScores) {
        public Summary {
            sentenceIndexes = Objects.requireNonNull(sentenceIndexes, "sentenceIndexes").clone();
            rankedScores = List.copyOf(Objects.requireNonNull(rankedScores, "rankedScores"));
        }

        @Override
        public int[] sentenceIndexes() {
            return sentenceIndexes.clone();
        }
    }

    private final M3StaticLanguageGeometry geometry;
    private final Config config;

    public M3LuhnSummarizer(M3StaticLanguageGeometry geometry) {
        this(geometry, Config.defaults(geometry));
    }

    public M3LuhnSummarizer(M3StaticLanguageGeometry geometry, Config config) {
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        this.config = Objects.requireNonNull(config, "config");
    }

    public Summary summarize(int[] wordIds, int[] sentenceEnds, int maximumSentences) {
        Objects.requireNonNull(wordIds, "wordIds");
        Objects.requireNonNull(sentenceEnds, "sentenceEnds");
        if (maximumSentences < 0) throw new IllegalArgumentException("maximumSentences < 0");
        validateDocument(wordIds, sentenceEnds);

        ArrayList<SentenceScore> scores = new ArrayList<>(sentenceEnds.length);
        int start = 0;
        for (int sentence = 0; sentence < sentenceEnds.length; sentence++) {
            int end = sentenceEnds[sentence];
            scores.add(score(sentence, wordIds, start, end));
            start = end;
        }

        scores.sort(Comparator.comparingLong(SentenceScore::totalQ20).reversed()
                .thenComparingInt(SentenceScore::sentenceIndex));
        int selectedCount = Math.min(maximumSentences, scores.size());
        int[] selected = new int[selectedCount];
        for (int index = 0; index < selectedCount; index++) {
            selected[index] = scores.get(index).sentenceIndex();
        }
        Arrays.sort(selected);
        return new Summary(selected, scores);
    }

    public SentenceScore score(int sentenceIndex, int[] words, int start, int end) {
        Objects.requireNonNull(words, "words");
        if (sentenceIndex < 0) throw new IllegalArgumentException("sentenceIndex < 0");
        Objects.checkFromToIndex(start, end, words.length);
        for (int index = start; index < end; index++) checkWord(words[index]);
        if (start == end) {
            return new SentenceScore(sentenceIndex, 0L, 0L, 0L, 0L, 0, 0);
        }

        ClusterScore cluster = luhn(words, start, end);
        long pair = pairScore(words, start, end);
        long semantic = semanticScore(words, start, end);
        long total = saturatedAdd(
                saturatedAdd(weight(cluster.scoreQ20(), config.luhnWeightQ10()),
                        weight(pair, config.pairWeightQ10())),
                weight(semantic, config.semanticWeightQ10()));
        return new SentenceScore(
                sentenceIndex, total, cluster.scoreQ20(), pair, semantic,
                cluster.significantWords(), cluster.strongestSpan());
    }

    private ClusterScore luhn(int[] words, int start, int end) {
        long best = 0L;
        int strongestSpan = 0;
        int significant = 0;
        int first = -1;
        int last = -1;
        int count = 0;

        for (int position = start; position < end; position++) {
            if (!significant(words[position])) continue;
            significant++;
            if (first < 0) {
                first = last = position;
                count = 1;
                continue;
            }
            if (position - last - 1 > config.maximumGap()) {
                ClusterValue value = cluster(first, last, count);
                if (value.scoreQ20() > best
                        || (value.scoreQ20() == best
                                && (strongestSpan == 0 || value.span() < strongestSpan))) {
                    best = value.scoreQ20();
                    strongestSpan = value.span();
                }
                first = position;
                count = 1;
            } else {
                count++;
            }
            last = position;
        }
        if (first >= 0) {
            ClusterValue value = cluster(first, last, count);
            if (value.scoreQ20() > best
                    || (value.scoreQ20() == best
                            && (strongestSpan == 0 || value.span() < strongestSpan))) {
                best = value.scoreQ20();
                strongestSpan = value.span();
            }
        }
        return new ClusterScore(best, significant, strongestSpan);
    }

    private boolean significant(int cwid) {
        long frequency = geometry.frequency(cwid);
        if (!config.significantFrequencyBand().contains(frequency)) return false;
        long classes = geometry.classMask(cwid);
        if (config.requiredClassMask() != 0L
                && (classes & config.requiredClassMask()) != config.requiredClassMask()) {
            return false;
        }
        return config.excludedClassMask() == 0L
                || (classes & config.excludedClassMask()) == 0L;
    }

    private long pairScore(int[] words, int start, int end) {
        if (end - start < 2) return 0L;
        long total = 0L;
        int count = 0;
        for (int index = start; index + 1 < end; index++) {
            total = saturatedAdd(total,
                    geometry.pairAssociationQ20(words[index], words[index + 1]));
            count++;
        }
        return count == 0 ? 0L : total / count;
    }

    private long semanticScore(int[] words, int start, int end) {
        if (end - start < 2) return 0L;
        long total = 0L;
        int count = 0;
        for (int index = start; index + 1 < end; index++) {
            int left = words[index];
            int right = words[index + 1];
            int classOverlap = geometry.classOverlap(left, right);
            int conceptOverlap = geometry.conceptOverlap(left, right);
            int subjectOverlap = geometry.subjectOverlap(left, right);
            int units = Math.min(64,
                    classOverlap + 4 * conceptOverlap + 2 * subjectOverlap);
            total = saturatedAdd(total,
                    ((long) units * M3StaticLanguageGeometry.Q20_ONE) / 64L);
            count++;
        }
        return count == 0 ? 0L : total / count;
    }

    private void validateDocument(int[] words, int[] sentenceEnds) {
        int previous = 0;
        for (int end : sentenceEnds) {
            if (end <= previous || end > words.length) {
                throw new IllegalArgumentException("sentence ends must be ascending and nonempty");
            }
            previous = end;
        }
        if ((words.length == 0 && sentenceEnds.length != 0)
                || (words.length != 0 && (sentenceEnds.length == 0
                        || sentenceEnds[sentenceEnds.length - 1] != words.length))) {
            throw new IllegalArgumentException("sentence ends must cover all words");
        }
        for (int word : words) checkWord(word);
    }

    private void checkWord(int cwid) {
        if (cwid <= 0 || cwid > geometry.wordCount()) {
            throw new IndexOutOfBoundsException("CWID " + cwid);
        }
    }

    private static ClusterValue cluster(int first, int last, int significant) {
        int span = last - first + 1;
        long numerator = (long) significant * significant;
        return new ClusterValue((numerator << 20) / span, span);
    }

    private static long weight(long scoreQ20, int weightQ10) {
        if (scoreQ20 == 0L || weightQ10 == 0) return 0L;
        return scoreQ20 > Long.MAX_VALUE / weightQ10
                ? Long.MAX_VALUE
                : (scoreQ20 * weightQ10) >>> 10;
    }

    private static long saturatedAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record ClusterScore(long scoreQ20, int significantWords, int strongestSpan) { }
    private record ClusterValue(long scoreQ20, int span) { }
}
