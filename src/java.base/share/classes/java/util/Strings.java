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

<<<<<<< HEAD
    /**
     * Returns whether the sequence is null or has no UTF-16 code units.
     *
     * @param source the sequence to inspect, or null
     * @return true if the sequence is null or empty
     */
||||||| merged common ancestors
    /** True when the supplied sequence is null or has no UTF-16 units. */
=======
    /**
     * Returns whether the sequence is null or has no characters.
     *
     * @param source the sequence, which may be null
     * @return true if the sequence is null or empty
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static boolean isNullOrEmpty(CharSequence source) {
        return source == null || source.length() == 0;
    }

<<<<<<< HEAD
    /**
     * Returns the empty string for a null reference.
     *
     * @param source the string to normalize, or null
     * @return the source itself when non-null, otherwise the empty string
     */
||||||| merged common ancestors
    /** Normalizes a null reference to the canonical empty String. */
=======
    /**
     * Returns the source, substituting an empty string for null.
     *
     * @param source the string, which may be null
     * @return the source, or an empty string if it is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String nullToEmpty(String source) {
        return source == null ? "" : source;
    }

<<<<<<< HEAD
    /**
     * Converts an empty string to null, retaining nonempty string identity.
     *
     * @param source the string to normalize, or null
     * @return null for a null or empty source, otherwise the source itself
     */
||||||| merged common ancestors
    /** Converts an empty String to null, leaving nonempty identity untouched. */
=======
    /**
     * Returns null for a null or empty source.
     *
     * @param source the string, which may be null
     * @return the source if nonempty, otherwise null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String emptyToNull(String source) {
        return source == null || source.isEmpty() ? null : source;
    }

    /**
<<<<<<< HEAD
     * Returns whether the sequence is null, empty, or contains only code points
     * classified as whitespace by {@link Character#isWhitespace(int)}.
     * This is the whitespace rule used by {@link String#isBlank()}.
     *
     * @param source the sequence to inspect, or null
     * @return true if the sequence has no non-whitespace code point
||||||| merged common ancestors
     * True for null or for text with no Unicode code point classified as whitespace by
     * {@link Character#isWhitespace(int)} (the rule also used by String.isBlank).
=======
     * Returns whether the sequence is null, empty, or consists only of Unicode
     * code points classified as whitespace by {@link Character#isWhitespace(int)}.
     *
     * @param source the sequence, which may be null
     * @return true if the sequence is null, empty, or contains only whitespace
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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

<<<<<<< HEAD
    /**
     * Returns whether the sequence contains a non-whitespace code point.
     *
     * @param source the sequence to inspect, or null
     * @return true if the source is non-null and is not blank
     */
||||||| merged common ancestors
    /** True for non-null text containing at least one non-whitespace code point. */
=======
    /**
     * Returns whether the sequence contains a code point that is not whitespace.
     *
     * @param source the sequence, which may be null
     * @return true if a non-whitespace code point is present
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static boolean hasText(CharSequence source) {
        return !isNullOrBlank(source);
    }

<<<<<<< HEAD
    /**
     * Returns the fallback for a null or empty source.
     *
     * @param source the string to inspect, or null
     * @param fallback the replacement, which may be null
     * @return the fallback if the source is null or empty, otherwise the source
     */
||||||| merged common ancestors
    /** Returns fallback only for null/empty text. The fallback itself may be null. */
=======
    /**
     * Substitutes the fallback for a null or empty source.
     *
     * @param source the string, which may be null
     * @param fallback the replacement, which may be null
     * @return the fallback if the source is null or empty, otherwise the source
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String defaultIfEmpty(String source, String fallback) {
        return source == null || source.isEmpty() ? fallback : source;
    }

<<<<<<< HEAD
    /**
     * Returns the fallback for a null or blank source.
     *
     * @param source the string to inspect, or null
     * @param fallback the replacement, which may be null
     * @return the fallback if the source is null or blank, otherwise the source
     */
||||||| merged common ancestors
    /** Returns fallback only for null/blank text. The fallback itself may be null. */
=======
    /**
     * Substitutes the fallback for a null, empty, or whitespace-only source.
     *
     * @param source the string, which may be null
     * @param fallback the replacement, which may be null
     * @return the fallback if the source is blank, otherwise the source
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String defaultIfBlank(String source, String fallback) {
        return isNullOrBlank(source) ? fallback : source;
    }

<<<<<<< HEAD
    /**
     * Repeats the source through {@link String#repeat(int)}.
     *
     * @param source the string to repeat
     * @param copies the nonnegative number of copies
     * @return the repeated string
     * @throws NullPointerException if the source is null
     * @throws IllegalArgumentException if the number of copies is negative
     */
