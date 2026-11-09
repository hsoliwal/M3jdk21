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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;
import jdk.internal.m3.collections.packed.DenseIndexedLongHeap;
import jdk.internal.m3.collections.sketch.DenseTopK;

/**
 * Constructors for mutable array-order/admission families. The existing
 * DenseCollections remains the owner of Eclipse Collections hash/primitive families.
 * Already-array-backed JDK containers are reused rather than reimplemented.
 * Generic numeric elements still box; use primitive collections or the indexed
 * long-priority heap for retained primitive lanes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseArrayCollections.java}
 * blob {@code b6eb9096beea05de7d7fa60e062ba8c949d0df5e} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseArrayCollections {
    private DenseArrayCollections() { }

    public static <E> ArrayList<E> list(int capacity) { return new ArrayList<>(capacity); }
    public static <E> ArrayDeque<E> deque(int capacity) { return new ArrayDeque<>(capacity); }
    public static <E> PriorityQueue<E> heap(int capacity, Comparator<? super E> comparator) {
        return new PriorityQueue<>(capacity, comparator);
    }
    public static BitSet bits(int bitCount) { return new BitSet(bitCount); }
    public static <E extends Enum<E>> EnumSet<E> enumSet(Class<E> type) { return EnumSet.noneOf(type); }
    public static <E extends Enum<E>, V> EnumMap<E, V> enumMap(Class<E> type) { return new EnumMap<>(type); }
    public static <E> ArrayBlockingQueue<E> blockingFifo(int capacity, boolean fair) {
        return new ArrayBlockingQueue<>(capacity, fair);
    }
    public static <E> PriorityBlockingQueue<E> blockingHeap(int capacity, Comparator<? super E> comparator) {
        return new PriorityBlockingQueue<>(capacity, comparator);
    }
    public static <K, V> DenseArraySortedMap<K, V> sortedMap(Comparator<? super K> comparator) {
        return new DenseArraySortedMap<>(comparator);
    }
    public static <E> DenseArraySortedSet<E> sortedSet(Comparator<? super E> comparator) {
        return new DenseArraySortedSet<>(comparator);
    }
    public static <E> DenseSortedBuffer<E> sortedBuffer(Comparator<? super E> comparator) {
        return new DenseSortedBuffer<>(comparator);
    }
    public static <E> DenseSparseSortedBuffer<E> sparseSortedBuffer(int capacity, double loadFactor,
            Comparator<? super E> comparator) {
        return new DenseSparseSortedBuffer<>(capacity, loadFactor, comparator);
    }
    public static <E> DenseTopK<E> topK(int capacity, Comparator<? super E> comparator,
            Predicate<? super E> predicate) {
        return new DenseTopK<>(capacity, comparator, predicate);
    }
    public static DenseIndexedLongHeap indexedLongHeap(int idCapacity, boolean minimumFirst) {
        return new DenseIndexedLongHeap(idCapacity, minimumFirst);
    }
    public static <E> DenseAdmission<E> filtered(Collection<E> backing, Predicate<? super E> predicate) {
        return new DenseAdmission<>(backing, predicate);
    }
}
