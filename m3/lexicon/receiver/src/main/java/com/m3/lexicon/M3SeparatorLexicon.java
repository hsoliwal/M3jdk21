// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.util.Arrays;
import java.util.List;

/**
 * Facade of the separator lexicons over a composite String given as atoms. Summaries are computed per
 * atom and composed by a balanced reduction (the shape of tuple nodes); token spans are read from the
 * root bits. Nothing here joins the atoms: split and tokenize never flatten.
 */
public final class M3SeparatorLexicon {
    /** Token spans as SoA lanes: token k is {@code [starts[k], ends[k])}. */
    public record Spans(int[] starts, int[] ends, int count) {
    }

    private M3SeparatorLexicon() {
    }

    /** Root bits for a kind: boundary-before bits (CAMEL, PASCAL) or separator-unit bits (CSV, PUNCT). */
    public static long[] bits(M3SeparatorKind kind, List<? extends CharSequence> atoms) {
        switch (kind) {
            case CSV:
                return csv(atoms, 0, atoms.size()).separators();
            case PUNCT:
                return punct(atoms, 0, atoms.size()).finish();
            default:
                return window(kind, atoms, 0, atoms.size()).finish();
        }
    }

    /** CSV root summary (separators, record ends, refusal). */
    public static M3CsvBoundarySummary csv(List<? extends CharSequence> atoms) {
        return csv(atoms, 0, atoms.size());
    }

    /** Token spans: segments between boundaries, or non-empty runs between separator units. */
    public static Spans tokens(M3SeparatorKind kind, List<? extends CharSequence> atoms) {
        M3AtomCursor cursor = new M3AtomCursor(atoms);
        long[] bits = bits(kind, atoms);
        int n = cursor.length();
        int[] starts = new int[n + 1];
        int[] ends = new int[n + 1];
        int count = kind.boundaryBefore() ? segments(bits, n, starts, ends) : runs(bits, n, starts, ends);
        return new Spans(Arrays.copyOf(starts, count), Arrays.copyOf(ends, count), count);
    }

    private static int segments(long[] bits, int n, int[] starts, int[] ends) {
        int count = 0;
        int start = 0;
        for (int j = 1; j <= n; j++) {
            if (j == n || M3BitLanes.get(bits, j)) {
                starts[count] = start;
                ends[count++] = j;
                start = j;
            }
        }
        return count;
    }

    private static int runs(long[] bits, int n, int[] starts, int[] ends) {
        int count = 0;
        int start = -1;
        for (int j = 0; j <= n; j++) {
            boolean separator = j == n || M3BitLanes.get(bits, j);
            if (separator && start >= 0) {
                starts[count] = start;
                ends[count++] = j;
                start = -1;
            } else if (!separator && start < 0) {
                start = j;
            }
        }
        return count;
    }

    private static M3WindowBoundarySummary window(M3SeparatorKind kind, List<? extends CharSequence> atoms,
            int from, int to) {
        if (to - from <= 1) {
            return M3WindowBoundarySummary.of(kind, to == from ? "" : atoms.get(from));
        }
        int middle = (from + to) >>> 1;
        return window(kind, atoms, from, middle).combine(window(kind, atoms, middle, to));
    }

    private static M3CsvBoundarySummary csv(List<? extends CharSequence> atoms, int from, int to) {
        if (to - from <= 1) {
            return M3CsvBoundarySummary.of(to == from ? "" : atoms.get(from));
        }
        int middle = (from + to) >>> 1;
        return csv(atoms, from, middle).combine(csv(atoms, middle, to));
    }

    private static M3PunctBoundarySummary punct(List<? extends CharSequence> atoms, int from, int to) {
        if (to - from <= 1) {
            return M3PunctBoundarySummary.of(M3SeparatorKind.PUNCTUATION_CLASSES,
                    to == from ? "" : atoms.get(from));
        }
        int middle = (from + to) >>> 1;
        return punct(atoms, from, middle).combine(punct(atoms, middle, to));
    }
}
