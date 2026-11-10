/* *****************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Predicate;

/**
 * Read-only live predicate view preserving the backing {@link SortedSet} ordering.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/jdk/FilteredSortedSetView.java}
 * blob {@code 73f7b6e03e8b8636a4a690e9944602d816ac6ddd} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public class FilteredSortedSetView<E> extends FilteredSetView<E> implements SortedSet<E> {
    private final SortedSet<E> sortedSource;

    public FilteredSortedSetView(SortedSet<E> source, Predicate<? super E> predicate) {
        super(source, predicate);
        this.sortedSource = Objects.requireNonNull(source, "source");
    }

    protected final SortedSet<E> sortedSource() {
        return sortedSource;
    }

    @Override
    public Spliterator<E> spliterator() {
        // Preserve the source's comparator and ordering without scanning size().
        return FilteredCollectionView.lazySpliterator(this,
                Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED,
                comparator());
    }

    @Override
    public Comparator<? super E> comparator() {
        return sortedSource.comparator();
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return new FilteredSortedSetView<>(
                sortedSource.subSet(fromElement, toElement), predicate());
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return new FilteredSortedSetView<>(sortedSource.headSet(toElement), predicate());
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return new FilteredSortedSetView<>(sortedSource.tailSet(fromElement), predicate());
    }

    @Override
    public E first() {
        Iterator<E> iterator = iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        return iterator.next();
    }

    @Override
    public E last() {
        Iterator<E> iterator = iterator();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        E last = iterator.next();
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }
}
