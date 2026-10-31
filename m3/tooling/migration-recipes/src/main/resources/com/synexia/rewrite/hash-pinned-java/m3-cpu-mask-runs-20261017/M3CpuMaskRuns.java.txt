// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;

/**
 * Run-length CPU atom over the existing indexed multi-plane mask selector.
 *
 * <p>The caller owns row IDs, physical offsets and state planes. Matching row
 * occurrences are streamed in signed-stride encounter order, retaining duplicate
 * row IDs and allowing a repeated run to span multiple bounded windows. The
 * only scratch payload is one reusable primitive int[4096] window; there are
 * no per-row Entry objects or boxed values. No text payload is materialized.
 *
 * <p>The Java policy is delegated to M3IndexPathMasks. NATIVE_REQUIRED delegates
 * to that owner's optional C17/JNI implementation, including explicit library
 * refusal. The callback may observe earlier runs before a later geometry,
 * concurrent modification, or callback failure; this operation is not atomic.
 * Source arrays and stage masks must be stable for the duration of the scan.
 */
public final class M3CpuMaskRuns {
    private M3CpuMaskRuns() { }
    private static final int[] NO_OUTPUT = new int[0];

    @FunctionalInterface
    public interface RunConsumer {
        void accept(int rowId, long occurrences);
    }

    /**
     * Streams at most maxRuns nonempty runs of consecutive selected row IDs.
     *
     * @return the number of run callbacks delivered (never an output array)
     */
    public static int scan(int[] orderedRows, int[] physicalOffsets,
            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,
            int first, int count, int stride, int maxRuns,
            M3IndexPathMasks.Backend backend, RunConsumer consumer) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(consumer, "consumer");
        if (maxRuns < 0) throw new IllegalArgumentException("negative run budget");
        // Always validate borrowed geometry and the JNI boundary before any callbacks.
        // The zero-length output is shared safely because limit zero never writes it.
        M3IndexPathMasks.scanWindow(orderedRows, physicalOffsets, statePlanes,
                requiredMasks, forbiddenMasks, first, count, stride, 0,
                NO_OUTPUT, 0, 0, backend);
        if (maxRuns == 0 || count == 0) return 0;

        int[] window = new int[M3IndexPathMasks.MAX_WINDOW];
        int next = 0;
        int runCount = 0;
        boolean pending = false;
        int currentRow = 0;
        long length = 0;
        while (next < count) {
            long result = M3IndexPathMasks.scanWindow(orderedRows, physicalOffsets,
                    statePlanes, requiredMasks, forbiddenMasks, first, count,
                    stride, next, window, 0, window.length, backend);
            int following = M3IndexPathMasks.nextOrdinal(result);
            int written = M3IndexPathMasks.written(result);
            if (following <= next || following > count
                    || written < 0 || written > window.length) {
                throw new IllegalStateException("mask window made invalid progress");
            }
            for (int i = 0; i < written; i++) {
                int row = window[i];
                if (pending && row != currentRow) {
                    consumer.accept(currentRow, length);
                    runCount++;
                    if (runCount == maxRuns) return runCount;
                    pending = false;
                }
                if (!pending) {
                    currentRow = row;
                    length = 1L;
                    pending = true;
                } else {
                    length = Math.addExact(length, 1L);
                }
            }
            next = following;
        }
        if (pending && runCount < maxRuns) {
            consumer.accept(currentRow, length);
            runCount++;
        }
        return runCount;
    }
}
