// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexStructurePathIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructurePathIndex.java}
 * blob {@code 99d19379c5560e11a2a2dba1d1b5698409cbf434} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 52d5b05fe9ea5fb713f5c149e2443f6f95182945856bb2feb89ad9426d54ff19}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3StructurePathIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S15}.
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
 *   <li>{@code nested types not received: Limits, Entry}</li>
 *   <li>{@code member compile(MIndexNodeRef) (parameter root)}</li>
 *   <li>{@code member compile(MIndexNodeRef, Limits) (parameter root)}</li>
 *   <li>{@code member pathAt(int) (return type)}</li>
 *   <li>{@code member nodeAt(int) (return type)}</li>
 *   <li>{@code member kindAt(int) (return type)}</li>
 *   <li>{@code member queryModel() (return type)}</li>
 *   <li>{@code member search() (return type)}</li>
 * </ul>
 */
public final class M3StructurePathIndex {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code store} of type {@code MIndexStructuralStore}. */
    private int[] storeLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code paths} of type {@code MIndexString[]}. */
    private int[] pathsLane;

    /** primitive lane for source field {@code nodeIds} of type {@code int[]}. */
    private int[] nodeIds;

    /** primitive lane for source field {@code kinds} of type {@code byte[]}. */
    private byte[] kinds;

    /** int handle lane (offsets into parent-owned storage) for source field {@code queryModel} of type {@code MIndexQueryModel}. */
    private int[] queryModelLane;

    private M3StructurePathIndex() {
        throw new M3NotYetReceived("M3StructurePathIndex#<init>/0");
    }

    public int size() {
        throw new M3NotYetReceived("M3StructurePathIndex#size/0");
    }

    public int nodeIdAt(int row) {
        throw new M3NotYetReceived("M3StructurePathIndex#nodeIdAt/1");
    }

    public long retainedPrimitiveBytes() {
        throw new M3NotYetReceived("M3StructurePathIndex#retainedPrimitiveBytes/0");
    }
}
