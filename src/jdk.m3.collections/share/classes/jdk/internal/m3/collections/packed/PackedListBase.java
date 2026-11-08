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
package jdk.internal.m3.collections.packed;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * List adaptation shared by the sorted and positional packed sequences.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/ds/packedlists/PackedListBase.java}
 * blob {@code 7ca9991653cbb537251c67b66d652783a0d928d1} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.ds batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class PackedListBase<E> extends AbstractList<E> implements Serializable {
    private static final long serialVersionUID = 1L;
    final PackedSequenceStore<E> store = new PackedSequenceStore<>();
    int root;

    @Override public int size() { return store.count(root); }
    @Override public E get(int index) { return store.value(store.at(root, index)); }

    final void insert(int index, E value) {
        root = store.insert(root, index, value);
        modCount++;
    }

    @Override public E remove(int index) {
        int slot = store.at(root, index);
        E value = store.value(slot);
        root = store.removeSlot(root, slot);
        modCount++;
        return value;
    }

    @Override public void clear() {
        store.clearArena();
        root = 0;
        modCount++;
    }

    @Override public int indexOf(Object value) {
        int index = 0;
        for (int s = store.first(root); s != 0; s = store.successor(s), index++) {
            if (Objects.equals(value, store.value(s))) return index;
        }
        return -1;
    }

    @Override public int lastIndexOf(Object value) {
        int index = size() - 1;
        for (int s = store.last(root); s != 0; s = store.predecessor(s), index--) {
            if (Objects.equals(value, store.value(s))) return index;
        }
        return -1;
    }

    @Override public boolean contains(Object value) { return indexOf(value) >= 0; }

    @Override public boolean remove(Object value) {
        int index = indexOf(value);
        if (index < 0) return false;
        remove(index);
        return true;
    }

    @Override public Iterator<E> iterator() { return listIterator(0); }
    @Override public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException("index=" + index);
        return new Cursor(index);
    }

    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        int expected = modCount;
        for (int s = store.first(root); s != 0; s = store.successor(s)) {
            action.accept(store.value(s));
            if (expected != modCount) throw new ConcurrentModificationException();
        }
    }

    @Override public Object[] toArray() {
        Object[] result = new Object[size()];
        int i = 0;
        for (int s = store.first(root); s != 0; s = store.successor(s)) result[i++] = store.value(s);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T[] toArray(T[] destination) {
        int n = size();
        T[] result = destination.length >= n ? destination
                : (T[]) Array.newInstance(destination.getClass().getComponentType(), n);
        int i = 0;
        for (int s = store.first(root); s != 0; s = store.successor(s)) result[i++] = (T) store.value(s);
        if (result.length > n) result[n] = null;
        return result;
    }

    private final class Cursor implements ListIterator<E> {
        private int next;
        private int index;
        private int lastIndex = -1;
        private int expected = modCount;

        Cursor(int index) {
            this.index = index;
            next = index == size() ? 0 : store.at(root, index);
        }
        private void check() {
            if (expected != modCount) throw new ConcurrentModificationException();
        }
        @Override public boolean hasNext() { return next != 0; }
        @Override public boolean hasPrevious() { return index > 0; }
        @Override public int nextIndex() { return index; }
        @Override public int previousIndex() { return index - 1; }
        @Override public E next() {
            check();
            if (next == 0) throw new NoSuchElementException();
            E result = store.value(next);
            next = store.successor(next);
            lastIndex = index++;
            return result;
        }
        @Override public E previous() {
            check();
            if (index == 0) throw new NoSuchElementException();
            next = next == 0 ? store.last(root) : store.predecessor(next);
            lastIndex = --index;
            return store.value(next);
        }
        @Override public void remove() {
            check();
            if (lastIndex < 0) throw new IllegalStateException();
            if (lastIndex == index) next = store.successor(next);
            PackedListBase.this.remove(lastIndex);
            if (lastIndex < index) index--;
            lastIndex = -1;
            expected = modCount;
        }
        @Override public void set(E value) {
            check();
            if (lastIndex < 0) throw new IllegalStateException();
            PackedListBase.this.set(lastIndex, value);
            expected = modCount;
        }
        @Override public void add(E value) {
            check();
            PackedListBase.this.add(index, value);
            index++;
            lastIndex = -1;
            expected = modCount;
        }
    }
}
