// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.precompute.MIndexKnowledgeOwnershipBoundary}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/precompute/MIndexKnowledgeOwnershipBoundary.java}
 * blob {@code c49a9402e54b25c05d77abb4bdc40f66c35e5e35} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 12a5f2d36d10a31afcb8aeb56afc250f99fe1993b6f45c0ac0e10794872e3e40}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.precompute.M3KnowledgeOwnershipBoundary}); projection
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
 *   <li>{@code nested types not received: SubjectKind, Custody}</li>
 *   <li>{@code member custody(SubjectKind) (return type)}</li>
 *   <li>{@code member requiresSourceProvenance(SubjectKind) (parameter kind)}</li>
 *   <li>{@code member grantsSourceCopyAuthority(SubjectKind) (parameter kind)}</li>
 *   <li>{@code member grantsPromotionAuthority(SubjectKind) (parameter kind)}</li>
 * </ul>
 */
public final class M3KnowledgeOwnershipBoundary {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3KnowledgeOwnershipBoundary() {
        throw new M3NotYetReceived("M3KnowledgeOwnershipBoundary#<init>/0");
    }
}
