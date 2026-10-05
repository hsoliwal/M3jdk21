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
 * <p>The fixed record deliberately includes the String-semantic facts that compose exactly:
 * UTF-16/code-point geometry, Java hash geometry, a conservative bit signal, and the trim/strip
 * boundaries used by {@link String}. Larger analyses such as trigram sets, prefix/Z lanes,
 * position masks and regex plans remain in separately budgeted internal caches.</p>
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
        char first = 0;
        char last = 0;

        int stripStart = 0;
        int stripEnd = 0;
        boolean leadingStrip = true;

        int index = 0;
        while (index < length) {
            int codePointStart = index;
            char unit = value.charAt(index);
            if (index == 0) first = unit;
            last = unit;
            hash = 31 * hash + unit;
            signal = addSignal(signal, unit);

            int codePoint = unit;
            int width = 1;
            if (Character.isHighSurrogate(unit)
                    && index + 1 < length
                    && Character.isLowSurrogate(value.charAt(index + 1))) {
                char low = value.charAt(index + 1);
                codePoint = Character.toCodePoint(unit, low);
                width = 2;
                last = low;
                hash = 31 * hash + low;
                signal = addSignal(signal, low);
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
            if (leadingStrip) {
                // Entire value is Unicode whitespace. start==length, end==0 is the
                // composable empty-boundary encoding used by tuple facts.
                stripEnd = 0;
            }
            if (trimStart == length) {
                // Same composable empty-boundary encoding for legacy trim().
                trimEnd = 0;
            }
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
                trimStart,
                trimEnd,
                stripStart,
                stripEnd);
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

    private static int mix32(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        return mixed ^ (mixed >>> 16);
    }
}
