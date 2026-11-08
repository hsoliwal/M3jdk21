// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob a947a2c59855 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class StringAlgorithms {
    private StringAlgorithms() {
    }

    public static int[] prefixFunction(CharSequence pattern, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "kmp-prefix-function", pattern.length());
        try {
            int[] prefix = new int[pattern.length()];
            for (int index = 1; index < pattern.length(); index++) {
                AlgorithmSupport.tick(monitor);
                int candidate = prefix[index - 1];
                while (candidate > 0 && pattern.charAt(index) != pattern.charAt(candidate)) {
                    monitor.checkCanceled();
                    candidate = prefix[candidate - 1];
                }
                if (pattern.charAt(index) == pattern.charAt(candidate)) {
                    candidate++;
                }
                prefix[index] = candidate;
            }
            return prefix;
        } finally {
            monitor.done();
        }
    }

    public static List<Integer> kmpSearch(
            CharSequence text,
            CharSequence pattern,
            ProgressMonitor monitor) {
        if (pattern.length() == 0) {
            return java.util.stream.IntStream.rangeClosed(0, text.length()).boxed().toList();
        }
        int[] prefix = prefixFunction(pattern, monitor);
        AlgorithmSupport.begin(monitor, "kmp-search", text.length());
        try {
            List<Integer> matches = new ArrayList<>();
            int matched = 0;
            for (int index = 0; index < text.length(); index++) {
                AlgorithmSupport.tick(monitor);
                while (matched > 0 && text.charAt(index) != pattern.charAt(matched)) {
                    matched = prefix[matched - 1];
                }
                if (text.charAt(index) == pattern.charAt(matched)) {
                    matched++;
                }
                if (matched == pattern.length()) {
                    matches.add(index - pattern.length() + 1);
                    matched = prefix[matched - 1];
                }
            }
            return List.copyOf(matches);
        } finally {
            monitor.done();
        }
    }

    public static int[] zFunction(CharSequence value, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "z-function", value.length());
        try {
            int[] z = new int[value.length()];
            int left = 0;
            int right = 0;
            for (int index = 1; index < value.length(); index++) {
                AlgorithmSupport.tick(monitor);
                if (index <= right) {
                    z[index] = Math.min(right - index + 1, z[index - left]);
                }
                while (index + z[index] < value.length()
                        && value.charAt(z[index]) == value.charAt(index + z[index])) {
                    monitor.checkCanceled();
                    z[index]++;
                }
                if (index + z[index] - 1 > right) {
                    left = index;
                    right = index + z[index] - 1;
                }
            }
            return z;
        } finally {
            monitor.done();
        }
    }

    public static String longestPalindrome(String value, ProgressMonitor monitor) {
        if (value.isEmpty()) {
            return "";
        }
        AlgorithmSupport.begin(monitor, "manacher-longest-palindrome", value.length());
        try {
            int length = value.length();
            int[] odd = new int[length];
            int left = 0;
            int right = -1;
            for (int index = 0; index < length; index++) {
                AlgorithmSupport.tick(monitor);
                int radius = index > right
                        ? 1
                        : Math.min(odd[left + right - index], right - index + 1);
                while (index - radius >= 0
                        && index + radius < length
                        && value.charAt(index - radius) == value.charAt(index + radius)) {
                    monitor.checkCanceled();
                    radius++;
                }
                odd[index] = radius;
                if (index + radius - 1 > right) {
                    left = index - radius + 1;
                    right = index + radius - 1;
                }
            }

            int[] even = new int[length];
            left = 0;
            right = -1;
            for (int index = 0; index < length; index++) {
                monitor.checkCanceled();
                int radius = index > right
                        ? 0
                        : Math.min(even[left + right - index + 1], right - index + 1);
                while (index - radius - 1 >= 0
                        && index + radius < length
                        && value.charAt(index - radius - 1) == value.charAt(index + radius)) {
                    radius++;
                }
                even[index] = radius;
                if (index + radius - 1 > right) {
                    left = index - radius;
                    right = index + radius - 1;
                }
            }

            int bestStart = 0;
            int bestLength = 1;
            for (int index = 0; index < length; index++) {
                int oddLength = odd[index] * 2 - 1;
                if (oddLength > bestLength) {
                    bestLength = oddLength;
                    bestStart = index - odd[index] + 1;
                }
                int evenLength = even[index] * 2;
                if (evenLength > bestLength) {
                    bestLength = evenLength;
                    bestStart = index - even[index];
                }
            }
            return value.substring(bestStart, bestStart + bestLength);
        } finally {
            monitor.done();
        }
    }

    public static boolean wildcardMatch(
            CharSequence text,
            CharSequence pattern,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "wildcard-greedy-match", text.length() + pattern.length());
        try {
            int textIndex = 0;
            int patternIndex = 0;
            int star = -1;
            int match = 0;
            while (textIndex < text.length()) {
                AlgorithmSupport.tick(monitor);
                if (patternIndex < pattern.length()
                        && (pattern.charAt(patternIndex) == '?'
                                || pattern.charAt(patternIndex) == text.charAt(textIndex))) {
                    textIndex++;
                    patternIndex++;
                } else if (patternIndex < pattern.length()
                        && pattern.charAt(patternIndex) == '*') {
                    star = patternIndex++;
                    match = textIndex;
                } else if (star >= 0) {
                    patternIndex = star + 1;
                    textIndex = ++match;
                } else {
                    return false;
                }
            }
            while (patternIndex < pattern.length() && pattern.charAt(patternIndex) == '*') {
                monitor.checkCanceled();
                patternIndex++;
            }
            return patternIndex == pattern.length();
        } finally {
            monitor.done();
        }
    }

    //FLLDCIM: FOLD - member ZAlgorithm.zSearch from donor blob 5834d9dfa933 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZAlgorithm.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    /**
     * First occurrence using extended Z matching over the pattern and text.
     * Keeps only the pattern's Z table, never concatenates the input strings,
     * and accepts every UTF-16 code unit (including '$' and unpaired surrogates).
     * Time O(n + m), auxiliary memory O(m).
     */
    public static int zSearch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        // Historical String concatenation converts a null text to the four
        // UTF-16 units "null". Keep that existing observable contract.
        text = String.valueOf(text);
        int n = text.length(), m = pattern.length();
        if (m > n) return -1;
        int[] patternZ = zFunction(pattern, ProgressMonitor.none());
        int left = 0, right = 0;
        for (int i = 0; i <= n - m; i++) {
            int matched = 0;
            if (i < right) {
                matched = Math.min(right - i, patternZ[i - left]);
            }
            while (matched < m && text.charAt(i + matched) == pattern.charAt(matched)) {
                matched++;
            }
            if (i + matched > right) {
                left = i;
                right = i + matched;
            }
            if (matched == m) return i;
        }
        return -1;
    }

    //FLLDCIM: FOLD - member ZAlgorithm.zSearch from donor blob 5834d9dfa933 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZAlgorithm.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    /** Progress-aware compatibility entrypoint. */
    public static int zSearch(String text, String pattern, ProgressMonitor monitor) {
        ProgressMonitor checked = ProgressChecks.nonNull(monitor);
        ProgressChecks.checkCanceled(checked);
        checked.beginTask("zSearch", 1L);
        try {
            var result = zSearch(text, pattern);
            ProgressChecks.checkCanceled(checked);
            checked.worked(1L);
            return result;
        } finally {
            checked.done();
        }
    }

    //FLLDCIM: FOLD - member ZPrefixMatch.SENTINEL from donor blob 41f8af0ca760 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZPrefixMatch.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    private static final int SENTINEL = Character.MAX_VALUE + 1;

    //FLLDCIM: FOLD - member ZPrefixMatch.prefixMatchLengths from donor blob 41f8af0ca760 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZPrefixMatch.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    /**
     * Longest pattern-prefix match at every text position, in Java UTF-16 code units.
     *
     * <p>This is reusable Z geometry, not another search engine. Entry {@code i} is the length of
     * the longest prefix of {@code pattern} equal to {@code text.substring(i)}. The virtual
     * separator remains outside the complete char domain, so isolated surrogates and NUL are exact.
     */
    public static int[] prefixMatchLengths(String text, String pattern) {
        return prefixMatchLengths(text, pattern, null);
    }

    //FLLDCIM: FOLD - member ZPrefixMatch.prefixMatchLengths from donor blob 41f8af0ca760 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZPrefixMatch.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    public static int[] prefixMatchLengths(
            String text, String pattern, ProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = Objects.requireNonNull(pattern, "pattern");
        ProgressMonitor checked = ProgressChecks.nonNull(monitor);
        ProgressChecks.checkCanceled(checked);
        int[] matches = new int[haystack.length()];
        if (haystack.isEmpty() || needle.isEmpty()) {
            return matches;
        }
        long totalLong = (long) needle.length() + 1L + haystack.length();
        if (totalLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("virtual Z geometry exceeds int range");
        }
        scanVirtualZ(haystack, needle, checked, matches, false);
        ProgressChecks.checkCanceled(checked);
        return matches;
    }

    //FLLDCIM: FOLD - member ZPrefixMatch.scanVirtualZ from donor blob 41f8af0ca760 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZPrefixMatch.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    /**
     * Shared virtual Z scan. When prefixMatches is non-null it receives one LCP length per text
     * position. Exact search passes null and may return immediately at the first full match.
     */
    private static int scanVirtualZ(
            String haystack,
            String needle,
            ProgressMonitor monitor,
            int[] prefixMatches,
            boolean stopAtFirstExact) {
        int patternLength = needle.length();
        int textLength = haystack.length();
        int total = Math.toIntExact((long) patternLength + 1L + textLength);
        if (prefixMatches != null && prefixMatches.length != textLength) {
            throw new IllegalArgumentException("prefixMatches length");
        }

        int[] z = new int[total];
        int left = 0;
        int right = 0;
        long comparisons = 0L;
        for (int index = 1; index < total; index++) {
            if ((index & 1023) == 0) {
                ProgressChecks.checkCanceled(monitor);
            }
            if (index <= right) {
                z[index] = Math.min(right - index + 1, z[index - left]);
            }

            while (z[index] < total - index
                    && virtualUnit(needle, haystack, z[index])
                            == virtualUnit(needle, haystack, index + z[index])) {
                z[index]++;
                if ((++comparisons & 1023L) == 0L) {
                    ProgressChecks.checkCanceled(monitor);
                }
            }

            int candidateRight = index + z[index] - 1;
            if (candidateRight > right) {
                left = index;
                right = candidateRight;
            }
            if (index > patternLength) {
                int textIndex = index - patternLength - 1;
                int matched = Math.min(patternLength, z[index]);
                if (prefixMatches != null) {
                    prefixMatches[textIndex] = matched;
                }
                if (stopAtFirstExact && matched >= patternLength) {
                    ProgressChecks.checkCanceled(monitor);
                    return textIndex;
                }
            }
        }
        ProgressChecks.checkCanceled(monitor);
        return -1;
    }

    //FLLDCIM: FOLD - member ZPrefixMatch.virtualUnit from donor blob 41f8af0ca760 (m3/algorithms/src/main/java/jdk/m3/algorithm/ZPrefixMatch.java); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/z-fold/FOLD_MAP.tsv
    private static int virtualUnit(
            String pattern, String text, int virtualIndex) {
        int patternLength = pattern.length();
        if (virtualIndex < patternLength) {
            return pattern.charAt(virtualIndex);
        }
        if (virtualIndex == patternLength) {
            return SENTINEL;
        }
        return text.charAt(virtualIndex - patternLength - 1);
    }

}
