// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derived word ids for folded spellings (sidecar M3FOLD01). {@code foldIds[cwid - 1]} is the CWID of the
 * folded spelling when that spelling is itself a lexicon row, else {@code DERIVED | classIndex}. For
 * {@link Kind#UNICODE_SIMPLE}, {@code a.equalsIgnoreCase(b)} holds exactly when the fold ids are equal.
 * Building is a cold path (once per lexicon generation); lookups are one array read.
 */
public final class M3LexiconFoldIds {
    /** High bit marks a derived (non-row) fold class. */
    public static final int DERIVED = 0x8000_0000;

    /** Fold rule. */
    public enum Kind {
        /** {@code A-Z -> a-z}; every other unit unchanged. */
        ASCII_LOWER,
        /** Per code point {@code toLowerCase(toUpperCase(cp))}, the String.equalsIgnoreCase rule. */
        UNICODE_SIMPLE
    }

    private M3LexiconFoldIds() {
    }

    /** Folds one spelling (cold path; allocates the folded key). */
    public static String fold(Kind kind, CharSequence word) {
        StringBuilder out = new StringBuilder(word.length());
        if (kind == Kind.ASCII_LOWER) {
            word.chars().forEach(c -> out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : (char) c));
        } else {
            word.codePoints().forEach(cp -> out.appendCodePoint(
                    Character.toLowerCase(Character.toUpperCase(cp))));
        }
        return out.toString();
    }

    /** Builds {@code foldIds} for a strictly ascending (UTF-16 order) word list; index = CWID - 1. */
    public static int[] build(List<? extends CharSequence> sortedWords, Kind kind) {
        int[] foldIds = new int[sortedWords.size()];
        Map<String, Integer> derived = new HashMap<>();
        for (int index = 0; index < foldIds.length; index++) {
            String key = fold(kind, sortedWords.get(index));
            int row = find(sortedWords, key);
            foldIds[index] = row >= 0 ? row + 1
                    : DERIVED | derived.computeIfAbsent(key, k -> derived.size());
        }
        return foldIds;
    }

    /** @return true when the two lexicon words are fold-equal (one compare). */
    public static boolean foldEquals(int[] foldIds, int leftCwid, int rightCwid) {
        return foldIds[leftCwid - 1] == foldIds[rightCwid - 1];
    }

    private static int find(List<? extends CharSequence> sortedWords, CharSequence key) {
        int low = 0;
        int high = sortedWords.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int order = CharSequence.compare(sortedWords.get(middle), key);
            if (order == 0) {
                return middle;
            }
            if (order < 0) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return -1;
    }
}
