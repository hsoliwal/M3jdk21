// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.llm.extract;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Frequency-weighted extractive summarizer: no generative model and no network access. */
public final class ExtractiveSummarizer {
    /** Preserves the original public no-argument constructor in the exported module. */
    public ExtractiveSummarizer() { }

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}']+");
    private static final Set<String> STOP = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "is",
            "it", "of", "on", "or", "that", "the", "this", "to", "was", "were", "with");

    public String summarize(String text, int sentenceLimit) {
        if (sentenceLimit < 1) {
            throw new IllegalArgumentException("sentenceLimit must be >= 1");
        }
        List<String> sentences = sentences(text);
        if (sentences.size() <= sentenceLimit) {
            return String.join(" ", sentences);
        }

        Map<String, Integer> frequencies = frequencies(text);
        List<ScoredSentence> scored = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = score(sentence, frequencies);
            scored.add(new ScoredSentence(i, sentence, score));
        }
        Set<Integer> selected = scored.stream()
                .sorted((a, b) -> {
                    int byScore = Double.compare(b.score(), a.score());
                    return byScore != 0 ? byScore : Integer.compare(a.index(), b.index());
                })
                .limit(sentenceLimit)
                .map(ScoredSentence::index)
                .collect(java.util.stream.Collectors.toSet());

        return scored.stream()
                .filter(sentence -> selected.contains(sentence.index()))
                .map(ScoredSentence::sentence)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static List<String> sentences(String text) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(text);
        List<String> result = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                result.add(sentence);
            }
        }
        return result;
    }

    private static Map<String, Integer> frequencies(String text) {
        Map<String, Integer> result = new HashMap<>();
        Matcher matcher = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (!STOP.contains(token)) {
                result.merge(token, 1, Integer::sum);
            }
        }
        return result;
    }

    private static double score(String sentence, Map<String, Integer> frequencies) {
        Matcher matcher = WORD.matcher(sentence.toLowerCase(Locale.ROOT));
        int words = 0;
        int total = 0;
        while (matcher.find()) {
            words++;
            total += frequencies.getOrDefault(matcher.group(), 0);
        }
        return words == 0 ? 0.0 : (double) total / Math.sqrt(words);
    }

    private record ScoredSentence(int index, String sentence, double score) {
    }
}
