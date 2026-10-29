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
     * Returns whether the sequence is null or has no UTF-16 code units.
     *
     * @param source the sequence to test, which may be null
     * @return true if source is null or empty
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Normalizes a null reference to the canonical empty String.
     *
     * @param source the String to normalize, which may be null
     * @return the empty String if source is null; otherwise source itself
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Converts an empty String to null, leaving nonempty identity untouched.
     *
     * @param source the String to normalize, which may be null
     * @return null if source is null or empty; otherwise source itself
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns whether the sequence is null, empty, or consists entirely of
     * Unicode code points classified as whitespace by
     * {@link Character#isWhitespace(int)}, the rule used by {@link String#isBlank()}.
     *
     * @param source the sequence to test, which may be null
     * @return true if source is null, empty, or entirely whitespace
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns whether the sequence contains at least one non-whitespace code point.
     * Whitespace is determined by {@link Character#isWhitespace(int)}.
     *
     * @param source the sequence to test, which may be null
     * @return true if source is non-null and contains a non-whitespace code point
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns fallback only for null or empty text.
     *
     * @param source the String to test, which may be null
     * @param fallback the replacement, which may be null
     * @return fallback if source is null or empty; otherwise source itself
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns fallback only for null, empty, or entirely whitespace text.
     * Whitespace is determined by {@link Character#isWhitespace(int)}.
     *
     * @param source the String to test, which may be null
     * @param fallback the replacement, which may be null
     * @return fallback if source is null or blank; otherwise source itself
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Delegates repetition to {@link String#repeat(int)}.
     *
     * @param source the non-null String to repeat
     * @param copies the number of repetitions
     * @return the String produced by repeating source copies times
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if copies is negative
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns the longest equal UTF-16 prefix that does not split a valid
     * surrogate pair in either source. Other code units, including unpaired
     * surrogates, retain String semantics.
     *
     * @param first the first non-null String
     * @param second the second non-null String
     * @return the common prefix, or the empty String if none exists
     * @throws NullPointerException if either String is null
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns the longest equal UTF-16 suffix that does not split a valid
     * surrogate pair in either source. Unpaired surrogates are compared as code units.
     *
     * @param first the first non-null String
     * @param second the second non-null String
     * @return the common suffix, or the empty String if none exists
     * @throws NullPointerException if either String is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Removes one matching, case-sensitive prefix.
     *
     * @param source the non-null String to examine
     * @param prefix the non-null prefix to remove, which may be empty
     * @return source without the prefix if matched; otherwise source itself
     * @throws NullPointerException if source or prefix is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Removes one matching, case-sensitive suffix.
     *
     * @param source the non-null String to examine
     * @param suffix the non-null suffix to remove, which may be empty
     * @return source without the suffix if matched; otherwise source itself
     * @throws NullPointerException if source or suffix is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns the part before the first case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter; an empty delimiter matches at index zero
     * @return the text before the match, or source itself if absent
     * @throws NullPointerException if source or delimiter is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns the part after the first case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter; an empty delimiter matches at index zero
     * @return the text after the match, or the empty String if absent
     * @throws NullPointerException if source or delimiter is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns the part before the last case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter to find
     * @return source itself if delimiter is empty or absent; otherwise the text before it
     * @throws NullPointerException if source or delimiter is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Returns the part after the last case-sensitive delimiter match.
     *
     * @param source the non-null String to examine
     * @param delimiter the non-null delimiter to find
     * @return the empty String if delimiter is empty or absent; otherwise the text after it
     * @throws NullPointerException if source or delimiter is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Counts non-overlapping, case-sensitive occurrences.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @return the number of non-overlapping matches, or zero if needle is empty
     * @throws NullPointerException if source or needle is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Tests the prefix using {@link String#regionMatches(boolean, int, String, int, int)}
     * with case ignored, without locale-sensitive case rewriting.
     *
     * @param source the non-null String to examine
     * @param prefix the non-null prefix to test, which may be empty
     * @return true if the prefix matches, ignoring case
     * @throws NullPointerException if source or prefix is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Tests the suffix using {@link String#regionMatches(boolean, int, String, int, int)}
     * with case ignored, without locale-sensitive case rewriting.
     *
     * @param source the non-null String to examine
     * @param suffix the non-null suffix to test, which may be empty
     * @return true if the suffix matches, ignoring case
     * @throws NullPointerException if source or suffix is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Finds the first match, starting at index zero, using
     * {@link String#regionMatches(boolean, int, String, int, int)} with case ignored.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @return the first matching UTF-16 index, zero for an empty needle, or -1 if absent
     * @throws NullPointerException if source or needle is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Tests whether the source contains the needle using
     * {@link String#regionMatches(boolean, int, String, int, int)} with case ignored.
     *
     * @param source the non-null String to examine
     * @param needle the non-null String to find
     * @return true if a match exists, including when needle is empty
     * @throws NullPointerException if source or needle is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Reverses the text using {@link StringBuilder#reverse()}, including its
     * surrogate-pair handling. Reversing unpaired surrogates may form new pairs.
     *
     * @param source the non-null String to reverse
     * @return source itself if shorter than two UTF-16 units; otherwise the reversed text
     * @throws NullPointerException if source is null
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Strips leading and trailing whitespace using {@link String#strip()}.
     *
     * @param source the String to strip, which may be null
     * @return null if source is null or the stripped result is empty; otherwise that result
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
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
     * Strips leading and trailing whitespace using {@link String#strip()},
     * normalizing a null reference to the empty String.
     *
     * @param source the String to strip, which may be null
     * @return the empty String if source is null; otherwise the stripped result
     */
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
    public static String stripToEmpty(String source) {
        return source == null ? "" : source.strip();
    }

    private static boolean surrogatePairAt(String source, int index) {
        return index >= 0 && index + 1 < source.length()
                && Character.isHighSurrogate(source.charAt(index))
                && Character.isLowSurrogate(source.charAt(index + 1));
    }
}
