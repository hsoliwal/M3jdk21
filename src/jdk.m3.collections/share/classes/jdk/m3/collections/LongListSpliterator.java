/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;

/**
 * Owner-indirected spliterator for live indexed long sequences.
 *
 * <p>The descriptor retains the logical owner plus index range, never a replaceable backing
 * {@code long[]}. Capacity relocation may therefore replace physical storage without stranding
 * an already-created live view. Structural mutation during traversal remains the owner's contract: an owner
 * that counts structural modifications passes its count probe, and the spliterator then fails fast
 * ({@link ConcurrentModificationException}) like the owner's iterators, with the count captured at creation.
 */
final class LongListSpliterator implements Spliterator.OfLong {
    private final LongList owner;
    private int index;
    private final int fence;
    private final IntSupplier modCount;
    private final int expectedModCount;

    LongListSpliterator(LongList owner, int index, int fence) {
        this(owner, index, fence, null, 0);
    }

    LongListSpliterator(LongList owner, int index, int fence, IntSupplier modCount) {
        this(owner, index, fence, modCount, modCount == null ? 0 : modCount.getAsInt());
    }

    private LongListSpliterator(LongList owner, int index, int fence, IntSupplier modCount,
            int expectedModCount) {
        this.owner = Objects.requireNonNull(owner, "owner");
        Objects.checkFromToIndex(index, fence, owner.size());
        this.index = index;
        this.fence = fence;
        this.modCount = modCount;
        this.expectedModCount = expectedModCount;
    }

    @Override
    public OfLong trySplit() {
        int low = index;
        int middle = (low + fence) >>> 1;
        if (low >= middle) {
            return null;
        }
        index = middle;
        return new LongListSpliterator(owner, low, middle, modCount, expectedModCount);
    }

    @Override
    public boolean tryAdvance(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        checkForComodification();
        if (index >= fence) {
            return false;
        }
        action.accept(owner.get(index++));
        return true;
    }

    @Override
    public void forEachRemaining(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        checkForComodification();
        while (index < fence) {
            action.accept(owner.get(index++));
        }
        checkForComodification();
    }

    @Override
    public long estimateSize() {
        return fence - index;
    }

    @Override
    public int characteristics() {
        return ORDERED | SIZED | SUBSIZED;
    }

    private void checkForComodification() {
        if (modCount != null && modCount.getAsInt() != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }
}
