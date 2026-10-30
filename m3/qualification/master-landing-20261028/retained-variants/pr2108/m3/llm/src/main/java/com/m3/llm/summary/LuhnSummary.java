// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
package com.m3.llm.summary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mechanical Java counterpart of {@code cpullm_opdag.luhn}. */
public final class LuhnSummary {
    private static final int MIN_SENTENCE_COUNT = 1;
    private static final int LINE_FEED = '\n';
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "have",
            "in", "is", "it", "of", "on", "or", "that", "the", "this", "to", "was", "were",
            "will", "with");

    private LuhnSummary() { }

    /** Returns the source-compatible extractive summary, ending in LF when non-empty. */
    public static String luhnSummary(String text, int maxSentences) {
        Objects.requireNonNull(text, "text");
        if (maxSentences < MIN_SENTENCE_COUNT) {
            throw new IllegalArgumentException("max_sentences must be positive");
        }

        List<String> sentences = splitSentences(text);
        if (sentences.size() <= maxSentences) {
            return joinedLines(sentences);
        }

        Map<String, Long> frequencies = new HashMap<>();
        for (String term : eligibleTerms(text)) {
            frequencies.merge(term, 1L, Long::sum);
        }
        if (frequencies.isEmpty()) {
            return joinedLines(sentences.subList(0, Math.min(maxSentences, sentences.size())));
        }

        List<ScoredSentence> ranked = new ArrayList<>();
        for (int index = 0; index < sentences.size(); index++) {
            List<String> words = eligibleTerms(sentences.get(index));
            long weighted = 0;
            for (String word : words) {
                weighted += frequencies.get(word);
            }
            double score = words.isEmpty() ? 0.0 : weighted / Math.sqrt(words.size());
            ranked.add(new ScoredSentence(index, score));
        }
        ranked.sort(Comparator.comparingDouble(ScoredSentence::score).reversed()
                .thenComparingInt(ScoredSentence::index));

        List<Integer> selected = ranked.stream().limit(maxSentences)
                .map(ScoredSentence::index).sorted().toList();
        List<String> result = new ArrayList<>();
        for (int index : selected) {
            result.add(sentences.get(index));
        }
        return joinedLines(result);
    }

    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        int segmentStart = 0;
        int cursor = 0;
        while (cursor < text.length()) {
            int codePoint = text.codePointAt(cursor);
            int previous = cursor == 0 ? -1 : text.codePointBefore(cursor);
            if (isPythonWhitespace(codePoint) && isSentenceEnd(previous)) {
                int end = cursor;
                while (end < text.length() && isPythonWhitespace(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end));
                }
                addSentence(sentences, text.substring(segmentStart, cursor));
                segmentStart = end;
                cursor = end;
            } else if (codePoint == LINE_FEED) {
                int end = cursor;
                while (end < text.length() && text.codePointAt(end) == LINE_FEED) {
                    end += Character.charCount(LINE_FEED);
                }
                addSentence(sentences, text.substring(segmentStart, cursor));
                segmentStart = end;
                cursor = end;
            } else {
                cursor += Character.charCount(codePoint);
            }
        }
        addSentence(sentences, text.substring(segmentStart));
        return sentences;
    }

    private static void addSentence(List<String> sentences, String candidate) {
        String stripped = pythonStrip(candidate);
        if (!stripped.isEmpty()) {
            sentences.add(stripped);
        }
    }

    private static boolean isSentenceEnd(int codePoint) {
        return codePoint == '.' || codePoint == '!' || codePoint == '?';
    }

    private static List<String> eligibleTerms(String text) {
        List<String> terms = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase(Locale.ROOT);
            if (word.length() > 1 && !STOP_WORDS.contains(word)) {
                terms.add(word);
            }
        }
        return terms;
    }

    private static String joinedLines(List<String> lines) {
        return lines.isEmpty() ? "" : String.join("\n", lines) + "\n";
    }

    private static String pythonStrip(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!isPythonWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (start < end) {
            int codePoint = value.codePointBefore(end);
            if (!isPythonWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    private static boolean isPythonWhitespace(int codePoint) {
        return codePoint >= 0x09 && codePoint <= 0x0d
                || codePoint >= 0x1c && codePoint <= 0x20
                || codePoint == 0x85 || codePoint == 0xa0 || codePoint == 0x1680
                || codePoint >= 0x2000 && codePoint <= 0x200a
                || codePoint == 0x2028 || codePoint == 0x2029 || codePoint == 0x202f
                || codePoint == 0x205f || codePoint == 0x3000;
    }

    private record ScoredSentence(int index, double score) { }
}
