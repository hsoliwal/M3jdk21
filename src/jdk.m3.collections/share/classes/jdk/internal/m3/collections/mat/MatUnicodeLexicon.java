/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.mat;

/**
 * Direct-address Unicode scalar lexicon.
 *
 * <p>The scalar code point is the ID. Metadata comes from the JDK Unicode tables and arithmetic,
 * so no per-code-point object or lookup table is retained by MAT.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatUnicodeLexicon.java}
 * blob {@code a3b2cfc6264cb98e4ca6d1807c8a80403fa64896} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatUnicodeLexicon {
    public static final int LETTER = 1 << 0;
    public static final int DIGIT = 1 << 1;
    public static final int WHITESPACE = 1 << 2;
    public static final int UPPERCASE = 1 << 3;
    public static final int LOWERCASE = 1 << 4;
    public static final int TITLECASE = 1 << 5;
    public static final int IDEOGRAPHIC = 1 << 6;
    public static final int IDENTIFIER_START = 1 << 7;
    public static final int IDENTIFIER_PART = 1 << 8;
    public static final int SUPPLEMENTARY = 1 << 9;
    public static final int MARK = 1 << 10;

    private MatUnicodeLexicon() { }

    public static int idOf(int codePoint) {
        requireScalar(codePoint);
        return codePoint;
    }

    public static int codePointOf(int id) {
        requireScalar(id);
        return id;
    }

    public static int utf16Length(int id) {
        requireScalar(id);
        return Character.charCount(id);
    }

    public static int utf8Length(int id) {
        requireScalar(id);
        if (id <= 0x7f) {
            return 1;
        }
        if (id <= 0x7ff) {
            return 2;
        }
        if (id <= 0xffff) {
            return 3;
        }
        return 4;
    }

    public static int generalCategory(int id) {
        requireScalar(id);
        return Character.getType(id);
    }

    public static int scriptOrdinal(int id) {
        requireScalar(id);
        return Character.UnicodeScript.of(id).ordinal();
    }

    public static String name(int id) {
        requireScalar(id);
        return Character.getName(id);
    }

    public static int flags(int id) {
        requireScalar(id);
        int flags = 0;
        if (Character.isLetter(id)) {
            flags |= LETTER;
        }
        if (Character.isDigit(id)) {
            flags |= DIGIT;
        }
        if (Character.isWhitespace(id)) {
            flags |= WHITESPACE;
        }
        if (Character.isUpperCase(id)) {
            flags |= UPPERCASE;
        }
        if (Character.isLowerCase(id)) {
            flags |= LOWERCASE;
        }
        if (Character.isTitleCase(id)) {
            flags |= TITLECASE;
        }
        if (Character.isIdeographic(id)) {
            flags |= IDEOGRAPHIC;
        }
        if (Character.isUnicodeIdentifierStart(id)) {
            flags |= IDENTIFIER_START;
        }
        if (Character.isUnicodeIdentifierPart(id)) {
            flags |= IDENTIFIER_PART;
        }
        if (Character.isSupplementaryCodePoint(id)) {
            flags |= SUPPLEMENTARY;
        }
        int type = Character.getType(id);
        if (type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK) {
            flags |= MARK;
        }
        return flags;
    }

    public static boolean isScalar(int codePoint) {
        return Character.isValidCodePoint(codePoint)
                && (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE);
    }

    private static void requireScalar(int codePoint) {
        if (!isScalar(codePoint)) {
            throw new IllegalArgumentException(
                    "Not a Unicode scalar value: U+" + Integer.toHexString(codePoint));
        }
    }
}
