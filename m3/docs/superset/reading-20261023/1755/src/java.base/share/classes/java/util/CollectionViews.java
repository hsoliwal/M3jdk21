/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: M3 receiver namespace, runtime-capability dispatch,
 * direct owner membership/removal and single-pass sorted snapshot acquisition.
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
 * Modified 2026-10-17: stack-safe deferred predicate plans for nested filters.
 * Modified 2026-10-17: java.base receiver; sole implementation behind legacy facade.
=======
 * Identity-set refinement: owner-gated membership and exact-reference removal.
 * Modified 2026-10-20: forward-reconcile the existing proposed java.base owner
 * from M3JDK21 PR 1551 with current collection behavioral atoms and COW removal.
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
import java.util.function.BiPredicate;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Composable backed filter views and explicit sorted snapshots of JDK collections.
 *
 * <p>These are reference collection adapters, not replacements for the existing
 * primitive admission filters or primitive indexed views. A filtered view
 * retains only its backing collection and predicate, never one object per entry.
 * Filter predicates should be pure and stable with respect to the equality and
 * ordering semantics of the backing collection.
 *
 * <p>Backed views are not automatically synchronized, serializable or snapshots.
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
 * Iterator removal is deliberately unsupported; bulk mutations and explicit
 * {@code remove} do write through to the underlying collection when supported.
=======
 * Filtered iterator removal is deliberately unsupported; bulk mutations and
 * explicit {@code remove} write through when the owner supports the required
 * physical deletion. Equal external probes never bypass stored visibility.
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /**
     * Returns a live view, preserving runtime set/sorted/navigable capabilities.
     *
     * @param <E> the type of elements in the backing collection
     * @param backing the collection to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live view of the backing elements accepted by the predicate
     */
