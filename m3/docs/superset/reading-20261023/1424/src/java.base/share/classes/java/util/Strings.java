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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * True when the supplied sequence is null or has no UTF-16 units.
     *
     * @param source the sequence to inspect; may be null
     * @return {@code true} if the sequence is null or empty
=======
     * Returns whether the sequence is null or has no characters.
     *
     * @param source the sequence, which may be null
     * @return true if the sequence is null or empty
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static boolean isNullOrEmpty(CharSequence source) {
        return source == null || source.length() == 0;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Normalizes a null reference to the canonical empty String.
     *
     * @param source the string to normalize; may be null
     * @return the original string, or the empty string if it is null
=======
     * Returns the source, substituting an empty string for null.
     *
     * @param source the string, which may be null
     * @return the source, or an empty string if it is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String nullToEmpty(String source) {
        return source == null ? "" : source;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Converts an empty String to null, leaving nonempty identity untouched.
     *
     * @param source the string to normalize; may be null
     * @return null for a null or empty string, otherwise the original string
=======
     * Returns null for a null or empty source.
     *
     * @param source the string, which may be null
     * @return the source if nonempty, otherwise null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String emptyToNull(String source) {
        return source == null || source.isEmpty() ? null : source;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * True for null, empty text, or text whose Unicode code points all satisfy
     * {@link Character#isWhitespace(int)} (the rule also used by String.isBlank).
     *
     * @param source the sequence to inspect; may be null
     * @return {@code true} if the sequence is null, empty, or entirely whitespace
=======
     * Returns whether the sequence is null, empty, or consists only of Unicode
     * code points classified as whitespace by {@link Character#isWhitespace(int)}.
     *
     * @param source the sequence, which may be null
     * @return true if the sequence is null, empty, or contains only whitespace
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * True for non-null text containing at least one non-whitespace code point.
     *
     * @param source the sequence to inspect; may be null
     * @return {@code true} if the sequence contains a non-whitespace code point
=======
     * Returns whether the sequence contains a code point that is not whitespace.
     *
     * @param source the sequence, which may be null
     * @return true if a non-whitespace code point is present
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static boolean hasText(CharSequence source) {
        return !isNullOrBlank(source);
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns fallback only for null/empty text. The fallback itself may be null.
     *
     * @param source the string to inspect; may be null
     * @param fallback the value to return for null or empty text
     * @return the fallback for null or empty text, otherwise the original string
=======
     * Substitutes the fallback for a null or empty source.
     *
     * @param source the string, which may be null
     * @param fallback the replacement, which may be null
     * @return the fallback if the source is null or empty, otherwise the source
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String defaultIfEmpty(String source, String fallback) {
        return source == null || source.isEmpty() ? fallback : source;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns fallback only for null/blank text. The fallback itself may be null.
     *
     * @param source the string to inspect; may be null
     * @param fallback the value to return for null or blank text
     * @return the fallback for null or blank text, otherwise the original string
=======
     * Substitutes the fallback for a null, empty, or whitespace-only source.
     *
     * @param source the string, which may be null
     * @param fallback the replacement, which may be null
     * @return the fallback if the source is blank, otherwise the source
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String defaultIfBlank(String source, String fallback) {
        return isNullOrBlank(source) ? fallback : source;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Delegates repetition to String's canonical runtime owner.
     *
     * @param source the non-null string to repeat
     * @param copies the non-negative number of repetitions
     * @return the string repeated the requested number of times
=======
     * Repeats the source the requested number of times.
     *
     * @param source the string to repeat
     * @param copies the nonnegative repetition count
     * @return the repeated string
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if copies is negative
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String repeat(String source, int copies) {
        return Objects.requireNonNull(source, "source").repeat(copies);
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Pads to at least minLength UTF-16 units; a larger source is returned unchanged.
     *
     * @param source the non-null string to pad
     * @param minLength the non-negative minimum result length in UTF-16 units
     * @param pad the UTF-16 unit to prepend as needed
     * @return the padded string, or the original string if it is already long enough
=======
     * Pads the beginning with the given character to reach the minimum UTF-16 length.
     *
     * @param source the string to pad
     * @param minLength the nonnegative minimum length in UTF-16 code units
     * @param pad the padding character
     * @return the source if already long enough, otherwise the padded string
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if minLength is negative
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String padStart(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return String.valueOf(pad).repeat(minLength - source.length()).concat(source);
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Pads to at least minLength UTF-16 units; a larger source is returned unchanged.
     *
     * @param source the non-null string to pad
     * @param minLength the non-negative minimum result length in UTF-16 units
     * @param pad the UTF-16 unit to append as needed
     * @return the padded string, or the original string if it is already long enough
=======
     * Pads the end with the given character to reach the minimum UTF-16 length.
     *
     * @param source the string to pad
     * @param minLength the nonnegative minimum length in UTF-16 code units
     * @param pad the padding character
     * @return the source if already long enough, otherwise the padded string
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if minLength is negative
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String padEnd(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return source.concat(String.valueOf(pad).repeat(minLength - source.length()));
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Longest equal UTF-16 prefix that does not split a valid surrogate pair in either
     * source. Other code units, including unpaired surrogates, retain String semantics.
     *
     * @param first the first non-null string
     * @param second the second non-null string
     * @return the longest common prefix that preserves paired surrogate boundaries
=======
     * Returns the common UTF-16 prefix without splitting a surrogate pair in either input.
     *
     * @param first the first string
     * @param second the second string
     * @return the common prefix, possibly empty
     * @throws NullPointerException if either string is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Longest equal UTF-16 suffix, without splitting a paired surrogate in either source.
     *
     * @param first the first non-null string
     * @param second the second non-null string
     * @return the longest common suffix that preserves paired surrogate boundaries
=======
     * Returns the common UTF-16 suffix without splitting a surrogate pair in either input.
     *
     * @param first the first string
     * @param second the second string
     * @return the common suffix, possibly empty
     * @throws NullPointerException if either string is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns source itself when prefix does not match.
     *
     * @param source the non-null string to inspect
     * @param prefix the non-null prefix to remove
     * @return the string after the matching prefix, or the original string if absent
=======
     * Removes a matching, case-sensitive prefix.
     *
     * @param source the string to examine
     * @param prefix the prefix to remove
     * @return the remaining string, or the source if the prefix does not match
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String removePrefix(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.startsWith(prefix) ? source.substring(prefix.length()) : source;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns source itself when suffix does not match.
     *
     * @param source the non-null string to inspect
     * @param suffix the non-null suffix to remove
     * @return the string before the matching suffix, or the original string if absent
=======
     * Removes a matching, case-sensitive suffix.
     *
     * @param source the string to examine
     * @param suffix the suffix to remove
     * @return the remaining string, or the source if the suffix does not match
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String removeSuffix(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return source.endsWith(suffix) ? source.substring(0, source.length() - suffix.length())
                : source;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns the part before the first delimiter, or the original text when absent.
     *
     * @param source the non-null string to inspect
     * @param delimiter the non-null delimiter to locate
     * @return the prefix before the first delimiter, or the original string if absent
=======
     * Returns the substring before the first occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the preceding substring, or the source if the delimiter is absent
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String substringBefore(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns the part after the first delimiter, or empty text when absent.
     *
     * @param source the non-null string to inspect
     * @param delimiter the non-null delimiter to locate
     * @return the suffix after the first delimiter, or the empty string if absent
=======
     * Returns the substring after the first occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the following substring, or an empty string if the delimiter is absent
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String substringAfter(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns the part before the last delimiter, or original text when absent.
     *
     * @param source the non-null string to inspect
     * @param delimiter the non-null delimiter to locate
     * @return the prefix before the last delimiter, or the original string if absent
=======
     * Returns the substring before the last occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the preceding substring, or the source if the delimiter is empty or absent
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String substringBeforeLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return source;
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Returns the part after the last delimiter, or empty text when absent.
     *
     * @param source the non-null string to inspect
     * @param delimiter the non-null delimiter to locate
     * @return the suffix after the last delimiter, or the empty string if absent
=======
     * Returns the substring after the last occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the following substring, or an empty string if the delimiter is empty or absent
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String substringAfterLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return "";
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Non-overlapping, case-sensitive occurrences; an empty needle yields zero.
     *
     * @param source the non-null string to search
     * @param needle the non-null text to count
     * @return the number of non-overlapping occurrences, or zero for an empty needle
=======
     * Counts nonoverlapping, case-sensitive occurrences of the needle.
     *
     * @param source the string to search
     * @param needle the literal string to count
     * @return the number of occurrences, or zero for an empty needle
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * JDK regionMatches(ignoreCase) semantics; no locale-sensitive case rewriting.
     *
     * @param source the non-null string to inspect
     * @param prefix the non-null prefix to compare
     * @return {@code true} if the prefix matches with case ignored
=======
     * Tests a prefix using the case-insensitive rules of {@link String#regionMatches}.
     *
     * @param source the string to examine
     * @param prefix the prefix to test
     * @return true if the prefix matches without regard to case
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static boolean startsWithIgnoreCase(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * JDK regionMatches(ignoreCase) semantics; no locale-sensitive case rewriting.
     *
     * @param source the non-null string to inspect
     * @param suffix the non-null suffix to compare
     * @return {@code true} if the suffix matches with case ignored
=======
     * Tests a suffix using the case-insensitive rules of {@link String#regionMatches}.
     *
     * @param source the string to examine
     * @param suffix the suffix to test
     * @return true if the suffix matches without regard to case
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static boolean endsWithIgnoreCase(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return suffix.length() <= source.length()
                && source.regionMatches(true, source.length() - suffix.length(),
                        suffix, 0, suffix.length());
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * An empty needle occurs at the clamped starting position.
     *
     * @param source the non-null string to search
     * @param needle the non-null text to locate with case ignored
     * @param fromIndex the starting UTF-16 index; negative values are treated as zero
     * @return the first matching UTF-16 index at or after the start, or {@code -1}
     *         if absent; an empty needle clamps the start to the source length
=======
     * Finds the first case-insensitive occurrence at or after the starting index.
     * The starting index is raised to zero if negative. An empty needle matches
     * at the starting index clamped to the source length.
     *
     * @param source the string to search
     * @param needle the literal string to find
     * @param fromIndex the starting UTF-16 index
     * @return the first matching index, or -1 if no occurrence exists
     * @throws NullPointerException if either string is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Locates the first occurrence using JDK case-insensitive region matching.
     *
     * @param source the non-null string to search
     * @param needle the non-null text to locate
     * @return the first matching UTF-16 index, zero for an empty needle, or
     *         {@code -1} if absent
=======
     * Finds the first case-insensitive occurrence, starting at index zero.
     *
     * @param source the string to search
     * @param needle the literal string to find
     * @return the first matching index, or -1 if no occurrence exists
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static int indexOfIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle, 0);
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Tests for an occurrence using JDK case-insensitive region matching.
     *
     * @param source the non-null string to search
     * @param needle the non-null text to locate
     * @return {@code true} if the text occurs, including an empty needle
=======
     * Tests whether the source contains the needle without regard to case.
     *
     * @param source the string to search
     * @param needle the literal string to find
     * @return true if a case-insensitive occurrence exists
     * @throws NullPointerException if either argument is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static boolean containsIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle) >= 0;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Preserve paired UTF-16 surrogate units, as StringBuilder.reverse() does.
     *
     * @param source the non-null string to reverse
     * @return the reversed string, or the original string if its length is below two
=======
     * Reverses the source with the surrogate-pair handling of {@link StringBuilder#reverse()}.
     *
     * @param source the string to reverse
     * @return the reversed string
     * @throws NullPointerException if source is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String reverse(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() < 2) return source;
        return new StringBuilder(source).reverse().toString();
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Reuse java.lang.String.strip, including its Unicode whitespace semantics.
     *
     * @param source the string to strip; may be null
     * @return the stripped string, or null if the input is null or strips to empty
=======
     * Strips Unicode whitespace from both ends and substitutes null for an empty result.
     *
     * @param source the string, which may be null
     * @return the stripped nonempty string, or null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
     */
    public static String stripToNull(String source) {
        if (source == null) return null;
        String trimmed = source.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Null becomes empty; strip results reuse the underlying JDK String route.
     *
     * @param source the string to strip; may be null
     * @return the empty string for null, otherwise the stripped string
=======
     * Strips Unicode whitespace from both ends and substitutes an empty string for null.
     *
     * @param source the string, which may be null
     * @return the stripped string, or an empty string if source is null
>>>>>>> 7e9fe6c851d317be04e69c6d5ca3fe5d16a41352
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
