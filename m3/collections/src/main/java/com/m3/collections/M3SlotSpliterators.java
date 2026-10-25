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
// Modified 2026 by Hitesh Soliwal and contributors: retain lazy comparator lookup for filtered split children.
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: late-binding collection adapters and comparator-preserving splits.
package com.m3.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Preserve encounter/comparator semantics for sequential and parallel stream boundaries. */
final class M3SlotSpliterators {
    private M3SlotSpliterators() { }
    static <E> Spliterator<E> ordered(Iterator<E> iterator, int size, boolean distinct) {
        return Spliterators.spliterator(iterator, size, Spliterator.ORDERED | (distinct ? Spliterator.DISTINCT : 0));
    }
    /** Bind iterator and size at first traversal/split/size query, as the JDK owners do. */
    static <E> Spliterator<E> ordered(Collection<E> collection, boolean distinct) {
        return Spliterators.spliterator(collection,
                Spliterator.ORDERED | (distinct ? Spliterator.DISTINCT : 0));
    }
    /**
     * Unknown-size, late-binding ordered iteration for live predicate views.
     * Unlike Spliterators.spliterator(Collection,...), this does not first call
     * Collection.size(), which would evaluate every predicate before findFirst.
     * All callbacks remain in Java; no duplicate element or index store exists.
     */
    static <E> Spliterator<E> lateBoundOrdered(
            Supplier<? extends Iterator<? extends E>> iteratorSupplier) {
        Objects.requireNonNull(iteratorSupplier, "iteratorSupplier");
        return new Spliterator<>() {
            private Iterator<? extends E> cursor;
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                Objects.requireNonNull(action, "action");
                if (cursor == null) cursor = Objects.requireNonNull(
                        iteratorSupplier.get(), "iterator");
                if (!cursor.hasNext()) return false;
                action.accept(cursor.next());
                return true;
            }
            @Override public Spliterator<E> trySplit() { return null; }
            @Override public long estimateSize() { return Long.MAX_VALUE; }
            @Override public int characteristics() { return Spliterator.ORDERED; }
        };
    }

    static <E> Spliterator<E> concurrent(Iterator<E> iterator, boolean ordered, boolean distinct) {
        int flags = Spliterator.CONCURRENT | Spliterator.NONNULL;
        if (ordered) { flags |= Spliterator.ORDERED; }
        if (distinct) { flags |= Spliterator.DISTINCT; }
        return Spliterators.spliteratorUnknownSize(iterator, flags);
    }
    static <E> Spliterator<E> sorted(Iterator<E> iterator, int size, Comparator<? super E> comparator) {
        Spliterator<E> source = new Spliterators.AbstractSpliterator<>(size, Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT) {
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                Objects.requireNonNull(action); if (!iterator.hasNext()) { return false; } action.accept(iterator.next()); return true;
            }
        };
        return withComparator(source, () -> comparator);
    }
    static <E> Spliterator<E> sorted(Collection<E> collection, Comparator<? super E> comparator) {
        return withComparator(Spliterators.spliterator(collection,
                Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT), () -> comparator);
    }
    /** Preserve the SortedSet default's lazy comparator lookup as well as child order. */
    static <E> Spliterator<E> sorted(SortedSet<E> collection) {
        return withComparator(Spliterators.spliterator(collection,
                Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT), collection::comparator);
    }
    /** JDK iterator batching returns array splits; retain custom ordering on each child. */
    private static <E> Spliterator<E> withComparator(
            Spliterator<E> source, Supplier<Comparator<? super E>> comparator) {
        return new Spliterator<>() {
            @Override public boolean tryAdvance(Consumer<? super E> action) {
                return source.tryAdvance(action);
            }
            @Override public void forEachRemaining(Consumer<? super E> action) {
                source.forEachRemaining(action);
            }
            @Override public Spliterator<E> trySplit() {
                Spliterator<E> prefix = source.trySplit();
                return prefix == null ? null : withComparator(prefix, comparator);
            }
            @Override public long estimateSize() { return source.estimateSize(); }
            @Override public int characteristics() { return source.characteristics(); }
            @Override public Comparator<? super E> getComparator() { return comparator.get(); }
        };
    }
}
