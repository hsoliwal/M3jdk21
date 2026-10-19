// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexStructureImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexStructureImage.java}
 * blob {@code 0724ac2cf92fb41b7f9b471eb7b0aeffe3e928fd} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 1e5c0a55c24a559a4bb94fa77607d5c5f079d5ded1a6ef5d66c0873cb04d3772}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3StructureImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S18}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Co-donors with the same proposed name (BLOB_DIFF, not merged, review required):
 * <ul>
 *   <li>{@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructureImage.java(at)b9e91f13a67f}</li>
 * </ul>
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code nested types not received: Row, Mapped, Utf16Cursor}</li>
 *   <li>{@code member writeNew(MIndexValues, int, Path) (parameter values)}</li>
 *   <li>{@code member open(Path) (return type)}</li>
 * </ul>
 */
public final class M3StructureImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3StructureImage() {
        throw new M3NotYetReceived("M3StructureImage#<init>/0");
    }
}
