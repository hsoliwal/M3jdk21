/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.function.IntPredicate;

/** Mutable indexed int[] contract; no per-element Integer boxing. */
public interface M3IntList extends M3IntCollection {
    boolean add(int value);
    void insert(int index, int value);
    boolean addAll(int[] values);
    boolean addAll(int index, int[] values);
    int get(int index);
    int set(int index, int value);
    int removeAt(int index);
    boolean removeValue(int value);
    int indexOf(int value);
    int lastIndexOf(int value);
    void sort();
    int binarySearch(int value);
    int deduplicateSorted();
    int filterInPlace(IntPredicate predicate);
    int retainByStateMask(byte[] alignedStates, int requiredMask);
    int capacity();
    void ensureCapacity(int minimumCapacity);
    void trimToSize();
    M3IntIndexedSnapshot snapshot();
    M3IntIndexedSnapshot filteredSnapshot(IntPredicate predicate);
    M3IntIndexedSnapshot maskedSnapshot(byte[] state, int required,
            M3IndexMaskCompactor.Backend backend);
    default boolean addIf(int value, IntPredicate predicate) {
        if (!java.util.Objects.requireNonNull(predicate).test(value)) return false;
        return add(value);
    }
}
