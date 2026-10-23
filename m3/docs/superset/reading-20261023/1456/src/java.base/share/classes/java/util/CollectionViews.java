/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
 * Modified 2026: M3 receiver namespace, runtime-capability dispatch,
 * direct owner membership/removal and single-pass sorted snapshot acquisition.
 * Modified 2026-10-17: stack-safe deferred predicate plans for nested filters.
 * Modified 2026-10-17: java.base receiver; sole implementation behind legacy facade.
 * Apache notice retained; core integration license review remains a promotion gate.
 */
package java.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
=======
 *
 * Modified 2026-10-19: target-owned M3JDK21 collection-view implementation.
 * First-party behavior/algorithm provenance: M3CollectionViews at
 * a3e88976ee8664d1ef5d0cade96aec1206b2133d and reentrant traversal atom at
 * e915f032dc0819cf3642837a98af36c16009dd3f. Original Apache attribution and
 * license are retained. Code qualification does not clear distribution in a
 * GPL/ClassPath product; the canonical intake retains a separate rightsholder
 * and distribution review gate. No implicit relicensing is asserted.
 */
package java.util;

>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
 * Composable backed filter views and explicit sorted snapshots of JDK collections.
 *
 * <p>These are reference collection adapters, not replacements for the existing
 * primitive admission filters or primitive indexed views. A filtered view
 * retains only its backing collection and predicate, never one object per entry.
 * Filter predicates should be pure and stable with respect to the equality and
 * ordering semantics of the backing collection.
 *
 * <p>Backed views are not automatically synchronized, serializable or snapshots.
 * Iterator removal is deliberately unsupported; bulk mutations and explicit
 * {@code remove} do write through to the underlying collection when supported.
 *
 * <p>All live wrappers keep the supplied owner authoritative, including nested
 * filters and sorted ranges. They neither sort the owner nor build a second
 * element store. An arbitrary value predicate still requires values to compute
 * exact size; no predicate results are cached without validity evidence. Stream
 * traversal uses an unsized selection cursor rather than an eager counting pass.
 * The separate sortedSnapshot method intentionally creates independent storage.
 */
public final class CollectionViews {
    private CollectionViews() {
    }

    /**
     * Returns a live view, preserving runtime set/sorted/navigable capabilities.
     *
     * @param <E> the type of elements in the backing collection
     * @param backing the collection to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live view of the backing elements accepted by the predicate
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> filtered(
            Collection<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        if (backing instanceof Set<?>) return filtered((Set<E>) backing, predicate);
        return new FilteredCollection<>(backing, predicate);
    }

    /**
     * Retains runtime sorted capability and set equality in the live view.
     *
     * @param <E> the type of elements in the backing set
     * @param backing the set to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live set view of the backing elements accepted by the predicate
     */
    @SuppressWarnings("unchecked")
    public static <E> Set<E> filtered(Set<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        return new FilteredSet<>(backing, predicate);
    }

    /**
     * Retains the comparator, range views and runtime navigable capability.
     *
     * @param <E> the type of elements in the backing sorted set
     * @param backing the sorted set to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live sorted-set view of the elements accepted by the predicate
     */
    @SuppressWarnings("unchecked")
    public static <E> SortedSet<E> filtered(
            SortedSet<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        return new FilteredSortedSet<>(backing, predicate);
    }

    /**
     * Retains navigable operations, descending order and range-view contracts.
     *
     * @param <E> the type of elements in the backing navigable set
     * @param backing the navigable set to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live navigable-set view of the elements accepted by the predicate
     */
    public static <E> NavigableSet<E> filtered(
            NavigableSet<E> backing, Predicate<? super E> predicate) {
        return new FilteredNavigableSet<>(backing, predicate);
    }

