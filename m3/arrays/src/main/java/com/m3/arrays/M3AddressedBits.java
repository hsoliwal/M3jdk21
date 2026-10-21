// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexAddressedBits}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexAddressedBits.java}
 * blob {@code 2694b63a9299f5e04d503798d52523340e5c3e99} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 10a3504d179e2c060d08b88dd0cda3f6fc1e5eb27f70f17936e5031fccf5c983}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3AddressedBits}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S01}.
 * Map family {@code ARRAYS}; naming-law family {@code ARRAYS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
final class M3AddressedBits {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code modes} of type {@code int[]}. */
    private int[] modes;

    /** primitive lane for source field {@code shifts} of type {@code int[]}. */
    private int[] shifts;

    /** primitive lane for source field {@code masks} of type {@code long[]}. */
    private long[] masks;

    /** primitive lane for source field {@code xor} of type {@code long}. */
    private long xor;

    /** primitive lane for source field {@code identity} of type {@code boolean}. */
    private boolean identity;

    /** primitive lane for source field {@code constant} of type {@code boolean}. */
    private boolean constant;

    private M3AddressedBits() {
        throw new M3NotYetReceived("M3AddressedBits#<init>/0");
    }
}
