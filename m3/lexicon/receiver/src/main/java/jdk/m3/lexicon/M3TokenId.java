// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

/**
 * Stable packed token ID: upper 16 bits hold language, low 32 bits hold dictionary-local word ID.
 * Language 65534 is the immutable global symbol table; 65535 is process-local overflow.
 */
//FLLDCIM: RELOCATE - donor blob a0739662f208 at commit 3385d79a2968 (repository, path, URL and digests: m3/lexicon/recipe/interned-language-20261106/PROVENANCE.tsv); recipe m3/lexicon/recipe/interned-language-20261106/relocate.py v1
public final class M3TokenId {
    public static final int SYMBOL_LANGUAGE_ID = 0xfffe;
    public static final int OVERFLOW_LANGUAGE_ID = 0xffff;

    private M3TokenId() { }

    public static long pack(int languageId, int wordId) {
        if (languageId < 0 || languageId > 0xffff) throw new IllegalArgumentException("languageId: 0..65535");
        if (wordId < 0) throw new IllegalArgumentException("wordId must be non-negative");
        return ((long) languageId << 32) | Integer.toUnsignedLong(wordId);
    }

    public static int languageId(long token) {
        return (int) ((token >>> 32) & 0xffffL);
    }

    public static int wordId(long token) {
        return (int) token;
    }

    public static boolean isSymbol(long token) {
        return languageId(token) == SYMBOL_LANGUAGE_ID;
    }

    public static boolean isOverflow(long token) {
        return languageId(token) == OVERFLOW_LANGUAGE_ID;
    }
}
