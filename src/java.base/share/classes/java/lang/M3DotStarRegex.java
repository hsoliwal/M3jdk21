/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

/**
 * Bounded two-row full-match DP for a deliberately small, Java-compatible dot/star subset.
 * No input text is copied, interned, retained or flattened.
 *
 * <p>LeetCode 10 permits dot to match LF; this JDK-specific kernel instead preserves default
 * java.util.regex.Pattern semantics (dot excludes line terminators). All other syntax and
 * UTF-16 surrogate input is declined to the authoritative Pattern/Matcher engine.</p>
 */
final class M3DotStarRegex {
    static final int UNSUPPORTED = -1;
    static final int NO_MATCH = 0;
    static final int MATCH = 1;
    private static final int MAX_PATTERN_UNITS = 512;
    private static final long MAX_DP_CELLS = 4_000_000L;

    private M3DotStarRegex() {}

    /** Negative means "use ordinary Pattern.matches", never "does not match". */
    static int tryFullMatch(CharSequence text, String regex) {
        int patternLength = regex.length();
        int textLength = text.length();
        if (patternLength == 0) return textLength == 0 ? MATCH : NO_MATCH;
        if (patternLength > MAX_PATTERN_UNITS) return UNSUPPORTED;

        // Transient pattern metadata only; never another canonical spelling store.
        char[] symbols = new char[patternLength];
        boolean[] repeated = new boolean[patternLength];
        int atoms = 0;
        for (int index = 0; index < patternLength; index++) {
            char symbol = regex.charAt(index);
            if (!((symbol >= 'a' && symbol <= 'z')
                    || (symbol >= 'A' && symbol <= 'Z')
                    || (symbol >= '0' && symbol <= '9')
                    || symbol == '_'
                    || symbol == '.')) {
                return UNSUPPORTED;
            }
            symbols[atoms] = symbol;
            if (index + 1 < patternLength && regex.charAt(index + 1) == '*') {
                repeated[atoms] = true;
                index++;
            }
            atoms++;
        }
        if (((long) textLength + 1L) * (atoms + 1L) > MAX_DP_CELLS) return UNSUPPORTED;

        boolean[] previous = new boolean[atoms + 1];
        boolean[] current = new boolean[atoms + 1];
        previous[0] = true;
        for (int j = 0; j < atoms; j++) {
            if (repeated[j]) previous[j + 1] = previous[j];
        }
        for (int i = 0; i < textLength; i++) {
            char unit = text.charAt(i);
            // A Java regex dot consumes a code point; UTF-16 halves need the JDK engine.
            if (Character.isSurrogate(unit)) return UNSUPPORTED;
            current[0] = false;
            for (int j = 0; j < atoms; j++) {
                char symbol = symbols[j];
                boolean accepts = symbol == '.'
                        ? !isLineTerminator(unit)
                        : symbol == unit;
                current[j + 1] = repeated[j]
                        ? (current[j] || (accepts && previous[j + 1]))
                        : (accepts && previous[j]);
            }
            boolean[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[atoms] ? MATCH : NO_MATCH;
    }

    private static boolean isLineTerminator(char unit) {
        return unit == '\n' || unit == '\r' || unit == '\u0085'
                || unit == '\u2028' || unit == '\u2029';
    }
}
