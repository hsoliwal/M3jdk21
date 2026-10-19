// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexFixedTypedLayout}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexFixedTypedLayout.java}
 * blob {@code bb1094cb2198262d9935f6607532901fb2d4fcda} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code da02f11ee4f6049c4a53ad03f039fca979d6333b64ca794f282629d2dddf53d5}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3FixedTypedLayout}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S07}.
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
 *   <li>{@code nested types not received: Binding, InvalidRecord}</li>
 *   <li>{@code member compile(MIndexFixedLayout, MIndexSchemaPlan, List<Binding>, MIndexProgressMonitor) (parameter schema)}</li>
 *   <li>{@code member recordPlan() (return type)}</li>
 *   <li>{@code member parseRecord(byte[], int, int, int, MIndexProgressMonitor) (return type)}</li>
 *   <li>{@code member parseRecord(byte[], MIndexFixedValidation.Backend, MIndexFixedRecordParser.SpanScanner, int, int, int, MIndexProgressMonitor) (return type)}</li>
 * </ul>
 */
public final class M3FixedTypedLayout {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code layout} of type {@code MIndexFixedLayout}. */
    private int[] layoutLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code recordPlan} of type {@code MIndexFixedRecordPlan}. */
    private int[] recordPlanLane;

    private M3FixedTypedLayout() {
        throw new M3NotYetReceived("M3FixedTypedLayout#<init>/0");
    }

    public M3FixedLayout layout() {
        throw new M3NotYetReceived("M3FixedTypedLayout#layout/0");
    }
}
