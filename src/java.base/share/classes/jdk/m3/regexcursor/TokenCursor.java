// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Chunk cursor over interned token-id lanes (TIL/lexicon ids): each chunk is one {@code int[]}
 * lane, read in place. The symbol alphabet is the token id, so a regex over this cursor can only
 * match whole token spans.
 *
 * <p>Public factories borrow inner arrays by identity and skip empty lanes. Borrowed
 * arrays may change, so presence masks are recomputed when requested and the regex
 * matcher does not cache source-dependent gates for them. Only an internally owned,
 * never exposed projection lane is eligible for immutable precomputation.</p>
 */
public final class TokenCursor implements ChunkCursor, CursorFacts {
    private final int[][] lanes;
    private final int[] starts;
    private final int total;
    private final long presence;
    private final boolean stableFacts;
    private int lane;

    private TokenCursor(int[][] lanes, boolean stableFacts) {
        this.lanes = lanes;
        this.stableFacts = stableFacts;
        this.starts = new int[lanes.length + 1];
        long mask = 0L;
        for (int index = 0; index < lanes.length; index++) {
            starts[index + 1] = Math.addExact(starts[index], lanes[index].length);
            for (int id : lanes[index]) {
                if (id < 0) throw new IllegalArgumentException("negative token id " + id);
                mask |= CursorFacts.bit(id);
            }
        }
        this.total = starts[lanes.length];
        this.presence = mask;
    }

    /** Cursor over one token lane. */
    public static TokenCursor of(int[] tokens) {
        int[] checked = Objects.requireNonNull(tokens, "tokens");
        return new TokenCursor(checked.length == 0 ? new int[0][] : new int[][] {checked}, false);
    }

    /** Cursor over discontiguous lanes in order (empty lanes are skipped). */
    public static TokenCursor lanes(int[][] lanes) {
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
        return new TokenCursor(kept, false);
    }

    /**
     * Internal handoff for a fresh projected token lane that is never exposed or mutated.
     * The caller must own the array exclusively. Not for public borrowed arrays.
     */
    static TokenCursor ownedProjection(int[] freshIds) {
        int[] checked = Objects.requireNonNull(freshIds, "freshIds");
        return new TokenCursor(checked.length == 0 ? new int[0][] : new int[][] {checked}, true);
    }

    /** True only for a privately owned, never-mutated projected ID generation. */
    boolean stableFactsForMatcher() {
        return stableFacts;
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
        Objects.checkIndex((long) position, (long) total + 1L);
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
        if (stableFacts) return presence;
        long current = 0L;
        for (int[] segment : lanes) {
            for (int id : segment) {
                // A negative ID introduced after initial validation is outside the
                // token alphabet. UNKNOWN refuses any unsafe absence deduction.
                if (id < 0) return CursorFacts.UNKNOWN;
                current |= CursorFacts.bit(id);
            }
        }
        return current;
    }
}
