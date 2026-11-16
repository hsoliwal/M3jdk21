/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Immutable set with first-encounter ordinals and a private object-to-int index.
 *
 * <p>Admission rejects nulls and collapses equal duplicates, retaining the first
 * representative as Set.copyOf/ImmutableSet.copyOf permit. Null queries find no
 * member. Exports are detached; set and iterator facades preserve encounter order
 * and reject all mutation. Payload and lookup storage use existing array owners,
 * with no retained per-element node or entry.
 *
 * <p>This receives the selected JDK/Guava immutable-set contracts into the
 * existing internal M3 owner. It does not implement the Guava interface or claim
 * Guava's separate hash-flood mitigation or complete provider integration. The
 * original JEP269/ImmutableSet lineage and compatibility descriptor are retained.
 */
public final class FrozenSet<E> {
    static final long STATE_FROZEN = 1L << 0;
    private static final long[] FROZEN_STATE = {STATE_FROZEN};
    private static final Object[] EMPTY = new Object[0];

    private final Object[] valueLane;
    private final ObjectIntHashMap<Object> ordinals;
    private final long[] stateMaskWords = FROZEN_STATE;
    private final int size;

    public FrozenSet() { this(EMPTY, new ObjectIntHashMap<>(0)); }

    private FrozenSet(Object[] values, ObjectIntHashMap<Object> index) {
        valueLane = values;
        ordinals = index;
        size = values.length;
    }

    /** Retained compatibility descriptor; lookup lanes are owned by ObjectIntHashMap. */
    public static String scaffoldDescriptor() {
        return "M3FrozenSet|FROZEN_SET|OBJECT|FROZEN|Object[] valueLane;int[] hashIndexLane";
    }

    public static <E> FrozenSet<E> copyOf(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        ArrayList<Object> values = new ArrayList<>();
        ObjectIntHashMap<Object> index = new ObjectIntHashMap<>(0);
        for (E element : source) {
            Objects.requireNonNull(element, "element");
            if (index.getOrDefault(element, -1) < 0) {
                index.put(element, values.size());
                values.add(element);
            }
        }
        return new FrozenSet<>(values.toArray(), index);
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public boolean contains(Object element) {
        return element != null && ordinals.getOrDefault(element, -1) >= 0;
    }

    @SuppressWarnings("unchecked")
    public E get(int ordinal) {
        return (E) valueLane[Objects.checkIndex(ordinal, size)];
    }

    public Object[] toArray() { return valueLane.clone(); }

    public Set<E> asSet() {
        return Collections.unmodifiableSet(new AbstractSet<>() {
            @Override public int size() { return size; }
            @Override public boolean contains(Object element) { return FrozenSet.this.contains(element); }
            @Override public Iterator<E> iterator() { return FrozenSet.this.iterator(); }
            @Override public Object[] toArray() { return FrozenSet.this.toArray(); }
            @Override public Spliterator<E> spliterator() {
                return Spliterators.spliterator(iterator(), size,
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
            }
        });
    }

    public Iterator<E> iterator() {
        return new Iterator<>() {
            private int cursor;
            @Override public boolean hasNext() { return cursor < size; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                return get(cursor++);
            }
        };
    }

    public boolean add(E element) {
        throw new UnsupportedOperationException("immutable set");
    }

    public boolean remove(Object element) {
        throw new UnsupportedOperationException("immutable set");
    }
}
