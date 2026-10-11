/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Fixed-size M3JDK receiver for Synexia's immutable code-text precompute snapshot.
 *
 * <p>This is candidate-routing metadata only. It never proves Java syntax, regex validity,
 * semantic equivalence, or recipe correctness. Exact Java and regex operations remain the
 * authority. The receiver owns no source spelling beyond the bounded primitive signature.</p>
 */
final class M3CodeTextFacts {
    static final int HAS_BRACE = 1;
    static final int HAS_PAREN = 1 << 1;
    static final int HAS_SEMICOLON = 1 << 2;
    static final int HAS_BACKSLASH = 1 << 3;
    static final int HAS_REGEX_META = 1 << 4;
    static final int HAS_ARROW = 1 << 5;
    static final int HAS_METHOD_REFERENCE = 1 << 6;
    static final int HAS_ASSIGN = 1 << 7;
    static final int HAS_COMPARISON = 1 << 8;
    static final int HAS_QUANTIFIER = 1 << 9;
    static final int HAS_CLASS_BRACKET = 1 << 10;
    static final int HAS_DOT = 1 << 11;
    static final int HAS_COMMA = 1 << 12;
    static final int HAS_COLON = 1 << 13;
    static final int HAS_AMP_PIPE = 1 << 14;
    static final int HAS_WHITESPACE = 1 << 15;

    private static final long HASH_BASE = 1_000_003L;
    private static final int MIN_HASH_LANES = 16;
    private static final String[] JAVA_WORDS = {
        "class", "interface", "enum", "record", "if", "else", "for", "while", "do",
        "switch", "case", "try", "catch", "finally", "throw", "throws", "return", "new",
        "public", "private", "protected", "static", "final", "synchronized", "native",
        "void", "int", "long", "boolean", "double", "float", "char", "byte", "short"
    };

    final int utf16Length;
    final int codePointCount;
    final int textFlags;
    final long contentHash64;
    final long presence64;
    final long simHash64;
    final long lexicalPacked;
    final int javaKeywordHits;
    final int codeScore;
    final int regexScore;
    private final long[] minHash;

    private M3CodeTextFacts(
            int utf16Length,
            int codePointCount,
            int textFlags,
            long contentHash64,
            long presence64,
            long simHash64,
            long lexicalPacked,
            int javaKeywordHits,
            int codeScore,
            int regexScore,
            long[] minHash) {
        this.utf16Length = utf16Length;
        this.codePointCount = codePointCount;
        this.textFlags = textFlags;
        this.contentHash64 = contentHash64;
        this.presence64 = presence64;
        this.simHash64 = simHash64;
        this.lexicalPacked = lexicalPacked;
        this.javaKeywordHits = javaKeywordHits;
        this.codeScore = codeScore;
        this.regexScore = regexScore;
        this.minHash = minHash.clone();
    }

    static M3CodeTextFacts fromPrecomputed(
            int utf16Length,
            int codePointCount,
            int textFlags,
            long contentHash64,
            long presence64,
            long simHash64,
            long lexicalPacked,
            int javaKeywordHits,
            int codeScore,
            int regexScore,
            long[] minHash) {
        Objects.requireNonNull(minHash, "minHash");
        if (utf16Length < 0
                || codePointCount < 0
                || codePointCount > utf16Length
                || javaKeywordHits < 0
                || codeScore < 0
                || regexScore < 0
                || minHash.length != MIN_HASH_LANES) {
            throw new IllegalArgumentException("invalid precomputed code-text facts");
        }
        return new M3CodeTextFacts(
                utf16Length,
                codePointCount,
                textFlags,
                contentHash64,
                presence64,
                simHash64,
                lexicalPacked,
                javaKeywordHits,
                codeScore,
                regexScore,
                minHash);
    }

    static M3CodeTextFacts scan(CharSequence source) {
        Objects.requireNonNull(source, "source");
        char[] units = new char[source.length()];
        for (int index = 0; index < units.length; index++) {
            units[index] = source.charAt(index);
        }
        if (source.length() != units.length) {
            throw new IllegalArgumentException("code-text length changed during snapshot");
        }
        return scan(units);
    }

