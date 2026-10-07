// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Immutable canonical view of stable priority-queue dequeue order.
 *
 * <p>Each entry occupies three integers: priority high bits, priority low bits,
 * and element ID. Equal-priority entries preserve stable encounter order.
 */
public final class MIndexFrozenPriorityQueue<E> implements MIndexCanonicalCollection {
    private static final int INTS_PER_ENTRY = 3;

    private final MIndexSpace<E> space;
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    private MIndexFrozenPriorityQueue(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int canonicalId) {
        this.space = Objects.requireNonNull(space, "space");
        this.index = Objects.requireNonNull(index, "index");
        this.canonicalId = canonicalId;
        requireCompatible();
    }

    public static <E> MIndexFrozenPriorityQueue<E> copyOf(
            MIndexPriorityQueue<E> source,
            MIndexCompositeIndex index) {
        Objects.requireNonNull(source, "source");
        return ofOrderedLane(source.space(), index, source.snapshotPriorityLane());
    }

    public static <E> MIndexFrozenPriorityQueue<E> ofOrderedLane(
            MIndexSpace<E> space,
            MIndexCompositeIndex index,
            int[] lane) {
        Objects.requireNonNull(space, "space");
        Objects.requireNonNull(index, "index");
        int[] values = Objects.requireNonNull(lane, "lane").clone();
        if (values.length % INTS_PER_ENTRY != 0) {
            throw new IllegalArgumentException("priority lane must contain triples");
        }
        long previous = Long.MIN_VALUE;
        for (int entry = 0; entry < values.length / INTS_PER_ENTRY; entry++) {
            long priority = priority(values, entry);
            if (entry > 0 && priority < previous) {
                throw new IllegalArgumentException("priority lane is not ordered");
            }
            previous = priority;
            space.requireId(values[entry * INTS_PER_ENTRY + 2]);
        }
        int id = index.intern(
                MIndexCompositeIndex.KIND_PRIORITY_QUEUE,
                space,
                null,
                values);
        return new MIndexFrozenPriorityQueue<>(space, index, id);
    }

    public MIndexSpace<E> space() {
        return space;
    }

    @Override
    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    @Override
    public int canonicalId() {
        return canonicalId;
    }

    @Override
    public int kind() {
        return MIndexCompositeIndex.KIND_PRIORITY_QUEUE;
    }

    @Override
    public int size() {
        return index.length(canonicalId) / INTS_PER_ENTRY;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public E first() {
        return space.value(firstId());
    }

    public int firstId() {
        requireNotEmpty();
        return idAt(0);
    }

    public long firstPriority() {
        requireNotEmpty();
        return priorityAt(0);
    }

    public E get(int position) {
        return space.value(idAt(position));
    }

    public int idAt(int position) {
        requirePosition(position);
        return index.valueAt(canonicalId, position * INTS_PER_ENTRY + 2);
    }

    public long priorityAt(int position) {
        requirePosition(position);
        int offset = position * INTS_PER_ENTRY;
        int high = index.valueAt(canonicalId, offset);
        int low = index.valueAt(canonicalId, offset + 1);
        return ((long) high << 32) | Integer.toUnsignedLong(low);
    }

    public MIndexFrozenPriorityQueue<E> with(E value, long priority) {
        return withId(space.id(value), priority);
    }

    public MIndexFrozenPriorityQueue<E> withId(int id, long priority) {
        space.requireId(id);
        int insertion = upperBoundPriority(priority);
        int[] source = copyLane();
        int[] target = new int[source.length + INTS_PER_ENTRY];
        int offset = insertion * INTS_PER_ENTRY;
        System.arraycopy(source, 0, target, 0, offset);
        target[offset] = (int) (priority >>> 32);
        target[offset + 1] = (int) priority;
        target[offset + 2] = id;
        System.arraycopy(
                source,
                offset,
                target,
                offset + INTS_PER_ENTRY,
                source.length - offset);
        return ofOrderedLane(space, index, target);
    }

    public MIndexFrozenPriorityQueue<E> withoutFirst() {
        requireNotEmpty();
        return ofOrderedLane(
                space,
                index,
                Arrays.copyOfRange(copyLane(), INTS_PER_ENTRY, index.length(canonicalId)));
    }

    public int[] copyLane() {
        return index.copyLane(canonicalId);
    }

    @Override
    public long structuralHash64() {
        return index.structuralHash64(canonicalId);
    }

    @Override
    public long signal64() {
        return index.signal64(canonicalId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexFrozenPriorityQueue<?> that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        long hash = structuralHash64();
        return (int) (hash ^ (hash >>> 32));
    }

    private int upperBoundPriority(long priority) {
        int low = 0;
        int high = size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (priorityAt(middle) <= priority) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static long priority(int[] lane, int entry) {
        int offset = entry * INTS_PER_ENTRY;
        return ((long) lane[offset] << 32) | Integer.toUnsignedLong(lane[offset + 1]);
    }

    private void requirePosition(int position) {
        if (position < 0 || position >= size()) {
            throw new IndexOutOfBoundsException(position);
        }
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("empty MIndexFrozenPriorityQueue");
        }
    }

    private void requireCompatible() {
        if (index.kind(canonicalId) != kind()
                || !index.hasDomains(canonicalId, space, null)
                || index.length(canonicalId) % INTS_PER_ENTRY != 0) {
            throw new IllegalArgumentException(
                    "canonical ID is not a priority queue in this MIndexSpace");
        }
    }
}
