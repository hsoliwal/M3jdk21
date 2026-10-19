// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexNodePoolTreeImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexNodePoolTreeImage.java}
 * blob {@code 6d7a65b5e54f6b3dd5649aebf027443a7b6f7918} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 6578d5c8e53a768ce45c280058aa60e8b4aebd6e7abc1c3eef30ae29880c00a9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3NodePoolTreeImage}); projection
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
 *   <li>{@code member write(Path, MIndexNodePoolTreeView) (parameter tree)}</li>
 *   <li>{@code member write(Path, MIndexNodePoolTreeView, MIndexProgressMonitor) (parameter tree)}</li>
 * </ul>
 */
public final class M3NodePoolTreeImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3NodePoolTreeImage() {
        throw new M3NotYetReceived("M3NodePoolTreeImage#<init>/0");
    }
}