    private static M3CodeTextFacts scan(char[] units) {
        int length = units.length;
        long[] characterKeys = new long[length];
        long[] bigramKeys = new long[Math.max(0, length - 1)];
        long contentHash = 0L;
        long presence = 0L;
        for (int index = 0; index < length; index++) {
            char unit = units[index];
            characterKeys[index] = unit;
            contentHash = contentHash * HASH_BASE + unit;
            presence = addSignal(presence, unit);
            if (index > 0) {
                bigramKeys[index - 1] = ((long) units[index - 1] << 16) | unit;
            }
        }

        int codePoints = 0;
        int letters = 0;
        int digits = 0;
        int whitespace = 0;
        int uppercase = 0;
        int lowercase = 0;
        boolean wellFormed = true;
        for (int index = 0; index < length; ) {
            char unit = units[index];
            int codePoint = unit;
            int width = 1;
            if (Character.isHighSurrogate(unit)
                    && index + 1 < length
                    && Character.isLowSurrogate(units[index + 1])) {
                codePoint = Character.toCodePoint(unit, units[index + 1]);
                width = 2;
            } else if (Character.isSurrogate(unit)) {
                wellFormed = false;
            }
            codePoints++;
            if (Character.isLetter(codePoint)) letters++;
            if (Character.isDigit(codePoint)) digits++;
            if (Character.isWhitespace(codePoint)) whitespace++;
            if (Character.isUpperCase(codePoint)) uppercase++;
            if (Character.isLowerCase(codePoint)) lowercase++;
            index += width;
        }

        int maximum = 0;
        for (char unit : units) {
            maximum = Math.max(maximum, unit);
        }
        int flags = 0;
        if (maximum < 128) flags |= M3TextFlags.ASCII;
        if (maximum < 256) flags |= M3TextFlags.LATIN1;
        if (whitespace == codePoints) flags |= M3TextFlags.BLANK;
        if (letters == codePoints) flags |= M3TextFlags.ALL_LETTERS;
        if (digits == codePoints) flags |= M3TextFlags.ALL_DIGITS;
        if (letters > 0) flags |= M3TextFlags.HAS_LETTER;
        if (digits > 0) flags |= M3TextFlags.HAS_DIGIT;
        if (uppercase > 0) flags |= M3TextFlags.HAS_UPPER;
        if (lowercase > 0) flags |= M3TextFlags.HAS_LOWER;
        if (wellFormed) flags |= M3TextFlags.WELL_FORMED_UTF16;

        Histogram characters = Histogram.of(characterKeys);
        Histogram bigrams = Histogram.of(bigramKeys);
        Histogram features = bigrams.keys.length == 0 ? characters : bigrams;
        long simHash = simHash(features, bigrams.keys.length != 0);
        long[] minHash = minHash(features, bigrams.keys.length != 0);
        long lexical = lexicalPacked(units);
        int keywords = keywordHits(units);
        int codeScore = saturate(
                keywords * 4L
                        + lane(lexical, 16) * 4L
                        + lane(lexical, 24) * 3L
                        + lane(lexical, 32)
                        + ((lexical & HAS_ARROW) != 0 ? 5 : 0)
                        + ((lexical & HAS_METHOD_REFERENCE) != 0 ? 5 : 0)
                        + ((lexical & HAS_ASSIGN) != 0 ? 2 : 0)
                        + ((lexical & HAS_COMPARISON) != 0 ? 2 : 0));
        int regexScore = saturate(
                lane(lexical, 48) * 2L
                        + lane(lexical, 40) * 2L
                        + ((lexical & HAS_CLASS_BRACKET) != 0 ? 4 : 0)
                        + ((lexical & HAS_QUANTIFIER) != 0 ? 4 : 0)
                        + ((lexical & HAS_REGEX_META) != 0 ? 2 : 0));

        return new M3CodeTextFacts(
                length, codePoints, flags, contentHash, presence, simHash, lexical,
                keywords, codeScore, regexScore, minHash);
    }

