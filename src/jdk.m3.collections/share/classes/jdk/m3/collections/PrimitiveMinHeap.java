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

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Exact-width primitive binary min-heap.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveMinHeap.java}
 * blob {@code cf053cec8ca0654bc3447b8eda5cdaf11715a2a4} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveMinHeap {
    private final PrimitiveKind kind;
    private Object heap;
    private int size;

    public PrimitiveMinHeap(PrimitiveKind kind) {
        this(kind, 8);
    }

    public PrimitiveMinHeap(PrimitiveKind kind, int capacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (capacity < 0) throw new IllegalArgumentException("capacity < 0");
        heap = kind.newArray(Math.max(1, capacity));
    }

    public PrimitiveKind kind() { return kind; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return java.lang.reflect.Array.getLength(heap); }
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    public void addBits(long bits) {
        ensure();
        int index = size++;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            long parentValue = kind.readBits(heap, parent);
            if (kind.compareBits(parentValue, bits) <= 0) break;
            kind.writeBits(heap, index, parentValue);
            index = parent;
        }
        kind.writeBits(heap, index, bits);
    }

    public long peekBits() {
        if (size == 0) throw new NoSuchElementException();
        return kind.readBits(heap, 0);
    }

    public long pollBits() {
        if (size == 0) throw new NoSuchElementException();
        long result = kind.readBits(heap, 0);
        long replacement = kind.readBits(heap, --size);
        kind.clear(heap, size, size + 1);
        if (size > 0) siftDown(0, replacement);
        return result;
    }

    public void clear() {
        kind.clear(heap, 0, size);
        size = 0;
    }

    Object backingArray() { return heap; }

    private void siftDown(int index, long value) {
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            long childValue = kind.readBits(heap, child);
            int right = child + 1;
            if (right < size) {
                long rightValue = kind.readBits(heap, right);
                if (kind.compareBits(rightValue, childValue) < 0) {
                    child = right;
                    childValue = rightValue;
                }
            }
            if (kind.compareBits(value, childValue) <= 0) break;
            kind.writeBits(heap, index, childValue);
            index = child;
        }
        kind.writeBits(heap, index, value);
    }

    private void ensure() {
        if (size < capacity()) return;
        int next = capacity() + (capacity() >>> 1) + 1;
        heap = kind.copyOf(heap, next);
    }
}
