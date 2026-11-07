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
import jdk.m3.collections.M3RuntimeHeap;

/**
 * Admission of optional sorted/viewport work based on estimated retained item AND signal bytes.
 *
 * <p>The caller's stable-ID sequence stays untouched. Visible items are mandatory; only an
 * optional contiguous suffix can be withheld under pressure. This planner retains no entries,
 * comparator wrappers, resources or native pointers. A Java/JNI owner supplies conservative
 * per-item estimates from its existing accounting; arbitrary live Java object sizes cannot be
 * inferred exactly from Runtime heap statistics.</p>
 *
 * <p>Capture one heap sample at the batch boundary, not on every comparison or item. The
 * returned count is deterministic for the supplied snapshot and estimates. This is advisory,
 * not an allocation guarantee. Recomputable optional payloads can be admitted later without
 * discarding semantic comparison evidence in M3SortSignalLane.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/M3SortItemPressurePlan.java}
 * blob {@code 3011a957aab944b8e069dcefc5f8c5a30c50158f} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3SortItemPressurePlan {
    private M3SortItemPressurePlan() { }

    public record Admission(int requiredCount, int optionalCount, long requiredBytes,
                            long optionalBytes, long optionalBudgetBytes,
                            M3RuntimeHeap.Pressure pressure) {
        public int totalCount() { return Math.addExact(requiredCount, optionalCount); }
    }

    /**
     * @param retainedItemBytes estimates indexed by stable ID (payload, resident wrapper and
     *                          cached derived item state; nonnegative)
     * @param retainedSignalBytes persistent signal evidence estimates indexed by stable ID;
     *                            these bytes are accounted but never evicted here
     * @param sortedIds a caller-owned sorted stable-ID lane, never modified
     * @param offset first sorted item to present
     * @param requiredRows visible rows, admitted even if pressure is high
     * @param optionalRows speculative overscan rows, best-effort contiguous prefix only
     */
    public static Admission plan(long[] retainedItemBytes, long[] retainedSignalBytes,
            int[] sortedIds, int offset, int requiredRows, int optionalRows,
            M3RuntimeHeap.Snapshot snapshot) {
        Objects.requireNonNull(retainedItemBytes, "retainedItemBytes");
        Objects.requireNonNull(retainedSignalBytes, "retainedSignalBytes");
        Objects.requireNonNull(sortedIds, "sortedIds");
        Objects.requireNonNull(snapshot, "snapshot");
        if (offset < 0 || offset > sortedIds.length || requiredRows < 0 || optionalRows < 0)
            throw new IllegalArgumentException("invalid sorted work window");
        int mandatory = Math.min(requiredRows, sortedIds.length - offset);
        int speculative = Math.min(optionalRows, sortedIds.length - offset - mandatory);
        // Validate all requested positions before computing or publishing admission.
        for (int i = 0; i < mandatory + speculative; i++) {
            int id = sortedIds[offset + i];
            if (id < 0 || id >= retainedItemBytes.length || id >= retainedSignalBytes.length
                    || retainedItemBytes[id] < 0 || retainedSignalBytes[id] < 0)
                throw new IllegalArgumentException("missing or negative item/signal weight");
        }
        long required = 0L;
        for (int i = 0; i < mandatory; i++) {
            int id = sortedIds[offset + i];
            required = plus(required, plus(retainedItemBytes[id], retainedSignalBytes[id]));
        }
        if (mandatory == 0 || speculative == 0) {
            return new Admission(mandatory, 0, required, 0L, 0L,
                    snapshot.pressure(required));
        }
        M3RuntimeHeap.Pressure pressure = snapshot.pressure(required);
        long free = Math.max(0L, snapshot.freeToMaximum() - snapshot.safetyReserve());
        long headroom = Math.max(0L, free - required);
        // Admission below warm threshold leaves space for independent JVM work.
        long warmLimit = snapshot.maximumBytes() / 10L * 7L
                + snapshot.maximumBytes() % 10L * 7L / 10L;
        long warmHeadroom = Math.max(0L, warmLimit - snapshot.usedBytes() - required);
        long budget = pressure == M3RuntimeHeap.Pressure.HIGH ? 0L
                : Math.min(headroom, warmHeadroom);
        if (pressure == M3RuntimeHeap.Pressure.HIGH) {
            return new Admission(mandatory, 0, required, 0L, 0L, pressure);
        }
        int admitted = 0;
        long optionalBytes = 0L;
        for (int i = 0; i < speculative; i++) {
            int id = sortedIds[offset + mandatory + i];
            long weight = plus(retainedItemBytes[id], retainedSignalBytes[id]);
            if (weight > budget - optionalBytes) break;
            optionalBytes += weight;
            admitted++;
        }
        return new Admission(mandatory, admitted, required, optionalBytes, budget, pressure);
    }

    /** Convenience for ordinary M3 execution; never called per comparator invocation. */
    public static Admission plan(long[] retainedItemBytes, long[] retainedSignalBytes,
            int[] sortedIds, int offset, int requiredRows, int optionalRows) {
        return plan(retainedItemBytes, retainedSignalBytes, sortedIds, offset,
                requiredRows, optionalRows, M3RuntimeHeap.current());
    }

    private static long plus(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
