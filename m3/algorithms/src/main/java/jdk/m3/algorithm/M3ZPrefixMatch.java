// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressChecks;
import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.Objects;

/**
 * Longest pattern-prefix match at every text position over Java UTF-16 code units (Z geometry).
 *
 * <p>The implementation does not build {@code pattern + delimiter + text}. Instead it scans a
 * virtual sequence whose separator is the integer value 65536, outside the complete UTF-16 code
 * unit domain. Therefore NUL, '$', isolated surrogates and every other {@code char} value retain
 * exact {@link String#indexOf(String)} semantics. First-hit search is M3ZAlgorithm.zSearch.</p>
 */
//FLLDCIM: RELOCATE - donor blob d7f00eb8fb85 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
public final class M3ZPrefixMatch {
    private static final int SENTINEL = Character.MAX_VALUE + 1;

    private M3ZPrefixMatch() {}

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

    public static int[] prefixMatchLengths(
            String text, String pattern, M3ProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = Objects.requireNonNull(pattern, "pattern");
        M3ProgressMonitor checked = M3ProgressChecks.nonNull(monitor);
        M3ProgressChecks.checkCanceled(checked);
        int[] matches = new int[haystack.length()];
        if (haystack.isEmpty() || needle.isEmpty()) {
            return matches;
        }
        long totalLong = (long) needle.length() + 1L + haystack.length();
        if (totalLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("virtual Z geometry exceeds int range");
        }
        scanVirtualZ(haystack, needle, checked, matches, false);
        M3ProgressChecks.checkCanceled(checked);
        return matches;
    }

    /**
     * Shared virtual Z scan. When prefixMatches is non-null it receives one LCP length per text
     * position. Exact search passes null and may return immediately at the first full match.
     */
    private static int scanVirtualZ(
            String haystack,
            String needle,
            M3ProgressMonitor monitor,
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
                M3ProgressChecks.checkCanceled(monitor);
            }
            if (index <= right) {
                z[index] = Math.min(right - index + 1, z[index - left]);
            }

            while (z[index] < total - index
                    && virtualUnit(needle, haystack, z[index])
                            == virtualUnit(needle, haystack, index + z[index])) {
                z[index]++;
                if ((++comparisons & 1023L) == 0L) {
                    M3ProgressChecks.checkCanceled(monitor);
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
                    M3ProgressChecks.checkCanceled(monitor);
                    return textIndex;
                }
            }
        }
        M3ProgressChecks.checkCanceled(monitor);
        return -1;
    }

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