||||||| merged common ancestors
    /** Delegates repetition to String's canonical runtime owner. */
=======
    /**
     * Repeats the source the requested number of times.
     *
     * @param source the string to repeat
     * @param copies the nonnegative repetition count
     * @return the repeated string
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if copies is negative
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String repeat(String source, int copies) {
        return Objects.requireNonNull(source, "source").repeat(copies);
    }

<<<<<<< HEAD
    /**
     * Pads the start of the source to at least the requested UTF-16 length.
     * A source already meeting that length is returned unchanged.
     *
     * @param source the string to pad
     * @param minLength the nonnegative minimum number of UTF-16 code units
     * @param pad the UTF-16 code unit to prepend as needed
     * @return the padded string, or the source itself if no padding is needed
     * @throws NullPointerException if the source is null
     * @throws IllegalArgumentException if the minimum length is negative
     */
||||||| merged common ancestors
    /** Pads to at least minLength UTF-16 units; a larger source is returned unchanged. */
=======
    /**
     * Pads the beginning with the given character to reach the minimum UTF-16 length.
     *
     * @param source the string to pad
     * @param minLength the nonnegative minimum length in UTF-16 code units
     * @param pad the padding character
     * @return the source if already long enough, otherwise the padded string
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if minLength is negative
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String padStart(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return String.valueOf(pad).repeat(minLength - source.length()).concat(source);
    }

<<<<<<< HEAD
    /**
     * Pads the end of the source to at least the requested UTF-16 length.
     * A source already meeting that length is returned unchanged.
     *
     * @param source the string to pad
     * @param minLength the nonnegative minimum number of UTF-16 code units
     * @param pad the UTF-16 code unit to append as needed
     * @return the padded string, or the source itself if no padding is needed
     * @throws NullPointerException if the source is null
     * @throws IllegalArgumentException if the minimum length is negative
     */
||||||| merged common ancestors
    /** Pads to at least minLength UTF-16 units; a larger source is returned unchanged. */
