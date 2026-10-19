// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSvgGeometry}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSvgGeometry.java}
 * blob {@code 4645d34d3ccb3af66d6b1998a15482473163e339} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code aa8c59d3d1d25920989d91bc2e9703acbc889ea50034958abcb3cedb5f55c3a0}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SvgGeometry}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S10}.
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
 *   <li>{@code member path(MIndexSvgTape, int, int, int) (parameter tape)}</li>
 *   <li>{@code member points(MIndexSvgTape, int, int) (return type)}</li>
 *   <li>{@code member viewBox(MIndexSvgTape, int) (return type)}</li>
 *   <li>{@code member transform(MIndexSvgTape, int, int, int) (return type)}</li>
 *   <li>{@code member numbers(MIndexSvgTape, int, int) (return type)}</li>
 * </ul>
 */
public final class M3SvgGeometry {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3SvgGeometry() {
        throw new M3NotYetReceived("M3SvgGeometry#<init>/0");
    }
}
