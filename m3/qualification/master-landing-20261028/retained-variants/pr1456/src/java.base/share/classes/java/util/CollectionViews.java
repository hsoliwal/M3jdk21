/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
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
            }
            return false;
        }

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
        }

        @Override
        @SuppressWarnings("unchecked")
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
                }
            }
            return false;
        }

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
                    changed = true;
                }
            }
            return changed;
        }
    }

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
                return false;
            }
        }

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
    }
}
