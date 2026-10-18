// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable token geometry. The payload is four parallel primitive arrays; a slice
 * retains only the arrays and a half-open token range, never per-token objects.
 * Source coordinates use long UTF-16 offsets so callers can index composite text.
 */
public final class M3TokenTape {
    private final int[] ids;
    private final int[] kinds;
    private final long[] starts;
    private final long[] ends;
    private final int from;
    private final int to;

    private M3TokenTape(int[] ids, int[] kinds, long[] starts, long[] ends, int from, int to) {
        this.ids = ids;
        this.kinds = kinds;
        this.starts = starts;
        this.ends = ends;
        this.from = from;
        this.to = to;
    }

    public static M3TokenTape capture(int[] ids, int[] kinds, long[] starts, long[] ends) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(kinds, "kinds");
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(ends, "ends");
        if (ids.length != kinds.length || ids.length != starts.length || ids.length != ends.length) {
            throw new IllegalArgumentException("token lanes must have equal lengths");
        }
        long previousEnd = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] < 0 || starts[i] < previousEnd || ends[i] < starts[i]) {
                throw new IllegalArgumentException("invalid token ID or ordered UTF-16 range at " + i);
            }
            previousEnd = ends[i];
        }
        return new M3TokenTape(ids.clone(), kinds.clone(), starts.clone(), ends.clone(), 0, ids.length);
    }

    public int size() { return to - from; }
    public int idAt(int index) { return ids[from + Objects.checkIndex(index, size())]; }
    public int kindMaskAt(int index) { return kinds[from + Objects.checkIndex(index, size())]; }
    public long startUtf16At(int index) { return starts[from + Objects.checkIndex(index, size())]; }
    public long endUtf16At(int index) { return ends[from + Objects.checkIndex(index, size())]; }

    /** Metadata-only view; no primitive payload is copied or retokenized. */
    public M3TokenTape slice(int begin, int end) {
        Objects.checkFromToIndex(begin, end, size());
        return new M3TokenTape(ids, kinds, starts, ends, from + begin, from + end);
    }

    /** Boundary position for an empty token match, including the end of a tape. */
    public long boundaryUtf16(int tokenBoundary) {
        Objects.checkFromToIndex(tokenBoundary, tokenBoundary, size());
        return tokenBoundary < size() ? starts[from + tokenBoundary]
                : size() == 0 ? 0 : ends[to - 1];
    }

    /** Returns detached primitive token IDs only on explicit API request. */
    public int[] idsCopy() { return Arrays.copyOfRange(ids, from, to); }
}
