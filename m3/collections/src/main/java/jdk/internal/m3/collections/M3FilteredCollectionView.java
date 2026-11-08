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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Read-only live view of a collection containing only elements accepted by a predicate.
 *
 * <p>The view stores no selected-element copy. Source changes are observed by later operations.
 * All mutation methods fail with {@link UnsupportedOperationException}; mutate the source owner
 * explicitly when mutation is required.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/jdk/FilteredCollectionView.java}
 * blob {@code aa67a64735a440170b3cae9a0c284939ed5fc145} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public class M3FilteredCollectionView<E> extends AbstractCollection<E> {
    private final Collection<E> source;
    private final Predicate<? super E> predicate;

    /**
     * Cache the actual iterator method owner once per subtype. A mere subclass
     * is not necessarily an iterator override, so getClass() alone is unsafe:
     * comparator-based membership and owner-side predicate checks must survive
     * inherited iterator implementations.
     */
    private static final ClassValue<Class<?>> ITERATOR_IMPLEMENTOR = new ClassValue<>() {
        @Override
        protected Class<?> computeValue(Class<?> type) {
            try {
                return type.getMethod("iterator").getDeclaringClass();
            } catch (NoSuchMethodException impossible) {
                throw new AssertionError("Collection.iterator() is required", impossible);
            }
        }
    };

    static boolean overridesIterator(Class<?> actual, Class<?> canonical) {
        return actual != canonical && ITERATOR_IMPLEMENTOR.get(actual) != canonical;
    }

    static int countExposed(Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            if (count == Integer.MAX_VALUE) {
                return count;
            }
            count++;
        }
        return count;
    }

    public M3FilteredCollectionView(Collection<E> source, Predicate<? super E> predicate) {
        this.source = Objects.requireNonNull(source, "source");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    protected final Collection<E> source() {
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
        // Reuse the existing exact-owner flattened spliterator plan. This
        // avoids recursive iterator construction over deep LinkedList/TreeSet
        // chains without changing subclass or single-wrapper virtual behavior.
        if (getClass() == M3FilteredCollectionView.class
                && source.getClass() == M3FilteredCollectionView.class) {
            Spliterator<E> flat = M3FilteredArrayListPredicatePlan.tryCreate(source, this, 0);
            if (flat != null) {
                return java.util.Spliterators.iterator(flat);
            }
        }
        return filteringIterator(source.iterator(), predicate);
    }

    @Override
    public Spliterator<E> spliterator() {
        return ownerCharacteristics(source, this, 0);
    }

    @Override
    public int size() {
        if (overridesIterator(getClass(), M3FilteredCollectionView.class)) {
            return countExposed(iterator());
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
        if (overridesIterator(getClass(), M3FilteredCollectionView.class)) {
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
        // Examine the actual source element: equal probes can carry different
        // predicate-only fields, so testing the probe would hide or invent rows.
        return containsStored(source, predicate, candidate);
    }

    /** No per-entry proxy or index. Equality is probe-to-stored, as in JDK collections. */
    static <E> boolean containsStored(Collection<E> source,
            Predicate<? super E> predicate, Object probe) {
        if (!source.contains(probe)) {
            return false;
        }
        if (source instanceof SortedSet<E> sorted) {
            // A TreeSet can contain a comparator-equal but not equals()-equal
            // probe. Resolve the actual key without materializing the set.
            @SuppressWarnings("unchecked")
            E key = (E) probe;
            E stored = sorted instanceof NavigableSet<E> navigable
                    ? navigable.ceiling(key) : sorted.tailSet(key).first();
            return predicate.test(stored);
        }
        if (source instanceof Set<?>) {
            // Identity-backed Sets may contain two equals()-equal references.
            // Resolve the owned reference before accepting an equals fallback.
            E equalRepresentative = null;
            boolean equalFound = false;
            for (E stored : source) {
                if (stored == probe) {
                    return predicate.test(stored);
                }
                if (!equalFound && Objects.equals(probe, stored)) {
                    equalRepresentative = stored;
                    equalFound = true;
                }
            }
            return equalFound && predicate.test(equalRepresentative);
        }
        for (E stored : source) {
            if (Objects.equals(probe, stored) && predicate.test(stored)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        // A subclass may override iterator(): Iterable.forEach must observe
        // that virtual cursor, just as enhanced-for and stream traversal do.
        // Keep the original direct owner loop for this exact base class.
        if (getClass() != M3FilteredCollectionView.class) {
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
        throw readOnly();
    }

    @Override
    public boolean addAll(Collection<? extends E> values) {
        Objects.requireNonNull(values, "values");
        throw readOnly();
    }

    @Override
    public boolean remove(Object value) {
        throw readOnly();
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        throw readOnly();
    }

    @Override
    public boolean retainAll(Collection<?> values) {
        Objects.requireNonNull(values, "values");
        throw readOnly();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter, "filter");
        throw readOnly();
    }

    @Override
    public void clear() {
        throw readOnly();
    }

    static <E> Iterator<E> filteringIterator(
            Iterator<E> source,
            Predicate<? super E> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        return new Iterator<>() {
            private E next;
            private boolean ready;
            private boolean exhausted;

            @Override
            public boolean hasNext() {
                if (ready) {
                    return true;
                }
                if (exhausted) {
                    return false;
                }
                while (source.hasNext()) {
                    E candidate = source.next();
                    if (predicate.test(candidate)) {
                        next = candidate;
                        ready = true;
                        return true;
                    }
                }
                exhausted = true;
                next = null;
                return false;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E result = next;
                next = null;
                ready = false;
                return result;
            }

            @Override
            public void remove() {
                throw readOnly();
            }
        };
    }

    /**
     * Preserve only proven upstream traversal metadata. Filtering has unknown
     * cardinality, so SIZED and SUBSIZED must never leak to downstream streams.
     * The upstream spliterator is consulted once for metadata, not traversed,
     * and no per-element buffers or Entry nodes are created.
     */
    static <E> Spliterator<E> ownerCharacteristics(
            Collection<E> source, Collection<E> view, int guaranteedFlags) {
        // A subclass may redefine its iterator's contents, order and null
        // handling independently of the backing owner's spliterator.
        // Do not bypass polymorphism or propagate unproven source flags.
        Class<?> viewType = view.getClass();
        if ((view instanceof M3FilteredCollectionView<?>
                    && viewType != M3FilteredCollectionView.class)
                || (view instanceof M3FilteredSetView<?>
                    && viewType != M3FilteredSetView.class
                    && viewType != M3FilteredSortedSetView.class
                    && viewType != M3FilteredNavigableSetView.class)) {
            // Unknown Set/Collection subclasses can override iterator() and
            // cannot inherit the source owner's ordering, comparator or
            // NONNULL metadata. Do not precompute or split their elements.
            return lazySpliterator(view, 0, null);
        }
        // Resolve exact nested filtered ArrayList chains before calling the
        // parent's spliterator, which otherwise recurses for every wrapper.
        Spliterator<E> nested = M3FilteredArrayListPredicatePlan.tryCreate(
                source, view, guaranteedFlags);
        if (nested != null) {
            return nested;
        }
        Spliterator<E> owner = source.spliterator();
        int flags = (owner.characteristics()
                & (Spliterator.ORDERED | Spliterator.DISTINCT
                        | Spliterator.SORTED | Spliterator.NONNULL))
                | guaranteedFlags;
        Comparator<? super E> comparator = null;
        if (source instanceof SortedSet<E> sorted) {
            // A SortedSet's comparator is authoritative even when its concrete
            // spliterator under-reports SORTED (e.g. a descending/ranged owner).
            flags |= Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT;
            comparator = sorted.comparator();
        } else if ((flags & Spliterator.SORTED) != 0) {
            comparator = owner.getComparator();
        }
        // Only exact JDK ArrayList admits index-range splitting without
        // intermediate Object[] batches; all other owner paths are unchanged.
        if (source.getClass() == ArrayList.class
                && view instanceof M3FilteredCollectionView<E> filtered) {
            // Reuse the metadata-inspected JDK ArrayList spliterator. Its
            // fence remains unbound until traversal or splitting.
            return new M3FilteredArrayListSplitCursor<>(owner,
                    filtered.predicate(), flags);
        }
        return lazySpliterator(view, flags, comparator);
    }

    /** Lazy, unsized and array-free Spliterator over the existing filtered cursor. */
    static <E> Spliterator<E> lazySpliterator(
            Collection<E> view, int flags, Comparator<? super E> comparator) {
        Objects.requireNonNull(view, "view");
        return new Spliterator<>() {
            private Iterator<E> cursor;
            private boolean exhausted;

            private Iterator<E> cursor() {
                if (cursor == null) {
                    cursor = view.iterator();
                }
                return cursor;
            }

            @Override
            public boolean tryAdvance(Consumer<? super E> action) {
                Objects.requireNonNull(action, "action");
                if (exhausted) {
                    return false;
                }
                Iterator<E> values = cursor();
                if (!values.hasNext()) {
                    exhausted = true;
                    return false;
                }
                action.accept(values.next());
                return true;
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                Objects.requireNonNull(action, "action");
                if (!exhausted) {
                    cursor().forEachRemaining(action);
                    exhausted = true;
                }
            }

            @Override
            public Spliterator<E> trySplit() {
                // No intermediate Object[] batches, index nodes or changed ordering.
                return null;
            }

            @Override
            public long estimateSize() {
                return exhausted ? 0L : Long.MAX_VALUE;
            }

            @Override
            public int characteristics() {
                return flags;
            }

            @Override
            public Comparator<? super E> getComparator() {
                if ((flags & Spliterator.SORTED) == 0) {
                    throw new IllegalStateException("not a sorted spliterator");
                }
                return comparator;
            }
        };
    }

    static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("filtered view is read-only");
    }
}
