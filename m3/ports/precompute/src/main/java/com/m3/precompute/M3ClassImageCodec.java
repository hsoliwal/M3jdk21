// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.type.MIndexClassImageCodec}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/type/MIndexClassImageCodec.java}
 * blob {@code 50db892d8086083f1806e15dcadd1ded1fe8c401} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 4817eec4f02b34ed595ff508d1c83c8e5f21c5e04d3030e9b76f68e8445b01e3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.type.M3ClassImageCodec}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S21}.
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
 *   <li>{@code nested types not received: SearchOrder, MemberSeed, ClassRow, MemberRow, AnnotationSeed, AnnotationRow}</li>
 *   <li>{@code member encode(MIndexClassfileArchive) (parameter archive)}</li>
 *   <li>{@code member write(MIndexClassfileArchive, Path) (parameter archive)}</li>
 * </ul>
 */
public final class M3ClassImageCodec {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    private M3ClassImageCodec() {
        throw new M3NotYetReceived("M3ClassImageCodec#<init>/0");
    }
}
