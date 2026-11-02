// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Chunk cursor over interned token-id lanes (TIL/lexicon ids): each chunk is one {@code int[]}
 * lane, read in place. The symbol alphabet is the token id, so a regex over this cursor can only
 * match whole token spans.
 *
 * <p>Lanes are shared, never copied; empty lanes are skipped. Presence facts (bit
 * {@code id & 63}) are OR-composed once so token patterns get the same precompute gate as text.</p>
 */
public final class M3TokenCursor implements M3ChunkCursor, M3CursorFacts {
    private final int[][] lanes;
    private final int[] starts;
    private final int total;
    private final long presence;
    private int lane;

    private M3TokenCursor(int[][] lanes) {
        this.lanes = lanes;
        this.starts = new int[lanes.length + 1];
        long mask = 0L;
        for (int index = 0; index < lanes.length; index++) {
            starts[index + 1] = Math.addExact(starts[index], lanes[index].length);
            for (int id : lanes[index]) {
                if (id < 0) throw new IllegalArgumentException("negative token id " + id);
                mask |= M3CursorFacts.bit(id);
            }
        }
        this.total = starts[lanes.length];
        this.presence = mask;
    }

    /** Cursor over one token lane. */
    public static M3TokenCursor of(int[] tokens) {
        int[] checked = Objects.requireNonNull(tokens, "tokens");
        return new M3TokenCursor(checked.length == 0 ? new int[0][] : new int[][] {checked});
    }

    /** Cursor over discontiguous lanes in order (empty lanes are skipped). */
    public static M3TokenCursor lanes(int[][] lanes) {
        if (lanes == null) {
            // Preserve the original JDK null diagnostic; no stream on valid inputs.
            Arrays.stream(lanes);
        }
        int nonempty = 0;
        for (int[] lane : lanes) {
            if (Objects.requireNonNull(lane).length > 0) nonempty++;
        }
        int[][] kept = new int[nonempty][];
        int next = 0;
        for (int[] lane : lanes) {
            if (lane.length > 0) kept[next++] = lane;
        }
        return new M3TokenCursor(kept);
    }

    @Override
    public int chunkLength() {
        return lanes.length == 0 ? 0 : lanes[lane].length;
    }

    @Override
    public int unit(int index) {
        return lanes[lane][index];
    }

    @Override
    public int offset() {
        return starts[lane];
    }

    @Override
    public int totalLength() {
        return total;
    }

    @Override
    public boolean advance() {
        if (lane + 1 >= lanes.length) return false;
        lane++;
        return true;
    }

    @Override
    public boolean backtrack() {
        if (lane == 0) return false;
        lane--;
        return true;
    }

    @Override
    public void seek(int position) {
        Objects.checkIndex(position, total + 1);
        if (lanes.length == 0) return;
        // The current lane is a locality hint, never a source of correctness.
        // After proving the search direction, the remaining binary search can
        // exclude the known current/neighbor prefixes or suffixes.
        int current = lane;
        int from = 0;
        int to = lanes.length;
        if (position >= starts[current]) {
            if (position < starts[current + 1]
                    || current == lanes.length - 1 && position == total) return;
            int next = current + 1;
            if (next < lanes.length && (position < starts[next + 1]
                    || next == lanes.length - 1 && position == total)) {
                lane = next;
                return;
            }
            from = next + 1;
        } else {
            int previous = current - 1;
            if (previous >= 0 && position >= starts[previous]) {
                lane = previous;
                return;
            }
            to = previous;
        }
        int found = Arrays.binarySearch(starts, from, to, position);
        lane = found >= 0 ? found : -found - 2;
    }

    @Override
    public long unitPresenceMask() {
        return presence;
    }
}
