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

import java.util.Objects;
import jdk.m3.collections.M3LazyPrimitiveAddressSpace;
import jdk.m3.collections.M3PrimitiveKind;

/**
 * Fixed-logical-size provider-backed list with bounded payload residency.
 *
 * <p>The logical list remains fully addressable. Only materialized payload is bounded by the
 * underlying CLOCK row owner. No per-index cache object is retained.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/BoundedProviderBackedPrimitiveList.java}
 * blob {@code caca3d8ac388698188c760f8bb56547c11989871} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3BoundedProviderBackedPrimitiveList {
    private final long logicalSize;
    private final M3BoundedProviderBackedPrimitiveRows rows;

    public M3BoundedProviderBackedPrimitiveList(
            long logicalSize,
            int maximumResident,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        this(
                logicalSize,
                M3LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                maximumResident,
                provider,
                laneKinds);
    }

    public M3BoundedProviderBackedPrimitiveList(
            long logicalSize,
            int rootBits,
            int directoryBits,
            int leafBits,
            int maximumResident,
            M3PrimitiveRowProvider provider,
            M3PrimitiveKind... laneKinds) {
        if (logicalSize < 0L) throw new IllegalArgumentException("logicalSize < 0");
        this.logicalSize = logicalSize;
        rows = new M3BoundedProviderBackedPrimitiveRows(
                rootBits,
                directoryBits,
                leafBits,
                maximumResident,
                Objects.requireNonNull(provider, "provider"),
                laneKinds);
        if (logicalSize > rows.logicalCapacity()) {
            throw new IllegalArgumentException(
                    "logicalSize exceeds lazy address capacity: " + logicalSize);
        }
    }

    public long size() { return logicalSize; }
    public boolean isEmpty() { return logicalSize == 0L; }
    public int laneCount() { return rows.laneCount(); }
    public M3PrimitiveKind laneKind(int lane) { return rows.laneKind(lane); }
    public long materializedSize() { return rows.materializedSize(); }
    public int maximumResident() { return rows.maxResidentRows(); }
    public int residentCount() { return rows.residentCount(); }
    public long allocatedPayloadBytes() { return rows.allocatedPayloadBytes(); }
    public long administrativePrimitiveBytes() { return rows.administrativePrimitiveBytes(); }
    public M3ProviderCollectionStats stats() { return rows.stats(); }

    public short flags(long index) {
        checkIndex(index);
        return rows.flags(index);
    }

    public boolean isMaterialized(long index) {
        checkIndex(index);
        return rows.isMaterialized(index);
    }

    public boolean isResident(long index) {
        checkIndex(index);
        return rows.isResident(index);
    }

    public boolean touch(long index) {
        checkIndex(index);
        return rows.touch(index);
    }

    public long getBits(int lane, long index) {
        checkIndex(index);
        return rows.getBits(lane, index);
    }

    public void readRow(long index, long[] target) {
        checkIndex(index);
        rows.readRow(index, target);
    }

    public void setBits(int lane, long index, long bits) {
        checkIndex(index);
        rows.setBits(lane, index, bits);
    }

    public boolean retry(long index) {
        checkIndex(index);
        return rows.retry(index);
    }

    public boolean invalidate(long index) {
        checkIndex(index);
        return rows.invalidate(index);
    }

    public boolean evict(long index) {
        checkIndex(index);
        return rows.evict(index);
    }

    public void pin(long index, boolean value) {
        checkIndex(index);
        rows.pin(index, value);
    }

    public void markDirty(long index, boolean value) {
        checkIndex(index);
        rows.markDirty(index, value);
    }

    public short applicationFlags(long index) {
        checkIndex(index);
        return rows.applicationFlags(index);
    }

    public void applicationFlags(long index, short bits) {
        checkIndex(index);
        rows.applicationFlags(index, bits);
    }

    public void prefetch(long fromInclusive, long toExclusive) {
        checkRange(fromInclusive, toExclusive);
        for (long index = fromInclusive; index < toExclusive; index++) {
            rows.touch(index);
        }
    }

    private void checkIndex(long index) {
        if (index < 0L || index >= logicalSize) {
            throw new IndexOutOfBoundsException(Long.toString(index));
        }
    }

    private void checkRange(long fromInclusive, long toExclusive) {
        if (fromInclusive < 0L
                || fromInclusive > toExclusive
                || toExclusive > logicalSize) {
            throw new IndexOutOfBoundsException(fromInclusive + ".." + toExclusive);
        }
    }
}
