// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: late-binding list spliterator.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;

/**
 * List and deque over reusable array slots, with integer links instead of nodes.
 * Empty owners share zero-length arrays until the first insertion.
 */
public final class M3LinkedList<E> extends AbstractSequentialList<E> implements Deque<E> {
    private final Storage storage;
    private final boolean backwards;
    private M3LinkedList<E> reverse;
    /** Empty list; null elements are supported. */
    public M3LinkedList() { this(new Storage(), false); }
    private M3LinkedList(Storage storage, boolean backwards) { this.storage = storage; this.backwards = backwards; }
    private int first() { return backwards ? storage.last : storage.first; }
    private int last() { return backwards ? storage.first : storage.last; }
    private int next(int s) { return backwards ? storage.before[s] : storage.after[s]; }
    private int previous(int s) { return backwards ? storage.after[s] : storage.before[s]; }
    private E value(int s) { return s < 0 ? null : M3SlotArrays.get(storage.values, s); }
    private void changed() { modCount++; if (reverse != null) { reverse.modCount++; } }
    private void insert(int before, int after, E value) {
        storage.insert(backwards ? after : before, backwards ? before : after, value); changed();
    }
    private E erase(int s) { E v = value(s); storage.erase(s); changed(); return v; }
    @Override public int size() { return storage.size; }
    @Override public void addFirst(E value) { insert(-1, first(), value); }
    @Override public void addLast(E value) { insert(last(), -1, value); }
    @Override public boolean offerFirst(E value) { addFirst(value); return true; }
    @Override public boolean offerLast(E value) { addLast(value); return true; }
    @Override public E pollFirst() { return first() < 0 ? null : erase(first()); }
    @Override public E pollLast() { return last() < 0 ? null : erase(last()); }
    @Override public E peekFirst() { return value(first()); }
    @Override public E peekLast() { return value(last()); }
    @Override public E removeFirst() { if (isEmpty()) { throw new NoSuchElementException(); } return erase(first()); }
    @Override public E removeLast() { if (isEmpty()) { throw new NoSuchElementException(); } return erase(last()); }
    @Override public E getFirst() { if (isEmpty()) { throw new NoSuchElementException(); } return value(first()); }
    @Override public E getLast() { if (isEmpty()) { throw new NoSuchElementException(); } return value(last()); }
    @Override public boolean add(E value) { addLast(value); return true; }
    @Override public boolean offer(E value) { return offerLast(value); }
    @Override public E poll() { return pollFirst(); }
    @Override public E remove() { return removeFirst(); }
    @Override public E peek() { return peekFirst(); }
    @Override public E element() { return getFirst(); }
    @Override public void push(E value) { addFirst(value); }
    @Override public E pop() { return removeFirst(); }
    @Override public boolean remove(Object value) { return removeFirstOccurrence(value); }
    @Override public boolean removeFirstOccurrence(Object value) { return removeOccurrence(value, false); }
    @Override public boolean removeLastOccurrence(Object value) { return removeOccurrence(value, true); }
    private boolean removeOccurrence(Object v, boolean back) {
        for (int s = back ? last() : first(); s >= 0; s = back ? previous(s) : next(s)) {
            if (Objects.equals(v, value(s))) { erase(s); return true; }
        }
        return false;
    }
    @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.ordered(this, false); }
    @Override public Iterator<E> descendingIterator() { return reversed().iterator(); }
    @Override public M3LinkedList<E> reversed() {
        if (reverse == null) {
            reverse = new M3LinkedList<>(storage, !backwards); reverse.reverse = this; reverse.modCount = modCount;
        }
        return reverse;
    }
    @Override public void clear() {
        if (isEmpty()) { return; }
        Arrays.fill(storage.values, null); storage.first = -1; storage.last = -1; storage.free = -1;
        storage.size = 0; storage.used = 0; changed();
    }
    private int slot(int index) {
        if (index < 0 || index > size()) { throw new IndexOutOfBoundsException(index); }
        if (index == size()) { return -1; }
        int s;
        if (index < size() / 2) { s = first(); for (int i = 0; i < index; i++) { s = next(s); } }
        else { s = last(); for (int i = size() - 1; i > index; i--) { s = previous(s); } }
        return s;
    }
    @Override public ListIterator<E> listIterator(int index) {
        int start = slot(index);
        return new ListIterator<>() {
            private int cursor = start, position = index, returned = -1, expected = modCount;
            private void check() { if (expected != modCount) { throw new ConcurrentModificationException(); } }
            @Override public boolean hasNext() { return position < size(); }
            @Override public boolean hasPrevious() { return position > 0; }
            @Override public int nextIndex() { return position; }
            @Override public int previousIndex() { return position - 1; }
            @Override public E next() {
                check(); if (!hasNext()) { throw new NoSuchElementException(); }
                returned = cursor; cursor = M3LinkedList.this.next(cursor); position++; return value(returned);
            }
            @Override public E previous() {
                check(); if (!hasPrevious()) { throw new NoSuchElementException(); }
                cursor = cursor < 0 ? last() : M3LinkedList.this.previous(cursor);
                returned = cursor; position--; return value(returned);
            }
            @Override public void remove() {
                check(); if (returned < 0) { throw new IllegalStateException(); }
                if (cursor == returned) { cursor = M3LinkedList.this.next(returned); } else { position--; }
                erase(returned); returned = -1; expected = modCount;
            }
            @Override public void set(E v) {
                check(); if (returned < 0) { throw new IllegalStateException(); } storage.values[returned] = v;
            }
            @Override public void add(E v) {
                check(); insert(cursor < 0 ? last() : M3LinkedList.this.previous(cursor), cursor, v);
                position++; returned = -1; expected = modCount;
            }
        };
    }
    /** Validate live and free links and released references. */
    public void verifyInvariants() { storage.verify(); }
    private static final class Storage {
        private Object[] values = M3SlotArrays.EMPTY_OBJECTS;
        private int[] before = M3SlotArrays.EMPTY_INTS, after = M3SlotArrays.EMPTY_INTS;
        private int first = -1, last = -1, free = -1, size, used;
        void insert(int b, int a, Object value) {
            if (free < 0 && used == values.length) {
                int n = M3SlotArrays.grow(values.length);
                Object[] v = Arrays.copyOf(values, n); int[] p = Arrays.copyOf(before, n), q = Arrays.copyOf(after, n);
                values = v; before = p; after = q;
            }
            int s;
            if (free < 0) { s = used++; } else { s = free; free = after[s]; }
            values[s] = value; before[s] = b; after[s] = a;
            if (b < 0) { first = s; } else { after[b] = s; }
            if (a < 0) { last = s; } else { before[a] = s; } size++;
        }
        void erase(int s) {
            int b = before[s], a = after[s];
            if (b < 0) { first = a; } else { after[b] = a; }
            if (a < 0) { last = b; } else { before[a] = b; }
            values[s] = null; before[s] = -1; after[s] = free; free = s; size--;
        }
        void verify() {
            boolean[] seen = new boolean[used]; int n = 0, p = -1;
            for (int s = first; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || before[s] != p) { throw new AssertionError("list links"); }
                seen[s] = true; p = s; n++;
            }
            if (n != size || p != last) { throw new AssertionError("list size"); }
            for (int s = free; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || values[s] != null) { throw new AssertionError("list free"); }
                seen[s] = true; n++;
            }
            if (n != used) { throw new AssertionError("list ownership"); }
        }
    }
}
