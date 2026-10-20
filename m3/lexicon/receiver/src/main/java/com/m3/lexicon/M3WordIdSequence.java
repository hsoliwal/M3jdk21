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
    // Optional cold directory over this resolver's existing immutable String references.
    // Larger dictionaries and unusually long identity-hash clusters keep comparator lookup.
    private static final int MAX_IDENTITY_ROWS = 1 << 20;
    private static final int MAX_IDENTITY_PROBES = 64;

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
        Resolver fallback = (text, start, end) -> {
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
        int[] identityRows = identityRows(words);
        if (identityRows == null) {
            return fallback;
        }
        return (text, start, end) -> {
            if (text != null && start >= 0 && start < end
                    && end <= text.length()) {
                int atom = text.wholeAtom(start, end);
                if (atom >= 0 && text.atoms().get(atom) instanceof String word
                        && word.length() == end - start) {
                    int cwid = identityWordId(words, identityRows, word);
                    if (cwid != 0) {
                        return cwid;
                    }
                }
            }
            return fallback.wordId(text, start, end);
        };
    }

    /**
     * Admits only exact immutable, strictly ordered spellings. The table contains row
     * coordinates into the already-owned list, never another text payload or word wrapper.
     */
    private static int[] identityRows(List<? extends CharSequence> words) {
        int count = words.size();
        if (count == 0 || count > MAX_IDENTITY_ROWS) {
            return null;
        }
        String previous = null;
        for (CharSequence value : words) {
            if (!(value instanceof String word)
                    || (previous != null && previous.compareTo(word) >= 0)) {
                return null;
            }
            previous = word;
        }
        int capacity = 2;
        while (capacity < count * 2) {
            capacity <<= 1;
        }
        int[] rows = new int[capacity];
        int mask = capacity - 1;
        for (int row = 0; row < count; row++) {
            int slot = identitySlot(words.get(row), mask);
            int probes = 0;
            while (probes < MAX_IDENTITY_PROBES && rows[slot] != 0) {
                slot = (slot + 1) & mask;
                probes++;
            }
            if (probes == MAX_IDENTITY_PROBES) {
                return null;
            }
            rows[slot] = row + 1;
        }
        return rows;
    }

    private static int identityWordId(List<? extends CharSequence> words, int[] rows,
            String word) {
        int mask = rows.length - 1;
        int slot = identitySlot(word, mask);
        for (int probe = 0; probe < MAX_IDENTITY_PROBES; probe++) {
            int cwid = rows[slot];
            if (cwid == 0) {
                return 0;
            }
            if (words.get(cwid - 1) == word) {
                return cwid;
            }
            slot = (slot + 1) & mask;
        }
        return 0;
    }

    private static int identitySlot(Object value, int mask) {
        int hash = System.identityHashCode(value);
        return (hash ^ (hash >>> 16)) & mask;
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
