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
package jdk.internal.m3.collections.sketch;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Bounded best-k admission. Comparator orders best first; root is the worst kept.
 *
 * <p>Object[] payloads and long[] sequence numbers, not per-candidate heap nodes.
 * Earlier comparator-equal candidates win. Rejection does not modify retained
 * state. Comparisons plan the sift before any slot is changed. Callbacks must
 * be pure, stable and non-reentrant. Not thread-safe, and not a Collection whose
 * add contract would conceal eviction/rejection.
 *
 * @param <E> candidate type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseTopK.java}
 * blob {@code e9e00a3c40df928f3ac80158c79b52eeb81342e5} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseTopK<E> implements TopKAdmission<E> {
    private final Object[] heap;
    private final long[] sequence;
    private final Comparator<? super E> comparator;
    private final Predicate<? super E> predicate;
    private int size;
    private long nextSequence;

    public DenseTopK(int capacity, Comparator<? super E> comparator) {
        this(capacity, comparator, element -> true);
    }

    public DenseTopK(int capacity, Comparator<? super E> comparator,
            Predicate<? super E> predicate) {
        if (capacity < 0 || capacity > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("capacity");
        }
        this.comparator = Objects.requireNonNull(comparator, "comparator");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.heap = new Object[capacity];
        this.sequence = new long[capacity];
    }

    public int size() { return size; }
    public int capacity() { return heap.length; }
    public boolean isEmpty() { return size == 0; }

    @SuppressWarnings("unchecked")
    private E at(int index) { return (E) heap[index]; }

    private int compare(E left, long leftSequence, E right, long rightSequence) {
        int result = comparator.compare(left, right);
        return result != 0 ? result : Long.compare(leftSequence, rightSequence);
    }

    public E peekWorst() { return size == 0 ? null : at(0); }

    /** Predicate is evaluated once, before capacity/cutoff admission. */
    public boolean offer(E candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (!predicate.test(candidate) || heap.length == 0) { return false; }
        if (size == heap.length && comparator.compare(candidate, at(0)) >= 0) { return false; }
        if (nextSequence == Long.MAX_VALUE) {
            throw new IllegalStateException("sequence exhausted; explicitly clear before reuse");
        }
        long ordinal = nextSequence;
        if (size < heap.length) {
            int target = size;
            while (target > 0) {
                int parent = (target - 1) >>> 1;
                if (compare(candidate, ordinal, at(parent), sequence[parent]) <= 0) { break; }
                target = parent;
            }
            int index = size;
            while (index > target) {
                int parent = (index - 1) >>> 1;
                heap[index] = heap[parent];
                sequence[index] = sequence[parent];
                index = parent;
            }
            heap[target] = candidate;
            sequence[target] = ordinal;
            size++;
        } else {
            replaceRoot(candidate, ordinal, size);
        }
        nextSequence++;
        return true;
    }

    /** Plan root-to-leaf movement, then replay its binary path without a path object. */
    private void replaceRoot(E candidate, long ordinal, int limit) {
        int target = 0;
        while (target < (limit >>> 1)) {
            int child = target * 2 + 1;
            if (child + 1 < limit
                    && compare(at(child + 1), sequence[child + 1], at(child), sequence[child]) > 0) {
                child++;
            }
            if (compare(candidate, ordinal, at(child), sequence[child]) >= 0) { break; }
            target = child;
        }
        int encodedPath = target + 1;
        int index = 0;
        for (int bit = Integer.highestOneBit(encodedPath) >>> 1; bit != 0; bit >>>= 1) {
            int child = index * 2 + 1 + ((encodedPath & bit) == 0 ? 0 : 1);
            heap[index] = heap[child];
            sequence[index] = sequence[child];
            index = child;
        }
        heap[target] = candidate;
        sequence[target] = ordinal;
    }

    public E pollWorst() {
        if (size == 0) { return null; }
        E previous = at(0);
        int last = size - 1;
        if (last != 0) { replaceRoot(at(last), sequence[last], last); }
        heap[last] = null;
        sequence[last] = 0;
        size = last;
        return previous;
    }

    public int offerAll(Iterable<? extends E> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        int accepted = 0;
        for (E candidate : candidates) {
            if (offer(candidate)) { accepted++; }
        }
        return accepted;
    }

    /** Visit retained occurrences without comparisons; the callback must not mutate this collector. */
    public void forEachRetained(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) { action.accept(at(i)); }
    }

    public void clear() {
        Arrays.fill(heap, 0, size, null);
        Arrays.fill(sequence, 0, size, 0L);
        size = 0;
        nextSequence = 0;
    }

    /** Best-first, shallow immutable snapshot. Equal ranks retain arrival order. */
    @SuppressWarnings("unchecked")
    public List<E> snapshotBestFirst() {
        DenseTopK<E> copy = new DenseTopK<>(size, comparator);
        System.arraycopy(heap, 0, copy.heap, 0, size);
        System.arraycopy(sequence, 0, copy.sequence, 0, size);
        copy.size = size;
        Object[] ordered = new Object[size];
        for (int i = size - 1; i >= 0; i--) { ordered[i] = copy.pollWorst(); }
        return Collections.unmodifiableList(Arrays.asList((E[]) ordered));
    }
}
