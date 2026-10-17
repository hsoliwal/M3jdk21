// SPDX-License-Identifier: Apache-2.0
// Guava forwarding design adapted; see the collection decorator donor ledger.
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/** Direct primitive delegation. Adds no storage, policy or concurrency guarantee. */
public abstract class M3ForwardingLongList implements M3LongList {
    protected M3ForwardingLongList() { }
    protected abstract M3LongList delegate();

    @Override public boolean add(long value) { return delegate().add(value); }
    @Override public void add(int index, long value) { delegate().add(index, value); }
    @Override public boolean addIf(long value, LongPredicate predicate) { return delegate().addIf(value, predicate); }
    @Override public boolean addAll(long[] values) { return delegate().addAll(values); }
    @Override public long get(int index) { return delegate().get(index); }
    @Override public long set(int index, long value) { return delegate().set(index, value); }
    @Override public long removeAt(int index) { return delegate().removeAt(index); }
    @Override public boolean remove(long value) { return delegate().remove(value); }
    @Override public int indexOf(long value) { return delegate().indexOf(value); }
    @Override public int lastIndexOf(long value) { return delegate().lastIndexOf(value); }
    @Override public int filterInPlace(LongPredicate predicate) { return delegate().filterInPlace(predicate); }
    @Override public void sort() { delegate().sort(); }
    @Override public int binarySearch(long value) { return delegate().binarySearch(value); }
    @Override public int deduplicateSorted() { return delegate().deduplicateSorted(); }
    @Override public int capacity() { return delegate().capacity(); }
    @Override public void ensureCapacity(int minimumCapacity) { delegate().ensureCapacity(minimumCapacity); }
    @Override public void trimToSize() { delegate().trimToSize(); }
    @Override public void insert(int index, long value) { delegate().insert(index, value); }
    @Override public boolean removeValue(long value) { return delegate().removeValue(value); }
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
