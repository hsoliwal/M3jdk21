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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Positional edits move int IDs; surviving payloads remain in reusable physical slots.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/IndexedShapeSequence.java}
 * blob {@code cfcc9ad1f2fd4ccf11b625b9060d8f49a883c2cc} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3IndexedShapeSequence<E> extends M3ShapeGuard implements M3ShapeSequence<E> {
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int[] EMPTY_INTS = new int[0];
    private Object[] values = EMPTY_OBJECTS;
    private int[] order = EMPTY_INTS;
    private int[] freeNext = EMPTY_INTS;
    private int size;
    private int highWater;
    private int free = -1;
    M3IndexedShapeSequence(M3CollectionRequirements required) { super(required); }
    @Override public int size() { return size; }
    @Override public void add(E value) { insert(size, value); }
    @Override public void insert(int at, E value) {
        writable(); position(at, size); element(value); growing(size);
        int row = free < 0 ? highWater : free;
        if (row == values.length) {
            int capacity = grown(values.length, row + 1, required.maximumSize());
            Object[] newValues = Arrays.copyOf(values, capacity);
            int[] newOrder = Arrays.copyOf(order, capacity);
            int[] newFreeNext = Arrays.copyOf(freeNext, capacity);
            values = newValues; order = newOrder; freeNext = newFreeNext;
        }
        if (free < 0) { highWater++; } else { free = freeNext[row]; }
        System.arraycopy(order, at, order, at + 1, size - at);
        order[at] = row; values[row] = value; size++;
    }
    @Override public E get(int at) { index(at, size); return read(values, order[at]); }
    @Override public E set(int at, E value) {
        writable(); index(at, size); element(value);
        int row = order[at]; E old = read(values, row); values[row] = value; return old;
    }
    @Override public E removeAt(int at) {
        writable(); index(at, size);
        int row = order[at]; E old = read(values, row);
        System.arraycopy(order, at + 1, order, at, size - at - 1);
        order[--size] = 0; values[row] = null; freeNext[row] = free; free = row;
        return old;
    }
    @Override public void clear() {
        writable(); values = EMPTY_OBJECTS; order = EMPTY_INTS; freeNext = EMPTY_INTS;
        size = 0; highWater = 0; free = -1;
    }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { for (int i = 0; i < size; i++) { action.accept(read(values, order[i])); } }
        finally { leave(); }
    }
    @Override public M3ShapeStorage storage() {
        return new M3ShapeStorage(values.length, 8L * order.length, values.length == 0 ? 0 : 3, order.length);
    }
}
