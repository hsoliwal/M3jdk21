// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: DOOP circular offset array deque, no linked nodes.
package com.m3.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Double-ended queue using a single dynamic circular Object[] and integer
 * head/size offsets. There are no per-element nodes, links or wrappers.
 * Empty instances share a zero-length payload array and allocate on first add.
 * This is not synchronized; stored nulls are prohibited by Deque.
 */
public final class M3ArrayDeque<E> extends AbstractCollection<E>
        implements Deque<E>, Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private static final int MAX_CAPACITY = 1 << 30;
    private Object[] elements = M3SlotArrays.EMPTY_OBJECTS;
    private int head, size, revision;

    public M3ArrayDeque() { }

    public M3ArrayDeque(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("negative capacity");
        // A capacity hint is retained lazily at insertion time.
        hint = initialCapacity;
    }

    public M3ArrayDeque(Collection<? extends E> source) {
        this(Objects.requireNonNull(source, "source").size());
        addAll(source);
    }

    private int hint;
    public int capacity() { return elements.length; }
    @Override public int size() { return size; }

    private int physical(int logical) {
        return (head + logical) & (elements.length - 1);
    }

    @SuppressWarnings("unchecked")
    private E at(int logical) { return (E) elements[physical(logical)]; }

    private void ensureCapacity(int required) {
        if (required <= elements.length) return;
        if (required > MAX_CAPACITY) throw new OutOfMemoryError("deque capacity exhausted");
        int n = 8;
        int target = Math.max(required, hint);
        if (target > MAX_CAPACITY) throw new OutOfMemoryError("deque capacity exhausted");
        while (n < target) n <<= 1;
        Object[] replacement = new Object[n];
        for (int i = 0; i < size; i++) replacement[i] = at(i);
        elements = replacement;
        head = 0;
    }

    @Override public void addFirst(E value) {
        Objects.requireNonNull(value, "deque elements");
        ensureCapacity(size + 1);
        head = (head - 1) & (elements.length - 1);
        elements[head] = value;
        size++;
        revision++;
    }

    @Override public void addLast(E value) {
        Objects.requireNonNull(value, "deque elements");
        ensureCapacity(size + 1);
        elements[physical(size++)] = value;
        revision++;
    }

    @Override public boolean offerFirst(E value) { addFirst(value); return true; }
    @Override public boolean offerLast(E value) { addLast(value); return true; }
    @Override public boolean add(E value) { addLast(value); return true; }
    @Override public boolean offer(E value) { return offerLast(value); }
    @Override public void push(E value) { addFirst(value); }

    @Override public E pollFirst() {
        if (size == 0) return null;
        E result = at(0);
        elements[head] = null;
        head = (head + 1) & (elements.length - 1);
        size--;
        if (size == 0) head = 0;
        revision++;
        return result;
    }

    @Override public E pollLast() {
        if (size == 0) return null;
        int s = physical(--size);
        @SuppressWarnings("unchecked") E result = (E) elements[s];
        elements[s] = null;
        if (size == 0) head = 0;
        revision++;
        return result;
    }

    @Override public E peekFirst() { return size == 0 ? null : at(0); }
    @Override public E peekLast() { return size == 0 ? null : at(size - 1); }
    @Override public E getFirst() {
        E value = peekFirst();
        if (value == null) throw new NoSuchElementException();
        return value;
    }
    @Override public E getLast() {
        E value = peekLast();
        if (value == null) throw new NoSuchElementException();
        return value;
    }
    @Override public E removeFirst() {
        E value = pollFirst();
        if (value == null) throw new NoSuchElementException();
        return value;
    }
    @Override public E removeLast() {
        E value = pollLast();
        if (value == null) throw new NoSuchElementException();
        return value;
    }
    @Override public E remove() { return removeFirst(); }
    @Override public E poll() { return pollFirst(); }
    @Override public E element() { return getFirst(); }
    @Override public E peek() { return peekFirst(); }
    @Override public E pop() { return removeFirst(); }

    private void eraseAt(int logical) {
        if (logical == 0) { pollFirst(); return; }
        if (logical == size - 1) { pollLast(); return; }
        for (int i = logical + 1; i < size; i++) {
            elements[physical(i - 1)] = elements[physical(i)];
        }
        elements[physical(--size)] = null;
        revision++;
    }

    @Override public boolean removeFirstOccurrence(Object item) {
        if (item == null) return false;
        for (int i = 0; i < size; i++) {
            if (item.equals(at(i))) { eraseAt(i); return true; }
        }
        return false;
    }
    @Override public boolean removeLastOccurrence(Object item) {
        if (item == null) return false;
        for (int i = size - 1; i >= 0; i--) {
            if (item.equals(at(i))) { eraseAt(i); return true; }
        }
        return false;
    }
    @Override public boolean remove(Object item) { return removeFirstOccurrence(item); }
    @Override public boolean contains(Object item) {
        if (item == null) return false;
        for (int i = 0; i < size; i++) if (item.equals(at(i))) return true;
        return false;
    }
    @Override public boolean addAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source == this) throw new IllegalArgumentException("cannot add deque to itself");
        boolean changed = false;
        for (E value : source) { addLast(value); changed = true; }
        return changed;
    }
    @Override public void clear() {
        for (int i = 0; i < size; i++) elements[physical(i)] = null;
        head = size = 0;
        revision++;
    }

    private final class Cursor implements Iterator<E> {
        private final boolean reverse;
        private int cursor, last = -1, expected = revision;
        Cursor(boolean reverse) {
            this.reverse = reverse;
            cursor = reverse ? size - 1 : 0;
        }
        private void check() {
            if (revision != expected) throw new ConcurrentModificationException();
        }
        @Override public boolean hasNext() {
            check();
            return reverse ? cursor >= 0 : cursor < size;
        }
        @Override public E next() {
            if (!hasNext()) throw new NoSuchElementException();
            int s = cursor;
            cursor += reverse ? -1 : 1;
            last = s;
            return at(s);
        }
        @Override public void remove() {
            check();
            if (last < 0) throw new IllegalStateException();
            eraseAt(last);
            if (!reverse) cursor = last;
            last = -1;
            expected = revision;
        }
    }
    @Override public Iterator<E> iterator() { return new Cursor(false); }
    @Override public Iterator<E> descendingIterator() { return new Cursor(true); }

    @Override public Object[] toArray() {
        Object[] snapshot = new Object[size];
        for (int i = 0; i < size; i++) snapshot[i] = at(i);
        return snapshot;
    }

    @Override public <T> T[] toArray(T[] destination) {
        Objects.requireNonNull(destination, "destination");
        T[] result = destination.length >= size ? destination
                : Arrays.copyOf(destination, size);
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked") T value = (T) at(i);
            result[i] = value;
        }
        if (result.length > size) result[size] = null;
        return result;
    }

    @Override public M3ArrayDeque<E> clone() {
        M3ArrayDeque<E> copy = new M3ArrayDeque<>(size);
        for (int i = 0; i < size; i++) copy.addLast(at(i));
        return copy;
    }
}
