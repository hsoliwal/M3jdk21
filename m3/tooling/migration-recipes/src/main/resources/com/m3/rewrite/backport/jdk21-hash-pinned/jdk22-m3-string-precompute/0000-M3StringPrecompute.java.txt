/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

package java.lang;

/**
 * Internal constant-size precompute plane for M3 String.
 *
 * <p>Synexia is donor/provenance only. Runtime facts live in M3JDK21. Constant-size facts are
 * retained once by the canonical {@link MIndexString} storage identity, so every ordinary
 * {@link String} wrapper referring to that storage reuses the same facts. No global cache owns
 * String payloads or extends mapped-storage lifetime. Length-proportional analyses belong in
 * separately budgeted precompute owners.</p>
 *
 * <p>Every filter is a necessary condition only. A negative result may reject work; a positive
 * result always requires exact canonical M3 String verification.</p>
 */
final class M3StringPrecompute {
    private M3StringPrecompute() {}

    static boolean mayContainChar(MIndexString value, char ch) {
        return facts(value).mayContainChar(ch);
    }

    static boolean mayContainCodePoint(MIndexString value, int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            return false;
        }
        if (Character.isBmpCodePoint(codePoint)) {
            return mayContainChar(value, (char) codePoint);
        }
        return mayContainChar(value, Character.highSurrogate(codePoint))
                && mayContainChar(value, Character.lowSurrogate(codePoint));
    }

    static Facts facts(MIndexString value) {
        return value.precomputedFacts();
    }

    /*
     * Joined-value folding is owned by MIndexString because its segment geometry is private.
     * Scalar and partial-range scans stay here so no second representation learns storage layout.
     */
    static Facts scan(MIndexString value, int start, int count) {
        if (count == 0) {
            return Facts.EMPTY;
        }

        long lowMask = 0L;
        long highMask = 0L;
        long prefix4 = 0L;
        long suffix4 = 0L;
        long bigrams = 0L;
        long trigrams = 0L;
        int upperHash = 0;
        int lowerHash = 0;
        int titleHash = 0;
        int trimStart = -1;
        int trimEnd = 0;
        int end = start + count;
        char previous2 = 0;
        char previous1 = 0;

        for (int index = start; index < end; index++) {
            char ch = value.charAt(index);
            int relative = index - start;
            int bit = mixChar(ch);
            if (bit < 64) {
                lowMask |= 1L << bit;
            } else {
                highMask |= 1L << (bit - 64);
            }
            if (relative < 4) {
                prefix4 |= (long) ch << (48 - (relative << 4));
            }
            suffix4 = (suffix4 << 16) | ch;
            upperHash = 31 * upperHash + asciiUpper(ch);
            lowerHash = 31 * lowerHash + asciiLower(ch);
            titleHash = 31 * titleHash + (relative == 0 ? asciiUpper(ch) : asciiLower(ch));
            if (relative >= 1) {
                bigrams = addBigramSignal(bigrams, previous1, ch);
            }
            if (relative >= 2) {
                trigrams = addTrigramSignal(trigrams, previous2, previous1, ch);
            }
            previous2 = previous1;
            previous1 = ch;
            if (ch > 0x20) {
                if (trimStart < 0) {
                    trimStart = relative;
                }
                trimEnd = relative + 1;
            }
        }
        if (trimStart < 0) {
            trimStart = 0;
            trimEnd = 0;
        }

        int codePoints = 0;
        int unpaired = 0;
        int stripStart = -1;
        int stripEnd = 0;
        for (int index = start; index < end; ) {
            char first = value.charAt(index);
            int relative = index - start;
            int codePoint = first;
            int width = 1;
            if (Character.isHighSurrogate(first)
                    && index + 1 < end
                    && Character.isLowSurrogate(value.charAt(index + 1))) {
                codePoint = Character.toCodePoint(first, value.charAt(index + 1));
                width = 2;
            } else if (Character.isSurrogate(first)) {
                unpaired++;
            }
            if (!Character.isWhitespace(codePoint)) {
                if (stripStart < 0) {
                    stripStart = relative;
                }
                stripEnd = relative + width;
            }
            codePoints++;
            index += width;
        }
        if (stripStart < 0) {
            stripStart = 0;
            stripEnd = 0;
        }

        return new Facts(
                count,
                lowMask,
                highMask,
                codePoints,
                unpaired,
                pow31(count),
                value.charAt(start),
                value.charAt(end - 1),
                upperHash,
                lowerHash,
                titleHash,
                prefix4,
                suffix4,
                bigrams,
                trigrams,
                new Whitespace(count, trimStart, trimEnd, stripStart, stripEnd));
    }

    /**
     * Constant-time seam composition for content/search facts. Whitespace is rescanned once for a
     * joined canonical value because Character.isWhitespace is a code-point operation and a
     * surrogate pair may cross an atom seam.
     */
    static Facts combine(Facts left, Facts right) {
        if (left.utf16Length == 0) {
            return right;
        }
        if (right.utf16Length == 0) {
            return left;
        }
        boolean joinsSurrogatePair =
                Character.isHighSurrogate(left.lastChar)
                        && Character.isLowSurrogate(right.firstChar);
        long prefix = left.utf16Length >= 4
                ? left.prefix4
                : left.prefix4 | (right.prefix4 >>> (left.utf16Length << 4));
        long suffix = right.utf16Length >= 4
                ? right.suffix4
                : (left.suffix4 << (right.utf16Length << 4)) | right.suffix4;
        long bigrams = addBigramSignal(
                left.bigramSignal64 | right.bigramSignal64, left.lastChar, right.firstChar);
        long trigrams = left.trigramSignal64 | right.trigramSignal64;
        if (left.utf16Length >= 2) {
            trigrams = addTrigramSignal(
                    trigrams, (char) (left.suffix4 >>> 16), left.lastChar, right.firstChar);
        }
        if (right.utf16Length >= 2) {
            trigrams = addTrigramSignal(
                    trigrams, left.lastChar, right.firstChar, (char) (right.prefix4 >>> 32));
        }
        return new Facts(
                Math.addExact(left.utf16Length, right.utf16Length),
                left.charMaskLow | right.charMaskLow,
                left.charMaskHigh | right.charMaskHigh,
                Math.addExact(left.codePointCount, right.codePointCount)
                        - (joinsSurrogatePair ? 1 : 0),
                Math.addExact(left.unpairedSurrogateCount, right.unpairedSurrogateCount)
                        - (joinsSurrogatePair ? 2 : 0),
                left.power31 * right.power31,
                left.firstChar,
                right.lastChar,
                left.asciiUpperHash * right.power31 + right.asciiUpperHash,
                left.asciiLowerHash * right.power31 + right.asciiLowerHash,
                left.asciiTitleHash * right.power31 + right.asciiLowerHash,
                prefix,
                suffix,
                bigrams,
                trigrams,
                Whitespace.UNKNOWN);
    }

    static Facts withWhitespace(MIndexString value, Facts facts) {
        if (facts.utf16Length == 0 || facts.whitespace.known()) {
            return facts;
        }
        Facts scanned = scan(value, 0, facts.utf16Length);
        return facts.withWhitespace(scanned.whitespace);
    }

    private static long addBigramSignal(long signal, char first, char second) {
        return addSignal(signal, ((long) first << 16) | second);
    }

    private static long addTrigramSignal(long signal, char first, char second, char third) {
        long value = ((long) first << 32) | ((long) second << 16) | third;
        return addSignal(signal, mix64(value));
    }

    private static long addSignal(long signal, long value) {
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

    private static int mixChar(char ch) {
        int value = ch;
        value ^= value >>> 7;
        value *= 0x9e3779b1;
        value ^= value >>> 16;
        return value & 127;
    }

    private static int pow31(int exponent) {
        int result = 1;
        int base = 31;
        for (int remaining = exponent; remaining != 0; remaining >>>= 1) {
            if ((remaining & 1) != 0) {
                result *= base;
            }
            base *= base;
        }
        return result;
    }

    private static char asciiUpper(char value) {
        return value >= 'a' && value <= 'z' ? (char) (value - ('a' - 'A')) : value;
    }

    private static char asciiLower(char value) {
        return value >= 'A' && value <= 'Z' ? (char) (value + ('a' - 'A')) : value;
    }

    static final class Facts {
        static final Facts EMPTY = new Facts(
                0, 0L, 0L, 0, 0, 1, (char) 0, (char) 0,
                0, 0, 0, 0L, 0L, 0L, 0L, Whitespace.EMPTY);

        final int utf16Length;
        final long charMaskLow;
        final long charMaskHigh;
        final int codePointCount;
        final int unpairedSurrogateCount;
        final int power31;
        final char firstChar;
        final char lastChar;
        final int asciiUpperHash;
        final int asciiLowerHash;
        final int asciiTitleHash;
        final long prefix4;
        final long suffix4;
        final long bigramSignal64;
        final long trigramSignal64;
        final Whitespace whitespace;

        Facts(
                int utf16Length,
                long charMaskLow,
                long charMaskHigh,
                int codePointCount,
                int unpairedSurrogateCount,
                int power31,
                char firstChar,
                char lastChar,
                int asciiUpperHash,
                int asciiLowerHash,
                int asciiTitleHash,
                long prefix4,
                long suffix4,
                long bigramSignal64,
                long trigramSignal64,
                Whitespace whitespace) {
            this.utf16Length = utf16Length;
            this.charMaskLow = charMaskLow;
            this.charMaskHigh = charMaskHigh;
            this.codePointCount = codePointCount;
            this.unpairedSurrogateCount = unpairedSurrogateCount;
            this.power31 = power31;
            this.firstChar = firstChar;
            this.lastChar = lastChar;
            this.asciiUpperHash = asciiUpperHash;
            this.asciiLowerHash = asciiLowerHash;
            this.asciiTitleHash = asciiTitleHash;
            this.prefix4 = prefix4;
            this.suffix4 = suffix4;
            this.bigramSignal64 = bigramSignal64;
            this.trigramSignal64 = trigramSignal64;
            this.whitespace = whitespace;
        }

        Facts withWhitespace(Whitespace replacement) {
            return new Facts(
                    utf16Length, charMaskLow, charMaskHigh, codePointCount,
                    unpairedSurrogateCount, power31, firstChar, lastChar,
                    asciiUpperHash, asciiLowerHash, asciiTitleHash, prefix4, suffix4,
                    bigramSignal64, trigramSignal64, replacement);
        }

        boolean mayContainChar(char ch) {
            int bit = mixChar(ch);
            return bit < 64
                    ? (charMaskLow & (1L << bit)) != 0
                    : (charMaskHigh & (1L << (bit - 64))) != 0;
        }

        boolean mayContain(Facts needle) {
            if (needle.utf16Length > utf16Length) {
                return false;
            }
            if ((charMaskLow & needle.charMaskLow) != needle.charMaskLow
                    || (charMaskHigh & needle.charMaskHigh) != needle.charMaskHigh) {
                return false;
            }
            if (needle.utf16Length >= 3
                    && (trigramSignal64 & needle.trigramSignal64) != needle.trigramSignal64) {
                return false;
            }
            return needle.utf16Length < 2
                    || (bigramSignal64 & needle.bigramSignal64) == needle.bigramSignal64;
        }

        boolean prefixMayMatch(Facts prefix) {
            int width = Math.min(4, prefix.utf16Length);
            if (width == 0) {
                return true;
            }
            long mask = width == 4 ? -1L : -1L << (64 - width * 16);
            return (prefix4 & mask) == (prefix.prefix4 & mask);
        }

        boolean suffixMayMatch(Facts suffix) {
            int width = Math.min(4, suffix.utf16Length);
            if (width == 0) {
                return true;
            }
            long mask = width == 4 ? -1L : (1L << (width * 16)) - 1L;
            return (suffix4 & mask) == (suffix.suffix4 & mask);
        }
    }

    static final class Whitespace {
        static final Whitespace EMPTY = new Whitespace(0, 0, 0, 0, 0);
        static final Whitespace UNKNOWN = new Whitespace(-1, -1, -1, -1, -1);

        final int utf16Length;
        final int trimStart;
        final int trimEnd;
        final int stripStart;
        final int stripEnd;

        Whitespace(int utf16Length, int trimStart, int trimEnd, int stripStart, int stripEnd) {
            this.utf16Length = utf16Length;
            this.trimStart = trimStart;
            this.trimEnd = trimEnd;
            this.stripStart = stripStart;
            this.stripEnd = stripEnd;
        }

        boolean known() {
            return utf16Length >= 0;
        }

        boolean trimBlank() {
            return known() && utf16Length != 0 && trimStart == 0 && trimEnd == 0;
        }

        boolean blank() {
            return known() && (utf16Length == 0 || (stripStart == 0 && stripEnd == 0));
        }
    }
}
