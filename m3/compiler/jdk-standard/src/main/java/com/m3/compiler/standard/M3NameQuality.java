// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.standard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic anti-pattern checks for new M3-owned type and method names. */
final class M3NameQuality {
    private static final int MAX_TYPE_LENGTH = 48;
    private static final int MAX_METHOD_LENGTH = 40;
    private static final int MAX_TYPE_WORDS = 7;
    private static final int MAX_METHOD_WORDS = 6;

    private static final Set<String> TYPE_PLACEHOLDERS = Set.of(
            "foo", "bar", "baz", "qux", "thing", "stuff", "helper", "manager",
            "util", "utils", "impl", "implementation", "placeholder", "dummy", "temporary");
    private static final Set<String> METHOD_PLACEHOLDERS = Set.of(
            "foo", "bar", "baz", "doit", "dostuff", "processdata", "handlething",
            "performaction", "executeoperation", "runtask", "helper", "util", "work");
    private static final List<List<String>> PROMOTIONAL_PHRASES = List.of(
            List.of("ultra"), List.of("hyper"), List.of("turbo"), List.of("fastest"),
            List.of("ultimate"), List.of("next", "gen"), List.of("super", "fast"),
            List.of("high", "performance"), List.of("low", "latency"),
            List.of("optimized"));
    private static final Pattern CAMEL_TOKEN = Pattern.compile(
            "M3|[A-Z]+(?=[A-Z][a-z]|[0-9]|$)|[A-Z]?[a-z]+|[0-9]+");

    private M3NameQuality() {
    }

    static Optional<String> typeIssue(String name) {
        return findIssue(name, MAX_TYPE_LENGTH, MAX_TYPE_WORDS, TYPE_PLACEHOLDERS);
    }

    static Optional<String> methodIssue(String name) {
        return findIssue(name, MAX_METHOD_LENGTH, MAX_METHOD_WORDS, METHOD_PLACEHOLDERS);
    }

    private static Optional<String> findIssue(String name, int maximumLength,
            int maximumWords, Set<String> placeholders) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (placeholders.contains(normalized)) {
            return Optional.of("is a placeholder; name the domain concept or operation");
        }
        List<String> tokens = camelTokens(name);
        if (containsPromotionalPhrase(tokens)) {
            return Optional.of(
                    "contains promotional wording; state the domain concept or operation");
        }
        if (hasVersionOrDateSuffix(tokens)) {
            return Optional.of(
                    "carries a version/date suffix; history does not belong in an API name");
        }

        for (int index = 1; index < tokens.size(); index++) {
            String previous = tokens.get(index - 1);
            String current = tokens.get(index);
            if (!isNumericToken(previous) && previous.equalsIgnoreCase(current)) {
                return Optional.of("repeats adjacent word " + current);
            }
        }
        if (name.length() > maximumLength) {
            return Optional.of("has " + name.length() + " characters; limit " + maximumLength);
        }

        long words = tokens.stream().filter(M3NameQuality::isSemanticWord).count();
        if (words > maximumWords) {
            return Optional.of("has " + words + " semantic words; limit " + maximumWords);
        }
        int segmentedLength = tokens.stream().map(String::length).mapToInt(Integer::intValue).sum();
        if (segmentedLength != name.length()) {
            return Optional.of("cannot be segmented into conventional camel-case words");
        }
        return Optional.empty();
    }

    private static List<String> camelTokens(String name) {
        List<String> result = new ArrayList<>();
        Matcher matcher = CAMEL_TOKEN.matcher(name);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return List.copyOf(result);
    }

    private static boolean containsPromotionalPhrase(List<String> tokens) {
        List<String> normalized = tokens.stream()
                .map(token -> token.toLowerCase(Locale.ROOT)).toList();
        return PROMOTIONAL_PHRASES.stream()
                .anyMatch(phrase -> containsSequence(normalized, phrase));
    }

    private static boolean containsSequence(List<String> words, List<String> phrase) {
        for (int start = 0; start + phrase.size() <= words.size(); start++) {
            if (words.subList(start, start + phrase.size()).equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVersionOrDateSuffix(List<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        String last = tokens.get(tokens.size() - 1);
        if (last.length() == 8 && last.startsWith("20") && isNumericToken(last)) {
            return true;
        }
        if (tokens.size() < 2 || !isNumericToken(last)) {
            return false;
        }
        String previous = tokens.get(tokens.size() - 2);
        return previous.equals("V") || previous.equalsIgnoreCase("Version");
    }

    private static boolean isSemanticWord(String token) {
        return !token.equals("M3") && !isNumericToken(token);
    }

    private static boolean isNumericToken(String token) {
        return token.chars().allMatch(Character::isDigit);
    }

}
