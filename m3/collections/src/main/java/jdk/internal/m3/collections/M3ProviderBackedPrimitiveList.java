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
import jdk.m3.collections.PrimitiveKind;

/**
 * Long-indexed provider-backed primitive list over the shared lazy row owner.
 *
 * <p>Index equals logical row id. No list node or wrapper object is retained per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderBackedPrimitiveList.java}
 * blob {@code acbe19194a9e31e4f3587eed6d48c37d3a9b00c6} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ProviderBackedPrimitiveList {
    private final long logicalSize;
    private final M3ProviderBackedPrimitiveRows rows;

    public M3ProviderBackedPrimitiveList(
            long logicalSize,
            M3PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        this(
                logicalSize,
                M3LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                provider,
                laneKinds);
    }

    public M3ProviderBackedPrimitiveList(
            long logicalSize,
            int rootBits,
            int directoryBits,
            int leafBits,
            M3PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        if (logicalSize < 0L) throw new IllegalArgumentException("logicalSize < 0");
        rows = new M3ProviderBackedPrimitiveRows(
                rootBits,
                directoryBits,
                leafBits,
                Objects.requireNonNull(provider, "provider"),
                laneKinds);
        if (logicalSize > rows.logicalCapacity()) {
            throw new IllegalArgumentException(
                    "logicalSize exceeds address space: " + logicalSize);
        }
        this.logicalSize = logicalSize;
    }

    public long size() { return logicalSize; }
    public boolean isEmpty() { return logicalSize == 0L; }
    public int laneCount() { return rows.laneCount(); }
    public PrimitiveKind laneKind(int lane) { return rows.laneKind(lane); }
    public long materializedSize() { return rows.materializedSize(); }
    public long allocatedPayloadBytes() { return rows.allocatedPayloadBytes(); }

    public short flags(long index) {
        checkIndex(index);
        return rows.flags(index);
    }

    public boolean isMaterialized(long index) {
        checkIndex(index);
        return rows.isMaterialized(index);
    }

    /** Touches/materializes one logical list element. */
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

    /** Existing touch semantics are preserved: every requested index is logically touched. */
    public void prefetch(long fromInclusive, long toExclusive) {
        checkRange(fromInclusive, toExclusive);
        for (long index = fromInclusive; index < toExclusive; index++) {
            rows.touch(index);
        }
    }

    /**
     * Provider materialization ahead of use without logical touch-observer events.
     *
     * <p>Requests are internally chunked so collection-owned batch scratch remains bounded.
     *
     * @return number of newly materialized indexes
     */
    public long prefetchMaterialize(long fromInclusive, long toExclusive) {
        checkRange(fromInclusive, toExclusive);
        long materialized = 0L;
        long cursor = fromInclusive;
        while (cursor < toExclusive) {
            int count = (int) Math.min(4096L, toExclusive - cursor);
            materialized += rows.prefetchMaterialize(cursor, count);
            cursor += count;
        }
        return materialized;
    }

    M3ProviderBackedPrimitiveRows rows() {
        return rows;
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
            throw new IndexOutOfBoundsException(
                    fromInclusive + ".." + toExclusive);
        }
    }
}
