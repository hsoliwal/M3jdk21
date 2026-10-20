/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package com.sun.tools.javac.m3;

import java.util.Objects;
import jdk.internal.access.M3StringAccess;
import jdk.internal.access.SharedSecrets;

/**
 * M3 AST lexical facts projected directly from canonical M3 String storage when available.
 *
 * <p>The compiler does not retain another text table. Exact source ownership remains with String.
 * M3-backed ranges reuse fixed-size String facts; non-M3 ranges are scanned once as a compatibility
 * fallback. Candidate signals may reject impossible work but exact String comparison remains the
 * semantic oracle.</p>
 */
public final class M3ASTTextFacts {
    private M3ASTTextFacts() { }

    public record Facts(
            int utf16Length,
            int utf8Length,
            int modifiedUtf8Length,
            int codePointCount,
            int unpairedSurrogateCount,
            int javaHash,
            int hash31Power,
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
            int stripEnd,
            long ownerStructuralHash64,
            long coordinate,
            boolean m3Backed,
            boolean signalsAvailable) {

        public boolean mayContainUnits(long requiredSignal64) {
            return !signalsAvailable
                    || (bitSignal64 & requiredSignal64) == requiredSignal64;
        }

        public boolean mayContainBigrams(long requiredSignal64) {
            return !signalsAvailable
                    || (bigramSignal64 & requiredSignal64) == requiredSignal64;
        }

        public boolean mayContainTrigrams(long requiredSignal64) {
            return !signalsAvailable
                    || (trigramSignal64 & requiredSignal64) == requiredSignal64;
        }
    }

    public static Facts of(String source) {
        Objects.requireNonNull(source, "source");
        return of(source, 0, source.length());
    }

    public static Facts of(String source, int beginIndex, int endIndex) {
        String checked = Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(beginIndex, endIndex, checked.length());

        M3StringAccess access = SharedSecrets.getM3StringAccess();
        if (access != null) {
            M3StringAccess.Facts facts = access.facts(checked, beginIndex, endIndex);
            if (facts != null) return fromM3(facts);
        }
        return scanFallback(checked, beginIndex, endIndex);
    }

    private static Facts fromM3(M3StringAccess.Facts facts) {
        return new Facts(
                facts.utf16Length(),
                facts.utf8Length(),
                facts.modifiedUtf8Length(),
                facts.codePointCount(),
                facts.unpairedSurrogateCount(),
                facts.javaHash(),
                facts.hash31Power(),
                facts.bitSignal64(),
                facts.ascii(),
                facts.latin1(),
                facts.asciiUpperHash(),
                facts.asciiLowerHash(),
                facts.asciiTitleHash(),
                facts.prefix4(),
                facts.suffix4(),
                facts.bigramSignal64(),
                facts.trigramSignal64(),
                facts.trimStart(),
                facts.trimEnd(),
                facts.stripStart(),
                facts.stripEnd(),
                facts.ownerStructuralHash64(),
                facts.coordinate(),
                true,
                true);
    }

    /**
     * Compatibility oracle for bootstrap/non-M3 Strings.
     *
     * <p>This intentionally does not duplicate the M3 private signal mixer. Signals are marked
     * unavailable so callers cannot reject candidates from fallback zero values.</p>
     */
    private static Facts scanFallback(String source, int beginIndex, int endIndex) {
        int length = endIndex - beginIndex;
        int javaHash = 0;
        int hash31Power = 1;
        long utf8 = 0L;
        long modifiedUtf8 = 0L;
        int codePoints = 0;
        int unpaired = 0;
        boolean ascii = true;
        boolean latin1 = true;
        int upperHash = 0;
        int lowerHash = 0;
        int titleHash = 0;
        long prefix4 = 0L;
        long suffix4 = 0L;
        int trimStart = 0;
        int trimEnd = 0;
        int stripStart = 0;
        int stripEnd = 0;
        boolean leadingTrim = true;
        boolean leadingStrip = true;

        for (int local = 0; local < length; local++) {
            char unit = source.charAt(beginIndex + local);
            javaHash = 31 * javaHash + unit;
            hash31Power *= 31;
            modifiedUtf8 += (unit > 0 && unit <= 0x7f) ? 1L
                    : (unit <= 0x07ff ? 2L : 3L);
            ascii &= unit <= 0x7f;
            latin1 &= unit <= 0xff;
            upperHash = 31 * upperHash + asciiUpper(unit);
            lowerHash = 31 * lowerHash + asciiLower(unit);
            titleHash = 31 * titleHash
                    + (local == 0 ? asciiUpper(unit) : asciiLower(unit));
            if (local < 4) prefix4 |= (long) unit << (48 - (local << 4));
            suffix4 = (suffix4 << 16) | unit;

            if (leadingTrim) {
                if (unit <= ' ') trimStart = local + 1;
                else leadingTrim = false;
            }
            if (unit > ' ') trimEnd = local + 1;

            int codePoint = unit;
            int width = 1;
            if (Character.isHighSurrogate(unit) && local + 1 < length) {
                char next = source.charAt(beginIndex + local + 1);
                if (Character.isLowSurrogate(next)) {
                    codePoint = Character.toCodePoint(unit, next);
                    width = 2;
                } else {
                    unpaired++;
                }
            } else if (Character.isLowSurrogate(unit)) {
                unpaired++;
            }

            boolean whitespace = Character.isWhitespace(codePoint);
            if (leadingStrip) {
                if (whitespace) stripStart = local + width;
                else leadingStrip = false;
            }
            if (!whitespace) stripEnd = local + width;

            if (width == 2) {
                utf8 += 4L;
                codePoints++;
                local++;
                char low = source.charAt(beginIndex + local);
                javaHash = 31 * javaHash + low;
                hash31Power *= 31;
                modifiedUtf8 += 3L;
                ascii = false;
                latin1 = false;
                upperHash = 31 * upperHash + low;
                lowerHash = 31 * lowerHash + low;
                titleHash = 31 * titleHash + low;
                if (local < 4) prefix4 |= (long) low << (48 - (local << 4));
                suffix4 = (suffix4 << 16) | low;
                continue;
            }

            utf8 += unit <= 0x7f ? 1L : (unit <= 0x7ff ? 2L : 3L);
            codePoints++;
        }

        return new Facts(
                length,
                utf8 > Integer.MAX_VALUE ? -1 : (int) utf8,
                modifiedUtf8 > Integer.MAX_VALUE ? -1 : (int) modifiedUtf8,
                codePoints,
                unpaired,
                javaHash,
                hash31Power,
                0L,
                ascii,
                latin1,
                upperHash,
                lowerHash,
                titleHash,
                prefix4,
                suffix4,
                0L,
                0L,
                trimStart,
                trimEnd,
                stripStart,
                stripEnd,
                0L,
                0L,
                false,
                false);
    }

    private static char asciiUpper(char value) {
        return value >= 'a' && value <= 'z'
                ? (char) (value - ('a' - 'A'))
                : value;
    }

    private static char asciiLower(char value) {
        return value >= 'A' && value <= 'Z'
                ? (char) (value + ('a' - 'A'))
                : value;
    }
}