    long[] minHash() {
        return minHash.clone();
    }

    int lexicalFlags() {
        return (int) lexicalPacked & 0xffff;
    }

    int semicolonCount() {
        return lane(lexicalPacked, 16);
    }

    int braceCount() {
        return lane(lexicalPacked, 24);
    }

    int parenCount() {
        return lane(lexicalPacked, 32);
    }

    int backslashCount() {
        return lane(lexicalPacked, 40);
    }

    int regexMetaCount() {
        return lane(lexicalPacked, 48);
    }

    int punctuationCount() {
        return lane(lexicalPacked, 56);
    }

    boolean likelyCode() {
        return codeScore >= 8;
    }

    boolean likelyRegex() {
        return regexScore >= 8;
    }

    int simHashDistance(M3CodeTextFacts other) {
        return Long.bitCount(simHash64 ^ Objects.requireNonNull(other, "other").simHash64);
    }

    double estimatedJaccard(M3CodeTextFacts other) {
        M3CodeTextFacts checked = Objects.requireNonNull(other, "other");
        if (minHash.length != checked.minHash.length || minHash.length == 0) {
            throw new IllegalArgumentException("incompatible MinHash geometry");
        }
        int equal = 0;
        for (int lane = 0; lane < minHash.length; lane++) {
            if (minHash[lane] == checked.minHash[lane]) equal++;
        }
        return (double) equal / minHash.length;
    }

