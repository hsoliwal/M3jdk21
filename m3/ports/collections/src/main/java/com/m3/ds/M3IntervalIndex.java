// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.foundation.MIndexIntervalIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation/MIndexIntervalIndex.java}
 * blob {@code 2599b619a6084d8076f1d195488ebc4194117e71} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4767efd4bf24daf74520c709ee0b4096bf656215122df53b7aa646223328fb3d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.foundation.M3IntervalIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S12}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3IntervalIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code starts} of type {@code int[]}. */
    private int[] starts;

    /** primitive lane for source field {@code ends} of type {@code int[]}. */
    private int[] ends;

    private M3IntervalIndex() {
        throw new M3NotYetReceived("M3IntervalIndex#<init>/0");
    }

    public static M3IntervalIndex of(int[] starts, int[] ends) {
        throw new M3NotYetReceived("M3IntervalIndex#of/2");
    }

    public int size() {
        throw new M3NotYetReceived("M3IntervalIndex#size/0");
    }

    public int start(int index) {
        throw new M3NotYetReceived("M3IntervalIndex#start/1");
    }

    public int end(int index) {
        throw new M3NotYetReceived("M3IntervalIndex#end/1");
    }

    public int[] mergedClosed() {
        throw new M3NotYetReceived("M3IntervalIndex#mergedClosed/0");
    }

    public int minimumConcurrentHalfOpen() {
        throw new M3NotYetReceived("M3IntervalIndex#minimumConcurrentHalfOpen/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3IntervalIndex#retainedPrimitiveBytes/0");
    }
}
