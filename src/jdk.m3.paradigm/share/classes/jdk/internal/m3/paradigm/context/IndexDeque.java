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
import java.util.NoSuchElementException;

/**
 * Power-of-two primitive ring deque for context indices.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextIndexDeque.java}
 * blob {@code 6c444882961dbc1d0fdff885097220f1be425a9a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IndexDeque {
    private static final int MIN_CAPACITY = 8;
    private int[] elements;
    private int head;
    private int size;

    public IndexDeque() {
        elements = new int[MIN_CAPACITY];
    }

    public IndexDeque(int expectedSize) {
        if (expectedSize < 0) throw new IllegalArgumentException("negative size");
        int capacity = MIN_CAPACITY;
        while (capacity < expectedSize) capacity <<= 1;
        elements = new int[capacity];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return elements.length; }

    public void addFirst(int value) {
        IndexList.requireIndexValue(value);
        ensureCapacity();
        head = (head - 1) & (elements.length - 1);
        elements[head] = value;
        size++;
    }

    public void addLast(int value) {
        IndexList.requireIndexValue(value);
        ensureCapacity();
        elements[(head + size) & (elements.length - 1)] = value;
        size++;
    }

    public int getFirst() {
        if (size == 0) throw new NoSuchElementException();
        return elements[head];
    }

    public int getLast() {
        if (size == 0) throw new NoSuchElementException();
        return elements[(head + size - 1) & (elements.length - 1)];
    }

    public int removeFirst() {
        int value = getFirst();
        elements[head] = 0;
        head = (head + 1) & (elements.length - 1);
        size--;
        return value;
    }

    public int removeLast() {
        int slot = (head + size - 1) & (elements.length - 1);
        int value = getLast();
        elements[slot] = 0;
        size--;
        return value;
    }

    public void clear() {
        Arrays.fill(elements, 0);
        head = 0;
        size = 0;
    }

    public int[] toArray() {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = elements[(head + i) & (elements.length - 1)];
        }
        return result;
    }

    private void ensureCapacity() {
        if (size < elements.length) return;
        int[] replacement = new int[elements.length << 1];
        for (int i = 0; i < size; i++) {
            replacement[i] = elements[(head + i) & (elements.length - 1)];
        }
        elements = replacement;
        head = 0;
    }
}
