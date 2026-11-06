// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Process-frozen UTF-16 character-type transition decisions for the original
 * {@link M3WindowBoundarySummary} CAMEL and PASCAL window policies.
 *
 * <p>JDK21 {@code Character.getType(char)} values 0..30 and the end-sentinel -1
 * are addressable with five bits. The two 32^3 Boolean tables occupy 8 KiB,
 * use only primitive long arrays, and retain no node or map object. This is
 * identifier separation, not discretionary linguistic hyphenation.</p>
 */
final class M3CaseBoundaryPrecompute {
    private static final int WIDTH = 32;
    private static final int MAX_TYPE = Character.FINAL_QUOTE_PUNCTUATION;
    private static final long[] CAMEL = build(true);
    private static final long[] PASCAL = build(false);

    private M3CaseBoundaryPrecompute() {
    }

    static boolean boundary(M3SeparatorKind kind, int before, int at, int after) {
        if (before < -1 || at < -1 || after < -1
                || before > MAX_TYPE || at > MAX_TYPE || after > MAX_TYPE) {
            // Package-local direct callers of the original scalar helper may
            // supply synthetic category values; retain their exact semantics.
            return scalar(kind == M3SeparatorKind.CAMEL, before, at, after);
        }
        int index = index(before, at, after);
        long[] table = kind == M3SeparatorKind.CAMEL ? CAMEL : PASCAL;
        return (table[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    /** Original owner decision, used only for one-time table generation and invalid fallback. */
    static boolean scalar(boolean camel, int before, int at, int after) {
        if (camel) {
            return at == Character.UPPERCASE_LETTER
                    && (before == Character.LOWERCASE_LETTER || before == Character.DECIMAL_DIGIT_NUMBER);
        }
        boolean change = at != before
                && !(at == Character.LOWERCASE_LETTER && before == Character.UPPERCASE_LETTER);
        boolean acronymTail = after == Character.LOWERCASE_LETTER && at == Character.UPPERCASE_LETTER
                && before == Character.UPPERCASE_LETTER;
        return change || acronymTail;
    }

    private static int index(int before, int at, int after) {
        return ((before + 1) * WIDTH + (at + 1)) * WIDTH + (after + 1);
    }

    private static long[] build(boolean camel) {
        long[] table = new long[WIDTH * WIDTH * WIDTH / Long.SIZE];
        for (int before = -1; before <= MAX_TYPE; before++) {
            for (int at = -1; at <= MAX_TYPE; at++) {
                for (int after = -1; after <= MAX_TYPE; after++) {
                    if (scalar(camel, before, at, after)) {
                        int slot = index(before, at, after);
                        table[slot >>> 6] |= 1L << (slot & 63);
                    }
                }
            }
        }
        return table;
    }
}
