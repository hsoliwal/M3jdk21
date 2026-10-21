// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;

/** Static syntax tables and lane helpers shared by the UTF-16 and token pattern parsers. */
final class M3RegexSyntax {
    /** {@code \d} (JDK default, ASCII). */
    static final int[] DIGIT = {'0', '9'};
    /** {@code \w} (JDK default, ASCII). */
    static final int[] WORD = {'a', 'z', 'A', 'Z', '_', '_', '0', '9'};
    /** {@code \s} = {@code [ \t\n\x0B\f\r]}. */
    static final int[] SPACE = {' ', ' ', '\t', '\r'};
    /** Units {@code .} excludes without DOTALL (stock CharPredicates dot). */
    static final int[] LINE_TERMINATORS = {'\n', '\n', '\r', '\r', 0x85, 0x85, 0x2028, 0x2029};

    private M3RegexSyntax() {}

    /** Ranges of {@code \d \w \s} (either case), else {@code null}. */
    static int[] predefined(char c) {
        switch (Character.toLowerCase(c)) {
            case 'd':
                return DIGIT;
            case 'w':
                return WORD;
            case 's':
                return SPACE;
            default:
                return null;
        }
    }

    /** Complement of {@code pairs} over {@code [0, alphabetMax]} as sorted, merged pairs. */
    static int[] complement(int[] pairs, int alphabetMax) {
        M3RegexAst scratch = new M3RegexAst(alphabetMax);
        int id = scratch.addClass(pairs, pairs.length / 2, true);
        int start = scratch.classStart[id];
        return Arrays.copyOfRange(scratch.ranges, start, start + 2 * scratch.classPairs[id]);
    }

    /** Repeat count of 1..4 decimal digits, at most {@code max}; else refused. */
    static int number(String digits, int max) {
        if (digits.isEmpty() || digits.length() > 4 || !digits.chars().allMatch(Character::isDigit)) {
            throw new M3RegexRefusal("count");
        }
        int value = Integer.parseInt(digits);
        if (value > max) throw new M3RegexRefusal("count");
        return value;
    }

    /** {@code items}, doubled when full. */
    static int[] grow(int[] items, int count) {
        return count < items.length ? items : Arrays.copyOf(items, items.length * 2);
    }
}
