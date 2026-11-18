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
import java.util.function.LongUnaryOperator;
import jdk.m3.collections.RuntimeHeap;

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
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongLazyPriorityFrontier.java}
 * blob {@code 63b800324034f0fcb626e7d6e62a2c1cb23ba064} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongLazyPriorityFrontier {
    private long[] ids;
    private long[] priorities;
    private long[] serials;
    /** One dynamically grown bit per 0-based heap offset, not a byte per entry. */
    private long[] resolvedWords;
    /** Fast-path witness: when zero, every live heap offset has a clear bit. */
    private int resolvedCount;
    private final LongUnaryOperator exactScorer;
    private int size;
    private long nextSerial;
    private long evaluations;
    private boolean evaluating;

    public LongLazyPriorityFrontier(LongUnaryOperator exactScorer) {
        this(8, exactScorer);
    }

    public LongLazyPriorityFrontier(int initialCapacity, LongUnaryOperator exactScorer) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        this.exactScorer = Objects.requireNonNull(exactScorer, "exactScorer");
        int capacity = Math.max(1, initialCapacity);
        ids = new long[capacity];
        priorities = new long[capacity];
        serials = new long[capacity];
        resolvedWords = new long[resolvedWordCount(capacity)];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long evaluations() { return evaluations; }

    /** Primitive payload only; excludes Java array headers and scorer footprint. */
    public long payloadBytes() {
        return (long) ids.length * 3L * Long.BYTES
                + (long) resolvedWords.length * Long.BYTES;
    }

    public void offer(long id, long upperBound) {
        requireNotEvaluating();
        if (nextSerial == Long.MAX_VALUE) throw new IllegalStateException("serial exhausted");
        ensureCapacity(Math.addExact(size, 1));
        int index = size++;
        ids[index] = id;
        priorities[index] = upperBound;
        serials[index] = nextSerial++;
        if (resolvedCount != 0) setResolved(index, false);
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
        if (resolvedCount == 0) {
            // Every inactive bit was cleared at poll/clear; new backing words
            // are zero. Heapifying an unresolved batch only needs serial writes.
            for (int i = size; i < nextSize; i++) serials[i] = nextSerial++;
        } else {
            for (int i = size; i < nextSize; i++) {
                serials[i] = nextSerial++;
                setResolved(i, false);
            }
        }
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
        // The certified root leaves first. Clear its former bit before an
        // unresolved tail moves into offset zero; all-unresolved sifts can
        // then elide mask writes without inheriting a stale certified bit.
        resolvedCount--;
        setResolved(0, false);
        if (last > 0) {
            copy(last, 0);
            siftDown(0);
        }
        ids[last] = 0;
        priorities[last] = 0;
        serials[last] = 0;
        setResolved(last, false);
        return id;
    }

    public void clear() {
        requireNotEvaluating();
        Arrays.fill(ids, 0, size, 0L);
        Arrays.fill(priorities, 0, size, 0L);
        Arrays.fill(serials, 0, size, 0L);
        Arrays.fill(resolvedWords, 0, resolvedWordCount(size), 0L);
        size = 0;
        resolvedCount = 0;
        nextSerial = 0L;
        evaluations = 0L;
    }

    private void certifyTop() {
        requireNotEvaluating();
        if (size == 0) throw new NoSuchElementException("empty priority frontier");
        while (!isResolved(0)) {
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
            setResolved(0, true);
            resolvedCount++;
            evaluations++;
            siftDown(0);
        }
    }

    private void requireNotEvaluating() {
        if (evaluating) throw new IllegalStateException("priority scorer reentered queue");
    }

    /** Conservative existing 25-byte-per-entry peak budget for heap pressure. */
    private static final int SLOT_BYTES = 3 * Long.BYTES + Byte.BYTES;

    /**
     * Pure decision at a backing-growth boundary. Normal heap headroom keeps geometric
     * amortization; pressure reduces only speculative slack, never the required elements.
     * The old arrays remain reachable until all four replacements have been prepared.
     */
    static int growthCapacity(RuntimeHeap.Snapshot heap, int current, int required) {
        Objects.requireNonNull(heap, "heap");
        if (current < 1 || required <= current)
            throw new IllegalArgumentException("growth must exceed current capacity");
        long geometric = (long) current + (current >>> 1) + 1L;
        long preferred = Math.min((long) Integer.MAX_VALUE,
                Math.max((long) required, geometric));
        return Math.toIntExact(RuntimeHeap.chooseAmortizedCapacity(
                heap, required, preferred, SLOT_BYTES));
    }

    private void ensureCapacity(int required) {
        if (required <= ids.length) return;
        // Single JVM Xmx/committed/used sample per growth, never per hot offer/peek.
        int capacity = growthCapacity(RuntimeHeap.current(), ids.length, required);
        // Allocate each replacement before publishing any lane.
        long[] newIds = Arrays.copyOf(ids, capacity);
        long[] newPriorities = Arrays.copyOf(priorities, capacity);
        long[] newSerials = Arrays.copyOf(serials, capacity);
        long[] newResolved = Arrays.copyOf(resolvedWords, resolvedWordCount(capacity));
        ids = newIds;
        priorities = newPriorities;
        serials = newSerials;
        resolvedWords = newResolved;
    }

    /** Number of 64-bit words covering heap offsets 0..capacity-1. */
    private static int resolvedWordCount(int capacity) {
        return capacity == 0 ? 0 : ((capacity - 1) >>> 6) + 1;
    }

    private boolean isResolved(int offset) {
        return (resolvedWords[offset >>> 6] & (1L << (offset & 63))) != 0L;
    }

    /** Change precisely one heap offset's certification bit, even during sifts. */
    private void setResolved(int offset, boolean value) {
        int word = offset >>> 6;
        long bit = 1L << (offset & 63);
        if (value) resolvedWords[word] |= bit;
        else resolvedWords[word] &= ~bit;
    }

    private boolean before(int a, int b) {
        return before(ids[a], priorities[a], serials[a], ids[b], priorities[b], serials[b]);
    }

    private static boolean before(long leftId, long leftPriority, long leftSerial,
                                  long rightId, long rightPriority, long rightSerial) {
        int order = Long.compare(leftPriority, rightPriority);
        if (order != 0) return order > 0;
        order = Long.compare(leftId, rightId);
        if (order != 0) return order < 0;
        return leftSerial < rightSerial;
    }

    private void siftUp(int start) {
        long id = ids[start];
        long priority = priorities[start];
        long serial = serials[start];
        boolean flag = isResolved(start);
        int index = start;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (!before(id, priority, serial, ids[parent], priorities[parent], serials[parent])) break;
            // Move one parent down into the hole; keep this entry in registers.
            copy(parent, index);
            index = parent;
        }
        if (index != start) place(index, id, priority, serial, flag);
    }

    private void siftDown(int start) {
        long id = ids[start];
        long priority = priorities[start];
        long serial = serials[start];
        boolean flag = isResolved(start);
        int index = start;
        while (index < (size >>> 1)) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size && before(right, child)) child = right;
            if (!before(ids[child], priorities[child], serials[child], id, priority, serial)) break;
            copy(child, index);
            index = child;
        }
        if (index != start) place(index, id, priority, serial, flag);
    }

    private void copy(int from, int to) {
        ids[to] = ids[from];
        priorities[to] = priorities[from];
        serials[to] = serials[from];
        // A common heapify/build-and-drain phase contains only unresolved
        // candidates. Zero is a proof that both source and target bits are
        // clear, so the hot sift need move only the three long payload lanes.
        if (resolvedCount != 0) setResolved(to, isResolved(from));
    }

    private void place(int index, long id, long priority, long serial, boolean flag) {
        ids[index] = id;
        priorities[index] = priority;
        serials[index] = serial;
        if (resolvedCount != 0) setResolved(index, flag);
    }
}
