// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Doubly linked primitive-long list over stable segmented handles.
 *
 * <p>Each element is three primitive long lanes: value, next handle and previous handle.
 * There is no node object per element. Known-handle insertion/removal is O(1); traversal follows
 * stable generation-checked handles. Handle zero is the null link.
 */
public final class M3LongLinkedList28 {
    /** Creates an empty instance with no populated storage. */
    public M3LongLinkedList28() {}

    public static final long NULL_HANDLE = 0L;

    private static final int VALUE = 0;
    private static final int NEXT = 1;
    private static final int PREVIOUS = 2;

    private final M3LongLaneStore28 rows = new M3LongLaneStore28(3);
    private long head;
    private long tail;

    public int size() {
        return rows.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public long firstHandle() {
        if (head == NULL_HANDLE) {
            throw new NoSuchElementException();
        }
        return head;
    }

    public long lastHandle() {
        if (tail == NULL_HANDLE) {
            throw new NoSuchElementException();
        }
        return tail;
    }

    public long firstLong() {
        return value(firstHandle());
    }

    public long lastLong() {
        return value(lastHandle());
    }

    public long addFirst(long value) {
        long handle = rows.allocate3(value, head, NULL_HANDLE);
        if (head == NULL_HANDLE) {
            tail = handle;
        } else {
            rows.set(head, PREVIOUS, handle);
        }
        head = handle;
        return handle;
    }

    public long addLast(long value) {
        long handle = rows.allocate3(value, NULL_HANDLE, tail);
        if (tail == NULL_HANDLE) {
            head = handle;
        } else {
            rows.set(tail, NEXT, handle);
        }
        tail = handle;
        return handle;
    }

    public long insertAfter(long existing, long value) {
        requireLive(existing);
        long following = rows.get(existing, NEXT);
        long handle = rows.allocate3(value, following, existing);
        rows.set(existing, NEXT, handle);
        if (following == NULL_HANDLE) {
            tail = handle;
        } else {
            rows.set(following, PREVIOUS, handle);
        }
        return handle;
    }

    public long insertBefore(long existing, long value) {
        requireLive(existing);
        long previous = rows.get(existing, PREVIOUS);
        long handle = rows.allocate3(value, existing, previous);
        rows.set(existing, PREVIOUS, handle);
        if (previous == NULL_HANDLE) {
            head = handle;
        } else {
            rows.set(previous, NEXT, handle);
        }
        return handle;
    }

    public long value(long handle) {
        return rows.get(handle, VALUE);
    }

    public long set(long handle, long value) {
        long previous = rows.get(handle, VALUE);
        rows.set(handle, VALUE, value);
        return previous;
    }

    public long nextHandle(long handle) {
        return rows.get(handle, NEXT);
    }

    public long previousHandle(long handle) {
        return rows.get(handle, PREVIOUS);
    }

    public boolean isLive(long handle) {
        return rows.isLive(handle);
    }

    public long remove(long handle) {
        requireLive(handle);
        long value = rows.get(handle, VALUE);
        long previous = rows.get(handle, PREVIOUS);
        long following = rows.get(handle, NEXT);

        if (previous == NULL_HANDLE) {
            head = following;
        } else {
            rows.set(previous, NEXT, following);
        }
        if (following == NULL_HANDLE) {
            tail = previous;
        } else {
            rows.set(following, PREVIOUS, previous);
        }

        rows.release(handle);
        return value;
    }

    public long removeFirstLong() {
        return remove(firstHandle());
    }

    public long removeLastLong() {
        return remove(lastHandle());
    }

    public void clear() {
        long handle = head;
        while (handle != NULL_HANDLE) {
            long following = rows.get(handle, NEXT);
            rows.release(handle);
            handle = following;
        }
        head = NULL_HANDLE;
        tail = NULL_HANDLE;
    }

    public long[] toArray() {
        long[] result = new long[size()];
        int index = 0;
        long handle = head;
        while (handle != NULL_HANDLE) {
            if (index == result.length) {
                throw new IllegalStateException("linked-list cycle or size corruption");
            }
            result[index++] = rows.get(handle, VALUE);
            handle = rows.get(handle, NEXT);
        }
        if (index != result.length) {
            throw new IllegalStateException("linked-list size/link corruption");
        }
        return result;
    }

    public void forEachLong(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int visited = 0;
        for (long handle = head; handle != NULL_HANDLE; handle = rows.get(handle, NEXT)) {
            if (visited++ >= size()) {
                throw new IllegalStateException("linked-list cycle");
            }
            consumer.accept(rows.get(handle, VALUE));
        }
        if (visited != size()) {
            throw new IllegalStateException("linked-list size/link corruption");
        }
    }

    public int allocatedDataBlockCount() {
        return rows.allocatedDataBlockCount();
    }

    public long payloadBytes() {
        return rows.payloadBytes();
    }

    private void requireLive(long handle) {
        if (!rows.isLive(handle)) {
            throw new IllegalStateException("stale or foreign list handle");
        }
    }
}
