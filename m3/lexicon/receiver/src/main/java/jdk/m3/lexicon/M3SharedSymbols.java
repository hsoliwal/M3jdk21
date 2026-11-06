// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

import java.util.List;

/**
 * Process-independent immutable symbols. Common separators never enter the overflow String table.
 *
 * <p>IDs are persistent format/API contracts. Append new symbols; never reorder existing entries.</p>
 */
//FLLDCIM: RELOCATE - donor blob 7788fc08cd7a at commit 3385d79a2968 (repository, path, URL and digests: m3/lexicon/recipe/interned-language-20261106/PROVENANCE.tsv); recipe m3/lexicon/recipe/interned-language-20261106/relocate.py v1
public final class M3SharedSymbols {
    private static final List<String> SYMBOLS = List.of(
        " ", "\n", "\t", "\r",
        ".", ",", "!", "?", ";", ":",
        "-", "—", "(", ")", "[", "]", "{", "}",
        "\"", "'", "/", "\\"
    );

    public static final long SPACE = M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, 0);
    public static final long NEWLINE = M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, 1);
    public static final long TAB = M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, 2);

    private M3SharedSymbols() { }

    public static long find(String text) {
        for (int i = 0; i < SYMBOLS.size(); i++) {
            if (SYMBOLS.get(i).equals(text)) return M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, i);
        }
        return -1L;
    }

    public static String resolve(long token) {
        if (!M3TokenId.isSymbol(token)) throw new IllegalArgumentException("not a shared-symbol token");
        int id = M3TokenId.wordId(token);
        if (id < 0 || id >= SYMBOLS.size()) throw new IndexOutOfBoundsException(id);
        return SYMBOLS.get(id);
    }

    public static int size() { return SYMBOLS.size(); }
}
