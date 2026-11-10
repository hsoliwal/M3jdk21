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

import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.LongConsumer;

/**
 * Random-access list retaining exact-width primitives or object references. Allows nulls.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatList.java}
 * blob {@code 8b7f9012837925bd77022cf4663ff9aa31091f08} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FlatList<E> extends AbstractList<E> implements RandomAccess {
    private FlatStore<E> store;
    private int size;
    public FlatList(StorageType<E> type) { this(type, 0); }
    public FlatList(StorageType<E> type, int capacity) { store = new FlatStore<>(type, capacity); }
    @Override public int size() { return size; }
    @Override public E get(int index) { Objects.checkIndex(index, size); return store.get(index); }
    @Override public E set(int index, E value) {
        Objects.checkIndex(index, size); store.type.validate(value);
        E old = store.get(index); store.set(index, value); return old;
    }
    private void ensure(int needed) {
        if (needed > store.capacity) store = store.resized(FlatStore.grow(store.capacity, needed), size);
    }
    @Override public void add(int index, E value) {
        Objects.checkIndex(index, size + 1); store.type.validate(value); ensure(size + 1);
        store.move(index, index + 1, size - index); store.set(index, value); size++; modCount++;
    }
    @Override public E remove(int index) {
        Objects.checkIndex(index, size); E old = store.get(index);
        store.move(index + 1, index, size - index - 1); store.clear(--size); modCount++; return old;
    }
    @Override protected void removeRange(int from, int to) {
        Objects.checkFromToIndex(from, to, size);
        if (from == to) return;
        store.move(to, from, size - to);
        int old = size; size -= to - from;
        for (int i = size; i < old; i++) store.clear(i);
        modCount++;
    }
    public void addBits(long bits) {
        store.type.checkBits(bits); ensure(size + 1); store.setBits(size++, bits); modCount++;
    }
    public long getBits(int index) { Objects.checkIndex(index, size); return store.bits(index); }
    public void setBits(int index, long bits) { Objects.checkIndex(index, size); store.setBits(index, bits); }
    public void forEachBits(LongConsumer action) {
        Objects.requireNonNull(action);
        if (!store.type.isPrimitive()) throw new UnsupportedOperationException();
        int expected = modCount;
        for (int i = 0; i < size; i++) {
            action.accept(store.bits(i));
            if (expected != modCount) throw new java.util.ConcurrentModificationException();
        }
    }
    public FlatList<E> copy() {
        FlatList<E> result = new FlatList<>(store.type);
        result.store = store.resized(size, size); result.size = size; return result;
    }
}
