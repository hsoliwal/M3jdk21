// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Mutable comparator-ordered set of canonical MIndexSpace IDs.
 *
 * <p>All range/reverse views share the same primitive topology. No per-element
 * node or payload reference is retained here. Lookup/navigation is O(log n),
 * insertion/removal O(n); this is not an automatic TreeSet replacement.
 * Comparator order must be stable and consistent with equals for Set contracts.
 * Non-null admission must project the exact supplied reference. A canonicalizing
 * space that substitutes a distinct reference is rejected before publication.
 * Null is a -1 ID sentinel and requires a comparator accepting null.
 *
 * <p>Not concurrent or serializable. Domains retain admitted identities after
 * removal; domain lifecycle remains their owner's responsibility. Heap admission
 * is advisory and does not add paging, native residency or an OOME guarantee.
 */
public final class M3IndexNavigableSet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final M3IndexRange<E> range;
    private final boolean additions;

    public M3IndexNavigableSet(MIndexSpace<E> space, Comparator<? super E> comparator) {
        this(new M3IndexRange<>(new M3IndexOrderedStore<>(space, comparator, false)), true);
    }

    public M3IndexNavigableSet(MIndexSpace<E> space) { this(space, null); }

    M3IndexNavigableSet(M3IndexRange<E> range, boolean additions) {
        this.range = range; this.additions = additions;
    }

    @Override public int size() { return range.size(); }
    @Override public Comparator<? super E> comparator() { return range.comparator(); }
    @Override public Iterator<E> iterator() { return range.iterator(range.store::key); }
    @Override public Iterator<E> descendingIterator() { return descendingSet().iterator(); }

    @Override public boolean add(E element) {
        if (!additions) throw new UnsupportedOperationException("map key view");
        range.requireKey(element);
        M3IndexOrderedStore<E> store = range.store;
        int ordinal = store.bound(element, false);
        if (ordinal < store.size && store.compare(store.key(ordinal), element) == 0) return false;
        int id = store.admit(store.space, element);
        store.insert(ordinal, id, -1);
        return true;
    }

    @Override public boolean contains(Object value) { return range.find(cast(value)) >= 0; }
    @Override public boolean remove(Object value) {
        int ordinal = range.find(cast(value));
        if (ordinal < 0) return false;
        range.store.remove(ordinal); return true;
    }
    @Override public void clear() { range.clear(); }

    private E at(int ordinal) { return ordinal < 0 ? null : range.store.key(ordinal); }
    @Override public E lower(E element) { return at(range.seek(element, -2)); }
    @Override public E floor(E element) { return at(range.seek(element, -1)); }
    @Override public E ceiling(E element) { return at(range.seek(element, 1)); }
    @Override public E higher(E element) { return at(range.seek(element, 2)); }
    @Override public E first() { return required(range.first()); }
    @Override public E last() { return required(range.last()); }
    private E required(int ordinal) {
        if (ordinal < 0) throw new NoSuchElementException();
        return range.store.key(ordinal);
    }
    private E poll(int ordinal) {
        if (ordinal < 0) return null;
        E value = range.store.key(ordinal); range.store.remove(ordinal); return value;
    }
    @Override public E pollFirst() { return poll(range.first()); }
    @Override public E pollLast() { return poll(range.last()); }

    @Override public M3IndexNavigableSet<E> descendingSet() {
        return new M3IndexNavigableSet<>(range.reverse(), additions);
    }
    @Override public M3IndexNavigableSet<E> reversed() { return descendingSet(); }
    @Override public M3IndexNavigableSet<E> subSet(E from, boolean fi, E to, boolean ti) {
        return new M3IndexNavigableSet<>(range.sub(from, fi, to, ti), additions);
    }
    @Override public M3IndexNavigableSet<E> headSet(E to, boolean inclusive) {
        return new M3IndexNavigableSet<>(range.head(to, inclusive), additions);
    }
    @Override public M3IndexNavigableSet<E> tailSet(E from, boolean inclusive) {
        return new M3IndexNavigableSet<>(range.tail(from, inclusive), additions);
    }
    @Override public M3IndexNavigableSet<E> subSet(E from, E to) { return subSet(from, true, to, false); }
    @Override public M3IndexNavigableSet<E> headSet(E to) { return headSet(to, false); }
    @Override public M3IndexNavigableSet<E> tailSet(E from) { return tailSet(from, true); }

    /** Canonical IDs in this view's encounter order; -1 represents null. No boxing. */
    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        M3IndexOrderedStore<E> store = range.store;
        int start = range.start(), end = range.end();
        long expected = store.mutation;
        for (int i = range.descending ? end - 1 : start;
                i >= start && i < end; i += range.descending ? -1 : 1) {
            action.accept(store.keys[i]); store.checkMutation(expected);
        }
    }

    /** Explicit caller-owned primitive export, not a retained payload copy. */
    public int[] snapshotIds() {
        int[] result = new int[size()];
        int start = range.start(), end = range.end(), output = 0;
        for (int i = range.descending ? end - 1 : start;
                i >= start && i < end; i += range.descending ? -1 : 1) result[output++] = range.store.keys[i];
        return result;
    }

    public MIndexSpace<E> space() { return range.store.space; }

    @SuppressWarnings("unchecked") private E cast(Object value) { return (E) value; }
}
