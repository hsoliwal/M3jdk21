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

    /** True when the supplied sequence is null or has no UTF-16 units. */
    public static boolean isNullOrEmpty(CharSequence source) {
        return source == null || source.length() == 0;
    }

    /** Normalizes a null reference to the canonical empty String. */
    public static String nullToEmpty(String source) {
        return source == null ? "" : source;
    }

    /** Converts an empty String to null, leaving nonempty identity untouched. */
    public static String emptyToNull(String source) {
        return source == null || source.isEmpty() ? null : source;
    }

    /**
     * True for null, empty text, or text whose every Unicode code point is whitespace
     * according to {@link Character#isWhitespace(int)} (also used by String.isBlank).
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

    /** True for non-null text containing at least one non-whitespace code point. */
    public static boolean hasText(CharSequence source) {
        return !isNullOrBlank(source);
    }

    /** Returns fallback only for null/empty text. The fallback itself may be null. */
    public static String defaultIfEmpty(String source, String fallback) {
        return source == null || source.isEmpty() ? fallback : source;
    }

    /** Returns fallback only for null/blank text. The fallback itself may be null. */
    public static String defaultIfBlank(String source, String fallback) {
        return isNullOrBlank(source) ? fallback : source;
    }

    /** Delegates repetition to String's canonical runtime owner. */
    public static String repeat(String source, int copies) {
        return Objects.requireNonNull(source, "source").repeat(copies);
    }

    /** Pads to at least minLength UTF-16 units; a larger source is returned unchanged. */
    public static String padStart(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return String.valueOf(pad).repeat(minLength - source.length()).concat(source);
    }

    /** Pads to at least minLength UTF-16 units; a larger source is returned unchanged. */
    public static String padEnd(String source, int minLength, char pad) {
        Objects.requireNonNull(source, "source");
        if (minLength < 0) throw new IllegalArgumentException("negative minLength");
        if (source.length() >= minLength) return source;
        return source.concat(String.valueOf(pad).repeat(minLength - source.length()));
    }

    /**
     * Longest equal UTF-16 prefix that does not split a valid surrogate pair in either
     * source. Other code units, including unpaired surrogates, retain String semantics.
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

    /** Longest equal UTF-16 suffix, without splitting a paired surrogate in either source. */
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

    /** Returns source itself when prefix does not match. */
    public static String removePrefix(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.startsWith(prefix) ? source.substring(prefix.length()) : source;
    }

    /** Returns source itself when suffix does not match. */
    public static String removeSuffix(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return source.endsWith(suffix) ? source.substring(0, source.length() - suffix.length())
                : source;
    }

    /** Returns the part before the first delimiter, or the original text when absent. */
    public static String substringBefore(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

    /** Returns the part after the first delimiter, or empty text when absent. */
    public static String substringAfter(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        int at = source.indexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

    /** Returns the part before the last delimiter, or original text when absent. */
    public static String substringBeforeLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return source;
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? source : source.substring(0, at);
    }

    /** Returns the part after the last delimiter, or empty text when absent. */
    public static String substringAfterLast(String source, String delimiter) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.isEmpty()) return "";
        int at = source.lastIndexOf(delimiter);
        return at < 0 ? "" : source.substring(at + delimiter.length());
    }

    /** Non-overlapping, case-sensitive occurrences; an empty needle yields zero. */
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

    /** JDK regionMatches(ignoreCase) semantics; no locale-sensitive case rewriting. */
    public static boolean startsWithIgnoreCase(String source, String prefix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(prefix, "prefix");
        return source.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /** JDK regionMatches(ignoreCase) semantics; no locale-sensitive case rewriting. */
    public static boolean endsWithIgnoreCase(String source, String suffix) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(suffix, "suffix");
        return suffix.length() <= source.length()
                && source.regionMatches(true, source.length() - suffix.length(),
                        suffix, 0, suffix.length());
    }

    /** An empty needle occurs at the clamped starting position. */
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

    public static int indexOfIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle, 0);
    }

    public static boolean containsIgnoreCase(String source, String needle) {
        return indexOfIgnoreCase(source, needle) >= 0;
    }

    /** Preserve paired UTF-16 surrogate units, as StringBuilder.reverse() does. */
    public static String reverse(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() < 2) return source;
        return new StringBuilder(source).reverse().toString();
    }

    /** Reuse java.lang.String.strip, including its Unicode whitespace semantics. */
    public static String stripToNull(String source) {
        if (source == null) return null;
        String trimmed = source.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Null becomes empty; strip results reuse the underlying JDK String route. */
    public static String stripToEmpty(String source) {
        return source == null ? "" : source.strip();
    }

    private static boolean surrogatePairAt(String source, int index) {
        return index >= 0 && index + 1 < source.length()
                && Character.isHighSurrogate(source.charAt(index))
                && Character.isLowSurrogate(source.charAt(index + 1));
    }
}
