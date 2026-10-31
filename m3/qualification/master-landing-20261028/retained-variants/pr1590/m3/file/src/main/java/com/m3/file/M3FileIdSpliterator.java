// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.ConcurrentModificationException;
import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * Id-level spliterator over the pre-order positions {@code [position, end)} of an {@link M3FileWalk}.
 *
 * <p>{@code position} always lies outside every closed subtree. {@link #trySplit()} halves the range
 * and normalises the split point past the highest closed ancestor, so the prefix and the remainder
 * cover disjoint, contiguous parts of the encounter order. Each split owns its own probe. A structural
 * change of the tree (lazy expansion) fails the traversal fast.</p>
 */
final class M3FileIdSpliterator implements Spliterator.OfInt {
    private static final int BASE = ORDERED | DISTINCT | NONNULL;

    private final M3FileWalk walk;
    private final M3FileTree tree;
    private final M3FileWalk.Probe probe;
    private final int end;
    private final boolean sized;
    private final int generation;
    private int position;

    M3FileIdSpliterator(M3FileWalk walk, int position, int end, boolean sized) {
        this(walk, position, end, sized, walk.tree().generation());
    }

    private M3FileIdSpliterator(M3FileWalk walk, int position, int end, boolean sized, int generation) {
        this.walk = walk;
        this.tree = walk.tree();
        this.probe = walk.probe();
        this.position = position;
        this.end = end;
        this.sized = sized;
        this.generation = generation;
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
        checkGeneration();
        while (position < end) {
            int at = position;
            position = walk.advance(at);
            int id = tree.idAt(at);
            if (probe.accepts(id)) {
                action.accept(id);
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEachRemaining(IntConsumer action) {
        checkGeneration();
        while (position < end) {
            int at = position;
            position = walk.advance(at);
            int id = tree.idAt(at);
            if (probe.accepts(id)) action.accept(id);
        }
        checkGeneration();
    }

    @Override
    public Spliterator.OfInt trySplit() {
        checkGeneration();
        int low = position;
        if (end - low < 2) return null;
        int middle = walk.normalize(low + ((end - low) >>> 1));
        if (middle <= low || middle >= end) return null;
        position = middle;
        return new M3FileIdSpliterator(walk, low, middle, sized, generation);
    }

    @Override
    public long estimateSize() {
        return Math.max(0, end - position);
    }

    @Override
    public int characteristics() {
        return sized ? BASE | SIZED | SUBSIZED : BASE;
    }

    private void checkGeneration() {
        if (tree.generation() != generation) throw new ConcurrentModificationException("tree changed during traversal");
    }
}
