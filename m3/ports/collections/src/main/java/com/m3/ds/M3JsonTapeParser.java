// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexJsonTapeParser}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexJsonTapeParser.java}
 * blob {@code e1547191aa3f0921b942231cc45fdba6dc43818c} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 0abfa67a0648ec7b8899468c709333d76068559f1b9c67fb2df037258e1a01a2}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3JsonTapeParser}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
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
 *   <li>{@code nested types not received: Parser}</li>
 *   <li>{@code member parse(byte[]) (return type)}</li>
 *   <li>{@code member parse(byte[], MIndexUtf8KeyIndex, MIndexUtf8KeyIndex, int, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member parseOwned(byte[], MIndexUtf8KeyIndex, MIndexUtf8KeyIndex, int, int, MIndexProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3JsonTapeParser {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3JsonTapeParser() {
        throw new M3NotYetReceived("M3JsonTapeParser#<init>/0");
    }
}
