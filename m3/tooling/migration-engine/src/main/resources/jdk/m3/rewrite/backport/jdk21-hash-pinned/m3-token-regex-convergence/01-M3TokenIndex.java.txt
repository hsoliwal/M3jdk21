/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Token occurrence index of one source snapshot, stored as parallel primitive lanes.
 *
 * <p>Token i covers the half-open source range [start(i), end(i)) in UTF-16 units of the same
 * M3String the lanes were derived from. The index never holds or rebuilds that text: it is the
 * only owner of lexical boundaries, so storage atoms of the String never split or join tokens.
 * Lanes: kind (lexer or M3AST kind id), lexeme (canonical lexeme id, 0 = not a lexicon word),
 * start/end, flags (bit 0 TRIVIA, bit 1 NEWLINE, other bits caller attributes), binding (M3AST
 * binding id, 0 = none) and named int attribute columns. Lanes are defensive snapshots; no
 * per-token object exists. Lineage: Synexia IndexedTokens (MIndex) to M3TokenIndex; plain text
 * lanes come from the LEXICONS separator masks (word ids plus boundary offsets).
 */
public final class M3TokenIndex {
    /** Whitespace or comment token: skipped by the significant view. */
    public static final long TRIVIA = 1L;
    /** Line terminator token. */
    public static final long NEWLINE = 1L << 1;

    private final long generation;
    private final int count;
    private final int[] kinds;
    private final int[] lexemes;
    private final long[] starts;
    private final long[] ends;
    private final long[] flags;
    private final int[] bindings;
    private final String[] attrNames;
    private final int[][] attrs;
    private volatile int[] significant;

    private M3TokenIndex(long generation, int[] kinds, int[] lexemes, long[] starts, long[] ends,
            long[] flags, int[] bindings, String[] attrNames, int[][] attrs) {
        this.generation = generation;
        this.count = kinds.length;
        this.kinds = kinds;
        this.lexemes = lexemes;
        this.starts = starts;
        this.ends = ends;
        this.flags = flags;
        this.bindings = bindings;
        this.attrNames = attrNames;
        this.attrs = attrs;
    }

    /** Index without bindings or attribute columns. */
    public static M3TokenIndex of(long generation, int[] kinds, int[] lexemes,
            long[] starts, long[] ends, long[] flags) {
        return of(generation, kinds, lexemes, starts, ends, flags, null, null, null);
    }

    /**
     * Full index. {@code bindings}, {@code attrNames} and {@code attrColumns} may be null.
     *
     * @throws IllegalArgumentException on mismatched lanes, negative or reversed ranges, or
     *         tokens that overlap or are out of source order
     */
    public static M3TokenIndex of(long generation, int[] kinds, int[] lexemes, long[] starts,
            long[] ends, long[] flags, int[] bindings, String[] attrNames, int[][] attrColumns) {
        int n = Objects.requireNonNull(kinds, "kinds").length;
        requireLength(Objects.requireNonNull(lexemes, "lexemes").length, n);
        requireLength(Objects.requireNonNull(starts, "starts").length, n);
        requireLength(Objects.requireNonNull(ends, "ends").length, n);
        requireLength(Objects.requireNonNull(flags, "flags").length, n);
        int[] bind = bindings == null ? new int[n] : bindings.clone();
        requireLength(bind.length, n);
        checkRanges(starts, ends);
        String[] names = attrNames == null ? new String[0] : attrNames.clone();
        int[][] columns = copyColumns(names, attrColumns, n);
        return new M3TokenIndex(generation, kinds.clone(), lexemes.clone(), starts.clone(),
                ends.clone(), flags.clone(), bind, names, columns);
    }

    private static void requireLength(int length, int expected) {
        if (length != expected) {
            throw new IllegalArgumentException("mismatched token lanes");
        }
    }

    private static void checkRanges(long[] starts, long[] ends) {
        long previousEnd = 0;
        for (int i = 0; i < starts.length; i++) {
            if (starts[i] < previousEnd || ends[i] < starts[i]) {
                throw new IllegalArgumentException("token " + i + " overlaps, is reversed or out of order");
            }
            previousEnd = ends[i];
        }
    }

    private static int[][] copyColumns(String[] names, int[][] columns, int n) {
        int c = names.length;
        if (columns == null ? c != 0 : columns.length != c) {
            throw new IllegalArgumentException("attribute names and columns differ");
        }
        int[][] copy = new int[c][];
        for (int k = 0; k < c; k++) {
            Objects.requireNonNull(names[k], "attribute name");
            copy[k] = Objects.requireNonNull(columns[k], "attribute column").clone();
            requireLength(copy[k].length, n);
        }
        return copy;
    }

    /** Canonical-store generation: cache key and stale-index refusal guard. */
    public long generation() { return generation; }
    /** Number of tokens in the lossless view. */
    public int count() { return count; }
    public int kindAt(int token) { return kinds[Objects.checkIndex(token, count)]; }
    public int lexemeAt(int token) { return lexemes[Objects.checkIndex(token, count)]; }
    public long startAt(int token) { return starts[Objects.checkIndex(token, count)]; }
    public long endAt(int token) { return ends[Objects.checkIndex(token, count)]; }
    public long flagsAt(int token) { return flags[Objects.checkIndex(token, count)]; }
    public int bindingAt(int token) { return bindings[Objects.checkIndex(token, count)]; }

    /** Column index of a named attribute, or -1. */
    public int attributeColumn(String name) {
        Objects.requireNonNull(name, "name");
        for (int k = 0; k < attrNames.length; k++) {
            if (attrNames[k].equals(name)) {
                return k;
            }
        }
        return -1;
    }

    public int attributeAt(int column, int token) {
        return attrs[Objects.checkIndex(column, attrs.length)][Objects.checkIndex(token, count)];
    }

    /**
     * Source start of token range [s, e): start(s) when s is a token, else the end of the last
     * token (empty range at the end of the tape), else 0 for an empty index.
     */
    public long spanStart(int s, int e) {
        Objects.checkFromToIndex(s, e, count);
        if (s < count) {
            return starts[s];
        }
        return count == 0 ? 0 : ends[count - 1];
    }

    /** Source end of token range [s, e): end(e - 1) for a non-empty range, else spanStart. */
    public long spanEnd(int s, int e) {
        Objects.checkFromToIndex(s, e, count);
        return e > s ? ends[e - 1] : spanStart(s, e);
    }

    /** Number of tokens whose TRIVIA bit is clear. */
    public int significantCount() {
        return significantView().length;
    }

    /** Lossless position of the k-th significant token. */
    public int significantAt(int k) {
        int[] view = significantView();
        return view[Objects.checkIndex(k, view.length)];
    }

    private int[] significantView() {
        int[] view = significant;
        if (view == null) {
            int n = 0;
            int[] all = new int[count];
            for (int i = 0; i < count; i++) {
                if ((flags[i] & TRIVIA) == 0) {
                    all[n++] = i;
                }
            }
            view = java.util.Arrays.copyOf(all, n);
            significant = view;
        }
        return view;
    }

    /** Retained primitive payload bytes (object headers excluded). */
    public long retainedPrimitiveBytes() {
        long perToken = 3L * Integer.BYTES + 3L * Long.BYTES + (long) attrs.length * Integer.BYTES;
        return perToken * count;
    }
}
