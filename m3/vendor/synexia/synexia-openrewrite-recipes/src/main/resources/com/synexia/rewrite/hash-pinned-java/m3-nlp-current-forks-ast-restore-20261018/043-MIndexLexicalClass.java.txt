// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Orthogonal entry classifications. A canonical symbol may occur in multiple
 * namespaces and each occurrence may carry multiple classes.
 */
public enum MIndexLexicalClass {
    WORD(0),
    PROPER_NOUN(1),
    PERSON(2),
    ORGANIZATION(3),
    PRODUCT(4),
    BRAND(5),
    LOCATION(6),
    ACRONYM(7),
    ABBREVIATION(8),
    TITLE(9),
    SCIENTIFIC(10),
    MATHEMATICAL(11),
    EQUATION(12),
    PHYSICS(13),
    CHEMICAL(14),
    TECHNICAL(15),
    EXPRESSION(16),
    EMOJI(17),
    PUNCTUATION(18),
    WHITESPACE(19),
    DYNAMIC(20);

    private final long mask;

    MIndexLexicalClass(int bit) {
        this.mask = 1L << bit;
    }

    public long mask() {
        return mask;
    }

    public static long mask(MIndexLexicalClass... classes) {
        long result = 0L;
        if (classes == null) return result;
        for (MIndexLexicalClass lexicalClass : classes) {
            if (lexicalClass != null) result |= lexicalClass.mask;
        }
        return result;
    }

    public static boolean has(long bits, MIndexLexicalClass lexicalClass) {
        return lexicalClass != null && (bits & lexicalClass.mask) != 0L;
    }
}