=======
    /**
     * Pads the end with the given character to reach the minimum UTF-16 length.
     *
     * @param source the string to pad
     * @param minLength the nonnegative minimum length in UTF-16 code units
     * @param pad the padding character
     * @return the source if already long enough, otherwise the padded string
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if minLength is negative
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String padEnd(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return source.concat(String.valueOf(pad).repeat(minLength - source.length()));
    }

    /**
<<<<<<< HEAD
     * Returns the longest equal UTF-16 prefix that does not split a valid
     * surrogate pair in either source. Unpaired surrogates retain String semantics.
     *
     * @param first the first string
     * @param second the second string
     * @return their common prefix, or the empty string when they have none
     * @throws NullPointerException if either argument is null
||||||| merged common ancestors
     * Longest equal UTF-16 prefix that does not split a valid surrogate pair in either
     * source. Other code units, including unpaired surrogates, retain String semantics.
=======
     * Returns the common UTF-16 prefix without splitting a surrogate pair in either input.
     *
     * @param first the first string
     * @param second the second string
     * @return the common prefix, possibly empty
     * @throws NullPointerException if either string is null
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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

<<<<<<< HEAD
    /**
     * Returns the longest equal UTF-16 suffix that does not split a valid
     * surrogate pair in either source. Unpaired surrogates retain String semantics.
     *
     * @param first the first string
     * @param second the second string
     * @return their common suffix, or the empty string when they have none
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Longest equal UTF-16 suffix, without splitting a paired surrogate in either source. */
=======
    /**
     * Returns the common UTF-16 suffix without splitting a surrogate pair in either input.
     *
     * @param first the first string
     * @param second the second string
     * @return the common suffix, possibly empty
     * @throws NullPointerException if either string is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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

<<<<<<< HEAD
    /**
     * Removes a matching prefix, using String's case-sensitive comparison.
     *
     * @param source the string to inspect
     * @param prefix the prefix to remove
     * @return the suffix following a match, otherwise the source itself
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Returns source itself when prefix does not match. */
=======
    /**
     * Removes a matching, case-sensitive prefix.
     *
     * @param source the string to examine
     * @param prefix the prefix to remove
     * @return the remaining string, or the source if the prefix does not match
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String removePrefix(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.startsWith(prefix) ? source.substring(prefix.length()) : source;
    }

<<<<<<< HEAD
    /**
     * Removes a matching suffix, using String's case-sensitive comparison.
     *
     * @param source the string to inspect
     * @param suffix the suffix to remove
     * @return the prefix preceding a match, otherwise the source itself
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Returns source itself when suffix does not match. */
=======
    /**
     * Removes a matching, case-sensitive suffix.
     *
     * @param source the string to examine
     * @param suffix the suffix to remove
     * @return the remaining string, or the source if the suffix does not match
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String removeSuffix(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return source.endsWith(suffix) ? source.substring(0, source.length() - suffix.length())
                : source;
    }

<<<<<<< HEAD
    /**
     * Returns the part before the first delimiter, or the source when absent.
     * An empty delimiter produces the empty string.
     *
     * @param source the string to inspect
     * @param delimiter the case-sensitive delimiter
     * @return the part before the first match, or the source if there is no match
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Returns the part before the first delimiter, or the original text when absent. */
=======
    /**
     * Returns the substring before the first occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the preceding substring, or the source if the delimiter is absent
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String substringBefore(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

<<<<<<< HEAD
    /**
     * Returns the part after the first delimiter, or the empty string when absent.
     * An empty delimiter produces the full source.
     *
     * @param source the string to inspect
     * @param delimiter the case-sensitive delimiter
     * @return the part after the first match, or the empty string if absent
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Returns the part after the first delimiter, or empty text when absent. */
=======
    /**
     * Returns the substring after the first occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the following substring, or an empty string if the delimiter is absent
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String substringAfter(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

<<<<<<< HEAD
    /**
     * Returns the part before the last delimiter, or the source when absent.
     * An empty delimiter produces the full source.
     *
     * @param source the string to inspect
     * @param delimiter the case-sensitive delimiter
     * @return the part before the last match, or the source if absent or empty
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Returns the part before the last delimiter, or original text when absent. */
=======
    /**
     * Returns the substring before the last occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the preceding substring, or the source if the delimiter is empty or absent
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String substringBeforeLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return source;
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

<<<<<<< HEAD
    /**
     * Returns the part after the last delimiter, or the empty string when absent.
     * An empty delimiter produces the empty string.
     *
     * @param source the string to inspect
     * @param delimiter the case-sensitive delimiter
     * @return the part after the last match, or the empty string if absent or empty
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Returns the part after the last delimiter, or empty text when absent. */
=======
    /**
     * Returns the substring after the last occurrence of the delimiter.
     *
     * @param source the string to search
     * @param delimiter the literal delimiter
     * @return the following substring, or an empty string if the delimiter is empty or absent
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String substringAfterLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return "";
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

<<<<<<< HEAD
    /**
     * Counts non-overlapping, case-sensitive occurrences of the needle.
     * An empty needle has zero occurrences for this operation.
     *
     * @param source the string to search
     * @param needle the text to count
     * @return the number of non-overlapping matches
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** Non-overlapping, case-sensitive occurrences; an empty needle yields zero. */
=======
    /**
     * Counts nonoverlapping, case-sensitive occurrences of the needle.
     *
     * @param source the string to search
     * @param needle the literal string to count
     * @return the number of occurrences, or zero for an empty needle
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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

<<<<<<< HEAD
    /**
     * Tests the prefix using {@link String#regionMatches(boolean,int,String,int,int)}
     * with case ignored, without locale-sensitive case rewriting.
     *
     * @param source the string to inspect
     * @param prefix the prefix to test
     * @return true if the prefix matches while ignoring case
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** JDK regionMatches(ignoreCase) semantics; no locale-sensitive case rewriting. */
=======
    /**
     * Tests a prefix using the case-insensitive rules of {@link String#regionMatches}.
     *
     * @param source the string to examine
     * @param prefix the prefix to test
     * @return true if the prefix matches without regard to case
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static boolean startsWithIgnoreCase(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

<<<<<<< HEAD
    /**
     * Tests the suffix using {@link String#regionMatches(boolean,int,String,int,int)}
     * with case ignored, without locale-sensitive case rewriting.
     *
     * @param source the string to inspect
     * @param suffix the suffix to test
     * @return true if the suffix matches while ignoring case
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
    /** JDK regionMatches(ignoreCase) semantics; no locale-sensitive case rewriting. */
