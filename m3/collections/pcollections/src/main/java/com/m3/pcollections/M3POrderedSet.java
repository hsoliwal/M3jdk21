/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import org.pcollections.AbstractUnmodifiableSet;
import org.pcollections.PSet;

/**
 * Persistent first-insertion-order set over the packed ordered map owner. An equal
 * duplicate preserves the original representative and position. Null is allowed;
 * removal and reinsertion append to the end of the new version.
 */
public final class M3POrderedSet<E> extends AbstractUnmodifiableSet<E>
        implements PSet<E>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3POrderedMap<E, Boolean> elements;

    private M3POrderedSet(M3POrderedMap<E, Boolean> elements) { this.elements = elements; }

    public static <E> M3POrderedSet<E> empty() {
        return new M3POrderedSet<>(M3POrderedMap.empty());
    }

    public static <E> M3POrderedSet<E> singleton(E element) {
        return M3POrderedSet.<E>empty().plus(element);
    }

    public static <E> M3POrderedSet<E> from(Collection<? extends E> source) {
        return M3POrderedSet.<E>empty().plusAll(source);
    }

    @Override public int size() { return elements.size(); }
    @Override public boolean contains(Object element) { return elements.containsKey(element); }
    @Override public Iterator<E> iterator() { return elements.keySet().iterator(); }
    @Override public Spliterator<E> spliterator() { return elements.keySet().spliterator(); }

    @Override public M3POrderedSet<E> plus(E element) {
        M3POrderedMap<E, Boolean> next = elements.plus(element, Boolean.TRUE);
        return next == elements ? this : new M3POrderedSet<>(next);
    }

    @Override public M3POrderedSet<E> plusAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        M3POrderedSet<E> result = this;
        for (E element : source) result = result.plus(element);
        return result;
    }

    @Override public M3POrderedSet<E> minus(Object element) {
        M3POrderedMap<E, Boolean> next = elements.minus(element);
        return next == elements ? this : new M3POrderedSet<>(next);
    }

    @Override public M3POrderedSet<E> minusAll(Collection<?> source) {
        M3POrderedMap<E, Boolean> next = elements.minusAll(source);
        return next == elements ? this : new M3POrderedSet<>(next);
    }

    @Override public M3POrderedSet<E> intersect(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        return minusAll(minusAll(source));
    }

    @Serial private Object writeReplace() { return new SerializationProxy(toArray()); }

    @Serial private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // ObjectOutputStream checks serializability of each visible element.
        @SuppressWarnings("serial") private final Object[] elements;
        SerializationProxy(Object[] elements) { this.elements = elements; }
        @Serial private Object readResolve() throws InvalidObjectException {
            if (elements == null) throw new InvalidObjectException("Missing ordered-set elements");
            M3POrderedSet<Object> result = empty();
            for (Object element : elements) {
                if (result.contains(element)) throw new InvalidObjectException("Duplicate ordered-set member");
                result = result.plus(element);
            }
            return result;
        }
    }
}
