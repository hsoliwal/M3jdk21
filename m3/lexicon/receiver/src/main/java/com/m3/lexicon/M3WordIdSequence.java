// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.List;

/**
 * Word-id sequence of a composite String: separator-lexicon token spans resolved to CWIDs, held as SoA
 * lanes ({@code wordIds}, {@code starts}, {@code ends}); CWID 0 marks an out-of-lexicon token. A span that
 * is exactly one lexicon atom takes the atom's id directly; other spans are resolved by comparing through
 * atom slices. No String[] and no joined char[] is produced. This is the input of word-id regex rows
 * (M3RXW001) and of token-sequence regex (REGEX_PRECOMPUTE_DESIGN.md §2e, §2f).
 */
public final class M3WordIdSequence {
    /** Resolves a composite span to a CWID (0 when absent). */
    @FunctionalInterface
    public interface Resolver {
        /** @return CWID of {@code text[start, end)} or 0 */
        int wordId(M3AtomCursor text, int start, int end);
    }

    private final int[] wordIds;
    private final int[] starts;
    private final int[] ends;

    private M3WordIdSequence(int[] wordIds, int[] starts, int[] ends) {
        this.wordIds = wordIds;
        this.starts = starts;
        this.ends = ends;
    }

    /** Tokenizes with a separator kind and resolves every token. */
    public static M3WordIdSequence of(M3SeparatorKind kind, List<? extends CharSequence> atoms,
            Resolver resolver) {
        M3SeparatorLexicon.Spans spans = M3SeparatorLexicon.tokens(kind, atoms);
        M3AtomCursor cursor = new M3AtomCursor(atoms);
        int[] ids = new int[spans.count()];
        for (int token = 0; token < ids.length; token++) {
            ids[token] = resolver.wordId(cursor, spans.starts()[token], spans.ends()[token]);
        }
        return new M3WordIdSequence(ids, spans.starts(), spans.ends());
    }

    /** Resolver over a strictly ascending word list (CWID = index + 1), comparing through atom slices. */
    public static Resolver sortedLexicon(List<? extends CharSequence> sortedWords) {
        List<? extends CharSequence> words = List.copyOf(sortedWords);
        return (text, start, end) -> {
            int low = 0;
            int high = words.size() - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int order = compare(words.get(middle), text, start, end);
                if (order == 0) {
                    return middle + 1;
                }
                if (order < 0) {
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            return 0;
        };
    }

    /** @return number of tokens */
    public int size() {
        return wordIds.length;
    }

    /** @return CWID of token k (0 = out of lexicon) */
    public int wordId(int token) {
        return wordIds[token];
    }

    /** @return start unit of token k */
    public int start(int token) {
        return starts[token];
    }

    /** @return end unit (exclusive) of token k */
    public int end(int token) {
        return ends[token];
    }

    /** @return a copy of the CWID lane */
    public int[] wordIds() {
        return wordIds.clone();
    }

    private static int compare(CharSequence word, M3AtomCursor text, int start, int end) {
        int common = Math.min(word.length(), end - start);
        for (int index = 0; index < common; index++) {
            int order = word.charAt(index) - text.charAt(start + index);
            if (order != 0) {
                return order;
            }
        }
        return word.length() - (end - start);
    }
}
