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
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Sparse struct-of-arrays table over stable logical row ids.
 *
 * <p>All columns share identical lazy address geometry. One sparse bit address
 * space distinguishes absent rows from rows whose primitive values are all zero.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LazyPrimitiveSoaTable.java}
 * blob {@code 40195c82df30a2c972834c310f6c6d31bfd6e839} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LazyPrimitiveSoaTable {
    private final M3PrimitiveKind[] laneKinds;
    private final M3LazyPrimitiveAddressSpace[] lanes;
    private final M3LazyBitAddressSpace present;

    public M3LazyPrimitiveSoaTable(M3PrimitiveKind... laneKinds) {
        this(
                M3LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                laneKinds);
    }

    public M3LazyPrimitiveSoaTable(
            int rootBits,
            int directoryBits,
            int leafBits,
            M3PrimitiveKind... laneKinds) {
        Objects.requireNonNull(laneKinds, "laneKinds");
        if (laneKinds.length == 0) throw new IllegalArgumentException("at least one lane required");
        this.laneKinds = laneKinds.clone();
        lanes = new M3LazyPrimitiveAddressSpace[laneKinds.length];
        for (int lane = 0; lane < laneKinds.length; lane++) {
            M3PrimitiveKind kind = Objects.requireNonNull(laneKinds[lane], "lane kind");
            lanes[lane] =
                    new M3LazyPrimitiveAddressSpace(kind, rootBits, directoryBits, leafBits);
        }
        present = new M3LazyBitAddressSpace(rootBits, directoryBits, leafBits);
    }

    public int laneCount() { return lanes.length; }
    public M3PrimitiveKind laneKind(int lane) { return laneKinds[checkLane(lane)]; }
    public long logicalCapacity() { return present.logicalCapacity(); }
    public long size() { return present.cardinality(); }

    public long allocatedPayloadBytes() {
        long bytes = present.allocatedPayloadBytes();
        for (M3LazyPrimitiveAddressSpace lane : lanes) bytes += lane.allocatedPayloadBytes();
        return bytes;
    }

    public long directoryReferenceSlots() {
        long slots = present.directoryReferenceSlots();
        for (M3LazyPrimitiveAddressSpace lane : lanes) slots += lane.directoryReferenceSlots();
        return slots;
    }

    public boolean contains(long rowId) {
        return present.get(rowId);
    }

    /** Inserts or replaces a complete primitive row. Returns true when newly inserted. */
    public boolean put(long rowId, long... rowBits) {
        Objects.requireNonNull(rowBits, "rowBits");
        if (rowBits.length != lanes.length) {
            throw new IllegalArgumentException("lane count mismatch");
        }
        boolean added = present.set(rowId);
        for (int lane = 0; lane < lanes.length; lane++) {
            lanes[lane].setBits(rowId, rowBits[lane]);
        }
        return added;
    }

    /**
     * Inserts/replaces one row from a caller-owned flat primitive buffer without slicing or
     * allocating a per-row array.
     */
    public boolean putFrom(long rowId, long[] rowBits, int offset) {
        Objects.requireNonNull(rowBits, "rowBits");
        if (offset < 0 || offset > rowBits.length - lanes.length) {
            throw new IndexOutOfBoundsException("offset=" + offset);
        }
        boolean added = present.set(rowId);
        for (int lane = 0; lane < lanes.length; lane++) {
            lanes[lane].setBits(rowId, rowBits[offset + lane]);
        }
        return added;
    }

    public long getBits(int lane, long rowId, long defaultBits) {
        int checkedLane = checkLane(lane);
        return present.get(rowId) ? lanes[checkedLane].getBits(rowId) : defaultBits;
    }

    public void setBits(int lane, long rowId, long bits) {
        int checkedLane = checkLane(lane);
        if (!present.get(rowId)) throw new IllegalArgumentException("row absent: " + rowId);
        lanes[checkedLane].setBits(rowId, bits);
    }

    public void readRow(long rowId, long[] target) {
        Objects.requireNonNull(target, "target");
        if (target.length < lanes.length) throw new IllegalArgumentException("target too small");
        if (!present.get(rowId)) throw new IllegalArgumentException("row absent: " + rowId);
        for (int lane = 0; lane < lanes.length; lane++) {
            target[lane] = lanes[lane].getBits(rowId);
        }
    }

    public boolean remove(long rowId) {
        if (!present.clear(rowId)) return false;
        for (M3LazyPrimitiveAddressSpace lane : lanes) lane.clearBits(rowId);
        return true;
    }

    public void forEachRowId(LongConsumer consumer) {
        present.forEachSetBit(consumer);
    }

    public void clear() {
        present.clear();
        for (M3LazyPrimitiveAddressSpace lane : lanes) lane.clear();
    }

    public int allocatedLeafCount(int lane) {
        return lanes[checkLane(lane)].allocatedLeafCount();
    }

    public int allocatedPresenceLeafCount() {
        return present.allocatedLeafCount();
    }

    private int checkLane(int lane) {
        if (lane < 0 || lane >= lanes.length) throw new IndexOutOfBoundsException(lane);
        return lane;
    }
}
