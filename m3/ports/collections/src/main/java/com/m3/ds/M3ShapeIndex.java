// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexShapeIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexShapeIndex.java}
 * blob {@code d13a572140a329ec279168803eb9e067ac518b77} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 667277f07540ae27f5a009ae0e52448993e34c205ccbf89a56e37a114329c4a8}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3ShapeIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S05}.
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
 *   <li>{@code member intern(MIndexObjectShape<?>) (parameter shape)}</li>
 * </ul>
 */
public final class M3ShapeIndex {
    public static final int UNSPECIFIED_TYPE_ID = -1;

    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code composites} of type {@code MIndexCompositeIndex}. */
    private int[] compositesLane;

    public M3ShapeIndex() {
        throw new M3NotYetReceived("M3ShapeIndex#<init>/0");
    }

    public M3ShapeIndex(M3CompositeIndex composites) {
        throw new M3NotYetReceived("M3ShapeIndex#<init>/1");
    }

    public int intern(int[] memberNameIds) {
        throw new M3NotYetReceived("M3ShapeIndex#intern/1");
    }

    public int intern(int[] memberNameIds, int[] memberTypeIds) {
        throw new M3NotYetReceived("M3ShapeIndex#intern/2");
    }

    public M3CompositeIndex compositeIndex() {
        throw new M3NotYetReceived("M3ShapeIndex#compositeIndex/0");
    }

    public int memberCount(int shapeId) {
        throw new M3NotYetReceived("M3ShapeIndex#memberCount/1");
    }

    public int nameId(int shapeId, int ordinal) {
        throw new M3NotYetReceived("M3ShapeIndex#nameId/2");
    }

    public int typeId(int shapeId, int ordinal) {
        throw new M3NotYetReceived("M3ShapeIndex#typeId/2");
    }

    public int ordinalOfNameId(int shapeId, int nameId) {
        throw new M3NotYetReceived("M3ShapeIndex#ordinalOfNameId/2");
    }

    public int[] copyNameIds(int shapeId) {
        throw new M3NotYetReceived("M3ShapeIndex#copyNameIds/1");
    }

    public int[] copyTypeIds(int shapeId) {
        throw new M3NotYetReceived("M3ShapeIndex#copyTypeIds/1");
    }

    public int[] copyLane(int shapeId) {
        throw new M3NotYetReceived("M3ShapeIndex#copyLane/1");
    }

    public long structuralHash64(int shapeId) {
        throw new M3NotYetReceived("M3ShapeIndex#structuralHash64/1");
    }
}
