// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexSvgSearch}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexSvgSearch.java}
 * blob {@code 028306f4587b2f9287e9af3db3b535384afcdf89} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 97608b6a7af605f2787edb7e22e3423c2e3a0af40dbcf5556816b0f7f6f58570}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3SvgSearch}); projection
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
 *   <li>{@code nested types not received: Builder}</li>
 *   <li>{@code member builder() (return type)}</li>
 *   <li>{@code member search(MIndexSvgTape, int) (parameter tape)}</li>
 *   <li>{@code member search(MIndexSvgTape, MIndexSvgDocumentIndex, int) (parameter tape)}</li>
 *   <li>{@code member first(MIndexSvgTape) (parameter tape)}</li>
 *   <li>{@code member first(MIndexSvgTape, MIndexSvgDocumentIndex) (parameter tape)}</li>
 * </ul>
 */
public final class M3SvgSearch {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code tagId} of type {@code int}. */
    private int tagId;

    /** primitive lane for source field {@code unknownTag} of type {@code byte[]}. */
    private byte[] unknownTag;

    /** int handle lane (offsets into parent-owned storage) for source field {@code tagPattern} of type {@code MIndexPattern}. */
    private int[] tagPatternLane;

    /** primitive lane for source field {@code attributeId} of type {@code int}. */
    private int attributeId;

    /** primitive lane for source field {@code unknownAttribute} of type {@code byte[]}. */
    private byte[] unknownAttribute;

    /** int handle lane (offsets into parent-owned storage) for source field {@code attributeNamePattern} of type {@code MIndexPattern}. */
    private int[] attributeNamePatternLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code attributeValuePattern} of type {@code MIndexPattern}. */
    private int[] attributeValuePatternLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code textPattern} of type {@code MIndexPattern}. */
    private int[] textPatternLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code idValue} of type {@code String}. */
    private int[] idValueLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code className} of type {@code String}. */
    private int[] classNameLane;

    /** primitive lane for source field {@code descendantText} of type {@code boolean}. */
    private boolean descendantText;

    private M3SvgSearch() {
        throw new M3NotYetReceived("M3SvgSearch#<init>/0");
    }
}
