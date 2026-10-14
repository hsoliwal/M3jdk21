/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;

/**
 * A bounded, operation-local full-match DP candidate for the lowercase-ASCII
 * LeetCode-10 grammar (letters, '.' and postfix '*'). This is NOT a Java regex
 * parser. All unrecognized patterns, inputs and budgets return UNAVAILABLE so
 * java.util.regex.Pattern remains the exact compatibility fallback.
 *
 * Compiled instructions are primitive metadata, not copied text payload. A
 * thread-confined M3String cursor reads canonical tuple/atom UTF-16 units
 * without flattening them. The only DP state is two rows, O(pattern tokens).
 */
final class M3StringRegexDp {
    static final int UNAVAILABLE = -1;
    static final int NO_MATCH = 0;
    static final int MATCH = 1;
    private static final int REPEATED = 1 << 8;
    private static final int ATOM_MASK = 0xff;
    private static final int MAX_PATTERN_UNITS = 8_192;
    private static final long MAX_CELL_VISITS = 4_000_000L;

    private M3StringRegexDp() {}

    /**
     * Return 0/1 for fully admitted semantics, -1 to execute the unchanged
     * JDK Pattern.matches route. Never changes exception behavior of String.matches.
     */
    static int tryMatches(M3String text, String regex) {
        int patternLength = regex.length();
        if (patternLength > MAX_PATTERN_UNITS) return UNAVAILABLE;

        // Compile postfix '*' onto its preceding atom, keeping no regex String
        // and not publishing mutable instructions. Reject malformed grammar.
        int[] program;
        try {
            program = new int[patternLength];
        } catch (OutOfMemoryError unavailable) {
            return UNAVAILABLE;
        }
        int tokens = 0;
        for (int at = 0; at < patternLength; at++) {
            char atom = regex.charAt(at);
            if (atom == '*') {
                if (tokens == 0 || (program[tokens - 1] & REPEATED) != 0) {
                    return UNAVAILABLE;
                }
                program[tokens - 1] |= REPEATED;
            } else if (atom == '.' || (atom >= 'a' && atom <= 'z')) {
                program[tokens++] = atom;
            } else {
                return UNAVAILABLE;
            }
        }
        int length = text.length();
        if ((long) length * Math.max(1, tokens) > MAX_CELL_VISITS) return UNAVAILABLE;

        boolean[] previous;
        boolean[] current;
        try {
            previous = new boolean[tokens + 1];
            current = new boolean[tokens + 1];
        } catch (OutOfMemoryError unavailable) {
            return UNAVAILABLE;
        }
        previous[0] = true;
        for (int j = 1; j <= tokens; j++) {
            if ((program[j - 1] & REPEATED) != 0) {
                previous[j] = previous[j - 1];
            }
        }

        M3String.CodeUnitCursor reader = new M3String.CodeUnitCursor(text);
        for (int i = 0; i < length; i++) {
            char unit = reader.at(i);
            // Restrict the fast path: Java '.' has line-terminator and Unicode
            // semantics different from the challenge's unrestricted dot.
            if (unit < 'a' || unit > 'z') return UNAVAILABLE;
            Arrays.fill(current, false);
            for (int j = 1; j <= tokens; j++) {
                int instruction = program[j - 1];
                int atom = instruction & ATOM_MASK;
                boolean equal = atom == '.' || atom == unit;
                current[j] = (instruction & REPEATED) != 0
                        ? current[j - 1] || (equal && previous[j])
                        : equal && previous[j - 1];
            }
            boolean[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[tokens] ? MATCH : NO_MATCH;
    }
}
