// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexCanonicalWrapperCache}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexCanonicalWrapperCache.java}
 * blob {@code 1ae86316836698f0e448c4e4d290bf672563b23b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 5614967c2f90815258418de8fa4ea21b0e76711d35f2e4faced1dd6c9c0418fe}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3CanonicalWrapperCache}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S13}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: CanonicalRef}</li>
 * </ul>
 */
final class M3CanonicalWrapperCache {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code canonical} of type {@code Map<Long, CanonicalRef>}. */
    private int[] canonicalLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code queue} of type {@code ReferenceQueue<MIndexString>}. */
    private int[] queueLane;

    M3CanonicalWrapperCache() {
        throw new M3NotYetReceived("M3CanonicalWrapperCache#<init>/0");
    }
}