=======
    /**
     * Tests a suffix using the case-insensitive rules of {@link String#regionMatches}.
     *
     * @param source the string to examine
     * @param suffix the suffix to test
     * @return true if the suffix matches without regard to case
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static boolean endsWithIgnoreCase(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return suffix.length() <= source.length()
                && source.regionMatches(true, source.length() - suffix.length(),
                        suffix, 0, suffix.length());
    }

<<<<<<< HEAD
    /**
     * Finds the first match from the requested UTF-16 index using String's
     * region comparison with case ignored. Negative starting indices become zero.
     * An empty needle matches at the starting index clamped to the source length.
     *
     * @param source the string to search
     * @param needle the text to find
     * @param fromIndex the first UTF-16 index to inspect
     * @return the first matching index, or -1 if no match exists
     * @throws NullPointerException if either string is null
     */
||||||| merged common ancestors
    /** An empty needle occurs at the clamped starting position. */
=======
    /**
     * Finds the first case-insensitive occurrence at or after the starting index.
     * The starting index is raised to zero if negative. An empty needle matches
     * at the starting index clamped to the source length.
     *
     * @param source the string to search
     * @param needle the literal string to find
     * @param fromIndex the starting UTF-16 index
     * @return the first matching index, or -1 if no occurrence exists
     * @throws NullPointerException if either string is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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

<<<<<<< HEAD
    /**
     * Finds the first match with case ignored, starting at index zero.
     *
     * @param source the string to search
     * @param needle the text to find
     * @return the first matching UTF-16 index, or -1 if no match exists
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
=======
    /**
     * Finds the first case-insensitive occurrence, starting at index zero.
     *
     * @param source the string to search
     * @param needle the literal string to find
     * @return the first matching index, or -1 if no occurrence exists
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static int indexOfIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle, 0);
    }

<<<<<<< HEAD
    /**
     * Tests whether the source contains a match with case ignored.
     * An empty needle is contained in every non-null source.
     *
     * @param source the string to search
     * @param needle the text to find
     * @return true if a match exists
     * @throws NullPointerException if either argument is null
     */
||||||| merged common ancestors
=======
    /**
     * Tests whether the source contains the needle without regard to case.
     *
     * @param source the string to search
     * @param needle the literal string to find
     * @return true if a case-insensitive occurrence exists
     * @throws NullPointerException if either argument is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static boolean containsIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle) >= 0;
    }

<<<<<<< HEAD
    /**
     * Reverses the source through {@link StringBuilder#reverse()}, preserving
     * valid UTF-16 surrogate pairs according to that operation's contract.
     *
     * @param source the string to reverse
     * @return the reversed string, or the source itself when shorter than two units
     * @throws NullPointerException if the source is null
     */
||||||| merged common ancestors
    /** Preserve paired UTF-16 surrogate units, as StringBuilder.reverse() does. */
=======
    /**
     * Reverses the source with the surrogate-pair handling of {@link StringBuilder#reverse()}.
     *
     * @param source the string to reverse
     * @return the reversed string
     * @throws NullPointerException if source is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String reverse(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() < 2) return source;
        return new StringBuilder(source).reverse().toString();
    }

<<<<<<< HEAD
    /**
     * Strips surrounding Unicode whitespace using {@link String#strip()}.
     *
     * @param source the string to strip, or null
     * @return null for a null source or empty stripped result, otherwise the result
     */
||||||| merged common ancestors
    /** Reuse java.lang.String.strip, including its Unicode whitespace semantics. */
=======
    /**
     * Strips Unicode whitespace from both ends and substitutes null for an empty result.
     *
     * @param source the string, which may be null
     * @return the stripped nonempty string, or null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String stripToNull(String source) {
        if (source == null) return null;
        String trimmed = source.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

<<<<<<< HEAD
    /**
     * Strips surrounding Unicode whitespace using {@link String#strip()}.
     *
     * @param source the string to strip, or null
     * @return the empty string for null, otherwise the stripped result
     */
||||||| merged common ancestors
    /** Null becomes empty; strip results reuse the underlying JDK String route. */
=======
    /**
     * Strips Unicode whitespace from both ends and substitutes an empty string for null.
     *
     * @param source the string, which may be null
     * @return the stripped string, or an empty string if source is null
     */
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static String stripToEmpty(String source) {
        return source == null ? "" : source.strip();
    }

    private static boolean surrogatePairAt(String source, int index) {
        return index >= 0 && index + 1 < source.length()
                && Character.isHighSurrogate(source.charAt(index))
                && Character.isLowSurrogate(source.charAt(index + 1));
    }
}
