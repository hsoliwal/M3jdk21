/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

/**
 * Internal, fixed-size precompute facts for one canonical M3 String owner/range.
 *
 * <p>This object never owns text, byte/char arrays, token arrays, native buffers, or
 * length-proportional search state. It is an implementation detail, not a Java API.</p>
 *
 * <p>The fixed record carries the mature Synexia donor fact bundle adapted to M3JDK:
 * UTF-16/code-point geometry, Java/hash geometry, character and n-gram candidate filters,
 * packed prefix/suffix units, ASCII case hashes, and trim/strip boundaries. Candidate facts may
 * reject impossible work, but exact M3 String comparison remains semantic authority.</p>
 */
final class M3StringFacts {
    final int utf16Length;
    final int codePointCount;
    final int unpairedSurrogateCount;
    final int javaHash;
    final int hash31Power;
    final char firstUtf16Unit;
    final char lastUtf16Unit;
    final long bitSignal64;

    final int asciiUpperHash;
    final int asciiLowerHash;
    final int asciiTitleHash;
    /** First four UTF-16 units, left aligned. */
    final long prefix4;
    /** Last four UTF-16 units, right aligned. */
    final long suffix4;
    final long bigramSignal64;
    final long trigramSignal64;

    /** First code-unit index not removed by legacy String.trim(). */
    final int trimStart;
    /** End-exclusive code-unit index not removed by legacy String.trim(). */
    final int trimEnd;
    /** First code-unit index whose code point is not Character.isWhitespace. */
    final int stripStart;
    /** End-exclusive code-unit index whose code point is not Character.isWhitespace. */
    final int stripEnd;

    private M3StringFacts(
            int utf16Length,
            int codePointCount,
            int unpairedSurrogateCount,
            int javaHash,
            int hash31Power,
            char firstUtf16Unit,
            char lastUtf16Unit,
            long bitSignal64,
            int asciiUpperHash,
            int asciiLowerHash,
            int asciiTitleHash,
            long prefix4,
            long suffix4,
            long bigramSignal64,
            long trigramSignal64,
            int trimStart,
            int trimEnd,
            int stripStart,
            int stripEnd) {
        this.utf16Length = utf16Length;
        this.codePointCount = codePointCount;
        this.unpairedSurrogateCount = unpairedSurrogateCount;
        this.javaHash = javaHash;
        this.hash31Power = hash31Power;
        this.firstUtf16Unit = firstUtf16Unit;
        this.lastUtf16Unit = lastUtf16Unit;
        this.bitSignal64 = bitSignal64;
        this.asciiUpperHash = asciiUpperHash;
        this.asciiLowerHash = asciiLowerHash;
        this.asciiTitleHash = asciiTitleHash;
        this.prefix4 = prefix4;
        this.suffix4 = suffix4;
        this.bigramSignal64 = bigramSignal64;
        this.trigramSignal64 = trigramSignal64;
        this.trimStart = trimStart;
        this.trimEnd = trimEnd;
        this.stripStart = stripStart;
        this.stripEnd = stripEnd;
    }

