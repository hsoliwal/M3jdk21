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

import jdk.m3.collections.M3LazyPrimitiveAddressSpace;
import jdk.m3.collections.PrimitiveKind;

/**
 * Collection-level CLOCK residency bookkeeping shared by bounded provider facades.
 *
 * <p>One bounded long[] holds resident logical ids. Sparse primitive address spaces map logical
 * id -> slot and hold the CLOCK reference bit. No cache node/object is retained per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderResidencyClock.java}
 * blob {@code 672e91ac5e13750bf0844862ad55006297a1e0eb} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
final class M3ProviderResidencyClock {

    @FunctionalInterface
    interface FlagsReader {
        short flags(long logicalId);
    }

    @FunctionalInterface
    interface Evictor {
        boolean evict(long logicalId);
    }

    enum Admission {
        ALREADY_RESIDENT,
        ADMITTED,
        ADMITTED_WITH_EVICTION
    }

    private final long[] residentIds;
    private final M3LazyPrimitiveAddressSpace slotById;
    private final M3LazyBitAddressSpace referenced;
    private int residentCount;
    private int clockHand;

    M3ProviderResidencyClock(
            int rootBits,
            int directoryBits,
            int leafBits,
            int maximumResident) {
        if (maximumResident <= 0) {
            throw new IllegalArgumentException("maximumResident <= 0");
        }
        residentIds = new long[maximumResident];
        slotById = new M3LazyPrimitiveAddressSpace(
                PrimitiveKind.INT, rootBits, directoryBits, leafBits);
        referenced = new M3LazyBitAddressSpace(rootBits, directoryBits, leafBits);
    }

    int maximumResident() {
        return residentIds.length;
    }

    int residentCount() {
        return residentCount;
    }

    boolean isResident(long logicalId) {
        return slotById.getBits(logicalId) != 0L;
    }

    void reference(long logicalId) {
        if (isResident(logicalId)) referenced.set(logicalId);
    }

    Admission admit(
            long logicalId,
            FlagsReader flags,
            Evictor evictor) {
        if (isResident(logicalId)) {
            referenced.set(logicalId);
            return Admission.ALREADY_RESIDENT;
        }

        boolean evicted = false;
        if (residentCount == residentIds.length) {
            if (!evictOne(flags, evictor)) {
                throw new IllegalStateException("PROVIDER_RESIDENCY_ALL_PINNED");
            }
            evicted = true;
        }

        int slot = findEmptySlot();
        residentIds[slot] = Math.addExact(logicalId, 1L);
        slotById.setBits(logicalId, slot + 1L);
        referenced.set(logicalId);
        residentCount++;
        return evicted ? Admission.ADMITTED_WITH_EVICTION : Admission.ADMITTED;
    }

    boolean remove(long logicalId) {
        long encodedSlot = slotById.getBits(logicalId);
        if (encodedSlot == 0L) return false;
        int slot = Math.toIntExact(encodedSlot - 1L);
        removeAt(slot, logicalId);
        return true;
    }

    long administrativePrimitiveBytes() {
        return slotById.allocatedPayloadBytes()
                + referenced.allocatedPayloadBytes()
                + (long) residentIds.length * Long.BYTES;
    }

    private boolean evictOne(
            FlagsReader flags,
            Evictor evictor) {
        for (int pass = 0; pass < 2; pass++) {
            for (int scanned = 0; scanned < residentIds.length; scanned++) {
                int slot = clockHand;
                clockHand = clockHand + 1 == residentIds.length ? 0 : clockHand + 1;
                long encoded = residentIds[slot];
                if (encoded == 0L) continue;

                long logicalId = encoded - 1L;
                if (M3LazyElementFlags.has(flags.flags(logicalId), M3LazyElementFlags.PINNED)) {
                    continue;
                }
                if (referenced.get(logicalId)) {
                    referenced.clear(logicalId);
                    continue;
                }
                if (!evictor.evict(logicalId)) {
                    continue;
                }

                removeAt(slot, logicalId);
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

    private void removeAt(int slot, long logicalId) {
        residentIds[slot] = 0L;
        slotById.clearBits(logicalId);
        referenced.clear(logicalId);
        residentCount--;
    }
}
