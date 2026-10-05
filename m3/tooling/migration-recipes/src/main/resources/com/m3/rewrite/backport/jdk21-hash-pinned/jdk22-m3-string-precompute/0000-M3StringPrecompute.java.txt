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

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Internal bounded precompute plane for M3 String.
 *
 * <p>Synexia is donor/provenance only. Runtime facts live in M3JDK21. This
 * sidecar deliberately does not add per-String or per-MIndexString payload
 * arrays. Facts are immutable, derived from canonical M3 String storage, and
 * retained in a bounded direct-mapped cache.</p>
 */
final class M3StringPrecompute {
    private static final int SLOTS = 1 << 12;
    private static final int SLOT_MASK = SLOTS - 1;
    private static final AtomicReferenceArray<Facts> CACHE =
            new AtomicReferenceArray<>(SLOTS);

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
        long id = value.canonicalId();
        int slot = slot(id);
        Facts current = CACHE.get(slot);
        if (current != null && current.owner == value && current.canonicalId == id) {
            return current;
        }
        Facts computed = compute(value, id);
        CACHE.set(slot, computed);
        return computed;
    }

    private static Facts compute(MIndexString value, long id) {
        int length = value.length();
        long low = 0L;
        long high = 0L;
        int codePoints = 0;
        int unpaired = 0;

        for (int index = 0; index < length; index++) {
            char ch = value.charAt(index);
            int bit = mixChar(ch);
            if (bit < 64) {
                low |= 1L << bit;
            } else {
                high |= 1L << (bit - 64);
            }

            codePoints++;
            if (Character.isHighSurrogate(ch)) {
                if (index + 1 < length && Character.isLowSurrogate(value.charAt(index + 1))) {
                    char lowSurrogate = value.charAt(++index);
                    int lowBit = mixChar(lowSurrogate);
                    if (lowBit < 64) {
                        low |= 1L << lowBit;
                    } else {
                        high |= 1L << (lowBit - 64);
                    }
                } else {
                    unpaired++;
                }
            } else if (Character.isLowSurrogate(ch)) {
                unpaired++;
            }
        }

        return new Facts(
                value,
                id,
                low,
                high,
                codePoints,
                unpaired,
                pow31(length),
                length == 0 ? 0 : value.charAt(0),
                length == 0 ? 0 : value.charAt(length - 1));
    }

    private static int slot(long canonicalId) {
        long mixed = canonicalId;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        return ((int) mixed) & SLOT_MASK;
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
        final MIndexString owner;
        final long canonicalId;
        final long charMaskLow;
        final long charMaskHigh;
        final int codePointCount;
        final int unpairedSurrogateCount;
        final int power31;
        final char firstChar;
        final char lastChar;

        Facts(
                MIndexString owner,
                long canonicalId,
                long charMaskLow,
                long charMaskHigh,
                int codePointCount,
                int unpairedSurrogateCount,
                int power31,
                char firstChar,
                char lastChar) {
            this.owner = owner;
            this.canonicalId = canonicalId;
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
