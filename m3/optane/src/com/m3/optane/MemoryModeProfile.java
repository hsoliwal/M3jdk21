// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.optane;

/**
 * OPERATOR-DECLARED capacities, not hardware detection or firmware admission.
 * In Memory Mode DRAM is hardware cache, not an application-addressable tier.
 * This class neither reserves DRAM nor reports physically installed capacity.
 */
public record MemoryModeProfile(long dramCacheBytes, long pmemBytes, int sockets) {
    public MemoryModeProfile {
        if (dramCacheBytes <= 0 || pmemBytes <= 0 || sockets <= 0)
            throw new IllegalArgumentException("positive declared capacities and socket count required");
    }

    /** Ratio only; not an OEM-supported DIMM population verdict. */
    public double pmemToDramRatio() { return (double) pmemBytes / dramCacheBytes; }

    /** Cache is not added to this operator-declared Memory Mode capacity. */
    public long declaredAddressableBytes() { return pmemBytes; }

    /**
     * Derives a soft aggregate temporary-work budget from declared cache size.
     * hotStateBytes is a caller estimate, not discovered cache occupancy.
     * workers and simultaneousCopies must include all cooperating active work.
     * No affinity, NUMA placement, memory reservation or executor is created.
     */
    public ScanPlan plan(long hotStateBytes, int workers, int simultaneousCopies,
                         int requestedWindowBytes, ProgressMonitor progress) {
        ProgressMonitor monitor = progress == null ? ProgressMonitor.NONE : progress;
        monitor.checkCancelled();
        if (hotStateBytes < 0 || hotStateBytes >= dramCacheBytes || workers <= 0
                || simultaneousCopies <= 0 || requestedWindowBytes <= 0)
            throw new IllegalArgumentException("invalid cache budget or worker/copy/window count");
        long available = dramCacheBytes - hotStateBytes;
        long allowance = available / workers / simultaneousCopies;
        long bounded = Math.min(allowance, Math.min(requestedWindowBytes, ScanPlan.MAX_WINDOW_BYTES));
        if (bounded < 1) throw new IllegalArgumentException("no byte budget per worker/copy");
        return new ScanPlan((int) bounded, workers, simultaneousCopies, available);
    }
}