    static M3StringFacts scan(M3String value) {
        int length = value.length();
        int hash = 0;
        int codePoints = 0;
        int unpaired = 0;
        long signal = 0L;
        long prefix = 0L;
        long suffix = 0L;
        long bigrams = 0L;
        long trigrams = 0L;
        int upperHash = 0;
        int lowerHash = 0;
        int titleHash = 0;
        char first = 0;
        char last = 0;
        char previous2 = 0;
        char previous1 = 0;

        for (int index = 0; index < length; index++) {
            char unit = value.charAt(index);
            if (index == 0) first = unit;
            last = unit;
            hash = 31 * hash + unit;
            signal = addSignal(signal, unit);
            upperHash = 31 * upperHash + asciiUpper(unit);
            lowerHash = 31 * lowerHash + asciiLower(unit);
            titleHash = 31 * titleHash + (index == 0 ? asciiUpper(unit) : asciiLower(unit));
            if (index < 4) prefix |= (long) unit << (48 - (index << 4));
            suffix = (suffix << 16) | unit;
            if (index >= 1) bigrams = addBigramSignal(bigrams, previous1, unit);
            if (index >= 2) trigrams = addTrigramSignal(trigrams, previous2, previous1, unit);
            previous2 = previous1;
            previous1 = unit;
        }

        int stripStart = 0;
        int stripEnd = 0;
        boolean leadingStrip = true;
        int index = 0;
        while (index < length) {
            int codePointStart = index;
            char unit = value.charAt(index);
            int codePoint = unit;
            int width = 1;
            if (Character.isHighSurrogate(unit)
                    && index + 1 < length
                    && Character.isLowSurrogate(value.charAt(index + 1))) {
                codePoint = Character.toCodePoint(unit, value.charAt(index + 1));
                width = 2;
            } else if (Character.isSurrogate(unit)) {
                unpaired++;
            }
            codePoints++;

            boolean whitespace = Character.isWhitespace(codePoint);
            if (leadingStrip) {
                if (whitespace) stripStart = codePointStart + width;
                else leadingStrip = false;
            }
            if (!whitespace) stripEnd = codePointStart + width;
            index += width;
        }

        int trimStart = 0;
        while (trimStart < length && value.charAt(trimStart) <= ' ') trimStart++;
        int trimEnd = length;
        while (trimEnd > trimStart && value.charAt(trimEnd - 1) <= ' ') trimEnd--;

        if (length == 0) {
            stripStart = stripEnd = trimStart = trimEnd = 0;
        } else {
            if (leadingStrip) stripEnd = 0;
            if (trimStart == length) trimEnd = 0;
        }

        return new M3StringFacts(
                length,
                codePoints,
                unpaired,
                hash,
                pow31(length),
                first,
                last,
                signal,
                upperHash,
                lowerHash,
                titleHash,
                prefix,
                suffix,
                bigrams,
                trigrams,
                trimStart,
                trimEnd,
                stripStart,
                stripEnd);
    }

    static M3StringFacts compose(M3StringFacts left, M3StringFacts right) {
        if (left.utf16Length == 0) return right;
        if (right.utf16Length == 0) return left;

        boolean seamPair =
                Character.isHighSurrogate(left.lastUtf16Unit)
                        && Character.isLowSurrogate(right.firstUtf16Unit);
        int length = Math.addExact(left.utf16Length, right.utf16Length);

        int trimStart =
                left.trimStart == left.utf16Length
                        ? Math.addExact(left.utf16Length, right.trimStart)
                        : left.trimStart;
        int trimEnd =
                right.trimEnd == 0
                        ? left.trimEnd
                        : Math.addExact(left.utf16Length, right.trimEnd);

        int stripStart =
                left.stripStart == left.utf16Length
                        ? Math.addExact(left.utf16Length, right.stripStart)
                        : left.stripStart;
        int stripEnd =
                right.stripEnd == 0
                        ? left.stripEnd
                        : Math.addExact(left.utf16Length, right.stripEnd);

        long prefix = left.utf16Length >= 4
                ? left.prefix4
                : left.prefix4 | (right.prefix4 >>> (left.utf16Length << 4));
        long suffix = right.utf16Length >= 4
                ? right.suffix4
                : (left.suffix4 << (right.utf16Length << 4)) | right.suffix4;
        long bigrams = addBigramSignal(
                left.bigramSignal64 | right.bigramSignal64,
                left.lastUtf16Unit,
                right.firstUtf16Unit);
        long trigrams = left.trigramSignal64 | right.trigramSignal64;
        if (left.utf16Length >= 2) {
            trigrams = addTrigramSignal(
                    trigrams,
                    (char) (left.suffix4 >>> 16),
                    left.lastUtf16Unit,
                    right.firstUtf16Unit);
        }
        if (right.utf16Length >= 2) {
            trigrams = addTrigramSignal(
                    trigrams,
                    left.lastUtf16Unit,
                    right.firstUtf16Unit,
                    (char) (right.prefix4 >>> 32));
        }

        return new M3StringFacts(
                length,
                Math.addExact(left.codePointCount, right.codePointCount) - (seamPair ? 1 : 0),
                Math.addExact(left.unpairedSurrogateCount, right.unpairedSurrogateCount)
                        - (seamPair ? 2 : 0),
                left.javaHash * right.hash31Power + right.javaHash,
                left.hash31Power * right.hash31Power,
                left.firstUtf16Unit,
                right.lastUtf16Unit,
                left.bitSignal64 | right.bitSignal64,
                left.asciiUpperHash * right.hash31Power + right.asciiUpperHash,
                left.asciiLowerHash * right.hash31Power + right.asciiLowerHash,
                left.asciiTitleHash * right.hash31Power + right.asciiLowerHash,
                prefix,
                suffix,
                bigrams,
                trigrams,
                trimStart,
                trimEnd,
                stripStart,
                stripEnd);
    }

