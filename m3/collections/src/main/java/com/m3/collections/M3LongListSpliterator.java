// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;

/**
 * Owner-indirected spliterator for live indexed long sequences.
 *
 * <p>The descriptor retains the logical owner plus index range, never a replaceable backing
 * {@code long[]}. Capacity relocation may therefore replace physical storage without stranding
 * an already-created live view. Structural mutation during traversal remains the owner's contract.
 */
final class M3LongListSpliterator implements Spliterator.OfLong {
    private final M3LongList owner;
    private int index;
    private final int fence;

    M3LongListSpliterator(M3LongList owner, int index, int fence) {
        this.owner = Objects.requireNonNull(owner, "owner");
        Objects.checkFromToIndex(index, fence, owner.size());
        this.index = index;
        this.fence = fence;
    }

    @Override public OfLong trySplit() {
        int low = index;
        int middle = (low + fence) >>> 1;
        if (low >= middle) return null;
        index = middle;
        return new M3LongListSpliterator(owner, low, middle);
    }

    @Override public boolean tryAdvance(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        if (index >= fence) return false;
        action.accept(owner.get(index++));
        return true;
    }

    @Override public void forEachRemaining(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        while (index < fence) action.accept(owner.get(index++));
    }

    @Override public long estimateSize() { return fence - index; }

    @Override public int characteristics() {
        return ORDERED | SIZED | SUBSIZED;
    }
}
