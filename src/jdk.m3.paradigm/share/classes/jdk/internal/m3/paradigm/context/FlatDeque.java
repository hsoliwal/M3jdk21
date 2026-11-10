/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.AbstractCollection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Exact-width array ring for FIFO, LIFO and deque operations. Null elements are forbidden.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatDeque.java}
 * blob {@code 8584796d1e972ab5de7d1da2576afd867980d22b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FlatDeque<E> extends AbstractCollection<E> implements Deque<E> {
    private FlatStore<E> store;
    private int head;
    private int size;
    private int version;
    public FlatDeque(StorageType<E> type) { this(type, 8); }
    public FlatDeque(StorageType<E> type, int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Negative capacity");
        store = new FlatStore<>(type, Math.max(1, capacity));
    }
    private int slot(int logical) { return (int) (((long) head + logical) % store.capacity); }
    private void grow() {
        if (size < store.capacity) return;
        FlatStore<E> next = new FlatStore<>(store.type, FlatStore.grow(store.capacity, size + 1));
        for (int i = 0; i < size; i++) store.copyTo(slot(i), next, i);
        store = next; head = 0;
    }
    @Override public int size() { return size; }
    @Override public boolean offerFirst(E value) {
        Objects.requireNonNull(value); store.type.validate(value); grow();
        head = head == 0 ? store.capacity - 1 : head - 1;
        store.set(head, value); size++; version++; return true;
    }
    @Override public boolean offerLast(E value) {
        Objects.requireNonNull(value); store.type.validate(value); grow();
        store.set(slot(size), value); size++; version++; return true;
    }
    public void addFirstBits(long bits) {
        store.type.checkBits(bits); grow(); head = head == 0 ? store.capacity - 1 : head - 1;
        store.setBits(head, bits); size++; version++;
    }
    public void addLastBits(long bits) {
        store.type.checkBits(bits); grow(); store.setBits(slot(size), bits); size++; version++;
    }
    public long removeFirstBits() {
        if (size == 0) throw new NoSuchElementException();
        long result = store.bits(head); removeAt(0); return result;
    }
    public long removeLastBits() {
        if (size == 0) throw new NoSuchElementException();
        long result = store.bits(slot(size - 1)); removeAt(size - 1); return result;
    }
    private void removeAt(int index) {
        if (index == 0) {
            store.clear(head); head = slot(1);
        } else {
            for (int i = index; i + 1 < size; i++) store.copyTo(slot(i + 1), store, slot(i));
            store.clear(slot(size - 1));
        }
        size--; if (size == 0) head = 0; version++;
    }
    @Override public E peekFirst() { return size == 0 ? null : store.get(head); }
    @Override public E peekLast() { return size == 0 ? null : store.get(slot(size - 1)); }
    @Override public E pollFirst() { E value = peekFirst(); if (size > 0) removeAt(0); return value; }
    @Override public E pollLast() { E value = peekLast(); if (size > 0) removeAt(size - 1); return value; }
    @Override public E getFirst() { E value = peekFirst(); if (value == null) throw new NoSuchElementException(); return value; }
    @Override public E getLast() { E value = peekLast(); if (value == null) throw new NoSuchElementException(); return value; }
    @Override public E removeFirst() { E value = pollFirst(); if (value == null) throw new NoSuchElementException(); return value; }
    @Override public E removeLast() { E value = pollLast(); if (value == null) throw new NoSuchElementException(); return value; }
    @Override public void addFirst(E value) { offerFirst(value); }
    @Override public void addLast(E value) { offerLast(value); }
    @Override public boolean offer(E value) { return offerLast(value); }
    @Override public boolean add(E value) { return offerLast(value); }
    @Override public E poll() { return pollFirst(); }
    @Override public E remove() { return removeFirst(); }
    @Override public E peek() { return peekFirst(); }
    @Override public E element() { return getFirst(); }
    @Override public void push(E value) { addFirst(value); }
    @Override public E pop() { return removeFirst(); }
    @Override public boolean remove(Object value) { return removeFirstOccurrence(value); }
    @Override public boolean contains(Object value) {
        for (int i = 0; i < size; i++) if (store.equalsAt(slot(i), value)) return true;
        return false;
    }
    @Override public boolean removeFirstOccurrence(Object value) {
        for (int i = 0; i < size; i++) if (store.equalsAt(slot(i), value)) { removeAt(i); return true; }
        return false;
    }
    @Override public boolean removeLastOccurrence(Object value) {
        for (int i = size - 1; i >= 0; i--) if (store.equalsAt(slot(i), value)) { removeAt(i); return true; }
        return false;
    }
    @Override public void clear() {
        if (size == 0) return;
        for (int i = 0; i < size; i++) store.clear(slot(i));
        size = head = 0; version++;
    }
    private Iterator<E> iterator(boolean descending) {
        return new Iterator<>() {
            private int cursor = descending ? size - 1 : 0;
            private int returned = -1;
            private int expected = version;
            private void check() { if (expected != version) throw new ConcurrentModificationException(); }
            @Override public boolean hasNext() { check(); return descending ? cursor >= 0 : cursor < size; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                returned = cursor; cursor += descending ? -1 : 1; return store.get(slot(returned));
            }
            @Override public void remove() {
                check(); if (returned < 0) throw new IllegalStateException();
                removeAt(returned); if (!descending) cursor--;
                returned = -1; expected = version;
            }
        };
    }
    @Override public Iterator<E> iterator() { return iterator(false); }
    @Override public Iterator<E> descendingIterator() { return iterator(true); }
}
