/* *****************************************************************************
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
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Dense primitive binary min-heap / priority queue.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/IntMinHeap.java}
 * blob {@code 52b829cbfc843e6e99fffc68b62604962d2e9d4a} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class IntMinHeap {
    private int[] heap;
    private int size;
    public IntMinHeap() { this(8); }
    public IntMinHeap(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException();
        heap = new int[Math.max(1, capacity)];
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return (long) heap.length * Integer.BYTES; }
    public void add(int value) {
        ensure();
        int i = size++;
        while (i > 0) {
            int p = (i - 1) >>> 1;
            int pv = heap[p];
            if (pv <= value) break;
            heap[i] = pv;
            i = p;
        }
        heap[i] = value;
    }

    /**
     * Adds a primitive batch using either O(k log(n + k)) sift-up or
     * O(n + k) bottom-up Floyd heapification, whichever has the smaller cost bound.
     * The input is copied; no per-value node or boxing is retained.
     */
    public void addAll(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) return;
        int newSize = Math.addExact(size, values.length);
        if (newSize > heap.length) {
            int grown = heap.length + (heap.length >>> 1) + 1;
            if (grown < 0) grown = Integer.MAX_VALUE - 8;
            heap = Arrays.copyOf(heap, Math.max(newSize, grown));
        }
        int oldSize = size;
        System.arraycopy(values, 0, heap, oldSize, values.length);
        size = newSize;
        // Small batches avoid a full-heap scan; larger batches use linear heapify.
        long insertionBound = (long) values.length * (Integer.SIZE - Integer.numberOfLeadingZeros(size));
        if (insertionBound < size) {
            for (int slot = oldSize; slot < size; slot++) {
                int value = heap[slot];
                int i = slot;
                while (i > 0) {
                    int parent = (i - 1) >>> 1;
                    int parentValue = heap[parent];
                    if (parentValue <= value) break;
                    heap[i] = parentValue;
                    i = parent;
                }
                heap[i] = value;
            }
            return;
        }
        for (int root = (size >>> 1) - 1; root >= 0; root--) {
            int value = heap[root];
            int i = root;
            int half = size >>> 1;
            while (i < half) {
                int child = (i << 1) + 1;
                int childValue = heap[child];
                int right = child + 1;
                if (right < size && heap[right] < childValue) {
                    child = right;
                    childValue = heap[right];
                }
                if (value <= childValue) break;
                heap[i] = childValue;
                i = child;
            }
            heap[i] = value;
        }
    }
    public int peek() {
        if (size == 0) throw new NoSuchElementException();
        return heap[0];
    }
    public int poll() {
        if (size == 0) throw new NoSuchElementException();
        int result = heap[0];
        int x = heap[--size];
        heap[size] = 0;
        if (size > 0) {
            int i = 0;
            int half = size >>> 1;
            while (i < half) {
                int child = (i << 1) + 1;
                int cv = heap[child];
                int right = child + 1;
                if (right < size && heap[right] < cv) {
                    child = right;
                    cv = heap[right];
                }
                if (x <= cv) break;
                heap[i] = cv;
                i = child;
            }
            heap[i] = x;
        }
        return result;
    }
    public void clear() { Arrays.fill(heap, 0, size, 0); size = 0; }
    private void ensure() {
        if (size == heap.length) heap = Arrays.copyOf(heap, heap.length + (heap.length >>> 1) + 1);
    }
}
