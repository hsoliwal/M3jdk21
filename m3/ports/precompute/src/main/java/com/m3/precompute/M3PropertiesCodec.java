// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexPropertiesCodec}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexPropertiesCodec.java}
 * blob {@code 05d8690a6aebd0c5764512112ddc5e3dd0f7a382} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c4c6fbcf13b2c44ce0925d28db169520c81ac808bd0437bddbf6f1edc7acd5a2}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3PropertiesCodec}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S25}.
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
 *   <li>{@code source supertypes not received: implements MIndexCodec}</li>
 *   <li>{@code member format() (return type)}</li>
 *   <li>{@code member decode(byte[], MIndexStructuralStore, boolean) (return type)}</li>
 * </ul>
 */
public final class M3PropertiesCodec {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    public M3PropertiesCodec() {
        throw new M3NotYetReceived("M3PropertiesCodec#<init>/0");
    }
}
