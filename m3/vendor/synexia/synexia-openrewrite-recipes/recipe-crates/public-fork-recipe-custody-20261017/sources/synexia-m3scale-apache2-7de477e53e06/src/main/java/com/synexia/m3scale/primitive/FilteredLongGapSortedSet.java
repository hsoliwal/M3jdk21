/* SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3scale.primitive;

import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Filter-before-insertion facade over LongGapSortedSet.
 * The predicate is one collection-level object; there is no per-entry wrapper.
 */
public final class FilteredLongGapSortedSet {
    private final LongGapSortedSet delegate;
    private final LongPredicate filter;
    private long rejected;

    public FilteredLongGapSortedSet(LongPredicate filter) {
        this(new LongGapSortedSet(), filter);
    }

    public FilteredLongGapSortedSet(LongGapSortedSet delegate, LongPredicate filter) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    public boolean add(long value) {
        if (!filter.test(value)) {
            rejected++;
            return false;
        }
        return delegate.add(value);
    }

    public int addAll(long[] values, int from, int to) {
        if (from < 0 || to < from || to > values.length) throw new IndexOutOfBoundsException();
        int added = 0;
        for (int i = from; i < to; i++) if (add(values[i])) added++;
        return added;
    }

    public boolean contains(long value) { return delegate.contains(value); }
    public boolean remove(long value) { return delegate.remove(value); }
    public int size() { return delegate.size(); }
    public long get(int index) { return delegate.get(index); }
    public long rejectedCount() { return rejected; }
    public long[] toArray() { return delegate.toArray(); }
    public void clear() { delegate.clear(); rejected = 0L; }
}
