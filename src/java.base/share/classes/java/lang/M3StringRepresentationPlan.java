/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Objects;

/**
 * M3JDK internal representation planner. It owns no text, mask or cached payload.
 * Physical string storage remains M3String(owner, packedCoordinate).
 * All choices are advisory until corresponding semantics-preserving execution is admitted.
 */
final class M3StringRepresentationPlan {
    enum Kind {
        EMPTY, SCALAR, LATIN1, UTF16, SHARED_LEXICON, LOCAL_ATOM,
        RANGE, SUBSTRING, TUPLE, BALANCED_DAG, REPEAT, RUNS, BITMAP,
        MASKED, SPARSE_EDITS, REPLACEMENT, CASE_MASK, REVERSE_CURSOR,
        FORWARD_CURSOR, COMPILED_LITERAL, REGEX_EFFECT, PREFIX_INDEX,
        SUFFIX_INDEX, TOKENIZED, CODE_POINT, MAPPED
    }

    private M3StringRepresentationPlan() {}

    record Selection(Kind kind, int visibleLength, int firstSourceIndex,
                     int lastSourceIndex, int runs, long metadataBytes) {}

    /**
     * Snapshot only the *facts* of a caller-owned mutable mask; retain no mask array.
     * Single interval selections are represented by existing M3 coordinate ranges.
     */
    static Selection select(boolean[] selected) {
        Objects.requireNonNull(selected, "selected");
        int count = 0;
        int runs = 0;
        int first = -1;
        int last = -1;
        boolean previous = false;
        for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
                if (!previous) runs++;
                if (first == -1) first = i;
                last = i;
                count++;
            }
            previous = selected[i];
        }
        if (count == 0) return new Selection(Kind.EMPTY, 0, -1, -1, 0, 0L);
        if (count == selected.length)
            return new Selection(Kind.SCALAR, count, 0, last, 1, 0L);
        if (runs == 1)
            return new Selection(Kind.RANGE, count, first, last, 1, 0L);

        long span = (long) last - first + 1L;
        long words = (span + 63L) >>> 6;
        long runBytes = Math.multiplyExact((long) runs, 2L * Integer.BYTES);
        long bitmapBytes = Math.addExact(
                Math.multiplyExact(words, Long.BYTES),
                Math.multiplyExact(words + 1L, Integer.BYTES));
        return runBytes <= bitmapBytes
                ? new Selection(Kind.RUNS, count, first, last, runs, runBytes)
                : new Selection(Kind.BITMAP, count, first, last, runs, bitmapBytes);
    }

    static long collapseRange(int parentStart, int parentLength, int from, int to) {
        Objects.checkFromToIndex(from, to, parentLength);
        int start = Math.addExact(parentStart, from);
        return ((long) start << Integer.SIZE) | Integer.toUnsignedLong(to - from);
    }
}
