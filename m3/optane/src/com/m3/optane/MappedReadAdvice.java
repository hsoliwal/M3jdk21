// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.optane;

import java.nio.MappedByteBuffer;
import java.util.Objects;

/**
 * Optional Linux hints on an EXISTING read-only mapping, never a new storage owner.
 * Hints affect kernel mapped-file policy, not Intel's hidden DRAM cache or durability.
 * No pointer is exposed or retained. The existing owner must prevent concurrent unmap,
 * truncation or mutation; this API does not grant a new lease on external native storage.
 */
public final class MappedReadAdvice {
    private MappedReadAdvice() {}

    public enum Advice { NORMAL, SEQUENTIAL, RANDOM, WILL_NEED }
    public enum Status { APPLIED, NO_COMPLETE_PAGES, NATIVE_UNAVAILABLE, OS_REJECTED }
    public record Result(Status status, long bytesAdvised, int errno) {
        public Result {
            Objects.requireNonNull(status, "status");
            if (bytesAdvised < 0 || errno < 0) throw new IllegalArgumentException("negative result");
        }
    }
    public static String nativeStatus() { return NativeSupport.STATUS; }

    /**
     * Applies at most 64 MiB, within [offset,offset+length) and buffer.limit().
     * Native code aligns INWARD to complete pages; adjacent bytes are never included.
     * Position/limit/content are unchanged. WILL_NEED is not a residency guarantee.
     * There is deliberately no DONTNEED, FREE, write, flush, pin or mode-change operation.
     */
    public static Result advise(MappedByteBuffer mapping, int offset, int length,
                                Advice advice, ProgressMonitor progress) {
        Objects.requireNonNull(mapping, "mapping");
        Objects.requireNonNull(advice, "advice");
        if (!mapping.isReadOnly()) throw new IllegalArgumentException("read-only mapping required");
        Objects.checkFromIndexSize(offset, length, mapping.limit());
        if (length > ScanPlan.MAX_WINDOW_BYTES) throw new IllegalArgumentException("hint exceeds 64 MiB");
        ProgressMonitor monitor = progress == null ? ProgressMonitor.NONE : progress;
        monitor.checkCancelled();
        if (length == 0) return new Result(Status.NO_COMPLETE_PAGES, 0, 0);
        if (!NativeSupport.AVAILABLE) return new Result(Status.NATIVE_UNAVAILABLE, 0, 0);
        long result = NativeSupport.advise0(mapping, offset, length, advice.ordinal());
        if (result < 0) return new Result(Status.OS_REJECTED, 0, Math.toIntExact(-result));
        if (result > length) throw new IllegalStateException("native advice escaped requested range");
        return new Result(result == 0 ? Status.NO_COMPLETE_PAGES : Status.APPLIED, result, 0);
    }
}
