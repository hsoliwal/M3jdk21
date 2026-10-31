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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import org.pcollections.AbstractUnmodifiableQueue;
import org.pcollections.PQueue;

/**
 * Persistent FIFO queue sharing the M3 vector's immutable pages. Dequeue deletes
 * the head path, so a repeatedly drained/refilled queue does not retain a growing
 * discarded prefix. Null payloads are supported; size distinguishes an empty queue
 * from one whose first value is null. Queue equality remains identity equality.
 */
public final class M3PQueue<E> extends AbstractUnmodifiableQueue<E>
        implements PQueue<E>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3PVector<E> elements;

    private M3PQueue(M3PVector<E> elements) { this.elements = elements; }

    public static <E> M3PQueue<E> empty() { return new M3PQueue<>(M3PVector.empty()); }
    public static <E> M3PQueue<E> singleton(E value) {
        return new M3PQueue<>(M3PVector.singleton(value));
    }
    public static <E> M3PQueue<E> from(Collection<? extends E> source) {
        return new M3PQueue<>(M3PVector.from(Objects.requireNonNull(source, "source")));
    }

    @Override public int size() { return elements.size(); }
    @Override public Iterator<E> iterator() { return elements.iterator(); }
    @Override public Spliterator<E> spliterator() { return elements.spliterator(); }
    @Override public E peek() { return isEmpty() ? null : elements.get(0); }
    @Override public E element() {
        if (isEmpty()) throw new NoSuchElementException();
        return elements.get(0);
    }
    @Override public M3PQueue<E> minus() {
        return isEmpty() ? this : new M3PQueue<>(elements.minus(0));
    }
    @Override public M3PQueue<E> plus(E value) {
        return new M3PQueue<>(elements.plus(value));
    }
    @Override public M3PQueue<E> plusAll(Collection<? extends E> source) {
        M3PVector<E> result = elements.plusAll(source);
        return result == elements ? this : new M3PQueue<>(result);
    }
    @Override public M3PQueue<E> minus(Object value) {
        M3PVector<E> result = elements.minus(value);
        return result == elements ? this : new M3PQueue<>(result);
    }
    @Override public M3PQueue<E> minusAll(Collection<?> source) {
        M3PVector<E> result = elements.minusAll(source);
        return result == elements ? this : new M3PQueue<>(result);
    }
}