    /**
     * Returns an independent, stable, read-only list sorted by the comparator.
     * Equal-comparator elements are not deduplicated. Unlike a backed filter,
     * this operation copies references and does not reflect later source changes.
     *
     * @param <E> the type of elements in the snapshot
     * @param source the collection whose elements are copied
     * @param comparator the comparator used to order the elements, or
     *        {@code null} to use their natural ordering
     * @return an unmodifiable list snapshot in the specified order
     */
    public static <E> List<E> sortedSnapshot(
            Collection<? extends E> source, Comparator<? super E> comparator) {
        Objects.requireNonNull(source, "source");
        ArrayList<E> sorted = new ArrayList<>();
        for (E element : source) sorted.add(element);
        sorted.sort(comparator);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Additive collection API: detached encounter-order snapshot with one predicate
     * evaluation per source occurrence. Unlike a backed filter, no later source
     * additions/removals are reflected. Nulls and duplicate references survive.
     * This never calls source.size(), performs no filter size prepass, and does
     * not change Collection.toArray or any existing JDK/Synexia contract.
     *
     * @param <E> the type of elements in the snapshot
     * @param source the collection whose elements are filtered and copied
     * @param predicate the predicate that selects elements for the snapshot
     * @return an unmodifiable list of selected elements in encounter order
     */
    public static <E> List<E> filteredSnapshot(
            Collection<? extends E> source, Predicate<? super E> predicate) {
        return Collections.unmodifiableList(selectedCopy(source, predicate));
    }

    /**
     * Additive collection API: fused filter and stable sort over one detached array
     * owner. Comparator-equivalent elements remain distinct and preserve their
     * original encounter order. A null comparator means natural ordering, as
     * specified by List.sort; mutable element objects are not deep-copied.
     *
     * @param <E> the type of elements in the snapshot
     * @param source the collection whose elements are filtered and copied
     * @param predicate the predicate that selects elements for the snapshot
     * @param comparator the comparator used to order the selected elements, or
     *        {@code null} to use their natural ordering
     * @return an unmodifiable list snapshot of selected elements in the
     *         specified order
     */
    public static <E> List<E> filteredSortedSnapshot(
            Collection<? extends E> source, Predicate<? super E> predicate,
            Comparator<? super E> comparator) {
        ArrayList<E> selected = selectedCopy(source, predicate);
        selected.sort(comparator);
        return Collections.unmodifiableList(selected);
    }

    /** A cohesive one-pass behavioral atom shared by the snapshot APIs. */
    private static <E> ArrayList<E> selectedCopy(
            Collection<? extends E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        ArrayList<E> selected = new ArrayList<>();
        for (E element : source) {
            if (predicate.test(element)) selected.add(element);
        }
        return selected;
    }

    /**
     * Persistent predicate metadata, not a chain of recursive callback invocations.
     * Each extension shares immutable terms, never an older view or its compiled
     * plan. Construction is O(1); the first test prepares an O(depth) ordered
     * predicate directory. Warm tests use constant stack and allocate no scratch.
     * Only executable steps are cached, never values or predicate outcomes.
     */
    private static final class FilterChain<E> implements Predicate<E> {
        private final FilterTerm<E> tail;
        private final int depth;
        private volatile Predicate<? super E>[] plan;

        @SuppressWarnings("unchecked")
        private FilterChain(Predicate<? super E> existing, Predicate<? super E> added) {
            if (existing instanceof FilterChain<?> nested) {
                FilterChain<E> previous = (FilterChain<E>) nested;
                this.depth = Math.addExact(previous.depth, 1);
                this.tail = new FilterTerm<>(previous.tail, added);
            } else {
                this.depth = 2;
                this.tail = new FilterTerm<>(new FilterTerm<>(null, existing), added);
            }
        }

        @Override
        public boolean test(E element) {
            Predicate<? super E>[] steps = plan;
            if (steps == null) steps = prepare();
            for (Predicate<? super E> step : steps) {
                if (!step.test(element)) return false;
            }
            return true;
        }

        /** User callbacks run outside the publication monitor, in original order. */
        @SuppressWarnings("unchecked")
        private synchronized Predicate<? super E>[] prepare() {
            Predicate<? super E>[] steps = plan;
            if (steps == null) {
                steps = (Predicate<? super E>[]) new Predicate<?>[depth];
                FilterTerm<E> current = tail;
                for (int i = depth - 1; i >= 0; i--) {
                    steps[i] = current.predicate;
                    current = current.previous;
                }
                plan = steps;
            }
            return steps;
        }
    }

    /** Prefix sharing does not retain compiled plans of intermediate wrappers. */
    private static final class FilterTerm<E> {
        private final FilterTerm<E> previous;
        private final Predicate<? super E> predicate;

        private FilterTerm(FilterTerm<E> previous, Predicate<? super E> predicate) {
            this.previous = previous;
            this.predicate = predicate;
        }
    }

    /** A live filter without per-element retained adapters. */
    private static class FilteredCollection<E> extends AbstractCollection<E> {
        protected final Collection<E> backing;
        protected final Predicate<? super E> predicate;

        @SuppressWarnings("unchecked")
        private FilteredCollection(Collection<E> source, Predicate<? super E> filter) {
            Objects.requireNonNull(source, "backing");
            Objects.requireNonNull(filter, "predicate");
            if (source instanceof FilteredCollection<?> nested) {
                FilteredCollection<E> previous = (FilteredCollection<E>) nested;
                this.backing = previous.backing;
                this.predicate = new FilterChain<>(previous.predicate, filter);
            } else {
                this.backing = source;
                this.predicate = filter;
            }
        }

        private boolean visible(E element) {
            return predicate.test(element);
        }

        @Override
        public Iterator<E> iterator() {
            return selected(backing.iterator(), predicate);
        }

        /**
         * Lazy selection over the existing owner's traversal. Metadata requests do
         * not call this view's size() or evaluate its predicate. The upstream
         * estimate is an upper estimate, never an exact filtered cardinality.
         * Binding, splitting and interference behavior follow the source; a source
         * may itself buffer or traverse during splitting. No such buffer is added
         * here. Predicates must be stable and safe for the selected traversal mode.
         */
        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(backing.spliterator(), predicate);
        }

        @Override
        public int size() {
            int count = 0;
            for (E element : backing) {
                if (visible(element)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public boolean isEmpty() {
            return !iterator().hasNext();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object candidate) {
            if (backing instanceof Set<?>) {
                if (!backing.contains(candidate)) return false;
                E equivalent = null;
                boolean found = false;
                for (E stored : backing) {
                    if (stored == candidate) return visible(stored);
                    if (!found && Objects.equals(candidate, stored)) {
                        equivalent = stored;
                        found = true;
                    }
                }
                return found && visible(equivalent);
            }
            // Collection membership is defined by the stored occurrence, not by a
            // caller-provided equal probe. Equivalence does not imply that a
            // predicate depending on other fields agrees for both instances.
            if (backing instanceof Set<?>) {
                // Equality against a stored value is not enough for identity-backed
                // sets. Delegate candidate admission to the actual owning Set.
                if (!backing.contains(candidate)) return false;
                E equivalent = null;
                boolean found = false;
                for (E stored : backing) {
                    if (stored == candidate) return visible(stored);
                    if (!found && Objects.equals(candidate, stored)) {
                        equivalent = stored;
                        found = true;
                    }
                }
                return found && visible(equivalent);
            }
            for (E stored : backing) {
                if (Objects.equals(candidate, stored) && visible(stored)) {
                    return true;
                }
=======
 * Live filtered collection views and explicit, stable collection snapshots.
 * This is an additive M3JDK21 API.
 *
 * <p>A live view retains its backing collection and predicate. It does not
 * retain a second element store, cache predicate results, or synchronize the
 * owner. Owner mutations remain visible, and supported view mutations write
 * through to that owner. Predicates should be stable for the chosen equality,
 * ordering and traversal rules. User predicates remain opaque callbacks.
 *
 * <p>Filtered iterators refuse removal. Other mutation methods retain the
 * backing owner's support and failure behavior. Streams use lazy, unsized
 * selection spliterators. A sorted view retains its comparator and range
 * capabilities. The explicitly named snapshot methods instead collect
 * references into an independent, unmodifiable list; elements are not cloned.
 */
public final class CollectionViews {
    private CollectionViews() { }

    /**
     * Returns a live view retaining runtime set, sorted and navigable capabilities.
     * @param <E> element type
     * @param source authoritative backing collection
     * @param predicate visibility test evaluated by live operations
     * @return a backed filtered collection view
     * @throws NullPointerException if source or predicate is null
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> filtered(Collection<E> source,
                                            Predicate<? super E> predicate) {
        if (source instanceof NavigableSet<?>) return filtered((NavigableSet<E>) source, predicate);
        if (source instanceof SortedSet<?>) return filtered((SortedSet<E>) source, predicate);
        if (source instanceof Set<?>) return filtered((Set<E>) source, predicate);
        return new View<>(source, predicate);
    }

    /**
     * Returns a live set view, retaining runtime sorted and navigable capabilities.
     * @param <E> element type
     * @param source authoritative backing set
     * @param predicate visibility test evaluated by live operations
     * @return a backed filtered set view
     * @throws NullPointerException if source or predicate is null
     */
    @SuppressWarnings("unchecked")
    public static <E> Set<E> filtered(Set<E> source, Predicate<? super E> predicate) {
        if (source instanceof NavigableSet<?>) return filtered((NavigableSet<E>) source, predicate);
        if (source instanceof SortedSet<?>) return filtered((SortedSet<E>) source, predicate);
        return new SetView<>(source, predicate);
    }

    /**
     * Returns a live sorted view retaining the owner's comparator and ranges.
     * @param <E> element type
     * @param source authoritative backing sorted set
     * @param predicate visibility test evaluated by live operations
     * @return a backed filtered sorted-set view
     * @throws NullPointerException if source or predicate is null
     */
    @SuppressWarnings("unchecked")
    public static <E> SortedSet<E> filtered(SortedSet<E> source,
                                          Predicate<? super E> predicate) {
        if (source instanceof NavigableSet<?>) return filtered((NavigableSet<E>) source, predicate);
        return new SortedView<>(source, predicate);
    }

    /**
     * Returns a live navigable view retaining the owner's navigation and ranges.
     * @param <E> element type
     * @param source authoritative backing navigable set
     * @param predicate visibility test evaluated by live operations
     * @return a backed filtered navigable-set view
     * @throws NullPointerException if source or predicate is null
     */
    public static <E> NavigableSet<E> filtered(NavigableSet<E> source,
                                             Predicate<? super E> predicate) {
        return new NavigableView<>(source, predicate);
    }

    /**
     * Collects a detached, stable sorted list in one source traversal.
     * Equal-comparator occurrences remain distinct. A null comparator selects
     * natural ordering, as for {@link List#sort(Comparator)}.
     * @param <E> element type
     * @param source source occurrences to collect
     * @param comparator sort comparator, or null for natural ordering
     * @return an independent unmodifiable list of stably sorted references
     * @throws NullPointerException if source is null
     */
    public static <E> List<E> sortedSnapshot(Collection<? extends E> source,
                                            Comparator<? super E> comparator) {
        Objects.requireNonNull(source, "source");
        ArrayList<E> result = new ArrayList<>();
        for (E value : source) result.add(value);
        result.sort(comparator);
        return Collections.unmodifiableList(result);
    }

    /**
     * Collects a detached encounter-order list with one predicate call per
     * source occurrence. It does not request source.size() before traversal.
     * Nulls and duplicate references survive when admitted by the predicate.
     * @param <E> element type
     * @param source source occurrences to collect
     * @param predicate test evaluated once for each source occurrence
     * @return an independent unmodifiable encounter-order list of selected references
     * @throws NullPointerException if source or predicate is null
     */
    public static <E> List<E> filteredSnapshot(Collection<? extends E> source,
                                              Predicate<? super E> predicate) {
        return Collections.unmodifiableList(collectSelected(source, predicate));
    }

    /**
     * Collects once, then stably sorts the selected detached references.
     * @param <E> element type
     * @param source source occurrences to collect
     * @param predicate test evaluated once for each source occurrence
     * @param comparator sort comparator, or null for natural ordering
     * @return an independent unmodifiable list of stably sorted selected references
     * @throws NullPointerException if source or predicate is null
     */
    public static <E> List<E> filteredSortedSnapshot(Collection<? extends E> source,
                                                    Predicate<? super E> predicate,
                                                    Comparator<? super E> comparator) {
        ArrayList<E> result = collectSelected(source, predicate);
        result.sort(comparator);
        return Collections.unmodifiableList(result);
    }

    private static <E> ArrayList<E> collectSelected(Collection<? extends E> source,
                                                   Predicate<? super E> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        ArrayList<E> result = new ArrayList<>();
        for (E value : source) if (predicate.test(value)) result.add(value);
        return result;
    }

    /* A persistent binary-carry tree retains callback references, never results.
     * Only this implementation's first conjunction operand is opened. A caller
     * predicate, including its virtual combinator behavior, remains one leaf.
     */
    private static final class PredicateNode<E> {
        final Predicate<? super E> callback;
        final PredicateNode<E> left;
        final PredicateNode<E> right;
        final int height;
        final boolean perfect;

        PredicateNode(Predicate<? super E> callback) {
            this.callback = callback;
            left = null;
            right = null;
            height = 0;
            perfect = true;
        }

        PredicateNode(PredicateNode<E> left, PredicateNode<E> right) {
            callback = null;
            this.left = left;
            this.right = right;
            height = Math.max(left.height, right.height) + 1;
            perfect = left.perfect && right.perfect && left.height == right.height;
        }

        boolean test(E value) {
            return callback != null ? callback.test(value) : left.test(value) && right.test(value);
        }
    }

    private static <E> PredicateNode<E> append(PredicateNode<E> prefix,
                                               PredicateNode<E> leaf) {
        return prefix.perfect ? new PredicateNode<>(prefix, leaf)
                : new PredicateNode<>(prefix.left, append(prefix.right, leaf));
    }

    private static final class Conjunction<E> implements Predicate<E> {
        final PredicateNode<E> root;
        Conjunction(PredicateNode<E> root) { this.root = root; }
        @Override public boolean test(E value) { return root.test(value); }
    }

    @SuppressWarnings("unchecked")
    private static <E> Predicate<E> compose(Predicate<? super E> first,
                                            Predicate<? super E> next) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(next, "next");
        PredicateNode<E> prefix = first instanceof Conjunction<?> previous
                ? ((Conjunction<E>) previous).root : new PredicateNode<>(first);
        return new Conjunction<>(append(prefix, new PredicateNode<>(next)));
    }

    private static class View<E> extends AbstractCollection<E> {
        final Collection<E> owner;
        final Predicate<? super E> accepts;

        @SuppressWarnings("unchecked")
        View(Collection<E> source, Predicate<? super E> predicate) {
            Objects.requireNonNull(source, "backing");
            Objects.requireNonNull(predicate, "predicate");
            if (source instanceof View<?> nested) {
                View<E> previous = (View<E>) nested;
                owner = previous.owner;
                accepts = compose(previous.accepts, predicate);
            } else {
                owner = source;
                accepts = predicate;
            }
        }

        @Override public Iterator<E> iterator() {
            return new SelectedIterator<>(owner.iterator(), accepts);
        }

        @Override public Spliterator<E> spliterator() {
            return new SelectedSpliterator<>(owner.spliterator(), accepts, 0, null);
        }

        @Override public int size() {
            int result = 0;
            for (E value : owner) {
                if (accepts.test(value)) result = result == Integer.MAX_VALUE ? result : result + 1;
            }
            return result;
        }

        @Override public boolean isEmpty() { return !iterator().hasNext(); }

        @Override public boolean contains(Object probe) {
            for (E stored : owner) {
                if (Objects.equals(probe, stored) && accepts.test(stored)) return true;
>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
            }
            return false;
        }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        @Override
        public boolean add(E element) {
            if (!visible(element)) {
                throw new IllegalArgumentException("element rejected by view predicate");
            }
            return backing.add(element);
        }

        @Override
        public boolean addAll(Collection<? extends E> elements) {
            Objects.requireNonNull(elements, "elements");
            for (E element : elements) {
                if (!visible(element)) {
                    throw new IllegalArgumentException("element rejected by view predicate");
                }
            }
            return backing.addAll(elements);
=======
        @Override public boolean add(E value) {
            if (!accepts.test(value)) {
                throw new IllegalArgumentException("element rejected by view predicate");
            }
            return owner.add(value);
        }

        @Override public boolean addAll(Collection<? extends E> values) {
            Objects.requireNonNull(values, "elements");
            return owner.addAll(collectSelected(values, value -> {
                if (!accepts.test(value)) {
                    throw new IllegalArgumentException("element rejected by view predicate");
                }
                return true;
            }));
>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
        }

        @Override
        @SuppressWarnings("unchecked")
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        public boolean remove(Object candidate) {
            if (backing instanceof Set<?>) {
                if (!backing.contains(candidate)) return false;
                E equivalent = null;
                boolean found = false;
                for (E stored : backing) {
                    if (stored == candidate) {
                        return visible(stored) && backing.remove(stored);
                    }
                    if (!found && Objects.equals(candidate, stored)) {
                        equivalent = stored;
                        found = true;
                    }
                }
                return found && visible(equivalent) && backing.remove(equivalent);
            }
            // A List can contain a hidden and visible occurrence that compare
            // equal. Removing by probe would delete the wrong physical element.
            // Indexed deletion also admits Lists whose iterators are read-only
            // but whose indexed removal is writable (CopyOnWriteArrayList).
            if (backing instanceof List<?>) {
                List<E> list = (List<E>) backing;
                int physical = 0;
                for (E stored : list) {
                    if (Objects.equals(candidate, stored) && visible(stored)) {
                        list.remove(physical);
                        return true;
                    }
                    physical++;
                }
                return false;
            }
            if (backing instanceof Set<?>) {
                // Select the actual stored member using this owner's membership
                // policy, including reference identity where applicable.
                if (!backing.contains(candidate)) return false;
                E equivalent = null;
                boolean found = false;
                for (E stored : backing) {
                    if (stored == candidate) {
                        return visible(stored) && backing.remove(stored);
                    }
                    if (!found && Objects.equals(candidate, stored)) {
                        equivalent = stored;
                        found = true;
                    }
                }
                return found && visible(equivalent) && backing.remove(equivalent);
            }
            Iterator<E> cursor = backing.iterator();
            boolean skippedEqual = false;
            while (cursor.hasNext()) {
                E stored = cursor.next();
                if (Objects.equals(candidate, stored)) {
                    if (visible(stored)) {
                        try {
                            cursor.remove();
                            return true;
                        } catch (UnsupportedOperationException unsupported) {
                            // A Set has at most one equal occurrence. For other
                            // owners, direct removal is safe only if no earlier
                            // hidden equal occurrence could be removed instead.
                            if (backing instanceof Set<?> || !skippedEqual) {
                                return backing.remove(stored);
                            }
                            throw unsupported;
                        }
                    }
                    skippedEqual = true;
=======
        public boolean remove(Object probe) {
            // A list's equal occurrences can have different visibility. Retain
            // the encountered physical ordinal, including writable COW lists.
            if (owner instanceof List<?>) {
                List<E> list = (List<E>) owner;
                int ordinal = 0;
                for (E stored : list) {
                    if (Objects.equals(probe, stored) && accepts.test(stored)) {
                        list.remove(ordinal);
                        return true;
                    }
                    ordinal++;
                }
                return false;
            }
            Iterator<E> cursor = owner.iterator();
            boolean earlierEqual = false;
            while (cursor.hasNext()) {
                E stored = cursor.next();
                if (Objects.equals(probe, stored)) {
                    if (accepts.test(stored)) {
                        try {
                            cursor.remove();
                            return true;
                        } catch (UnsupportedOperationException failure) {
                            if (owner instanceof Set<?> || !earlierEqual) return owner.remove(stored);
                            throw failure;
                        }
                    }
                    earlierEqual = true;
>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
                }
            }
            return false;
        }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        @Override
        public void clear() {
            removeMatching(element -> true);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter, "filter");
            return removeMatching(filter);
        }

        @Override
        public boolean removeAll(Collection<?> elements) {
            Objects.requireNonNull(elements, "elements");
            return removeMatching(elements::contains);
        }

        @Override
        public boolean retainAll(Collection<?> elements) {
            Objects.requireNonNull(elements, "elements");
            return removeMatching(element -> !elements.contains(element));
        }

        private boolean removeMatching(Predicate<? super E> condition) {
            boolean changed = false;
            Iterator<E> iterator = backing.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (visible(element) && condition.test(element)) {
                    iterator.remove();
=======
        @Override public void clear() { removeSelected(value -> true); }

        @Override public boolean removeIf(Predicate<? super E> predicate) {
            Objects.requireNonNull(predicate, "filter");
            return removeSelected(predicate);
        }

        @Override public boolean removeAll(Collection<?> values) {
            Objects.requireNonNull(values, "elements");
            return removeSelected(values::contains);
        }

        @Override public boolean retainAll(Collection<?> values) {
            Objects.requireNonNull(values, "elements");
            return removeSelected(value -> !values.contains(value));
        }

        private boolean removeSelected(Predicate<? super E> condition) {
            boolean changed = false;
            Iterator<E> cursor = owner.iterator();
            while (cursor.hasNext()) {
                E value = cursor.next();
                if (accepts.test(value) && condition.test(value)) {
                    cursor.remove();
>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
                    changed = true;
                }
            }
            return changed;
        }
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** Reuses all collection mutations while implementing the Set value contract. */
    private static class FilteredSet<E> extends FilteredCollection<E> implements Set<E> {
        private FilteredSet(Set<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
        }

        /**
         * Preserve owner membership before resolving stored visibility. Identity
         * sets may retain equals-equal references, so an exact reference wins.
         */
        @Override
        public boolean contains(Object candidate) {
            if (!backing.contains(candidate)) return false;
            for (E stored : backing) {
                if (stored == candidate) return predicate.test(stored);
            }
            return super.contains(candidate);
        }

        @Override
        public boolean remove(Object candidate) {
            if (!backing.contains(candidate)) return false;
            Iterator<E> cursor = backing.iterator();
            while (cursor.hasNext()) {
                E stored = cursor.next();
                if (stored == candidate) {
                    if (!predicate.test(stored)) return false;
                    try {
                        cursor.remove();
                        return true;
                    } catch (UnsupportedOperationException unsupported) {
                        // Preserve the existing Set owner fallback, including
                        // CopyOnWriteArraySet's read-only traversal cursors.
                        return backing.remove(stored);
                    }
                }
            }
            return super.remove(candidate);
        }

        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(backing.spliterator(), predicate,
                    Spliterator.DISTINCT, null);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Set<?> otherSet)) {
                return false;
            }
            try {
                return size() == otherSet.size() && containsAll(otherSet);
            } catch (ClassCastException | NullPointerException ex) {
=======
    private static class SetView<E> extends View<E> implements Set<E> {
        SetView(Set<E> source, Predicate<? super E> predicate) { super(source, predicate); }

        @Override public boolean contains(Object probe) {
            if (!owner.contains(probe)) return false;
            for (E stored : owner) if (stored == probe) return accepts.test(stored);
            return super.contains(probe);
        }

        @Override public boolean remove(Object probe) {
            if (!owner.contains(probe)) return false;
            Iterator<E> cursor = owner.iterator();
            while (cursor.hasNext()) {
                E stored = cursor.next();
                if (stored == probe) {
                    if (!accepts.test(stored)) return false;
                    try {
                        cursor.remove();
                        return true;
                    } catch (UnsupportedOperationException failure) {
                        return owner.remove(stored);
                    }
                }
            }
            return super.remove(probe);
        }

        @Override public Spliterator<E> spliterator() {
            return new SelectedSpliterator<>(owner.spliterator(), accepts, Spliterator.DISTINCT, null);
        }

        @Override public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Set<?> set)) return false;
            try {
                return size() == set.size() && containsAll(set);
            } catch (ClassCastException | NullPointerException failure) {
>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
                return false;
            }
        }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        @Override
        public int hashCode() {
            int hash = 0;
            for (E element : this) {
                if (element != null) {
                    hash += element.hashCode();
                }
            }
            return hash;
        }
    }

    /** Filtered sorted range views remain backed and inherit source comparators. */
    private static class FilteredSortedSet<E> extends FilteredSet<E> implements SortedSet<E> {
        private final SortedSet<E> sorted;

        private FilteredSortedSet(SortedSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            this.sorted = (SortedSet<E>) super.backing;
        }

        // M3_SORTED_COMPARATOR_EQUIVALENCE_V1
        /**
         * A SortedSet locates elements by comparator equivalence, not Object.equals.
         * Test visibility on the stored representative rather than the probe.
         */
        @Override
        public boolean contains(Object probe) {
            if (!sorted.contains(probe)) {
                return false;
            }
            return super.predicate.test(storedRepresentative(probe));
        }

        @Override
        public boolean remove(Object probe) {
            if (!sorted.contains(probe)) {
                return false;
            }
            E stored = storedRepresentative(probe);
            return super.predicate.test(stored) && sorted.remove(stored);
        }

        @SuppressWarnings("unchecked")
        private E storedRepresentative(Object probe) {
            // contains() has already validated this exact comparator class.
            return sorted.tailSet((E) probe).first();
        }

        @Override
        public Comparator<? super E> comparator() {
            return sorted.comparator();
        }

        /** Owner order is authoritative, including bounded and descending views. */
        @Override
        public Spliterator<E> spliterator() {
            return new SelectionSpliterator<>(sorted.spliterator(), super.predicate,
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED,
                    sorted.comparator());
        }

        @Override
        public SortedSet<E> subSet(E fromInclusive, E toExclusive) {
            return filtered(sorted.subSet(fromInclusive, toExclusive), super.predicate);
        }

        @Override
        public SortedSet<E> headSet(E toExclusive) {
            return filtered(sorted.headSet(toExclusive), super.predicate);
        }

        @Override
        public SortedSet<E> tailSet(E fromInclusive) {
            return filtered(sorted.tailSet(fromInclusive), super.predicate);
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
            E current;
            do {
                current = iterator.next();
            } while (iterator.hasNext());
            return current;
        }
    }

    /** Filtered navigation seeks through ordered source ranges without an index copy. */
    private static final class FilteredNavigableSet<E> extends FilteredSortedSet<E>
            implements NavigableSet<E> {
        private final NavigableSet<E> navigable;

        private FilteredNavigableSet(NavigableSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            this.navigable = (NavigableSet<E>) super.backing;
        }

        @Override
        public E lower(E element) {
            return seek(element, false, true);
        }

        @Override
        public E floor(E element) {
            return seek(element, true, true);
        }

        @Override
        public E ceiling(E element) {
            return seek(element, true, false);
        }

        @Override
        public E higher(E element) {
            return seek(element, false, false);
        }

        /**
         * A navigation key need not lie inside a backing range. Only construct a
         * new range when the key lies between its current first/last members;
         * otherwise use the whole ordered cursor or an empty result. This keeps
         * O(1) scratch and the backing seek plus rejected-member scan, without
         * treating a legitimate null member as an exhaustion sentinel.
         */
        private E seek(E element, boolean inclusive, boolean descending) {
            NavigableSet<E> ordered = descending ? navigable.descendingSet() : navigable;
            Comparator<? super E> order = ordered.comparator();
            compareKey(element, element, order); // Keep key-domain admission on empty sets.
            Iterator<E> iterator = ordered.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            int fromFirst = compareKey(element, iterator.next(), order);
            if (fromFirst < 0 || fromFirst == 0 && inclusive) {
                return firstOrNull(selected(ordered.iterator(), super.predicate));
            }
            int fromLast = compareKey(element, ordered.last(), order);
            if (fromLast > 0 || fromLast == 0 && !inclusive) {
                return null;
            }
            return firstOrNull(selected(ordered.tailSet(element, inclusive).iterator(), super.predicate));
        }

        @SuppressWarnings("unchecked")
        private int compareKey(E left, E right, Comparator<? super E> order) {
            return order == null ? ((Comparable<? super E>) left).compareTo(right) : order.compare(left, right);
        }

        @Override
        public E last() {
            Iterator<E> iterator = descendingIterator();
            if (!iterator.hasNext()) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public E pollFirst() {
            return removeFirst(iterator());
        }

        @Override
        public E pollLast() {
            return removeFirst(descendingIterator());
        }

        private E removeFirst(Iterator<E> iterator) {
            if (!iterator.hasNext()) {
                return null;
            }
            E selected = iterator.next();
            navigable.remove(selected);
            return selected;
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return filtered(navigable.descendingSet(), super.predicate);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return selected(navigable.descendingIterator(), super.predicate);
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return filtered(navigable.subSet(
                    fromElement, fromInclusive, toElement, toInclusive), super.predicate);
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return filtered(navigable.headSet(toElement, inclusive), super.predicate);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return filtered(navigable.tailSet(fromElement, inclusive), super.predicate);
        }
    }

    /** Per-traversal state only; no cached element, copied index or result array. */
    private static final class SelectionSpliterator<E> implements Spliterator<E>, Consumer<E> {
        private final Spliterator<E> source;
        private final Predicate<? super E> predicate;
        private final int guaranteed;
        private final Comparator<? super E> ownerComparator;
        private Consumer<? super E> action;
        private boolean delivered;

        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate) {
            this(source, predicate, 0, null);
        }

        private SelectionSpliterator(Spliterator<E> source, Predicate<? super E> predicate,
                int guaranteed, Comparator<? super E> ownerComparator) {
            this.source = source;
            this.predicate = predicate;
            this.guaranteed = guaranteed;
            this.ownerComparator = ownerComparator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer, "action");
            Consumer<? super E> enclosingAction = action;
            boolean enclosingDelivered = delivered;
            action = consumer;
            delivered = false;
            try {
                while (!delivered && source.tryAdvance(this)) {
                    // Advance past rejected values without a second scan or buffer.
                }
                return delivered;
            } finally {
                action = enclosingAction;
                delivered = enclosingDelivered;
            }
        }

        @Override
        public void accept(E element) {
            if (predicate.test(element)) {
                delivered = true;
                action.accept(element);
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer, "action");
            // The callback is operation-local, never one allocation per element.
            source.forEachRemaining(element -> {
                if (predicate.test(element)) consumer.accept(element);
            });
        }

        @Override
        public Spliterator<E> trySplit() {
            Spliterator<E> prefix = source.trySplit();
            return prefix == null ? null : new SelectionSpliterator<>(prefix, predicate, guaranteed, ownerComparator);
        }

        @Override
        public long estimateSize() {
            return source.estimateSize();
        }

        @Override
        public int characteristics() {
            return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE)) | guaranteed;
        }

        @Override
        public Comparator<? super E> getComparator() {
            return (guaranteed & SORTED) != 0 ? ownerComparator : source.getComparator();
        }
    }

    private static <E> E firstOrNull(Iterator<E> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    private static <E> Iterator<E> selected(
            Iterator<E> source, Predicate<? super E> predicate) {
        return new Iterator<>() {
            private boolean prepared;
            private boolean found;
            private E next;

            @Override
            public boolean hasNext() {
                if (!prepared) {
                    prepared = true;
                    while (source.hasNext()) {
                        E candidate = source.next();
                        if (predicate.test(candidate)) {
                            next = candidate;
                            found = true;
                            break;
                        }
                    }
                }
                return found;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E result = next;
                next = null;
                prepared = false;
                found = false;
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("filtered iterator removal");
            }
        };
=======
        @Override public int hashCode() {
            int result = 0;
            for (E value : this) if (value != null) result += value.hashCode();
            return result;
        }
    }

    private static class SortedView<E> extends SetView<E> implements SortedSet<E> {
        final SortedSet<E> ordered;

        SortedView(SortedSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            ordered = (SortedSet<E>) owner;
        }

        @Override public boolean contains(Object probe) {
            if (!ordered.contains(probe)) return false;
            return accepts.test(representative(probe));
        }

        @Override public boolean remove(Object probe) {
            if (!ordered.contains(probe)) return false;
            E stored = representative(probe);
            return accepts.test(stored) && ordered.remove(stored);
        }

        @SuppressWarnings("unchecked")
        private E representative(Object probe) {
            return ordered.tailSet((E) probe).first();
        }

        @Override public Comparator<? super E> comparator() { return ordered.comparator(); }

        @Override public Spliterator<E> spliterator() {
            return new SelectedSpliterator<>(ordered.spliterator(), accepts,
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED,
                    ordered.comparator());
        }

        @Override public SortedSet<E> subSet(E first, E last) {
            return filtered(ordered.subSet(first, last), accepts);
        }
        @Override public SortedSet<E> headSet(E last) { return filtered(ordered.headSet(last), accepts); }
        @Override public SortedSet<E> tailSet(E first) { return filtered(ordered.tailSet(first), accepts); }

        @Override public E first() {
            Iterator<E> cursor = iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            return cursor.next();
        }

        @Override public E last() {
            Iterator<E> cursor = iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            E value;
            do { value = cursor.next(); } while (cursor.hasNext());
            return value;
        }
    }

    private static final class NavigableView<E> extends SortedView<E> implements NavigableSet<E> {
        final NavigableSet<E> navigable;

        NavigableView(NavigableSet<E> source, Predicate<? super E> predicate) {
            super(source, predicate);
            navigable = (NavigableSet<E>) owner;
        }

        @Override public E lower(E key) { return seek(key, false, true); }
        @Override public E floor(E key) { return seek(key, true, true); }
        @Override public E ceiling(E key) { return seek(key, true, false); }
        @Override public E higher(E key) { return seek(key, false, false); }

        private E seek(E key, boolean inclusive, boolean descending) {
            NavigableSet<E> traversal = descending ? navigable.descendingSet() : navigable;
            Comparator<? super E> comparator = traversal.comparator();
            compare(key, key, comparator);
            Iterator<E> cursor = traversal.iterator();
            if (!cursor.hasNext()) return null;
            int first = compare(key, cursor.next(), comparator);
            if (first < 0 || first == 0 && inclusive) {
                return firstOrNull(new SelectedIterator<>(traversal.iterator(), accepts));
            }
            int last = compare(key, traversal.last(), comparator);
            if (last > 0 || last == 0 && !inclusive) return null;
            return firstOrNull(new SelectedIterator<>(traversal.tailSet(key, inclusive).iterator(), accepts));
        }

        @SuppressWarnings("unchecked")
        private int compare(E left, E right, Comparator<? super E> comparator) {
            return comparator == null ? ((Comparable<? super E>) left).compareTo(right)
                    : comparator.compare(left, right);
        }

        @Override public E last() {
            Iterator<E> cursor = descendingIterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            return cursor.next();
        }

        @Override public E pollFirst() { return removeEndpoint(iterator()); }
        @Override public E pollLast() { return removeEndpoint(descendingIterator()); }

        private E removeEndpoint(Iterator<E> cursor) {
            if (!cursor.hasNext()) return null;
            E value = cursor.next();
            navigable.remove(value);
            return value;
        }

        @Override public NavigableSet<E> descendingSet() {
            return filtered(navigable.descendingSet(), accepts);
        }
        @Override public Iterator<E> descendingIterator() {
            return new SelectedIterator<>(navigable.descendingIterator(), accepts);
        }
        @Override public NavigableSet<E> subSet(E first, boolean firstInclusive,
                                               E last, boolean lastInclusive) {
            return filtered(navigable.subSet(first, firstInclusive, last, lastInclusive), accepts);
        }
        @Override public NavigableSet<E> headSet(E last, boolean inclusive) {
            return filtered(navigable.headSet(last, inclusive), accepts);
        }
        @Override public NavigableSet<E> tailSet(E first, boolean inclusive) {
            return filtered(navigable.tailSet(first, inclusive), accepts);
        }
    }

    private static <E> E firstOrNull(Iterator<E> cursor) {
        return cursor.hasNext() ? cursor.next() : null;
    }

    /* Preparation is set before advancing an opaque source or predicate. This
     * intentionally preserves the incumbent cursor's failure/retry semantics.
     */
    private static final class SelectedIterator<E> implements Iterator<E> {
        final Iterator<E> source;
        final Predicate<? super E> predicate;
        boolean prepared;
        boolean found;
        E value;

        SelectedIterator(Iterator<E> source, Predicate<? super E> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override public boolean hasNext() {
            if (!prepared) {
                prepared = true;
                while (source.hasNext()) {
                    E candidate = source.next();
                    if (predicate.test(candidate)) {
                        value = candidate;
                        found = true;
                        break;
                    }
                }
            }
            return found;
        }

        @Override public E next() {
            if (!hasNext()) throw new NoSuchElementException();
            E result = value;
            value = null;
            prepared = false;
            found = false;
            return result;
        }

        @Override public void remove() {
            throw new UnsupportedOperationException("filtered iterator removal");
        }
    }

    private static final class SelectedSpliterator<E> implements Spliterator<E>, Consumer<E> {
        final Spliterator<E> source;
        final Predicate<? super E> predicate;
        final int guaranteed;
        final Comparator<? super E> comparator;
        Consumer<? super E> action;
        boolean delivered;

        SelectedSpliterator(Spliterator<E> source, Predicate<? super E> predicate,
                            int guaranteed, Comparator<? super E> comparator) {
            this.source = source;
            this.predicate = predicate;
            this.guaranteed = guaranteed;
            this.comparator = comparator;
        }

        @Override public boolean tryAdvance(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer, "action");
            Consumer<? super E> savedAction = action;
            boolean savedDelivered = delivered;
            action = consumer;
            delivered = false;
            try {
                while (!delivered && source.tryAdvance(this)) { }
                return delivered;
            } finally {
                action = savedAction;
                delivered = savedDelivered;
            }
        }

        @Override public void accept(E value) {
            if (predicate.test(value)) {
                delivered = true;
                action.accept(value);
            }
        }

        @Override public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer, "action");
            source.forEachRemaining(value -> {
                if (predicate.test(value)) consumer.accept(value);
            });
        }

        @Override public Spliterator<E> trySplit() {
            Spliterator<E> prefix = source.trySplit();
            return prefix == null ? null
                    : new SelectedSpliterator<>(prefix, predicate, guaranteed, comparator);
        }

        @Override public long estimateSize() { return source.estimateSize(); }
        @Override public int characteristics() {
            return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE))
                    | guaranteed;
        }
        @Override public Comparator<? super E> getComparator() {
            return (guaranteed & SORTED) != 0 ? comparator : source.getComparator();
        }
>>>>>>> b2d03f8fccfdce93696e6162246b6f84b998f7f9
    }
}
