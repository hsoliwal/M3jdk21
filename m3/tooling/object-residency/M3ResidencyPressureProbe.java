// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.verification;

import com.m3.collections.M3ObjectResidency;

/** Real small-Xmx LRU/recreation proof with canonical state retained in the parent. */
public final class M3ResidencyPressureProbe {
    private static final int SLOTS = 1_024;
    private static final long ESTIMATED_PROJECTION_BYTES = 16 * 1024;
    private M3ResidencyPressureProbe() { }

    private static final class Projection {
        private final long[] parentState;
        private final int slot;
        private Projection(long[] parentState, int slot) {
            this.parentState = parentState;
            this.slot = slot;
        }
        private long incrementCanonicalState() { return ++parentState[slot]; }
    }

    public static void main(String[] args) {
        long[] parentState = new long[SLOTS];
        int[] creations = new int[SLOTS];
        int flags = M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU
                | M3ObjectResidency.PRESSURE_AWARE | M3ObjectResidency.RECREATE;
        M3ObjectResidency<Projection> arena = new M3ObjectResidency<>(
                SLOTS, flags, Long.MAX_VALUE, 24L << 20,
                slot -> {
                    creations[slot]++;
                    return new Projection(parentState, slot);
                }, null, null);

        for (int slot = 0; slot < SLOTS; slot++) {
            long observed = arena.use(slot, ESTIMATED_PROJECTION_BYTES, Projection::incrementCanonicalState);
            if (observed != 1L) throw new AssertionError("first canonical mutation lost at slot " + slot);
        }
        if (arena.state(0) != M3ObjectResidency.State.ABSENT) {
            throw new AssertionError("small-Xmx pressure did not evict the cold first projection");
        }
        long restored = arena.use(0, ESTIMATED_PROJECTION_BYTES, Projection::incrementCanonicalState);
        if (restored != 2L || creations[0] < 2) {
            throw new AssertionError("recreated projection did not observe parent-owned canonical state");
        }
        M3ObjectResidency.HeapSnapshot heap = M3ObjectResidency.HeapSnapshot.current();
        if (heap.maxBytes() <= 0 || arena.residentBytes() > heap.estimatedHeadroomBytes()) {
            throw new AssertionError("invalid heap/residency observation");
        }
        System.out.println("M3_RESIDENCY_PRESSURE_PASS slots=" + SLOTS
                + " firstSlotCreations=" + creations[0]
                + " firstSlotCanonicalValue=" + restored
                + " residentBytes=" + arena.residentBytes()
                + " xmxBytes=" + heap.maxBytes()
                + " usedBytes=" + heap.usedBytes()
                + " headroomBytes=" + heap.estimatedHeadroomBytes());
    }
}
