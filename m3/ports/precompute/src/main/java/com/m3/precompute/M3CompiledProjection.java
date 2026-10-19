// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexCompiledProjection}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexCompiledProjection.java}
 * blob {@code 3eca72e31f9d3ee4575bbb69b78be132bfc2ff6f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 6caa1a66286390d7a5ce396ecb8f88f2c9dd7092c8f3be96b35953c24fdfe06c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3CompiledProjection}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S21}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Builder}</li>
 *   <li>{@code member builder(MIndexStructuralStore) (return type)}</li>
 *   <li>{@code member store() (return type)}</li>
 *   <li>{@code member path(int) (return type)}</li>
 * </ul>
 */
public final class M3CompiledProjection {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code store} of type {@code MIndexStructuralStore}. */
    private int[] storeLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code paths} of type {@code MIndexCompiledPath[]}. */
    private int[] pathsLane;

    private M3CompiledProjection() {
        throw new M3NotYetReceived("M3CompiledProjection#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3CompiledProjection#size/0");
    }

    public int resolveIds(int rootNodeId, int[] destination, int offset) {
        throw new M3NotYetReceived("M3CompiledProjection#resolveIds/3");
    }

    public int[] resolveIds(int rootNodeId) {
        throw new M3NotYetReceived("M3CompiledProjection#resolveIds/1");
    }

    public long retainedPayloadBytes() {
        throw new M3NotYetReceived("M3CompiledProjection#retainedPayloadBytes/0");
    }
}
