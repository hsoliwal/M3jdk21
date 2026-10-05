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

    private M3StringFacts(
            int utf16Length,
            int codePointCount,
            int unpairedSurrogateCount,
            int javaHash,
            int hash31Power,
            char firstUtf16Unit,
            char lastUtf16Unit,
            long bitSignal64) {
        this.utf16Length = utf16Length;
        this.codePointCount = codePointCount;
        this.unpairedSurrogateCount = unpairedSurrogateCount;
        this.javaHash = javaHash;
        this.hash31Power = hash31Power;
        this.firstUtf16Unit = firstUtf16Unit;
        this.lastUtf16Unit = lastUtf16Unit;
        this.bitSignal64 = bitSignal64;
    }

    static M3StringFacts scan(M3String value) {
        int length = value.length();
        int hash = 0;
        int codePoints = 0;
        int unpaired = 0;
        long signal = 0L;
        char first = 0;
        char last = 0;
        for (int index = 0; index < length; index++) {
            char unit = value.charAt(index);
            if (index == 0) first = unit;
            last = unit;
            hash = 31 * hash + unit;
            signal = addSignal(signal, unit);
            if (Character.isHighSurrogate(unit)) {
                if (index + 1 < length && Character.isLowSurrogate(value.charAt(index + 1))) {
                    codePoints++;
                    index++;
                    char low = value.charAt(index);
                    last = low;
                    hash = 31 * hash + low;
                    signal = addSignal(signal, low);
                } else {
                    codePoints++;
                    unpaired++;
                }
            } else if (Character.isLowSurrogate(unit)) {
                codePoints++;
                unpaired++;
            } else {
                codePoints++;
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
                signal);
    }

    static M3StringFacts compose(M3StringFacts left, M3StringFacts right) {
        if (left.utf16Length == 0) return right;
        if (right.utf16Length == 0) return left;
        boolean seamPair =
                Character.isHighSurrogate(left.lastUtf16Unit)
                        && Character.isLowSurrogate(right.firstUtf16Unit);
        int length = Math.addExact(left.utf16Length, right.utf16Length);
        return new M3StringFacts(
                length,
                Math.addExact(left.codePointCount, right.codePointCount) - (seamPair ? 1 : 0),
                Math.addExact(left.unpairedSurrogateCount, right.unpairedSurrogateCount)
                        - (seamPair ? 2 : 0),
                left.javaHash * right.hash31Power + right.javaHash,
                left.hash31Power * right.hash31Power,
                left.firstUtf16Unit,
                right.lastUtf16Unit,
                left.bitSignal64 | right.bitSignal64);
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
