// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lightweight, admission-cadenced pressure feedback for the canonical parent
 * facade store. It never owns a separate cache, array or eviction policy.
 */
//FLLDCIM: RELOCATE - donor blob 0d8fcea8e52e at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
final class FacadePressureController {
    private static final int SAMPLE_MASK = 63;
    private final Supplier<RuntimeHeap.Snapshot> source;
    private int admissions;
    private RuntimeHeap.Pressure last = RuntimeHeap.Pressure.NORMAL;

    FacadePressureController(Supplier<RuntimeHeap.Snapshot> source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    /** Called only on misses with optional retention; never on ordinary facade hits. */
    boolean mayRetain(TransientFacadeStore<?> owner) {
        if ((admissions++ & SAMPLE_MASK) == 0) {
            RuntimeHeap.Snapshot snapshot = Objects.requireNonNull(source.get(), "heap sample");
            last = snapshot.pressure(0L);
            if (last != RuntimeHeap.Pressure.NORMAL) {
                int pinned = owner.pinnedCount();
                int eligible = owner.residentCount() - pinned;
                int target = last == RuntimeHeap.Pressure.HIGH
                        ? pinned : pinned + eligible / 2;
                owner.releaseOptionalResidency(target);
                // Same parent owner, no second cache or GC-triggered reuse decision.
                // Scrubbed internal facades are optional and never pinned.
                owner.releaseReusable(last == RuntimeHeap.Pressure.HIGH ? 0
                        : owner.reusableIdleCount() / 2);
            }
        }
        return last != RuntimeHeap.Pressure.HIGH;
    }

    RuntimeHeap.Pressure level() { return last; }
}
