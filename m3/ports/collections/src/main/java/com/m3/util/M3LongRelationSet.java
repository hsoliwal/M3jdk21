// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.label.MIndexLongRelationSet}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/algorithm/src/main/java/com/synexia/mindex/label/MIndexLongRelationSet.java}
 * blob {@code a5703e3d85e5ec78590e3b84fb070ee970cacaac} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 3f466e273ade356f2f7bd0dd96c508b9463ddf57f5605a2aed8cf59fb8eafe5e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.label.M3LongRelationSet}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: slots are addressed by offset in growable primitive lanes bounded by
 * {@code m3SlotCount}; per-slot state lives in {@code m3StateMask} bits, 64 slots per
 * word; source primitive fields keep their element type as lanes and source reference
 * fields become int handle lanes into parent-owned storage. No stored entry or node
 * objects; API facades are transient and parent-owned.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code no source constructor received; private scaffold constructor declared}</li>
 * </ul>
 */
final class M3LongRelationSet {
    /** DOOP slot bound: number of live slots addressed by offset in every lane. */
    private int m3SlotCount;

    /** DOOP per-slot state bits (live, deleted, frozen, dirty, resident); 64 slots per word. */
    private long[] m3StateMask;

    /** primitive lane for source field {@code keys} of type {@code long[]}. */
    private long[] keys;

    /** primitive lane for source field {@code states} of type {@code byte[]}. */
    private byte[] states;

    /** primitive lane for source field {@code size} of type {@code int}. */
    private int size;

    /** primitive lane for source field {@code used} of type {@code int}. */
    private int used;

    private M3LongRelationSet() {
        throw new M3NotYetReceived("M3LongRelationSet#<init>/0");
    }
}
