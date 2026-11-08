// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.AlgorithmSupport;
import jdk.m3.algorithm.ProgressMonitor;
import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-window search with at most one differing UTF-16 code unit.
 *
 * <p>This is the reviewed mechanical shape shared by public challenge metadata such as
 * LeetCode 3303 and HackerRank Save Humanity. Public problem pages are evidence/taxonomy donors
 * only; this implementation is original Synexia code. It composes the canonical Z-prefix geometry
 * in {@link StringAlgorithms#prefixMatchLengths(String, String, ProgressMonitor)} in forward and reverse directions.</p>
 *
 * <p>For each alignment, let {@code p} be the longest matching prefix and {@code s} the longest
 * matching suffix. The window differs in at most one position iff {@code p + s >= m - 1}.
 * Therefore preprocessing plus the alignment scan is O(n + m) time and O(n + m) scratch.</p>
 */
//FLLDCIM: RELOCATE - donor blob 9efbe34aa3d1 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
public final class OneMismatchWindowSearch {
    public record Query(String text, String pattern) {
        public Query {
            Objects.requireNonNull(text, "text");
            requirePattern(pattern);
        }
    }

    private static final int FIRST_MATCH_CAPACITY = 8;

    private OneMismatchWindowSearch() {}

    public static int firstIndex(Query query, ProgressMonitor monitor) {
        Query checked = Objects.requireNonNull(query, "query");
        return firstIndex(checked.text(), checked.pattern(), monitor);
    }

    public static int firstIndex(String text, String pattern) {
        return firstIndex(text, pattern, null);
    }

    public static int firstIndex(
            String text, String pattern, ProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = requirePattern(pattern);
        ProgressMonitor checked = AlgorithmSupport.nonNull(monitor);
        AlgorithmSupport.checkCanceled(checked);
        if (needle.length() > haystack.length()) {
            return -1;
        }
        // A one-code-unit window can differ in at most one code unit.
        // Keep custom monitor callbacks on the original geometry path.
        if (monitor == null && needle.length() == 1) {
            AlgorithmSupport.checkCanceled(checked);
            return 0;
        }

        Geometry geometry = geometry(haystack, needle, checked);
        int maximum = haystack.length() - needle.length();
        for (int start = 0; start <= maximum; start++) {
            if ((start & 1023) == 0) {
                AlgorithmSupport.checkCanceled(checked);
            }
            if (geometry.matches(start)) {
                AlgorithmSupport.checkCanceled(checked);
                return start;
            }
        }
        AlgorithmSupport.checkCanceled(checked);
        return -1;
    }

    public static int[] allIndices(String text, String pattern) {
        return allIndices(text, pattern, null);
    }

    public static int[] allIndices(
            String text, String pattern, ProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = requirePattern(pattern);
        ProgressMonitor checked = AlgorithmSupport.nonNull(monitor);
        AlgorithmSupport.checkCanceled(checked);
        if (needle.length() > haystack.length()) {
            return new int[0];
        }
        // Every singleton-pattern position is a match under distance <= 1.
        // Avoid reverse strings and two text-sized Z tables on this Java path.
        // Caller-supplied monitors keep their original progress/poll cadence.
        if (monitor == null && needle.length() == 1) {
            int[] everyPosition = new int[haystack.length()];
            for (int index = 0; index < everyPosition.length; index++) {
                if ((index & 1023) == 0) {
                    AlgorithmSupport.checkCanceled(checked);
                }
                everyPosition[index] = index;
            }
            AlgorithmSupport.checkCanceled(checked);
            return everyPosition;
        }

        Geometry geometry = geometry(haystack, needle, checked);
        int maximum = haystack.length() - needle.length();
        // Allocate result slots only on observed hits, not for every possible window.
        // The upper bound fits int because a nonempty needle is mandatory.
        int limit = maximum + 1;
        int[] matches = new int[0];
        int count = 0;
        for (int start = 0; start <= maximum; start++) {
            if ((start & 1023) == 0) {
                AlgorithmSupport.checkCanceled(checked);
            }
            if (geometry.matches(start)) {
                if (count == matches.length) {
                    int capacity = matches.length;
                    int next = capacity == 0 ? Math.min(FIRST_MATCH_CAPACITY, limit)
                            : capacity >= limit - capacity ? limit : capacity * 2;
                    matches = Arrays.copyOf(matches, next);
                }
                matches[count++] = start;
            }
        }
        AlgorithmSupport.checkCanceled(checked);
        return count == matches.length ? matches : Arrays.copyOf(matches, count);
    }

    /** Direct reference predicate used by parity/property tests and bounded callers. */
    public static boolean matchesAt(String text, String pattern, int start) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = requirePattern(pattern);
        if (start < 0 || start > haystack.length() - needle.length()) {
            return false;
        }
        int mismatches = 0;
        for (int index = 0; index < needle.length(); index++) {
            if (haystack.charAt(start + index) != needle.charAt(index)
                    && ++mismatches > 1) {
                return false;
            }
        }
        return true;
    }

    private static Geometry geometry(
            String text, String pattern, ProgressMonitor monitor) {
        int[] prefix = StringAlgorithms.prefixMatchLengths(text, pattern, monitor);
        String reversedText = reverseCodeUnits(text);
        String reversedPattern = reverseCodeUnits(pattern);
        int[] reversedPrefix =
                StringAlgorithms.prefixMatchLengths(reversedText, reversedPattern, monitor);
        return new Geometry(text.length(), pattern.length(), prefix, reversedPrefix);
    }

    private record Geometry(
            int textLength,
            int patternLength,
            int[] prefix,
            int[] reversedPrefix) {
        private Geometry {
            if (patternLength < 1
                    || prefix.length != textLength
                    || reversedPrefix.length != textLength) {
                throw new IllegalArgumentException("invalid one-mismatch geometry");
            }
        }

        boolean matches(int start) {
            int leading = Math.min(patternLength, prefix[start]);
            if (leading >= patternLength) {
                return true;
            }
            int reversedStart = textLength - start - patternLength;
            int trailing = Math.min(patternLength, reversedPrefix[reversedStart]);
            return leading + trailing >= patternLength - 1;
        }
    }

    private static String requirePattern(String pattern) {
        String value = Objects.requireNonNull(pattern, "pattern");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
        return value;
    }

    /** Reverse raw UTF-16 code units; do not apply StringBuilder's surrogate-pair repair. */
    private static String reverseCodeUnits(String value) {
        char[] units = value.toCharArray();
        for (int left = 0, right = units.length - 1; left < right; left++, right--) {
            char swap = units[left];
            units[left] = units[right];
            units[right] = swap;
        }
        return new String(units);
    }
}
