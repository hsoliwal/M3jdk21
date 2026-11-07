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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Intrusive doubly-linked list whose links and membership token live inside arena record lanes.
 *
 * <p>One set of next/previous/owner lanes represents one intrusive membership slot per record. A
 * node must be removed before it is freed from the arena.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntrusiveHandleList.java}
 * blob {@code bf14f94235af08eb694505671a586147c88351e0} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIntrusiveHandleList {
    private static final AtomicLong NEXT_OWNER = new AtomicLong(1L);

    private final M3MatRecordArena arena;
    private final int nextLane;
    private final int previousLane;
    private final int ownerLane;
    private final long ownerToken;
    private long first;
    private long last;
    private int size;

    public M3MatIntrusiveHandleList(
            M3MatRecordArena arena, int nextLane, int previousLane, int ownerLane) {
        this.arena = java.util.Objects.requireNonNull(arena, "arena");
        if (nextLane == previousLane || nextLane == ownerLane || previousLane == ownerLane) {
            throw new IllegalArgumentException("intrusive lanes must be distinct");
        }
        java.util.Objects.checkIndex(nextLane, arena.lanes());
        java.util.Objects.checkIndex(previousLane, arena.lanes());
        java.util.Objects.checkIndex(ownerLane, arena.lanes());
        this.nextLane = nextLane;
        this.previousLane = previousLane;
        this.ownerLane = ownerLane;
        long token = NEXT_OWNER.getAndIncrement();
        if (token <= 0L) throw new IllegalStateException("intrusive owner token space exhausted");
        this.ownerToken = token;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public long first() { return first; }
    public long last() { return last; }
    public long ownerToken() { return ownerToken; }

    public boolean contains(long handle) {
        return arena.isAlive(handle) && arena.get(handle, ownerLane) == ownerToken;
    }

    public void addFirst(long handle) {
        requireUnlinked(handle);
        arena.set(handle, ownerLane, ownerToken);
        arena.set(handle, previousLane, M3MatPointerHandle.NULL);
        arena.set(handle, nextLane, first);
        if (first != M3MatPointerHandle.NULL) {
            requireMember(first);
            arena.set(first, previousLane, handle);
        } else {
            last = handle;
        }
        first = handle;
        size = Math.addExact(size, 1);
    }

    public void addLast(long handle) {
        requireUnlinked(handle);
        arena.set(handle, ownerLane, ownerToken);
        arena.set(handle, nextLane, M3MatPointerHandle.NULL);
        arena.set(handle, previousLane, last);
        if (last != M3MatPointerHandle.NULL) {
            requireMember(last);
            arena.set(last, nextLane, handle);
        } else {
            first = handle;
        }
        last = handle;
        size = Math.addExact(size, 1);
    }

    public long next(long handle) {
        requireMember(handle);
        return arena.get(handle, nextLane);
    }

    public long previous(long handle) {
        requireMember(handle);
        return arena.get(handle, previousLane);
    }

    public void remove(long handle) {
        requireMember(handle);
        long previous = arena.get(handle, previousLane);
        long next = arena.get(handle, nextLane);

        if (previous != M3MatPointerHandle.NULL) {
            requireMember(previous);
            arena.set(previous, nextLane, next);
        } else {
            first = next;
        }

        if (next != M3MatPointerHandle.NULL) {
            requireMember(next);
            arena.set(next, previousLane, previous);
        } else {
            last = previous;
        }

        arena.set(handle, nextLane, M3MatPointerHandle.NULL);
        arena.set(handle, previousLane, M3MatPointerHandle.NULL);
        arena.set(handle, ownerLane, 0L);
        size--;
        if (size == 0 && (first != M3MatPointerHandle.NULL || last != M3MatPointerHandle.NULL)) {
            throw new IllegalStateException("intrusive list boundary drift");
        }
    }

    public void moveToFront(long handle) {
        requireMember(handle);
        if (handle == first) return;
        remove(handle);
        addFirst(handle);
    }

    public void unlinkAndFree(long handle) {
        remove(handle);
        arena.free(handle);
    }

    public void clear() {
        while (first != M3MatPointerHandle.NULL) remove(first);
    }

    public long[] toArray() {
        long[] output = new long[size];
        long cursor = first;
        int index = 0;
        while (cursor != M3MatPointerHandle.NULL) {
            if (index >= output.length) {
                throw new IllegalStateException("intrusive list cycle or size drift");
            }
            requireMember(cursor);
            output[index++] = cursor;
            cursor = arena.get(cursor, nextLane);
        }
        if (index != output.length || (size != 0 && output[output.length - 1] != last)) {
            throw new IllegalStateException("intrusive list size/boundary drift");
        }
        return output;
    }

    private void requireUnlinked(long handle) {
        if (!arena.isAlive(handle)) throw new IllegalStateException("stale arena handle");
        if (arena.get(handle, ownerLane) != 0L
                || arena.get(handle, nextLane) != M3MatPointerHandle.NULL
                || arena.get(handle, previousLane) != M3MatPointerHandle.NULL) {
            throw new IllegalArgumentException("record already linked");
        }
    }

    private void requireMember(long handle) {
        if (!arena.isAlive(handle) || arena.get(handle, ownerLane) != ownerToken) {
            throw new IllegalArgumentException("record is not a member of this intrusive list");
        }
    }
}
