// SPDX-License-Identifier: Apache-2.0
// Guava forwarding design adapted; see the collection decorator donor ledger.
package com.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/** Live read-only primitive facade. Backing aliases remain live. */
public final class M3ReadOnlyLongList extends M3ForwardingLongList {
    private final M3LongList owner;
    public M3ReadOnlyLongList(M3LongList owner) {
        this.owner = java.util.Objects.requireNonNull(owner, "owner");
    }
    @Override protected M3LongList delegate() { return owner; }

    @Override public boolean add(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void add(int index, long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean addIf(long value, LongPredicate predicate) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean addAll(long[] values) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long set(int index, long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public long removeAt(int index) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean remove(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int filterInPlace(LongPredicate predicate) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void sort() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public int deduplicateSorted() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void ensureCapacity(int minimumCapacity) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void trimToSize() { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void insert(int index, long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public boolean removeValue(long value) { throw new UnsupportedOperationException("read-only collection"); }
    @Override public void clear() { throw new UnsupportedOperationException("read-only collection"); }
}
