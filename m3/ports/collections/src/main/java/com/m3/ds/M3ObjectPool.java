// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexObjectPool}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexObjectPool.java}
 * blob {@code 94bb259782dc3d226f846c6022791de6c604d3ed} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 41f99219f6b4a631d2aca7d1c67125f32afb6e8310a2b129862afe7495e8ff37}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3ObjectPool}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
final class M3ObjectPool {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code signal64} of type {@code long[]}. */
    private long[] signal64;

    /** primitive lane for source field {@code signalHigh} of type {@code long[]}. */
    private long[] signalHigh;

    /** primitive lane for source field {@code signalLow} of type {@code long[]}. */
    private long[] signalLow;

    /** primitive lane for source field {@code typeIds} of type {@code int[]}. */
    private int[] typeIds;

    /** primitive lane for source field {@code states} of type {@code byte[]}. */
    private byte[] states;

    /** int handle lane (offsets into parent-owned storage) for source field {@code entries} of type {@code WeakReference<MIndexObject<?>>[]}. */
    private int[] entriesLane;

    /** primitive lane for source field {@code occupied} of type {@code int}. */
    private int occupied;

    M3ObjectPool() {
        throw new M3NotYetReceived("M3ObjectPool#<init>/0");
    }
}
