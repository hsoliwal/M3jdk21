// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.MIndexPackedLexemeSnapshot}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexPackedLexemeSnapshot.java}
 * blob {@code 6f2eacfcdabeeffda407369beeed6fa9636cfb86} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e670fccd7a484403e1d6920539172163c5f099fb56c1bfe63311e8eb52502eeb}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.M3PackedLexemeSnapshot}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S17}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
final class M3PackedLexemeSnapshot {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3PackedLexemeSnapshot() {
        throw new M3NotYetReceived("M3PackedLexemeSnapshot#<init>/0");
    }
}
