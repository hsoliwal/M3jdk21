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
package jdk.internal.m3.collections.shape;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;
import jdk.internal.m3.collections.range.SegmentedSlotAllocator28;

/**
 * Shared handle lifetime and payload lanes; subclasses move only primitive IDs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/AbstractAddressablePriorityQueue.java}
 * blob {@code 76cf80490b642b53a039febab49d69824ca2789c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class AbstractAddressablePriorityQueue<E> extends ShapeGuard implements ShapeAddressablePriorityQueue<E> {
    private static final Object[] EMPTY = new Object[0];
    private Object[] values = EMPTY;
    private SegmentedSlotAllocator28 slots;
    AbstractAddressablePriorityQueue(CollectionRequirements required) { super(required); }
    @Override public final int size() { return slots == null ? 0 : slots.size(); }
    private int live(long handle) {
        if (slots == null) { throw new IllegalStateException("no live handle"); }
        return slots.requireLiveSlot(handle);
    }
    @Override public final boolean isLive(long handle) { return slots != null && slots.isLive(handle); }
    @Override public final long add(E value, long priority) {
        writable(); element(value); growing(size());
        if (slots == null) { slots = new SegmentedSlotAllocator28(); }
        if (slots.freeSlotCount() == 0 && slots.highWaterMark() == values.length) {
            int capacity = grown(values.length, values.length + 1, required.maximumSize());
            Object[] replacement = Arrays.copyOf(values, capacity);
            reserveIndex(capacity); values = replacement;
        }
        long handle = slots.allocate();
        int id = SegmentedSlotAllocator28.slot(handle);
        values[id] = value; putPriority(id, priority);
        return handle;
    }
    @Override public final E get(long handle) { return read(values, live(handle)); }
    @Override public final E set(long handle, E value) {
        writable(); int id = live(handle); element(value);
        E old = read(values, id); values[id] = value; return old;
    }
    @Override public final long priorityOf(long handle) { return priority(live(handle)); }
    @Override public final long updatePriority(long handle, long priority) {
        writable(); int id = live(handle); long old = priority(id);
        putPriority(id, priority); return old;
    }
    @Override public final void updatePriorities(long[] handles, long[] priorities) {
        writable(); Objects.requireNonNull(handles, "handles"); Objects.requireNonNull(priorities, "priorities");
        if (handles.length != priorities.length) { throw new IllegalArgumentException("lane lengths differ"); }
        for (long handle : handles) { live(handle); }
        for (int i = 0; i < handles.length; i++) { putPriority(SegmentedSlotAllocator28.slot(handles[i]), priorities[i]); }
    }
    @Override public final long firstHandle() { return handle(firstId()); }
    private long handle(int id) { return slots.handleForLiveSlot(id); }
    @Override public final E first() { return read(values, firstId()); }
    @Override public final long firstPriority() { return priority(firstId()); }
    @Override public final E removeFirst() { writable(); return remove(firstHandle()); }
    @Override public final E remove(long handle) {
        writable(); int id = live(handle); E old = read(values, id);
        // Generation exhaustion must be rejected before payload/index mutation.
        slots.release(handle); removeId(id); values[id] = null;
        return old;
    }
    @Override public final void clear() {
        writable();
        walkIds(id -> {
            if (slots.generationAtSlot(id) == -1) { throw new IllegalStateException("slot generation exhausted"); }
        });
        walkIds(id -> { slots.release(handle(id)); values[id] = null; });
        clearIndex();
        // Keep generations and capacity: resetting the allocator would resurrect stale handles.
    }
    @Override public final void forEach(HandlePriorityConsumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { walkIds(id -> action.accept(handle(id), read(values, id), priority(id))); }
        finally { leave(); }
    }
    @Override public final ShapeStorage storage() {
        return new ShapeStorage(values.length, 16L * values.length + (slots == null ? 0 : slots.payloadBytes()),
                (values.length == 0 ? 0 : 4) + (slots == null ? 0 : slots.allocatedBlockCount()), 2 * values.length);
    }
    abstract void reserveIndex(int capacity);
    abstract void putPriority(int id, long priority);
    abstract long priority(int id);
    abstract int firstId();
    abstract void removeId(int id);
    abstract void walkIds(IntConsumer action);
    abstract void clearIndex();
}
