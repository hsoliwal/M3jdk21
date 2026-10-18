/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Read-only Collection facade over a replayable M3ObjectStream plan. */
public final class M3ObjectCollection<T> extends AbstractCollection<T> {
    private final M3ObjectStream<T> plan;

    M3ObjectCollection(M3ObjectStream<T> plan) {
        this.plan = Objects.requireNonNull(plan, "plan");
        if (!plan.replayable()) throw new IllegalArgumentException("a virtual collection requires a replayable plan");
    }

    public M3ObjectStream<T> plan() { return plan; }

    @Override public Iterator<T> iterator() {
        Stream<T> stream = plan.stream();
        final Iterator<T> delegate;
        try { delegate = stream.iterator(); }
        catch (RuntimeException | Error failure) { stream.close(); throw failure; }
        return new Iterator<>() {
            private boolean closed;
            private void close() {
                if (!closed) { closed = true; stream.close(); }
            }
            @Override public boolean hasNext() {
                if (closed) return false;
                try {
                    boolean more = delegate.hasNext();
                    if (!more) close();
                    return more;
                } catch (RuntimeException | Error failure) { close(); throw failure; }
            }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                try { return delegate.next(); }
                catch (RuntimeException | Error failure) { close(); throw failure; }
            }
            @Override public void remove() { throw new UnsupportedOperationException("read-only M3 virtual collection"); }
        };
    }

    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, plan.count()); }

    @Override public boolean isEmpty() {
        try (Stream<T> source = plan.stream()) { return !source.iterator().hasNext(); }
    }

    @Override public Stream<T> stream() { return plan.stream(); }
    @Override public boolean add(T value) { throw immutable(); }
    @Override public boolean addAll(Collection<? extends T> values) { throw immutable(); }
    @Override public boolean remove(Object value) { throw immutable(); }
    @Override public boolean removeAll(Collection<?> values) { throw immutable(); }
    @Override public boolean retainAll(Collection<?> values) { throw immutable(); }
    @Override public boolean removeIf(Predicate<? super T> filter) { throw immutable(); }
    @Override public void clear() { throw immutable(); }

    private static UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("read-only M3 virtual collection");
    }
}
