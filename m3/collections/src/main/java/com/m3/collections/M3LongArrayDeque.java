// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Primitive long deque backed by one power-of-two circular {@code long[]}.
 *
 * <p>Queue and stack operations are O(1) amortized with no node allocation.
 */
public final class M3LongArrayDeque implements M3LongDeque {
    private long[] elements;
    private int head;
    private int size;

    public M3LongArrayDeque() {
        this(8);
    }

    public M3LongArrayDeque(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        elements = new long[M3PackedFlatArrays.powerOfTwoCapacity(expectedSize, 2)];
    }

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

    public void addLast(long value) {
        ensureSpace();
        elements[M3PackedRingAtoms.powerOfTwoIndex(head, size, elements.length)] = value;
        size++;
    }

    public boolean addLastIf(long value, LongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (!predicate.test(value)) {
            return false;
        }
        addLast(value);
        return true;
    }

    public long first() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[head];
    }

    public long last() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return elements[M3PackedRingAtoms.powerOfTwoIndex(head, size - 1, elements.length)];
    }

    public long removeFirst() {
        long value = first();
        head = M3PackedRingAtoms.powerOfTwoNext(head, elements.length);
        size--;
        head = M3PackedRingAtoms.normalizeHeadAfterRemoval(head, size);
        return value;
    }

    public long removeLast() {
        long value = last();
        size--;
        head = M3PackedRingAtoms.normalizeHeadAfterRemoval(head, size);
        return value;
    }

    public long get(int index) {
        M3PackedSupport.checkIndex(index, size);
        return elements[M3PackedRingAtoms.powerOfTwoIndex(head, index, elements.length)];
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
        M3PackedFlatArrays.copyLogicalRing(elements, head, size, elements.length, result);
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
        long[] grown = new long[M3PackedFlatArrays.growPowerOfTwo(elements.length)];
        M3PackedFlatArrays.copyLogicalRing(elements, head, size, elements.length, grown);
        elements = grown;
        head = 0;
    }
}
