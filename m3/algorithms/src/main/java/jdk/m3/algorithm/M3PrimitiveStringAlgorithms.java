// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;

import java.util.Objects;

/**
 * Deterministic string algorithms.
 *
 * <p>Index-returning methods intentionally use Java UTF-16 code-unit indices, matching
 * {@link String} indexing. They do not claim grapheme-cluster semantics.</p>
 */
//FLLDCIM: RELOCATE - donor blob d0b951405040 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3PrimitiveStringAlgorithms {
    private static final int CHAR_SPACE = Character.MAX_VALUE + 1;

    private M3PrimitiveStringAlgorithms() {}

    public static String minimumWindow(String source, String required) {
        return minimumWindow(source, required, M3ProgressMonitor.none());
    }

    public static String minimumWindow(
            String source,
            String required,
            M3ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(required, "required");
        if (required.isEmpty()) return "";
        M3ProgressMonitor progress = monitor == null ? M3ProgressMonitor.none() : monitor;
        progress.beginTask("minimum-window", source.length());
        try {
            int[] need = new int[CHAR_SPACE];
            for (int i = 0; i < required.length(); i++) need[required.charAt(i)]++;
            int remaining = required.length();
            int left = 0;
            int bestStart = 0;
            int bestLength = Integer.MAX_VALUE;
            for (int right = 0; right < source.length(); right++) {
                progress.checkCanceled();
                char added = source.charAt(right);
                if (need[added] > 0) remaining--;
                need[added]--;
                while (remaining == 0) {
                    int length = right - left + 1;
                    if (length < bestLength) {
                        bestLength = length;
                        bestStart = left;
                    }
                    char removed = source.charAt(left++);
                    need[removed]++;
                    if (need[removed] > 0) remaining++;
                }
                progress.worked(1);
            }
            return bestLength == Integer.MAX_VALUE
                    ? ""
                    : source.substring(bestStart, bestStart + bestLength);
        } finally {
            progress.done();
        }
    }

    public static int kmpIndexOf(String text, String pattern) {
        return kmpIndexOf(text, pattern, M3ProgressMonitor.none());
    }

    public static int kmpIndexOf(
            String text,
            String pattern,
            M3ProgressMonitor monitor) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) return 0;
        M3ProgressMonitor progress = monitor == null ? M3ProgressMonitor.none() : monitor;
        progress.beginTask("kmp-search", text.length() + pattern.length());
        try {
            int[] prefix = prefixFunction(pattern);
            int matched = 0;
            for (int i = 0; i < text.length(); i++) {
                progress.checkCanceled();
                while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                    matched = prefix[matched - 1];
                }
                if (text.charAt(i) == pattern.charAt(matched)) matched++;
                if (matched == pattern.length()) return i - pattern.length() + 1;
                progress.worked(1);
            }
            return -1;
        } finally {
            progress.done();
        }
    }

    public static int[] prefixFunction(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        int[] prefix = new int[pattern.length()];
        for (int i = 1; i < pattern.length(); i++) {
            int matched = prefix[i - 1];
            while (matched > 0 && pattern.charAt(i) != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(matched)) matched++;
            prefix[i] = matched;
        }
        return prefix;
    }

    public static int rabinKarpIndexOf(String text, String pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        int m = pattern.length();
        if (m == 0) return 0;
        if (m > text.length()) return -1;

        long base = 1_000_003L;
        long power = 1L;
        long patternHash = 0L;
        long windowHash = 0L;
        for (int i = 0; i < m; i++) {
            patternHash = patternHash * base + pattern.charAt(i) + 1L;
            windowHash = windowHash * base + text.charAt(i) + 1L;
            if (i + 1 < m) power *= base;
        }
        if (patternHash == windowHash && text.regionMatches(0, pattern, 0, m)) return 0;
        for (int start = 1; start <= text.length() - m; start++) {
            long outgoing = text.charAt(start - 1) + 1L;
            long incoming = text.charAt(start + m - 1) + 1L;
            windowHash = (windowHash - outgoing * power) * base + incoming;
            if (windowHash == patternHash && text.regionMatches(start, pattern, 0, m)) {
                return start;
            }
        }
        return -1;
    }

    public static boolean isAnagram(String left, String right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length() != right.length()) return false;
        int[] counts = new int[CHAR_SPACE];
        for (int i = 0; i < left.length(); i++) {
            counts[left.charAt(i)]++;
            counts[right.charAt(i)]--;
        }
        for (int count : counts) if (count != 0) return false;
        return true;
    }

    public static boolean isPalindromeIgnoringNonAlphanumeric(String value) {
        Objects.requireNonNull(value, "value");
        int left = 0;
        int right = value.length() - 1;
        while (left < right) {
            while (left < right && !Character.isLetterOrDigit(value.charAt(left))) left++;
            while (left < right && !Character.isLetterOrDigit(value.charAt(right))) right--;
            if (Character.toLowerCase(value.charAt(left))
                    != Character.toLowerCase(value.charAt(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    public static String longestCommonPrefix(String[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) return "";
        for (String value : values) Objects.requireNonNull(value, "value");
        int length = values[0].length();
        for (int i = 1; i < values.length; i++) {
            length = Math.min(length, values[i].length());
            int j = 0;
            while (j < length && values[0].charAt(j) == values[i].charAt(j)) j++;
            length = j;
            if (length == 0) return "";
        }
        return values[0].substring(0, length);
    }

    public static String reverseWords(String value) {
        Objects.requireNonNull(value, "value");
        String stripped = value.strip();
        if (stripped.isEmpty()) return "";
        String[] words = stripped.split("\\s+");
        StringBuilder output = new StringBuilder(stripped.length());
        for (int i = words.length - 1; i >= 0; i--) {
            if (!output.isEmpty()) output.append(' ');
            output.append(words[i]);
        }
        return output.toString();
    }

}
