// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexRecordAdmission}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexRecordAdmission.java}
 * blob {@code d95c8417f568c72103eb97f045ef85ee8cad4d32} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f1aab595b5110fd89217fdcabf6cc6d0a392089623792e73d16b8a4162ef4c42}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3RecordAdmission}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
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
 *   <li>{@code nested types not received: Result}</li>
 *   <li>{@code member admitText(MIndexSchemaPlan, Map<String, ? extends CharSequence>, int, int, int) (return type)}</li>
 * </ul>
 */
public final class M3RecordAdmission {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3RecordAdmission() {
        throw new M3NotYetReceived("M3RecordAdmission#<init>/0");
    }
}
