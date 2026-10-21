// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Stable logical coordinate into one MIndex storage region.
 *
 * <p>This is deliberately not a JVM object address. Region identity and generation describe the
 * storage epoch; offset and length describe the immutable byte range within that epoch. A runtime
 * may lower this coordinate to heap, native, mapped, persistent-memory, GPU, or other storage
 * without changing semantic identity.</p>
 */
public record MIndexAddress(long regionId, long offset, int length, long generation) {
    public MIndexAddress {
        if (regionId <= 0L) {
            throw new IllegalArgumentException("regionId must be positive: " + regionId);
        }
        if (offset < 0L) {
            throw new IllegalArgumentException("offset must not be negative: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative: " + length);
        }
        if (generation < 0L) {
            throw new IllegalArgumentException("generation must not be negative: " + generation);
        }
        Math.addExact(offset, (long) length);
    }

    public long endExclusive() {
        return Math.addExact(offset, (long) length);
    }

    /** True when both addresses belong to the same storage epoch. */
    public boolean sameRegion(MIndexAddress other) {
        return other != null
                && regionId == other.regionId
                && generation == other.generation;
    }

    /** Create a bounded zero-copy logical subrange in the same storage epoch. */
    public MIndexAddress slice(int relativeOffset, int sliceLength) {
        Objects.checkFromIndexSize(relativeOffset, sliceLength, length);
        return new MIndexAddress(
                regionId,
                Math.addExact(offset, (long) relativeOffset),
                sliceLength,
                generation);
    }
}
