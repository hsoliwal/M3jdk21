// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Live JDK Deque and Queue filtered views backed by an existing Deque owner.
 *
 * <p>Each view retains only its backing Deque and Java predicate. No element,
 * entry, node, offset array or predicate outcome is retained per source element.
 * Mutation is directed at the exact physical occurrence visited by an underlying
 * deque iterator, preserving hidden elements and duplicate object identities.
 * Position-based iterator removal deliberately rebinds its source cursor after
 * editing, avoiding removal of a prefetched (rather than delivered) element.
 * It may cost O(n) for one remove; scanning and size are O(n).
 *
 * <p>Predicates must be stable and non-reentrant. The backing source must support
 * iterator removal for writable removals; concurrent/external structural changes
 * during a traversal have the backing source's semantics and are unsupported.
 * A null selected element may be returned by poll/peek for null-permitting Deques;
 * this is consistent with their JDK null-sentinel ambiguity. Arbitrary Queue
 * implementations are deliberately excluded: their iterator order may differ
 * from removal order (e.g., PriorityQueue). No JNI invokes Java callbacks.
 */
public final class M3FilteredDeques {
    private M3FilteredDeques() { }

    /** Writable, live encounter-order filtered Deque, also implementing Queue. */
    public static <E> Deque<E> filtered(Deque<E> source, Predicate<? super E> predicate) {
        return new View<>(source, predicate);
    }

    /** Live read-only Deque, refusing every attempted mutation including empty no-ops. */
    public static <E> Deque<E> readOnlyFiltered(
            Deque<E> source, Predicate<? super E> predicate) {
        return new ReadOnly<>(filtered(source, predicate));
    }

    private static final class View<E> extends AbstractCollection<E> implements Deque<E> {
        private final Deque<E> backing;
        private final Predicate<? super E> predicate;

        private View(Deque<E> backing, Predicate<? super E> predicate) {
            this.backing = Objects.requireNonNull(backing, "source");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        private void admit(E value) {
            if (!predicate.test(value)) {
                throw new IllegalArgumentException("element rejected by filtered Deque");
            }
        }

        private Iterator<E> sourceCursor(boolean descending) {
            return descending ? backing.descendingIterator() : backing.iterator();
        }

        @Override public Iterator<E> iterator() { return new Cursor(false); }
        @Override public Iterator<E> descendingIterator() { return new Cursor(true); }

        /** One cursor, one value and physical ordinal; no retained element index. */
        private final class Cursor implements Iterator<E> {
            private final boolean descending;
            private Iterator<E> source;
            private int consumed;
            private int readyOrdinal;
            private int delivered = -1;
            private E readyValue;
            private boolean ready;
            private boolean ended;
            private boolean poisoned;

            private Cursor(boolean descending) {
                this.descending = descending;
                source = sourceCursor(descending);
            }

            @Override public boolean hasNext() {
                if (poisoned) throw new IllegalStateException("failed filtered Deque cursor");
                if (ready) return true;
                if (ended) return false;
                try {
                    while (source.hasNext()) {
                        E value = source.next();
                        int ordinal = consumed++;
                        if (predicate.test(value)) {
                            ready = true;
                            readyValue = value;
                            readyOrdinal = ordinal;
                            return true;
                        }
                    }
                    ended = true;
                    return false;
                } catch (RuntimeException | Error failure) {
                    poisoned = true;
                    throw failure;
                }
            }

            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                ready = false;
                delivered = readyOrdinal;
                E value = readyValue;
                readyValue = null;
                return value;
            }

            @Override public void remove() {
                if (poisoned) throw new IllegalStateException("failed filtered Deque cursor");
                if (delivered < 0) throw new IllegalStateException("next required before remove");
                // Hot path: no lookahead advanced the backing cursor since the
                // last delivered element. Preserve its native O(1) iterator removal.
                if (delivered == consumed - 1) {
                    source.remove();
                    consumed--;
                    delivered = -1;
                    return;
                }
                // A prefetched element cannot be removed through the current
                // iterator: its remove() would target the WRONG physical row.
                Iterator<E> mutation = sourceCursor(descending);
                for (int index = 0; index <= delivered; index++) mutation.next();
                mutation.remove();
                // Rebind at the next unread physical row after the removed one;
                // retain a prepared value, if any, without re-running its predicate.
                consumed--;
                if (ready) readyOrdinal--;
                source = sourceCursor(descending);
                for (int index = 0; index < consumed; index++) source.next();
                delivered = -1;
            }
        }

        @Override public int size() {
            int total = 0;
            for (E value : backing) if (predicate.test(value)) total++;
            return total;
        }

        @Override public boolean isEmpty() {
            for (E value : backing) if (predicate.test(value)) return false;
            return true;
        }

        @Override public boolean contains(Object item) {
            for (E value : backing) {
                if (Objects.equals(item, value) && predicate.test(value)) return true;
            }
            return false;
        }

        @Override public boolean remove(Object item) {
            Iterator<E> cursor = backing.iterator();
            while (cursor.hasNext()) {
                E value = cursor.next();
                if (Objects.equals(value, item) && predicate.test(value)) {
                    cursor.remove();
                    return true;
                }
            }
            return false;
        }

        @Override public boolean removeFirstOccurrence(Object item) {
            return remove(item);
        }

        @Override public boolean removeLastOccurrence(Object item) {
            Iterator<E> cursor = backing.descendingIterator();
            while (cursor.hasNext()) {
                E value = cursor.next();
                if (Objects.equals(value, item) && predicate.test(value)) {
                    cursor.remove();
                    return true;
                }
            }
            return false;
        }

