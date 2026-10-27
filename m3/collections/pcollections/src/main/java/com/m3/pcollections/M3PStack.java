/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.pcollections;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import org.pcollections.AbstractUnmodifiableList;
import org.pcollections.PStack;

/**
 * Persistent stack over the M3 paged vector owner. Index zero is the stack top.
 * Conversion preserves encounter order; both push-all forms reverse it, matching
 * the PStack push convention. Each update shares untouched immutable pages.
 * Null elements are supported. Destructive List operations always refuse.
 */
public final class M3PStack<E> extends AbstractUnmodifiableList<E>
        implements PStack<E>, RandomAccess, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3PVector<E> elements;

    private M3PStack(M3PVector<E> elements) {
        this.elements = elements;
    }

    public static <E> M3PStack<E> empty() {
        return new M3PStack<>(M3PVector.empty());
    }

    public static <E> M3PStack<E> singleton(E value) {
        return new M3PStack<>(M3PVector.singleton(value));
    }

    /** Retains the source's encounter order, rather than applying repeated pushes. */
    public static <E> M3PStack<E> from(Collection<? extends E> source) {
        return new M3PStack<>(M3PVector.from(Objects.requireNonNull(source, "source")));
    }

    @Override public int size() { return elements.size(); }
    @Override public E get(int index) { return elements.get(index); }
    @Override public Iterator<E> iterator() { return elements.iterator(); }
    @Override public ListIterator<E> listIterator() { return elements.listIterator(); }
    @Override public ListIterator<E> listIterator(int index) { return elements.listIterator(index); }
    @Override public Spliterator<E> spliterator() { return elements.spliterator(); }
    @Override public List<E> reversed() { return elements.reversed(); }

    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public void addFirst(E value) { throw new UnsupportedOperationException(); }
    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public void addLast(E value) { throw new UnsupportedOperationException(); }
    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public E removeFirst() { throw new UnsupportedOperationException(); }
    /** @deprecated Unsupported destructive operation. */
    @Deprecated @Override public E removeLast() { throw new UnsupportedOperationException(); }

    @Override public M3PStack<E> plus(E value) {
        return new M3PStack<>(elements.plus(0, value));
    }

    @Override public M3PStack<E> plusAll(Collection<? extends E> source) {
        return plusAll(0, source);
    }

    @Override public M3PStack<E> plus(int index, E value) {
        return new M3PStack<>(elements.plus(index, value));
    }

    /** Inserts the source in reverse order at the requested stack coordinate. */
    @Override public M3PStack<E> plusAll(int index, Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException(index);
        M3PVector<E> result = elements;
        for (E value : source) result = result.plus(index, value);
        return result == elements ? this : new M3PStack<>(result);
    }

    @Override public M3PStack<E> with(int index, E value) {
        M3PVector<E> result = elements.with(index, value);
        return result == elements ? this : new M3PStack<>(result);
    }

    @Override public M3PStack<E> minus(Object value) {
        M3PVector<E> result = elements.minus(value);
        return result == elements ? this : new M3PStack<>(result);
    }

    @Override public M3PStack<E> minus(int index) {
        return new M3PStack<>(elements.minus(index));
    }

    @Override public M3PStack<E> minusAll(Collection<?> source) {
        M3PVector<E> result = elements.minusAll(source);
        return result == elements ? this : new M3PStack<>(result);
    }

    @Override public M3PStack<E> subList(int from, int to) {
        M3PVector<E> result = elements.subList(from, to);
        return result == elements ? this : new M3PStack<>(result);
    }

    @Override public M3PStack<E> subList(int from) {
        return subList(from, size());
    }
}
