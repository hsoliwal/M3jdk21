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

import jdk.m3.collections.LongLongConsumer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive long/long pair sequence backed by two parallel {@code long[]} lanes.
 * No pair, tuple, record or Map.Entry object is retained per row.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongPairs.java}
 * blob {@code d3f78b0c9e57eb36e707f594b509dea979b66995} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3PackedLongPairs implements M3LongPairs {
    private long[] first;
    private long[] second;
    private int size;

    public M3PackedLongPairs() {
        this(8);
    }

    public M3PackedLongPairs(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        int capacity = Math.max(1, expectedSize);
        first = new long[capacity];
        second = new long[capacity];
    }

    public void add(long firstValue, long secondValue) {
        ensureCapacity(size + 1);
        first[size] = firstValue;
        second[size] = secondValue;
        size++;
    }

    public long first(int index) {
        M3PackedSupport.checkIndex(index, size);
        return first[index];
    }

    public long second(int index) {
        M3PackedSupport.checkIndex(index, size);
        return second[index];
    }

    public void set(int index, long firstValue, long secondValue) {
        M3PackedSupport.checkIndex(index, size);
        first[index] = firstValue;
        second[index] = secondValue;
    }

    public void removeAt(int index) {
        M3PackedSupport.checkIndex(index, size);
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(first, index + 1, first, index, moved);
            System.arraycopy(second, index + 1, second, index, moved);
        }
        size--;
    }

    public int filterInPlace(M3LongLongPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int write = 0;
        for (int read = 0; read < size; read++) {
            if (predicate.test(first[read], second[read])) {
                first[write] = first[read];
                second[write] = second[read];
                write++;
            }
        }
        int removed = size - write;
        size = write;
        return removed;
    }

    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) {
            consumer.accept(first[i], second[i]);
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public long[] firstToArray() {
        return Arrays.copyOf(first, size);
    }

    public long[] secondToArray() {
        return Arrays.copyOf(second, size);
    }

    private void ensureCapacity(int minimum) {
        if (minimum <= first.length) {
            return;
        }
        int capacity = M3PackedFlatArrays.growthCapacity(first.length, minimum);
        first = Arrays.copyOf(first, capacity);
        second = Arrays.copyOf(second, capacity);
    }
}
