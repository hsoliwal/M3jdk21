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

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/** Exact-width binary min-heap. Custom comparators must obey the Comparator contract.
 * Iterators traverse a detached primitive array copy and remove from the owner.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/collections/flat/FlatPriorityQueue.java}
 * blob {@code b99e7e96df3c7889d87f0971c450adc1a2b3b8e0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class FlatPriorityQueue<E> extends AbstractQueue<E> {
    private FlatStore<E> store;
    private final FlatStore<E> scratch;
    private final Comparator<? super E> comparator;
    private int size;
    private int version;
    public FlatPriorityQueue(StorageType<E> type) { this(type, null); }
    public FlatPriorityQueue(StorageType<E> type, Comparator<? super E> comparator) {
        store = new FlatStore<>(type, 8); scratch = new FlatStore<>(type, 1); this.comparator = comparator;
    }
    public Comparator<? super E> comparator() { return comparator; }
    @Override public int size() { return size; }
    @SuppressWarnings("unchecked")
    private int compare(int a, int b) {
        if (comparator != null) return comparator.compare(store.get(a), store.get(b));
        if (!store.type.isPrimitive()) return ((Comparable<? super E>) store.get(a)).compareTo(store.get(b));
        long left = store.bits(a), right = store.bits(b);
        return switch (store.type.kind) {
            case BYTE -> Byte.compare((byte) left, (byte) right);
            case SHORT -> Short.compare((short) left, (short) right);
            case CHAR -> Character.compare((char) left, (char) right);
            case INT -> Integer.compare((int) left, (int) right);
            case LONG -> Long.compare(left, right);
            case FLOAT -> Float.compare(Float.intBitsToFloat((int) left), Float.intBitsToFloat((int) right));
            case DOUBLE -> Double.compare(Double.longBitsToDouble(left), Double.longBitsToDouble(right));
            case BOOLEAN -> Long.compare(left, right);
            default -> throw new IllegalStateException();
        };
    }
    private void swap(int a, int b) {
        store.copyTo(a, scratch, 0); store.copyTo(b, store, a); scratch.copyTo(0, store, b); scratch.clear(0);
    }
    private int up(int i) {
        while (i > 0) {
            int parent = (i - 1) >>> 1;
            if (compare(i, parent) >= 0) break;
            swap(i, parent); i = parent;
        }
        return i;
    }
    private void down(int i) {
        while (i < size / 2) {
            int child = (i << 1) + 1;
            if (child + 1 < size && compare(child + 1, child) < 0) child++;
            if (compare(i, child) <= 0) return;
            swap(i, child); i = child;
        }
    }
    private void ensure() {
        if (size == store.capacity) store = store.resized(FlatStore.grow(store.capacity, size + 1), size);
    }
    @Override public boolean offer(E value) {
        Objects.requireNonNull(value); store.type.validate(value);
        if (comparator == null && !store.type.isPrimitive() && !(value instanceof Comparable<?>)) {
            throw new ClassCastException("Natural ordering requires Comparable");
        }
        ensure(); store.set(size, value); size++; version++; up(size - 1); return true;
    }
    public void addBits(long bits) {
        store.type.checkBits(bits); ensure(); store.setBits(size, bits); size++; version++; up(size - 1);
    }
    @Override public E peek() { return size == 0 ? null : store.get(0); }
    @Override public E poll() { if (size == 0) return null; E result = store.get(0); removeAt(0); return result; }
    public long removeBits() {
        if (size == 0) throw new NoSuchElementException();
        long result = store.bits(0); removeAt(0); return result;
    }
    private void removeAt(int index) {
        --size;
        if (index < size) store.copyTo(size, store, index);
        store.clear(size); version++;
        if (index < size && up(index) == index) down(index);
    }
    @Override public boolean contains(Object value) {
        for (int i = 0; i < size; i++) if (store.equalsAt(i, value)) return true;
        return false;
    }
    @Override public boolean remove(Object value) {
        for (int i = 0; i < size; i++) if (store.equalsAt(i, value)) { removeAt(i); return true; }
        return false;
    }
    @Override public void clear() {
        if (size == 0) return;
        for (int i = 0; i < size; i++) store.clear(i);
        size = 0; version++;
    }
    @Override public Iterator<E> iterator() {
        FlatStore<E> snapshot = store.resized(size, size);
        int count = size;
        return new Iterator<>() {
            private int cursor;
            private int returned = -1;
            private int expected = version;
            private void check() { if (expected != version) throw new ConcurrentModificationException(); }
            @Override public boolean hasNext() { check(); return cursor < count; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException(); returned = cursor++; return snapshot.get(returned);
            }
            @Override public void remove() {
                check(); if (returned < 0) throw new IllegalStateException();
                for (int i = 0; i < size; i++) {
                    boolean same = store.type.isPrimitive()
                            ? store.bitsEqual(i, snapshot.bits(returned)) : store.get(i) == snapshot.get(returned);
                    if (same) { removeAt(i); break; }
                }
                returned = -1; expected = version;
            }
        };
    }
}