=======
    /** Returns a live view, preserving runtime set/sorted/navigable capabilities. */
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> filtered(
            Collection<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        if (backing instanceof Set<?>) return filtered((Set<E>) backing, predicate);
        return new FilteredCollection<>(backing, predicate);
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /**
     * Retains runtime sorted capability and set equality in the live view.
     *
     * @param <E> the type of elements in the backing set
     * @param backing the set to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live set view of the backing elements accepted by the predicate
     */
=======
    /** Retains runtime sorted capability and set equality in the live view. */
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    @SuppressWarnings("unchecked")
    public static <E> Set<E> filtered(Set<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        if (backing instanceof SortedSet<?>) return filtered((SortedSet<E>) backing, predicate);
        return new FilteredSet<>(backing, predicate);
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /**
     * Retains the comparator, range views and runtime navigable capability.
     *
     * @param <E> the type of elements in the backing sorted set
     * @param backing the sorted set to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live sorted-set view of the elements accepted by the predicate
     */
=======
    /** Retains the comparator, range views and runtime navigable capability. */
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    @SuppressWarnings("unchecked")
    public static <E> SortedSet<E> filtered(
            SortedSet<E> backing, Predicate<? super E> predicate) {
        if (backing instanceof NavigableSet<?>) return filtered((NavigableSet<E>) backing, predicate);
        return new FilteredSortedSet<>(backing, predicate);
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /**
     * Retains navigable operations, descending order and range-view contracts.
     *
     * @param <E> the type of elements in the backing navigable set
     * @param backing the navigable set to filter
     * @param predicate the predicate that determines which elements are visible
     * @return a live navigable-set view of the elements accepted by the predicate
     */
=======
    /** Retains navigable operations, descending order and range-view contracts. */
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    public static <E> NavigableSet<E> filtered(
            NavigableSet<E> backing, Predicate<? super E> predicate) {
        return new FilteredNavigableSet<>(backing, predicate);
    }

    /**
     * Returns an independent, stable, read-only list sorted by the comparator.
     * Equal-comparator elements are not deduplicated. Unlike a backed filter,
     * this operation copies references and does not reflect later source changes.
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     *
     * @param <E> the type of elements in the snapshot
     * @param source the collection whose elements are copied
     * @param comparator the comparator used to order the elements, or
     *        {@code null} to use their natural ordering
     * @return an unmodifiable list snapshot in the specified order
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Additive collection API: detached encounter-order snapshot with one predicate
=======
     * M3 additive API: detached encounter-order snapshot with one predicate
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
     * evaluation per source occurrence. Unlike a backed filter, no later source
     * additions/removals are reflected. Nulls and duplicate references survive.
     * This never calls source.size(), performs no filter size prepass, and does
     * not change Collection.toArray or any existing JDK/Synexia contract.
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     *
     * @param <E> the type of elements in the snapshot
     * @param source the collection whose elements are filtered and copied
     * @param predicate the predicate that selects elements for the snapshot
     * @return an unmodifiable list of selected elements in encounter order
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
     */
    public static <E> List<E> filteredSnapshot(
            Collection<? extends E> source, Predicate<? super E> predicate) {
        return Collections.unmodifiableList(selectedCopy(source, predicate));
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
     * M3 additive API: fused filter and stable sort over one detached array
     * owner. Comparator-equivalent elements remain distinct and preserve their
     * original encounter order. A null comparator means natural ordering, as
     * specified by List.sort; mutable element objects are not deep-copied.
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
     */
    public static <E> List<E> filteredSortedSnapshot(
            Collection<? extends E> source, Predicate<? super E> predicate,
            Comparator<? super E> comparator) {
        ArrayList<E> selected = selectedCopy(source, predicate);
        selected.sort(comparator);
        return Collections.unmodifiableList(selected);
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** A cohesive one-pass behavioral atom shared by the snapshot APIs. */
=======
    /** A cohesive one-pass behavioral atom shared by the two new M3 APIs. */
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
    /** Shared count atom: callers maintain a nonnegative JDK Collection/Map cardinality. */
    static int incrementSize(int count) {
        return count == Integer.MAX_VALUE ? count : count + 1;
    }

    /**
     * Persistent binary-carry predicate composition shared by Set and Map views.
     * Only M3-created conjunctions are opened; caller predicates remain opaque.
     * Order, duplicate occurrences, short circuiting and thrown objects survive.
     * A perfect subtree becomes an immutable prefix on append; only the smaller
     * suffix is rebuilt. For d leaves, height is ceil(log2(d)), retained storage
     * is O(d), append is O(log d), and testing allocates no traversal buffer.
     * This bounds M3 composition frames, not recursion inside a caller predicate.
     */
    @SuppressWarnings("unchecked")
    static <E> Predicate<E> orderedAnd(
            Predicate<? super E> first, Predicate<? super E> next) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(next, "next");
        FilterNode<Predicate<? super E>> prefix = first instanceof OrderedPredicate<?> old
                ? ((OrderedPredicate<E>) old).root : new FilterNode<>(first);
        return new OrderedPredicate<>(appendFilter(prefix, new FilterNode<>(next)));
    }

    @SuppressWarnings("unchecked")
    static <K, V> BiPredicate<K, V> orderedAnd(
            BiPredicate<? super K, ? super V> first,
            BiPredicate<? super K, ? super V> next) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(next, "next");
        FilterNode<BiPredicate<? super K, ? super V>> prefix = first instanceof OrderedBiPredicate<?, ?> old
                ? ((OrderedBiPredicate<K, V>) old).root : new FilterNode<>(first);
        return new OrderedBiPredicate<>(appendFilter(prefix, new FilterNode<>(next)));
    }

    private static final class FilterNode<P> {
        private final P leaf;
        private final FilterNode<P> left;
        private final FilterNode<P> right;
        private final int height;
        private final boolean perfect;

        private FilterNode(P leaf) {
            this.leaf = leaf;
            this.left = null;
            this.right = null;
            this.height = 0;
            this.perfect = true;
        }

        private FilterNode(FilterNode<P> left, FilterNode<P> right) {
            this.leaf = null;
            this.left = left;
            this.right = right;
            this.height = Math.max(left.height, right.height) + 1;
            this.perfect = left.perfect && right.perfect && left.height == right.height;
        }
    }

    private static <P> FilterNode<P> appendFilter(FilterNode<P> prefix, FilterNode<P> leaf) {
        if (prefix.perfect) return new FilterNode<>(prefix, leaf);
        return new FilterNode<>(prefix.left, appendFilter(prefix.right, leaf));
    }

    private static final class OrderedPredicate<E> implements Predicate<E> {
        private final FilterNode<Predicate<? super E>> root;

        private OrderedPredicate(FilterNode<Predicate<? super E>> root) {
            this.root = root;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        }

        @Override
        public boolean test(E element) {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
            return evaluate(root, element);
        }

        private static <E> boolean evaluate(FilterNode<Predicate<? super E>> node, E element) {
            if (node.leaf != null) return node.leaf.test(element);
            return evaluate(node.left, element) && evaluate(node.right, element);
        }
    }

    private static final class OrderedBiPredicate<K, V> implements BiPredicate<K, V> {
        private final FilterNode<BiPredicate<? super K, ? super V>> root;

        private OrderedBiPredicate(FilterNode<BiPredicate<? super K, ? super V>> root) {
            this.root = root;
        }

        @Override
        public boolean test(K key, V value) {
            return evaluate(root, key, value);
        }

        private static <K, V> boolean evaluate(
                FilterNode<BiPredicate<? super K, ? super V>> node, K key, V value) {
            if (node.leaf != null) return node.leaf.test(key, value);
            return evaluate(node.left, key, value) && evaluate(node.right, key, value);
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
                this.predicate = new FilterChain<>(previous.predicate, filter);
=======
                Predicate<? super E> existing = previous.predicate;
                this.predicate = orderedAnd(existing, filter);
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
                    count++;
=======
                    count = incrementSize(count);
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
            // Collection membership is defined by the stored occurrence, not by a
            // caller-provided equal probe. Equivalence does not imply that a
            // predicate depending on other fields agrees for both instances.
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
            for (E stored : backing) {
                if (Objects.equals(candidate, stored) && visible(stored)) {
                    return true;
                }
            }
            return false;
        }

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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            for (E element : elements) {
                if (!visible(element)) {
                    throw new IllegalArgumentException("element rejected by view predicate");
                }
            }
            return backing.addAll(elements);
=======
            // Hand the owner only the references admitted by this traversal.
            // Reuse the existing snapshot atom; never read the caller twice.
            return backing.addAll(selectedCopy(elements, element -> {
                if (!visible(element)) {
                    throw new IllegalArgumentException("element rejected by view predicate");
                }
                return true;
            }));
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean remove(Object candidate) {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
                }
            }
            return false;
        }

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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
            if (backing instanceof java.util.concurrent.CopyOnWriteArrayList<?>) {
                // Snapshot iterators cannot remove. Let the actual owner publish its
                // survivor array only after every callback has completed successfully.
                // Other owners retain their existing iterator/partial-write behavior.
                return backing.removeIf(element -> visible(element) && condition.test(element));
            }
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
            boolean changed = false;
            Iterator<E> iterator = backing.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (visible(element) && condition.test(element)) {
                    iterator.remove();
                    changed = true;
                }
            }
            return changed;
        }
    }

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
                return false;
            }
        }

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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            Consumer<? super E> enclosingAction = action;
            boolean enclosingDelivered = delivered;
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
            action = consumer;
            delivered = false;
            try {
                while (!delivered && source.tryAdvance(this)) {
                    // Advance past rejected values without a second scan or buffer.
                }
                return delivered;
            } finally {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
                action = enclosingAction;
                delivered = enclosingDelivered;
=======
                action = null;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
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
    }
}
