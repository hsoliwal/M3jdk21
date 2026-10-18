// SPDX-License-Identifier: Apache-2.0
// Guava forwarding design adapted; see the collection decorator donor ledger.
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/** Direct primitive delegation. Adds no storage, policy or concurrency guarantee. */
public abstract class M3ForwardingLongQueue implements M3LongQueue {
    protected M3ForwardingLongQueue() { }
    protected abstract M3LongQueue delegate();

    @Override public boolean offer(long value) { return delegate().offer(value); }
    @Override public long peek() { return delegate().peek(); }
    @Override public long poll() { return delegate().poll(); }
    @Override public int capacity() { return delegate().capacity(); }
    @Override public boolean tryPoll(long[] target, int targetIndex) { return delegate().tryPoll(target, targetIndex); }
    @Override public long pollReserved() { return delegate().pollReserved(); }
    @Override public int offerBatch(long[] source, int offset, int length) { return delegate().offerBatch(source, offset, length); }
    @Override public int drainTo(long[] target, int offset, int length) { return delegate().drainTo(target, offset, length); }
    @Override public int size() { return delegate().size(); }
    @Override public boolean contains(long value) { return delegate().contains(value); }
    @Override public void clear() { delegate().clear(); }
    @Override public M3LongIterator iterator() { return delegate().iterator(); }
    @Override public long[] toArray() { return delegate().toArray(); }
    @Override public boolean isEmpty() { return delegate().isEmpty(); }
    @Override public void forEachLong(LongConsumer consumer) { delegate().forEachLong(consumer); }
    @Override public java.util.Spliterator.OfLong longSpliterator() { return delegate().longSpliterator(); }
    @Override public java.util.stream.LongStream longStream() { return delegate().longStream(); }
    @Override public java.util.stream.LongStream parallelLongStream() { return delegate().parallelLongStream(); }
}
