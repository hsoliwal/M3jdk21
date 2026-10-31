/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.util;

/**
 * M3JDK21's additive String utility family, alongside {@link Objects}, {@link Arrays}
 * and {@link Collections}. No duplicate String payload, regex parser, cache or JNI ABI.
 *
 * <p>The underlying JDK {@link String} operations retain semantic authority, including
 * M3 String owner/range optimizations when enabled. Utility operations that must return
 * new text create their results through normal String APIs. Null and Unicode contracts
 * are specified per method, rather than silently adopting one donor library's policy.
 *
 * <p>API shapes were independently reviewed against Guava Strings, Apache Commons Lang
 * StringUtils, Spring StringUtils and OpenJDK. No third-party implementation is copied.
 */
public final class Strings {
    private Strings() { throw new AssertionError("no instances"); }

    /**
     * Returns whether the sequence is null or has no UTF-16 code units.
     *
     * @param source the sequence to test, which may be null
     * @return true if source is null or empty
     */
    public static boolean isNullOrEmpty(CharSequence source) {
        return source == null || source.length() == 0;
    }

    /**
     * Normalizes a null reference to the canonical empty String.
     *
     * @param source the String to normalize, which may be null
     * @return the empty String if source is null; otherwise source itself
     */
    public static String nullToEmpty(String source) {
        return source == null ? "" : source;
    }

    /**
     * Converts an empty String to null, leaving nonempty identity untouched.
     *
     * @param source the String to normalize, which may be null
     * @return null if source is null or empty; otherwise source itself
     */
    public static String emptyToNull(String source) {
        return source == null || source.isEmpty() ? null : source;
    }

