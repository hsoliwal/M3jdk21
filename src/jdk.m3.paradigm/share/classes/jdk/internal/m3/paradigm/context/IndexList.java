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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Primitive non-negative context-index list with no boxing or node objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextIndexList.java}
 * blob {@code dd932224597d4b1b6e00005d4816c8c563a771b1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IndexList {
    private static final int DEFAULT_CAPACITY = 8;
    private int[] values;
    private int size;

    public IndexList() {
        values = new int[DEFAULT_CAPACITY];
    }

    public IndexList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("negative capacity");
        }
        values = new int[Math.max(1, initialCapacity)];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public int get(int index) {
        return values[Objects.checkIndex(index, size)];
    }

    public void set(int index, int value) {
        requireIndexValue(value);
        values[Objects.checkIndex(index, size)] = value;
    }

    public void add(int value) {
        requireIndexValue(value);
        ensureCapacity(size + 1);
        values[size++] = value;
    }

    public void add(int index, int value) {
        requireIndexValue(value);
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(index);
        }
        ensureCapacity(size + 1);
        System.arraycopy(values, index, values, index + 1, size - index);
        values[index] = value;
        size++;
    }

    public int removeAt(int index) {
        Objects.checkIndex(index, size);
        int previous = values[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(values, index + 1, values, index, moved);
        }
        values[--size] = 0;
        return previous;
    }

    public int removeLast() {
        if (size == 0) {
            throw new java.util.NoSuchElementException();
        }
        int previous = values[--size];
        values[size] = 0;
        return previous;
    }

    public int indexOf(int value) {
        requireIndexValue(value);
        for (int i = 0; i < size; i++) {
            if (values[i] == value) return i;
        }
        return -1;
    }

    public boolean contains(int value) {
        return indexOf(value) >= 0;
    }

    public void clear() {
        Arrays.fill(values, 0, size, 0);
        size = 0;
    }

    public void forEach(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(values[i]);
    }

    public int[] toArray() {
        return Arrays.copyOf(values, size);
    }

    public int capacity() {
        return values.length;
    }

    private void ensureCapacity(int required) {
        if (required <= values.length) return;
        int current = values.length;
        int next = current + (current >>> 1) + 1;
        if (next < required) next = required;
        values = Arrays.copyOf(values, next);
    }

    static void requireIndexValue(int value) {
        if (value < 0) throw new IllegalArgumentException("context index must be >= 0");
    }
}
