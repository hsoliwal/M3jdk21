// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Primitive-long FIFO contract with allocation-free concurrent hot paths.
 */
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

    /**
     * Consumes one externally reserved item. Concurrent packed queues override
     * this to spin on publication without transient exception allocation.
     */
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
        while (accepted < length && offer(source[offset + accepted])) accepted++;
        return accepted;
    }

    default int drainTo(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        int removed = 0;
        while (removed < length && tryPoll(target, offset + removed)) removed++;
        return removed;
    }
}
