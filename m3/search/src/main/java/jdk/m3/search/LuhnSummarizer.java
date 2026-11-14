/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/reduce/LuhnSummarizer.java (blob efd2bbf67b2f) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classical Luhn-style significance clustering. Significant words are frequent content words or
 * capitalized lexical anchors; nearby significant words form density clusters scored as s^2/span.
 */
public final class LuhnSummarizer {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}_']+");
    private static final Set<String> STOP = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "been", "by", "for", "from", "has",
            "have", "he", "in", "is", "it", "its", "of", "on", "or", "that", "the", "their",
            "this", "to", "was", "were", "will", "with");
    private final int minimumFrequency;
    private final int maxGap;

    public LuhnSummarizer(int minimumFrequency, int maxGap) {
        if (minimumFrequency < 1 || maxGap < 0) {
            throw new IllegalArgumentException("invalid Luhn configuration");
        }
        this.minimumFrequency = minimumFrequency;
        this.maxGap = maxGap;
    }

    public List<SummarySentence> rank(String text, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        Map<String, Integer> frequencies = frequencies(text);
        List<String> sentences = sentences(text);
        List<SummarySentence> scored = new ArrayList<>(sentences.size());
        for (int i = 0; i < sentences.size(); i++) {
            scored.add(score(i, sentences.get(i), frequencies));
        }
        return scored.stream()
                .filter(row -> row.score() > 0.0)
                .sorted(Comparator.comparingDouble(SummarySentence::score).reversed()
                        .thenComparingInt(SummarySentence::index))
                .limit(limit)
                .toList();
    }

    public String summarize(String text, int limit) {
        List<SummarySentence> top = new ArrayList<>(rank(text, limit));
        top.sort(Comparator.comparingInt(SummarySentence::index));
        StringBuilder out = new StringBuilder();
        for (SummarySentence row : top) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(row.text());
        }
        return out.toString();
    }

    private SummarySentence score(int index, String sentence, Map<String, Integer> frequencies) {
        Matcher matcher = WORD.matcher(sentence);
        IntPositions significant = new IntPositions();
        int wordPosition = 0;
        while (matcher.find()) {
            String original = matcher.group();
            String normalized = original.toLowerCase(Locale.ROOT);
            boolean lexical = !STOP.contains(normalized) && normalized.length() > 1;
            boolean properAnchor = lexical && Character.isUpperCase(original.codePointAt(0));
            if (lexical && (properAnchor
                    || frequencies.getOrDefault(normalized, 0) >= minimumFrequency)) {
                significant.add(wordPosition);
            }
            wordPosition++;
        }
        if (significant.size() == 0) {
            return new SummarySentence(index, sentence, 0.0, 0, 0);
        }

        double best = 0.0;
        int bestCount = 1;
        int bestSpan = 1;
        int start = 0;
        for (int end = 0; end < significant.size(); end++) {
            while (start < end && significant.get(end) - significant.get(start)
                    > (end - start) + maxGap * Math.max(1, end - start)) {
                start++;
            }
            int count = end - start + 1;
            int span = significant.get(end) - significant.get(start) + 1;
            double score = (double) count * count / Math.max(1, span);
            if (score > best) {
                best = score;
                bestCount = count;
                bestSpan = span;
            }
        }
        return new SummarySentence(index, sentence, best, bestCount, bestSpan);
    }

    private static Map<String, Integer> frequencies(String text) {
        String source = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Map<String, Integer> frequencies = new HashMap<>();
        Matcher matcher = WORD.matcher(source);
        while (matcher.find()) {
            String word = matcher.group();
            if (!STOP.contains(word) && word.length() > 1) {
                frequencies.merge(word, 1, Integer::sum);
            }
        }
        return frequencies;
    }

    private static List<String> sentences(String text) {
        String source = text == null ? "" : text;
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(source);
        List<String> result = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE;
                start = end, end = iterator.next()) {
            String sentence = source.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                result.add(sentence);
            }
        }
        return result;
    }

    public record SummarySentence(
            int index, String text, double score, int significantWords, int spanWords) {
    }

    private static final class IntPositions {
        private int[] values = new int[8];
        private int size;

        private void add(int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length << 1);
            }
            values[size++] = value;
        }

        private int get(int index) {
            return values[index];
        }

        private int size() {
            return size;
        }
    }
}
