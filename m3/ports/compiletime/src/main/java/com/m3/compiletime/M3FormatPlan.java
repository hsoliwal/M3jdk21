// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.util.Arrays;

/**
 * Parsed plan of a constant {@link java.util.Formatter} format string as a flat int image.
 *
 * <p>Grammar: {@code %[argument_index$][flags][width][.precision][tT]conversion}, the
 * {@code java.util.Formatter} specifier syntax. Image records are {@code [LITERAL, begin, end]}
 * (a UTF-16 range of the format string itself: the plan never copies spelling) or
 * {@code [CONVERSION, conversion, flags, width, precision, argIndex]}. The plan is a
 * precompute candidate only: {@code Formatter} remains the semantic authority, including every
 * argument-dependent exception.</p>
 */
public record M3FormatPlan(Kind kind, int literalSegments, int conversions, int[] image) {
    /** Plan classes used by the inventory. */
    public enum Kind {
        /** Only literal text, {@code %%} and {@code %n}. */
        NO_CONVERSION,
        /** Only bare {@code %s}/{@code %d} (plus {@code %%}, {@code %n}). */
        SIMPLE,
        /** Grammar-valid with richer conversions, flags, width, precision or indices. */
        GENERAL,
        /** A specifier the {@code Formatter} grammar rejects. */
        INVALID
    }

    public static final int LITERAL = 1;
    public static final int CONVERSION = 2;
    private static final String FLAGS = "-#+ 0,(<";
    private static final String CONVERSIONS = "bBhHsScCdoxXeEfgGaAtT%n";

    /** Parses {@code format}; never throws. */
    public static M3FormatPlan parse(String format) {
        int[] image = new int[16];
        int size = 0;
        int literals = 0;
        int conversions = 0;
        boolean simple = true;
        int i = 0;
        int literalStart = 0;
        while (i < format.length()) {
            if (format.charAt(i) != '%') {
                i++;
                continue;
            }
            if (literalStart < i) {
                image = grow(image, size + 3);
                size = put(image, size, LITERAL, literalStart, i);
                literals++;
            }
            int[] spec = specifier(format, i + 1);
            if (spec == null) {
                return new M3FormatPlan(Kind.INVALID, literals, conversions, Arrays.copyOf(image, size));
            }
            image = grow(image, size + 6);
            size = put(image, size, CONVERSION, spec[1], spec[2], spec[3], spec[4], spec[5]);
            char conv = (char) (spec[1] & 0xffff);
            if (conv != '%' && conv != 'n') {
                conversions++;
                simple &= (conv == 's' || conv == 'd') && spec[2] == 0 && spec[3] < 0
                        && spec[4] < 0 && spec[5] < 0;
            }
            i = spec[0];
            literalStart = i;
        }
        if (literalStart < format.length()) {
            image = grow(image, size + 3);
            size = put(image, size, LITERAL, literalStart, format.length());
            literals++;
        }
        Kind kind = conversions == 0 ? Kind.NO_CONVERSION : simple ? Kind.SIMPLE : Kind.GENERAL;
        return new M3FormatPlan(kind, literals, conversions, Arrays.copyOf(image, size));
    }

    /** Returns {end, conversion, flags, width, precision, argIndex} or null when invalid. */
    private static int[] specifier(String f, int at) {
        int i = at;
        int argIndex = -1;
        int digits = digitsEnd(f, i);
        if (digits > i && digits < f.length() && f.charAt(digits) == '$') {
            argIndex = parseBounded(f, i, digits);
            i = digits + 1;
        }
        int flags = 0;
        while (i < f.length() && FLAGS.indexOf(f.charAt(i)) >= 0) {
            flags |= 1 << FLAGS.indexOf(f.charAt(i));
            i++;
        }
        int width = -1;
        digits = digitsEnd(f, i);
        if (digits > i) {
            width = parseBounded(f, i, digits);
            i = digits;
        }
        int precision = -1;
        if (i < f.length() && f.charAt(i) == '.') {
            digits = digitsEnd(f, i + 1);
            if (digits == i + 1) {
                return null;
            }
            precision = parseBounded(f, i + 1, digits);
            i = digits;
        }
        if (i >= f.length() || CONVERSIONS.indexOf(f.charAt(i)) < 0) {
            return null;
        }
        int conversion = f.charAt(i);
        if (conversion == 't' || conversion == 'T') {
            if (i + 1 >= f.length()) {
                return null;
            }
            conversion |= f.charAt(++i) << 16;
        }
        return new int[] {i + 1, conversion, flags, width, precision, argIndex};
    }

    private static int digitsEnd(String f, int i) {
        int end = i;
        while (end < f.length() && f.charAt(end) >= '0' && f.charAt(end) <= '9') {
            end++;
        }
        return end;
    }

    private static int parseBounded(String f, int begin, int end) {
        return end - begin > 9 ? Integer.MAX_VALUE : Integer.parseInt(f, begin, end, 10);
    }

    private static int[] grow(int[] image, int need) {
        return need <= image.length ? image : Arrays.copyOf(image, Math.max(need, image.length * 2));
    }

    private static int put(int[] image, int size, int... values) {
        System.arraycopy(values, 0, image, size, values.length);
        return size + values.length;
    }
}
