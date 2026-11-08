// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Arrays;

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

    //FLLDCIM: FOLD 5834d9dfa933 ZAlgorithm.zSearch
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

    //FLLDCIM: FOLD 5834d9dfa933 ZAlgorithm.zSearch
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

    //FLLDCIM: FOLD 41f8af0ca760 ZPrefixMatch.SENTINEL
    private static final int SENTINEL = Character.MAX_VALUE + 1;

    //FLLDCIM: FOLD 41f8af0ca760 ZPrefixMatch.prefixMatchLengths
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

    //FLLDCIM: FOLD 41f8af0ca760 ZPrefixMatch.prefixMatchLengths
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

    //FLLDCIM: FOLD 41f8af0ca760 ZPrefixMatch.scanVirtualZ
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

    //FLLDCIM: FOLD 41f8af0ca760 ZPrefixMatch.virtualUnit
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


    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.manacherOdd
    /** Odd-palindrome radii where radius includes the center character. */
    public static int[] manacherOdd(String value) {
        Objects.requireNonNull(value, "value");
        int n = value.length();
        int[] radius = new int[n];
        int left = 0;
        int right = -1;
        for (int i = 0; i < n; i++) {
            int k = i > right ? 1 : Math.min(radius[left + right - i], right - i + 1);
            while (i - k >= 0 && i + k < n && value.charAt(i - k) == value.charAt(i + k)) k++;
            radius[i] = k;
            if (i + k - 1 > right) {
                left = i - k + 1;
                right = i + k - 1;
            }
        }
        return radius;
    }

    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.manacherEven
    /** Even-palindrome radii centered between i-1 and i. */
    public static int[] manacherEven(String value) {
        Objects.requireNonNull(value, "value");
        int n = value.length();
        int[] radius = new int[n];
        int left = 0;
        int right = -1;
        for (int i = 0; i < n; i++) {
            int k = i > right ? 0 : Math.min(radius[left + right - i + 1], right - i + 1);
            while (i - k - 1 >= 0 && i + k < n
                    && value.charAt(i - k - 1) == value.charAt(i + k)) {
                k++;
            }
            radius[i] = k;
            if (i + k - 1 > right) {
                left = i - k;
                right = i + k - 1;
            }
        }
        return radius;
    }

    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.suffixArray
    /**
     * Suffix array of UTF-16 code-unit indices using doubling + primitive stable merge sort.
     * Complexity is O(n log^2 n) comparisons and O(n) auxiliary primitive storage.
     */
    public static int[] suffixArray(String value) {
        Objects.requireNonNull(value, "value");
        int n = value.length();
        if (n == 0) return new int[0];
        int[] order = new int[n];
        int[] buffer = new int[n];
        int[] rank = new int[n];
        int[] nextRank = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
            rank[i] = value.charAt(i);
        }

        int width = 1;
        while (true) {
            mergeSortSuffixes(order, buffer, rank, width, n);
            int classes = 0;
            nextRank[order[0]] = 0;
            for (int i = 1; i < n; i++) {
                if (compareSuffix(order[i - 1], order[i], rank, width, n) != 0) classes++;
                nextRank[order[i]] = classes;
            }
            int[] swap = rank;
            rank = nextRank;
            nextRank = swap;
            if (classes == n - 1) break;
            if (width >= n) break;
            width = width > n / 2 ? n : width << 1;
        }
        return order;
    }

    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.lcpArray
    /** Kasai LCP array: result[i] is LCP(sa[i], sa[i+1]); length=max(0,n-1). */
    public static int[] lcpArray(String value, int[] suffixArray) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(suffixArray, "suffixArray");
        int n = value.length();
        if (suffixArray.length != n) throw new IllegalArgumentException("suffix array length mismatch");
        if (n <= 1) return new int[0];
        int[] inverse = new int[n];
        boolean[] seen = new boolean[n];
        for (int position = 0; position < n; position++) {
            int suffix = suffixArray[position];
            if (suffix < 0 || suffix >= n || seen[suffix]) {
                throw new IllegalArgumentException("suffix array is not a permutation");
            }
            seen[suffix] = true;
            inverse[suffix] = position;
        }
        int[] lcp = new int[n - 1];
        int length = 0;
        for (int suffix = 0; suffix < n; suffix++) {
            int position = inverse[suffix];
            if (position == n - 1) {
                length = 0;
                continue;
            }
            int other = suffixArray[position + 1];
            while (suffix + length < n
                    && other + length < n
                    && value.charAt(suffix + length) == value.charAt(other + length)) {
                length++;
            }
            lcp[position] = length;
            if (length > 0) length--;
        }
        return lcp;
    }

    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.longestRepeatedSubstring
    public static String longestRepeatedSubstring(String value) {
        Objects.requireNonNull(value, "value");
        int[] suffixes = suffixArray(value);
        int[] lcp = lcpArray(value, suffixes);
        int bestLength = 0;
        int bestStart = 0;
        for (int i = 0; i < lcp.length; i++) {
            if (lcp[i] > bestLength) {
                bestLength = lcp[i];
                bestStart = suffixes[i];
            }
        }
        return value.substring(bestStart, bestStart + bestLength);
    }

    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.mergeSortSuffixes
    private static void mergeSortSuffixes(
            int[] order,
            int[] buffer,
            int[] rank,
            int width,
            int n) {
        for (int run = 1; run < n; run = run > n / 2 ? n : run << 1) {
            for (int start = 0; start < n; start += run << 1) {
                int middle = Math.min(start + run, n);
                int end = Math.min(start + (run << 1), n);
                int left = start;
                int right = middle;
                int cursor = start;
                while (left < middle && right < end) {
                    if (compareSuffix(order[left], order[right], rank, width, n) <= 0) {
                        buffer[cursor++] = order[left++];
                    } else {
                        buffer[cursor++] = order[right++];
                    }
                }
                while (left < middle) buffer[cursor++] = order[left++];
                while (right < end) buffer[cursor++] = order[right++];
            }
            System.arraycopy(buffer, 0, order, 0, n);
            if (run == n) break;
        }
    }

    //FLLDCIM: FOLD 739221ead5fa AdvancedStringAlgorithms.compareSuffix
    private static int compareSuffix(
            int left,
            int right,
            int[] rank,
            int width,
            int n) {
        int comparison = Integer.compare(rank[left], rank[right]);
        if (comparison != 0) return comparison;
        int leftSecond = left + width < n ? rank[left + width] : -1;
        int rightSecond = right + width < n ? rank[right + width] : -1;
        return Integer.compare(leftSecond, rightSecond);
    }

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.CHAR_SPACE
    private static final int CHAR_SPACE = Character.MAX_VALUE + 1;

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.minimumWindow
    public static String minimumWindow(String source, String required) {
        return minimumWindow(source, required, ProgressMonitor.none());
    }

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.minimumWindow
    public static String minimumWindow(
            String source,
            String required,
            ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(required, "required");
        if (required.isEmpty()) return "";
        ProgressMonitor progress = monitor == null ? ProgressMonitor.none() : monitor;
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.kmpIndexOf
    public static int kmpIndexOf(String text, String pattern) {
        return kmpIndexOf(text, pattern, ProgressMonitor.none());
    }

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.kmpIndexOf
    public static int kmpIndexOf(
            String text,
            String pattern,
            ProgressMonitor monitor) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) return 0;
        ProgressMonitor progress = monitor == null ? ProgressMonitor.none() : monitor;
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.prefixFunction
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.rabinKarpIndexOf
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.isAnagram
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.isPalindromeIgnoringNonAlphanumeric
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.longestCommonPrefix
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

    //FLLDCIM: FOLD 9f840d89c5a7 PrimitiveStringAlgorithms.reverseWords
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


    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.Query
    public record OneMismatchQuery(String text, String pattern) {
        public OneMismatchQuery {
            Objects.requireNonNull(text, "text");
            requireOneMismatchPattern(pattern);
        }
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.FIRST_MATCH_CAPACITY
    private static final int ONE_MISMATCH_FIRST_MATCH_CAPACITY = 8;

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.firstIndex
    public static int oneMismatchFirstIndex(OneMismatchQuery query, ProgressMonitor monitor) {
        OneMismatchQuery checked = Objects.requireNonNull(query, "query");
        return oneMismatchFirstIndex(checked.text(), checked.pattern(), monitor);
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.firstIndex
    public static int oneMismatchFirstIndex(String text, String pattern) {
        return oneMismatchFirstIndex(text, pattern, null);
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.firstIndex
    public static int oneMismatchFirstIndex(
            String text, String pattern, ProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = requireOneMismatchPattern(pattern);
        ProgressMonitor checked = ProgressChecks.nonNull(monitor);
        ProgressChecks.checkCanceled(checked);
        if (needle.length() > haystack.length()) {
            return -1;
        }
        // A one-code-unit window can differ in at most one code unit.
        // Keep custom monitor callbacks on the original geometry path.
        if (monitor == null && needle.length() == 1) {
            ProgressChecks.checkCanceled(checked);
            return 0;
        }

        OneMismatchGeometry geometry = oneMismatchGeometry(haystack, needle, checked);
        int maximum = haystack.length() - needle.length();
        for (int start = 0; start <= maximum; start++) {
            if ((start & 1023) == 0) {
                ProgressChecks.checkCanceled(checked);
            }
            if (geometry.matches(start)) {
                ProgressChecks.checkCanceled(checked);
                return start;
            }
        }
        ProgressChecks.checkCanceled(checked);
        return -1;
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.allIndices
    public static int[] oneMismatchAllIndices(String text, String pattern) {
        return oneMismatchAllIndices(text, pattern, null);
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.allIndices
    public static int[] oneMismatchAllIndices(
            String text, String pattern, ProgressMonitor monitor) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = requireOneMismatchPattern(pattern);
        ProgressMonitor checked = ProgressChecks.nonNull(monitor);
        ProgressChecks.checkCanceled(checked);
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
                    ProgressChecks.checkCanceled(checked);
                }
                everyPosition[index] = index;
            }
            ProgressChecks.checkCanceled(checked);
            return everyPosition;
        }

        OneMismatchGeometry geometry = oneMismatchGeometry(haystack, needle, checked);
        int maximum = haystack.length() - needle.length();
        // Allocate result slots only on observed hits, not for every possible window.
        // The upper bound fits int because a nonempty needle is mandatory.
        int limit = maximum + 1;
        int[] matches = new int[0];
        int count = 0;
        for (int start = 0; start <= maximum; start++) {
            if ((start & 1023) == 0) {
                ProgressChecks.checkCanceled(checked);
            }
            if (geometry.matches(start)) {
                if (count == matches.length) {
                    int capacity = matches.length;
                    int next = capacity == 0 ? Math.min(ONE_MISMATCH_FIRST_MATCH_CAPACITY, limit)
                            : capacity >= limit - capacity ? limit : capacity * 2;
                    matches = Arrays.copyOf(matches, next);
                }
                matches[count++] = start;
            }
        }
        ProgressChecks.checkCanceled(checked);
        return count == matches.length ? matches : Arrays.copyOf(matches, count);
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.matchesAt
    /** Direct reference predicate used by parity/property tests and bounded callers. */
    public static boolean oneMismatchMatchesAt(String text, String pattern, int start) {
        String haystack = Objects.requireNonNull(text, "text");
        String needle = requireOneMismatchPattern(pattern);
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

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.geometry
    private static OneMismatchGeometry oneMismatchGeometry(
            String text, String pattern, ProgressMonitor monitor) {
        int[] prefix = StringAlgorithms.prefixMatchLengths(text, pattern, monitor);
        String reversedText = reverseCodeUnits(text);
        String reversedPattern = reverseCodeUnits(pattern);
        int[] reversedPrefix =
                StringAlgorithms.prefixMatchLengths(reversedText, reversedPattern, monitor);
        return new OneMismatchGeometry(text.length(), pattern.length(), prefix, reversedPrefix);
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.Geometry
    private record OneMismatchGeometry(
            int textLength,
            int patternLength,
            int[] prefix,
            int[] reversedPrefix) {
        private OneMismatchGeometry {
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

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.requirePattern
    private static String requireOneMismatchPattern(String pattern) {
        String value = Objects.requireNonNull(pattern, "pattern");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
        return value;
    }

    //FLLDCIM: FOLD a0482a54939c OneMismatchWindowSearch.reverseCodeUnits
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
