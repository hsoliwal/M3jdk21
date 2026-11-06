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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongUnaryOperator;

/**
 * Max-priority frontier with exact scores computed only when necessary to certify the top.
 *
 * <p>Every submitted score is an <em>upper bound</em> on a stable exact score. Until a
 * candidate reaches the root, the heap orders its upper bound without calling the scorer.
 * When an unresolved candidate reaches the root, its score is evaluated once and the root
 * is repaired. The first resolved root is consequently the exact maximum. Ties prefer the
 * smaller signed ID, then earlier insertion, including when IDs repeat.
 *
 * <p>Only primitive arrays are retained per entry. The one scorer is collection-level.
 * The scorer must be deterministic for the queue's lifetime, must not mutate the queue,
 * and may not change underlying scores while entries are live. This class is single-threaded.
 * A bound violation or scorer exception leaves the current root unresolved and unconsumed.
 *
 * <p>Offer costs O(log n), batch heapification O(n), and each certified extraction may
 * require multiple refinements. Worst-case certification can evaluate all remaining items;
 * this is not a promise of uniformly distributed work or bounded peek latency.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/LongLazyPriorityFrontier.java}
 * blob {@code 63b800324034f0fcb626e7d6e62a2c1cb23ba064} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongLazyPriorityFrontier {
    private long[] ids;
    private long[] priorities;
    private long[] serials;
    private byte[] resolved;
    private final LongUnaryOperator exactScorer;
    private int size;
    private long nextSerial;
    private long evaluations;
    private boolean evaluating;

    public M3LongLazyPriorityFrontier(LongUnaryOperator exactScorer) {
        this(8, exactScorer);
    }

    public M3LongLazyPriorityFrontier(int initialCapacity, LongUnaryOperator exactScorer) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        this.exactScorer = Objects.requireNonNull(exactScorer, "exactScorer");
        int capacity = Math.max(1, initialCapacity);
        ids = new long[capacity];
        priorities = new long[capacity];
        serials = new long[capacity];
        resolved = new byte[capacity];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long evaluations() { return evaluations; }

    /** Primitive payload only; excludes Java array headers and scorer footprint. */
    public long payloadBytes() {
        return (long) ids.length * (3L * Long.BYTES + Byte.BYTES);
    }

    public void offer(long id, long upperBound) {
        requireNotEvaluating();
        if (nextSerial == Long.MAX_VALUE) throw new IllegalStateException("serial exhausted");
        ensureCapacity(Math.addExact(size, 1));
        int index = size++;
        ids[index] = id;
        priorities[index] = upperBound;
        serials[index] = nextSerial++;
        resolved[index] = 0;
        siftUp(index);
    }

    /**
     * Appends independent primitive lanes and heapifies once. Neither input is retained.
     * No exact scores are evaluated during this operation.
     */
    public void offerAll(long[] values, long[] upperBounds) {
        requireNotEvaluating();
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(upperBounds, "upperBounds");
        if (values.length != upperBounds.length) {
            throw new IllegalArgumentException("value/upper-bound length mismatch");
        }
        if (Long.MAX_VALUE - nextSerial < values.length) {
            throw new IllegalStateException("serial exhausted");
        }
        int nextSize = Math.addExact(size, values.length);
        ensureCapacity(nextSize);
        System.arraycopy(values, 0, ids, size, values.length);
        System.arraycopy(upperBounds, 0, priorities, size, values.length);
        Arrays.fill(resolved, size, nextSize, (byte) 0);
        for (int i = size; i < nextSize; i++) serials[i] = nextSerial++;
        size = nextSize;
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) siftDown(parent);
    }

    /** Returns the exact best ID; may evaluate and reorder several candidates. */
    public long peekId() {
        certifyTop();
        return ids[0];
    }

    /** Returns the exact best score; may evaluate and reorder several candidates. */
    public long peekPriority() {
        certifyTop();
        return priorities[0];
    }

    /** Removes only the certified best ID. A failed certification removes nothing. */
    public long pollId() {
        certifyTop();
        long id = ids[0];
        int last = --size;
        if (last > 0) {
            copy(last, 0);
            siftDown(0);
        }
        ids[last] = 0;
        priorities[last] = 0;
        serials[last] = 0;
        resolved[last] = 0;
        return id;
    }

    public void clear() {
        requireNotEvaluating();
        Arrays.fill(ids, 0, size, 0L);
        Arrays.fill(priorities, 0, size, 0L);
        Arrays.fill(serials, 0, size, 0L);
        Arrays.fill(resolved, 0, size, (byte) 0);
        size = 0;
        nextSerial = 0L;
        evaluations = 0L;
    }

    private void certifyTop() {
        requireNotEvaluating();
        if (size == 0) throw new NoSuchElementException("empty priority frontier");
        while (resolved[0] == 0) {
            long exact;
            evaluating = true;
            try {
                exact = exactScorer.applyAsLong(ids[0]);
            } finally {
                evaluating = false;
            }
            if (exact > priorities[0]) {
                throw new IllegalArgumentException("exact score exceeds optimistic upper bound");
            }
            priorities[0] = exact;
            resolved[0] = 1;
            evaluations++;
            siftDown(0);
        }
    }

    private void requireNotEvaluating() {
        if (evaluating) throw new IllegalStateException("priority scorer reentered queue");
    }

    /** Exact 25-byte primitive payload per logical score, ignoring Java array headers. */
    private static final int SLOT_BYTES = 3 * Long.BYTES + Byte.BYTES;

    /**
     * Pure decision at a backing-growth boundary. Normal heap headroom keeps geometric
     * amortization; pressure reduces only speculative slack, never the required elements.
     * The old arrays remain reachable until all four replacements have been prepared.
     */
    static int growthCapacity(M3RuntimeHeap.Snapshot heap, int current, int required) {
        Objects.requireNonNull(heap, "heap");
        if (current < 1 || required <= current)
            throw new IllegalArgumentException("growth must exceed current capacity");
        long geometric = (long) current + (current >>> 1) + 1L;
        long preferred = Math.min((long) Integer.MAX_VALUE,
                Math.max((long) required, geometric));
        return Math.toIntExact(M3RuntimeHeap.chooseAmortizedCapacity(
                heap, required, preferred, SLOT_BYTES));
    }

    private void ensureCapacity(int required) {
        if (required <= ids.length) return;
        // Single JVM Xmx/committed/used sample per growth, never per hot offer/peek.
        int capacity = growthCapacity(M3RuntimeHeap.current(), ids.length, required);
        // Allocate each replacement before publishing any lane.
        long[] newIds = Arrays.copyOf(ids, capacity);
        long[] newPriorities = Arrays.copyOf(priorities, capacity);
        long[] newSerials = Arrays.copyOf(serials, capacity);
        byte[] newResolved = Arrays.copyOf(resolved, capacity);
        ids = newIds;
        priorities = newPriorities;
        serials = newSerials;
        resolved = newResolved;
    }

    private boolean before(int a, int b) {
        int order = Long.compare(priorities[a], priorities[b]);
        if (order != 0) return order > 0;
        order = Long.compare(ids[a], ids[b]);
        if (order != 0) return order < 0;
        return serials[a] < serials[b];
    }

    private void siftUp(int start) {
        int index = start;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (!before(index, parent)) break;
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int start) {
        int index = start;
        while (index < (size >>> 1)) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size && before(right, child)) child = right;
            if (!before(child, index)) return;
            swap(index, child);
            index = child;
        }
    }

    private void copy(int from, int to) {
        ids[to] = ids[from];
        priorities[to] = priorities[from];
        serials[to] = serials[from];
        resolved[to] = resolved[from];
    }

    private void swap(int a, int b) {
        long id = ids[a];
        long priority = priorities[a];
        long serial = serials[a];
        byte flag = resolved[a];
        copy(b, a);
        ids[b] = id;
        priorities[b] = priority;
        serials[b] = serial;
        resolved[b] = flag;
    }
}
