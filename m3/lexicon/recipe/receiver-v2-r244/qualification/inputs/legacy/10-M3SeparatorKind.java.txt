// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

/**
 * Separator lexicons. A BOUNDARY_BEFORE kind sets bit j when a token boundary sits before code unit j;
 * a SEPARATOR_UNIT kind sets bit j when unit j itself separates tokens.
 */
public enum M3SeparatorKind {
    /** camelCase: LOWERCASE_LETTER or DECIMAL_DIGIT_NUMBER followed by UPPERCASE_LETTER. */
    CAMEL(true),
    /** Commons Lang splitByCharacterTypeCamelCase: type changes, acronym run split before its last capital. */
    PASCAL(true),
    /** RFC 4180 delimiters and record separators outside quoted fields (quote state crosses atom seams). */
    CSV(false),
    /** Code points whose Character.getType is in a configurable punctuation class set. */
    PUNCT(false);

    /** Default PUNCT classes: every Unicode P* general category. */
    public static final int PUNCTUATION_CLASSES = (1 << Character.CONNECTOR_PUNCTUATION)
            | (1 << Character.DASH_PUNCTUATION) | (1 << Character.START_PUNCTUATION)
            | (1 << Character.END_PUNCTUATION) | (1 << Character.INITIAL_QUOTE_PUNCTUATION)
            | (1 << Character.FINAL_QUOTE_PUNCTUATION) | (1 << Character.OTHER_PUNCTUATION);

    private final boolean boundaryBefore;

    M3SeparatorKind(boolean boundaryBefore) {
        this.boundaryBefore = boundaryBefore;
    }

    /** @return true for BOUNDARY_BEFORE kinds, false for SEPARATOR_UNIT kinds */
    public boolean boundaryBefore() {
        return boundaryBefore;
    }
}
