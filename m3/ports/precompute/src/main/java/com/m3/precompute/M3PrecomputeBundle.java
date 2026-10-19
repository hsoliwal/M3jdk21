// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexPrecomputeBundle}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexPrecomputeBundle.java}
 * blob {@code 45c90bec1d0715de9d654893c731fae1fa9bb3b5} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 18f828def92951a69e553062d95aba7570eb4ac65321f11c1e75f26f95e0301d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3PrecomputeBundle}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
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
 *   <li>{@code nested types not received: Result}</li>
 *   <li>{@code member compile(MIndexPrecomputePlan, List<? extends CharSequence>, List<? extends CharSequence>, List<? extends CharSequence>, CompiledLanguageImage, MIndexAstArena) (return type)}</li>
 *   <li>{@code member compileMappedImage(CompiledLanguageImage) (return type)}</li>
 * </ul>
 */
public final class M3PrecomputeBundle {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3PrecomputeBundle() {
        throw new M3NotYetReceived("M3PrecomputeBundle#<init>/0");
    }
}
