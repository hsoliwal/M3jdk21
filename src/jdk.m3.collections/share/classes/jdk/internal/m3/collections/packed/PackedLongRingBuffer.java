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

import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongQueue;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Fixed-capacity primitive long ring buffer.
 *
 * <p>Unlike {@code PackedLongDeque}, capacity never grows. This makes memory usage
 * deterministic for telemetry, rolling caches and bounded queues.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongRingBuffer.java}
 * blob {@code c73df12a32514f6aebf46c5c77c22a1b67d9ce17} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongRingBuffer implements LongQueue {
    private final long[] elements;
    private int head;
    private int size;

    public PackedLongRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity");
        }
        elements = new long[capacity];
    }

    public boolean offer(long value) {
        if (size == elements.length) {
            return false;
        }
        elements[PackedRingAtoms.index(head, size, elements.length)] = value;
        size++;
        return true;
    }

    public boolean offerIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return predicate.test(value) && offer(value);
    }

    /**
     * Appends and overwrites the oldest value when full.
     *
     * @return true when an old value had to be evicted
     */
    public boolean addOverwrite(long value) {
        boolean evicted = size == elements.length;
        if (evicted) {
            elements[head] = value;
            head = PackedRingAtoms.next(head, elements.length);
        } else {
            elements[PackedRingAtoms.index(head, size, elements.length)] = value;
            size++;
        }
        return evicted;
    }

    public long peek() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[head];
    }

    public long poll() {
        long value = peek();
        head = PackedRingAtoms.next(head, elements.length);
        size--;
        head = PackedRingAtoms.normalizeHeadAfterRemoval(head, size);
        return value;
    }

    public long get(int index) {
        PackedSupport.checkIndex(index, size);
        return elements[PackedRingAtoms.index(head, index, elements.length)];
    }

    @Override
    public boolean contains(long value) {
        for (int i = 0; i < size; i++) {
            if (get(i) == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    public int remainingCapacity() {
        return elements.length - size;
    }

    @Override
    public void clear() {
        head = 0;
        size = 0;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        PackedFlatArrays.copyLogicalRing(elements, head, size, elements.length, result);
        return result;
    }

    @Override
    public LongIterator iterator() {
        return new LongIterator() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(index++);
            }
        };
    }
}
