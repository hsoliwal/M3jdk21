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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Predicate;

/**
 * Array-backed, short-circuit predicate plan for an exact chain of the existing
 * filtered Collection view over an exact JDK ArrayList.
 *
 * <p>This is one transient predicate array per stream/spliterator, never an
 * array of selected elements and never a retained per-element Entry/Node.
 * Subclassed wrappers and non-ArrayList owners keep the original path.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/jdk/FilteredArrayListPredicatePlan.java}
 * blob {@code 74c05668e92f6474b4cc2bf65fcbe67664d6603a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class FilteredArrayListPredicatePlan<E> implements Predicate<E> {
    private final Object[] outerToInner;
    private final int length;

    private FilteredArrayListPredicatePlan(Object[] outerToInner, int length) {
        this.outerToInner = outerToInner;
        this.length = length;
    }

    /**
     * Returns null when the existing implementation must remain authoritative.
     * The exact-class test prevents bypassing overridden view or list behavior.
     */
    static <E> Spliterator<E> tryCreate(
            Collection<E> source, Collection<E> view, int inheritedFlags) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(view, "view");
        if (source.getClass() != FilteredCollectionView.class
                || view.getClass() != FilteredCollectionView.class) {
            return null;
        }
        Object[] predicates = new Object[8];
        int count = 0;
        Collection<E> candidate = view;
        while (candidate.getClass() == FilteredCollectionView.class) {
            @SuppressWarnings("unchecked")
            FilteredCollectionView<E> current = (FilteredCollectionView<E>) candidate;
            if (count == predicates.length) {
                int doubled = predicates.length <= (Integer.MAX_VALUE - 8) / 2
                        ? predicates.length * 2 : Integer.MAX_VALUE - 8;
                if (doubled <= count) {
                    throw new IllegalStateException("filtered predicate plan exceeds array limits");
                }
                predicates = Arrays.copyOf(predicates, doubled);
            }
            predicates[count++] = current.predicate();
            candidate = current.source();
        }
        // An exact nested chain is safe to flatten regardless of its ultimate
        // Collection owner. Only exact ArrayList retains the indexed split path.
        // Other backing types retain their original iterator semantics, but
        // without recursively constructing thousands of filtered iterators.
        Spliterator<E> owner = candidate.spliterator();
        int flags = (owner.characteristics()
                & (Spliterator.ORDERED | Spliterator.DISTINCT
                        | Spliterator.SORTED | Spliterator.NONNULL))
                | inheritedFlags;
        FilteredArrayListPredicatePlan<E> plan =
                new FilteredArrayListPredicatePlan<>(predicates, count);
        if (candidate.getClass() == ArrayList.class) {
            // The metadata-inspected JDK cursor is already late-bound. Reuse it
            // instead of constructing a second ArrayList spliterator.
            return new FilteredArrayListSplitCursor<>(owner, plan, flags);
        }
        Comparator<? super E> comparator = null;
        if (candidate instanceof SortedSet<E> sorted) {
            flags |= Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT;
            comparator = sorted.comparator();
        } else if ((flags & Spliterator.SORTED) != 0) {
            comparator = owner.getComparator();
        }
        // Single ephemeral composite view, no selected-element array, no
        // per-value Entry or node. This retains the existing unsplittable cursor.
        return FilteredCollectionView.lazySpliterator(
                new FilteredCollectionView<>(candidate, plan), flags, comparator);
    }

    @Override
    public boolean test(E value) {
        // Existing nested iterator semantics: innermost predicate evaluates first.
        // A rejected value never reaches an outer predicate.
        for (int index = length - 1; index >= 0; index--) {
            @SuppressWarnings("unchecked")
            Predicate<? super E> predicate = (Predicate<? super E>) outerToInner[index];
            if (!predicate.test(value)) {
                return false;
            }
        }
        return true;
    }
}
