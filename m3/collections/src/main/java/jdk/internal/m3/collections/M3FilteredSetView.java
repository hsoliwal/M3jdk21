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
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Read-only live predicate view preserving {@link Set} equality and hash-code contracts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/jdk/FilteredSetView.java}
 * blob {@code b248d51e608a67478eeb8ede3eb402ee915402a9} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public class M3FilteredSetView<E> extends AbstractSet<E> {
    private final Set<E> source;
    private final Predicate<? super E> predicate;

    public M3FilteredSetView(Set<E> source, Predicate<? super E> predicate) {
        this.source = Objects.requireNonNull(source, "source");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    protected final Set<E> source() {
        return source;
    }

    protected final Predicate<? super E> predicate() {
        return predicate;
    }

    protected final boolean matches(E value) {
        return predicate.test(value);
    }

    @Override
    public Iterator<E> iterator() {
        return M3FilteredCollectionView.filteringIterator(source.iterator(), predicate);
    }

    @Override
    public Spliterator<E> spliterator() {
        return M3FilteredCollectionView.ownerCharacteristics(
                source, this, Spliterator.DISTINCT);
    }

    @Override
    public int size() {
        // SortedSet's comparator, range and owner size remain authoritative.
        // Only non-sorted subclasses with a real iterator override change lane.
        if (!(this instanceof java.util.SortedSet<?>)
                && M3FilteredCollectionView.overridesIterator(
                        getClass(), M3FilteredSetView.class)) {
            return M3FilteredCollectionView.countExposed(iterator());
        }
        int count = 0;
        for (E value : source) {
            if (predicate.test(value)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        if (!(this instanceof java.util.SortedSet<?>)
                && M3FilteredCollectionView.overridesIterator(
                        getClass(), M3FilteredSetView.class)) {
            return !iterator().hasNext();
        }
        for (E value : source) {
            if (predicate.test(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(Object candidate) {
        // Most hash-backed misses remain O(1). On a hit, evaluate visibility
        // against the real stored representative rather than the supplied probe.
        return M3FilteredCollectionView.containsStored(source, predicate, candidate);
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        // Keep the low-allocation direct path only for the three known exact
        // owners. Unknown subclasses may substitute a different iterator.
        Class<?> viewType = getClass();
        if (viewType != M3FilteredSetView.class
                && viewType != M3FilteredSortedSetView.class
                && viewType != M3FilteredNavigableSetView.class) {
            iterator().forEachRemaining(action);
            return;
        }
        for (E value : source) {
            if (predicate.test(value)) {
                action.accept(value);
            }
        }
    }

    @Override
    public boolean add(E value) {
        throw M3FilteredCollectionView.readOnly();
    }

    @Override
    public boolean addAll(Collection<? extends E> values) {
        Objects.requireNonNull(values, "values");
        throw M3FilteredCollectionView.readOnly();
    }

    @Override
    public boolean remove(Object value) {
        throw M3FilteredCollectionView.readOnly();
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        throw M3FilteredCollectionView.readOnly();
    }

    @Override
    public boolean retainAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        throw M3FilteredCollectionView.readOnly();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter, "filter");
        throw M3FilteredCollectionView.readOnly();
    }

    @Override
    public void clear() {
        throw M3FilteredCollectionView.readOnly();
    }
}
