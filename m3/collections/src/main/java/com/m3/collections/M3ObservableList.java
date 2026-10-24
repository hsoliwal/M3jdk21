// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.RandomAccess;

/**
 * Observable {@link java.util.List} using the existing dynamic-array M3 owner.
 * The delegate owns all elements; there are no retained per-element event nodes.
 * Changes are delivered synchronously after a successful write without an event
 * object allocation. Bulk changes carry an index and count, not an eagerly
 * copied payload list. The old/new value parameters apply only to single-slot
 * changes; a null value is valid and must not be treated as an absence marker.
 *
 * <p>Listener registration is copy-on-write, so changing subscriptions during
 * dispatch affects later events, not the current dispatch. Listener failures
 * propagate after the underlying mutation has committed, stopping dispatch.
 * Reentrant mutation during dispatch is rejected; reading is permitted.
 * This class requires external synchronization for concurrent access.
 *
 * <p>This is a Java collection notification primitive, not an implementation
 * of EMF {@code EList}, {@code NotificationChain}, inverse containment, proxy
 * resolution, or Eclipse's resource notification service. Those policies
 * belong to the compatibility receiver and require separate contract tests.
 */
public final class M3ObservableList<E> extends AbstractList<E> implements RandomAccess {
    /** The kind of a committed change; {@code count} records a half-open range. */
    public enum Change { ADD, REMOVE, SET, CLEAR }

    /** No event object is allocated to notify a change. */
    @FunctionalInterface
    public interface Listener<T> {
        void changed(M3ObservableList<T> source, Change change, int index, int count,
                     T oldValue, T newValue, long generation);
    }

    private static final Object[] NO_LISTENERS = new Object[0];
    private final M3ArrayList<E> elements;
    private Object[] listeners = NO_LISTENERS;
    private long generation;
    private boolean dispatching;

    public M3ObservableList() {
        elements = new M3ArrayList<>();
    }

    /** Constructs a detached list; initial contents do not issue notifications. */
    public M3ObservableList(Collection<? extends E> source) {
        elements = new M3ArrayList<>(Objects.requireNonNull(source, "source"));
    }

    public long generation() { return generation; }
    public int listenerCount() { return listeners.length; }

    /**
     * A read-only, live filtered projection of the same dynamic-array owner.
     * Only selected primitive positions are cached. Structural and set() updates
     * automatically invalidate that index; mutations to referenced objects
     * require explicit view.invalidate().
     */
    public M3IndexedLiveView<E> filteredView(Predicate<? super E> predicate) {
        return M3IndexedLiveView.filtered(elements, predicate);
    }

    /** Stable natural/comparator-sorted view; no sorting of the payload owner. */
    public M3IndexedLiveView<E> sortedView(Comparator<? super E> comparator) {
        return M3IndexedLiveView.sorted(elements, comparator);
    }

    /** One reusable filter-mask + stable primitive-index sort over the same owner. */
    public M3IndexedLiveView<E> filteredSortedView(
            Predicate<? super E> predicate, Comparator<? super E> comparator) {
        return M3IndexedLiveView.filteredSorted(elements, predicate, comparator);
    }

    /**
     * An explicit JNI option compacts only the primitive selection mask;
     * predicates, comparators and M3ObservableList callbacks always run in Java.
     * NATIVE_REQUIRED fails if the matching native binary is unavailable.
     */
    public M3IndexedLiveView<E> filteredSortedView(
            Predicate<? super E> predicate, Comparator<? super E> comparator,
            M3IndexMaskCompactor.Backend backend) {
        return M3IndexedLiveView.filteredSorted(elements, predicate, comparator, backend);
    }

    /** Duplicates are permitted and each registration receives one callback. */
    public void addListener(Listener<E> listener) {
        Objects.requireNonNull(listener, "listener");
        Object[] next = Arrays.copyOf(listeners, listeners.length + 1);
        next[listeners.length] = listener;
        listeners = next;
    }

    /** Removes the first identical listener registration, not equals()-matching handlers. */
    public boolean removeListener(Listener<E> listener) {
        Objects.requireNonNull(listener, "listener");
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) {
                Object[] next = new Object[listeners.length - 1];
                System.arraycopy(listeners, 0, next, 0, i);
                System.arraycopy(listeners, i + 1, next, i, next.length - i);
                listeners = next.length == 0 ? NO_LISTENERS : next;
                return true;
            }
        }
        return false;
    }

    @Override public int size() { return elements.size(); }
    @Override public E get(int index) { return elements.get(index); }

    @Override public E set(int index, E value) {
        preflight();
        E previous = elements.set(index, value);
        changed(Change.SET, index, 1, previous, value, false);
        return previous;
    }

    @Override public void add(int index, E value) {
        preflight();
        elements.add(index, value);
        changed(Change.ADD, index, 1, null, value, true);
    }

    @Override public boolean addAll(Collection<? extends E> values) {
        return addAll(size(), values);
    }

    @Override public boolean addAll(int index, Collection<? extends E> values) {
        preflight();
        int before = elements.size();
        if (!elements.addAll(index, Objects.requireNonNull(values, "values"))) return false;
        changed(Change.ADD, index, elements.size() - before, null, null, true);
        return true;
    }

    @Override public E remove(int index) {
        preflight();
        E previous = elements.remove(index);
        changed(Change.REMOVE, index, 1, previous, null, true);
        return previous;
    }

    @Override protected void removeRange(int from, int to) {
        preflight();
        Objects.checkFromToIndex(from, to, elements.size());
        if (from == to) return;
        elements.subList(from, to).clear();
        changed(Change.REMOVE, from, to - from, null, null, true);
    }

    @Override public void clear() {
        preflight();
        int oldSize = elements.size();
        if (oldSize == 0) return;
        elements.clear();
        changed(Change.CLEAR, 0, oldSize, null, null, true);
    }

    private void preflight() {
        if (dispatching) throw new IllegalStateException("reentrant observable list mutation");
        if (generation == Long.MAX_VALUE) throw new IllegalStateException("generation exhausted");
    }

    @SuppressWarnings("unchecked")
    private void changed(Change change, int index, int count, E before, E after,
                         boolean structural) {
        generation++;
        if (structural) modCount++;
        Object[] current = listeners;
        if (current.length == 0) return;
        dispatching = true;
        try {
            for (Object callback : current) {
                ((Listener<E>) callback).changed(this, change, index, count,
                        before, after, generation);
            }
        } finally {
            dispatching = false;
        }
    }
}