    private static long lexicalPacked(char[] units) {
        int flags = 0;
        int semicolons = 0;
        int braces = 0;
        int parens = 0;
        int backslashes = 0;
        int regexMeta = 0;
        int punctuation = 0;
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
            switch (unit) {
                case '{', '}' -> {
                    flags |= HAS_BRACE;
                    braces++;
                    regexMeta++;
                    punctuation++;
                }
                case '(', ')' -> {
                    flags |= HAS_PAREN;
                    parens++;
                    regexMeta++;
                    punctuation++;
                }
                case ';' -> {
                    flags |= HAS_SEMICOLON;
                    semicolons++;
                    punctuation++;
                }
                case '\\' -> {
                    flags |= HAS_BACKSLASH;
                    backslashes++;
                    punctuation++;
                }
                case '.', '*', '+', '?', '|', '^', '$' -> {
                    flags |= HAS_REGEX_META;
                    regexMeta++;
                    punctuation++;
                    if (unit == '*' || unit == '+' || unit == '?') flags |= HAS_QUANTIFIER;
                    if (unit == '.') flags |= HAS_DOT;
                    if (unit == '|') flags |= HAS_AMP_PIPE;
                }
                case '[', ']' -> {
                    flags |= HAS_REGEX_META | HAS_CLASS_BRACKET;
                    regexMeta++;
                    punctuation++;
                }
                case '=' -> {
                    flags |= HAS_ASSIGN;
                    punctuation++;
                }
                case '<', '>', '!' -> {
                    flags |= HAS_COMPARISON;
                    punctuation++;
                }
                case ',' -> {
                    flags |= HAS_COMMA;
                    punctuation++;
                }
                case ':' -> {
                    flags |= HAS_COLON;
                    punctuation++;
                    if (index + 1 < units.length && units[index + 1] == ':') {
                        flags |= HAS_METHOD_REFERENCE;
                    }
                }
                case '&' -> {
                    flags |= HAS_AMP_PIPE;
                    punctuation++;
                }
                case '-' -> {
                    punctuation++;
                    if (index + 1 < units.length && units[index + 1] == '>') {
                        flags |= HAS_ARROW;
                    }
                }
                default -> {
                    if (Character.isWhitespace(unit)) flags |= HAS_WHITESPACE;
                }
            }
        }
        return Integer.toUnsignedLong(flags)
                | ((long) Math.min(semicolons, 255) << 16)
                | ((long) Math.min(braces, 255) << 24)
                | ((long) Math.min(parens, 255) << 32)
                | ((long) Math.min(backslashes, 255) << 40)
                | ((long) Math.min(regexMeta, 255) << 48)
                | ((long) Math.min(punctuation, 255) << 56);
    }

    private static int keywordHits(char[] units) {
        int hits = 0;
        StringBuilder token = new StringBuilder();
        for (int index = 0; index <= units.length; index++) {
            char unit = index == units.length ? '\0' : units[index];
            if (index < units.length && Character.isJavaIdentifierPart(unit)) {
                token.append(unit);
                continue;
            }
            if (token.length() != 0) {
                String word = token.toString().toLowerCase(Locale.ROOT);
                for (String keyword : JAVA_WORDS) {
                    if (keyword.equals(word)) {
                        hits++;
                        break;
                    }
                }
                token.setLength(0);
            }
        }
        return hits;
    }

    private static long addSignal(long signal, char unit) {
        int folded = unit >= 'A' && unit <= 'Z' ? unit | 0x20 : unit;
        int first = mix32(folded);
        int second = mix32(folded ^ 0x9e3779b9);
        return signal | (1L << (first & 63)) | (1L << (second & 63));
    }

    private static long simHash(Histogram features, boolean bigramFeatures) {
        long result = 0L;
        for (int bit = 0; bit < Long.SIZE; bit++) {
            long vote = 0L;
            for (int index = 0; index < features.keys.length; index++) {
                long key = features.keys[index];
                long hash = mix(key | (bigramFeatures ? 1L << 32 : 0L), 0x53494dL);
                vote += ((hash >>> bit) & 1L) == 0L
                        ? -(long) features.counts[index]
                        : features.counts[index];
            }
            if (vote > 0L) result |= 1L << bit;
        }
        return result;
    }

    private static long[] minHash(Histogram features, boolean bigramFeatures) {
        long[] result = new long[MIN_HASH_LANES];
        java.util.Arrays.fill(result, -1L);
        for (int lane = 0; lane < result.length; lane++) {
            for (int index = 0; index < features.keys.length; index++) {
                long key = features.keys[index];
                long hash = mix(key | (bigramFeatures ? 1L << 32 : 0L), lane);
                if (Long.compareUnsigned(hash, result[lane]) < 0) {
                    result[lane] = hash;
                }
            }
        }
        return result;
    }

    private static long mix(long left, long right) {
        long z = left ^ Long.rotateLeft(right, 29) ^ 0x9e3779b97f4a7c15L;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int mix32(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }

    private static int lane(long value, int shift) {
        return (int) ((value >>> shift) & 0xffL);
    }

    private static int saturate(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    private static final class Histogram {
        final long[] keys;
        final int[] counts;

        private Histogram(long[] keys, int[] counts) {
            this.keys = keys;
            this.counts = counts;
        }

        static Histogram of(long[] values) {
            Map<Long, Integer> counts = new HashMap<>();
            for (long value : values) {
                counts.merge(value, 1, Integer::sum);
            }
            TreeMap<Long, Integer> sorted = new TreeMap<>(counts);
            long[] keys = new long[sorted.size()];
            int[] valuesByKey = new int[keys.length];
            int index = 0;
            for (Map.Entry<Long, Integer> entry : sorted.entrySet()) {
                keys[index] = entry.getKey();
                valuesByKey[index] = entry.getValue();
                index++;
            }
            return new Histogram(keys, valuesByKey);
        }
    }

    private static final class M3TextFlags {
        static final int ASCII = 1;
        static final int LATIN1 = 1 << 1;
        static final int BLANK = 1 << 2;
        static final int ALL_LETTERS = 1 << 3;
        static final int ALL_DIGITS = 1 << 4;
        static final int HAS_LETTER = 1 << 5;
        static final int HAS_DIGIT = 1 << 6;
        static final int HAS_UPPER = 1 << 7;
        static final int HAS_LOWER = 1 << 8;
        static final int WELL_FORMED_UTF16 = 1 << 9;

        private M3TextFlags() {}
    }
}
