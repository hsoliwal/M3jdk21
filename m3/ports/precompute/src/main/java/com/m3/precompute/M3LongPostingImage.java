// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.MIndexLongPostingImage}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexLongPostingImage.java}
 * blob {@code 36d4e753446fbf7018dc65ca8b64db80257f857f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 9d49d9806cac13168f6c6f6e7a61f30e87e8c99dbc848956fd4113b119bfe49b}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.M3LongPostingImage}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S23}.
 * Map family {@code PRECOMPUTE}; naming-law family {@code PRECOMPUTE}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 */
final class M3LongPostingImage {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code universeSize} of type {@code int}. */
    private int universeSize;

    /** primitive lane for source field {@code keys} of type {@code long[]}. */
    private long[] keys;

    /** primitive lane for source field {@code offsets} of type {@code int[]}. */
    private int[] offsets;

    /** primitive lane for source field {@code rows} of type {@code int[]}. */
    private int[] rows;

    private M3LongPostingImage() {
        throw new M3NotYetReceived("M3LongPostingImage#<init>/0");
    }
}
