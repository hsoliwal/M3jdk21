// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexLongSelectWorkspace}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/data-structure/src/main/java/com/synexia/mindex/structure/MIndexLongSelectWorkspace.java}
 * blob {@code 8a0822e38c1199abd63c2c2c8737a79d410762e1} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7a3f609dda0fa6342a45a3dba948d65e5edc540364479815d76a548bbf84652a}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3LongSelectWorkspace}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S06}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
public final class M3LongSelectWorkspace {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code values} of type {@code long[]}. */
    private long[] values;

    /** primitive lane for source field {@code resizeCount} of type {@code int}. */
    private int resizeCount;

    public M3LongSelectWorkspace() {
        throw new M3NotYetReceived("M3LongSelectWorkspace#<init>/0");
    }

    public long[] copyOf(long[] source) {
        throw new M3NotYetReceived("M3LongSelectWorkspace#copyOf/1");
    }

    public int capacity() {
        throw new M3NotYetReceived("M3LongSelectWorkspace#capacity/0");
    }

    public int resizeCount() {
        throw new M3NotYetReceived("M3LongSelectWorkspace#resizeCount/0");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3LongSelectWorkspace#retainedPrimitiveBytes/0");
    }
}
