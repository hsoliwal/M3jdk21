// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexStructuralByteScanner}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexStructuralByteScanner.java}
 * blob {@code 5ddb8a1352853944e5ccf44fe53cf31749d02cba} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f248eee98a6961faf2b0233bf72cdd99c11d6451eda59433f7d7bf1a9402f48c}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3StructuralByteScanner}); projection
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
 *   <li>{@code nested types not received: Scan}</li>
 *   <li>{@code member scan(MIndexSourceFormat, byte[], int) (return type)}</li>
 *   <li>{@code member scan(MIndexDocument, int) (return type)}</li>
 * </ul>
 */
public final class M3StructuralByteScanner {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3StructuralByteScanner() {
        throw new M3NotYetReceived("M3StructuralByteScanner#<init>/0");
    }
}
