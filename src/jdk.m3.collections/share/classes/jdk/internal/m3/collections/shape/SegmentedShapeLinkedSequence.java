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
package jdk.internal.m3.collections.shape;

import java.util.Objects;
import java.util.function.Consumer;
import jdk.internal.m3.collections.range.SegmentedIntLane28;
import jdk.internal.m3.collections.range.SegmentedSlotAllocator28;

/**
 * Reference payload plus primitive links, reusing Synexia's existing generation-checked allocator.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SegmentedShapeLinkedSequence.java}
 * blob {@code 973b6e840c871046b9f7978d26ae9b972d7437e6} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SegmentedShapeLinkedSequence<E> extends ShapeGuard implements ShapeLinkedSequence<E> {
    private SegmentedSlotAllocator28 slots;
    private SegmentedObjectLane28<E> values;
    private SegmentedIntLane28 next;
    private SegmentedIntLane28 previous;
    private int head = -1;
    private int tail = -1;
    SegmentedShapeLinkedSequence(CollectionRequirements required) { super(required); }
    @Override public int size() { return slots == null ? 0 : slots.size(); }
    private long handle(int slot) { return slot < 0 ? 0 : slots.handleForLiveSlot(slot); }
    private int live(long handle) {
        if (slots == null) { throw new IllegalStateException("no live handle"); }
        return slots.requireLiveSlot(handle);
    }
    @Override public long firstHandle() { return handle(head); }
    @Override public long lastHandle() { return handle(tail); }
    @Override public long addFirst(E value) { return insert(-1, head, value); }
    @Override public long addLast(E value) { return insert(tail, -1, value); }
    @Override public long insertAfter(long handle, E value) {
        writable(); int at = live(handle); return insert(at, next.get(at) - 1, value);
    }
    @Override public long insertBefore(long handle, E value) {
        writable(); int at = live(handle); return insert(previous.get(at) - 1, at, value);
    }
    private long insert(int before, int after, E value) {
        writable(); element(value); growing(size());
        if (slots == null) {
            slots = new SegmentedSlotAllocator28(); values = new SegmentedObjectLane28<>();
            next = new SegmentedIntLane28(); previous = new SegmentedIntLane28();
        }
        long handle = slots.allocate();
        int slot = slots.requireLiveSlot(handle);
        values.set(slot, value); next.set(slot, after + 1); previous.set(slot, before + 1);
        if (before < 0) { head = slot; } else { next.set(before, slot + 1); }
        if (after < 0) { tail = slot; } else { previous.set(after, slot + 1); }
        return handle;
    }
    @Override public E get(long handle) { return valuesAt(live(handle)); }
    private E valuesAt(int slot) { return values.get(slot); }
    @Override public E set(long handle, E value) {
        writable(); int slot = live(handle); element(value);
        E old = values.get(slot); values.set(slot, value); return old;
    }
    @Override public long next(long handle) { return handle(next.get(live(handle)) - 1); }
    @Override public long previous(long handle) { return handle(previous.get(live(handle)) - 1); }
    @Override public boolean isLive(long handle) { return slots != null && slots.isLive(handle); }
    @Override public E remove(long handle) {
        writable(); int slot = live(handle);
        int before = previous.get(slot) - 1;
        int after = next.get(slot) - 1;
        E old = values.get(slot);
        // Release can refuse generation wrap. Do it before modifying links or payload.
        slots.release(handle);
        if (before < 0) { head = after; } else { next.set(before, after + 1); }
        if (after < 0) { tail = before; } else { previous.set(after, before + 1); }
        values.set(slot, null); next.clear(slot); previous.clear(slot);
        return old;
    }
    @Override public void clear() {
        writable();
        if (slots == null) { return; }
        for (int slot = head; slot >= 0; slot = next.get(slot) - 1) {
            if (slots.generationAtSlot(slot) == -1) { throw new IllegalStateException("slot generation exhausted"); }
        }
        while (head >= 0) { remove(handle(head)); }
        // Keep allocator generations; releasing them would resurrect old numeric handles.
        values = new SegmentedObjectLane28<>(); next = new SegmentedIntLane28(); previous = new SegmentedIntLane28();
    }
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try {
            for (int slot = head; slot >= 0; slot = next.get(slot) - 1) { action.accept(values.get(slot)); }
        } finally { leave(); }
    }
    @Override public ShapeStorage storage() {
        if (slots == null) { return new ShapeStorage(0, 0, 0, 0); }
        return new ShapeStorage(values.referenceSlots(), slots.payloadBytes() + next.payloadBytes() + previous.payloadBytes(),
                values.blocks() + slots.allocatedBlockCount() + next.allocatedBlockCount() + previous.allocatedBlockCount(), 0);
    }
}
