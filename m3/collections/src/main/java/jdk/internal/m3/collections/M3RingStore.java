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

import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Shared circular addressing and end mechanics for deque and window shapes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/RingStore.java}
 * blob {@code 69d7f657f2422d8de39405169a04e584700288a1} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class M3RingStore<E> {
    final int maximum;
    int size;
    int head;
    M3RingStore(int maximum) { this.maximum = maximum; }
    abstract int capacity();
    abstract E readSlot(int slot);
    abstract void writeSlot(int slot, E value);
    abstract void grow();
    abstract void releaseVacated(int slot);
    abstract void reset();
    abstract M3ShapeStorage storage();
    final int slot(int offset) { return (head + offset) % capacity(); }
    final void ensureSpace() { if (size == capacity()) { grow(); } }
    final E get(int index) { M3ShapeGuard.index(index, size); return readSlot(slot(index)); }
    final E first() { requireValue(); return readSlot(head); }
    final E last() { requireValue(); return readSlot(slot(size - 1)); }
    private void requireValue() { if (size == 0) { throw new NoSuchElementException(); } }
    final void addFirst(E value) {
        ensureSpace();
        int target = (head + capacity() - 1) % capacity();
        writeSlot(target, value); head = target; size++;
    }
    final void addLast(E value) { ensureSpace(); writeSlot(slot(size), value); size++; }
    final E removeFirst() {
        E old = first(); int removed = head;
        writeSlot(removed, null); size--; head = size == 0 ? 0 : (head + 1) % capacity();
        releaseVacated(removed); return old;
    }
    final E removeLast() {
        E old = last(); int removed = slot(size - 1);
        writeSlot(removed, null); size--; if (size == 0) { head = 0; }
        releaseVacated(removed); return old;
    }
    final boolean appendWindow(E value) {
        if (maximum == 0) { throw new IllegalStateException("zero-capacity window"); }
        boolean evicted = size == maximum;
        if (evicted && size == capacity()) {
            writeSlot(head, value); head = (head + 1) % capacity();
        } else {
            // Allocate the new leaf before removing the old value on segmented growth.
            addLast(value); if (evicted) { removeFirst(); }
        }
        return evicted;
    }
    final void walk(Consumer<? super E> action) {
        for (int i = 0; i < size; i++) { action.accept(readSlot(slot(i))); }
    }
    final void clear() { reset(); size = 0; head = 0; }
}
