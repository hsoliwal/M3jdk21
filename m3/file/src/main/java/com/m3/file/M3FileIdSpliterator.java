// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * Id-level spliterator over the pre-order range {@code [position, end)} of an {@link M3FileWalk}.
 *
 * <p>{@code position} always lies outside every closed subtree. {@link #trySplit()} halves the range
 * and normalises the split point past the highest closed ancestor, so the prefix and the remainder
 * cover disjoint, contiguous parts of the encounter order. Each split owns its own probe.</p>
 */
final class M3FileIdSpliterator implements Spliterator.OfInt {
    private static final int BASE = ORDERED | DISTINCT | NONNULL;

    private final M3FileWalk walk;
    private final M3FileWalk.Probe probe;
    private final int end;
    private final boolean sized;
    private int position;

    M3FileIdSpliterator(M3FileWalk walk, int position, int end, boolean sized) {
        this.walk = walk;
        this.probe = walk.probe();
        this.position = position;
        this.end = end;
        this.sized = sized;
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        while (position < end) {
            int id = position;
            position = walk.advance(id);
            if (probe.accepts(id)) {
                action.accept(id);
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEachRemaining(IntConsumer action) {
        while (position < end) {
            int id = position;
            position = walk.advance(id);
            if (probe.accepts(id)) action.accept(id);
        }
    }

    @Override
    public Spliterator.OfInt trySplit() {
        int low = position;
        if (end - low < 2) return null;
        int middle = walk.normalize(low + ((end - low) >>> 1));
        if (middle <= low || middle >= end) return null;
        position = middle;
        return new M3FileIdSpliterator(walk, low, middle, sized);
    }

    @Override
    public long estimateSize() {
        return Math.max(0, end - position);
    }

    @Override
    public int characteristics() {
        return sized ? BASE | SIZED | SUBSIZED : BASE;
    }
}