        private E boundary(boolean descending, boolean remove, boolean strict) {
            Iterator<E> cursor = sourceCursor(descending);
            while (cursor.hasNext()) {
                E value = cursor.next();
                if (predicate.test(value)) {
                    if (remove) cursor.remove();
                    return value;
                }
            }
            if (strict) throw new NoSuchElementException();
            return null;
        }

        @Override public E getFirst() { return boundary(false, false, true); }
        @Override public E getLast() { return boundary(true, false, true); }
        @Override public E peekFirst() { return boundary(false, false, false); }
        @Override public E peekLast() { return boundary(true, false, false); }
        @Override public E removeFirst() { return boundary(false, true, true); }
        @Override public E removeLast() { return boundary(true, true, true); }
        @Override public E pollFirst() { return boundary(false, true, false); }
        @Override public E pollLast() { return boundary(true, true, false); }

        @Override public void addFirst(E value) { admit(value); backing.addFirst(value); }
        @Override public void addLast(E value) { admit(value); backing.addLast(value); }
        @Override public boolean offerFirst(E value) {
            admit(value);
            return backing.offerFirst(value);
        }
        @Override public boolean offerLast(E value) {
            admit(value);
            return backing.offerLast(value);
        }
        @Override public boolean add(E value) { addLast(value); return true; }
        @Override public boolean offer(E value) { return offerLast(value); }
        @Override public E remove() { return removeFirst(); }
        @Override public E poll() { return pollFirst(); }
        @Override public E element() { return getFirst(); }
        @Override public E peek() { return peekFirst(); }
        @Override public void push(E value) { addFirst(value); }
        @Override public E pop() { return removeFirst(); }

        @Override public boolean addAll(Collection<? extends E> values) {
            Objects.requireNonNull(values, "values");
            // User-owned mutable/aliased sources must not change while iterated.
            // One temporary object-reference array is confined to the explicit
            // bulk API boundary, never retained in the view or collection store.
            Object[] snapshot = values.toArray();
            for (Object element : snapshot) {
                @SuppressWarnings("unchecked") E value = (E) element;
                admit(value);
            }
            for (Object element : snapshot) {
                @SuppressWarnings("unchecked") E value = (E) element;
                backing.addLast(value);
            }
            return snapshot.length != 0;
        }

        @Override public boolean removeIf(Predicate<? super E> condition) {
            Objects.requireNonNull(condition, "condition");
            boolean changed = false;
            Iterator<E> cursor = backing.iterator();
            while (cursor.hasNext()) {
                E value = cursor.next();
                if (predicate.test(value) && condition.test(value)) {
                    cursor.remove();
                    changed = true;
                }
            }
            return changed;
        }

        @Override public boolean removeAll(Collection<?> values) {
            Objects.requireNonNull(values, "values");
            return removeIf(values::contains);
        }

        @Override public boolean retainAll(Collection<?> values) {
            Objects.requireNonNull(values, "values");
            return removeIf(value -> !values.contains(value));
        }

        @Override public void clear() { removeIf(value -> true); }

        @Override public Deque<E> reversed() { return filtered(backing.reversed(), predicate); }
    }

    private static final class ReadOnly<E> extends AbstractCollection<E> implements Deque<E> {
        private final Deque<E> selected;
        private ReadOnly(Deque<E> selected) { this.selected = selected; }

        private Iterator<E> noRemove(Iterator<E> delegate) {
            return new Iterator<>() {
                @Override public boolean hasNext() { return delegate.hasNext(); }
                @Override public E next() { return delegate.next(); }
                @Override public void remove() { throw readonly(); }
            };
        }
        private UnsupportedOperationException readonly() {
            return new UnsupportedOperationException("read-only filtered Deque");
        }
        @Override public int size() { return selected.size(); }
        @Override public boolean isEmpty() { return selected.isEmpty(); }
        @Override public boolean contains(Object item) { return selected.contains(item); }
        @Override public Iterator<E> iterator() { return noRemove(selected.iterator()); }
        @Override public Iterator<E> descendingIterator() { return noRemove(selected.descendingIterator()); }
        @Override public E getFirst() { return selected.getFirst(); }
        @Override public E getLast() { return selected.getLast(); }
        @Override public E peekFirst() { return selected.peekFirst(); }
        @Override public E peekLast() { return selected.peekLast(); }
        @Override public E peek() { return selected.peek(); }
        @Override public E element() { return selected.element(); }
        @Override public void addFirst(E e) { throw readonly(); }
        @Override public void addLast(E e) { throw readonly(); }
        @Override public boolean offerFirst(E e) { throw readonly(); }
        @Override public boolean offerLast(E e) { throw readonly(); }
        @Override public boolean add(E e) { throw readonly(); }
        @Override public boolean offer(E e) { throw readonly(); }
        @Override public boolean addAll(Collection<? extends E> e) { throw readonly(); }
        @Override public E removeFirst() { throw readonly(); }
        @Override public E removeLast() { throw readonly(); }
        @Override public E pollFirst() { throw readonly(); }
        @Override public E pollLast() { throw readonly(); }
        @Override public E remove() { throw readonly(); }
        @Override public E poll() { throw readonly(); }
        @Override public boolean remove(Object e) { throw readonly(); }
        @Override public boolean removeFirstOccurrence(Object e) { throw readonly(); }
        @Override public boolean removeLastOccurrence(Object e) { throw readonly(); }
        @Override public boolean removeAll(Collection<?> e) { throw readonly(); }
        @Override public boolean retainAll(Collection<?> e) { throw readonly(); }
        @Override public boolean removeIf(Predicate<? super E> e) { throw readonly(); }
        @Override public void clear() { throw readonly(); }
        @Override public void push(E e) { throw readonly(); }
        @Override public E pop() { throw readonly(); }
        @Override public Deque<E> reversed() { return new ReadOnly<>(selected.reversed()); }
    }
}
