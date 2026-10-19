// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexCodecs}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexCodecs.java}
 * blob {@code 1f914178985a708ef03db0a3bb521c0e8bea469b} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code e1f6f14dd0681302b6bd9afab20200590a94da384be633090a02812307bcc5f3}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3Codecs}); projection
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
 *   <li>{@code member supports(MIndexSourceFormat) (parameter format)}</li>
 *   <li>{@code member decode(MIndexSourceFormat, byte[], MIndexStructuralStore, boolean) (return type)}</li>
 *   <li>{@code member defer(MIndexSourceFormat, byte[], MIndexStructuralStore, boolean) (return type)}</li>
 * </ul>
 */
public final class M3Codecs {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** int handle lane (offsets into parent-owned storage) for source field {@code codecs} of type {@code Map<MIndexSourceFormat, MIndexCodec>}. */
    private int[] codecsLane;

    public M3Codecs() {
        throw new M3NotYetReceived("M3Codecs#<init>/0");
    }

    public M3Codecs register(M3Codec codec) {
        throw new M3NotYetReceived("M3Codecs#register/1");
    }
}
