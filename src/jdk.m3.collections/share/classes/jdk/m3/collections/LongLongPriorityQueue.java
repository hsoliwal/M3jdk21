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

/**
 * Priority queue with parallel long[] value and priority lanes.
 *
 * <p>The root is the top priority. There is no entry object per queued item.
 * An optional admission filter can reject candidates using the current root,
 * which supports top/min/max gating before insertion.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongLongPriorityQueue.java}
 * blob {@code a06d3f6a28a55250937ed287e65cc59d506d4589} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class LongLongPriorityQueue {
    @FunctionalInterface
    public interface Consumer {
        void accept(long value, long priority);
    }

    @FunctionalInterface
    public interface Admission {
        boolean test(long value, long priority, long topValue, long topPriority, int size);
    }

    private static final Admission ACCEPT_ALL =
            (value, priority, topValue, topPriority, size) -> true;

    private long[] values;
    private long[] priorities;
    private int size;
    private final boolean minFirst;
    private final Admission admission;

    public LongLongPriorityQueue() {
        this(8, true, ACCEPT_ALL);
    }

    public LongLongPriorityQueue(int initialCapacity, boolean minFirst) {
        this(initialCapacity, minFirst, ACCEPT_ALL);
    }

    public LongLongPriorityQueue(int initialCapacity, boolean minFirst, Admission admission) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        int capacity = Math.max(1, initialCapacity);
        values = new long[capacity];
        priorities = new long[capacity];
        this.minFirst = minFirst;
        this.admission = Objects.requireNonNull(admission, "admission");
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return values.length; }
    public boolean minFirst() { return minFirst; }
    public long payloadBytes() {
        return (long) values.length * Long.BYTES + (long) priorities.length * Long.BYTES;
    }

    public boolean offer(long value, long priority) {
        long topValue = size == 0 ? value : values[0];
        long topPriority = size == 0 ? priority : priorities[0];
        if (!admission.test(value, priority, topValue, topPriority, size)) return false;

        ensureCapacity(size + 1);
        int index = size++;
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (!before(value, priority, values[parent], priorities[parent])) break;
            values[index] = values[parent];
            priorities[index] = priorities[parent];
            index = parent;
        }
        values[index] = value;
        priorities[index] = priority;
        return true;
    }

    /**
     * Appends primitive value/priority lanes and heapifies once. With an
     * admission filter configured, candidates are offered individually so the
     * filter observes the evolving root exactly as single insertion would.
     */
    public int offerAll(long[] sourceValues, long[] sourcePriorities) {
        Objects.requireNonNull(sourceValues, "sourceValues");
        Objects.requireNonNull(sourcePriorities, "sourcePriorities");
        if (sourceValues.length != sourcePriorities.length) {
            throw new IllegalArgumentException("value/priority length mismatch");
        }
        if (sourceValues.length == 0) return 0;

        if (admission != ACCEPT_ALL) {
            int added = 0;
            for (int i = 0; i < sourceValues.length; i++) {
                if (offer(sourceValues[i], sourcePriorities[i])) added++;
            }
            return added;
        }

        ensureCapacity(Math.addExact(size, sourceValues.length));
        System.arraycopy(sourceValues, 0, values, size, sourceValues.length);
        System.arraycopy(sourcePriorities, 0, priorities, size, sourcePriorities.length);
        size += sourceValues.length;
        heapify();
        return sourceValues.length;
    }

    public long peekValue() {
        if (size == 0) throw new NoSuchElementException();
        return values[0];
    }

    public long peekPriority() {
        if (size == 0) throw new NoSuchElementException();
        return priorities[0];
    }

    public void poll(Consumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (size == 0) throw new NoSuchElementException();

        long resultValue = values[0];
        long resultPriority = priorities[0];
        int last = --size;
        long value = values[last];
        long priority = priorities[last];
        values[last] = 0L;
        priorities[last] = 0L;

        if (size > 0) siftDownFrom(0, value, priority);
        consumer.accept(resultValue, resultPriority);
    }

    public void replaceTop(long value, long priority) {
        if (size == 0) throw new NoSuchElementException();
        siftDownFrom(0, value, priority);
    }

    public void clear() {
        Arrays.fill(values, 0, size, 0L);
        Arrays.fill(priorities, 0, size, 0L);
        size = 0;
    }

    private void heapify() {
        for (int index = (size >>> 1) - 1; index >= 0; index--) {
            siftDownFrom(index, values[index], priorities[index]);
        }
    }

    private void siftDownFrom(int index, long value, long priority) {
        int half = size >>> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int right = child + 1;
            if (right < size
                    && before(values[right], priorities[right], values[child], priorities[child])) {
                child = right;
            }
            if (!before(values[child], priorities[child], value, priority)) break;
            values[index] = values[child];
            priorities[index] = priorities[child];
            index = child;
        }
        values[index] = value;
        priorities[index] = priority;
    }

    private boolean before(long leftValue, long leftPriority,
                           long rightValue, long rightPriority) {
        int priorityOrder = Long.compare(leftPriority, rightPriority);
        if (priorityOrder == 0) priorityOrder = Long.compare(leftValue, rightValue);
        return minFirst ? priorityOrder < 0 : priorityOrder > 0;
    }

    public void ensureCapacity(int required) {
        if (required < 0) throw new IllegalArgumentException("required < 0");
        if (required <= values.length) return;
        int capacity = values.length + (values.length >>> 1) + 1;
        if (capacity < required) capacity = required;
        // Stage both backing lanes before publishing either one. A failed
        // second allocation leaves the original paired geometry untouched.
        long[] grownValues = Arrays.copyOf(values, capacity);
        long[] grownPriorities = Arrays.copyOf(priorities, capacity);
        values = grownValues;
        priorities = grownPriorities;
    }
}
