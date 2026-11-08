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

/**
 * Internal heap-aware capacity choice for replacement primitive arrays.
 *
 * <p>Samples the current JVM once per growth, never per element. A pressure signal
 * only removes speculative capacity: it never rejects a required allocation, changes
 * logical size, evicts canonical elements, or forces GC. Java remains the allocator.
 * Available heap is advisory; concurrent allocation and VM object layout are unknown.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedHeapGrowth.java}
 * blob {@code 64d80dec1d4fc0e181e5249212fb10735389bdb4} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedHeapGrowth {
    private static final long MIB = 1024L * 1024L;

    private PackedHeapGrowth() { }

    static int capacity(int current, int minimum, int elementBytes) {
        Runtime runtime = Runtime.getRuntime();
        return choose(current, minimum, elementBytes,
                runtime.maxMemory(), runtime.totalMemory() - runtime.freeMemory());
    }

    /** Pure decision atom; explicit measurements make it deterministically testable. */
    static int choose(int current, int minimum, int elementBytes, long max, long used) {
        if (elementBytes != 1 && elementBytes != 2
                && elementBytes != 4 && elementBytes != 8) {
            throw new IllegalArgumentException("primitive element width");
        }
        int geometric = PackedSupport.growArrayCapacity(current, minimum);
        if (geometric == minimum || max <= 0 || max == Long.MAX_VALUE
                || used < 0 || used > max) {
            return geometric;
        }
        long available = max - used;
        long reserve = Math.min(64L * MIB, Math.max(MIB, max >>> 3));
        // A replacement is live alongside its source during Arrays.copyOf.
        // Heap sampling is advisory, not an allocation guarantee.
        if (available <= reserve) return minimum;

        long spendable = available - reserve;
        // 16 bytes is a conservative bookkeeping allowance, NOT a VM layout guarantee.
        long affordable = spendable > 16L ? (spendable - 16L) / elementBytes : 0L;
        if (affordable >= geometric) return geometric;
        if (affordable <= minimum) return minimum;

        // A required-only reallocation on every append makes copy traffic quadratic.
        // When headroom exists, retain a bounded burst, not the whole geometric excess.
        // 4 KiB..1 MiB, scaled with existing payload; this runs only on a growth event.
        long payloadBytes = Math.max(0L, (long) current) * elementBytes;
        long burstBytes = Math.max(4L << 10, Math.min(MIB, payloadBytes >>> 4));
        long burstEntries = Math.max(1L, burstBytes / elementBytes);
        long slack = Math.min((long) geometric - minimum,
                Math.min(affordable - minimum, burstEntries));
        return (int) (minimum + slack);
    }
}
