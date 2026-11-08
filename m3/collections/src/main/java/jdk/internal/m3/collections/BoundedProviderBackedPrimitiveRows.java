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
import jdk.m3.collections.LazyPrimitiveAddressSpace;
import jdk.m3.collections.PrimitiveKind;
import jdk.internal.m3.collections.bits.LazyBitAddressSpace;

/**
 * Provider-backed primitive rows with bounded steady-state residency.
 *
 * <p>The existing {@link ProviderBackedPrimitiveRows} remains the semantic owner for loading,
 * callback safety, flags and payload. This wrapper adds a memory-bounded CLOCK residency policy
 * using one bounded {@code long[]} resident ring, one sparse int slot lane and one sparse
 * reference bit. No cache-node object is retained per row.
 *
 * <p>The underlying row owner is single-thread/external-synchronization; this wrapper preserves
 * that contract. A successful miss may transiently materialize one extra row before CLOCK
 * eviction. This avoids evicting unrelated resident state when the provider load itself fails.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/BoundedProviderBackedPrimitiveRows.java}
 * blob {@code b2bbc69ecd5e0adebbbf81ea36338715bee13b0a} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class BoundedProviderBackedPrimitiveRows {
    private final ProviderBackedPrimitiveRows rows;
    private final int maxResidentRows;
    private final long[] residentIds;
    private final LazyPrimitiveAddressSpace slotByRow;
    private final LazyBitAddressSpace referenced;

    private int residentCount;
    private int clockHand;

    private long touches;
    private long cacheHits;
    private long loads;
    private long loadFailures;
    private long retries;
    private long policyEvictions;
    private long explicitEvictions;
    private long invalidations;
    private long materializedPuts;
    private long admissionFailures;

    public BoundedProviderBackedPrimitiveRows(
            int maxResidentRows,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        this(
                LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                maxResidentRows,
                provider,
                laneKinds);
    }

    public BoundedProviderBackedPrimitiveRows(
            int rootBits,
            int directoryBits,
            int leafBits,
            int maxResidentRows,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        if (maxResidentRows <= 0) {
            throw new IllegalArgumentException("maxResidentRows <= 0");
        }
        this.maxResidentRows = maxResidentRows;
        residentIds = new long[maxResidentRows];
        rows = new ProviderBackedPrimitiveRows(
                rootBits, directoryBits, leafBits,
                Objects.requireNonNull(provider, "provider"), laneKinds);
        slotByRow = new LazyPrimitiveAddressSpace(
                PrimitiveKind.INT, rootBits, directoryBits, leafBits);
        referenced = new LazyBitAddressSpace(rootBits, directoryBits, leafBits);
    }

    public int laneCount() { return rows.laneCount(); }
    public PrimitiveKind laneKind(int lane) { return rows.laneKind(lane); }
    public long logicalCapacity() { return rows.logicalCapacity(); }
    public long materializedSize() { return rows.materializedSize(); }
    public int maxResidentRows() { return maxResidentRows; }
    public int residentCount() { return residentCount; }
    public short flags(long rowId) { return rows.flags(rowId); }
    public boolean isMaterialized(long rowId) { return rows.isMaterialized(rowId); }
    public boolean isResident(long rowId) { return slotByRow.getBits(rowId) != 0L; }

    public long allocatedPayloadBytes() {
        return rows.allocatedPayloadBytes()
                + slotByRow.allocatedPayloadBytes()
                + referenced.allocatedPayloadBytes()
                + (long) residentIds.length * Long.BYTES;
    }

    public long administrativePrimitiveBytes() {
        return slotByRow.allocatedPayloadBytes()
                + referenced.allocatedPayloadBytes()
                + (long) residentIds.length * Long.BYTES;
    }

    public ProviderCollectionStats stats() {
        return new ProviderCollectionStats(
                touches,
                cacheHits,
                loads,
                loadFailures,
                retries,
                policyEvictions,
                explicitEvictions,
                invalidations,
                materializedPuts,
                admissionFailures);
    }

    /** Materialize on demand. Returns true only when the provider loaded the row. */
    public boolean touch(long rowId) {
        boolean hit = rows.isMaterialized(rowId);
        touches++;
        try {
            boolean loaded = rows.touch(rowId);
            afterSuccessfulTouch(rowId, hit, loaded);
            return loaded;
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    public long getBits(int lane, long rowId) {
        boolean hit = rows.isMaterialized(rowId);
        touches++;
        try {
            long value = rows.getBits(lane, rowId);
            afterSuccessfulTouch(rowId, hit, !hit);
            return value;
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    public void readRow(long rowId, long[] target) {
        boolean hit = rows.isMaterialized(rowId);
        touches++;
        try {
            rows.readRow(rowId, target);
            afterSuccessfulTouch(rowId, hit, !hit);
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    public void setBits(int lane, long rowId, long bits) {
        boolean hit = rows.isMaterialized(rowId);
        touches++;
        try {
            rows.setBits(lane, rowId, bits);
            afterSuccessfulTouch(rowId, hit, !hit);
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    /** Explicit retry after provider failure. */
    public boolean retry(long rowId) {
        boolean hit = rows.isMaterialized(rowId);
        touches++;
        retries++;
        try {
            boolean loaded = rows.retry(rowId);
            afterSuccessfulTouch(rowId, hit, loaded);
            return loaded;
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    /** Install a materialized row without invoking the provider. */
    public boolean putMaterialized(long rowId, long... rowBits) {
        boolean hit = rows.isMaterialized(rowId);
        boolean added = rows.putMaterialized(rowId, rowBits);
        materializedPuts++;
        if (!hit) admit(rowId);
        referenced.set(rowId);
        return added;
    }

    /** Explicit invalidation preserves existing lazy-element flag semantics. */
    public boolean invalidate(long rowId) {
        boolean resident = isResident(rowId);
        boolean removed = rows.invalidate(rowId);
        if (resident) removeResident(rowId);
        if (removed || resident) invalidations++;
        return removed;
    }

    /** Explicit payload eviction; pinned rows remain resident. */
    public boolean evict(long rowId) {
        boolean removed = rows.evict(rowId);
        if (removed) {
            removeResident(rowId);
            explicitEvictions++;
        }
        return removed;
    }

    public void pin(long rowId, boolean value) {
        rows.pin(rowId, value);
    }

    public void markDirty(long rowId, boolean value) {
        rows.markDirty(rowId, value);
    }

    public short applicationFlags(long rowId) {
        return rows.applicationFlags(rowId);
    }

    public void applicationFlags(long rowId, short applicationBits) {
        rows.applicationFlags(rowId, applicationBits);
    }

    public void prefetch(long... rowIds) {
        Objects.requireNonNull(rowIds, "rowIds");
        for (long rowId : rowIds) touch(rowId);
    }

    private void afterSuccessfulTouch(long rowId, boolean hit, boolean loaded) {
        if (hit) {
            cacheHits++;
        } else if (loaded || rows.isMaterialized(rowId)) {
            loads++;
            admit(rowId);
        }
        if (rows.isMaterialized(rowId)) referenced.set(rowId);
    }

    private void admit(long rowId) {
        if (isResident(rowId)) return;
        if (residentCount == maxResidentRows && !evictOneByClock()) {
            // Restore the steady-state bound without discarding a pinned resident row.
            if (rows.evict(rowId)) {
                admissionFailures++;
            }
            throw new IllegalStateException("PROVIDER_RESIDENCY_ALL_PINNED");
        }

        int slot = findEmptySlot();
        residentIds[slot] = Math.addExact(rowId, 1L);
        slotByRow.setBits(rowId, slot + 1L);
        referenced.set(rowId);
        residentCount++;
    }

    private boolean evictOneByClock() {
        for (int pass = 0; pass < 2; pass++) {
            for (int scanned = 0; scanned < residentIds.length; scanned++) {
                int slot = clockHand;
                clockHand = clockHand + 1 == residentIds.length ? 0 : clockHand + 1;
                long encoded = residentIds[slot];
                if (encoded == 0L) continue;

                long rowId = encoded - 1L;
                if (LazyElementFlags.has(rows.flags(rowId), LazyElementFlags.PINNED)) continue;
                if (referenced.get(rowId)) {
                    referenced.clear(rowId);
                    continue;
                }
                if (!rows.evict(rowId)) continue;

                removeResidentAt(slot, rowId);
                policyEvictions++;
                return true;
            }
        }
        return false;
    }

    private int findEmptySlot() {
        for (int scanned = 0; scanned < residentIds.length; scanned++) {
            int slot = clockHand;
            clockHand = clockHand + 1 == residentIds.length ? 0 : clockHand + 1;
            if (residentIds[slot] == 0L) return slot;
        }
        throw new IllegalStateException("RESIDENCY_SLOT_INVARIANT");
    }

    private void removeResident(long rowId) {
        long encodedSlot = slotByRow.getBits(rowId);
        if (encodedSlot == 0L) return;
        int slot = Math.toIntExact(encodedSlot - 1L);
        removeResidentAt(slot, rowId);
    }

    private void removeResidentAt(int slot, long rowId) {
        residentIds[slot] = 0L;
        slotByRow.clearBits(rowId);
        referenced.clear(rowId);
        residentCount--;
    }
}
