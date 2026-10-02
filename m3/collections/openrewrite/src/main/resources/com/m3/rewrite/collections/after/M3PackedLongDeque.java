/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.PackedLongDeque
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0,
 * with ring arithmetic from Synexia PR #7675 head
 * 8fa47689547a1efeabbb35d2caf1683d902a87e4.
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/** Primitive long deque backed by one power-of-two circular long array. */
public final class M3PackedLongDeque implements M3LongDeque {
    private long[] elements;
    private int head;
    private int size;

    public M3PackedLongDeque() {
        this(8);
    }

    public M3PackedLongDeque(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        elements = new long[M3PackedArrays.powerOfTwoCapacity(expectedSize, 2)];
    }

    @Override
    public void addFirst(long value) {
        ensureSpace();
        head = M3PackedRingAtoms.powerOfTwoPrevious(head, elements.length);
        elements[head] = value;
        size++;
    }

    @Override
    public boolean offer(long value) {
        addLast(value);
        return true;
    }

    @Override
    public long peek() {
        return first();
    }

    @Override
    public long poll() {
        return removeFirst();
    }

    @Override
    public void addLast(long value) {
        ensureSpace();
        elements[M3PackedRingAtoms.powerOfTwoIndex(head, size, elements.length)] = value;
        size++;
    }

    @Override
    public boolean addLastIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        addLast(value);
        return true;
    }

    @Override
    public long first() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[head];
    }

    @Override
    public long last() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[M3PackedRingAtoms.powerOfTwoIndex(head, size - 1, elements.length)];
    }

    @Override
    public long removeFirst() {
        long value = first();
        head = M3PackedRingAtoms.powerOfTwoNext(head, elements.length);
        size--;
        head = M3PackedRingAtoms.normalizeHeadAfterRemoval(head, size);
        return value;
    }

    @Override
    public long removeLast() {
        long value = last();
        size--;
        head = M3PackedRingAtoms.normalizeHeadAfterRemoval(head, size);
        return value;
    }

    @Override
    public long get(int index) {
        M3PackedSupport.checkIndex(index, size);
        return elements[M3PackedRingAtoms.powerOfTwoIndex(head, index, elements.length)];
    }

    @Override
    public boolean contains(long value) {
        for (int index = 0; index < size; index++) {
            if (get(index) == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int filterInPlace(LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long[] source = toArray();
        clear();
        for (long value : source) {
            if (predicate.test(value)) {
                addLast(value);
            }
        }
        return source.length - size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return elements.length;
    }

    @Override
    public void clear() {
        head = 0;
        size = 0;
    }

    @Override
    public long[] toArray() {
        long[] result = new long[size];
        M3PackedArrays.copyLogicalRing(elements, head, size, elements.length, result);
        return result;
    }

    @Override
    public M3LongIterator iterator() {
        return new M3LongIterator() {
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

    private void ensureSpace() {
        if (size < elements.length) {
            return;
        }
        long[] grown = new long[M3PackedArrays.growPowerOfTwo(elements.length)];
        M3PackedArrays.copyLogicalRing(elements, head, size, elements.length, grown);
        elements = grown;
        head = 0;
    }
}
