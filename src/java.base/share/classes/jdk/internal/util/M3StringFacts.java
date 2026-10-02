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
package jdk.internal.util;

import java.util.Objects;

/**
 * Necessary-condition UTF-16 facts shared by M3 String/search/regex paths.
 *
 * <p>The bit signal deliberately mirrors Synexia MIndexString's two-bit,
 * ASCII-case-folded presence signal. A missing bit can reject work; matching
 * bits never prove equality or a regex result.</p>
 */
public final class M3StringFacts {
    public static final int ASCII = 1;
    public static final int LATIN1 = 1 << 1;
    public static final int HAS_ASCII_UPPER = 1 << 2;
    public static final int HAS_ASCII_LOWER = 1 << 3;
    public static final int HAS_SURROGATE = 1 << 4;
    public static final int BLANK = 1 << 5;
    public static final int EMPTY = 1 << 6;
    public static final int HAS_ASCII_DIGIT = 1 << 7;
    public static final int HAS_ASCII_WORD = 1 << 8;
    public static final int HAS_ASCII_SPACE = 1 << 9;
    public static final int REGEX_PRESENCE =
            HAS_ASCII_UPPER | HAS_ASCII_LOWER | HAS_ASCII_DIGIT
                    | HAS_ASCII_WORD | HAS_ASCII_SPACE;

    private M3StringFacts() {}

    public static long bitSignal64(CharSequence value) {
        Objects.requireNonNull(value);
        long signal = 0L;
        for (int index = 0; index < value.length(); index++) {
            signal = addSignal(signal, value.charAt(index));
        }
        return signal;
    }

    public static int characterFlags(CharSequence value) {
        Objects.requireNonNull(value);
        int flags = initialFlags(value.length());
        for (int index = 0; index < value.length(); index++) {
            flags = addFlags(flags, value.charAt(index));
        }
        return flags;
    }

    public static boolean mayContain(long haystackSignal, long requiredSignal) {
        return (haystackSignal & requiredSignal) == requiredSignal;
    }

    public static long addSignal(long signal, char value) {
        int folded = foldAscii(value);
        int first = mix(folded);
        int second = mix(folded ^ 0x9e37_79b9);
        return signal | (1L << (first & 63)) | (1L << (second & 63));
    }

    public static int initialFlags(int length) {
        if (length < 0) throw new IllegalArgumentException("length");
        int flags = ASCII | LATIN1 | BLANK;
        return length == 0 ? flags | EMPTY : flags;
    }

    public static int addFlags(int flags, char value) {
        if (value > 0x7f) flags &= ~ASCII;
        if (value > 0xff) flags &= ~LATIN1;
        if (value >= 'A' && value <= 'Z') flags |= HAS_ASCII_UPPER | HAS_ASCII_WORD;
        if (value >= 'a' && value <= 'z') flags |= HAS_ASCII_LOWER | HAS_ASCII_WORD;
        if (value >= '0' && value <= '9') flags |= HAS_ASCII_DIGIT | HAS_ASCII_WORD;
        if (value == '_') flags |= HAS_ASCII_WORD;
        if (value == ' ' || (value >= '\t' && value <= '\r')) flags |= HAS_ASCII_SPACE;
        if (Character.isSurrogate(value)) flags |= HAS_SURROGATE;
        if (!Character.isWhitespace(value)) flags &= ~BLANK;
        return flags;
    }

    private static int foldAscii(char value) {
        return value >= 'A' && value <= 'Z' ? value | 0x20 : value;
    }

    private static int mix(int value) {
        int x = value;
        x ^= x >>> 16;
        x *= 0x7feb_352d;
        x ^= x >>> 15;
        x *= 0x846c_a68b;
        return x ^ (x >>> 16);
    }
}
