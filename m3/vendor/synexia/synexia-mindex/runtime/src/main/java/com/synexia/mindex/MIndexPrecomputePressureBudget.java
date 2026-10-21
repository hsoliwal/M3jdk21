// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * One pure, shared memory-pressure admission decision for optional immutable M3 precompute
 * images. The original string, atom ID, regex/literal semantics and compiled result belong
 * to their existing owners; this class neither caches values nor creates entry objects.
 *
 * <p>Return a retention ceiling, not permission to skip required precompute. Under heap
 * pressure callers return the exact newly compiled image but may decline to retain it.
 * Take a JVM snapshot at a cold compile/upgrade boundary, never for each cache hit or
 * comparison. Byte weights exclude image headers, native memory, and GC movement.</p>
 */
public final class MIndexPrecomputePressureBudget {
    private MIndexPrecomputePressureBudget() { }

    /**
     * Retained weight budget after reserving JVM headroom. The caller-supplied configured
     * maximum remains an upper bound. Normal/soft pressure admit at most 1/4 or 1/8 of
     * spare heap respectively, and hard pressure declines all optional residency.
     *
     * <p>The snapshot includes the full candidate allocation peak while existing images
     * remain live. This is advisory: it cannot guarantee the next allocation succeeds.</p>
     */
    public static long activeBudget(long configuredBytes, MIndexHeapAdmission.Snapshot heap) {
        if (configuredBytes < 0L) throw new IllegalArgumentException("negative retention budget");
        MIndexHeapAdmission.Snapshot observed = Objects.requireNonNull(heap, "heap");
        long free = Math.max(0L, observed.availableToMaximumBytes()
                - Math.min(observed.availableToMaximumBytes(), observed.additionalPeakBytes()));
        long spare = Math.max(0L, free - Math.min(free, observed.reserveBytes()));
        long ceiling = switch (observed.level()) {
            case NORMAL -> spare >>> 2;
            case SOFT -> spare >>> 3;
            case HARD -> 0L;
        };
        return Math.min(configuredBytes, ceiling);
    }
}