    /**
     * Returns whether the sequence is null, empty, or consists entirely of
     * Unicode code points classified as whitespace by
     * {@link Character#isWhitespace(int)}, the rule used by {@link String#isBlank()}.
     *
     * @param source the sequence to test, which may be null
     * @return true if source is null, empty, or entirely whitespace
     */
    public static boolean isNullOrBlank(CharSequence source) {
        if (source == null) return true;
        for (int i = 0, size = source.length(); i < size;) {
            int cp = Character.codePointAt(source, i);
            if (!Character.isWhitespace(cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    /**
     * Returns whether the sequence contains at least one non-whitespace code point.
     * Whitespace is determined by {@link Character#isWhitespace(int)}.
     *
     * @param source the sequence to test, which may be null
     * @return true if source is non-null and contains a non-whitespace code point
     */
    public static boolean hasText(CharSequence source) {
        return !isNullOrBlank(source);
    }

    /**
     * Returns fallback only for null or empty text.
     *
     * @param source the String to test, which may be null
     * @param fallback the replacement, which may be null
     * @return fallback if source is null or empty; otherwise source itself
     */
    public static String defaultIfEmpty(String source, String fallback) {
        return source == null || source.isEmpty() ? fallback : source;
    }

    /**
     * Returns fallback only for null, empty, or entirely whitespace text.
     * Whitespace is determined by {@link Character#isWhitespace(int)}.
     *
     * @param source the String to test, which may be null
     * @param fallback the replacement, which may be null
     * @return fallback if source is null or blank; otherwise source itself
     */
    public static String defaultIfBlank(String source, String fallback) {
        return isNullOrBlank(source) ? fallback : source;
    }

    /**
     * Delegates repetition to {@link String#repeat(int)}.
     *
     * @param source the non-null String to repeat
     * @param copies the number of repetitions
     * @return the String produced by repeating source copies times
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if copies is negative
     */
    public static String repeat(String source, int copies) {
        return Objects.requireNonNull(source, "source").repeat(copies);
    }

    /**
     * Prepends padding to reach at least minLength UTF-16 code units.
     * An already long enough source is returned unchanged; padding is by char,
     * without adjusting surrogate pairs.
     *
     * @param source the non-null String to pad
     * @param minLength the minimum length in UTF-16 code units
     * @param pad the UTF-16 code unit to prepend
     * @return source itself if long enough; otherwise the left-padded String
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if minLength is negative
     */
    public static String padStart(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return String.valueOf(pad).repeat(minLength - source.length()).concat(source);
    }

    /**
     * Appends padding to reach at least minLength UTF-16 code units.
     * An already long enough source is returned unchanged; padding is by char,
     * without adjusting surrogate pairs.
     *
     * @param source the non-null String to pad
     * @param minLength the minimum length in UTF-16 code units
     * @param pad the UTF-16 code unit to append
     * @return source itself if long enough; otherwise the right-padded String
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if minLength is negative
     */
    public static String padEnd(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return source.concat(String.valueOf(pad).repeat(minLength - source.length()));
    }

    /**
     * Returns the longest equal UTF-16 prefix that does not split a valid
     * surrogate pair in either source. Other code units, including unpaired
     * surrogates, retain String semantics.
     *
     * @param first the first non-null String
     * @param second the second non-null String
     * @return the common prefix, or the empty String if none exists
     * @throws NullPointerException if either String is null
     */
    public static String commonPrefix(String first, String second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        int end = Math.min(first.length(), second.length());
        int i = 0;
        while (i < end && first.charAt(i) == second.charAt(i)) i++;
        if (i > 0 && (surrogatePairAt(first, i - 1) || surrogatePairAt(second, i - 1))) i--;
        return first.substring(0, i);
    }

    /**
     * Returns the longest equal UTF-16 suffix that does not split a valid
     * surrogate pair in either source. Unpaired surrogates are compared as code units.
     *
     * @param first the first non-null String
     * @param second the second non-null String
     * @return the common suffix, or the empty String if none exists
     * @throws NullPointerException if either String is null
     */
    public static String commonSuffix(String first, String second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        int count = 0;
        while (count < Math.min(first.length(), second.length())
                && first.charAt(first.length() - count - 1)
                        == second.charAt(second.length() - count - 1)) count++;
        int a = first.length() - count;
        int b = second.length() - count;
        if (surrogatePairAt(first, a - 1) || surrogatePairAt(second, b - 1)) count--;
        return first.substring(first.length() - count);
    }

    /**
     * Removes one matching, case-sensitive prefix.
     *
     * @param source the non-null String to examine
     * @param prefix the non-null prefix to remove, which may be empty
     * @return source without the prefix if matched; otherwise source itself
     * @throws NullPointerException if source or prefix is null
     */
    public static String removePrefix(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.startsWith(prefix) ? source.substring(prefix.length()) : source;
    }

    /**
     * Removes one matching, case-sensitive suffix.
     *
     * @param source the non-null String to examine
     * @param suffix the non-null suffix to remove, which may be empty
     * @return source without the suffix if matched; otherwise source itself
     * @throws NullPointerException if source or suffix is null
     */
    public static String removeSuffix(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return source.endsWith(suffix) ? source.substring(0, source.length() - suffix.length())
                : source;
    }

    /**
     * Returns the part before the first case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter; an empty delimiter matches at index zero
     * @return the text before the match, or source itself if absent
     * @throws NullPointerException if source or delimiter is null
     */
    public static String substringBefore(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

    /**
     * Returns the part after the first case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter; an empty delimiter matches at index zero
     * @return the text after the match, or the empty String if absent
     * @throws NullPointerException if source or delimiter is null
     */
    public static String substringAfter(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

    /**
     * Returns the part before the last case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter to find
     * @return source itself if delimiter is empty or absent; otherwise the text before it
     * @throws NullPointerException if source or delimiter is null
     */
    public static String substringBeforeLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return source;
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

    /**
     * Returns the part after the last case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter to find
     * @return the empty String if delimiter is empty or absent; otherwise the text after it
     * @throws NullPointerException if source or delimiter is null
     */
    public static String substringAfterLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return "";
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

    /**
     * Counts non-overlapping, case-sensitive occurrences.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @return the number of non-overlapping matches, or zero if needle is empty
     * @throws NullPointerException if source or needle is null
     */
    public static int countOccurrences(String source, String needle) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while (from <= source.length() - needle.length()) {
            int at = source.indexOf(needle, from);
            if (at < 0) break;
            count++;
            from = at + needle.length();
        }
        return count;
    }

    /**
     * Tests the prefix using {@link String#regionMatches(boolean, int, String, int, int)}
     * with case ignored, without locale-sensitive case rewriting.
     *
     * @param source the non-null String to examine
     * @param prefix the non-null prefix to test, which may be empty
     * @return true if the prefix matches, ignoring case
     * @throws NullPointerException if source or prefix is null
     */
    public static boolean startsWithIgnoreCase(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Tests the suffix using {@link String#regionMatches(boolean, int, String, int, int)}
     * with case ignored, without locale-sensitive case rewriting.
     *
     * @param source the non-null String to examine
     * @param suffix the non-null suffix to test, which may be empty
     * @return true if the suffix matches, ignoring case
     * @throws NullPointerException if source or suffix is null
     */
    public static boolean endsWithIgnoreCase(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return suffix.length() <= source.length()
                && source.regionMatches(true, source.length() - suffix.length(),
                        suffix, 0, suffix.length());
    }

    /**
     * Finds the first match at or after fromIndex using
     * {@link String#regionMatches(boolean, int, String, int, int)} with case ignored.
     * A negative starting index is treated as zero. An empty needle matches at the
     * starting index clamped to the source length.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @param fromIndex the starting UTF-16 code-unit index
     * @return the first matching UTF-16 index, or -1 if absent
     * @throws NullPointerException if source or needle is null
     */
    public static int indexOfIgnoreCase(String source, String needle, int fromIndex) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(needle, "needle");
        int from = Math.max(0, fromIndex);
        if (needle.isEmpty()) return Math.min(from, source.length());
        int last = source.length() - needle.length();
        for (int i = from; i <= last; i++) {
            if (source.regionMatches(true, i, needle, 0, needle.length())) return i;
        }
        return -1;
    }

    /**
     * Finds the first match, starting at index zero, using
     * {@link String#regionMatches(boolean, int, String, int, int)} with case ignored.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @return the first matching UTF-16 index, zero for an empty needle, or -1 if absent
     * @throws NullPointerException if source or needle is null
     */
    public static int indexOfIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle, 0);
    }

    /**
     * Tests whether the source contains the needle using
     * {@link String#regionMatches(boolean, int, String, int, int)} with case ignored.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @return true if a match exists, including when needle is empty
     * @throws NullPointerException if source or needle is null
     */
    public static boolean containsIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle) >= 0;
    }

    /**
     * Reverses the text using {@link StringBuilder#reverse()}, including its
     * surrogate-pair handling. Reversing unpaired surrogates may form new pairs.
     *
     * @param source the non-null String to reverse
     * @return source itself if shorter than two UTF-16 units; otherwise the reversed text
     * @throws NullPointerException if source is null
     */
    public static String reverse(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() < 2) return source;
        return new StringBuilder(source).reverse().toString();
    }

    /**
     * Strips leading and trailing whitespace using {@link String#strip()}.
     *
     * @param source the String to strip, which may be null
     * @return null if source is null or the stripped result is empty; otherwise that result
     */
    public static String stripToNull(String source) {
        if (source == null) return null;
        String trimmed = source.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Strips leading and trailing whitespace using {@link String#strip()},
     * normalizing a null reference to the empty String.
     *
     * @param source the String to strip, which may be null
     * @return the empty String if source is null; otherwise the stripped result
     */
    public static String stripToEmpty(String source) {
        return source == null ? "" : source.strip();
    }

    private static boolean surrogatePairAt(String source, int index) {
        return index >= 0 && index + 1 < source.length()
                && Character.isHighSurrogate(source.charAt(index))
                && Character.isLowSurrogate(source.charAt(index + 1));
    }
}
