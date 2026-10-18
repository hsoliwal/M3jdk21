// SPDX-License-Identifier: Apache-2.0
// Guava forwarding design adapted; see the collection decorator donor ledger.
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/** Live read-only primitive facade. Backing aliases remain live. */
public final class M3ReadOnlyLongQueue extends M3ForwardingLongQueue {
    private final M3LongQueue owner;
    public M3ReadOnlyLongQueue(M3LongQueue owner) {
        this.owner = java.util.Objects.requireNonNull(owner, "owner");
    }
    @Override protected M3LongQueue delegate() { return owner; }

    @Override public boolean offer(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long poll() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean tryPoll(long[] target, int targetIndex) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long pollReserved() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int offerBatch(long[] source, int offset, int length) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int drainTo(long[] target, int offset, int length) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void clear() { throw new UnsupportedOperationException("read-only collection"); }
}
