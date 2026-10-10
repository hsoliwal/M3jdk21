/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/**
 * M3JDK's immutable numeric vocabulary projection for Synexia's
 * {@code dictlang.numbers.0-10000} source.
 *
 * <p>The value views share one UTF-16 backing piece.  A number lookup returns
 * the same cached view every time; flattening is explicit and limited to the
 * ordinary {@link String} boundary.</p>
 */
public final class M3NumberSpace {
    public static final String SOURCE_ID = "dictlang.numbers.0-10000";
    public static final String SOURCE_REVISION = "64a2ea61c73b548413fed6686a9daeeb0b9b0564";
    public static final String RECORD_ID = "number";
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 10_000;
    public static final M3NumberSpace INSTANCE = new M3NumberSpace();

    private final LocalM3StringPiece[] values;
    private final LocalM3StringPiece backing;

    private M3NumberSpace() {
        int totalUnits = 0;
        for (int value = MIN_VALUE; value <= MAX_VALUE; value++)
            totalUnits = Math.addExact(totalUnits, Integer.toString(value).length());
        char[] all = new char[totalUnits];
        int[] offsets = new int[MAX_VALUE + 1];
        int[] lengths = new int[MAX_VALUE + 1];
        int cursor = 0;
        for (int value = MIN_VALUE; value <= MAX_VALUE; value++) {
            String spelling = Integer.toString(value);
            offsets[value] = cursor;
            lengths[value] = spelling.length();
            spelling.getChars(0, spelling.length(), all, cursor);
            cursor += spelling.length();
        }
        backing = new LocalM3Arena().copyUtf16(all);
        values = new LocalM3StringPiece[MAX_VALUE + 1];
        for (int value = MIN_VALUE; value <= MAX_VALUE; value++)
            values[value] = backing.subSequence(offsets[value], offsets[value] + lengths[value]);
    }

    /** Return the canonical cached view for one value. */
    public LocalM3StringPiece number(int value) {
        checkValue(value);
        return values[value];
    }

    /** Materialize the canonical spelling at the ordinary String boundary. */
    public String spelling(int value) {
        return number(value).flatten();
    }

    /** Parse a canonical decimal view without calling {@code toString()}. */
    public LocalM3StringPiece parse(CharSequence spelling) {
        Objects.requireNonNull(spelling, "spelling");
        int length = spelling.length();
        if (length == 0 || (length > 1 && spelling.charAt(0) == '0'))
            throw new NumberFormatException("non-canonical number spelling: " + describe(spelling));
        int value = 0;
        for (int index = 0; index < length; index++) {
            char character = spelling.charAt(index);
            if (character < '0' || character > '9')
                throw new NumberFormatException("non-decimal number spelling: " + describe(spelling));
            int digit = character - '0';
            if (value > (MAX_VALUE - digit) / 10)
                throw new NumberFormatException("number outside 0..10000: " + describe(spelling));
            value = value * 10 + digit;
        }
        return number(value);
    }

    public int precomputedValueCount() { return values.length; }

    /** All values retain the same immutable backing owner. */
    public StorageIdentity storageIdentity() { return backing.storageIdentity(); }

    private static String describe(CharSequence spelling) {
        if (spelling instanceof String value) return value;
        StringBuilder result = new StringBuilder(spelling.length());
        for (int index = 0; index < spelling.length(); index++) result.append(spelling.charAt(index));
        return result.toString();
    }

    private static void checkValue(int value) {
        if (value < MIN_VALUE || value > MAX_VALUE)
            throw new IndexOutOfBoundsException("number value: " + value);
    }
}