    boolean mayContain(M3StringFacts needle) {
        if (needle.utf16Length > utf16Length) return false;
        if ((bitSignal64 & needle.bitSignal64) != needle.bitSignal64) return false;
        if (needle.utf16Length >= 3
                && (trigramSignal64 & needle.trigramSignal64) != needle.trigramSignal64) {
            return false;
        }
        return needle.utf16Length < 2
                || (bigramSignal64 & needle.bigramSignal64) == needle.bigramSignal64;
    }

    boolean prefixMayMatch(M3StringFacts prefix) {
        int width = Math.min(4, prefix.utf16Length);
        if (width == 0) return true;
        long mask = width == 4 ? -1L : -1L << (64 - width * 16);
        return (prefix4 & mask) == (prefix.prefix4 & mask);
    }

    boolean suffixMayMatch(M3StringFacts suffix) {
        int width = Math.min(4, suffix.utf16Length);
        if (width == 0) return true;
        long mask = width == 4 ? -1L : (1L << (width * 16)) - 1L;
        return (suffix4 & mask) == (suffix.suffix4 & mask);
    }

    boolean mayContainCodeUnit(char unit) {
        long required = addSignal(0L, unit);
        return (bitSignal64 & required) == required;
    }

    private static int pow31(int length) {
        int result = 1;
        int base = 31;
        for (int remaining = length; remaining != 0; remaining >>>= 1) {
            if ((remaining & 1) != 0) result *= base;
            base *= base;
        }
        return result;
    }

    private static long addSignal(long signal, char value) {
        int folded = value >= 'A' && value <= 'Z' ? value | 0x20 : value;
        int first = mix32(folded);
        int second = mix32(folded ^ 0x9e3779b9);
        return signal | (1L << (first & 63)) | (1L << (second & 63));
    }

    private static long addBigramSignal(long signal, char first, char second) {
        return addNgramSignal(signal, ((long) first << 16) | second);
    }

    private static long addTrigramSignal(long signal, char first, char second, char third) {
        long packed = ((long) first << 32) | ((long) second << 16) | third;
        return addNgramSignal(signal, mix64(packed));
    }

    private static long addNgramSignal(long signal, long value) {
        long mixed = mix64(value);
        int first = (int) mixed & 63;
        int second = (int) (mixed >>> 32) & 63;
        return signal | (1L << first) | (1L << second);
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    private static int mix32(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }

    private static char asciiUpper(char value) {
        return value >= 'a' && value <= 'z' ? (char) (value - ('a' - 'A')) : value;
    }

    private static char asciiLower(char value) {
        return value >= 'A' && value <= 'Z' ? (char) (value + ('a' - 'A')) : value;
    }
}
