// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexKnowledgeImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexKnowledgeImage.java}
 * blob {@code cbc55c207736b8bc5595a3ecd7188f6ade9a8d99} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code f54221123b4ea6e9e97f26403c71b1ea5e0c23df9c3c9cfe03f69659a0053a85}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3KnowledgeImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S22}.
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
 *   <li>{@code nested types not received: Header}</li>
 *   <li>{@code member write(MIndexKnowledgeSnapshot, Path) (parameter snapshot)}</li>
 *   <li>{@code member write(MIndexKnowledgeSnapshot, Path, IProgressMonitor) (parameter snapshot)}</li>
 *   <li>{@code member read(Path) (return type)}</li>
 *   <li>{@code member read(Path, IProgressMonitor) (return type)}</li>
 *   <li>{@code member map(Path) (return type)}</li>
 * </ul>
 */
public final class M3KnowledgeImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3KnowledgeImage() {
        throw new M3NotYetReceived("M3KnowledgeImage#<init>/0");
    }
}
