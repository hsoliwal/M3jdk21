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
import java.util.NoSuchElementException;

/**
 * Dense primitive binary min-heap / priority queue.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongMinHeap.java}
 * blob {@code 09282c3a7776a4186cbcee061652e78a64bb20be} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongMinHeap {
    private long[] heap;
    private int size;
    public M3LongMinHeap() { this(8); }
    public M3LongMinHeap(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException();
        heap = new long[Math.max(1, capacity)];
    }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return (long) heap.length * Long.BYTES; }
    public void add(long value) {
        ensure();
        int i = size++;
        while (i > 0) {
            int p = (i - 1) >>> 1;
            long pv = heap[p];
            if (pv <= value) break;
            heap[i] = pv;
            i = p;
        }
        heap[i] = value;
    }
    public long peek() {
        if (size == 0) throw new NoSuchElementException();
        return heap[0];
    }
    public long poll() {
        if (size == 0) throw new NoSuchElementException();
        long result = heap[0];
        long x = heap[--size];
        heap[size] = 0L;
        if (size > 0) {
            int i = 0;
            int half = size >>> 1;
            while (i < half) {
                int child = (i << 1) + 1;
                long cv = heap[child];
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
    public void clear() { Arrays.fill(heap, 0, size, 0L); size = 0; }
    private void ensure() {
        if (size == heap.length) heap = Arrays.copyOf(heap, heap.length + (heap.length >>> 1) + 1);
    }
}
