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
package jdk.internal.m3.collections.packed;

import jdk.internal.m3.collections.bits.PackedLongBitSet;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.LongPredicate;

/**
 * Live bit selection over one primitive lane.
 *
 * <p>The predicate is evaluated before insertion/update mutation. A throwing predicate leaves
 * the store unchanged. Selection state is one bit per physical row plus collection-level state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneSelection.java}
 * blob {@code ba717063330bbfe94acdd0119ff0f90a53734b01} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections lane batch (v6, after U2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLaneSelection implements PackedLongLaneIndex {
    private final PackedLongLaneStore store;
    private final int lane;
    private final LongPredicate predicate;
    private final PackedLongBitSet selected;
    private int pendingRow = -1;
    private boolean pendingValue;

    PackedLongLaneSelection(
            PackedLongLaneStore store,
            int lane,
            LongPredicate predicate) {
        this.store = Objects.requireNonNull(store, "store");
        this.lane = lane;
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        selected = new PackedLongBitSet(store.capacity());
    }

    public int lane() {
        return lane;
    }

    public boolean containsRow(int row) {
        Objects.checkIndex(row, store.physicalSize());
        return selected.get(row);
    }

    public int size() {
        return selected.cardinality();
    }

    public int nextRow(int fromInclusive) {
        if (fromInclusive < 0) {
            throw new IndexOutOfBoundsException("fromInclusive");
        }
        if (fromInclusive >= store.physicalSize()) {
            return -1;
        }
        int row = selected.nextSetBit(fromInclusive);
        return row >= store.physicalSize() ? -1 : row;
    }

    public int[] rowsToArray() {
        int[] rows = new int[size()];
        int write = 0;
        for (int row = nextRow(0); row >= 0; row = nextRow(row + 1)) {
            rows[write++] = row;
        }
        return rows;
    }

    public void forEachRow(IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int row = nextRow(0); row >= 0; row = nextRow(row + 1)) {
            consumer.accept(row);
        }
    }

    /** Re-evaluates the current store without changing payload rows. */
    public void rebuild() {
        selected.clearAll();
        for (int row = 0; row < store.physicalSize(); row++) {
            selected.set(
                    row,
                    predicate.test(store.valueAtUnchecked(row, lane)));
        }
    }

    @Override
    public void ensureRowCapacity(int capacity) {
        // PackedLongBitSet grows on demand when a selected high row appears.
    }

    @Override
    public void validateInsert(int row) {
        pendingRow = row;
        pendingValue = predicate.test(store.valueAtUnchecked(row, lane));
    }

    @Override
    public void insert(int row) {
        if (pendingRow != row) {
            throw new IllegalStateException("selection insert was not validated");
        }
        selected.set(row, pendingValue);
        pendingRow = -1;
    }

    @Override
    public void validateUpdate(int row, int updatedLane, long newValue) {
        if (updatedLane == lane) {
            pendingRow = row;
            pendingValue = predicate.test(newValue);
        }
    }

    @Override
    public void beforeUpdate(int row, int updatedLane) {
        // New selection was already validated; payload has not changed yet.
    }

    @Override
    public void afterUpdate(int row, int updatedLane) {
        if (updatedLane == lane) {
            if (pendingRow != row) {
                throw new IllegalStateException(
                        "selection update was not validated");
            }
            selected.set(row, pendingValue);
            pendingRow = -1;
        }
    }

    @Override
    public void remove(int row) {
        selected.clear(row);
    }

    @Override
    public void relocate(int fromRow, int toRow) {
        boolean value = selected.get(fromRow);
        selected.set(toRow, value);
        selected.clear(fromRow);
    }

    @Override
    public void clear() {
        selected.clearAll();
        pendingRow = -1;
    }

    @Override
    public long payloadBytes() {
        return selected.capacityBits() >>> 3;
    }
}
