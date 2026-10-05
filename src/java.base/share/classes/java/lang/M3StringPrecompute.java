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
 * <p>All filters are necessary conditions only. A negative result may reject work; a positive
 * result always requires exact canonical M3 String verification.</p>
 */
final class M3StringPrecompute {
    private M3StringPrecompute() {}

    static boolean mayContainChar(MIndexString value, char ch) {
        Facts facts = facts(value);
        int bit = mixChar(ch);
        return bit < 64
                ? (facts.charMaskLow & (1L << bit)) != 0
                : (facts.charMaskHigh & (1L << (bit - 64))) != 0;
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

    /**
     * Computes one canonical fact bundle. Joined M3 Strings reuse already-retained complete-atom
     * facts and scan only partial atom ranges.
     */
    static Facts compute(MIndexString value) {
        int length = value.length();
        if (length == 0) {
            return Facts.EMPTY;
        }
        return scan(value, 0, length);
    }

    static Facts scan(MIndexString value, int start, int count) {
        if (count == 0) {
            return Facts.EMPTY;
        }

        long lowMask = 0L;
        long highMask = 0L;
        int codePoints = 0;
        int unpaired = 0;
        int end = start + count;

        for (int index = start; index < end; index++) {
            char ch = value.charAt(index);
            int bit = mixChar(ch);
            if (bit < 64) {
                lowMask |= 1L << bit;
            } else {
                highMask |= 1L << (bit - 64);
            }

            codePoints++;
            if (Character.isHighSurrogate(ch)) {
                if (index + 1 < end && Character.isLowSurrogate(value.charAt(index + 1))) {
                    char lowSurrogate = value.charAt(++index);
                    int lowBit = mixChar(lowSurrogate);
                    if (lowBit < 64) {
                        lowMask |= 1L << lowBit;
                    } else {
                        highMask |= 1L << (lowBit - 64);
                    }
                } else {
                    unpaired++;
                }
            } else if (Character.isLowSurrogate(ch)) {
                unpaired++;
            }
        }

        return new Facts(
                count,
                lowMask,
                highMask,
                codePoints,
                unpaired,
                pow31(count),
                value.charAt(start),
                value.charAt(end - 1));
    }

    /**
     * Constant-time seam composition for complete immutable fact bundles.
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
                right.lastChar);
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

    static final class Facts {
        static final Facts EMPTY = new Facts(0, 0L, 0L, 0, 0, 1, (char) 0, (char) 0);

        final int utf16Length;
        final long charMaskLow;
        final long charMaskHigh;
        final int codePointCount;
        final int unpairedSurrogateCount;
        final int power31;
        final char firstChar;
        final char lastChar;

        Facts(
                int utf16Length,
                long charMaskLow,
                long charMaskHigh,
                int codePointCount,
                int unpairedSurrogateCount,
                int power31,
                char firstChar,
                char lastChar) {
            this.utf16Length = utf16Length;
            this.charMaskLow = charMaskLow;
            this.charMaskHigh = charMaskHigh;
            this.codePointCount = codePointCount;
            this.unpairedSurrogateCount = unpairedSurrogateCount;
            this.power31 = power31;
            this.firstChar = firstChar;
            this.lastChar = lastChar;
        }
    }
}
