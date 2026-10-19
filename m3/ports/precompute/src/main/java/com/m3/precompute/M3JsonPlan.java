// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexJsonPlan}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/structure/MIndexJsonPlan.java}
 * blob {@code 95fa29e05e21ee01d483616950471e0e00981f10} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4778cdc47038c24216b7808f7c01a804ae92234e57503eedf72c7dd401073be8}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3JsonPlan}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S16}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 *   <li>{@code member MIndexJsonPlan(MIndexSchemaPlan) (parameter schema)}</li>
 *   <li>{@code member schema() (return type)}</li>
 *   <li>{@code member decode(byte[], int, int, int) (return type)}</li>
 *   <li>{@code member encode(MIndexValues, int) (parameter values)}</li>
 * </ul>
 */
public final class M3JsonPlan {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code schema} of type {@code MIndexSchemaPlan}. */
    private int[] schemaLane;

    /** int handle lane (offsets into parent-owned storage) for source field {@code codec} of type {@code MIndexJson}. */
    private int[] codecLane;

    private M3JsonPlan() {
        throw new M3NotYetReceived("M3JsonPlan#<init>/0");
    }
}
