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

import jdk.internal.m3.collections.range.SegmentedAddress28;


/**
 * Circular reference lane with reclamation of leaves no longer intersecting live positions.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SegmentedRingStore.java}
 * blob {@code ab40312233cf43e12c3670661aacc14c062dcfd6} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SegmentedRingStore<E> extends RingStore<E> {
    private SegmentedObjectLane28<E> values = new SegmentedObjectLane28<>();
    SegmentedRingStore(int maximum) { super(maximum); }
    @Override int capacity() { return 1 << 28; }
    @Override E readSlot(int slot) { return values.get(slot); }
    @Override void writeSlot(int slot, E value) { values.set(slot, value); }
    @Override void grow() { throw new IllegalStateException("28-bit ring is full"); }
    @Override void releaseVacated(int slot) {
        int block = slot / SegmentedAddress28.VALUES_PER_BLOCK;
        if (size == 0 || (head / SegmentedAddress28.VALUES_PER_BLOCK != block
                && slot(size - 1) / SegmentedAddress28.VALUES_PER_BLOCK != block)) {
            values.releaseBlockContaining(slot);
        }
    }
    @Override void reset() { values = new SegmentedObjectLane28<>(); }
    @Override ShapeStorage storage() { return new ShapeStorage(values.referenceSlots(), 0, values.blocks(), 0); }
}
