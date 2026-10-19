// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.LinkedHashMap;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexJavaValues}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexJavaValues.java}
 * blob {@code 2987c1b5ee9d776b9be6531457f8d4d2ed690918} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 962e4a783c36bd61ae73a23e6514e68ab1038f9d0b6a2b6ec45ded771376a131}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3JavaValues}); projection
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
 *   <li>{@code nested types not received: Tuple, Table, Edge, Ref, Event}</li>
 *   <li>{@code member admit(MIndexStructureBuilder, Object, MIndexSourceFormat, boolean) (parameter builder)}</li>
 *   <li>{@code member materialize(MIndexNode, MIndexSourceFormat, boolean) (parameter node)}</li>
 *   <li>{@code member materializeMap(MIndexNode, MIndexSourceFormat, boolean) (parameter map)}</li>
 * </ul>
 */
public final class M3JavaValues {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3JavaValues() {
        throw new M3NotYetReceived("M3JavaValues#<init>/0");
    }
}
