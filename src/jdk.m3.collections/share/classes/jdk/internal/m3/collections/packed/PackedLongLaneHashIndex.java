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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;
import java.util.Objects;

/**
 * Unique one- or two-lane hash index storing only primitive row ordinals.
 *
 * <p>Keys stay in the shared lane store. Expected lookup is O(1); collision worst case is linear.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongLaneHashIndex.java}
 * blob {@code 6d9f6c2fa94ce7efd83d7c1407fc0f0512b5543a} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections lane batch (v6, after U2): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongLaneHashIndex implements PackedLongLaneIndex {
    private static final int EMPTY = 0;

    private final PackedLongLaneStore store;
    private final int firstLane;
    private final int secondLane;
    private int[] slots;
    private int size;
    private int threshold;

    PackedLongLaneHashIndex(
            PackedLongLaneStore store,
            int firstLane,
            int secondLane) {
        this.store = Objects.requireNonNull(store, "store");
        this.firstLane = firstLane;
        this.secondLane = secondLane;
        slots = new int[PackedSupport.tableSizeForExpected(
                Math.max(1, store.physicalSize()))];
        threshold = threshold(slots.length);
    }

    public boolean pairKey() {
        return secondLane >= 0;
    }

    public int firstLane() {
        return firstLane;
    }

    public int secondLane() {
        return secondLane;
    }

    public int size() {
        return size;
    }

    public int rowOf(long key) {
        if (pairKey()) {
            throw new IllegalStateException("index requires a two-lane key");
        }
        return find(key, 0L);
    }

    public int rowOf(long first, long second) {
        if (!pairKey()) {
            throw new IllegalStateException("index has a one-lane key");
        }
        return find(first, second);
    }

    public boolean contains(long key) {
        return rowOf(key) >= 0;
    }

    public boolean contains(long first, long second) {
        return rowOf(first, second) >= 0;
    }

    @Override
    public void ensureRowCapacity(int capacity) {
        // Row capacity is not mirrored; this index stores only live row ordinals.
    }

    @Override
    public void validateInsert(int row) {
        long first = store.valueAtUnchecked(row, firstLane);
        long second = pairKey() ? store.valueAtUnchecked(row, secondLane) : 0L;
        int existing = find(first, second);
        if (existing >= 0 && existing != row) {
            throw duplicate(first, second);
        }
    }

    @Override
    public void insert(int row) {
        ensureTable(Math.addExact(size, 1));
        place(row, slots);
        size++;
    }

    @Override
    public void validateUpdate(int row, int lane, long newValue) {
        if (!affects(lane)) {
            return;
        }
        long first = lane == firstLane
                ? newValue : store.valueAtUnchecked(row, firstLane);
        long second = pairKey()
                ? (lane == secondLane
                        ? newValue : store.valueAtUnchecked(row, secondLane))
                : 0L;
        int existing = find(first, second);
        if (existing >= 0 && existing != row) {
            throw duplicate(first, second);
        }
    }

    @Override
    public void beforeUpdate(int row, int lane) {
        if (affects(lane)) {
            remove(row);
        }
    }

    @Override
    public void afterUpdate(int row, int lane) {
        if (affects(lane)) {
            insert(row);
        }
    }

    @Override
    public void remove(int row) {
        int slot = findSlotForRow(row);
        if (slot < 0) {
            throw new IllegalStateException("row missing from hash index: " + row);
        }
        deleteSlot(slot);
        size--;
    }

    @Override
    public void relocate(int fromRow, int toRow) {
        int slot = findSlotForRow(fromRow);
        if (slot < 0) {
            throw new IllegalStateException(
                    "relocated row missing from hash index: " + fromRow);
        }
        slots[slot] = toRow + 1;
    }

    @Override
    public void clear() {
        Arrays.fill(slots, EMPTY);
        size = 0;
    }

    @Override
    public long payloadBytes() {
        return 4L * slots.length;
    }

    private boolean affects(int lane) {
        return lane == firstLane || lane == secondLane;
    }

    private int find(long first, long second) {
        int mask = slots.length - 1;
        int slot = hash(first, second) & mask;
        while (true) {
            int encoded = slots[slot];
            if (encoded == EMPTY) {
                return -1;
            }
            int row = encoded - 1;
            if (matches(row, first, second)) {
                return row;
            }
            slot = (slot + 1) & mask;
        }
    }

    private boolean matches(int row, long first, long second) {
        return store.valueAtUnchecked(row, firstLane) == first
                && (!pairKey()
                || store.valueAtUnchecked(row, secondLane) == second);
    }

    private int findSlotForRow(int row) {
        long first = store.valueAtUnchecked(row, firstLane);
        long second = pairKey() ? store.valueAtUnchecked(row, secondLane) : 0L;
        int mask = slots.length - 1;
        int slot = hash(first, second) & mask;
        while (true) {
            int encoded = slots[slot];
            if (encoded == EMPTY) {
                return -1;
            }
            if (encoded - 1 == row) {
                return slot;
            }
            slot = (slot + 1) & mask;
        }
    }

    private void ensureTable(int required) {
        if (required <= threshold) {
            return;
        }
        int capacity = PackedSupport.nextTableCapacity(slots.length);
        int[] replacement = new int[capacity];
        for (int encoded : slots) {
            if (encoded != EMPTY) {
                place(encoded - 1, replacement);
            }
        }
        slots = replacement;
        threshold = threshold(capacity);
    }

    private void place(int row, int[] target) {
        long first = store.valueAtUnchecked(row, firstLane);
        long second = pairKey() ? store.valueAtUnchecked(row, secondLane) : 0L;
        int mask = target.length - 1;
        int slot = hash(first, second) & mask;
        while (target[slot] != EMPTY) {
            slot = (slot + 1) & mask;
        }
        target[slot] = row + 1;
    }

    private void deleteSlot(int gap) {
        int mask = slots.length - 1;
        int scan = (gap + 1) & mask;
        while (slots[scan] != EMPTY) {
            int row = slots[scan] - 1;
            long first = store.valueAtUnchecked(row, firstLane);
            long second = pairKey()
                    ? store.valueAtUnchecked(row, secondLane) : 0L;
            int home = hash(first, second) & mask;
            if (((scan - home) & mask) >= ((scan - gap) & mask)) {
                slots[gap] = slots[scan];
                gap = scan;
            }
            scan = (scan + 1) & mask;
        }
        slots[gap] = EMPTY;
    }

    private int hash(long first, long second) {
        int hash = PackedSupport.mix(first);
        if (pairKey()) {
            hash ^= Integer.rotateLeft(PackedSupport.mix(second), 13);
            hash *= 0x9e3779b9;
            hash ^= hash >>> 16;
        }
        return hash;
    }

    private IllegalArgumentException duplicate(long first, long second) {
        return pairKey()
                ? new IllegalArgumentException(
                        "duplicate lane key: (" + first + "," + second + ")")
                : new IllegalArgumentException("duplicate lane key: " + first);
    }

    private static int threshold(int capacity) {
        return Math.max(
                1,
                (int) (capacity * PackedSupport.DEFAULT_LOAD_FACTOR));
    }
}
