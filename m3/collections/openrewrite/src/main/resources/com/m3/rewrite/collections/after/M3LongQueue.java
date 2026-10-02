/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.LongQueue
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/** Primitive-long FIFO contract. */
public interface M3LongQueue extends M3LongCollection {
    boolean offer(long value);

    long peek();

    long poll();

    int capacity();

    default boolean tryPoll(long[] target, int targetIndex) {
        Objects.requireNonNull(target, "target");
        Objects.checkIndex(targetIndex, target.length);
        try {
            target[targetIndex] = poll();
            return true;
        } catch (NoSuchElementException empty) {
            return false;
        }
    }

    default long pollReserved() {
        for (;;) {
            try {
                return poll();
            } catch (NoSuchElementException notPublishedYet) {
                Thread.onSpinWait();
            }
        }
    }

    default int offerBatch(long[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int accepted = 0;
        while (accepted < length && offer(source[offset + accepted])) {
            accepted++;
        }
        return accepted;
    }

    default int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int removed = 0;
        while (removed < length && tryPoll(target, offset + removed)) {
            removed++;
        }
        return removed;
    }
}
