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
package jdk.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Mutable sorted primitive set: binary search + arraycopy, no nodes or wrappers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSortedArraySet.java}
 * blob {@code 3aaa3f2ea199255555a6e76b6735ab187e54bc62} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveSortedArraySet {
    private final PrimitiveKind kind;
    private Object elements;
    private int size;

    public M3PrimitiveSortedArraySet(PrimitiveKind kind) {
        this(kind, 8);
    }

    public M3PrimitiveSortedArraySet(PrimitiveKind kind, int initialCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        elements = kind.newArray(Math.max(1, initialCapacity));
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(elements); }
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    public boolean containsBits(long bits) { return search(bits) >= 0; }

    public boolean addBits(long bits) {
        int result = search(bits);
        if (result >= 0) return false;
        int insertion = -result - 1;
        ensure(size + 1);
        System.arraycopy(elements, insertion, elements, insertion + 1, size - insertion);
        kind.writeBits(elements, insertion, bits);
        size++;
        return true;
    }

    public boolean removeBits(long bits) {
        int index = search(bits);
        if (index < 0) return false;
        int moved = size - index - 1;
        if (moved > 0) System.arraycopy(elements, index + 1, elements, index, moved);
        size--;
        kind.clear(elements, size, size + 1);
        return true;
    }

    public long firstBits() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return kind.readBits(elements, 0);
    }

    public long lastBits() {
        if (size == 0) throw new java.util.NoSuchElementException();
        return kind.readBits(elements, size - 1);
    }

    public long getBits(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, index);
    }

    public int binarySearchBits(long bits) { return search(bits); }

    public void clear() {
        kind.clear(elements, 0, size);
        size = 0;
    }

    public Object toPrimitiveArray() { return kind.copyOf(elements, size); }

    public void forEachBits(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(kind.readBits(elements, i));
    }

    Object backingArray() { return elements; }

    /** Delegate to the already typed JDK primitive-array binary-search owner. */
    private int search(long bits) {
        return kind.binarySearch(elements, 0, size, bits);
    }

    private void ensure(int required) {
        int capacity = capacity();
        if (required <= capacity) return;
        int next = capacity + (capacity >>> 1) + 1;
        if (next < required) next = required;
        elements = kind.copyOf(elements, next);
    }
}
