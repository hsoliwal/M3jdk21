/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import jdk.internal.util.ArraysSupport;

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
    private static final int UTF8_REPLACEMENT_BYTES =
            java.nio.charset.StandardCharsets.UTF_8.newEncoder().replacement().length;

    final int utf16Length;
    final int utf8Length;
    final int codePointCount;
    final int unpairedSurrogateCount;
    final int javaHash;
    final int hash31Power;
    final char firstUtf16Unit;
    final char lastUtf16Unit;
    final long bitSignal64;
    final boolean ascii;
    final boolean latin1;

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
            int utf8Length,
            int codePointCount,
            int unpairedSurrogateCount,
            int javaHash,
            int hash31Power,
            char firstUtf16Unit,
            char lastUtf16Unit,
            long bitSignal64,
            boolean ascii,
            boolean latin1,
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
        this.utf8Length = utf8Length;
        this.codePointCount = codePointCount;
        this.unpairedSurrogateCount = unpairedSurrogateCount;
        this.javaHash = javaHash;
        this.hash31Power = hash31Power;
        this.firstUtf16Unit = firstUtf16Unit;
        this.lastUtf16Unit = lastUtf16Unit;
        this.bitSignal64 = bitSignal64;
        this.ascii = ascii;
        this.latin1 = latin1;
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

    /**
     * Bounded materialization window for {@link #scan}: facts are folded over bulk-read UTF-16
     * units, never through per-unit owner dispatch. The window is transient scratch, not a shadow.
     */
    private static final int SCAN_WINDOW = 1024;

    /**
     * Code-unit signal bits for every Latin-1 unit, folded once at class initialization: the
     * per-unit scan and the flat-needle folds read the table instead of mixing twice per unit.
     * Units above U+00FF are mixed on demand. Pure cache of {@link #addSignal}; no output changes.
     */
    private static final long[] LATIN1_UNIT_SIGNAL = latin1UnitSignals();

    /** {@link Character#isWhitespace(char)} for every Latin-1 unit (the strip rule). */
    private static final boolean[] LATIN1_WHITESPACE = latin1Whitespace();

    /**
     * Folds every fact in one streaming pass over bulk-read windows. The code-point lane keeps a
     * one-unit delay so a high surrogate is resolved by its successor (or at the end), which keeps
     * the pass exact across window boundaries. Donor shape: Synexia
     * {@code MIndexTextPrecomputedFacts.compile} (single loop), extended with the code-point,
     * strip and trim lanes that are M3JDK-only.
     */
    static M3StringFacts scan(M3String value) {
        return value.coder() == String.LATIN1 ? scanLatin1(value) : scanUnits(value);
    }

    /**
     * The Latin-1 lane (A15): the units are bytes read once in bulk, the polynomial hashes come
     * from the vectorized hash, the title hash is derived exactly from the lower hash, ascii and
     * the UTF-8 length from the count of positive bytes, trim and strip from front and back scans,
     * and only the n-gram mixing folds every unit. Every fact is bit-identical to
     * {@link #scanUnits}; the two byte arrays are transient scratch of the owner's size.
     */
    static M3StringFacts scanLatin1(M3String value) {
        int length = value.length();
        if (length == 0) return scanUnits(value);
        byte[] bytes = new byte[length];
        value.getBytes(bytes, 0, 0, String.LATIN1, length);

        int positives = StringCoding.countPositives(bytes, 0, length);
        boolean ascii = positives == length;
        int hash = ArraysSupport.vectorizedHashCode(bytes, 0, length, 0, ArraysSupport.T_BOOLEAN);

        char first = (char) (bytes[0] & 0xff);
        char last = (char) (bytes[length - 1] & 0xff);
        int utf8 = length;
        long signal = 0L;
        long bigrams = 0L;
        long trigrams = 0L;
        long prefix = 0L;
        long suffix = 0L;
        int previous2 = 0;
        int previous1 = 0;
        int trimStart = 0;
        int trimEnd = 0;
        boolean leadingTrim = true;
        int stripStart = 0;
        int stripEnd = 0;
        boolean leadingStrip = true;

        // Capture every original-byte fact and lower-fold the same scratch byte before advancing.
        for (int index = 0; index < length; index++) {
            int unit = bytes[index] & 0xff;
            if (bytes[index] < 0) utf8++;

            signal |= LATIN1_UNIT_SIGNAL[unit];
            if (index < 4) prefix |= (long) unit << (48 - (index << 4));
            suffix = (suffix << 16) | unit;
            if (index >= 1) bigrams = addBigramSignal(bigrams, (char) previous1, (char) unit);
            if (index >= 2) {
                trigrams = addTrigramSignal(trigrams, (char) previous2, (char) previous1, (char) unit);
            }

            if (leadingTrim) {
                if (unit <= ' ') trimStart = index + 1;
                else leadingTrim = false;
            }
            if (unit > ' ') trimEnd = index + 1;

            boolean whitespace = LATIN1_WHITESPACE[unit];
            if (leadingStrip) {
                if (whitespace) stripStart = index + 1;
                else leadingStrip = false;
            }
            if (!whitespace) stripEnd = index + 1;

            previous2 = previous1;
            previous1 = unit;
            bytes[index] = (byte) (unit >= 'A' && unit <= 'Z' ? unit | 0x20 : unit);
        }
        if (leadingTrim) trimEnd = 0;
        if (leadingStrip) stripEnd = 0;

        int lowerHash = ArraysSupport.vectorizedHashCode(bytes, 0, length, 0, ArraysSupport.T_BOOLEAN);
        for (int index = 0; index < length; index++) {
            int unit = bytes[index] & 0xff;
            bytes[index] = (byte) (unit >= 'a' && unit <= 'z' ? unit & ~0x20 : unit);
        }
        int upperHash = ArraysSupport.vectorizedHashCode(bytes, 0, length, 0, ArraysSupport.T_BOOLEAN);
        int titleHash = lowerHash + pow31(length - 1) * (asciiUpper(first) - asciiLower(first));

        return new M3StringFacts(
                length,
                utf8,
                length,
                0,
                hash,
                pow31(length),
                first,
                last,
                signal,
                ascii,
                true,
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

    /** The A8 single pass over UTF-16 unit windows, every coder; the lane's exactness oracle. */
    static M3StringFacts scanUnits(M3String value) {
        int length = value.length();
        int hash = 0;
        int utf8 = 0;
        int codePoints = 0;
        int unpaired = 0;
        long signal = 0L;
        boolean ascii = true;
        boolean latin1 = true;
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
        boolean pendingHigh = false;
        int stripStart = 0;
        int stripEnd = 0;
        boolean leadingStrip = true;
        int trimStart = 0;
        int trimEnd = 0;
        boolean leadingTrim = true;

        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int base = 0; base < length; base += window.length) {
            int count = Math.min(window.length, length - base);
            value.getChars(base, base + count, window, 0);
            for (int offset = 0; offset < count; offset++) {
                int index = base + offset;
                char unit = window[offset];
                if (index == 0) first = unit;
                last = unit;
                hash = 31 * hash + unit;
                signal |= unitSignal(unit);
                ascii &= unit <= 0x7f;
                latin1 &= unit <= 0xff;
                upperHash = 31 * upperHash + asciiUpper(unit);
                lowerHash = 31 * lowerHash + asciiLower(unit);
                titleHash = 31 * titleHash + (index == 0 ? asciiUpper(unit) : asciiLower(unit));
                if (index < 4) prefix |= (long) unit << (48 - (index << 4));
                suffix = (suffix << 16) | unit;
                if (index >= 1) bigrams = addBigramSignal(bigrams, previous1, unit);
                if (index >= 2) trigrams = addTrigramSignal(trigrams, previous2, previous1, unit);

                if (leadingTrim) {
                    if (unit <= ' ') trimStart = index + 1;
                    else leadingTrim = false;
                }
                if (unit > ' ') trimEnd = index + 1;

                boolean whitespace;
                if (pendingHigh) {
                    pendingHigh = false;
                    if (Character.isLowSurrogate(unit)) {
                        utf8 = Math.addExact(utf8, 4);
                        codePoints++;
                        whitespace = Character.isWhitespace(Character.toCodePoint(previous1, unit));
                        if (leadingStrip) {
                            if (whitespace) stripStart = index + 1;
                            else leadingStrip = false;
                        }
                        if (!whitespace) stripEnd = index + 1;
                        previous2 = previous1;
                        previous1 = unit;
                        continue;
                    }
                    // The previous high surrogate stays unpaired: a lone surrogate is never whitespace.
                    unpaired++;
                    utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
                    codePoints++;
                    leadingStrip = false;
                    stripEnd = index;
                }
                if (Character.isHighSurrogate(unit)) {
                    pendingHigh = true;
                } else {
                    if (Character.isLowSurrogate(unit)) {
                        unpaired++;
                        utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
                    } else {
                        utf8 = Math.addExact(utf8, utf8Bytes(unit));
                    }
                    codePoints++;
                    whitespace = Character.isWhitespace(unit);
                    if (leadingStrip) {
                        if (whitespace) stripStart = index + 1;
                        else leadingStrip = false;
                    }
                    if (!whitespace) stripEnd = index + 1;
                }
                previous2 = previous1;
                previous1 = unit;
            }
        }
        if (pendingHigh) {
            unpaired++;
            utf8 = Math.addExact(utf8, UTF8_REPLACEMENT_BYTES);
            codePoints++;
            leadingStrip = false;
            stripEnd = length;
        }

        if (length == 0) {
            stripStart = stripEnd = trimStart = trimEnd = 0;
        } else {
            if (leadingStrip) stripEnd = 0;
            if (leadingTrim) trimEnd = 0;
        }

        return new M3StringFacts(
                length,
                utf8,
                codePoints,
                unpaired,
                hash,
                pow31(length),
                first,
                last,
                signal,
                ascii,
                latin1,
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
        int utf8 = Math.addExact(left.utf8Length, right.utf8Length);
        if (seamPair) {
            utf8 = Math.addExact(utf8, 4 - 2 * UTF8_REPLACEMENT_BYTES);
        }

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
                utf8,
                Math.addExact(left.codePointCount, right.codePointCount) - (seamPair ? 1 : 0),
                Math.addExact(left.unpairedSurrogateCount, right.unpairedSurrogateCount)
                        - (seamPair ? 2 : 0),
                left.javaHash * right.hash31Power + right.javaHash,
                left.hash31Power * right.hash31Power,
                left.firstUtf16Unit,
                right.lastUtf16Unit,
                left.bitSignal64 | right.bitSignal64,
                left.ascii && right.ascii,
                left.latin1 && right.latin1,
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

    /**
     * Necessary-condition gate for a flat (non-M3) needle such as a String literal: the needle's
     * code-unit, bigram and trigram signals are folded on the fly, without allocation, and must be
     * covered by this record's signals. Rejection proves absence; acceptance proves nothing.
     */
    boolean mayContain(String needle) {
        int length = needle.length();
        if (length > utf16Length) return false;
        long units = 0L;
        long bigrams = 0L;
        long trigrams = 0L;
        char previous2 = 0;
        char previous1 = 0;
        for (int index = 0; index < length; index++) {
            char unit = needle.charAt(index);
            units |= unitSignal(unit);
            if (index >= 1) bigrams = addBigramSignal(bigrams, previous1, unit);
            if (index >= 2) trigrams = addTrigramSignal(trigrams, previous2, previous1, unit);
            previous2 = previous1;
            previous1 = unit;
        }
        if ((bitSignal64 & units) != units) return false;
        if (length >= 3 && (trigramSignal64 & trigrams) != trigrams) return false;
        return length < 2 || (bigramSignal64 & bigrams) == bigrams;
    }

    /** Flat-prefix form of {@link #prefixMayMatch(M3StringFacts)}: first four UTF-16 units. */
    boolean prefixMayMatch(String prefix) {
        int width = Math.min(4, prefix.length());
        if (width == 0) return true;
        long packed = 0L;
        for (int index = 0; index < width; index++) {
            packed |= (long) prefix.charAt(index) << (48 - (index << 4));
        }
        long mask = width == 4 ? -1L : -1L << (64 - width * 16);
        return (prefix4 & mask) == (packed & mask);
    }

    /** Flat-suffix form of {@link #suffixMayMatch(M3StringFacts)}: last four UTF-16 units. */
    boolean suffixMayMatch(String suffix) {
        int length = suffix.length();
        int width = Math.min(4, length);
        if (width == 0) return true;
        long packed = 0L;
        for (int index = length - width; index < length; index++) {
            packed = (packed << 16) | suffix.charAt(index);
        }
        long mask = width == 4 ? -1L : (1L << (width * 16)) - 1L;
        return (suffix4 & mask) == (packed & mask);
    }

    boolean mayContainCodeUnit(char unit) {
        long required = codeUnitSignal(unit);
        return (bitSignal64 & required) == required;
    }

    static long codeUnitSignal(char unit) {
        return unitSignal(unit);
    }

    private static long unitSignal(char unit) {
        return unit < 0x100 ? LATIN1_UNIT_SIGNAL[unit] : addSignal(0L, unit);
    }

    private static long[] latin1UnitSignals() {
        long[] table = new long[0x100];
        for (int unit = 0; unit < table.length; unit++) {
            table[unit] = addSignal(0L, (char) unit);
        }
        return table;
    }

    private static boolean[] latin1Whitespace() {
        boolean[] table = new boolean[0x100];
        for (int unit = 0; unit < table.length; unit++) {
            table[unit] = Character.isWhitespace((char) unit);
        }
        return table;
    }

    private static int utf8Bytes(char value) {
        if (value <= 0x7f) return 1;
        if (value <= 0x7ff) return 2;
        return 3;
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

    /**
     * ASCII-lower hash of a flat region, the same 31-polynomial fold as {@link #asciiLowerHash}, as
     * a non-negative value; -1 when a unit above U+007F occurs (the gate then proves nothing, because
     * non-ASCII units can equal ASCII units ignoring case). Allocation-free.
     */
    static long asciiLowerHashOrNegative(String value, int from, int to) {
        int hash = 0;
        for (int index = from; index < to; index++) {
            char unit = value.charAt(index);
            if (unit > 0x7f) return -1L;
            hash = 31 * hash + asciiLower(unit);
        }
        return hash & 0xffffffffL;
    }

    private static char asciiLower(char value) {
        return value >= 'A' && value <= 'Z' ? (char) (value + ('a' - 'A')) : value;
    }
}
