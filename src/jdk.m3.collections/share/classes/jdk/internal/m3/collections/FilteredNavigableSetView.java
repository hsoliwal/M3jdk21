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
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.function.Predicate;

/**
 * Read-only live filtered {@link NavigableSet}.
 *
 * <p>Navigation starts with the backing tree's logarithmic seek and skips only rejected neighbors,
 * so a query costs the backing seek plus work proportional to the rejected run it crosses.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/jdk/FilteredNavigableSetView.java}
 * blob {@code 98c547ee28a07f722f0bae33e84ac230b15825dd} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FilteredNavigableSetView<E>
        extends FilteredSortedSetView<E> implements NavigableSet<E> {
    private final NavigableSet<E> navigableSource;

    public FilteredNavigableSetView(
            NavigableSet<E> source,
            Predicate<? super E> predicate) {
        super(source, predicate);
        this.navigableSource = source;
    }

    @Override
    public E lower(E value) {
        return seek(value, false, false);
    }

    @Override
    public E floor(E value) {
        return seek(value, false, true);
    }

    @Override
    public E ceiling(E value) {
        return seek(value, true, true);
    }

    @Override
    public E higher(E value) {
        return seek(value, true, false);
    }

    /**
     * Resolve null as a legitimate stored key before interpreting a null
     * navigation result as absence. Reuse owner seek/higher/lower operations;
     * no selected array or intermediate Entry objects are materialized.
     */
    private E seek(E probe, boolean forward, boolean inclusive) {
        E candidate = forward
                ? (inclusive ? navigableSource.ceiling(probe) : navigableSource.higher(probe))
                : (inclusive ? navigableSource.floor(probe) : navigableSource.lower(probe));
        boolean visitedNull = false;
        while (true) {
            if (candidate == null) {
                if (visitedNull || !nullIsCandidate(probe, forward, inclusive)) {
                    return null;
                }
                visitedNull = true;
                if (matches(null)) {
                    return null;
                }
                candidate = forward
                        ? navigableSource.higher(null) : navigableSource.lower(null);
            } else {
                if (matches(candidate)) {
                    return candidate;
                }
                candidate = forward
                        ? navigableSource.higher(candidate) : navigableSource.lower(candidate);
            }
        }
    }

    private boolean nullIsCandidate(E probe, boolean forward, boolean inclusive) {
        Comparator<? super E> comparator = navigableSource.comparator();
        if (comparator == null) {
            if (probe != null || !inclusive) {
                return false;
            }
            try {
                return ownsActualNullKey();
            } catch (NullPointerException | ClassCastException unsupportedNull) {
                return false;
            }
        }
        try {
            int relation = comparator.compare(null, probe);
            boolean eligible = forward
                    ? (inclusive ? relation >= 0 : relation > 0)
                    : (inclusive ? relation <= 0 : relation < 0);
            return eligible && ownsActualNullKey();
        } catch (NullPointerException | ClassCastException unsupportedNull) {
            // Do not add a new exception merely to resolve an ambiguous null sentinel.
            return false;
        }
    }

    /**
     * Comparator membership can identify a non-null stored key as equal to null.
     * Check the actual representative returned by the owner, not just contains().
     * A genuine stored null is ambiguous with the API's absence sentinel, but
     * contains(null) plus ceiling(null) disambiguates that exact case.
     */
    private boolean ownsActualNullKey() {
        return navigableSource.contains(null)
                && navigableSource.ceiling(null) == null;
    }

    @Override
    public E pollFirst() {
        throw FilteredCollectionView.readOnly();
    }

    @Override
    public E pollLast() {
        throw FilteredCollectionView.readOnly();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new FilteredNavigableSetView<>(navigableSource.descendingSet(), predicate());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return FilteredCollectionView.filteringIterator(
                navigableSource.descendingIterator(), predicate());
    }

    @Override
    public NavigableSet<E> subSet(
            E fromElement,
            boolean fromInclusive,
            E toElement,
            boolean toInclusive) {
        return new FilteredNavigableSetView<>(
                navigableSource.subSet(fromElement, fromInclusive, toElement, toInclusive),
                predicate());
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new FilteredNavigableSetView<>(
                navigableSource.headSet(toElement, inclusive), predicate());
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new FilteredNavigableSetView<>(
                navigableSource.tailSet(fromElement, inclusive), predicate());
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        Iterator<E> iterator = iterator();
        if (!iterator.hasNext()) {
            throw new java.util.NoSuchElementException();
        }
        return iterator.next();
    }

    @Override
    public E last() {
        Iterator<E> iterator = descendingIterator();
        if (!iterator.hasNext()) {
            throw new java.util.NoSuchElementException();
        }
        return iterator.next();
    }
}
