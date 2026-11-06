// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

/**
 * Synchronous owner-scoped operations between SDK regions.
 * No native pointer, mapped view, or array escapes to Java callers.
 * Copy changes destination bytes; call {@link M3PmemRegion#persist(long, long)}
 * separately if durable completion is required.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemTransfer.java blob ebd4dbc93549; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class M3PmemTransfer {
    private M3PmemTransfer() {}

    /**
     * Copy byte ranges (FILE uses at most 64 KiB scratch). Supports overlapping
     * ranges in the SAME mapping (memmove). For distinct mappings of the same
     * backing file, overlapping file spans are rejected to avoid file aliasing.
     * Operations acquire both region leases in a stable order.
     */
    public static void copy(M3PmemRegion source, long sourceOffset,
                            M3PmemRegion destination, long destinationOffset, long bytes) {
        M3PmemMapping.operate(source, sourceOffset, destination, destinationOffset, bytes, true);
    }

    /** Compare exact bytes; FILE uses at most 64 KiB combined scratch. */
    public static boolean equalRange(M3PmemRegion a, long aOffset,
                                     M3PmemRegion b, long bOffset, long bytes) {
        return M3PmemMapping.operate(a, aOffset, b, bOffset, bytes, false);
    }
    /**
     * Copy only when the byte ranges differ; returns true only if bytes were
     * written. Compare and copy are one operation protected by both ordered
     * region leases. Does not persist: call destination.persist() explicitly.
     * The same mapping supports memmove; distinct aliases of an overlapping
     * file range remain invalid even if their contents are identical.
     */
    public static boolean copyIfDifferent(M3PmemRegion source, long sourceOffset,
                                          M3PmemRegion destination, long destinationOffset,
                                          long bytes) {
        return M3PmemMapping.operateIfDifferent(source, sourceOffset,
                                               destination, destinationOffset, bytes);
    }
}
